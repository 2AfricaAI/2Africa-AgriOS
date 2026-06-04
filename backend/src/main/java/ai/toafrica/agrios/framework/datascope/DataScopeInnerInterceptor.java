package ai.toafrica.agrios.framework.datascope;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sprint 51 -- MyBatis-Plus inner interceptor that rewrites SELECT
 * statements based on the ambient {@link DataScopeContext}.
 *
 * <p>Strategy:</p>
 * <ul>
 *   <li>scope=all  -- no rewrite (audit logged elsewhere)</li>
 *   <li>scope=group + node ids <= maxInList -- inject WHERE col IN (...)</li>
 *   <li>scope=group + node ids > maxInList  -- inject EXISTS subquery against org_node.ancestors</li>
 *   <li>scope=group + user has no primary node -- inject WHERE 1=0 (default lock-out)</li>
 *   <li>scope=self -- inject WHERE created_by = ?</li>
 * </ul>
 *
 * <p>Only rewrites queries that mention the table named in
 * {@link DataScope#table()}. Other tables in the same query are untouched.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DataScopeInnerInterceptor implements InnerInterceptor {

    /**
     * Lazy suppliers -- the interceptor is built by MyBatis-Plus
     * autoconfig, which is in turn a dependency of {@code OrgUserMapper}.
     * Holding direct refs to {@link DataScopeService} / {@link DataScopeProperties}
     * here would form a bean-creation cycle, so we look them up on first use.
     */
    private final Supplier<DataScopeService> dsServiceSupplier;
    private final Supplier<DataScopeProperties> propsSupplier;

    private volatile DataScopeService dsService;
    private volatile DataScopeProperties props;

    private DataScopeService ds() {
        DataScopeService v = dsService;
        if (v == null) dsService = v = dsServiceSupplier.get();
        return v;
    }
    private DataScopeProperties props() {
        DataScopeProperties v = props;
        if (v == null) props = v = propsSupplier.get();
        return v;
    }

    /**
     * Note: ResultHandler intentionally raw to match the InnerInterceptor
     * interface erasure (MP exposes it as raw to avoid generic mismatch
     * with the Ibatis SPI). Do not add a wildcard here.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler,
                            BoundSql boundSql) {
        if (!props().isEnabled()) return;
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) return;
        DataScopeContext.Holder holder = DataScopeContext.peek();
        if (holder == null) return;

        String original = boundSql.getSql();
        String rewritten = tryRewrite(original, holder);
        if (rewritten != null && !rewritten.equals(original)) {
            PluginUtils.mpBoundSql(boundSql).sql(rewritten);
            if (log.isDebugEnabled()) {
                log.debug("[datascope] sql rewritten table={} scope={} primary={}",
                        holder.table(), holder.dataScope(), holder.primaryNodeId());
            }
        }
    }

    // -------------------------------------------------------------------
    // Rewrite engine
    // -------------------------------------------------------------------

    private String tryRewrite(String sql, DataScopeContext.Holder h) {
        // Fast path -- if the SQL doesn't mention the target table at all, no work
        String lc = sql.toLowerCase();
        if (!lc.contains(h.table().toLowerCase())) return sql;

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select select)) return sql;
            if (!(select.getSelectBody() instanceof PlainSelect plain)) return sql;

            String fromAlias = matchedAliasOrName(plain, h.table());
            if (fromAlias == null) return sql;

            Expression injection = buildFilter(h, fromAlias);
            if (injection == null) return sql;

            Expression existing = plain.getWhere();
            plain.setWhere(existing == null ? injection : new AndExpression(existing, injection));
            return select.toString();
        } catch (JSQLParserException ex) {
            log.warn("[datascope] parse failed, falling back to raw SQL: {}", ex.getMessage());
            return sql;
        }
    }

    private String matchedAliasOrName(PlainSelect plain, String table) {
        var from = plain.getFromItem();
        if (from instanceof Table t) {
            if (table.equalsIgnoreCase(t.getName())) {
                return t.getAlias() != null ? t.getAlias().getName() : t.getName();
            }
        }
        if (plain.getJoins() != null) {
            for (var j : plain.getJoins()) {
                if (j.getRightItem() instanceof Table t
                        && table.equalsIgnoreCase(t.getName())) {
                    return t.getAlias() != null ? t.getAlias().getName() : t.getName();
                }
            }
        }
        return null;
    }

    private Expression buildFilter(DataScopeContext.Holder h, String tableAlias) {
        String scope = h.dataScope() == null ? "self" : h.dataScope().toLowerCase();
        Column col;
        switch (scope) {
            case "all":
                return null;

            case "self":
                if (h.useCreatedByForSelf()) {
                    col = new Column(tableAlias + "." + h.createdByColumn());
                    EqualsTo eq = new EqualsTo();
                    eq.setLeftExpression(col);
                    eq.setRightExpression(new LongValue(h.currentUserId()));
                    return eq;
                }
                // fall through to group semantics

            case "group":
            default: {
                col = new Column(tableAlias + "." + h.column());
                List<Long> ids = ds().visibleNodeIds(h.currentUserId(), scope);
                if (ids == null) return null;
                if (ids.isEmpty()) {
                    // User has no primary node -- lock them out
                    EqualsTo never = new EqualsTo();
                    never.setLeftExpression(new LongValue(1));
                    never.setRightExpression(new LongValue(0));
                    return never;
                }
                if (ids.size() <= props().getMaxInList()) {
                    InExpression in = new InExpression();
                    in.setLeftExpression(col);
                    // JSqlParser 4.6 -- ExpressionList is NOT generic
                    List<Expression> items = new ArrayList<>(ids.size());
                    for (Long id : ids) items.add(new LongValue(id));
                    in.setRightItemsList(new ExpressionList(items));
                    return in;
                }
                // Too many ids -- fall back to a subquery against org_node
                try {
                    String subSql = String.format(
                            "%s IN (SELECT n.id FROM org_node n WHERE n.deleted_at IS NULL "
                          + "AND (n.id = %d OR n.ancestors = '%d' "
                          + "OR n.ancestors LIKE '%d/%%' OR n.ancestors LIKE '%%/%d/%%' "
                          + "OR n.ancestors LIKE '%%/%d'))",
                            tableAlias + "." + h.column(),
                            h.primaryNodeId(), h.primaryNodeId(),
                            h.primaryNodeId(), h.primaryNodeId(), h.primaryNodeId());
                    return CCJSqlParserUtil.parseCondExpression(subSql);
                } catch (JSQLParserException e) {
                    log.warn("[datascope] fallback subquery parse failed: {}", e.getMessage());
                    return null;
                }
            }
        }
    }
}

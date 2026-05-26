package ai.toafrica.agrios.finance.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.finance.dto.CollectionLogForm;
import ai.toafrica.agrios.finance.entity.CollectionLog;
import ai.toafrica.agrios.finance.mapper.CollectionLogMapper;
import ai.toafrica.agrios.finance.vo.CollectionLogVO;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import ai.toafrica.agrios.system.entity.SysUser;
import ai.toafrica.agrios.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 催收跟催记录 Service - Sprint 16.2
 *   - 录入跟催 → 回写 customer.last_collection_date + next_action_date
 *   - "下次跟催日" 进 ActionBoard 提醒列表
 *   - "客户承诺还款" 进 13 周现金流预测
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionLogService {

    private final CollectionLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper userMapper;

    public PageResult<CollectionLogVO> page(Long customerId, Long orderId, String outcome, PageQuery pq) {
        QueryWrapper<CollectionLogVO> q = new QueryWrapper<>();
        q.isNull("l.deleted_at");
        if (customerId != null) q.eq("l.customer_id", customerId);
        if (orderId != null)    q.eq("l.order_id", orderId);
        if (outcome != null && !outcome.isBlank()) q.eq("l.outcome", outcome.trim());
        q.orderByDesc("l.log_date").orderByDesc("l.id");

        Page<CollectionLogVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(logMapper.pageWithJoin(p, q));
    }

    public List<CollectionLogVO> listByCustomer(Long customerId) {
        return logMapper.findByCustomer(customerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CollectionLogForm form) {
        Customer customer = customerMapper.selectById(form.getCustomerId());
        if (customer == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");

        // outcome=promised 时必须填承诺还款日
        if ("promised".equals(form.getOutcome()) && form.getPromisedDate() == null) {
            throw new BusinessException(R.BUSINESS_ERROR,
                    "promisedDate is required when outcome = promised");
        }

        Long uid = SecurityUtil.currentUserId();
        SysUser user = userMapper.selectById(uid);
        String uname = user != null ? user.getUsername() : null;

        CollectionLog log = new CollectionLog();
        log.setCustomerId(form.getCustomerId());
        log.setOrderId(form.getOrderId());
        log.setLogDate(form.getLogDate());
        log.setChannel(form.getChannel());
        log.setContactPerson(form.getContactPerson());
        log.setOutcome(form.getOutcome());
        log.setPromisedDate(form.getPromisedDate());
        log.setPromisedAmount(form.getPromisedAmount());
        log.setContent(form.getContent());
        log.setNextActionDate(form.getNextActionDate());
        log.setOperatorId(uid);
        log.setOperatorName(uname);
        logMapper.insert(log);

        // 级联回写客户冷字段
        refreshCustomerCollectionDates(customer, form.getLogDate(), form.getNextActionDate());

        CollectionLogService.log.info(
                "[Collection] customer={} order={} channel={} outcome={} promisedDate={} nextAction={} by user={}",
                customer.getCode(),
                form.getOrderId() != null ? form.getOrderId() : "-",
                form.getChannel(), form.getOutcome(),
                form.getPromisedDate(), form.getNextActionDate(), uname);

        return log.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CollectionLog log = logMapper.selectById(id);
        if (log == null) throw new BusinessException(R.NOT_FOUND, "Collection log not found");
        log.setDeletedAt(java.time.LocalDateTime.now());
        logMapper.updateById(log);
    }

    /**
     * 重新计算客户的 last_collection_date / next_action_date.
     * 取该客户所有未删除跟催的:
     *   - last_collection_date = MAX(log_date)
     *   - next_action_date     = MIN(next_action_date WHERE next_action_date >= today)
     */
    private void refreshCustomerCollectionDates(Customer customer, LocalDate newLogDate, LocalDate newNextAction) {
        // 快速路径: 新增的本次记录就是最近一次
        LocalDate today = LocalDate.now();

        // last = max(原 last, 本次)
        LocalDate currentLast = customer.getLastCollectionDate();
        if (currentLast == null || newLogDate.isAfter(currentLast)) {
            customer.setLastCollectionDate(newLogDate);
        }

        // next = min(原 next 若 >=today, 本次 next 若 >=today)
        LocalDate currentNext = customer.getNextActionDate();
        boolean currentNextValid = currentNext != null && !currentNext.isBefore(today);
        boolean newNextValid     = newNextAction != null && !newNextAction.isBefore(today);

        if (newNextValid && (!currentNextValid || newNextAction.isBefore(currentNext))) {
            customer.setNextActionDate(newNextAction);
        } else if (!currentNextValid && !newNextValid) {
            customer.setNextActionDate(null);
        }

        customerMapper.updateById(customer);
    }

    /**
     * 当前生效的客户承诺还款列表 (进现金流预测)
     */
    public List<Map<String, Object>> findActivePromises() {
        return logMapper.findActivePromises(LocalDate.now());
    }
}

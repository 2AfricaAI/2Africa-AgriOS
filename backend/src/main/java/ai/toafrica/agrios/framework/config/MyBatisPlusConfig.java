package ai.toafrica.agrios.framework.config;

import ai.toafrica.agrios.framework.datascope.DataScopeInnerInterceptor;
import ai.toafrica.agrios.framework.datascope.DataScopeProperties;
import ai.toafrica.agrios.framework.datascope.DataScopeService;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * - 分页插件
 * - 乐观锁插件（用于 inventory.version 等）
 * - Sprint 51: DataScope 拦截器（按 @DataScope 注解注入 WHERE）
 * - 自动填充 createdAt / updatedAt / createdBy / updatedBy
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ApplicationContext ctx) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // Sprint 51 -- the DataScope inner interceptor depends transitively
        // on Mapper beans, so we use Supplier-based lazy lookup to avoid
        // the bean-creation cycle (interceptor -> service -> mapper ->
        // sqlSessionFactory -> mybatisPlusInterceptor).
        interceptor.addInnerInterceptor(new DataScopeInnerInterceptor(
                () -> ctx.getBean(DataScopeService.class),
                () -> ctx.getBean(DataScopeProperties.class)
        ));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler autoFillMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                // createdBy / updatedBy 由 AOP 或 Service 注入
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}

package ai.toafrica.agrios.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据范围注解
 * 由 AOP 拦截方法上的注解，根据当前用户的 dataScope 自动追加 SQL WHERE 条件
 *
 * 用法：
 * <pre>
 * &#64;DataScope(field = "owner_id")
 * List&lt;Plot&gt; list(...)
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 业务字段名（数据库列），用于拼接 WHERE，如 owner_id */
    String field() default "owner_id";

    /** 业务表别名 */
    String alias() default "";

    /** 仅 manager / admin 角色绕过 */
    String[] bypassRoles() default {"SUPER_ADMIN", "MANAGER"};
}

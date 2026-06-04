package ai.toafrica.agrios.framework.datascope;

/**
 * Sprint 51 -- thread-local bridge between the {@code @DataScope}
 * annotation (captured by an AOP aspect) and the MyBatis interceptor
 * (executed later in the same thread).
 *
 * <p>MyBatis interceptors cannot see controller-method annotations
 * directly, so the aspect pushes {@link Holder} here before the method
 * runs and clears it in a {@code finally} block.</p>
 *
 * <p>Stack semantics: nested {@code @DataScope} calls (e.g. service
 * calling a sub-service) push additional frames. The interceptor reads
 * {@link #peek()} (top of stack).</p>
 */
public final class DataScopeContext {

    private DataScopeContext() {}

    private static final ThreadLocal<java.util.Deque<Holder>> STACK =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    public static void push(Holder h) { STACK.get().push(h); }

    public static Holder peek() { return STACK.get().peek(); }

    public static void pop() {
        java.util.Deque<Holder> s = STACK.get();
        if (!s.isEmpty()) s.pop();
        if (s.isEmpty()) STACK.remove();    // avoid leaking via pooled threads
    }

    public static boolean active() {
        Holder h = peek();
        return h != null;
    }

    /** Immutable payload captured at the AOP entry point. */
    public record Holder(
            String resource,
            String table,
            String column,
            boolean useCreatedByForSelf,
            String createdByColumn,
            Long currentUserId,
            String dataScope,
            String roleCodes,
            Long primaryNodeId
    ) {}
}

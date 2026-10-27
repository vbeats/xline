package com.csf.xline.core.routing;

/**
 * 当前节点上下文。
 *
 * <p>基于 ThreadLocal 指定当前线程要操作的节点，供 MyBatis-Plus 等方式在
 * 多节点之间切换数据源。通过事件总线推送数据时无需关心本上下文。
 *
 * <p>使用完毕后务必调用 {@link #clear()}，避免线程复用导致串节点。
 */
public final class NodeContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<String>();

    private NodeContext() {
    }

    /**
     * 指定当前线程使用的节点。
     *
     * @param nodeName 节点名
     */
    public static void use(String nodeName) {
        CURRENT.set(nodeName);
    }

    /** 清除当前线程的节点设置 */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 获取当前线程指定的节点。
     *
     * @return 节点名，未指定时返回 {@code null}
     */
    public static String current() {
        return CURRENT.get();
    }
}

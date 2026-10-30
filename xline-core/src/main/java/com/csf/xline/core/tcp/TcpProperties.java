package com.csf.xline.core.tcp;

import lombok.Data;

/**
 * TCP 节点配置段（{@code xline.nodes[].tcp}）。
 *
 * <p>节点可工作在两种模式下：
 * <ul>
 *     <li>{@link Mode#SERVER} —— 监听端口，收到对端数据后作为数据源头推给下游；</li>
 *     <li>{@link Mode#CLIENT} —— 主动连接对端，向上游收到的数据写往对端。</li>
 * </ul>
 * 本类只是配置载体，连接与收发由 {@code xline-tcp} 模块实现，core 不含任何通信依赖。
 */
@Data
public class TcpProperties {

    /** TCP 节点运行模式 */
    public enum Mode {

        /** 监听端口，接受对端连接 */
        SERVER,

        /** 主动连接对端 */
        CLIENT
    }

    /** 运行模式：server 监听端口、client 连接对端 */
    private Mode mode = Mode.SERVER;

    /** server 模式为绑定地址（留空表示本机全部地址），client 模式为对端地址 */
    private String host;

    /** server 模式为监听端口，client 模式为对端端口，必填 */
    private Integer port;

    /** 收发使用的字符编码 */
    private String charset = "UTF-8";

    /** 发送时是否追加换行符：按行收发的场景开启 */
    private Boolean appendNewLine = Boolean.TRUE;

    /** 接收缓冲区大小（字节） */
    private Integer bufferSize = 1024;

    /**
     * 读取超时（毫秒）：0 表示不启用（一直等满一帧）。
     *
     * <p>自定义帧解码器靠它做「空闲即出帧」：不足最大帧长、且后面没有新帧头时，
     * 空闲超过该时长就把已收到的字节作为一帧交出，否则最后一帧要等到连接关闭才吐出。
     */
    private Integer readTimeoutMillis = 0;

    /** client 模式断线后是否自动重连 */
    private Boolean reconnect = Boolean.TRUE;

    /** 重连间隔（毫秒） */
    private Long reconnectIntervalMillis = 3000L;

    /**
     * 取生效端口。
     *
     * @return 端口
     * @throws IllegalStateException 未配置端口
     */
    public int resolvedPort() {
        if (port == null || port <= 0) {
            throw new IllegalStateException("tcp 节点必须配置 port");
        }
        return port;
    }
}

package com.csf.xline.tcp;

import com.csf.xline.core.tcp.TcpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP 节点连接入口。
 *
 * <p>各节点的 socket 生命周期集中在此登记：
 * 启动即监听（server）或连接（client），停止时统一关闭。
 *
 * <p>数据方向：
 * <ul>
 *     <li>读到对端数据 —— 以本节点为源头推向下游；</li>
 *     <li>{@link #send(TcpNode, Object)} —— 写往对端，由业务处理器触发。</li>
 * </ul>
 */
@Slf4j
public class TcpEndpoint {

    /** 已启动的节点连接：key 为节点名 */
    private static final Map<String, TcpLink> LINKS = new ConcurrentHashMap<>();

    /**
     * 启动节点连接。
     *
     * @param node TCP 节点
     * @throws Exception 端口被占用、绑定地址非法等
     */
    public static void start(TcpNode node) throws Exception {
        TcpProperties properties = node.getTcp();
        Assert.notNull(properties, String.format("【%s】 未配置 tcp 配置段", node.getName()));

        TcpLink link = new TcpLink(node);
        link.start();
        LINKS.put(node.getName(), link);

        log.info("【Tcp - {}】started: {}:{}", properties.getMode(), properties.getHost(), properties.resolvedPort());
    }

    /**
     * 停止节点连接并释放全部 socket。
     *
     * @param node TCP 节点
     */
    public static void stop(TcpNode node) {
        TcpLink link = LINKS.remove(node.getName());
        if (link != null) {
            link.close();
            log.info("【Tcp】stopped: {}", node.getName());
        }
    }

    /**
     * 向对端写出数据。
     *
     * @param node TCP 节点
     * @param data 待发送数据
     * @throws Exception 节点未初始化
     */
    public static void send(TcpNode node, Object data) throws Exception {
        TcpLink link = LINKS.get(node.getName());
        Assert.notNull(link, String.format("【%s】 TCP 节点尚未初始化", node.getName()));
        link.send(data);
    }
}

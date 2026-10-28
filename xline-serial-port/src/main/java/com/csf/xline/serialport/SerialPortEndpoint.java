package com.csf.xline.serialport;

import com.csf.xline.core.serialport.SerialPortProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 串口节点入口。
 *
 * <p>各节点的串口生命周期集中在此登记：启动时打开，停止时统一关闭。
 *
 * <p>数据方向：
 * <ul>
 *     <li>读到串口数据 —— 以本节点为源头推向下游；</li>
 *     <li>{@link #send(SerialPortNode, Object)} —— 写往串口，由业务处理器触发，
 *         无业务处理器时由节点直接透明转发。</li>
 * </ul>
 */
@Slf4j
public class SerialPortEndpoint {

    /** 已启动的节点链路：key 为节点名 */
    private static final Map<String, SerialPortLink> LINKS = new ConcurrentHashMap<>();

    /**
     * 打开节点串口。
     *
     * <p>串口不存在 / 被占用时不抛异常：留给后台线程按 {@code reconnect} 重开。
     *
     * @param node 串口节点
     * @throws Exception 未配置 serial-port 配置段
     */
    public static void start(SerialPortNode node) throws Exception {
        SerialPortProperties properties = node.getSerialPort();
        Assert.notNull(properties, String.format("【%s】 未配置 serial-port 配置段", node.getName()));

        SerialPortLink link = new SerialPortLink(node);
        link.start();
        LINKS.put(node.getName(), link);
    }

    /**
     * 关闭节点串口并释放线程。
     *
     * @param node 串口节点
     */
    public static void stop(SerialPortNode node) {
        SerialPortLink link = LINKS.remove(node.getName());
        if (link != null) {
            link.close();
            log.info("【SerialPort】stopped: {}", node.getName());
        }
    }

    /**
     * 向串口写出数据。
     *
     * @param node 串口节点
     * @param data 待发送数据：{@code byte[]} 原样写出，其余按 {@code charset} 转成文本后写出
     * @throws Exception 节点未初始化
     */
    public static void send(SerialPortNode node, Object data) throws Exception {
        SerialPortLink link = LINKS.get(node.getName());
        Assert.notNull(link, String.format("【%s】 串口节点尚未初始化", node.getName()));
        link.send(toBytes(data, node.getSerialPort()));
    }

    /**
     * 把上游数据转为串口字节。
     *
     * @param data       上游数据
     * @param properties 本节点配置
     * @return 待写出字节
     */
    private static byte[] toBytes(Object data, SerialPortProperties properties) {
        if (data == null) {
            return new byte[0];
        }
        if (data instanceof byte[]) {
            return (byte[]) data;
        }
        return String.valueOf(data).getBytes(Charset.forName(properties.getCharset()));
    }
}

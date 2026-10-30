package com.csf.xline.app.service;

import cn.hutool.core.util.HexUtil;
import com.csf.xline.core.tcp.TcpProperties;
import com.csf.xline.tcp.TcpNode;
import com.csf.xline.tcp.handler.TcpDataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TCP 数据转发 Demo：把上游推来的数据写往本节点的对端。
 *
 * <p>配合 {@code demo/tcp-test.yml} 使用：
 * <pre>
 *   9847 的客户端    ──▶ node1 ──▶ node2 ──▶ 127.0.0.1:8888 的服务端
 *   127.0.0.1:8888 ──▶ node2 ──▶ node1 ──▶ 9847 上的全部客户端
 * </pre>
 * 转发不需要任何路由代码：struct 已定义两个方向的边，
 * 本处理器只需在 {@code rec} 中把数据写往当前节点的对端 ——
 * server 模式广播给全部在线连接，client 模式写往对端。
 *
 * <p>推过来的是帧解码器解出的原始字节（含帧头），{@code node.send} 原样写出，不做任何转换；
 * 日志按大写十六进制记录，避免非 ASCII 字节打印成乱码。
 *
 * <p>容器中只有一个 {@link TcpDataHandler} 实现时，全部 TCP 节点共用它（按 Bean 名与节点名匹配，
 * 匹配不到即取唯一实现），故 node1 / node2 由本类统一处理。
 */
@Service
@Slf4j
public class TcpForwardDataHandler implements TcpDataHandler {

    @Override
    public void init(TcpNode node) throws Exception {
        TcpProperties tcp = node.getTcp();
        if (tcp == null) {
            log.warn("【Tcp - demo】节点 [{}] 未配置 tcp 段，不参与转发", node.getName());
            return;
        }
        log.info("【Tcp - demo】节点 [{}] 就绪：mode={} host={} port={}",
                node.getName(), tcp.getMode(), tcp.getHost(), tcp.getPort());
    }

    /**
     * 收到上游数据：原样写往本节点的对端。
     *
     * @param node 当前节点（node1 或 node2）
     * @param data 上游数据：帧解码器解出的一帧字节
     */
    @Override
    public void rec(TcpNode node, Object data) throws Exception {
        if (data == null) {
            return;
        }
        node.send(data);
        log.info("【Tcp - demo】[{}] → 对端：{}", node.getName(), text(data));
    }

    @Override
    public void destroy(TcpNode node) throws Exception {
        log.info("【Tcp - demo】节点 [{}] 停止转发", node.getName());
    }

    /**
     * 日志文本：字节按大写十六进制，其余按字符串。
     *
     * @param data 上游数据
     * @return 可打印文本
     */
    private static String text(Object data) {
        return data instanceof byte[] ? HexUtil.encodeHexStr((byte[]) data, false) : String.valueOf(data);
    }
}

package com.csf.xline.tcp;

import com.csf.xline.core.tcp.TcpProperties;
import com.csf.xline.tcp.codec.LineFrameDecoder;
import com.csf.xline.tcp.codec.TcpFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 单个 TCP 节点的连接：监听 / 连接、按行读取、写出数据。
 *
 * <p>读取到的每行数据以本节点为源头推向下游（{@link TcpNode#pushMsg(Object)}）；
 * 写出方向由业务通过 {@code TcpNode.send} 触发。
 *
 * <p>client 模式的首次连接与重连都在后台线程完成，不阻塞节点初始化。
 */
@Slf4j
class TcpLink {

    private final TcpNode node;
    private final TcpProperties properties;
    private final Charset charset;
    private final ExecutorService executor;

    /** 帧解码器：由装配阶段注入，缺省按行分帧 */
    private final TcpFrameDecoder decoder;

    /** server 模式下已接入的客户端 */
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();

    private volatile ServerSocket serverSocket;

    /** client 模式下与对端的连接 */
    private volatile Socket socket;

    private volatile boolean running = true;

    TcpLink(TcpNode node) {
        this.node = node;
        this.properties = node.getTcp();
        this.charset = Charset.forName(properties.getCharset());
        this.executor = Executors.newCachedThreadPool(threadFactory(node));
        this.decoder = node.getDecoder() == null ? new LineFrameDecoder() : node.getDecoder();
    }

    /**
     * 启动：server 模式监听端口，client 模式后台连接对端。
     *
     * @throws Exception 端口占用、绑定地址非法，或 client 模式未配置 host
     */
    void start() throws Exception {
        if (properties.getMode() == TcpProperties.Mode.SERVER) {
            InetAddress bind = StringUtils.hasText(properties.getHost())
                    ? InetAddress.getByName(properties.getHost()) : null;
            serverSocket = new ServerSocket(properties.resolvedPort(), 50, bind);
            executor.execute(this::acceptLoop);
            return;
        }
        if (!StringUtils.hasText(properties.getHost())) {
            throw new IllegalStateException(String.format("【%s】 client 模式必须配置 host", node.getName()));
        }
        executor.execute(this::connectLoop);
    }

    /**
     * 写出数据：server 模式广播给全部在线客户端，client 模式写往对端。
     *
     * @param data 待发送数据：{@code byte[]} 原样写出（自定义帧解码场景），
     *             其余按 {@code charset} 转文本，换行由 {@code append-new-line} 决定
     * @throws IOException 写出失败
     */
    void send(Object data) throws IOException {
        byte[] bytes = toBytes(data);
        if (serverSocket != null) {
            for (Socket client : clients) {
                write(client, bytes);
            }
            return;
        }
        Socket target = socket;
        if (target == null) {
            log.warn("【Tcp】尚未连接对端，数据未发送：{}", node.getName());
            return;
        }
        write(target, bytes);
    }

    /**
     * 把待发送数据转为字节：{@code byte[]} 原样写出，其余按 {@code charset} 编码。
     *
     * @param data 待发送数据
     * @return 待写出字节
     */
    private byte[] toBytes(Object data) {
        if (data == null) {
            return new byte[0];
        }
        if (data instanceof byte[]) {
            return (byte[]) data;
        }
        String text = String.valueOf(data);
        return Boolean.TRUE.equals(properties.getAppendNewLine())
                ? (text + "\n").getBytes(charset) : text.getBytes(charset);
    }

    /**
     * 关闭全部 socket 与线程。
     *
     * <p>server 模式可能挂着几十万客户端连接，逐个串行 {@code close()}
     * 会把停机时间拉长到秒级以上，故并行关闭。
     */
    void close() {
        running = false;
        close(serverSocket);
        if (!clients.isEmpty()) {
            // 几十万级连接时并行关闭：close() 是阻塞系统调用，串行会把停机拖长
            clients.parallelStream().forEach(client -> close(client));
        }
        clients.clear();
        close(socket);
        executor.shutdownNow();
    }

    /** server 模式：循环接收连接，每个连接起一个读线程 */
    private void acceptLoop() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket accepted = serverSocket.accept();
                applyOptions(accepted);
                clients.add(accepted);
                log.info("【Tcp】客户端接入：{} <- {}", node.getName(), accepted.getRemoteSocketAddress());
                executor.execute(() -> readLoop(accepted));
            } catch (IOException e) {
                if (running) {
                    log.warn("【Tcp】接收连接失败：{}", node.getName(), e);
                }
                break;
            }
        }
    }

    /** client 模式：连接对端并在断开后按配置重连 */
    private void connectLoop() {
        while (running) {
            try {
                Socket connected = new Socket(properties.getHost(), properties.resolvedPort());
                applyOptions(connected);
                socket = connected;
                log.info("【Tcp】已连接对端：{} -> {}:{}",
                        node.getName(), properties.getHost(), properties.resolvedPort());
                readLoop(connected);
            } catch (Exception e) {
                if (running) {
                    log.warn("【Tcp】连接失败或连接已断开：{}", node.getName(), e);
                }
            }
            if (!running || !Boolean.TRUE.equals(properties.getReconnect())) {
                break;
            }
            sleepQuietly(properties.getReconnectIntervalMillis());
        }
    }

    /** 按解码器分帧读取对端数据，每帧以本节点为源头推向下游 */
    private void readLoop(Socket connected) {
        int bufferSize = properties.getBufferSize() == null || properties.getBufferSize() <= 0
                ? 1024 : properties.getBufferSize();
        try (PushbackInputStream in = new PushbackInputStream(
                new BufferedInputStream(connected.getInputStream(), bufferSize), TcpFrameDecoder.PUSHBACK_SIZE)) {
            while (running) {
                byte[] frame;
                try {
                    frame = decoder.decode(in);
                } catch (SocketTimeoutException e) {
                    // 解码器把超时抛回来了：连接仍可用，续等下一帧
                    continue;
                }
                if (frame == null) {
                    break;
                }
                node.pushMsg(frame);
            }
        } catch (IOException e) {
            if (running) {
                log.warn("【Tcp】读取数据异常：{}", node.getName(), e);
            }
        } finally {
            clients.remove(connected);
            close(connected);
        }
    }

    private void write(Socket target, byte[] bytes) {
        if (target == null || target.isClosed()) {
            return;
        }
        try {
            OutputStream out = target.getOutputStream();
            synchronized (target) {
                out.write(bytes);
                out.flush();
            }
        } catch (IOException e) {
            log.warn("【Tcp】写出失败，关闭连接：{}", node.getName(), e);
            clients.remove(target);
            close(target);
        }
    }

    /**
     * 应用 socket 选项：读取超时供帧解码器做「空闲即出帧」。
     *
     * <p>0 / 未配置时不设置，保持一直等待 —— 默认行为不受影响。
     */
    private void applyOptions(Socket target) {
        Integer timeout = properties.getReadTimeoutMillis();
        if (timeout == null || timeout <= 0) {
            return;
        }
        try {
            target.setSoTimeout(timeout);
        } catch (IOException e) {
            log.warn("【Tcp】设置读取超时失败：{}", node.getName(), e);
        }
    }

    /** 线程工厂：优先用节点注入的供给（JDK 21 虚拟线程），缺省用平台线程 */
    private static ThreadFactory threadFactory(TcpNode node) {
        return node.getThreadFactory() == null
                ? Executors.defaultThreadFactory()
                : node.getThreadFactory().create("xline-tcp-" + node.getName());
    }

    private static void sleepQuietly(Long millis) {
        long timeout = millis == null || millis <= 0 ? 3000L : millis;
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 关闭失败无需处理
        }
    }
}

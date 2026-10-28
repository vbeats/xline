package com.csf.xline.serialport;

import cn.hutool.core.util.HexUtil;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.serialport.SerialPortProperties;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 单个串口节点的链路：打开串口、读空接收缓冲、写出数据。
 *
 * <p>读到的字节以本节点为源头推向下游（{@link XLineContext#pushMsg(String, Object)}）；
 * 写出方向由 {@link #send(byte[])} 触发，二者都以大写十六进制记录原始数据。
 *
 * <p>串口设备不存在或被拔掉时不阻塞节点初始化：
 * 首次打开失败与运行中断开都在后台线程按 {@code reconnect} 配置重开。
 */
@Slf4j
class SerialPortLink {

    private final SerialPortNode node;

    private final SerialPortProperties properties;

    private final ExecutorService executor;

    /**
     * jSerialComm 回调。
     *
     * <p>有数据可读时读空接收缓冲并逐段推向下游；串口断开时关闭并重开。
     */
    private final SerialPortDataListener listener = new SerialPortDataListener() {

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                receive();
                return;
            }
            if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                onDisconnected();
            }
        }
    };

    private volatile SerialPort serialPort;

    private volatile boolean running = true;

    private volatile boolean reconnecting = false;

    SerialPortLink(SerialPortNode node) {
        this.node = node;
        this.properties = node.getSerialPort();
        this.executor = Executors.newSingleThreadExecutor(threadFactory(node));
    }

    /**
     * 打开串口；设备不存在时转入后台重开，不抛异常。
     */
    void start() {
        if (!open()) {
            reconnect();
        }
    }

    /**
     * 写出数据。
     *
     * @param data 待发送字节
     */
    void send(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        SerialPort port = serialPort;
        if (port == null || !port.isOpen()) {
            log.warn("【SerialPort】{}【{}】 串口未打开，数据未发送：{}",
                    node.getName(), properties.getDevice(), HexUtil.encodeHexStr(data, false));
            return;
        }

        log.info("【SerialPort】{}【{}】 send data: {}",
                node.getName(), properties.getDevice(), HexUtil.encodeHexStr(data, false));

        int written = port.writeBytes(data, data.length);
        if (written < data.length) {
            log.warn("【SerialPort】{}【{}】 写出不完整：{}/{}",
                    node.getName(), properties.getDevice(), written, data.length);
        }
    }

    /** 关闭串口与线程 */
    void close() {
        running = false;
        closePort();
        executor.shutdownNow();
    }

    /**
     * 打开串口并挂上数据监听。
     *
     * @return 打开成功返回 {@code true}
     */
    private boolean open() {
        try {
            SerialPort port = SerialPort.getCommPort(properties.resolvedDevice());
            port.setComPortParameters(intValue(properties.getBaudRate(), 115200),
                    intValue(properties.getDataBits(), 8),
                    intValue(properties.getStopBits(), 1),
                    intValue(properties.getParity(), 0),
                    boolValue(properties.getUseRs485Mode(), false));

            if (!port.openPort()) {
                log.warn("【SerialPort】{}【{}】 打开失败，串口不存在或被占用",
                        node.getName(), properties.getDevice());
                return false;
            }

            serialPort = port;
            port.addDataListener(listener);
            log.info("【SerialPort】{}【{}】 打开成功，数据监听中", node.getName(), properties.getDevice());
            return true;
        } catch (SerialPortInvalidPortException e) {
            log.warn("【SerialPort】{}【{}】 非法串口设备", node.getName(), properties.getDevice());
            return false;
        } catch (Exception e) {
            log.warn("【SerialPort】{}【{}】 打开异常：{}", node.getName(), properties.getDevice(), e.getMessage());
            return false;
        }
    }

    /** 读空接收缓冲，每段数据以本节点为源头推向下游 */
    private void receive() {
        SerialPort port = serialPort;
        if (port == null || !port.isOpen()) {
            return;
        }
        while (true) {
            int available = port.bytesAvailable();
            if (available <= 0) {
                return;
            }
            byte[] buffer = new byte[available];
            int read = port.readBytes(buffer, buffer.length);
            if (read <= 0) {
                return;
            }
            byte[] data = read == buffer.length ? buffer : Arrays.copyOf(buffer, read);

            log.info("【SerialPort】{}【{}】 receive data: {}",
                    node.getName(), properties.getDevice(), HexUtil.encodeHexStr(data, false));

            XLineContext context = node.getContext();
            if (context == null) {
                log.warn("【SerialPort】{} 未注入上下文，数据未推送", node.getName());
                return;
            }
            context.pushMsg(node.getName(), data);
        }
    }

    /** 串口被拔掉：先摘掉监听再关闭，避免关闭动作自身触发重连 */
    private void onDisconnected() {
        if (!running) {
            return;
        }
        log.warn("【SerialPort】{}【{}】 串口已断开", node.getName(), properties.getDevice());
        closePort();
        reconnect();
    }

    /** 后台重开：同一时刻只允许一个重开线程 */
    private void reconnect() {
        if (!running || !boolValue(properties.getReconnect(), true)) {
            return;
        }
        synchronized (this) {
            if (reconnecting) {
                return;
            }
            reconnecting = true;
        }
        executor.execute(this::reconnectLoop);
    }

    private void reconnectLoop() {
        try {
            while (running) {
                sleepQuietly(longValue(properties.getReconnectIntervalMillis(), 3000L));
                if (!running || open()) {
                    return;
                }
            }
        } finally {
            reconnecting = false;
        }
    }

    private void closePort() {
        SerialPort port = serialPort;
        if (port == null) {
            return;
        }
        serialPort = null;
        port.removeDataListener();
        if (port.isOpen()) {
            port.closePort();
        }
    }

    /** 线程工厂：优先用节点注入的供给（JDK 21 虚拟线程），缺省用平台线程 */
    private static ThreadFactory threadFactory(SerialPortNode node) {
        return node.getThreadFactory() == null
                ? Executors.defaultThreadFactory()
                : node.getThreadFactory().create("xline-serial-port-" + node.getName());
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int intValue(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static long longValue(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static boolean boolValue(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }
}

package com.csf.xline.core.serialport;

import lombok.Data;

/**
 * 串口节点配置段（{@code xline.nodes[].serial-port}）。
 *
 * <p>串口既可作为数据源头（读到数据推向下游），也可作为数据出口
 * （把上游数据写往串口）：在 struct 中给成对的串口各配一条边，
 * 二者即可无业务代码双向转发。
 *
 * <p>本类只是配置载体，串口的打开与收发由 {@code xline-serial-port} 模块实现，
 * core 不含任何通信依赖。
 */
@Data
public class SerialPortProperties {

    /** 串口设备：Windows 为 {@code COM1}，Linux / macOS 为 {@code /dev/ttyS0} */
    private String device;

    /** 波特率 */
    private Integer baudRate = 115200;

    /** 数据位 */
    private Integer dataBits = 8;

    /** 停止位 */
    private Integer stopBits = 1;

    /** 奇偶校验：0 无、1 奇、2 偶、3 MARK、4 SPACE */
    private Integer parity = 0;

    /** 是否使用 RS485 模式 */
    private Boolean useRs485Mode = Boolean.FALSE;

    /** 打开失败或串口断开（如 USB 拔掉）后是否自动重开 */
    private Boolean reconnect = Boolean.TRUE;

    /** 自动重开间隔（毫秒） */
    private Long reconnectIntervalMillis = 3000L;

    /** 文本转字节的编码：上游推送文本数据时按此编码写出 */
    private String charset = "UTF-8";

    /**
     * 取生效设备名。
     *
     * @return 设备名
     * @throws IllegalStateException 未配置 device
     */
    public String resolvedDevice() {
        if (device == null || device.trim().isEmpty()) {
            throw new IllegalStateException("serial-port 节点必须配置 device");
        }
        return device.trim();
    }
}

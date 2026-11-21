package com.csf.xline.tcp.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * 默认帧解码器：按换行分帧。
 *
 * <p>一帧即以 {@code \n} 结尾的一行（行尾的 {@code \r} 一并去掉），
 * 与 {@code tcp.append-new-line} 配套使用：对端按行发送，本端按行收。
 *
 * <p>单帧有长度上限：对端持续发送却不发换行时，攒满上限即作为一帧交出，
 * 余下字节归入下一帧，避免缓冲无限增长导致内存溢出。
 */
public class LineFrameDecoder implements TcpFrameDecoder {

    /** 默认单帧上限（字节） */
    public static final int DEFAULT_MAX_FRAME_LENGTH = 64 * 1024;

    private final int maxFrameLength;

    public LineFrameDecoder() {
        this(DEFAULT_MAX_FRAME_LENGTH);
    }

    /**
     * @param maxFrameLength 单帧上限（字节），不大于 0 时取默认值
     */
    public LineFrameDecoder(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength > 0 ? maxFrameLength : DEFAULT_MAX_FRAME_LENGTH;
    }

    @Override
    public byte[] decode(PushbackInputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                return buf.toByteArray();
            }
            if (b != '\r') {
                buf.write(b);
                if (buf.size() >= maxFrameLength) {
                    return buf.toByteArray();
                }
            }
        }
        // 对端关闭时还剩半行：交出，避免末尾数据丢失
        return buf.size() > 0 ? buf.toByteArray() : null;
    }
}

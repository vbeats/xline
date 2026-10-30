package com.csf.xline.app.codec;

import com.csf.xline.tcp.codec.TcpFrameDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * 自定义帧解码器 Demo：帧头 {@code 0x0A 0x0B 0x0C 0x0D} 起始，整帧最长 100 字节。
 *
 * <p>帧结束的三种判定（先到先出帧）：
 * <ol>
 *     <li>收到下一个帧头 —— 帧头永远属于下一帧，故把它 {@code unread} 回留；</li>
 *     <li>收满 {@value #MAX_FRAME_LENGTH} 字节 —— 超长即截断，后续字节按下一帧重新找帧头；</li>
 *     <li>空闲超过 {@code tcp.read-timeout-millis} —— 已攒到「帧头 + 至少 1 字节」就出帧，
 *         否则最后一帧要等到连接关闭才吐出。</li>
 * </ol>
 *
 * <p>解出的一帧含帧头，原样推向下游：转发时无需再拼帧头。
 * 本类无状态（帧的中间状态只存在于 {@link #decode(PushbackInputStream)} 的局部变量），
 * 可在全部连接间共享。
 */
@Component
public class HeaderFrameDecoder implements TcpFrameDecoder {

    /** 帧头：0x0A 0x0B 0x0C 0x0D */
    private static final byte[] HEADER = {0x0A, 0x0B, 0x0C, 0x0D};

    /** 数据帧最长长度（含帧头，单位字节） */
    private static final int MAX_FRAME_LENGTH = 100;

    @Override
    public byte[] decode(PushbackInputStream in) throws IOException {
        byte[] frame = new byte[MAX_FRAME_LENGTH];
        int size = 0;
        while (true) {
            int b;
            try {
                b = in.read();
            } catch (SocketTimeoutException e) {
                // 空闲：够一帧就交出，否则继续等下一帧
                if (size > HEADER.length) {
                    return Arrays.copyOf(frame, size);
                }
                continue;
            }
            if (b < 0) {
                // 对端关闭：残帧够一帧就交出
                return size > HEADER.length ? Arrays.copyOf(frame, size) : null;
            }
            frame[size++] = (byte) b;

            if (size <= HEADER.length) {
                // 尚未凑满帧头：不是帧头前缀就丢掉首字节，重新对齐
                while (size > 0 && !isHeaderPrefix(frame, size)) {
                    System.arraycopy(frame, 1, frame, 0, --size);
                }
                continue;
            }
            if (endsWithHeader(frame, size)) {
                int length = size - HEADER.length;
                if (length <= HEADER.length) {
                    // 连续两个帧头：前一帧没有数据，丢弃它并从新帧头继续
                    System.arraycopy(frame, length, frame, 0, HEADER.length);
                    size = HEADER.length;
                    continue;
                }
                // 末尾 4 字节是下一帧的帧头：退回给下一帧
                in.unread(HEADER);
                return Arrays.copyOf(frame, length);
            }
            if (size == MAX_FRAME_LENGTH) {
                return frame;
            }
        }
    }

    /**
     * 判断已收到的 {@code size} 个字节是否为帧头的前缀。
     *
     * @param frame 帧缓冲
     * @param size  已收到字节数
     * @return 是帧头前缀（含已凑满帧头）时返回 {@code true}
     */
    private static boolean isHeaderPrefix(byte[] frame, int size) {
        for (int i = 0; i < size; i++) {
            if (frame[i] != HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断已收到的字节是否以帧头结尾（即下一帧已经开始）。
     *
     * @param frame 帧缓冲
     * @param size  已收到字节数
     * @return 以帧头结尾时返回 {@code true}
     */
    private static boolean endsWithHeader(byte[] frame, int size) {
        if (size < HEADER.length) {
            return false;
        }
        for (int i = 0; i < HEADER.length; i++) {
            if (frame[size - HEADER.length + i] != HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}

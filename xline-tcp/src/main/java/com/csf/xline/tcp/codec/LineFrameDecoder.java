package com.csf.xline.tcp.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * 默认帧解码器：按换行分帧。
 *
 * <p>一帧即以 {@code \n} 结尾的一行（行尾的 {@code \r} 一并去掉），
 * 与 {@code tcp.append-new-line} 配套使用：对端按行发送，本端按行收。
 */
public class LineFrameDecoder implements TcpFrameDecoder {

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
            }
        }
        // 对端关闭时还剩半行：交出，避免末尾数据丢失
        return buf.size() > 0 ? buf.toByteArray() : null;
    }
}

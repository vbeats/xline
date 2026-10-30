package com.csf.xline.tcp.codec;

import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * TCP 帧解码器：决定「怎样才算一帧」。
 *
 * <p>由业务侧实现并注册为 Spring Bean，装配阶段按 Bean 名与节点名匹配注入
 * （容器中只有一个实现时全部节点共用，无任何实现时沿用 {@link LineFrameDecoder}）。
 * 二进制协议（固定帧头 / 定长 / 长度域）用它替掉默认的按行分帧。
 *
 * <p>实例在所有连接间共享，故实现必须无状态：
 * 一帧的中间状态只能存在于 {@link #decode(PushbackInputStream)} 的局部变量里。
 */
public interface TcpFrameDecoder {

    /**
     * 留给下一帧的回退缓冲区大小：解码器最多可 {@code unread} 这么多个字节。
     *
     * <p>典型用法是「解到下一帧的帧头才发现本帧结束」，把帧头退回流里让下一帧从它开始。
     */
    int PUSHBACK_SIZE = 64;

    /**
     * 解出一帧。
     *
     * <p>调用方循环调用本方法，返回一帧就推向下游，返回 {@code null} 即认为对端关闭、结束读取。
     * 中途可能抛出 {@link java.net.SocketTimeoutException}（仅当配置了
     * {@code tcp.read-timeout-millis} 时会出现），两种处理均可：
     * <ul>
     *     <li>解码器内部消化 —— 空闲即把已收到的字节作为一帧返回；</li>
     *     <li>抛给调用方 —— 调用方忽略超时继续读，本帧中间状态会丢失。</li>
     * </ul>
     *
     * @param in 连接输入流（已按 {@code buffer-size} 缓冲，支持回退 {@link #PUSHBACK_SIZE} 字节）
     * @return 一帧字节；{@code null} 表示流已结束
     * @throws IOException 读取失败
     */
    byte[] decode(PushbackInputStream in) throws IOException;
}

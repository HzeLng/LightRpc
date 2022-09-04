package com.practise.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcEncoder
 * @date 2022/3/4 16:06
 */
public class RpcEncoder  extends MessageToByteEncoder {

    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);

    private Class<?> genericClass;

    private Serializer serializer;

    public RpcEncoder(Class<?> genericClass) {
        logger.info("RpcEncoder`s constructor: one args");
        this.genericClass = genericClass;
    }

    public RpcEncoder(Class<?> genericClass, Serializer serializer) {
        logger.info("RpcEncoder`s constructor: two args");
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            if(serializer == null){
                byte[] data = SerializationUtil.serialize(in);
                // 写入此次传输的长度
                out.writeInt(data.length);
                // 再写数据 解码那边也要对应
                out.writeBytes(data);
            }
            else{
                try {
                    byte[] data = serializer.serialize(in);
                    out.writeInt(data.length);
                    out.writeBytes(data);
                } catch (Exception ex) {
                    logger.error("Encode error: " + ex.toString());
                }
            }
        }
    }
}

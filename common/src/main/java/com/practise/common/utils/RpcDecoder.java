package com.practise.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcDecoder
 * @date 2022/3/4 16:00
 */

/**
 * 继承netty类
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);

    private Class<?> genericClass;
    private Serializer serializer;

    public RpcDecoder(Class<?> genericClass) {
        logger.info("RpcDecoder`s constructor, one args");
        this.genericClass = genericClass;
    }

    public RpcDecoder(Class<?> genericClass, Serializer serializer) {
        logger.info("RpcDecoder`s constructor, double args");
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        // 返回当前索引的(无符号) 整型，读索引加4
        int dataLength = in.readInt();
        if (dataLength < 0) {
            ctx.close();
        }
        //把 ByteBuf 里面的数据全部读取到 dst 字节数组中，这里 dst 字节数组的大小通常等于 readableBytes()
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        // 把 ByteBuf 里面的数据全部读取到 data 字节数组中，这里 data 字节数组的大小通常等于 readableBytes()
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        if(serializer == null ){
            Object obj = SerializationUtil.deserialize(data, genericClass);
            out.add(obj);
        }
        else{
            Object obj = null;
            try {
                obj = serializer.deserialize(data, genericClass);
                out.add(obj);
            } catch (Exception ex) {
                logger.error("Decode error: " + ex.toString());
            }
        }


    }
}

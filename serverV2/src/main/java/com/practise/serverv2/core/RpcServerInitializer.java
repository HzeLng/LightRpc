package com.practise.serverv2.core;

import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.practise.common.Serializer.protostuff.ProtostuffSerializer;
import com.practise.common.entity.Beat;
import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import com.practise.common.utils.RpcDecoder;
import com.practise.common.utils.RpcEncoder;
import com.practise.common.utils.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcServerInitializer
 * @date 2022/3/5 20:53
 */
public class RpcServerInitializer  extends ChannelInitializer<SocketChannel> {

    private Map<String, Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // kryo是一个高性能的序列化/反序列化工具
        Serializer serializer = ProtostuffSerializer.class.newInstance();
//        Serializer serializer = HessianSerializer.class.newInstance();
//        Serializer serializer = KryoSerializer.class.newInstance();
        ChannelPipeline cp = socketChannel.pipeline();
        // 心跳机制，心跳间隔
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcRequest.class, serializer));
        cp.addLast(new RpcEncoder(RpcResponse.class, serializer));
        cp.addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));
    }
}

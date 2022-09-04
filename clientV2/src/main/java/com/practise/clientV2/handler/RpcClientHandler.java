package com.practise.clientV2.handler;

import com.practise.clientV2.connect.ConnectionManager;
import com.practise.common.entity.Beat;
import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import com.practise.common.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcClientHandler
 * @date 2022/3/5 22:41
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<String, RpcFuture>();
    private volatile Channel channel;
    private SocketAddress remotePeer;
    private RpcProtocol rpcProtocol;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        logger.info("RpcClientHandler-channelRead0: use the ctx is {} and the channel is {}",ctx,ctx.channel());
        logger.info("RpcClientHandler-channelRead0: ctx name {}",ctx.name());
        logger.debug("Receive response: " + requestId);

        RpcFuture rpcFuture = pendingRPC.get(requestId);
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);
            rpcFuture.done(response);
        } else {
            logger.warn("Can not get pending response for request id: " + requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception: " + cause.getMessage());
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 客户端在外部使用时，通过调用chooseHandler拿到之前保存好的连接
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        logger.info("RpcClientHandler-sendRequest: the channel is {}",channel);
        logger.info("RpcClientHandler-sendRequest: use the channel is {} to sendRequest ",channel);
        logger.info("RpcClientHandler-sendRequest: ready to send RpcRequest {}",request.getRequestId());
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRPC.put(request.getRequestId(), rpcFuture);
        try {
            // 同步等待消息发出
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                logger.error("Send request {} error", request.getRequestId());
            }
        } catch (InterruptedException e) {
            logger.error("Send request exception: " + e.getMessage());
        }

        return rpcFuture;
    }

    /**
     * 客户端发送心跳包
     * 触发器还得多看看
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            //Send ping
            sendRequest(Beat.BEAT_PING);
            logger.debug("Client send beat-ping to " + remotePeer);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    /**
     *      * 什么时候会触发handler的channelInactive：
     *      *      1. 客户端发送关闭帧
     *      *      2. 客户端结束进程
     *      *      3. 服务端主动调用channel.close()
     *      * 在这里应该是第三种情况，所以不用在客户端选择handler.close()
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcProtocol);
    }
}

package com.practise.common.entity;

/**
 * @author HzeLng
 * @version 1.0
 * @description Beat
 * @date 2022/3/5 21:12
 */
public final class Beat {

    public static final int BEAT_INTERVAL = 30;
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;
    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest() {};
        BEAT_PING.setRequestId(BEAT_ID);
    }

}
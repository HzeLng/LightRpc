package com.practise.serverv2.core;

/**
 * @author HzeLng
 * @version 1.0
 * @description Server
 * @date 2022/3/5 17:25
 */
public abstract class Server {

    /**
     * start server
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * stop server
     * @throws Exception
     */
    public abstract void stop() throws Exception;

}

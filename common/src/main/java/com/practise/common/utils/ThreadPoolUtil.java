package com.practise.common.utils;

import java.util.concurrent.*;

/**
 * @author HzeLng
 * @version 1.0
 * @description ThreadPoolUtil
 * @date 2022/3/5 20:18
 */
public class ThreadPoolUtil {

    public static ThreadPoolExecutor makeServerThreadPool(final String serviceName, int corePoolSize, int maxPoolSize) {
        ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        // 规定线程工厂，创建线程的统一ID格式
                        return new Thread(r, "HzeLng-netty-rpc-" + serviceName + "-" + r.hashCode());
                    }
                },
                // 拒绝策略
                new ThreadPoolExecutor.AbortPolicy());
        return serverHandlerPool;
    }

}

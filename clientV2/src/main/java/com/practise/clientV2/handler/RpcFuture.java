package com.practise.clientV2.handler;

import com.practise.clientV2.RpcClient;
import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author HzeLng
 * @version 1.0
 * @description RpcFuture
 * @date 2022/3/5 22:42
 */
public class RpcFuture implements Future<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    private long responseTimeThreshold = 5000;
    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() {
        sync.acquire(1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    /**
     * 获取结果
     * 如果还没有结果，就阻塞等待，使用的自实现的内部类sync 仿照可重入锁的sync写法
     * 可不可中断？
     *          假如可中断，那就是阻塞期间，被中断了，醒过来，补一次中断
     *          假如不可中断，那就阻塞期间，被中断了，抛出异常，做出对应的处理即可
     * 而还是看是否调用了对应的（可中断/不可中断）方法
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    /**
     * 结果到了，释放锁，唤醒因get被阻塞的线程
     * 准备执行回调函数（任务清单）
     * （执行任务清单，会上锁，因为是共享队列，防止执行任务的时候，还有人往里加）
     * @param reponse
     */
    public void done(RpcResponse reponse) {
        this.response = reponse;
        sync.release(1);
        // 为什么还要这一步？
        // 调用回调函数
        invokeCallbacks();
        // Threshold
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            logger.warn("Service response time is too slow. Request id = " + reponse.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    /**
     * 执行回调函数
     * 会上锁，对回调函数任务列表上锁
     * 防止在执行回调函数时，有新的任务加入
     * 使用的是可重入锁
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final AsyncRPCCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 假如在添加的时候，结果已经到了
     * 那就直接执行回调函数，不用加入到队列清单了
     * 也不会出现，结果都到了，任务清单也做完了， 过一会儿又有线程往清单里加任务，但没有触发执行任务清单的方法和时机了
     * @param callback
     * @return
     */
    public RpcFuture addCallback(AsyncRPCCallback callback) {
        lock.lock();
        try {
            // 添加回调函数的时候，直接就检查是否已经有response了
            // 如果有，就直接执行回调函数
            if (isDone()) {
                runCallback(callback);
            } else {
                // 如果没有再加入列表中，等候，等待response到来
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    /**
     * 根据传进的回调函数，执行
     * @param callback
     */
    private void runCallback(final AsyncRPCCallback callback) {
        final RpcResponse res = this.response;
        RpcClient.submit(new Runnable() {
            @Override
            public void run() {
                // 只有在response到来后，才会执行回调函数
                // 判断response是否出错
                if (res.getError() == null) {
                    // 如果没有，则执行，用户在一开始设置的
                    // 没错的情况下，做什么
                    callback.success(res.getResult());
                } else {
                    callback.fail(new RuntimeException("Response error", new Throwable(res.getError())));
                }
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }
}

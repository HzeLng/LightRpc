package com.practise.rpc1.config;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServiceRegistry
 * @date 2022/3/4 15:15
 */
@Component
public class ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    /**
     * 用来
     */
    private CountDownLatch latch = new CountDownLatch(1);

    private String registryAddress;

    public ServiceRegistry() {
        LOGGER.info("server: ServiceRegistry`s constructor ");
        registryAddress = "127.0.0.1:2181";
       LOGGER.info("server: the registryAddress is {}", registryAddress);
    }
//
//    public ServiceRegistry(String registryAddress) {
//        this.registryAddress = registryAddress;
//    }

    public void registry(String data){
        if(data != null){
            ZooKeeper zk = connectServer();
            if(zk != null){
                creatNode(zk, data);
            }
            else{
                LOGGER.error("can`t connect to server ");
            }
        }
        else{
            LOGGER.error("null data");
        }
    }

    /**
     * 连接到zookeeper服务器
     * 用来发布服务的
     * @return
     */
    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try{
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                        latch.countDown();
                    }
                }
            });
            /**
             * 在这里等process的初始化完，才继续下一步，否则zk有可能返回为null
             */
            latch.await();
        }
        catch (IOException  | InterruptedException e){
            LOGGER.error("",e);
        }
        // 连接到zk服务器成功，返回可操作性的zk客户端
        return zk;
    }

    /**
     * 在zk服务器上创建结点，其实就是存储内容
     * 存储什么内容，提供服务的服务本身IP地址端口
     * @param zk
     * @param data
     */
    private void creatNode(ZooKeeper zk, String data){
        try{
            byte[] bytes = data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node ({} => {})", path, data);
        }catch (KeeperException | InterruptedException e){
            LOGGER.error("error in creating node",e);
        }
    }
}

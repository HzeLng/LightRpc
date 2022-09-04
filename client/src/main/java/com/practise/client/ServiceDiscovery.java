package com.practise.client;

import com.practise.common.entity.Constant;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServiceDiscovery
 * @date 2022/3/4 16:46
 */

/**
 * 使用 ZooKeeper 实现服务发现功能
 * 找到提供服务的主机所在地址
 */
@Component
public class ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private final String registryAddress = "127.0.0.1:2181";

    public ServiceDiscovery() {
        LOGGER.info("client: ServiceDiscovery`s constructor");
        ZooKeeper zk = connectServer();
        if(zk != null){
            watchNode(zk);
        }
        else{
            LOGGER.error("空的zk结点");
        }
    }

    /**
     *
     * @return
     */
    public String discover() {
        String data = null;
        int size = dataList.size();
        if (size > 0) {
            // 如果只有一个服务器提供，直接返回这个服务器所在的地址就可以了
            if (size == 1) {
                data = dataList.get(0);
                LOGGER.debug("using only data: {}", data);
            } else {
                // 如果有多个服务器提供，那就随机一个
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.debug("using random data: {}", data);
            }
        }
        return data;
    }

    /**
     * 连接到zk服务器
     * 去上面拿远程服务的主机所在地址
     * @return
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("", e);
        }
        LOGGER.info("client:connected to the zk server");
        return zk;
    }

    /**
     * 监控zk服务器上对应节点的信息
     *
     * @param zk
     */
    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    // 如果
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(zk);
                    }
                }
            });
            // 如果结点信息变了，那就从新获取数据
            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            LOGGER.debug("client: node data: {}", dataList);
            this.dataList = dataList;
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("", e);
        }
    }
}

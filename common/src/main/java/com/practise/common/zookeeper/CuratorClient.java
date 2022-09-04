package com.practise.common.zookeeper;

import com.practise.common.entity.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author HzeLng
 * @version 1.0
 * @description CuratorClient
 * @date 2022/3/5 17:31
 *
 * 类似redisTemplate的更方便的操作zookeeper
 */
public class CuratorClient {

    private static final Logger logger = LoggerFactory.getLogger(CuratorClient.class);

    /**
     * Curator Framework提供了简化使用zookeeper更高级的API接口
     * 它包涵很多优秀的特性，主要包括以下三点：
     *          1. 自动连接管理：自动处理zookeeper的连接和重试存在一些潜在的问题；可以watch NodeDataChanged event和获取updateServerList;Watches可以自动被Cruator recipes删除；
     *          2. 更干净的API：简化raw zookeeper方法，事件等；提供现代流式API接口
     *          3. Recipe实现：leader选举，分布式锁，path缓存，和watcher,分布式队列等。
     */
    private CuratorFramework client;

    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectionTimeout) {
        client = CuratorFrameworkFactory.builder().namespace(namespace).connectString(connectString)
                .sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .build();
        client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, Constant.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, Constant.ZK_NAMESPACE, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getClient() {
        return client;
    }

    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    public String createPathData(String path, byte[] data) throws Exception {
        // EPHEMERAL（临时的）类型的目录节点不能有子节点目录
        // Ephemeral 节点，在创建它的客户端与服务器间的 Session 结束时自动被删除。
        // 服务器重启会导致 Session 结束，因此 Ephemeral 类型的 znode 此时也会自动删除
        return client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }

    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }

    public void watchNode(String path, Watcher watcher) throws Exception {
        client.getData().usingWatcher(watcher).forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    public void watchTreeNode(String path, TreeCacheListener listener) {
        // TreeCache Deprecated.replace by CuratorCache
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener(listener);
    }
//    public void watchTreeNode(String path, TreeCacheListener listener){
//    }

    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        //BUILD_INITIAL_CACHE 代表使用同步的方式进行缓存初始化。
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    public void close() {
        client.close();
    }


}

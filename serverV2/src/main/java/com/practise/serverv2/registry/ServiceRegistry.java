package com.practise.serverv2.registry;

import com.practise.common.entity.Constant;
import com.practise.common.protocol.RpcProtocol;
import com.practise.common.protocol.RpcServiceInfo;
import com.practise.common.utils.ServiceUtil;
import com.practise.common.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServiceRegistry
 * @date 2022/3/5 17:28
 */
public class ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CuratorClient curatorClient;
    /**
     *
     */
    private List<String> pathList = new ArrayList<>();

    public ServiceRegistry(String registryAddress) {
        // String registryAddress = "127.0.0.1:2181";
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    /**
     * 注册服务
     * 将本服务端向外界提供的所有服务进行打包处理
     * 几个注意点：
     * zk上path的路径名——封装了host和服务列表信息的RpcProtocol对象的hashcode 为什么，短，唯一
     * 存储的数据，则是这个对象转为json再转为bytes的bytes数据
     *
     * 在zk上创建结点后，注册该节点的连接状态监听器，用来重连。
     * @param host
     * @param port
     * @param serviceMap
     */
    public void registerService(String host, int port, Map<String, Object> serviceMap) {
        // Register service info
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key : serviceMap.keySet()) {
            // 这里的key 是 serviceName#version
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            // 如果数组不为空
            if (serviceInfo.length > 0) {
                // 用自定义实体类 封装提供的服务信息
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                logger.info("Register new service: {} ", key);
                serviceInfoList.add(rpcServiceInfo);
            } else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            // 在前面通过扫描得到本服务器可以提供的所有服务后
            // 再用一个实体类 封装起来
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            // 用json数据进行保存这些服务数据
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            // path为这个
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            path = this.curatorClient.createPathData(path, bytes);
            pathList.add(path);
            logger.info("ServiceRegistry-registerService: Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
            logger.info("ServiceRegistry-registerService: the created path: {}",path);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }

        // 注册连接状态监听器
        curatorClient.addConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.info("ServiceRegistry-stateChanged: Connection state: {}, register service after reconnected", connectionState);
                    registerService(host, port, serviceMap);
                }
            }
        });
    }

    /**
     * 服务端下线，选择取消注册服务
     */
    public void unregisterService() {
        logger.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }
}

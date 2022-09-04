package com.practise.clientV2.route;

import com.practise.clientV2.handler.RpcClientHandler;
import com.practise.common.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcLoadBalanceRandom
 * @date 2022/3/5 23:04
 *
 *
 */
public class RpcLoadBalanceRandom extends RpcLoadBalance {
    private Random random = new Random();

    public RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        // Random
        return addressList.get(random.nextInt(size));
    }

    /**
     * 根据serviceKey，也就是当前想要使用的服务，得到对应节点下的数据
     * 根据结点的数据列表 随机选择一个服务器进行远程调用
     * @param serviceKey
     * @param connectedServerNodes
     * @return
     * @throws Exception
     */
    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}

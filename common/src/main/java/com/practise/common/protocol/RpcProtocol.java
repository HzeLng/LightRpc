package com.practise.common.protocol;

import com.practise.common.utils.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcProtocol
 * @date 2022/3/5 20:15
 */
public class RpcProtocol implements Serializable {

    private static final long serialVersionUID = -1102180003395190700L;
    // service host
    private String host;
    // service port
    private int port;
    // service info list
    private List<RpcServiceInfo> serviceInfoList;

    public String toJson() {
        String json = JsonUtil.objectToJson(this);
        return json;
    }

    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getServiceInfoList() {
        return serviceInfoList;
    }

    public void setServiceInfoList(List<RpcServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol that = (RpcProtocol) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(serviceInfoList, that.serviceInfoList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceInfoList);
    }


}

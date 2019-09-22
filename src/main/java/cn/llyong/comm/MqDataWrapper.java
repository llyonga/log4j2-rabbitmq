package cn.llyong.comm;


import com.rabbitmq.client.Channel;

import java.io.Serializable;

/**
 * @description:
 * @author: llyong
 * @date: 2019/9/21
 * @time: 21:13
 * @version: 1.0
 */
public class MqDataWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    private String data;
    private Channel channel;
    private String exchange;
    private String routingKey;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}

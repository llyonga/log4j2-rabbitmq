package cn.llyong.disruptor;

import cn.llyong.comm.MqDataWrapper;
import com.lmax.disruptor.RingBuffer;
import com.rabbitmq.client.Channel;

/**
 * Created with IntelliJ IDEA.
 *
 * @description:
 * @author: lvyong
 * @date: 2019-09-05
 * @time: 4:24 下午
 * @version: 1.0
 */
public class MessageProducer {

    private String producerId;
    private RingBuffer<MqDataWrapper> ringBuffer;

    public MessageProducer(String producerId, RingBuffer<MqDataWrapper> ringBuffer) {
        this.producerId = producerId;
        this.ringBuffer = ringBuffer;
    }

    public void onData(String logMsg, Channel channel, String exchange, String routingKey) {
        long sequence = ringBuffer.next();
        MqDataWrapper wrapper = ringBuffer.get(sequence);
        wrapper.setData(logMsg);
        wrapper.setChannel(channel);
        wrapper.setExchange(exchange);
        wrapper.setRoutingKey(routingKey);
        ringBuffer.publish(sequence);
    }
}

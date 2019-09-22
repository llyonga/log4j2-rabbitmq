package cn.llyong.rabbitmq;

import cn.llyong.comm.MqDataWrapper;
import cn.llyong.disruptor.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description:
 * @author: llyong
 * @date: 2019/9/21
 * @time: 22:34
 * @version: 1.0
 */
public class RabbitmqConsumer extends MessageConsumer {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public RabbitmqConsumer(String consumerId) {
        super(consumerId);
    }

    @Override
    public void onEvent(MqDataWrapper wrapper) throws Exception {
        try {
            System.out.println(wrapper.getData());
            //发送消息
            wrapper.getChannel().basicPublish(wrapper.getExchange(), wrapper.getRoutingKey(), null, wrapper.getData().getBytes("UTF-8"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        System.out.println("消息已发送！");
    }
}

package cn.llyong.disruptor;

import cn.llyong.comm.MqDataWrapper;
import com.lmax.disruptor.WorkHandler;

/**
 * Created with IntelliJ IDEA.
 *
 * @description:
 * @author: lvyong
 * @date: 2019-09-05
 * @time: 4:35 下午
 * @version: 1.0
 */
public abstract class MessageConsumer implements WorkHandler<MqDataWrapper> {

    private String consumerId;

    public MessageConsumer(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}

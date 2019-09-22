package cn.llyong.disruptor;

import cn.llyong.comm.MqDataWrapper;
import com.lmax.disruptor.ExceptionHandler;

/**
 * Created with IntelliJ IDEA.
 *
 * @description:
 * @author: lvyong
 * @date: 2019-09-05
 * @time: 4:49 下午
 * @version: 1.0
 */
public class EventExceptionHandler implements ExceptionHandler<MqDataWrapper> {

    @Override
    public void handleEventException(Throwable ex, long sequence, MqDataWrapper event) {
    }

    @Override
    public void handleOnStartException(Throwable ex) {
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
    }
}

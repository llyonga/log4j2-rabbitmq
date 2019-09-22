package cn.llyong.disruptor;

import cn.llyong.comm.MqDataWrapper;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @description:
 * @author: lvyong
 * @date: 2019-09-05
 * @time: 4:32 下午
 * @version: 1.0
 */
public class RingBufferWorkerPoolFactory {

    private static class SingletonHolder {
        static final RingBufferWorkerPoolFactory INSTANCE = new RingBufferWorkerPoolFactory();
    }

    private RingBufferWorkerPoolFactory() {

    }

    public static RingBufferWorkerPoolFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private Map<String, MessageProducer> producers = new ConcurrentHashMap<>();

    private Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();

    private ExecutorService executorService = new ThreadPoolExecutor(
            10,
            15,
            10L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024));

    private RingBuffer<MqDataWrapper> ringBuffer;

    private SequenceBarrier sequenceBarrier;

    private WorkerPool<MqDataWrapper> workerPool;

    public void initAndStart(ProducerType type, int bufferSize, WaitStrategy waitStrategy, MessageConsumer[] messageConsumers) {

        //1、构建RingBuffer对象
        this.ringBuffer = RingBuffer.create(
                type,
                new EventFactory<MqDataWrapper>() {
                    @Override
                    public MqDataWrapper newInstance() {
                        return new MqDataWrapper();
                    }
                },
                bufferSize,
                waitStrategy
        );
        //2、设置序号栅栏
        this.sequenceBarrier = this.ringBuffer.newBarrier();
        //3、设置工作池

        this.workerPool = new WorkerPool<MqDataWrapper>(
                this.ringBuffer,
                this.sequenceBarrier,
                new EventExceptionHandler(),
                messageConsumers
        );
        //4、把构建的消费者加入导池中
        for (MessageConsumer consumer : messageConsumers) {
            this.consumers.put(consumer.getConsumerId(), consumer);
        }
        //5、添加我们的sequences
        this.ringBuffer.addGatingSequences(this.workerPool.getWorkerSequences());

        //6、启动我们的工作池
        this.workerPool.start(executorService);
    }

    public MessageProducer getMessageProducer(String producerId) {
        MessageProducer messageProducer = this.producers.get(producerId);
        if(null == messageProducer) {
            messageProducer = new MessageProducer(producerId, this.ringBuffer);
            this.producers.put(producerId, messageProducer);
        }
        return messageProducer;
    }

}

package cn.llyong.log.appender;

import cn.llyong.comm.MqDataWrapper;
import cn.llyong.disruptor.MessageConsumer;
import cn.llyong.disruptor.MessageProducer;
import cn.llyong.disruptor.RingBufferWorkerPoolFactory;
import cn.llyong.rabbitmq.RabbitmqConsumer;
import com.alibaba.fastjson.JSON;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * @description:
 * @author: llyong
 * @date: 2019/9/21
 * @time: 15:38
 * @version: 1.0
 */
@Plugin(name = "RabbitmqAppender", category = "Core", elementType = "appender", printObject = true)
public class RabbitmqAppender extends AbstractAppender {

    private String username = "guest";
    private String password = "guest";
    private String host = "localhost";
    private int port = 5762;
    private String virtualHost = "/";
    private String exchange = "amqp-exchange";
    private String type = "direct";
    private boolean durable = false;
    private String queue = "amqp-queue";
    private String routingKey = "";

    private MqDataWrapper mqData;

    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection = null;
    private Channel channel = null;

    public RabbitmqAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions,
                            String host, int port, String username, String password, String virtualHost, String exchange,
                            String queue, String type, boolean durable, String routingKey) {
        super(name, filter, layout, ignoreExceptions, null);

        //初始化属性值
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        this.virtualHost = virtualHost;
        this.exchange = exchange;
        this.queue = queue;
        this.type = type;
        this.durable = durable;
        this.routingKey = routingKey;
        //创建连接
        this.connect();
        initRabbitmqConsumer();
    }

    @PluginFactory
    public static RabbitmqAppender createAppender(@PluginElement("Filter") Filter filter,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                  @PluginAttribute("name") String name,
                                                  @PluginAttribute("host") String host,
                                                  @PluginAttribute("port") int port,
                                                  @PluginAttribute("username") String username,
                                                  @PluginAttribute("password") String password,
                                                  @PluginAttribute("virtualHost") String virtualHost,
                                                  @PluginAttribute("exchange") String exchange,
                                                  @PluginAttribute("queue") String queue,
                                                  @PluginAttribute("type") String type,
                                                  @PluginAttribute("durable") boolean durable,
                                                  @PluginAttribute("routingKey") String routingKey) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        if (host == null) {
            host = "localhost";
        }
        if (username == null) {
            username = "guest";
        }
        if (password == null) {
            password = "guest";
        }
        if (virtualHost == null) {
            virtualHost = "/";
        }
        if (exchange == null) {
            exchange = "amqp-exchange";
        }
        if (queue == null) {
            queue = "amqp-queue";
        }
        if (type == null) {
            type = "direct";
        }
        if (routingKey == null) {
            routingKey = "";
        }
        return new RabbitmqAppender(name, filter, layout, ignoreExceptions, host, port, username, password, virtualHost, exchange, queue, type, durable, routingKey);
    }

    @Override
    public void append(LogEvent event) {
        Properties properties = System.getProperties();
        Map<String, Object> map = new HashMap<>(16);
        map.put("dcn", properties.getProperty("dcn"));
        map.put("timeStamp", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeMillis()), ZoneId.systemDefault())));
        map.put("level", event.getLevel().toString());
        map.put("threadName", event.getThreadName());
        map.put("className", event.getLoggerName());
        map.put("method", event.getSource().getMethodName());
        map.put("lineNumber", event.getSource().getLineNumber());
        map.put("stackTraceElement", event.getSource());
        map.put("message", event.getMessage());
        map.put("throwable", event.getThrown());
        map.put("throwable2", event.getThrown() == null ? null : event.getThrown().fillInStackTrace());
        String logMessage = JSON.toJSONString(map);

        //自已的应用服务应该有一个ID生成规则
        String producerId = UUID.randomUUID().toString();
        MessageProducer messageProducer = RingBufferWorkerPoolFactory.getInstance().getMessageProducer(producerId);
        messageProducer.onData(logMessage, channel, exchange, routingKey);
    }

    void connect() {
        try {
            //creating connection
            this.createConnection();
            //creating channel
            this.createChannel();
            //create exchange
            this.createExchange();
            //create queue
            this.createQueue();
        } catch (Exception e) {
            this.getHandler().error(e.getMessage(), e);
        }
    }

    /**
     * 配置连接属性
     */
    private void setFactoryConfiguration() {
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setUsername(this.username);
        factory.setPassword(this.password);
        factory.setVirtualHost(this.virtualHost);
    }

    /**
     * 创建一个connection
     * @return
     * @throws IOException
     */
    private Connection createConnection() throws Exception {
        setFactoryConfiguration();
        if (this.connection == null || !this.connection.isOpen()) {
            this.connection = factory.newConnection();
        }
        return this.connection;
    }

    /**
     * 创建一个Channel
     * @return
     * @throws IOException
     */
    private Channel createChannel() throws IOException {
        boolean b = this.channel == null || !this.channel.isOpen() && (this.connection != null && this.connection.isOpen());
        if (b) {
            this.channel = this.connection.createChannel();
        }
        return this.channel;
    }

    /**
     * 创建一个exchange
     * @throws IOException
     */
    private void createExchange() throws IOException {
        if (this.channel != null && this.channel.isOpen()) {
            synchronized (this.channel) {
                this.channel.exchangeDeclare(this.exchange, this.type, this.durable);
            }
        }
    }

    /**
     * 创建一个queue
     * @throws IOException
     */
    private void createQueue() throws IOException {
        if (this.channel != null && this.channel.isOpen()) {
            synchronized (this.channel) {
                this.channel.queueDeclare(this.queue, this.durable, false, false, null);
                this.channel.queueBind(this.queue, this.exchange, this.routingKey);
            }
        }
    }

    public void close() {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                this.getHandler().error(e.getMessage(), e);
            }
        }
        if (connection != null && connection.isOpen()) {
            try {
                this.connection.close();
            } catch (IOException ioe) {
                this.getHandler().error(ioe.getMessage(), ioe);
            }
        }
    }

    void initRabbitmqConsumer() {
        MessageConsumer[] consumers = new MessageConsumer[12];
        MessageConsumer messageConsumer;
        int length = consumers.length;
        for (int i = 0; i < length ; i++) {
            messageConsumer = new RabbitmqConsumer(UUID.randomUUID().toString());
            consumers[i] = messageConsumer;
        }

        RingBufferWorkerPoolFactory.getInstance().initAndStart(
                ProducerType.MULTI,
                1024*1024,
                new YieldingWaitStrategy(),
                consumers);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}



package com.travel_system.backend_app.config;

import com.google.api.client.util.Value;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.Topic;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_NOTIFICATION_NAME = "queue_notification";

    public static final String EXCHANGE_NOTIFICATION_NAME = "push_distance_notification";
    public static final String EXCHANGE_GPS_NAME = "tg_gps_exchange";

    public static final String NOTIFICATION_ROUTE_KEY = "notification_distance";
    // definir: gps.<id_cidade>.<id_linha>.<id_veiculo>
    public static final String GPS_ROUTE_KEY = "";

    TopicExchange amqTopic = new TopicExchange("amq.topic");

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NOTIFICATION_NAME, true);
    }

    @Bean
    public TopicExchange exchangeNotification() {
        return new TopicExchange(EXCHANGE_NOTIFICATION_NAME);
    }

    @Bean
    public TopicExchange exchangeGps() {
        return new TopicExchange(EXCHANGE_GPS_NAME);
    }

    // faz o mqtt olhar para a exchange custom e não para a padrão amqTopic
    @Bean
    public Binding bindGpsExchangeToTopicExchange() {
        return BindingBuilder.bind(amqTopic).to(exchangeGps()).with("#");
    }


    @Bean
    public Binding bindingNotification(Queue queue, TopicExchange exchangeNotification) {
        return BindingBuilder.bind(queue).to(exchangeNotification).with(NOTIFICATION_ROUTE_KEY);
    }

    // serialização
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

}

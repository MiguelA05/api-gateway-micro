package com.uniquindio.archmicroserv.apigateway.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Objects;

@Configuration
@ConditionalOnProperty(
	prefix = "spring.rabbitmq",
	name = "host",
	matchIfMissing = false
)
@Profile("!test")
public class RabbitMQConfig {

    public static final String DOMINIO_EVENTS_EXCHANGE = "dominio.events";

    @Bean
    public TopicExchange dominioEventsExchange() {
        return new TopicExchange(DOMINIO_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(Objects.requireNonNull(connectionFactory, "ConnectionFactory must not be null"));
        template.setMessageConverter(Objects.requireNonNull(jsonMessageConverter(), "MessageConverter must not be null"));
        return template;
    }
}


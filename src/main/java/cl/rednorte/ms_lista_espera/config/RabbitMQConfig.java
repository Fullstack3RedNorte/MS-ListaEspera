package cl.rednorte.ms_lista_espera.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nombres de colas
    public static final String NOTIFICACIONES_QUEUE = "notificaciones.queue";
    public static final String MEDICOS_QUEUE = "medicos.queue";
    public static final String HORAS_QUEUE = "horas.queue";

    // Nombres de exchanges
    public static final String EXCHANGE = "rednorte.exchange";

    // Routing keys
    public static final String ROUTING_KEY_NOTIFICACIONES = "notificaciones.routing.key";
    public static final String ROUTING_KEY_HORAS = "horas.routing.key";

    // Declarar colas
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_QUEUE).build();
    }

    @Bean
    public Queue medicosQueue() {
        return QueueBuilder.durable(MEDICOS_QUEUE).build();
    }

    @Bean
    public Queue horasQueue() {
        return QueueBuilder.durable(HORAS_QUEUE).build();
    }

    // declarar exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Binding colas con exchange
    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder
                .bind(notificacionesQueue())
                .to(exchange())
                .with(ROUTING_KEY_NOTIFICACIONES);
    }

    @Bean
    public Binding bindingHoras() {
        return BindingBuilder
                .bind(horasQueue())
                .to(exchange())
                .with(ROUTING_KEY_HORAS);
    }

    //convertidor de mensajes a JSON
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // rabbitTemplate con convertidor JSON
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
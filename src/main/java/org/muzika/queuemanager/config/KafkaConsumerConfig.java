package org.muzika.queuemanager.config;




import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.muzika.queuemanager.kafkaMassages.UserCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<UUID, LoadedSong> loadedSongConsumerFactory() {
        Map<String, Object> props =FactoryConfig();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LoadedSong.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<UUID, RequestSlskdSong> songConsumerFactory() {
        Map<String, Object> props =FactoryConfig();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RequestSlskdSong.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, UserCreatedEvent> userCreatedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "queue-manager-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserCreatedEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.muzika.queuemanager.kafkaMassages,org.muzika.authorizationmanager.kafkaMessages");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        log.info("Configuring user-created consumer factory with bootstrap servers: {}, groupId: queue-manager-group", bootstrapServers);
        log.info("Trusted packages: org.muzika.queuemanager.kafkaMassages,org.muzika.authorizationmanager.kafkaMessages");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private Map<String, Object> FactoryConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group-id");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, LoadedSong> loadedSongListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID,LoadedSong> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(loadedSongConsumerFactory());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, RequestSlskdSong> songConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, RequestSlskdSong> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(songConsumerFactory());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent> userCreatedListenerContainerFactory() {
        log.info("Creating user-created listener container factory");
        ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userCreatedConsumerFactory());
        
        // Add error handler to log deserialization and other errors
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.error("Failed to process user-created event. Delivery attempt: {}, Record: {}, Error: {}", 
                    deliveryAttempt, record, ex.getMessage(), ex);
        });
        factory.setCommonErrorHandler(errorHandler);
        
        log.info("User-created listener container factory created successfully");
        return factory;
    }



}

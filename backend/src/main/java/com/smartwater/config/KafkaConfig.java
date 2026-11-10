package com.smartwater.config;

import com.smartwater.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {
    private static final int PARTITIONS = 3;

    @Value("${app.kafka.replicas:1}")
    private int replicas;                                  // use 3 on a real multi-broker cluster (e.g. MSK)

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(replicas).build();
    }

    @Bean NewTopic emailTopic()     { return topic(Topics.EMAIL); }
    @Bean NewTopic usageTopic()     { return topic(Topics.USAGE_INGESTED); }
    @Bean NewTopic emailDlt()       { return topic(Topics.EMAIL + ".DLT"); }
    @Bean NewTopic usageDlt()       { return topic(Topics.USAGE_INGESTED + ".DLT"); }

    /** Retry a failing message 3 times, 2 seconds apart, then publish it to "<topic>.DLT". */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(2000L, 3L));
    }
}

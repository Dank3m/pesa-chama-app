package com.tablebanking.loanmanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.contribution-events:contribution-events}")
    private String contributionEventsTopic;

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Value("${app.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Value("${app.kafka.topics.member-registration:member-registration-events}")
    private String memberRegistrationEventsTopic;

    @Bean
    public NewTopic contributionEventsTopic() {
        return TopicBuilder.name(contributionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(loanEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(paymentEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(notificationEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memberRegistrationEventsTopic() {
        return TopicBuilder.name(memberRegistrationEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

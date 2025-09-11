package co.com.bancolombia.sqs.config;

import co.com.bancolombia.model.notifications.gateways.DecisionPublisher;
import co.com.bancolombia.sqs.SqsDecisionPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


@Configuration
public class SqsConfig {

    @Bean
    SqsAsyncClient sqsAsyncClient(@Value("${aws.region:us-east-1}") String region) {
        return SqsAsyncClient.builder().region(Region.of(region)).build();
    }

    @Bean
    DecisionPublisher decisionPublisher(SqsAsyncClient sqs, ObjectMapper om) {
        return new SqsDecisionPublisher(sqs, om);
    }
}
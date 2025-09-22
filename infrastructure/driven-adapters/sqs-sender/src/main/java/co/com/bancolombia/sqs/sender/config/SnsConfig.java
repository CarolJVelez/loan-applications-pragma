package co.com.bancolombia.sqs.sender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

@Configuration
public class SnsConfig {

    @Bean
    public SnsAsyncClient snsAsyncClient(
            @Value("${aws.region:us-east-1}") String region) {
        return SnsAsyncClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create())
                .build();
    }

}

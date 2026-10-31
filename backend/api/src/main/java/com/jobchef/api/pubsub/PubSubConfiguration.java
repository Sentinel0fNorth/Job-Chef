package com.jobchef.api.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfiguration {
    @Bean(destroyMethod = "shutdown")
    Publisher jobChefPublisher(
            @Value("${jobchef.gcp-project-id}") String projectId,
            @Value("${jobchef.pubsub-topic}") String topicId,
            @Value("${jobchef.pubsub-emulator-host:}") String emulatorHost) throws IOException {
        Publisher.Builder builder = Publisher.newBuilder(TopicName.of(projectId, topicId));
        if (!emulatorHost.isBlank()) {
            var channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build();
            builder.setChannelProvider(FixedTransportChannelProvider.create(
                    GrpcTransportChannel.create(channel)));
            builder.setCredentialsProvider(NoCredentialsProvider.create());
        }
        return builder.build();
    }
}

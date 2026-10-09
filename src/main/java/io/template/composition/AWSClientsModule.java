package io.template.composition;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.template.environment.models.EnvironmentVariables;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class AWSClientsModule extends AbstractModule {

    @Provides
    @Singleton
    DynamoDbClient provideDynamoDBClient(EnvironmentVariables environmentVariables) {
        return DynamoDbClient.builder()
                .region(environmentVariables.awsRegion())
                .build();
    }

    public static void closeResources(Injector injector) {
        injector.getInstance(DynamoDbClient.class).close();
    }
}

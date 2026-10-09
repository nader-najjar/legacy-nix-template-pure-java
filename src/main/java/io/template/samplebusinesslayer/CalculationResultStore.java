package io.template.samplebusinesslayer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import io.template.environment.models.EnvironmentVariables;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Saves calculation results to DynamoDB.
 */
public class CalculationResultStore {

    private final DynamoDbClient dynamoDBClient;
    private final String tableName;

    @Inject
    public CalculationResultStore(DynamoDbClient dynamoDBClient, EnvironmentVariables environmentVariables) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = environmentVariables.calculationResultsTableName();
    }

    public void save(CalculationRequest request, CalculationResult result) {
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "id", AttributeValue.fromS(idFor(request)),
                        "operation", AttributeValue.fromS(result.operation()),
                        "result", AttributeValue.fromN(Double.toString(result.result()))
                ))
                .build();

        dynamoDBClient.putItem(putItemRequest);
    }

    private static String idFor(CalculationRequest request) {
        String key = request.operandA() + ":" + request.operandB() + ":" + request.operation();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
}

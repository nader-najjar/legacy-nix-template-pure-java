package io.template.samplebusinesslayer;

import java.util.Set;

import io.template.environment.models.EnvironmentVariables;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationResultStoreTest {

    private static final String TABLE_NAME = "CalculationResults";

    @Mock
    private DynamoDbClient dynamoDBClient;

    @Mock
    private EnvironmentVariables environmentVariables;

    private CalculationResultStore calculationResultStore;

    @BeforeEach
    void setUp() {
        when(environmentVariables.calculationResultsTableName()).thenReturn(TABLE_NAME);

        calculationResultStore = new CalculationResultStore(dynamoDBClient, environmentVariables);
    }

    @Test
    void savesResultToConfiguredTable() {
        PutItemRequest request = saveAndCapture(new CalculationRequest(10.0, 5.0, "ADD"));

        assertEquals(TABLE_NAME, request.tableName());
        assertEquals(Set.of("id", "operation", "result"), request.item().keySet());
        assertEquals(AttributeValue.fromS("ADD"), request.item().get("operation"));
        assertEquals(AttributeValue.fromN("15.0"), request.item().get("result"));
    }

    @Test
    void usesSameIdForSameRequest() {
        String firstId = saveAndCapture(new CalculationRequest(10.0, 5.0, "ADD")).item().get("id").s();
        String secondId = saveAndCapture(new CalculationRequest(10.0, 5.0, "ADD")).item().get("id").s();

        assertEquals(firstId, secondId);
    }

    @Test
    void usesDifferentIdForDifferentRequest() {
        String firstId = saveAndCapture(new CalculationRequest(10.0, 5.0, "ADD")).item().get("id").s();
        String secondId = saveAndCapture(new CalculationRequest(7.0, 8.0, "ADD")).item().get("id").s();

        assertNotEquals(firstId, secondId);
    }

    private PutItemRequest saveAndCapture(CalculationRequest calculationRequest) {
        clearInvocations(dynamoDBClient);
        calculationResultStore.save(calculationRequest, new CalculationResult(15.0, "ADD"));

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDBClient).putItem(captor.capture());
        return captor.getValue();
    }
}

package io.template.environment.models;

import software.amazon.awssdk.regions.Region;

/**
 * Environment variables configuration.
 *
 * @param stage Deployment stage
 * @param awsRegion Deployment region
 * @param calculationResultsTableName DynamoDB table that stores calculation results
 * @param exampleStringVar Example string environment variable
 * @param exampleIntVar Example integer environment variable
 * @param exampleBooleanVar Example boolean environment variable
 */
public record EnvironmentVariables(
        Stage stage,
        Region awsRegion,
        String calculationResultsTableName,
        String exampleStringVar,
        int exampleIntVar,
        boolean exampleBooleanVar
) { }

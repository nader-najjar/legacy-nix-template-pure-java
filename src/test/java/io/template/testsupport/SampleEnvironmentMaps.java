package io.template.testsupport;

import java.util.HashMap;
import java.util.Map;

public final class SampleEnvironmentMaps {

    private SampleEnvironmentMaps() {
    }

    public static Map<String, String> validEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("STAGE", "BETA");
        environment.put("AWS_REGION", "us-east-1");
        environment.put("EXAMPLE_STRING_VAR", "test");
        environment.put("EXAMPLE_INT_VAR", "1");
        environment.put("EXAMPLE_BOOLEAN_VAR", "true");
        return environment;
    }
}

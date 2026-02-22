package io.template.testsupport;

import java.time.Instant;
import java.util.List;

import io.template.orchestration.models.ApplicationInput;

public final class SampleApplicationInputs {

    private SampleApplicationInputs() {
    }

    public static ApplicationInput exampleApplicationInput() {
        return new ApplicationInput(
                "test",
                1,
                true,
                Instant.parse("2024-01-01T00:00:00Z"),
                List.of("a")
        );
    }
}


package io.template.orchestration.injectionmodules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.template.orchestration.EnvironmentVariablesFactory;
import io.template.shared.models.EnvironmentVariables;

public class EnvironmentModule extends AbstractModule {

    @Provides
    @Singleton
    EnvironmentVariables provideEnvironmentVariables() {
        return EnvironmentVariablesFactory.from(System.getenv());
    }
}

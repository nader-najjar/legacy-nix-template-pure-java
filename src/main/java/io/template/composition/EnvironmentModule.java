package io.template.composition;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.template.environment.EnvironmentVariablesFactory;
import io.template.environment.models.EnvironmentVariables;

public class EnvironmentModule extends AbstractModule {

    @Provides
    @Singleton
    EnvironmentVariables provideEnvironmentVariables() {
        return EnvironmentVariablesFactory.from(System.getenv());
    }
}

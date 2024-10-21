package uk.gov.hmcts.reform.lib.util.serenity5.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import net.serenitybdd.core.di.SerenityInfrastructure;
import net.thucydides.junit.listeners.TestCountListener;
import net.thucydides.model.statistics.TestCount;
import net.thucydides.model.steps.StepListener;
import net.thucydides.model.util.EnvironmentVariables;


public class JUnit5Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(StepListener.class)
            .annotatedWith(TestCounterBinding.class)
            .toProvider(TestCountListenerProvider.class)
            .in(Singleton.class);
    }

    public static class TestCountListenerProvider implements Provider<StepListener> {

        @Override
        public StepListener get() {
            EnvironmentVariables environmentVariables = SerenityInfrastructure.getEnvironmentVariables();
            TestCount testCount = SerenityInfrastructure.getTestCount();
            return new TestCountListener(environmentVariables, testCount);
        }
    }
}


package uk.gov.hmcts.reform.lib.util.serenity5.extension;

import com.google.inject.Key;
import net.serenitybdd.core.environment.ConfiguredEnvironment;
import net.serenitybdd.core.injectors.EnvironmentDependencyInjector;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.Listeners;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepListener;
import net.thucydides.junit.listeners.TestCounter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import uk.gov.hmcts.reform.lib.util.serenity5.guice.JUnitInjectors;



public class SerenityExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        StepEventBus.getEventBus().clear();

        registerListenersOnEventBus(
            createBaseStepListener(),
            Listeners.getLoggingListener(),
            testCountListener());
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        injectEnvironmentVariablesInto(extensionContext.getRequiredTestInstance());
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        StepEventBus.getEventBus().dropAllListeners();
    }

    private BaseStepListener createBaseStepListener() {
        return Listeners.getBaseStepListener().withOutputDirectory(ConfiguredEnvironment.getConfiguration()
            .getOutputDirectory());
    }

    private void registerListenersOnEventBus(final StepListener... stepListeners) {
        for (StepListener currentStepListener : stepListeners) {
            StepEventBus.getEventBus().registerListener(currentStepListener);
        }
    }

    private StepListener testCountListener() {
        return JUnitInjectors.getInjector().getInstance(Key.get(StepListener.class, TestCounter.class));
    }

    private void injectEnvironmentVariablesInto(final Object testCase) {
        new EnvironmentDependencyInjector().injectDependenciesInto(testCase);
    }
}

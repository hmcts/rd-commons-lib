package uk.gov.hmcts.reform.lib.util.serenity5.extension;

import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.StepAnnotations;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;



public class SerenityStepExtension implements BeforeEachCallback, InvocationInterceptor {

    @Override
    public void beforeEach(final ExtensionContext context) {
        StepAnnotations.injector().injectScenarioStepsInto(context.getRequiredTestInstance(), StepFactory.getFactory());
    }

    @Override
    public void interceptTestMethod(final Invocation<Void> invocation,
                                    final ReflectiveInvocationContext<Method> invocationContext,
                                    final ExtensionContext extensionContext) throws Throwable {
        try {
            invocation.proceed();
        } catch (final AssertionError assertionError) {
            if (noStepInTheCurrentTestHasFailed()) {
                throw assertionError;
            }
        }

        throwStepFailures(extensionContext);
        throwStepAssumptionViolations(extensionContext);
    }

    private boolean noStepInTheCurrentTestHasFailed() {
        return !StepEventBus.getEventBus().aStepInTheCurrentTestHasFailed();
    }

    private void throwStepFailures(final ExtensionContext extensionContext) throws Throwable {
        final BaseStepListener baseStepListener = baseStepListener();
        if (baseStepListener.aStepHasFailed()) {
            throw baseStepListener.getTestFailureCause().toException();
        }
    }

    private void throwStepAssumptionViolations(final ExtensionContext extensionContext) {
        final StepEventBus eventBus = StepEventBus.getEventBus();
        if (eventBus.assumptionViolated()) {
            throw new TestAbortedException(eventBus.getAssumptionViolatedMessage());
        }
    }

    private BaseStepListener baseStepListener() {
        return StepEventBus.getEventBus().getBaseStepListener();
    }

}

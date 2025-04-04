package uk.gov.hmcts.reform.lib.util.serenity5.extension;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.annotations.Manual;
import net.thucydides.core.annotations.ManualTestMarkedAsError;
import net.thucydides.core.annotations.ManualTestMarkedAsFailure;
import net.thucydides.model.domain.TestResult;
import net.thucydides.core.steps.StepEventBus;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;



@Slf4j
public class SerenityManualExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(final Invocation<Void> invocation,
                                    final ReflectiveInvocationContext<Method> invocationContext,
                                    final ExtensionContext extensionContext) throws Throwable {
        final Method testMethod = extensionContext.getRequiredTestMethod();

        if (testMethod.isAnnotationPresent(Manual.class)) {
            try {
                invocation.proceed();
            } catch (Exception e) {
                log.error("Exception during test execution on @Manual test will be ignored.", e);
            }
            markAsManual(testMethod.getAnnotation(Manual.class), StepEventBus.getEventBus());
        } else {
            invocation.proceed();
        }
    }

    @SuppressWarnings("checkstyle:Indentation")
    private void markAsManual(final Manual manualAnnotation, final StepEventBus eventBus) throws Throwable {
        eventBus.testIsManual();

        eventBus.getBaseStepListener().latestTestOutcome().ifPresent(
            outcome -> {
                outcome.setResult(manualAnnotation.result());
                if (manualAnnotation.result() == TestResult.FAILURE) {
                    outcome.setTestFailureMessage(manualReasonDeclaredIn(manualAnnotation));
                }
            }
        );

        switch (manualAnnotation.result()) {
            case SUCCESS:
                return;
            case FAILURE:
                final Throwable failure = new ManualTestMarkedAsFailure(manualReasonDeclaredIn(manualAnnotation));
                eventBus.testFailed(failure);
                throw failure;
            case ERROR, COMPROMISED, UNSUCCESSFUL: {
                final Throwable error = new ManualTestMarkedAsError(manualReasonDeclaredIn(manualAnnotation));
                eventBus.testFailed(error);
                throw error;
            }
            case IGNORED: {
                eventBus.testIgnored();
                throwMarkerExceptionForAnAbortedExecutionInJunit5(manualAnnotation);
                break;
            }
            case SKIPPED: {
                eventBus.testSkipped();
                throwMarkerExceptionForAnAbortedExecutionInJunit5(manualAnnotation);
                break;
            }
            default: {
                eventBus.testPending();
                throwMarkerExceptionForAnAbortedExecutionInJunit5(manualAnnotation);
            }
        }
    }

    private void throwMarkerExceptionForAnAbortedExecutionInJunit5(final Manual manualAnnotation) {
        throw new SerenityJUnitLifecycleAdapterExtension.ManualTestAbortedException(manualAnnotation.result());
    }

    private String manualReasonDeclaredIn(final Manual manualAnnotation) {
        final String reason = manualAnnotation.reason();
        return reason == null ? "Manual test failure" : "Manual test failure: " + reason;
    }
}

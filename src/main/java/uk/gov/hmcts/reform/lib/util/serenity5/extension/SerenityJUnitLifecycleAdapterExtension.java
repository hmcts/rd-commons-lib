package uk.gov.hmcts.reform.lib.util.serenity5.extension;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.serenitybdd.annotations.Manual;
import net.serenitybdd.annotations.Pending;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.model.domain.TestResult;
import net.thucydides.model.domain.TestTag;
import net.thucydides.model.steps.TestSourceType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.Optional;


public class SerenityJUnitLifecycleAdapterExtension implements BeforeEachCallback, AfterAllCallback, TestWatcher,
    LifecycleMethodExecutionExceptionHandler {

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        notifyTestStarted(extensionContext, extensionContext.getDisplayName().replace("()", ""));

        final Method testMethod = extensionContext.getRequiredTestMethod();
        if (testMethod.isAnnotationPresent(Pending.class) && !testMethod.isAnnotationPresent(Manual.class)) {
            throw new PendingException();
        }
    }

    public void afterAll(final ExtensionContext extensionContext) {
        StepEventBus.getEventBus().testSuiteFinished();
    }

    @Override
    public void testSuccessful(final ExtensionContext extensionContext) {
        notifyTestFinished();
    }

    @Override
    public void testDisabled(ExtensionContext extensionContext, Optional<String> reason) {
        beforeEach(extensionContext);
        StepEventBus.getEventBus().testIgnored();
        notifyTestFinished();
    }

    @Override
    public void testAborted(final ExtensionContext extensionContext, final Throwable cause) {
        if (cause instanceof PendingException) {
            StepEventBus.getEventBus().testPending();
        } else {
            StepEventBus.getEventBus().assumptionViolated(cause.getMessage());
        }
        notifyTestFinished();
    }

    @Override
    public void testFailed(final ExtensionContext extensionContext, final Throwable cause) {
        StepEventBus.getEventBus().testFailed(cause);
        notifyTestFinished();
    }

    @Override
    public void handleBeforeAllMethodExecutionException(final ExtensionContext extensionContext,
                                                        final Throwable throwable) throws Throwable {
        handleTestClassLevelLifecycleFailure(extensionContext, throwable, "Initialization");
    }

    @Override
    public void handleAfterAllMethodExecutionException(final ExtensionContext extensionContext,
                                                       final Throwable throwable) throws Throwable {
        handleTestClassLevelLifecycleFailure(extensionContext, throwable, "Tear down");
    }

    private void notifyTestStarted(final ExtensionContext extensionContext, final String name) {
        startTestSuiteForFirstTest(extensionContext);
        StepEventBus.getEventBus().clear();
        StepEventBus.getEventBus().setTestSource(TestSourceType.TEST_SOURCE_JUNIT.getValue());
        StepEventBus.getEventBus().testStarted(name, extensionContext.getRequiredTestClass());
        StepEventBus.getEventBus().addTagsToCurrentTest(extensionContext.getTags().stream().map(TestTag::withValue)
                                               .toList());
    }

    private void startTestSuiteForFirstTest(ExtensionContext extensionContext) {
        if (!StepEventBus.getEventBus().testSuiteHasStarted()) {
            StepEventBus.getEventBus().testSuiteStarted(extensionContext.getRequiredTestClass());
        }
    }

    private void handleTestClassLevelLifecycleFailure(final ExtensionContext extensionContext,
                                                      final Throwable throwable, final String scenario)
        throws Throwable {
        notifyTestStarted(extensionContext, scenario);
        testFailed(extensionContext, throwable);
        throw throwable;
    }


    private void notifyTestFinished() {
        StepEventBus.getEventBus().testFinished();
    }

    private static class PendingException extends TestAbortedException {

    }

    @RequiredArgsConstructor
    @ToString
    static class ManualTestAbortedException extends TestAbortedException {

        private final TestResult annotatedResult;
    }
}

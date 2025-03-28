package uk.gov.hmcts.reform.lib.util.serenity5.extension;

import net.serenitybdd.model.environment.ConfiguredEnvironment;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.model.domain.TestOutcome;
import net.thucydides.model.reports.AcceptanceTestReporter;
import net.thucydides.model.reports.ReportService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class SerenityReportExtension implements AfterAllCallback {

    @Override
    public void afterAll(@NotNull ExtensionContext context) throws Exception {
        final BaseStepListener baseStepListener = StepEventBus.getEventBus().getBaseStepListener();
        generateReports(baseStepListener);
    }

    protected void generateReports(BaseStepListener baseStepListener) {
        generateReportsFor(baseStepListener.getTestOutcomes());
    }

    private void generateReportsFor(final List<TestOutcome> testOutcomeResults) {
        final ReportService reportService = new ReportService(getOutputDirectory(), getDefaultReporters());
        reportService.generateReportsFor(testOutcomeResults);
        reportService.generateConfigurationsReport();
    }

    protected Collection<AcceptanceTestReporter> getDefaultReporters() {
        return ReportService.getDefaultReporters();
    }

    public File getOutputDirectory() {
        return ConfiguredEnvironment.getConfiguration().getOutputDirectory();
    }

}

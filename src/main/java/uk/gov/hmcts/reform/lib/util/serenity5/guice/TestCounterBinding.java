package uk.gov.hmcts.reform.lib.util.serenity5.guice;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface TestCounterBinding {}

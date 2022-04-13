package serenity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import serenity.extension.SerenityManualExtension;
import serenity.extension.page.SerenityPageExtension;
import serenity.extension.SerenityExtension;
import serenity.extension.SerenityJUnitLifecycleAdapterExtension;
import serenity.extension.SerenityStepExtension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith({
    SerenityExtension.class,
    SerenityJUnitLifecycleAdapterExtension.class,
    SerenityManualExtension.class,
    SerenityPageExtension.class,
    SerenityStepExtension.class})
public @interface SerenityTestWithoutReporting {
}

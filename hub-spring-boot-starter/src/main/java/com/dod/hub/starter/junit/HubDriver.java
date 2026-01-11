package com.dod.hub.starter.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.core.config.HubBrowserType;

/**
 * Marks a field or parameter to be injected with a managed HubWebDriver
 * instance.
 * The instance is created before the test and quit after the test.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface HubDriver {
    HubProviderType provider() default HubProviderType.DEFAULT;

    HubBrowserType browser() default HubBrowserType.DEFAULT;

    String gridUrl() default "";

    String[] options() default {};
}

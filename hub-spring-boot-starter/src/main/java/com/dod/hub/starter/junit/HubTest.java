package com.dod.hub.starter.junit;

import com.dod.hub.starter.HubAutoConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience meta-annotation for a Spring Boot Test that uses the Hub
 * Extension.
 * Ensures the HubContext is loaded and the lifecycle extension is active.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = HubAutoConfiguration.class)
@ExtendWith(HubExtension.class)
public @interface HubTest {
}

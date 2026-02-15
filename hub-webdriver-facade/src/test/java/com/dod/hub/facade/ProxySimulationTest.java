package com.dod.hub.facade;

import com.dod.hub.core.command.HubCommand;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.pipeline.CommandContext;
import com.dod.hub.core.pipeline.CommandPipeline;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxySimulationTest {

    @Mock
    private HubProvider provider;
    @Mock
    private ProviderSession session;
    @Mock
    private CommandPipeline pipeline;
    @Mock
    private HubElementRef elementRef;

    private HubWebDriver driver;
    private HubWebElement realElement;

    @BeforeEach
    void setUp() throws Exception {
        when(provider.getName()).thenReturn("mock-provider");
        // when(session.getSessionId()).thenReturn("session-123"); // session is mocked
        // but might not be called if we inject it directly

        // Use the public constructor
        driver = new HubWebDriver(provider);

        // Inject mocks via Reflection
        injectField(driver, "pipeline", pipeline);
        injectField(driver, "session", session);

        // We need session.getSessionId() to return something because HubWebDriver might
        // use it in logging/context
        lenient().when(session.getSessionId()).thenReturn("session-123");

        realElement = new HubWebElement(driver, elementRef);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void executeScript_shouldUnwrapDirectHubWebElement() {
        // Setup pipeline to execute the action
        setupPipelineExecution();

        driver.executeScript("return arguments[0];", realElement);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(provider).executeScript(any(), anyString(), captor.capture());

        Object[] args = captor.getValue();
        assertEquals(1, args.length);
        assertSame(elementRef, args[0]);
    }

    @Test
    void executeScript_shouldUnwrapJDKProxyViaWrapsElement() {
        // Create a JDK Proxy that simulates a Selenium PageFactory proxy
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getWrappedElement".equals(method.getName())) {
                    return realElement;
                }
                // For other methods, we delegate to realElement (though not strictly needed for
                // unwrap)
                return method.invoke(realElement, args);
            }
        };

        WebElement proxyElement = (WebElement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { WebElement.class, WrapsElement.class },
                handler);

        setupPipelineExecution();

        driver.executeScript("return arguments[0];", proxyElement);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(provider).executeScript(any(), anyString(), captor.capture());

        Object[] args = captor.getValue();
        assertEquals(1, args.length);
        assertSame(elementRef, args[0]);
    }

    /**
     * Simulates an object that is NOT related to HubWebElement by class hierarchy,
     * but has the exact same method signature `getElementRef()`.
     * This tests the "Duck Typing" fallback.
     */
    @Test
    void executeScript_shouldUnwrapViaDuckTyping() {
        Object duckTypedElement = new Object() {
            public HubElementRef handle() {
                return elementRef;
            }
        };

        setupPipelineExecution();

        driver.executeScript("return arguments[0];", duckTypedElement);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(provider).executeScript(any(), anyString(), captor.capture());

        Object[] args = captor.getValue();
        assertEquals(1, args.length);
        assertSame(elementRef, args[0]);
    }

    private void setupPipelineExecution() {
        when(pipeline.execute(any(CommandContext.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
    }
}

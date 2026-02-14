package com.dod.hub.samples.spring;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pagefactory.HubComponent;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
import com.dod.hub.starter.context.HubContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ComponentInjectionTest.TestConfig.class)
public class ComponentInjectionTest {

    @Autowired
    private HubSpringFactory hubSpringFactory;

    @Configuration
    static class TestConfig {
        @Bean
        public HubSpringFactory hubSpringFactory(org.springframework.context.ApplicationContext context) {
            return new HubSpringFactory(context);
        }

        @Bean
        public DummyService dummyService() {
            return new DummyService();
        }
    }

    @Service
    static class DummyService {
        public String sayHello() {
            return "Hello";
        }
    }

    // A HubComponent that expects injection
    public static class MyComponent extends HubComponent {
        @Autowired
        private DummyService dummyService;

        public DummyService getDummyService() {
            return dummyService;
        }
    }

    // A Page Object using the component
    @Component
    public static class MyPage {
        @FindBy(id = "foo")
        private MyComponent myComponent;

        public MyComponent getMyComponent() {
            return myComponent;
        }
    }

    @BeforeEach
    void setup() {
        // Mock driver to satisfy HubContext
        HubWebDriver mockDriver = Mockito.mock(HubWebDriver.class);
        HubContext.set(mockDriver);
    }

    @AfterEach
    void tearDown() {
        HubContext.remove();
    }

    @Test
    void testComponentInjection() {
        // Create the page using HubSpringFactory
        MyPage page = hubSpringFactory.createPage(MyPage.class);

        // Access the component (this triggers initialization/proxying)
        MyComponent component = page.getMyComponent();

        // Use reflection or just access the component (it's initialized by
        // HubFieldDecorator)
        // Note: FieldDecorator initializes the field with an instance (or proxy).
        // Since HubComponent is instantiated by HubFieldDecorator, it should now be
        // injected.

        assertNotNull(component, "Component should not be null");
        assertNotNull(component.getDummyService(), "Autowired service in component should not be null");
    }
}

package com.dod.hub.starter.pagefactory;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pagefactory.HubPageFactory;
import com.dod.hub.starter.context.HubContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * A Spring-aware PageFactory that creates Page Objects as full Spring Beans
 * (supporting @Autowired), while also initializing them with HubPageFactory
 * (supporting @FindBy for nested components).
 */
@Component
public class HubSpringFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public HubSpringFactory(ApplicationContext context) {
        this.beanFactory = context.getAutowireCapableBeanFactory();
    }

    /**
     * Creates a new instance of the given Page Class, performing Spring Autowiring,
     * and then initializing Selenium/Hub elements using the active HubWebDriver.
     *
     * @param pageClass The class of the Page Object.
     * @param <T>       The type of the Page Object.
     * @return The fully initialized Page Object.
     */
    public <T> T createPage(Class<T> pageClass) {
        // 1. Get the current active driver
        HubWebDriver driver = HubContext.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No active HubWebDriver found. Ensure you are calling this within a @HubTest method.");
        }

        return createPage(driver, pageClass);
    }

    /**
     * Creates a new instance with a specific driver.
     */
    public <T> T createPage(HubWebDriver driver, Class<T> pageClass) {
        // 1. Create and Autowire the bean
        // createBean() instantiates, autowires, and initializes (PostConstruct)
        T page = beanFactory.createBean(pageClass);

        // 2. Initialize Selenium elements (Nested Components)
        HubPageFactory.initElements(driver, page);

        return page;
    }
}

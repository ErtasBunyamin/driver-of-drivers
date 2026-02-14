package com.dod.hub.facade.pagefactory;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.WebDriver;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HubFieldDecoratorTest {

    public static class MyComponent extends HubComponent {
        @Override
        public void init(WebElement root) {
            super.init(root);
        }
    }

    public static class MyPage {
        @FindBy(id = "test")
        List<MyComponent> components;
    }

    @Test
    public void testDecorateList() throws Exception {
        WebDriver driver = Mockito.mock(WebDriver.class);
        ElementLocatorFactory factory = new DefaultElementLocatorFactory(driver);
        HubFieldDecorator decorator = new HubFieldDecorator(factory);

        Field field = MyPage.class.getDeclaredField("components");
        Object decorated = decorator.decorate(MyPage.class.getClassLoader(), field);

        assertNotNull(decorated, "Decorated list should not be null");
        assertTrue(decorated instanceof List, "Decorated object should be a List");
    }
}

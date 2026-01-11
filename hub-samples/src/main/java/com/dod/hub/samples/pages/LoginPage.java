package com.dod.hub.samples.pages;

import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pagefactory.HubPageFactory;
import com.dod.hub.starter.junit.HubDriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Sample Page Object using standard Selenium @FindBy annotations.
 */
public class LoginPage {

    @FindBy(css = "h1")
    private WebElement header;

    @FindBy(linkText = "Learn more")
    private WebElement moreLink;

    // Nested Component
    @FindBy(css = "h1") // Reusing h1 as 'popup' for demo purposes
    private LoginPopup popup;

    @HubDriver(provider = HubProviderType.SELENIUM)
    private HubWebDriver driver;

    public LoginPage() {
        HubPageFactory.initElements(driver, this); // Changed to HubPageFactory
    }

    public LoginPage(WebDriver driver) {
        HubPageFactory.initElements(driver, this); // Changed to HubPageFactory
    }

    public String getHeaderText() {
        return header.getText();
    }

    public void clickMoreLink() {
        moreLink.click();
    }

    public LoginPopup getPopup() {
        return popup;
    }
}

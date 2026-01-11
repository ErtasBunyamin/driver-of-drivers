package com.dod.hub.provider.selenium;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.config.HubBrowserType; // Added
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import com.dod.hub.core.exception.HubTimeoutException;
import com.dod.hub.core.exception.HubException;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import java.util.List;
import java.util.stream.Collectors;

public class SeleniumProvider implements HubProvider {
    @Override
    public String getName() {
        return "selenium";
    }

    @Override
    public ProviderSession start(SessionCapabilities caps) {
        WebDriver driver;

        HubBrowserType browser = caps.getBrowserName();

        switch (browser) {
            case FIREFOX:
                FirefoxOptions fOptions = new FirefoxOptions();
                if (caps.isHeadless())
                    fOptions.addArguments("-headless");
                applyOptions(fOptions, caps.getOptions());
                driver = createDriver(caps, fOptions);
                break;
            case CHROME:
            default:
                ChromeOptions cOptions = new ChromeOptions();
                if (caps.isHeadless())
                    cOptions.addArguments("--headless=new");
                applyOptions(cOptions, caps.getOptions());
                driver = createDriver(caps, cOptions);
                break;
        }

        return new ProviderSession(getName(), caps, driver);
    }

    private void applyOptions(MutableCapabilities options, Map<String, Object> caps) {
        if (caps != null) {
            caps.forEach(options::setCapability);
        }
    }

    private WebDriver createDriver(SessionCapabilities caps, MutableCapabilities options) {
        if (caps.getGridUrl() != null && !caps.getGridUrl().isEmpty()) {
            try {
                return new RemoteWebDriver(URI.create(caps.getGridUrl()).toURL(), options);
            } catch (MalformedURLException e) {
                throw new HubException("Invalid Grid URL: " + caps.getGridUrl(), e);
            }
        }

        if (options instanceof ChromeOptions) {
            return new ChromeDriver((ChromeOptions) options);
        } else if (options instanceof FirefoxOptions) {
            return new FirefoxDriver((FirefoxOptions) options);
        } else {
            throw new HubException("Unsupported options type for local execution: " + options.getClass().getName());
        }
    }

    @Override
    public void stop(ProviderSession session) {
        WebDriver driver = (WebDriver) session.getRawDriver();
        if (driver != null) {
            driver.quit();
        }
    }

    private By toBy(HubLocator locator) {
        switch (locator.getStrategy()) {
            case CSS:
                return By.cssSelector(locator.getValue());
            case XPATH:
                return By.xpath(locator.getValue());
            case ID:
                return By.id(locator.getValue());
            case NAME:
                return By.name(locator.getValue());
            case CLASS_NAME:
                return By.className(locator.getValue());
            case TAG_NAME:
                return By.tagName(locator.getValue());
            case LINK_TEXT:
                return By.linkText(locator.getValue());
            case PARTIAL_LINK_TEXT:
                return By.partialLinkText(locator.getValue());
            default:
                throw new IllegalArgumentException("Unsupported strategy for Selenium: " + locator.getStrategy());
        }
    }

    private WebElement getElement(HubElementRef ref) {
        return (WebElement) ref.getProviderHandle();
    }

    private WebDriver getDriver(ProviderSession session) {
        return (WebDriver) session.getRawDriver();
    }

    @Override
    public HubElementRef find(ProviderSession session, HubLocator locator) {
        try {
            WebDriver driver = getDriver(session);
            WebElement el = driver.findElement(toBy(locator));
            return new HubElementRef(locator, el);
        } catch (TimeoutException e) {
            throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
        } catch (NoSuchElementException e) {
            throw new HubException("Element not found: " + locator, e);
        } catch (WebDriverException e) {
            throw new HubException("WebDriver error finding element: " + locator, e);
        }
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubLocator locator) {
        WebDriver driver = getDriver(session);
        List<WebElement> els = driver.findElements(toBy(locator));
        return els.stream()
                .map(el -> new HubElementRef(locator, el))
                .collect(Collectors.toList());
    }

    @Override
    public HubElementRef find(ProviderSession session, HubElementRef parent, HubLocator locator) {
        try {
            WebElement parentEl = getElement(parent);
            WebElement el = parentEl.findElement(toBy(locator));
            return new HubElementRef(locator, el);
        } catch (TimeoutException e) {
            throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
        } catch (NoSuchElementException e) {
            throw new HubException("Element not found: " + locator, e);
        } catch (WebDriverException e) {
            throw new HubException("WebDriver error finding element: " + locator, e);
        }
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubElementRef parent, HubLocator locator) {
        WebElement parentEl = getElement(parent);
        List<WebElement> els = parentEl.findElements(toBy(locator));
        return els.stream()
                .map(el -> new HubElementRef(locator, el))
                .collect(Collectors.toList());
    }

    @Override
    public void click(ProviderSession session, HubElementRef element) {
        getElement(element).click();
    }

    @Override
    public void type(ProviderSession session, HubElementRef element, String text) {
        getElement(element).sendKeys(text);
    }

    @Override
    public void clear(ProviderSession session, HubElementRef element) {
        getElement(element).clear();
    }

    @Override
    public String getText(ProviderSession session, HubElementRef element) {
        return getElement(element).getText();
    }

    @Override
    public String getAttribute(ProviderSession session, HubElementRef element, String attributeName) {
        return getElement(element).getAttribute(attributeName);
    }

    @Override
    public boolean isDisplayed(ProviderSession session, HubElementRef element) {
        return getElement(element).isDisplayed();
    }

    @Override
    public boolean isEnabled(ProviderSession session, HubElementRef element) {
        return getElement(element).isEnabled();
    }

    @Override
    public boolean isSelected(ProviderSession session, HubElementRef element) {
        return getElement(element).isSelected();
    }

    @Override
    public void navigate(ProviderSession session, String url) {
        getDriver(session).get(url);
    }

    @Override
    public void back(ProviderSession session) {
        getDriver(session).navigate().back();
    }

    @Override
    public void forward(ProviderSession session) {
        getDriver(session).navigate().forward();
    }

    @Override
    public void refresh(ProviderSession session) {
        getDriver(session).navigate().refresh();
    }

    @Override
    public String getTitle(ProviderSession session) {
        return getDriver(session).getTitle();
    }

    @Override
    public String getCurrentUrl(ProviderSession session) {
        return getDriver(session).getCurrentUrl();
    }

    @Override
    public String getPageSource(ProviderSession session) {
        return getDriver(session).getPageSource();
    }

    @Override
    public byte[] takeScreenshot(ProviderSession session) {
        return ((TakesScreenshot) getDriver(session)).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    public void setTimeouts(ProviderSession session, long implicitWaitMs, long pageLoadMs) {
        WebDriver.Options manage = getDriver(session).manage();
        if (implicitWaitMs > 0)
            manage.timeouts().implicitlyWait(java.time.Duration.ofMillis(implicitWaitMs));
        if (pageLoadMs > 0)
            manage.timeouts().pageLoadTimeout(java.time.Duration.ofMillis(pageLoadMs));
    }
}

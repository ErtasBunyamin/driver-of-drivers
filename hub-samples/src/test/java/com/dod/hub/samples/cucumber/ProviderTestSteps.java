package com.dod.hub.samples.cucumber;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.samples.cucumber.pages.StabilityPage;
import com.dod.hub.starter.context.HubContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;

import java.net.URL;
import java.util.Set;

/**
 * Step definitions for the comprehensive POM-based provider test.
 * Uses {@link StabilityPage} as the Page Object and {@link HubContext}
 * to access the current {@link HubWebDriver}.
 */
public class ProviderTestSteps {

    private StabilityPage page;
    private Object jsResult;
    private String originalWindowHandle;

    private HubWebDriver driver() {
        return (HubWebDriver) HubContext.get();
    }

    private String getStabilityPageUrl() {
        URL url = ProviderTestSteps.class.getResource("/stability/stability-page.html");
        if (url == null) {
            throw new RuntimeException("stability-page.html not found on classpath.");
        }
        return url.toString();
    }

    // ── Initialization ──

    @Given("I initialize the StabilityPage")
    public void i_initialize_the_stability_page() {
        HubWebDriver d = driver();
        d.get(getStabilityPageUrl());
        page = new StabilityPage(d);
    }

    // ── Navigation & Title ──

    @Then("the page title text should be {string}")
    public void the_page_title_text_should_be(String expected) {
        Assert.assertEquals(page.getPageTitleText(), expected);
    }

    @Then("the current URL should contain {string}")
    public void the_current_url_should_contain(String fragment) {
        String url = driver().getCurrentUrl();
        Assert.assertTrue(url.contains(fragment),
                "Expected URL to contain '" + fragment + "' but was '" + url + "'");
    }

    @Then("the subtitle text should be {string}")
    public void the_subtitle_text_should_be(String expected) {
        Assert.assertEquals(page.subtitle.getText(), expected);
    }

    // ── Item List (Input, Click, findElements) ──

    @When("I add an item {string} via POM")
    public void i_add_an_item_via_pom(String text) {
        page.addItem(text);
    }

    @Then("the item list should have {int} entries")
    public void the_item_list_should_have_entries(int expected) {
        // Re-init page to refresh @FindBy list
        page = new StabilityPage(driver());
        Assert.assertEquals(page.getItemCount(), expected);
    }

    @Then("item {int} should have text {string}")
    public void item_should_have_text(int index, String expected) {
        Assert.assertEquals(page.getItemText(index), expected);
    }

    // ── Type, Clear, getValue ──

    @When("I type {string} into the item input")
    public void i_type_into_the_item_input(String text) {
        ((JavascriptExecutor) driver()).executeScript(
                "document.getElementById('item-input').value = '" + text.replace("'", "\\'") + "';");
    }

    @Then("the item input value should be {string}")
    public void the_item_input_value_should_be(String expected) {
        Object val = ((JavascriptExecutor) driver()).executeScript(
                "return document.getElementById('item-input').value;");
        String actual = val != null ? val.toString() : "";
        Assert.assertEquals(actual, expected);
    }

    @When("I clear the item input")
    public void i_clear_the_item_input() {
        ((JavascriptExecutor) driver()).executeScript(
                "document.getElementById('item-input').value = '';");
    }

    // ── Element State: isEnabled ──

    @Then("the disabled input should not be enabled")
    public void the_disabled_input_should_not_be_enabled() {
        Assert.assertFalse(page.isDisabledInputEnabled(), "Disabled input should not be enabled");
    }

    @Then("the item input should be enabled")
    public void the_item_input_should_be_enabled() {
        Assert.assertTrue(page.itemInput.isEnabled(), "Item input should be enabled");
    }

    // ── Element State: isDisplayed ──

    @Then("the page title should be displayed")
    public void the_page_title_should_be_displayed() {
        Assert.assertTrue(page.pageTitle.isDisplayed(), "Page title should be displayed");
    }

    // ── Element State: isSelected (checkbox) ──

    @Then("the agree checkbox should not be selected")
    public void the_agree_checkbox_should_not_be_selected() {
        Assert.assertFalse(page.isAgreeChecked(), "Checkbox should not be selected");
    }

    @When("I toggle the agree checkbox")
    public void i_toggle_the_agree_checkbox() {
        page.toggleAgreeCheckbox();
    }

    @Then("the agree checkbox should be selected")
    public void the_agree_checkbox_should_be_selected() {
        Assert.assertTrue(page.isAgreeChecked(), "Checkbox should be selected");
    }

    // ── Select ──

    @Then("the selected color should be {string}")
    public void the_selected_color_should_be(String expected) {
        Assert.assertEquals(page.getSelectedColor(), expected);
    }

    @When("I select color {string}")
    public void i_select_color(String value) {
        page.selectColor(value);
    }

    // ── getAttribute ──

    @Then("the nav link href should contain {string}")
    public void the_nav_link_href_should_contain(String fragment) {
        String href = page.getNavLinkHref();
        Assert.assertTrue(href.contains(fragment),
                "Expected href to contain '" + fragment + "' but was '" + href + "'");
    }

    // ── Async Update ──

    @When("I click async update via POM")
    public void i_click_async_update_via_pom() {
        page.clickAsyncUpdate();
    }

    @Then("the status should eventually be {string}")
    public void the_status_should_eventually_be(String expected) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (expected.equals(page.getStatus())) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Assert.assertEquals(page.getStatus(), expected);
    }

    // ── Toggle Details ──

    @When("I toggle details via POM")
    public void i_toggle_details_via_pom() {
        page.toggleDetails();
    }

    @Then("the details panel should be hidden")
    public void the_details_panel_should_be_hidden() {
        Assert.assertFalse(page.isDetailsPanelVisible(), "Details panel should be hidden");
    }

    @Then("the details panel should be visible")
    public void the_details_panel_should_be_visible() {
        Assert.assertTrue(page.isDetailsPanelVisible(), "Details panel should be visible");
    }

    // ── JavaScript Execution ──

    @When("I execute JS {string} with element {string} via driver")
    public void i_execute_js_via_driver(String script, String id) {
        WebElement element = driver().findElement(By.id(id));
        jsResult = ((JavascriptExecutor) driver()).executeScript(script, element);
    }

    @When("I execute async JS scroll script with element {string} via driver")
    public void i_execute_async_js_scroll_script_with_element_via_driver(String id) {
        WebElement element = driver().findElement(By.id(id));
        String js = """
                  const el = arguments[0];
                  const offset = Number(arguments[1] || 0);
                  const done = arguments[arguments.length - 1];

                  if (!el) return done({ ok: false, reason: "element-null" });

                  const vh = () => (window.innerHeight || document.documentElement.clientHeight);

                  const isInView = () => {
                    const r = el.getBoundingClientRect();
                    const topOk = r.top >= offset;
                    const bottomOk = r.bottom <= vh();
                    // yatayda da biraz kontrol edelim
                    const leftOk = r.left >= 0;
                    const rightOk = r.right <= (window.innerWidth || document.documentElement.clientWidth);
                    return topOk && bottomOk && leftOk && rightOk;
                  };

                  const clamp = (v, min, max) => Math.max(min, Math.min(max, v));

                  const start = Date.now();
                  const maxMs = 1500; // tarayıcı içinde mikro-wait; scriptTimeout bunun üstünde olmalı

                  const initial = () => {
                    const r = el.getBoundingClientRect();
                    const centerPad = Math.max(0, (vh() - r.height) / 2);
                    const targetY = window.pageYOffset + r.top - offset - centerPad;
                    window.scrollTo(0, clamp(targetY, 0, document.documentElement.scrollHeight));
                  };

                  initial();

                  (function tick() {
                    if (isInView()) return done({ ok: true });

                    if (Date.now() - start > maxMs) {
                      return done({ ok: false, reason: "not-in-view", rect: el.getBoundingClientRect() });
                    }

                    // küçük düzeltme adımları: overshoot / sticky header etkisini azaltır
                    const r = el.getBoundingClientRect();
                    const delta = (r.top - offset) - 20;
                    window.scrollBy(0, delta);

                    requestAnimationFrame(tick);
                  })();
                """;
        jsResult = ((JavascriptExecutor) driver()).executeAsyncScript(js, element, 90);
    }

    @When("I execute JS {string} via driver")
    public void i_execute_js_via_driver(String script) {
        jsResult = ((JavascriptExecutor) driver()).executeScript(script);
    }

    @Then("the JS result should be {string}")
    public void the_js_result_should_be(String expected) {
        Assert.assertNotNull(jsResult, "JS result should not be null");
        Assert.assertEquals(jsResult.toString(), expected);
    }

    // ── Screenshot ──

    @Then("I should be able to take a screenshot")
    public void i_should_be_able_to_take_a_screenshot() {
        byte[] screenshot = ((TakesScreenshot) driver()).getScreenshotAs(OutputType.BYTES);
        Assert.assertNotNull(screenshot, "Screenshot should not be null");
        Assert.assertTrue(screenshot.length > 0, "Screenshot should not be empty");
    }

    // ── Cookies ──

    @When("I click the cookie button")
    public void i_click_the_cookie_button() {
        page.clickCookieButton();
    }

    @Then("the JS cookie {string} should have value {string}")
    public void the_js_cookie_should_have_value(String name, String expectedValue) {
        Object result = ((JavascriptExecutor) driver()).executeScript("return document.cookie;");
        String cookies = result != null ? result.toString() : "";
        boolean found = false;
        for (String pair : cookies.split(";")) {
            String trimmed = pair.trim();
            if (trimmed.startsWith(name + "=")) {
                String actual = trimmed.substring(name.length() + 1);
                if (!actual.equals(expectedValue)) {
                    throw new AssertionError(
                            "Cookie '" + name + "' expected value '" + expectedValue + "' but was '" + actual + "'");
                }
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError("Cookie '" + name + "' not found in document.cookie: " + cookies);
        }
    }

    // ── Window Management ──

    @When("I open a new window via POM")
    public void i_open_a_new_window_via_pom() {
        originalWindowHandle = driver().getWindowHandle();
        ((JavascriptExecutor) driver()).executeScript("window.open('about:blank', '_blank');");
    }

    @Then("I should have at least {int} window handles")
    public void i_should_have_at_least_window_handles(int expected) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (driver().getWindowHandles().size() >= expected) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Assert.assertTrue(driver().getWindowHandles().size() >= expected,
                "Expected at least " + expected + " handles but got " + driver().getWindowHandles().size());
    }

    @When("I close the extra window and switch back")
    public void i_close_the_extra_window_and_switch_back() {
        Set<String> handles = driver().getWindowHandles();
        for (String h : handles) {
            if (!h.equals(originalWindowHandle)) {
                driver().switchTo().window(h);
                driver().close();
                break;
            }
        }
        driver().switchTo().window(originalWindowHandle);
    }

    // ── Frame Switching ──

    @When("I switch to frame {string}")
    public void i_switch_to_frame(String frameId) {
        WebElement frame = driver().findElement(By.id(frameId));
        driver().switchTo().frame(frame);
    }

    @Then("the frame text should be {string}")
    public void the_frame_text_should_be(String expected) {
        WebElement el = driver().findElement(By.id("frame-text"));
        Assert.assertEquals(el.getText(), expected);
    }

    @When("I switch to default content")
    public void i_switch_to_default_content() {
        driver().switchTo().defaultContent();
    }

    // ── Capabilities ──

    @Then("the capabilities should include {string}")
    public void the_capabilities_should_include(String key) {
        Object val = ((HasCapabilities) driver()).getCapabilities().getCapability(key);
        Assert.assertNotNull(val, "Capability '" + key + "' should be present");
    }

    // ── Page Source ──

    @Then("the page source should contain {string}")
    public void the_page_source_should_contain(String fragment) {
        String source = driver().getPageSource();
        Assert.assertNotNull(source, "Page source should not be null");
        Assert.assertTrue(source.contains(fragment),
                "Expected page source to contain '" + fragment + "'");
    }

    // ── Actions ──

    @When("I perform a hover action on element {string}")
    public void i_perform_a_hover_action_on_element(String id) {
        WebElement element = driver().findElement(By.id(id));
        //new Actions(driver()).doubleClick(element).click(element).sendKeys(Keys.BACK_SPACE).build().perform();
        new Actions(driver()).sendKeys(element,"test").build().perform();
    }
}

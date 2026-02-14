package com.dod.hub.samples.cucumber.pages;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pagefactory.HubPageFactory;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Page Object for the DOD Stability Lab test page.
 * Uses @FindBy annotations with HubPageFactory for provider-agnostic element
 * location.
 */
public class StabilityPage {

    private final HubWebDriver driver;

    // ── Header ──
    @FindBy(id = "page-title")
    public WebElement pageTitle;

    @FindBy(id = "subtitle")
    public WebElement subtitle;

    // ── Input & Buttons ──
    @FindBy(id = "item-input")
    public WebElement itemInput;

    @FindBy(id = "add-btn")
    public WebElement addButton;

    @FindBy(id = "async-btn")
    public WebElement asyncButton;

    @FindBy(id = "toggle-btn")
    public WebElement toggleButton;

    // ── Status & Panels ──
    @FindBy(id = "status")
    public WebElement statusDiv;

    @FindBy(id = "details")
    public WebElement detailsPanel;

    @FindBy(id = "delayed")
    public WebElement delayedBadge;

    // ── List ──
    @FindBy(css = "#items li")
    public List<WebElement> itemsList;

    // ── Form Elements ──
    @FindBy(id = "color-select")
    public WebElement colorSelect;

    @FindBy(id = "agree-check")
    public WebElement agreeCheckbox;

    @FindBy(id = "disabled-input")
    public WebElement disabledInput;

    // ── Links ──
    @FindBy(id = "nav-link")
    public WebElement navLink;

    // ── Action Buttons ──
    @FindBy(id = "alert-btn")
    public WebElement alertButton;

    @FindBy(id = "cookie-btn")
    public WebElement cookieButton;

    public StabilityPage(HubWebDriver driver) {
        this.driver = driver;
        HubPageFactory.initElements(driver, this);
    }

    // ── Helper Methods ──

    public String getPageTitleText() {
        return pageTitle.getText();
    }

    public String getStatus() {
        return statusDiv.getText();
    }

    public String getStatusDataState() {
        return statusDiv.getAttribute("data-state");
    }

    public void addItem(String text) {
        itemInput.clear();
        itemInput.sendKeys(text);
        addButton.click();
    }

    public int getItemCount() {
        return itemsList.size();
    }

    public String getItemText(int index) {
        return itemsList.get(index).getText();
    }

    public void clickAsyncUpdate() {
        asyncButton.click();
    }

    public void toggleDetails() {
        toggleButton.click();
    }

    public boolean isDetailsPanelVisible() {
        try {
            return detailsPanel.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void selectColor(String value) {
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('color-select').value = '" + value.replace("'", "\\'") + "';");
    }

    public String getSelectedColor() {
        Object val = ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('color-select').value;");
        return val != null ? val.toString() : null;
    }

    public void toggleAgreeCheckbox() {
        agreeCheckbox.click();
    }

    public boolean isAgreeChecked() {
        return agreeCheckbox.isSelected();
    }

    public boolean isDisabledInputEnabled() {
        return disabledInput.isEnabled();
    }

    public String getNavLinkHref() {
        return navLink.getAttribute("href");
    }

    public void clickAlertButton() {
        alertButton.click();
    }

    public void clickCookieButton() {
        cookieButton.click();
    }

    public String getInputValue() {
        Object val = ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('item-input').value;");
        return val != null ? val.toString() : "";
    }
}

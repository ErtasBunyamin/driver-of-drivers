package com.dod.hub.samples;

import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.SessionCapabilities;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.provider.hybrid.HybridProvider;
import com.dod.hub.provider.playwright.PlaywrightProvider;
import com.dod.hub.provider.selenium.SeleniumProvider;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;

/**
 * Demonstrates the use of switchTo() methods in HybridProvider:
 * 1. switchTo().newWindow(WindowType.TAB)
 * 2. switchTo().alert() – accept, getText
 * 3. switchTo().activeElement()
 * 4. switchTo().window(handle)
 */
public class HybridSwitchToDemo {

    public static void main(String[] args) {
        HubProvider provider = new HybridProvider();
        SessionCapabilities caps = new SessionCapabilities();
        HubWebDriver driver = new HubWebDriver(provider, caps);

        try {
            System.out.println("=== Starting Hybrid SwitchTo Demo ===");

            // ---- 1. Navigate to initial page ----
            driver.get("https://the-internet.herokuapp.com/windows");
            System.out.println("Navigated to: " + driver.getTitle());
            String originalWindow = driver.getWindowHandle();
            System.out.println("Original window handle: " + originalWindow);

            // ---- 2. switchTo().newWindow(TAB) ----
            System.out.println("\n--- Testing switchTo().newWindow() ---");
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get("https://the-internet.herokuapp.com/javascript_alerts");
            System.out.println("New Tab Title: " + driver.getTitle());

            // ---- 3. switchTo().alert() ----
            System.out.println("\n--- Testing switchTo().alert() ---");
            driver.findElement(By.xpath("//button[text()='Click for JS Alert']")).click();
            Thread.sleep(500); // Small wait for alert to appear
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            System.out.println("Alert Text: " + alertText);
            alert.accept();
            System.out.println("Alert accepted.");

            // Verify result on page
            String result = driver.findElement(By.id("result")).getText();
            System.out.println("Page Result: " + result);

            // ---- 4. switchTo().activeElement() ----
            System.out.println("\n--- Testing switchTo().activeElement() ---");
            WebElement active = driver.switchTo().activeElement();
            System.out.println("Active Element Tag: " + active.getTagName());

            // ---- 5. switchTo().window(originalHandle) ----
            System.out.println("\n--- Testing switchTo().window() (Back to original) ---");
            driver.switchTo().window(originalWindow);
            System.out.println("Back to Original Window Title: " + driver.getTitle());

            System.out.println("\n=== Demo Completed Successfully ===");

        } catch (Exception e) {
            System.err.println("Demo failed with exception:");
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}

package com.dod.hub.facade;

import com.dod.hub.core.command.CommandType;
import com.dod.hub.core.command.HubCommand;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.pipeline.CommandContext;
import com.dod.hub.core.pipeline.CommandPipeline;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;

import java.util.List;

/**
 * Enterprise-grade implementation of {@link WebElement} that delegates
 * operations
 * to a selected {@link HubProvider} via the {@link CommandPipeline}.
 */
public class HubWebElement implements WebElement {

    private final HubElementRef elementRef;
    private final ProviderSession session;
    private final HubProvider provider;
    private final CommandPipeline pipeline;
    private final HubWebDriver driver;

    public HubWebElement(HubWebDriver driver, HubElementRef elementRef) {
        this.driver = driver;
        this.elementRef = elementRef;
        this.session = driver.getSession();
        this.provider = driver.getProvider();
        this.pipeline = driver.getPipeline();
    }

    public HubElementRef getElementRef() {
        return elementRef;
    }

    private CommandContext ctx(CommandType type, String target) {
        HubCommand cmd = new HubCommand(type, session.getSessionId(), provider.getName());
        cmd.setTarget(target);
        return new CommandContext(session, provider, cmd);
    }

    @Override
    public void click() {
        CommandContext context = ctx(CommandType.CLICK, elementRef.getLocator().toString());
        pipeline.execute(context, () -> {
            provider.click(session, elementRef);
            return null;
        });
    }

    @Override
    public void submit() {
        sendKeys(Keys.ENTER);
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        if (keysToSend == null || keysToSend.length == 0)
            return;
        StringBuilder sb = new StringBuilder();
        for (CharSequence cs : keysToSend)
            sb.append(cs);
        String text = sb.toString();

        HubCommand cmd = new HubCommand(CommandType.TYPE, session.getSessionId(), provider.getName());
        cmd.setTarget(elementRef.getLocator().toString());
        cmd.addParam("text", text);

        CommandContext context = new CommandContext(session, provider, cmd);

        pipeline.execute(context, () -> {
            provider.type(session, elementRef, text);
            return null;
        });
    }

    @Override
    public void clear() {
        CommandContext context = ctx(CommandType.CLEAR, elementRef.getLocator().toString());
        pipeline.execute(context, () -> {
            provider.clear(session, elementRef);
            return null;
        });
    }

    @Override
    public String getTagName() {
        return getAttribute("tagName");
    }

    @Override
    public String getAttribute(String name) {
        CommandContext context = ctx(CommandType.GET_ATTRIBUTE, elementRef.getLocator().toString());
        return pipeline.execute(context, () -> provider.getAttribute(session, elementRef, name));
    }

    @Override
    public boolean isSelected() {
        CommandContext context = ctx(CommandType.IS_SELECTED, elementRef.getLocator().toString());
        return pipeline.execute(context, () -> provider.isSelected(session, elementRef));
    }

    @Override
    public boolean isEnabled() {
        CommandContext context = ctx(CommandType.IS_ENABLED, elementRef.getLocator().toString());
        return pipeline.execute(context, () -> provider.isEnabled(session, elementRef));
    }

    @Override
    public String getText() {
        CommandContext context = ctx(CommandType.GET_TEXT, elementRef.getLocator().toString());
        return pipeline.execute(context, () -> provider.getText(session, elementRef));
    }

    @Override
    public List<WebElement> findElements(By by) {
        HubLocator locator = HubBy.toHubLocator(by);
        CommandContext context = ctx(CommandType.FIND_ELEMENTS, locator.toString());
        List<HubElementRef> helper = pipeline.execute(context, () -> provider.findAll(session, elementRef, locator));
        return helper.stream()
                .map(ref -> new HubWebElement(driver, ref))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        HubLocator locator = HubBy.toHubLocator(by);
        CommandContext context = ctx(CommandType.FIND_ELEMENT, locator.toString());
        HubElementRef found = pipeline.execute(context, () -> provider.find(session, elementRef, locator));
        return new HubWebElement(driver, found);
    }

    @Override
    public boolean isDisplayed() {
        CommandContext context = ctx(CommandType.IS_DISPLAYED, elementRef.getLocator().toString());
        return pipeline.execute(context, () -> provider.isDisplayed(session, elementRef));
    }

    @Override
    public Point getLocation() {
        return new Point(0, 0);
    }

    @Override
    public Dimension getSize() {
        return new Dimension(0, 0);
    }

    @Override
    public Rectangle getRect() {
        return new Rectangle(getLocation(), getSize());
    }

    @Override
    public String getCssValue(String propertyName) {
        return "";
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        throw new UnsupportedOperationException("Element-level screenshots are not supported in the current version.");
    }
}

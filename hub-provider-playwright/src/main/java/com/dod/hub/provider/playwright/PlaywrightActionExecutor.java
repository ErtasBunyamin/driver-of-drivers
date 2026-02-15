package com.dod.hub.provider.playwright;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.facade.HubElement;
import com.dod.hub.facade.HubWebElement;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.BoundingBox;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper class to execute W3C actions using Playwright.
 */
public class PlaywrightActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightActionExecutor.class);
    private final PlaywrightProvider provider;

    public PlaywrightActionExecutor(PlaywrightProvider provider) {
        this.provider = provider;
    }

    public void performActions(ProviderSession session, Collection<?> actions) {
        Page page = provider.getPage(session);
        if (page == null) {
            return;
        }

        List<Sequence> sequences = new ArrayList<>();
        for (Object obj : actions) {
            if (obj instanceof Sequence seq) {
                sequences.add(seq);
            }
        }

        if (sequences.isEmpty()) {
            return;
        }

        List<List<Map<String, Object>>> allQueues = new ArrayList<>();
        for (Sequence seq : sequences) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) seq.encode().get("actions");
            allQueues.add(steps);
        }

        int maxSteps = allQueues.stream().mapToInt(List::size).max().orElse(0);

        for (int i = 0; i < maxSteps; i++) {
            for (int s = 0; s < sequences.size(); s++) {
                List<Map<String, Object>> queue = allQueues.get(s);
                if (i < queue.size()) {
                    Map<String, Object> action = queue.get(i);
                    Map<String, Object> encodedSeq = sequences.get(s).encode();
                    String type = (String) encodedSeq.get("type");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) encodedSeq.get("parameters");

                    performAction(page, type, parameters, action);
                }
            }
        }
    }

    private void performAction(Page page, String inputType, Map<String, Object> parameters,
            Map<String, Object> action) {
        String actionType = (String) action.get("type");
        if ("pause".equals(actionType)) {
            Long duration = getDuration(action);
            if (duration > 0) {
                page.waitForTimeout(duration.doubleValue());
            }
            return;
        }

        switch (inputType) {
            case "key":
                handleKeyAction(page, actionType, action);
                break;
            case "pointer":
                handlePointerAction(page, actionType, parameters, action);
                break;
            case "wheel":
                handleWheelAction(page, actionType, action);
                break;
            case "none":
                // 'none' source only supports 'pause' which is handled above.
                break;
            default:
                log.debug("Unknown input type: {}", inputType);
                break;
        }
    }

    private Long getDuration(Map<String, Object> action) {
        Object d = action.get("duration");
        if (d instanceof Number) {
            return ((Number) d).longValue();
        }
        return 0L;
    }

    private void handleKeyAction(Page page, String actionType, Map<String, Object> action) {
        String rawKey = (String) action.get("value");
        if (rawKey == null)
            return;
        String key = mapKey(rawKey);

        if ("keyDown".equals(actionType)) {
            page.keyboard().down(key);
        } else if ("keyUp".equals(actionType)) {
            page.keyboard().up(key);
        }
    }

    // Map Selenium Keys to Playwright Key Names
    private String mapKey(String s) {
        if (s.length() == 1) {
            char c = s.charAt(0);
            if (c >= '\uE000' && c <= '\uF8FF') {
                switch (c) {
                    case '\uE000':
                        return "null";
                    case '\uE001':
                        return "Cancel"; // ^break
                    case '\uE002':
                        return "Help";
                    case '\uE003':
                        return "Backspace";
                    case '\uE004':
                        return "Tab";
                    case '\uE005':
                        return "Clear";
                    case '\uE006':
                        return "Return";
                    case '\uE007':
                        return "Enter";
                    case '\uE008':
                        return "Shift";
                    case '\uE009':
                        return "Control";
                    case '\uE00A':
                        return "Alt";
                    case '\uE00B':
                        return "Pause";
                    case '\uE00C':
                        return "Escape";
                    case '\uE00D':
                        return "Space";
                    case '\uE00E':
                        return "PageUp";
                    case '\uE00F':
                        return "PageDown";
                    case '\uE010':
                        return "End";
                    case '\uE011':
                        return "Home";
                    case '\uE012':
                        return "ArrowLeft";
                    case '\uE013':
                        return "ArrowUp";
                    case '\uE014':
                        return "ArrowRight";
                    case '\uE015':
                        return "ArrowDown";
                    case '\uE016':
                        return "Insert";
                    case '\uE017':
                        return "Delete";
                    case '\uE018':
                        return ";";
                    case '\uE019':
                        return "=";

                    // Numpad
                    case '\uE01A':
                        return "Numpad0";
                    case '\uE01B':
                        return "Numpad1";
                    case '\uE01C':
                        return "Numpad2";
                    case '\uE01D':
                        return "Numpad3";
                    case '\uE01E':
                        return "Numpad4";
                    case '\uE01F':
                        return "Numpad5";
                    case '\uE020':
                        return "Numpad6";
                    case '\uE021':
                        return "Numpad7";
                    case '\uE022':
                        return "Numpad8";
                    case '\uE023':
                        return "Numpad9";
                    case '\uE024':
                        return "NumpadMultiply";
                    case '\uE025':
                        return "NumpadAdd";
                    case '\uE026':
                        return "NumpadSubtract"; // separator?
                    case '\uE027':
                        return "NumpadSubtract";
                    case '\uE028':
                        return "NumpadDecimal";
                    case '\uE029':
                        return "NumpadDivide";

                    // Function Keys
                    case '\uE031':
                        return "F1";
                    case '\uE032':
                        return "F2";
                    case '\uE033':
                        return "F3";
                    case '\uE034':
                        return "F4";
                    case '\uE035':
                        return "F5";
                    case '\uE036':
                        return "F6";
                    case '\uE037':
                        return "F7";
                    case '\uE038':
                        return "F8";
                    case '\uE039':
                        return "F9";
                    case '\uE03A':
                        return "F10";
                    case '\uE03B':
                        return "F11";
                    case '\uE03C':
                        return "F12";

                    case '\uE03D':
                        return "Meta"; // Command/Meta

                    default:
                        // return "Unidentified"; // Or map more keys
                        return "Unidentified";
                }
            }
        }
        return s;
    }

    private void handlePointerAction(Page page, String actionType, Map<String, Object> parameters,
            Map<String, Object> action) {
        String pointerType = "mouse";
        if (parameters != null && parameters.containsKey("pointerType")) {
            pointerType = (String) parameters.get("pointerType");
        }

        if (!"mouse".equals(pointerType)) {
            // Check if we can support touch or pen via trusted events or mapping
            // For now, logging and falling back to mouse emulation is the safest MVP
            log.trace("Pointer type '{}' requested, falling back to mouse emulation.", pointerType);
        }

        if ("pointerMove".equals(actionType)) {
            handlePointerMove(page, action);
        } else if ("pointerDown".equals(actionType)) {
            handlePointerDown(page, action);
        } else if ("pointerUp".equals(actionType)) {
            handlePointerUp(page, action);
        } else if ("pointerCancel".equals(actionType)) {
            // Best effort cancel - release all buttons
            page.mouse().up();
        }
    }

    private void handlePointerMove(Page page, Map<String, Object> action) {
        Object xObj = action.get("x");
        Object yObj = action.get("y");
        Object originObj = action.get("origin");
        String origin = originObj instanceof String ? (String) originObj : null;

        if (xObj instanceof Number && yObj instanceof Number) {
            double x = ((Number) xObj).doubleValue();
            double y = ((Number) yObj).doubleValue();

            if ("pointer".equals(origin)) {
                // relative move
            } else if ("viewport".equals(origin)) {
                page.mouse().move(x, y);
            } else if (originObj != null && !"viewport".equals(origin) && !"pointer".equals(origin)) {
                // Element origin
                Object unwrapped = unwrap(originObj);
                BoundingBox box = null;

                HubElementRef ref = null;
                if (unwrapped instanceof HubElement element) {
                    unwrapped = element.getRef();
                }
                if (unwrapped instanceof HubElementRef) {
                    ref = (HubElementRef) unwrapped;
                    Object handle = ref.handle();

                    if (handle instanceof Locator) {
                        try {
                            box = ((Locator) handle).boundingBox(new Locator.BoundingBoxOptions().setTimeout(2000));
                        } catch (Exception e) {
                        }
                    } else if (handle instanceof ElementHandle) {
                        try {
                            box = ((ElementHandle) handle).boundingBox();
                        } catch (Exception e) {
                        }
                    }
                } else if (unwrapped instanceof Locator) {
                    try {
                        box = ((Locator) unwrapped).boundingBox(new Locator.BoundingBoxOptions().setTimeout(2000));
                    } catch (Exception e) {
                    }
                } else if (unwrapped instanceof ElementHandle) {
                    try {
                        box = ((ElementHandle) unwrapped).boundingBox();
                    } catch (Exception e) {
                    }
                }

                if (box == null && ref != null) {
                    HubLocator locator = ref.getLocator();
                    if (locator != null) {
                        String selector = provider.toSelector(locator);
                        try {
                            box = page.locator(selector).first()
                                    .boundingBox(new Locator.BoundingBoxOptions().setTimeout(2000));
                        } catch (Exception e) {
                        }
                    }
                }

                if (box != null) {
                    double centerX = box.x + box.width / 2;
                    double centerY = box.y + box.height / 2;
                    page.mouse().move(centerX + x, centerY + y);
                }
            }
        }
    }

    private void handlePointerDown(Page page, Map<String, Object> action) {
        Object b = action.get("button");
        if (b instanceof Number) {
            int code = ((Number) b).intValue();
            MouseButton mb = MouseButton.LEFT;
            if (code == 1)
                mb = MouseButton.MIDDLE;
            if (code == 2)
                mb = MouseButton.RIGHT;
            page.mouse().down(new com.microsoft.playwright.Mouse.DownOptions().setButton(mb));
        } else {
            page.mouse().down();
        }
    }

    private void handlePointerUp(Page page, Map<String, Object> action) {
        Object b = action.get("button");
        if (b instanceof Number) {
            int code = ((Number) b).intValue();
            MouseButton mb = MouseButton.LEFT;
            if (code == 1)
                mb = MouseButton.MIDDLE;
            if (code == 2)
                mb = MouseButton.RIGHT;
            page.mouse().up(new com.microsoft.playwright.Mouse.UpOptions().setButton(mb));
        } else {
            page.mouse().up();
        }
    }

    private void handleWheelAction(Page page, String actionType, Map<String, Object> action) {
        if ("scroll".equals(actionType)) {
            Object deltaX = action.get("deltaX");
            Object deltaY = action.get("deltaY");

            if (deltaX instanceof Number && deltaY instanceof Number) {
                page.mouse().wheel(((Number) deltaX).doubleValue(), ((Number) deltaY).doubleValue());
            }
        }
    }

    private Object unwrap(Object value) {
        if (value == null)
            return null;
        if (value instanceof HubElementRef)
            return value;

        if (value instanceof HubElement) {
            return ((HubElement) value).getRef();
        }

        try {
            Method getRef = value.getClass().getMethod("getRef");
            return getRef.invoke(value);
        } catch (Exception e) {
            // ignore
        }
        return value;
    }
}

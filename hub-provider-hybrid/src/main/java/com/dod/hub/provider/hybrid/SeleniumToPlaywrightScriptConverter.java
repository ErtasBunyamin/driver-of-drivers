package com.dod.hub.provider.hybrid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to convert Selenium-style JavaScript to Playwright-compatible
 * scripts.
 * <p>
 * Selenium scripts often:
 * 1. Use 'arguments[0]', 'arguments[1]' to refer to passed arguments.
 * 2. Rely on explicit 'return' or implicit return of the last statement
 * (sometimes).
 * <p>
 * Playwright scripts:
 * 1. Are run as a function: (args) => { ... }
 * 2. Arguments are passed as a single object/array or destructured.
 * <p>
 * This converter attempts to wrap the Selenium script body into a function that
 * maps 'arguments' correctly.
 */
public class SeleniumToPlaywrightScriptConverter {

    private static final Pattern ARGUMENTS_PATTERN = Pattern.compile("arguments\\[(\\d+)]");

    /**
     * Converts a Selenium script to a Playwright-compatible function body.
     * <p>
     * Input: "return arguments[0].innerText;"
     * Output: "arguments => { return arguments[0].innerText; }"
     * <p>
     * Or more robustly, Playwright accepts a function. We can wrap it:
     * "([arg1, arg2, ...]) => { var arguments = [arg1, arg2, ...]; ... script ...
     * }"
     * <p>
     * But Playwright's `evaluate` takes `expression` and `arg`.
     * If `arg` is a list/array, it's passed as the first argument to the function.
     * <p>
     * Strategy:
     * Wrap the script in:
     * "arguments => { ... original script ... }"
     * And ensure we pass the args as a single array to `evaluate`.
     *
     * @param seleniumScript The raw Selenium script.
     * @return A Playwright-compatible function string.
     */
    public static String convert(String seleniumScript) {
        if (seleniumScript == null)
            return null;

        // If it already looks like a function, return as is (maybe).
        String trimmed = seleniumScript.trim();
        if (trimmed.startsWith("function") || trimmed.startsWith("(")
                || trimmed.indexOf("=>") > 0 && trimmed.indexOf("=>") < 20) {
            // Heuristic: user might have provided a Playwright script already.
            return seleniumScript;
        }

        // Selenium script body. We want to emulate 'arguments' availability.
        // Playwright evaluates: `page.evaluate("args => { ... }", [arg1, arg2])`
        // So 'args' will be the array [arg1, arg2].
        // We can just name the parameter 'arguments'.

        return "arguments => { " + seleniumScript + " }";
    }
}

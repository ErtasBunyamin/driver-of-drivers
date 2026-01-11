package com.dod.hub.core.pipeline;

import com.dod.hub.core.command.CommandResult;
import com.dod.hub.core.command.HubCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Orchestrates the execution of a command.
 * Handles Start -> Stabilize -> Execute -> Stop/Verify -> Error logic.
 */
public class CommandPipeline {
    private static final Logger logger = LoggerFactory.getLogger(CommandPipeline.class);

    /**
     * Executes the provided action within a managed lifecycle.
     * The lifecycle includes logging, error handling, and artifact collection.
     *
     * @param context The execution context containing session and provider data.
     * @param action  The functional strategy to execute.
     * @param <T>     The return type of the action.
     * @return The result of the action execution.
     * @throws RuntimeException If an error occurs during execution.
     */
    public <T> T execute(CommandContext context, Supplier<T> action) {
        HubCommand command = context.getCommand();
        logger.info("[{}] Requesting execution for {}", command.getId(), command.getType());

        try {
            // Future extension: Stabilization logic (e.g., waiting for document readiness)

            T value = action.get();

            logger.info("[{}] Completed execution for {}", command.getId(), command.getType());
            command.complete(CommandResult.success(value));

            return value;

        } catch (Exception e) {
            logger.error("[{}] Execution failed for {}: {}", command.getId(), command.getType(), e.getMessage());
            CommandResult failure = CommandResult.failure(e);

            // Capture diagnostic artifacts on failure
            try {
                byte[] screenshot = context.getProvider().takeScreenshot(context.getSession());
                failure.addArtifact("failure_screenshot", "memory://" + screenshot.length + "_bytes");
            } catch (Exception se) {
                logger.warn("Diagnostic artifact collection failed", se);
            }

            command.complete(failure);
            throw e;
        } finally {
            // Hook for telemetry emission or cleanup
        }
    }
}

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

    // MVP: No retry policy injection yet, straightforward execution.

    public <T> T execute(CommandContext context, Supplier<T> action) {
        HubCommand command = context.getCommand();
        logger.info("[{}] START {}", command.getId(), command.getType());

        try {
            // 1. Stabilize (MVP: Placeholder)
            // performStabilization(context);

            // 2. Execute
            T value = action.get();

            // 3. Success
            logger.info("[{}] SUCCESS {}", command.getId(), command.getType());
            command.complete(CommandResult.success(value));

            // 4. Artifacts (MVP: Only on failure default, so nothing here for now)

            return value;

        } catch (Exception e) {
            // 5. Error
            logger.error("[{}] FAILED {}: {}", command.getId(), command.getType(), e.getMessage());
            CommandResult failure = CommandResult.failure(e);

            // MVP: Capture screenshot on failure
            try {
                byte[] screenshot = context.getProvider().takeScreenshot(context.getSession());
                // In a real app, write to disk. For MVP, we add metadata indicating a
                // screenshot was taken.
                failure.addArtifact("screenshot", "memory://" + screenshot.length + "_bytes");
            } catch (Exception se) {
                logger.warn("Failed to capture screenshot on error", se);
            }

            command.complete(failure);
            throw e; // Re-throw to caller
        } finally {
            // Telemetry emit could go here
        }
    }
}

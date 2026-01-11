package com.dod.hub.core.pipeline;

public class PipelineException extends RuntimeException {
    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}

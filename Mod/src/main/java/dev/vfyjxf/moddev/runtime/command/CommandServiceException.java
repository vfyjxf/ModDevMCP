package dev.vfyjxf.moddev.runtime.command;

public final class CommandServiceException extends RuntimeException {

    private final String errorCode;

    public CommandServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}


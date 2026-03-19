package dev.vfyjxf.moddev.runtime.world;

public final class WorldServiceException extends RuntimeException {

    private final String errorCode;

    public WorldServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}


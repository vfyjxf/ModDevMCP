package dev.vfyjxf.mcp.runtime.command;

public enum CommandType {
    CLIENT("client"),
    SERVER("server");

    private final String side;

    CommandType(String side) {
        this.side = side;
    }

    public String side() {
        return side;
    }
}

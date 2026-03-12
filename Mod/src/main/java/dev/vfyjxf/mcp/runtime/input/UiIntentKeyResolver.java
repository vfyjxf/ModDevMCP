package dev.vfyjxf.mcp.runtime.input;

@FunctionalInterface
interface UiIntentKeyResolver {

    int resolve(String intent);
}

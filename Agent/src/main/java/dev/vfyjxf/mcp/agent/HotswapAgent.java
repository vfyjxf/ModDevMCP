package dev.vfyjxf.mcp.agent;

import java.lang.instrument.Instrumentation;

public final class HotswapAgent {

    private static volatile Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static Instrumentation instrumentation() {
        return instrumentation;
    }
}

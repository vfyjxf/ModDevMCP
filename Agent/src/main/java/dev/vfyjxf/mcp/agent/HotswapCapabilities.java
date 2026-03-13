package dev.vfyjxf.mcp.agent;

import java.lang.instrument.Instrumentation;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HotswapCapabilities {

    private final boolean redefineSupported;
    private final boolean retransformSupported;
    private final boolean enhancedHotswap;
    private final String vmName;

    private HotswapCapabilities(boolean redefineSupported, boolean retransformSupported,
                                boolean enhancedHotswap, String vmName) {
        this.redefineSupported = redefineSupported;
        this.retransformSupported = retransformSupported;
        this.enhancedHotswap = enhancedHotswap;
        this.vmName = vmName;
    }

    public static HotswapCapabilities detect(Instrumentation inst) {
        boolean redefine = inst.isRedefineClassesSupported();
        boolean retransform = inst.isRetransformClassesSupported();
        boolean enhanced = isEnhancedHotswap();
        String vmName = System.getProperty("java.vm.name", "unknown");
        return new HotswapCapabilities(redefine, retransform, enhanced, vmName);
    }

    public static boolean isEnhancedHotswap() {
        String vmName = System.getProperty("java.vm.name", "");
        if (vmName.contains("JBR")) {
            return true;
        }
        String dcevmVersion = System.getProperty("dcevm.version");
        return dcevmVersion != null && !dcevmVersion.isEmpty();
    }

    public boolean canRedefineClasses() {
        return redefineSupported;
    }

    public boolean canAddMethods() {
        return enhancedHotswap;
    }

    public boolean canAddFields() {
        return enhancedHotswap;
    }

    public boolean isEnhanced() {
        return enhancedHotswap;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vmName", vmName);
        map.put("canRedefineClasses", redefineSupported);
        map.put("canRetransformClasses", retransformSupported);
        map.put("enhancedHotswap", enhancedHotswap);
        map.put("canAddMethods", canAddMethods());
        map.put("canAddFields", canAddFields());
        return map;
    }
}

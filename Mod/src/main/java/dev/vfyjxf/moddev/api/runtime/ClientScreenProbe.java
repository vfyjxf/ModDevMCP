package dev.vfyjxf.moddev.api.runtime;

/**
 * Probe that reports the currently active client screen metrics.
 */
public interface ClientScreenProbe {

    /**
     * Returns the most recent live-screen metrics known to the probe.
     */
    ClientScreenMetrics metrics();
}


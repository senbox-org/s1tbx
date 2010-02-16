package com.bc.ceres.core.runtime;

/**
 * A module's state.
 */
public enum ModuleState {
    /**
     * The 'installed' state.
     */
    INSTALLED(0x01),
    /**
     * The 'resolved' state. Implies 'installed'.
     */
    RESOLVED(0x02),
    /**
     * The 'starting' state. Implies 'resolved'.
     */
    STARTING(0x04),
    /**
     * The 'active' state. Implies 'resolved'.
     */
    ACTIVE(0x08),
    /**
     * The 'stopping' state. Implies 'resolved'.
     */
    STOPPING(0x10),
    /**
     * The 'uninstalled' state. Implies 'active' because modules cannot be uninstalled while the runtime is still up and running.
     */
    UNINSTALLED(0x20),
    /**
     * The 'null' state, which means that the state has not been set.
     * Clients will never observe this state.
     */
    NULL(0x00);

    private final int value;

    private ModuleState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean is(ModuleState state) {
        return (value & state.value) != 0;
    }

    public boolean isOneOf(ModuleState state0, ModuleState... states) {
        int result = state0.value;
        for (ModuleState state : states) {
            result |= state.value;
        }
        return (value & result) != 0;
    }
}

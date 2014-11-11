/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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

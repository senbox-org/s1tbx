package com.bc.ceres.core.runtime;

import junit.framework.TestCase;

public class ModuleStateTest extends TestCase {
    public void testIsOneOf() {
        assertFalse(ModuleState.NULL.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.INSTALLED.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.UNINSTALLED.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.RESOLVED.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.STARTING.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.STOPPING.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.NULL.isOneOf(
                ModuleState.NULL,
                ModuleState.INSTALLED,
                ModuleState.UNINSTALLED,
                ModuleState.ACTIVE,
                ModuleState.RESOLVED,
                ModuleState.STARTING,
                ModuleState.STOPPING));



        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.NULL));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.INSTALLED));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.UNINSTALLED));
        assertTrue(ModuleState.ACTIVE.isOneOf(ModuleState.ACTIVE));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.RESOLVED));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.STARTING));
        assertFalse(ModuleState.ACTIVE.isOneOf(ModuleState.STOPPING));
        assertTrue(ModuleState.ACTIVE.isOneOf(
                ModuleState.INSTALLED,
                ModuleState.UNINSTALLED,
                ModuleState.ACTIVE,
                ModuleState.RESOLVED,
                ModuleState.STARTING,
                ModuleState.STOPPING));
        assertFalse(ModuleState.ACTIVE.isOneOf(
                ModuleState.INSTALLED,
                ModuleState.UNINSTALLED,
                ModuleState.RESOLVED,
                ModuleState.STARTING,
                ModuleState.STOPPING));
    }
}

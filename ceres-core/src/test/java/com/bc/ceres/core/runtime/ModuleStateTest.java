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

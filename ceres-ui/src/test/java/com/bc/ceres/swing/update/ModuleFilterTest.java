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

package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.CoreException;
import junit.framework.TestCase;

public class ModuleFilterTest extends TestCase {

    public void testFilter() throws CoreException {
        ModuleItem[] moduleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-c", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-d", "1.0", ModuleState.INSTALLED)
        };

        ModuleItem[] filteredModuleItems;
        filteredModuleItems = ModuleManagerPane.filterModuleItems(moduleItems, "");
        assertSame(filteredModuleItems, moduleItems);

        filteredModuleItems = ModuleManagerPane.filterModuleItems(moduleItems, "Apple");
        assertNotNull(filteredModuleItems);
        assertEquals(3, filteredModuleItems.length);

        filteredModuleItems = ModuleManagerPane.filterModuleItems(moduleItems, "Oran");
        assertNotNull(filteredModuleItems);
        assertEquals(2, filteredModuleItems.length);

        filteredModuleItems = ModuleManagerPane.filterModuleItems(moduleItems, "ain");
        assertNotNull(filteredModuleItems);
        assertEquals(0, filteredModuleItems.length);
    }
}

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

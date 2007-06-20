package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

class ModuleSyncRunner {

    public static ModuleItem[] sync(ModuleItem[] installedModuleItems, Module[] repositoryModules) {

        HashMap<String, ModuleItem> imap = new HashMap<String, ModuleItem>(installedModuleItems.length);
        for (ModuleItem installedModuleItem : installedModuleItems) {
            imap.put(installedModuleItem.getModule().getSymbolicName(), installedModuleItem);
        }

        ArrayList<ModuleItem> syncList = new ArrayList<ModuleItem>(installedModuleItems.length);

        for (Module repositoryModule : repositoryModules) {
            ModuleItem installedItem = imap.get(repositoryModule.getSymbolicName());
            if (installedItem != null) {
                Version installedVersion = Version.parseVersion(installedItem.getModule().getVersion());
                Version availableVersion = Version.parseVersion(repositoryModule.getVersion());
                if (installedItem.getModule().getState().isOneOf(ModuleState.ACTIVE,
                                                                 ModuleState.INSTALLED,
                                                                 ModuleState.RESOLVED)) {
                    if (availableVersion.compareTo(installedVersion) > 0) {
                        if (installedItem.getRepositoryModule() != null) {
                            Version updateVersion = Version.parseVersion(
                                    installedItem.getRepositoryModule().getVersion());
                            if (availableVersion.compareTo(updateVersion) > 0) {
                                installedItem.setRepositoryModule(repositoryModule);
                            }
                        } else {
                            installedItem.setRepositoryModule(repositoryModule);
                        }
                    }
                }
            } else {
                syncList.add(new ModuleItem(repositoryModule));
            }
        }

        return syncList.toArray(new ModuleItem[syncList.size()]);
    }

}

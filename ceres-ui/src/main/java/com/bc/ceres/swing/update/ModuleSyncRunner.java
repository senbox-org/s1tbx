package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Version;

import java.util.HashMap;

class ModuleSyncRunner {

    public static ModuleItem[] sync(ModuleItem[] installedModuleItems, Module[] repositoryModules) {

        HashMap<String, ModuleItem> imap = new HashMap<String, ModuleItem>(installedModuleItems.length);
        for (ModuleItem installedModuleItem : installedModuleItems) {
            imap.put(installedModuleItem.getModule().getSymbolicName(), installedModuleItem);
        }

        HashMap<String, ModuleItem> syncList = new HashMap<String, ModuleItem>(installedModuleItems.length);

        for (Module repositoryModule : repositoryModules) {
            ModuleItem installedItem = imap.get(repositoryModule.getSymbolicName());
            if (installedItem != null) {
                Version installedVersion = installedItem.getModule().getVersion();
                Version availableVersion = repositoryModule.getVersion();
                if (installedItem.getModule().getState().isOneOf(ModuleState.ACTIVE,
                                                                 ModuleState.INSTALLED,
                                                                 ModuleState.RESOLVED)) {
                    if (availableVersion.compareTo(installedVersion) > 0) {
                        if (installedItem.getRepositoryModule() != null) {
                            Version updateVersion = installedItem.getRepositoryModule().getVersion();
                            if (availableVersion.compareTo(updateVersion) > 0) {
                                installedItem.setRepositoryModule(repositoryModule);
                            }
                        } else {
                            installedItem.setRepositoryModule(repositoryModule);
                        }
                    }
                }
            } else {
                ModuleItem moduleItem = syncList.get(repositoryModule.getSymbolicName());
                if (moduleItem != null) {
                    if (moduleItem.getModule().getVersion().compareTo(repositoryModule.getVersion()) < 0) {
                        syncList.put(repositoryModule.getSymbolicName(), new ModuleItem(repositoryModule));

                    }
                } else {
                    syncList.put(repositoryModule.getSymbolicName(), new ModuleItem(repositoryModule));
                }
            }
        }

        return syncList.values().toArray(new ModuleItem[syncList.size()]);
    }

}

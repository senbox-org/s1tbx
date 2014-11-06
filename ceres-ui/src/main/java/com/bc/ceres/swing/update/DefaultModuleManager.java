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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.RuntimeContext;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.RepositoryScanner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class DefaultModuleManager implements ModuleManager {

    private ModuleContext context;
    private URL repositoryUrl;
    private ProxyConfig proxyConfig;
    private Module[] installedModules;
    private ArrayList<File> generatedFileList;
    private ModuleItem[] installedModuleItems;
    private ModuleItem[] updatableModuleItems;
    private ModuleItem[] availableModuleItems;

    public DefaultModuleManager() {
        this(RuntimeContext.getModuleContext());
    }

    public DefaultModuleManager(ModuleContext context) {
        this.context = context;
        this.proxyConfig = ProxyConfig.NULL;
        this.generatedFileList = new ArrayList<File>(8);
    }

    public URL getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(URL repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public Module[] getInstalledModules() {
        if (installedModules == null) {
            installedModules = context.getModules();
        }
        return installedModules;
    }

    public Module[] getRepositoryModules(ProgressMonitor pm) throws CoreException {
        if (repositoryUrl == null) {
            throw new CoreException("Repository URL not set.");
        }
        context.getLogger().info("Connecting repository using " + repositoryUrl);
        RepositoryScanner repositoryScanner = new RepositoryScanner(context.getLogger(), repositoryUrl, proxyConfig);
        return repositoryScanner.scan(pm);
    }

    public ModuleItem[] getInstalledModuleItems() {
        if (installedModuleItems == null) {
            installedModuleItems = toModuleItems(getInstalledModules());
        }
        return installedModuleItems;
    }

    public ModuleItem[] getUpdatableModuleItems() {
        if (updatableModuleItems == null) {
            return new ModuleItem[0];

        }
        return updatableModuleItems;
    }

    public ModuleItem[] getAvailableModuleItems() {
        if (availableModuleItems == null) {
            return new ModuleItem[0];
        }
        return availableModuleItems;
    }

    public void synchronizeWithRepository(ProgressMonitor pm) throws CoreException {
        availableModuleItems = ModuleSyncRunner.sync(getInstalledModuleItems(), getRepositoryModules(pm));
        updatableModuleItems = extractUpdates(getInstalledModuleItems());
    }

    public Module installModule(Module newModule, ProgressMonitor pm) throws CoreException {
        URL location = newModule.getLocation();
        Module installedModule = context.installModule(location, proxyConfig, pm);
        generatedFileList.add(new File(installedModule.getLocation().getPath()));
        return installedModule;
    }

    public Module updateModule(Module oldModule, Module newModule, ProgressMonitor pm) throws CoreException {
        pm.beginTask("Updating module", 100);
        try {
            Module installedModule = installModule(newModule, SubProgressMonitor.create(pm, 50));
            uninstallModule(oldModule, SubProgressMonitor.create(pm, 50));
            return installedModule;
        } finally {
            pm.done();
        }
    }

    public void uninstallModule(Module oldModule, ProgressMonitor pm) throws CoreException {
        oldModule.uninstall(pm);
        File file = new File(oldModule.getLocation().getPath());
        generatedFileList.add(new File(file.getParent(), file.getName() + ".uninstall"));
    }

    public void startTransaction() {
        generatedFileList.clear();
    }

    public void endTransaction() {
        generatedFileList.clear();
    }

    public void rollbackTransaction() {
        for (File file : generatedFileList) {
            try {
                context.getLogger().info(String.format("Module manager rollback: Deleting file [%s]", file));
                if (!file.delete()) {
                    file.deleteOnExit();
                }
            } catch (Exception e) {
                context.getLogger().severe(String.format("Module manager rollback: %s", e.getMessage()));
            }
        }
    }

    private static ModuleItem[] toModuleItems(Module[] modules) {
        ModuleItem[] items = new ModuleItem[modules.length];
        for (int i = 0; i < modules.length; i++) {
            ModuleImpl module = (ModuleImpl) modules[i];
            items[i] = new ModuleItem(module);
        }
        return items;
    }

    private static ModuleItem[] extractUpdates(ModuleItem[] moduleItems) {
        ArrayList<ModuleItem> updates = new ArrayList<ModuleItem>(moduleItems.length);
        for (ModuleItem installedModuleItem : moduleItems) {
            if (installedModuleItem.getRepositoryModule() != null) {
                updates.add(installedModuleItem);
            }
        }
        return updates.toArray(new ModuleItem[updates.size()]);
    }


// ============================
// Reactivate if required
// ============================
//
//    private static final String BEAMIID = "beamuid";
//
//    private URL createHashedRepositoryUrl() {
//        URL url = repositoryUrl;
//        try {
//            if (repositoryUrl.getQuery() == null) {
//                url = createHashedUrl(repositoryUrl);
//            }
//        } catch (Exception e) {
//            // ignore
//        }
//        return url;
//    }
//
//    private static URL createHashedUrl(URL repositoryUrl) throws MalformedURLException, BackingStoreException {
//        if (repositoryUrl.getQuery() == null) {
//            String query = BEAMIID + "=" + getUniqueHash();
//            return new URL(repositoryUrl.toExternalForm() + "?" + query);
//        }
//        return repositoryUrl;
//    }
//
//    private static String getUniqueHash()  {
//        String beamiid;
//        try {
//            Preferences preferences = Preferences.userNodeForPackage(ModuleManager.class);
//            beamiid = preferences.get(BEAMIID, "");
//            if (beamiid.isEmpty()) {
//                beamiid = Long.toHexString(System.nanoTime());
//                preferences.put(BEAMIID, beamiid);
//                preferences.flush();
//            }
//        } catch (Exception e) {
//            beamiid = null;
//        }
//        return beamiid;
//    }
}

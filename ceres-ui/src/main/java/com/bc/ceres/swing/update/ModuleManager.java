package com.bc.ceres.swing.update;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;

import java.net.Proxy;
import java.net.URL;

public interface ModuleManager {

    URL getRepositoryUrl();

    ProxyConfig getProxyConfig();

    Module[] getInstalledModules();

    Module[] getRepositoryModules(ProgressMonitor pm) throws CoreException;

    Module installModule(Module newModule, ProgressMonitor pm) throws CoreException;

    Module updateModule(Module oldModule, Module newModule, ProgressMonitor pm) throws CoreException;

    void uninstallModule(Module oldModule, ProgressMonitor pm) throws CoreException;

    void startTransaction();

    void endTransaction();

    void rollbackTransaction();
}

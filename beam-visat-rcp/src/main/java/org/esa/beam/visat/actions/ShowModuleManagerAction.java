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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.swing.update.ConnectionConfigData;
import com.bc.ceres.swing.update.DefaultModuleManager;
import com.bc.ceres.swing.update.ModuleManagerPane;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This action shows the update module manager
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ShowModuleManagerAction extends ExecCommand {

    // System keys
    private static final String SYS_KEY_BEAM_REPOSITORY_URL = SystemUtils.getApplicationContextId() + ".repository.url";

    // Preferences keys
    private static final String KEY_BEAM_REPOSITORY_PROXY_USED = "beam.repository.proxyUsed";
    private static final String KEY_BEAM_REPOSITORY_PROXY_HOST = "beam.repository.proxy.host";
    private static final String KEY_BEAM_REPOSITORY_PROXY_PORT = "beam.repository.proxy.port";
    private static final String KEY_BEAM_REPOSITORY_PROXY_AUTH_USED = "beam.repository.proxy.authUsed";
    private static final String KEY_BEAM_REPOSITORY_PROXY_USERNAME = "beam.repository.proxy.username";
    private static final String KEY_BEAM_REPOSITORY_PROXY_PASSWORD = "beam.repository.proxy.password";


    private static final String RTSM = "Please check the module repository settings in the preferences dialog.";

    @Override
    public void actionPerformed(final CommandEvent event) {
        ConnectionConfigData connectionConfigData = new ConnectionConfigData();
        transferConnectionData(VisatApp.getApp().getPreferences(), connectionConfigData);

        if (connectionConfigData.getRepositoryUrl().isEmpty()) {
            VisatApp.getApp().showErrorDialog("Module Manager",
                                              "Repository URL not set.\n\n" + RTSM);
            return;
        }


        DefaultModuleManager moduleManager = new DefaultModuleManager();

        URL repositoryUrl;
        try {
            repositoryUrl = getRepositoryUrl(connectionConfigData);
        } catch (MalformedURLException e) {
            VisatApp.getApp().showErrorDialog("Module Manager",
                                              "Malformed repository URL.\n\n" + RTSM);
            return;
        }
        ProxyConfig proxyConfig = getProxyConfig(connectionConfigData);

        moduleManager.setRepositoryUrl(repositoryUrl);
        moduleManager.setProxyConfig(proxyConfig);

        ModuleManagerPane moduleManagerPane = new ModuleManagerPane(moduleManager);
        moduleManagerPane.setRepositoryTroubleShootingMessage(RTSM);
        setEnabled(false);
        Runnable doneHandler = new Runnable() {
            public void run() {
                setEnabled(true);
            }
        };
        moduleManagerPane.showDialog(VisatApp.getApp().getMainFrame(), "Module Manager", doneHandler, new ModuleManagerPane.HelpHandler() {
            public void configureHelpButton(JButton button) {
                HelpSys.enableHelpOnButton(button, getHelpId());
            }
        });
    }

    private URL getRepositoryUrl(ConnectionConfigData connectionConfigData) throws MalformedURLException {
        return new URL(connectionConfigData.getRepositoryUrl());
    }

    private ProxyConfig getProxyConfig(ConnectionConfigData connectionConfigData) {
        return connectionConfigData.isProxyUsed() ? connectionConfigData.getProxyConfig() : ProxyConfig.NULL;
    }

    public static void transferConnectionData(ConnectionConfigData connectionConfigData, PropertyMap propertyMap) {
        ProxyConfig proxyConfig = connectionConfigData.getProxyConfig();
        propertyMap.setPropertyBool(KEY_BEAM_REPOSITORY_PROXY_USED, connectionConfigData.isProxyUsed());
        propertyMap.setPropertyString(KEY_BEAM_REPOSITORY_PROXY_HOST, proxyConfig.getHost());
        propertyMap.setPropertyInt(KEY_BEAM_REPOSITORY_PROXY_PORT, proxyConfig.getPort());
        propertyMap.setPropertyBool(KEY_BEAM_REPOSITORY_PROXY_AUTH_USED, proxyConfig.isAuthorizationUsed());
        propertyMap.setPropertyString(KEY_BEAM_REPOSITORY_PROXY_USERNAME, proxyConfig.getUsername());
        propertyMap.setPropertyString(KEY_BEAM_REPOSITORY_PROXY_PASSWORD, proxyConfig.getScrambledPassword());
    }

    public static void transferConnectionData(PropertyMap propertyMap, ConnectionConfigData connectionConfigData) {
        ProxyConfig proxyConfig = new ProxyConfig();
        connectionConfigData.setProxyConfig(proxyConfig);

        connectionConfigData.setRepositoryUrl(System.getProperty(SYS_KEY_BEAM_REPOSITORY_URL, ""));
        connectionConfigData.setProxyUsed(propertyMap.getPropertyBool(KEY_BEAM_REPOSITORY_PROXY_USED, false));
        proxyConfig.setHost(propertyMap.getPropertyString(KEY_BEAM_REPOSITORY_PROXY_HOST));
        proxyConfig.setPort(propertyMap.getPropertyInt(KEY_BEAM_REPOSITORY_PROXY_PORT));
        proxyConfig.setAuthorizationUsed(propertyMap.getPropertyBool(KEY_BEAM_REPOSITORY_PROXY_AUTH_USED));
        proxyConfig.setUsername(propertyMap.getPropertyString(KEY_BEAM_REPOSITORY_PROXY_USERNAME));
        proxyConfig.setScrambledPassword(propertyMap.getPropertyString(KEY_BEAM_REPOSITORY_PROXY_PASSWORD));
    }
}

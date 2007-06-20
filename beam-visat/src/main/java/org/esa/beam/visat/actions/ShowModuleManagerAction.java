/*
 * $Id: ShowModuleManagerAction.java,v 1.7 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.swing.update.ConnectionConfigData;
import com.bc.ceres.swing.update.ModuleManagerPane;
import com.bc.ceres.swing.update.DefaultModuleManager;
import com.bc.ceres.core.runtime.ProxyConfig;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.framework.help.HelpSys;

import javax.swing.JButton;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * This action shows the update module manager
 *
 * @author Marco Peters
 * @version $Revision: 1.7 $ $Date: 2007/04/18 13:01:13 $
 */
public class ShowModuleManagerAction extends ExecCommand {

    private static final String DEFAULT_REPO = "http://www.brockmann-consult.de/beam/software/repositories/4.0";
    private static final String RTSM = "Please check the module repository settings in the preferences dialog.";

    @Override
    public void actionPerformed(final CommandEvent event) {
        ConnectionConfigData connectionConfigData = new ConnectionConfigData();
        transferConnectionData(VisatApp.getApp().getPreferences(), connectionConfigData);
        DefaultModuleManager moduleManager = new DefaultModuleManager();

        URL repositoryUrl = null;
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
        propertyMap.setPropertyString("beam.repository.url", connectionConfigData.getRepositoryUrl());
        propertyMap.setPropertyBool("beam.repository.proxyUsed", connectionConfigData.isProxyUsed());
        propertyMap.setPropertyString("beam.repository.proxy.host", proxyConfig.getHost());
        propertyMap.setPropertyInt("beam.repository.proxy.port", proxyConfig.getPort());
        propertyMap.setPropertyBool("beam.repository.proxy.authUsed", proxyConfig.isAuthorizationUsed());
        propertyMap.setPropertyString("beam.repository.proxy.username", proxyConfig.getUsername());
        propertyMap.setPropertyString("beam.repository.proxy.password", proxyConfig.getScrambledPassword());
    }

    public static void transferConnectionData(PropertyMap propertyMap, ConnectionConfigData connectionConfigData) {
        ProxyConfig proxyConfig = new ProxyConfig();
        connectionConfigData.setProxyConfig(proxyConfig);

        connectionConfigData.setRepositoryUrl(propertyMap.getPropertyString("beam.repository.url", System.getProperty(
                "beam.repository.url", DEFAULT_REPO)));
        connectionConfigData.setProxyUsed(propertyMap.getPropertyBool("beam.repository.proxyUsed", false));
        proxyConfig.setHost(propertyMap.getPropertyString("beam.repository.proxy.host"));
        proxyConfig.setPort(propertyMap.getPropertyInt("beam.repository.proxy.port"));
        proxyConfig.setAuthorizationUsed(propertyMap.getPropertyBool("beam.repository.proxy.authUsed"));
        proxyConfig.setUsername(propertyMap.getPropertyString("beam.repository.proxy.username"));
        proxyConfig.setScrambledPassword(propertyMap.getPropertyString("beam.repository.proxy.password"));
    }
}

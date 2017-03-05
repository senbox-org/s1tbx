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
package org.esa.snap;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.SystemUtils;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class does not belong to the public API.
 * It is not intended to be used by clients.
 * We only use {@link #loadServices}.</i>
 *
 * @author Marco Peters
 */
public class SnapCoreActivator implements Activator {

    public static boolean isStarted() {
        return false;
    }

    public static <T> void loadServices(ServiceRegistry<T> registry) {
        Iterable<T> iterable = SystemUtils.loadServices(registry.getServiceType());
        final Iterator<T> iterator = iterable.iterator();

        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            try {
                registry.addService(iterator.next());
            } catch (ServiceConfigurationError e) {
                SystemUtils.LOG.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        registerRGBProfiles(moduleContext);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {

    }

    private static void registerRGBProfiles(ModuleContext moduleContext) throws CoreException {
        ExtensionPoint rgbExtensionPoint = moduleContext.getModule().getExtensionPoint("rgbProfiles");
        Extension[] rgbExtensions = rgbExtensionPoint.getExtensions();
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        for (Extension extension : rgbExtensions) {
            ConfigurationElement confElem = extension.getConfigurationElement();
            ConfigurationElement[] rgbElements = confElem.getChildren("rgbProfile");
            for (ConfigurationElement rgbElement : rgbElements) {
                RGBImageProfile rgbImageProfile = new RGBImageProfile();
                rgbImageProfile.configure(rgbElement);
                profileManager.addProfile(rgbImageProfile);
            }
        }
    }
}

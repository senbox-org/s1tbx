/*
 * $Id: BeamCoreActivator.java,v 1.14 2007/03/22 16:56:59 marcop Exp $
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
package org.esa.beam;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ModuleRuntime;
import com.bc.ceres.core.runtime.ModuleState;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RGBImageProfileManager;
import org.esa.beam.util.SystemUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class does not belong to the public API.
 * It is not intended to be used by clients.
 * It is invoked by the {@link ModuleRuntime ceres runtime} to activate the {@code beam-core} module.</i>
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class BeamCoreActivator implements Activator {

    private static ModuleContext moduleContext;

    public static boolean isStarted() {
        return moduleContext != null;
    }

    public static <T> void loadServices(ServiceRegistry<T> registry) {
        Iterable<T> iterable = SystemUtils.loadServices(registry.getServiceType());
        final Iterator<T> iterator = iterable.iterator();

        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            try {
                registry.addService(iterator.next());
            } catch (ServiceConfigurationError e) {
                if (moduleContext != null) {
                    moduleContext.getLogger().log(Level.WARNING, e.getMessage(), e.getCause());
                } else {
                    System.err.print(e.getMessage());    
                }
            }
        }
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        BeamCoreActivator.moduleContext = moduleContext;
        registerRGBProfiles(moduleContext);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        BeamCoreActivator.moduleContext = null;
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

    // todo - move this method elsewhere. ModuleContext or Module?

    public static <T> List<T> loadExecutableExtensions(ModuleContext moduleContext,
                                                       String extensionPointId,
                                                       String elementName,
                                                       Class<T> extensionType) {
        Module module = moduleContext.getModule();
        ExtensionPoint extensionPoint = module.getExtensionPoint(extensionPointId);
        ConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
        List<T> executableExtensions = new ArrayList<T>(32);
        for (ConfigurationElement configurationElement : configurationElements) {
            ConfigurationElement[] children = configurationElement.getChildren(elementName);
            for (ConfigurationElement child : children) {
                try {
                    ModuleState moduleState = child.getDeclaringExtension().getDeclaringModule().getState();
                    if (moduleState.isOneOf(ModuleState.STARTING, ModuleState.RESOLVED)) {
                        T executableExtension = child.createExecutableExtension(extensionType);
                        executableExtensions.add(executableExtension);
                    }
                } catch (CoreException e) {
                    // todo - better throw CoreException? Or better register error in moduleContext?
                    moduleContext.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        return executableExtensions;
    }
//
//    // todo - move this method elsewhere. ModuleContext or Module?
//    public static <T> List<T> loadExecutableExtensions(ModuleContext moduleContext,
//                                                       String extensionPointId,
//                                                       String elementName,
//                                                       String keyElementName,
//                                                       Class<T> extensionType) {
//        Module module = moduleContext.getModule();
//        ExtensionPoint extensionPoint = module.getExtensionPoint(extensionPointId);
//        ConfigurationElement[] configurationElements = keyElementName != null ? extensionPoint.getConfigurationElements(keyElementName) : extensionPoint.getConfigurationElements();
//        List<T> executableExtensions = new ArrayList<T>(32);
//        for (ConfigurationElement configurationElement : configurationElements) {
//            ConfigurationElement[] children = configurationElement.getChildren(elementName);
//            for (ConfigurationElement child : children) {
//                try {
//                    ModuleState moduleState = child.getDeclaringExtension().getDeclaringModule().getState();
//                    if (moduleState.isOneOf(ModuleState.STARTING, ModuleState.RESOLVED)) {
//                        T executableExtension = child.createExecutableExtension(extensionType);
//                        executableExtensions.add(executableExtension);
//                    }
//                } catch (CoreException e) {
//                    // todo - better throw CoreException? Or better register error in moduleContext?
//                    moduleContext.getLogger().log(Level.SEVERE, e.getMessage(), e);
//                }
//            }
//        }
//        return executableExtensions;
//    }
}

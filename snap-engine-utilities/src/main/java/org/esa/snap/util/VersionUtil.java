/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import com.bc.ceres.core.runtime.internal.RuntimeActivator;
import org.esa.beam.util.VersionChecker;

/**
 * check if version of the software is up to date
 */
public class VersionUtil {

    public static String getContextID() {
        if (RuntimeActivator.getInstance() != null
                && RuntimeActivator.getInstance().getModuleContext() != null) {
            return RuntimeActivator.getInstance().getModuleContext().getRuntimeConfig().getContextId();
        }
        return System.getProperty("ceres.context", "nest");
    }

    public static String getRemoteVersionURL(final String appName) {
        final String contextID = getContextID();
        final String arch = System.getProperty("sun.arch.data.model");
        final String os = System.getProperty("os.name").replaceAll(" ", "") + arch;
        final String src = System.getProperty(contextID + ".source");
        String remoteVersionUrl = "http://www.array.ca/nest-web/";
        remoteVersionUrl += "getversion.php?u=" + System.getProperty("user.name") + "&a=" + contextID + appName +
                "&r=" + System.getProperty("user.country") + "&v=" + System.getProperty(contextID + ".version") +
                "&o=" + os + "&s=" + src;
        remoteVersionUrl = remoteVersionUrl.replace(' ', '_');
        return remoteVersionUrl;
    }

    public static void getVersion(final String appName) {
        try {
            // check version
            final VersionChecker versionChecker = new VersionChecker();
            versionChecker.setRemoteVersionUrlString(getRemoteVersionURL(appName));
            versionChecker.getRemoteVersion();
        } catch (Exception e) {
            // ignore
        }
    }
}

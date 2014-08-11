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

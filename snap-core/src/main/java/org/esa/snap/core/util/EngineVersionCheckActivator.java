package org.esa.snap.core.util;

import org.esa.snap.runtime.Activator;

import java.util.logging.Level;

/**
 * @author Marco Peters
 */
public class EngineVersionCheckActivator implements Activator {

    public static final String MSG_UPDATE_INFO = "A new SNAP version is available for download.\nCurrently installed %s, available is %s.\nPlease visit %s\n";
    private static final VersionChecker VERSION_CHECKER = VersionChecker.getInstance();
    private static boolean hasChecked = false;

    @Override
    public void start() {
        if (VERSION_CHECKER.mustCheck()) {
            hasChecked = true;
            if (VERSION_CHECKER.checkForNewRelease()) {
                String localVersion = String.valueOf(VERSION_CHECKER.getLocalVersion());
                String remoteVersion = String.valueOf(VERSION_CHECKER.getRemoteVersion());
                String logMsg = String.format(MSG_UPDATE_INFO, localVersion, remoteVersion, SystemUtils.getApplicationHomepageUrl());
                SystemUtils.LOG.log(Level.WARNING, logMsg);
            }
        }
    }

    @Override
    public void stop() {
        if (hasChecked) {
            VERSION_CHECKER.setChecked();
        }
    }

    @Override
    public int getStartLevel() {
        // no need to start early, others can be served first.
        return 1000;
    }
}

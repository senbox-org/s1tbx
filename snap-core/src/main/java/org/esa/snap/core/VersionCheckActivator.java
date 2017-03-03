package org.esa.snap.core;

import com.bc.ceres.core.runtime.Version;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;
import org.esa.snap.runtime.Engine;
import org.esa.snap.runtime.EngineConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import static org.esa.snap.core.util.SystemUtils.*;

/**
 * @author Marco Peters
 */
public class VersionCheckActivator implements Activator {

    private static final String REMOTE_VERSION_FILE_URL = "http://step.esa.int/downloads/VERSION.txt";
    private static final String PK_CHECK_INTERVAL = "snap.versionCheck.interval";
    private static final String PK_LAST_DATE = "snap.versionCheck.lastDate";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    public static final String STEP_WEB_PAGE = "http://step.esa.int";
    public static final String MSG_UPDATE_INFO = "A new SNAP version is available for download.\nCurrently installed %s, available is %s.\nPlease visit %s";

    private enum CHECK {
        ON_START(0),
        DAILY(1),
        WEEKLY(7),
        MONTHLY(30),
        NEVER(-1);

        private final int days;

        CHECK(int days) {
            this.days = days;
        }

        public boolean exceedsInterval(long daysAgo) {
            return daysAgo > this.days;
        }

    }

    @Override
    public void start() {
        EngineConfig config = Engine.getInstance().getConfig();
        Preferences preferences = config.preferences();

        boolean doCheck = mustCheck(preferences.get(PK_CHECK_INTERVAL, CHECK.DAILY.name()),
                                    preferences.get(PK_LAST_DATE, null));

        if (doCheck) {
            try {
                Version localVersion = getLocalVersion();
                if (localVersion == null) {
                    SystemUtils.LOG.log(Level.WARNING, "Not able to check for new SNAP version. Local version could not be retrieved.");
                }
                Version remoteVersion = getRemoteVersion();
                if (localVersion == null) {
                    SystemUtils.LOG.log(Level.WARNING, "Not able to check for new SNAP version. Remote version could not be retrieved.");
                }
                if (remoteVersion.compareTo(localVersion) > 0) { // Housten, we have an update
                    signalUpdateToUser(localVersion, remoteVersion);
                }
                preferences.put(PK_LAST_DATE, LocalDateTime.now().format(DATE_FORMATTER));
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING, "Not able to check for new SNAP version.", e);
            }
        }

    }

    private void signalUpdateToUser(Version localVersion, Version remoteVersion) {
        Method showInformation = null;
        try {
            Class<?> dialogsClass = Class.forName("org.esa.snap.rcp.util.Dialogs");
            showInformation = dialogsClass.getMethod("showInformation", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }


        if (showInformation != null) {
            String linkText = "<a href=\"" + STEP_WEB_PAGE + "\">" + STEP_WEB_PAGE + "</a>";
            String msg = String.format(MSG_UPDATE_INFO, localVersion.toString(), remoteVersion.toString(), linkText);
            try {
                showInformation.invoke(msg);
            } catch (IllegalAccessException | InvocationTargetException ignore) {
            }
        } else {
            String msg = String.format(MSG_UPDATE_INFO, localVersion.toString(), remoteVersion.toString(), STEP_WEB_PAGE);
            SystemUtils.LOG.log(Level.WARNING, msg);
        }
    }

    private Version getRemoteVersion() throws IOException {
        URL url = new URL(REMOTE_VERSION_FILE_URL);
        return readVersionFromStream(url.openStream());
    }


    private Version getLocalVersion() throws IOException {
        Path versionFile = getApplicationHomeDir().toPath().resolve("VERSION.txt");
        try (InputStream is = Files.newInputStream(versionFile)) {
            return readVersionFromStream(is);
        } catch (IOException e) {
            String msg = String.format("Can not read from version file '%s'.", versionFile);
            throw new IOException(msg, e);
        }
    }

    private Version readVersionFromStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            line = reader.readLine();
            if (line != null) {
                return Version.parseVersion(line.toUpperCase());
            }
        }
        return null;
    }

    private boolean mustCheck(String checkIntervalTxt, String lastDateTxt) {
        CHECK checkInterval = CHECK.valueOf(checkIntervalTxt);
        if (CHECK.NEVER.equals(checkInterval)) {
            return false;
        }
        if (CHECK.ON_START.equals(checkInterval)) {
            return true;
        }

        if (lastDateTxt == null) { // no checked yet, so do it now
            return true;
        } else {
            LocalDateTime lastDate = LocalDateTime.parse(lastDateTxt, DATE_FORMATTER);
            Duration duration = Duration.between(LocalDateTime.now(), lastDate);
            long daysAgo = duration.toDays();
            return checkInterval.exceedsInterval(daysAgo);
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public int getStartLevel() {
        // no need to start early, others can be served first.
        return 1000;
    }
}

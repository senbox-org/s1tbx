/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * A profile used for the creation of RGB images. The profile comprises the band arithmetic expressions
 * for the computation of red, green, blue and alpha (optional) channels of the resulting image.
 */
public class RGBImageProfileManager {

    public static final String DEFAULT_PROFILES_DIR_NAME = "rgb_profiles";

    private static RGBImageProfileManager _instance;
    private final List<RGBImageProfile> _profiles;
    private static File defaultProfilesDir;

    static {
        defaultProfilesDir = new File(SystemUtils.getUserHomeDir(),
                                      ".beam/beam-core/auxdata/" + DEFAULT_PROFILES_DIR_NAME);
        if (!defaultProfilesDir.exists()) {
            defaultProfilesDir.mkdirs();
        }
    }

    private RGBImageProfileManager() {
        _profiles = new ArrayList<RGBImageProfile>();
        loadDefaultProfiles();
    }

    public static File getDefaultProfilesDir() {
        return defaultProfilesDir;
    }

    public static RGBImageProfileManager getInstance() {
        if (_instance == null) {
            _instance = new RGBImageProfileManager();
        }
        return _instance;
    }

    public void addProfile(RGBImageProfile profile) {
        if (profile != null && !_profiles.contains(profile)) {
            _profiles.add(profile);
        }
    }

    public void removeProfile(RGBImageProfile profile) {
        if (profile != null) {
            _profiles.remove(profile);
        }
    }

    public int getProfileCount() {
        return _profiles.size();
    }

    public RGBImageProfile getProfile(int i) {
        return _profiles.get(i);
    }

    public RGBImageProfile[] getAllProfiles() {
        return (RGBImageProfile[]) _profiles.toArray(new RGBImageProfile[_profiles.size()]);
    }

    /**
     * Loads all profiles from the BEAM default RGB profile location.
     */
    private void loadDefaultProfiles() {
        final File[] files = getDefaultProfilesDir().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(RGBImageProfile.FILENAME_EXTENSION) || name.endsWith(
                        RGBImageProfile.FILENAME_EXTENSION.toUpperCase());
            }
        });
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                addProfile(RGBImageProfile.loadProfile(file));
            } catch (IOException e) {
                BeamLogManager.getSystemLogger().log(Level.SEVERE, "Failed to load RGB-image profile " + file, e);
            }
        }
    }
}

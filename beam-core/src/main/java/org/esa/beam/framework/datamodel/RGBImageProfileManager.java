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

    private final List<RGBImageProfile> _profiles;
    private final static File profilesDir;

    static {
        profilesDir = new File(SystemUtils.getApplicationDataDir(),
                               "beam-core/auxdata/rgb_profiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
    }

    private RGBImageProfileManager() {
        _profiles = new ArrayList<RGBImageProfile>();
        loadDefaultProfiles();
    }

    public static File getProfilesDir() {
        return profilesDir;
    }

    public static RGBImageProfileManager getInstance() {
        return Holder.instance;
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
        return _profiles.toArray(new RGBImageProfile[_profiles.size()]);
    }

    /**
     * Loads all profiles from the BEAM default RGB profile location.
     */
    private void loadDefaultProfiles() {
        if (!getProfilesDir().exists()) {
            profilesDir.mkdirs();
            BeamLogManager.getSystemLogger().log(Level.INFO, "Directory for RGB-image profiles not found: " + getProfilesDir());
        }
        final File[] files = getProfilesDir().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(RGBImageProfile.FILENAME_EXTENSION) || name.endsWith(
                        RGBImageProfile.FILENAME_EXTENSION.toUpperCase());
            }
        });
        if (files != null) {
            for (File file : files) {
                try {
                    addProfile(RGBImageProfile.loadProfile(file));
                } catch (IOException e) {
                    BeamLogManager.getSystemLogger().log(Level.SEVERE, "Failed to load RGB-image profile from " + file, e);
                }
            }
        } else {
            BeamLogManager.getSystemLogger().log(Level.INFO, "No RGB-image profiles found in " + getProfilesDir());
        }
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final RGBImageProfileManager instance = new RGBImageProfileManager();
    }
}

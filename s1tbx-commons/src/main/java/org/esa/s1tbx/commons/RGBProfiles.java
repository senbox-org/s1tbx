/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.commons;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

/**
 * RGB Profiles for dual pol and quad pol products
 */
public class RGBProfiles {

    private enum TYPE {RATIO, MULTIPLE, DIFFERENCE}

    static void registerRGBProfiles() {
        final RGBImageProfileManager manager = RGBImageProfileManager.getInstance();

        addProfile(manager, TYPE.RATIO);
        addProfile(manager, TYPE.MULTIPLE);
        addProfile(manager, TYPE.DIFFERENCE);
    }

    private static void addProfile(final RGBImageProfileManager manager, final TYPE profileType) {
        // Intensity
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "HV", ""));
        manager.addProfile(createDPProfile(profileType, "Intensity", "VV", "VH", ""));
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "VV", ""));
        manager.addProfile(createDPProfile(profileType, "Intensity", "RCH", "RCV", ""));
        // Intensity dB
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "HV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Intensity", "VV", "VH", "_db"));
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "VV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Intensity", "RCH", "RCV", "_db"));

        // Sigma0
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "HV", ""));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "VV", "VH", ""));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "VV", ""));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "RCH", "RCV", ""));
        // Sigma0 dB
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "HV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "VV", "VH", "_db"));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "VV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "RCH", "RCV", "_db"));
    }

    private static RGBImageProfile createDPProfile(final TYPE profileType,
                                                   final String name, final String pol1, final String pol2,
                                                   final String suffix) {
        switch (profileType) {
            case RATIO:
                return new RGBImageProfile("Dual Pol Ratio " + name + suffix + ' ' + pol1 + '+' + pol2,
                                           new String[]{
                                                   name + '_' + pol1 + suffix,
                                                   name + '_' + pol2 + suffix,
                                                   name + '_' + pol1 + suffix + '/' + name + '_' + pol2 + suffix
                                           }
                );
            case MULTIPLE:
                return new RGBImageProfile("Dual Pol Multiple " + name + suffix + ' ' + pol1 + '+' + pol2,
                                           new String[]{
                                                   name + '_' + pol1 + suffix,
                                                   name + '_' + pol2 + suffix,
                                                   "abs(" + name + '_' + pol1 + suffix + '*' + name + '_' + pol2 + suffix + ')'
                                           }
                );
            case DIFFERENCE:
                return new RGBImageProfile("Dual Pol Difference " + name + suffix + ' ' + pol1 + '+' + pol2,
                                           new String[]{
                                                   name + '_' + pol2 + suffix,
                                                   name + '_' + pol1 + suffix,
                                                   "abs(" + name + '_' + pol1 + suffix + '-' + name + '_' + pol2 + suffix + ')'
                                           }
                );
            default:
                return null;
        }
    }
}

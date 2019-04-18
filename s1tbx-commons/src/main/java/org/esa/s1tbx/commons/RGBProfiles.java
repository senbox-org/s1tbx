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
        manager.addProfile(new RGBImageProfile("Pauli",
                                               new String[]{
                                                       "((i_HH-i_VV)*(i_HH-i_VV)+(q_HH-q_VV)*(q_HH-q_VV))/2",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/2",
                                                       "((i_HH+i_VV)*(i_HH+i_VV)+(q_HH+q_VV)*(q_HH+q_VV))/2"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Pauli Sigma0",
                                               new String[]{
                                                       "sqrt(Sigma0_HH-Sigma0_VV)",
                                                       "sqrt(Sigma0_HV+Sigma0_VH)",
                                                       "sqrt(Sigma0_HH+Sigma0_VV)"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Pauli Gamma0",
                                                new String[]{
                                                        "sqrt(Gamma0_HH-Gamma0_VV)",
                                                        "sqrt(Gamma0_HV+Gamma0_VH)",
                                                        "sqrt(Gamma0_HH+Gamma0_VV)"
                                                }
        ));
        manager.addProfile(new RGBImageProfile("Pauli Beta0",
                                                new String[]{
                                                        "sqrt(Beta0_HH-Beta0_VV)",
                                                        "sqrt(Beta0_HV+Beta0_VH)",
                                                        "sqrt(Beta0_HH+Beta0_VV)"
                                                }
        ));
        manager.addProfile(new RGBImageProfile("Pauli Intensity",
                                                new String[]{
                                                        "sqrt(Intensity_HH-Intensity_VV)",
                                                        "sqrt(Intensity_HV+Intensity_VH)",
                                                        "sqrt(Intensity_HH+Intensity_VV)"
                                                }
        ));
        manager.addProfile(new RGBImageProfile("Pauli Amplitude",
                                                new String[]{
                                                        "sqrt(Amplitude_HH-Amplitude_VV)",
                                                        "sqrt(Amplitude_HV+Amplitude_VH)",
                                                        "sqrt(Amplitude_HH+Amplitude_VV)"
                                                }
        ));
        manager.addProfile(new RGBImageProfile("Sinclair",
                                               new String[]{
                                                       "i_VV*i_VV+q_VV*q_VV",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/4",
                                                       "i_HH*i_HH+q_HH*q_HH"
                                               }
        ));

        addProfile(manager, TYPE.RATIO);
        addProfile(manager, TYPE.MULTIPLE);
        addProfile(manager, TYPE.DIFFERENCE);
    }

    private static void addProfile(final RGBImageProfileManager manager, final TYPE profileType) {
        // Intensity
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "HV", ""));
        manager.addProfile(createDPProfile(profileType, "Intensity", "VV", "VH", ""));
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "VV", ""));
        // Intensity dB
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "HV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Intensity", "VV", "VH", "_db"));
        manager.addProfile(createDPProfile(profileType, "Intensity", "HH", "VV", "_db"));

        // Sigma0
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "HV", ""));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "VV", "VH", ""));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "VV", ""));
        // Sigma0 dB
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "HV", "_db"));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "VV", "VH", "_db"));
        manager.addProfile(createDPProfile(profileType, "Sigma0", "HH", "VV", "_db"));
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

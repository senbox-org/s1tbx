/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.openide.modules.OnStart;

/**
 * Handle OnStart for module
 */
public class polarimetricModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            registerRGBProfiles();
        }
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("T3",
                new String[]{
                        "T11",
                        "T22",
                        "T33"
                }
        ));
        manager.addProfile(new RGBImageProfile("C3",
                new String[]{
                        "C11",
                        "C22",
                        "C33"
                }
        ));
        manager.addProfile(new RGBImageProfile("T2",
                new String[]{
                        "T11",
                        "T22",
                        "T11/T22"
                }
        ));
        manager.addProfile(new RGBImageProfile("C2",
                new String[]{
                        "C11",
                        "C22",
                        "C11/C22"
                }
        ));

        manager.addProfile(new RGBImageProfile("Sinclair",
                new String[]{
                        "Sinclair_r",
                        "Sinclair_g",
                        "Sinclair_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("Pauli",
                new String[]{
                        "Pauli_r",
                        "Pauli_g",
                        "Pauli_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("Freeman-Durden",
                new String[]{
                        "Freeman_dbl_r",
                        "Freeman_vol_g",
                        "Freeman_surf_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("Yamaguchi",
                new String[]{
                        "Yamaguchi_dbl_r",
                        "Yamaguchi_vol_g",
                        "Yamaguchi_surf_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("VanZyl",
                new String[]{
                        "VanZyl_dbl_r",
                        "VanZyl_vol_g",
                        "VanZyl_surf_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("Cloude",
                new String[]{
                        "Cloude_dbl_r",
                        "Cloude_vol_g",
                        "Cloude_surf_b"
                }
        ));
        manager.addProfile(new RGBImageProfile("H-a Alpha",
                new String[]{
                        "Entropy",
                        "Anisotropy",
                        "Alpha"
                }
        ));
        manager.addProfile(new RGBImageProfile("Touzi",
                new String[]{
                        "Psi",
                        "Tau",
                        "Alpha"
                }
        ));
    }
}

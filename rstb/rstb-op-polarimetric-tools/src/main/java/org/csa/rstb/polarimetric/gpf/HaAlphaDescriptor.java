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
package org.csa.rstb.polarimetric.gpf;

/**
 * Created by lveci on 26/05/2014.
 */
public class HaAlphaDescriptor {

    /* H, A and alpha decision boundaries */
    public static final double Alpha1 = 55.0;
    public static final double Alpha2 = 50.0;
    public static final double Alpha3 = 48.0;
    public static final double Alpha4 = 42.0;
    public static final double Alpha5 = 40.0;
    public static final double H1 = 0.9;
    public static final double H2 = 0.5;
    public static final double A1 = 0.5;            //todo A1 is never used?

    /**
     * Compute zone index (1 to 9) for given entropy and alpha
     *
     * @param entropy                     The entropy
     * @param alpha                       The alpha
     * @param useLeeHAlphaPlaneDefinition Use Lee's H-Alpha plane definition if true, otherwise use PolSARPro definition
     * @return The zone index
     */
    public static int getZoneIndex(final double entropy, final double alpha, final boolean useLeeHAlphaPlaneDefinition) {

        if (useLeeHAlphaPlaneDefinition) {

            if (entropy > H1 && alpha > Alpha1) { // zone 1
                return 1;
            } else if (entropy > H1 && alpha <= Alpha1 && alpha > Alpha5) {  // zone 2
                return 2;
            } else if (entropy > H1 && alpha <= Alpha5) {  // zone 3
                return 3;
            } else if (entropy <= H1 && entropy > H2 && alpha > Alpha2) {  // zone 4
                return 4;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha2 && alpha > Alpha5) {  // zone 5
                return 5;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha5) {  // zone 6
                return 6;
            } else if (entropy <= H2 && alpha > Alpha3) {  // zone 7
                return 7;
            } else if (entropy <= H2 && alpha <= Alpha3 && alpha > Alpha4) {  // zone 8
                return 8;
            } else if (entropy <= H2 && alpha <= Alpha4) {  // zone 9
                return 9;
            }

        } else { // PolSARPro definition

            if (entropy > H1 && alpha > Alpha1) { // zone 7
                return 7;
            } else if (entropy > H1 && alpha <= Alpha1 && alpha > Alpha5) {  // zone 8
                return 8;
            } else if (entropy > H1 && alpha <= Alpha5) {  // zone 9
                return 9;
            } else if (entropy <= H1 && entropy > H2 && alpha > Alpha2) {  // zone 4
                return 4;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha2 && alpha > Alpha5) {  // zone 5
                return 5;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha5) {  // zone 6
                return 6;
            } else if (entropy <= H2 && alpha > Alpha3) {  // zone 1
                return 1;
            } else if (entropy <= H2 && alpha <= Alpha3 && alpha > Alpha4) {  // zone 2
                return 2;
            } else if (entropy <= H2 && alpha <= Alpha4) {  // zone 3
                return 3;
            }
        }
        return 0;
    }
}

/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.examples.data_export;

import java.io.IOException;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

/**
 * Reads and dumps out solar flux for all MERIS L1b bands.
 */
public class SolarFluxEx {

    public static void main(String[] args) {
        try {
            Product product = ProductIO.readProduct("C:/Projects/BEAM/data/MER_RR__1P_A.N1");
            float[] solarFlux = getSolarFlux(product);
            for (int i = 0; i < solarFlux.length; i++) {
                System.out.println("solarFlux[" + i + "] = " + solarFlux[i]);
            }
            product.closeIO();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float[] getSolarFlux(Product product) {
        MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataElement gadsElem = metadataRoot.getElement("Scaling_Factor_GADS");
        MetadataAttribute solarFluxAtt = gadsElem.getAttribute("sun_spec_flux");
        return (float[]) solarFluxAtt.getDataElems();
    }
}

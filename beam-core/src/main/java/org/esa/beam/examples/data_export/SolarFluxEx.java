package org.esa.beam.examples.data_export;

/*
 * $Id: SolarFluxEx.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

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

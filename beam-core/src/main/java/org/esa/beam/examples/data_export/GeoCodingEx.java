package org.esa.beam.examples.data_export;

/*
 * $Id: GeoCodingEx.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

/**
 * Gets and dumps out geo-coding information.
 * It shows how to convert from pixel coordinates to geo coordinates and back.
 * Note that you can import the output of this test program in BEAM application as transect profile.
 */
public class GeoCodingEx {

    public static void main(String[] args) {
        try {
            Product product = ProductIO.readProduct("C:/Projects/BEAM/data/MER_RR__1P_A.N1");
            GeoCoding geoCoding = product.getGeoCoding();
            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();
            PixelPos pixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();

            System.out.println();
            System.out.println("# pixel coordinates --> geo coordinates");
            System.out.println("Pixel-X Pixel-Y Longitude Latitude");

            for (float y = 0.5f; y < height; y += 128) {
                for (float x = 0.5f; x < width; x += 128) {
                    pixelPos.x = x;
                    pixelPos.y = y;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    System.out.println(pixelPos.x + "  " + pixelPos.y + "  " +
                                       geoPos.lon + "  " + geoPos.lat);
                }
            }

            System.out.println();
            System.out.println("# geo coordinates --> pixel coordinates");
            System.out.println("Pixel-X Pixel-Y Longitude Latitude");

            float lon0 = -4;
            float lon1 = 14;
            float lat0 = 55;
            int n = 10;
            for (int i = 0; i < n; i++) {
                geoPos.lon = lon0 + i * (lon1 - lon0) / (n - 1.0f);
                geoPos.lat = lat0;
                geoCoding.getPixelPos(geoPos, pixelPos);
                System.out.println(pixelPos.x + "  " + pixelPos.y + "  " +
                                   geoPos.lon + "  " + geoPos.lat);
            }

            product.dispose();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.support;

import org.esa.beam.binning.DataPeriod;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A tool for analysing the coverage of product related to spatial-data-day
 */
public class SpatialDataDayComputer {

    public static final String DATE_INPUT_PATTERN = "yyyy-MM-dd";


    public static void main(String[] args) throws IOException, ParseException {
        String startDate = args[0];
        Double minDataHour = Double.valueOf(args[1]);
        File directory = new File(args[2]);
        String extension = args[3];

        File[] subdirs = getDirectories(directory);
        List<File> filesList = new ArrayList<>();
        for (File dir : subdirs) {
            Collections.addAll(filesList, getProductFiles(dir, extension));
        }
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        int mode = 3; // 1 precise, 2, basic, 3 both
        Double periodDuration = 1.0;
        ProductData.UTC startUtc = ProductData.UTC.parse(startDate, DATE_INPUT_PATTERN);
        DataPeriod dataPeriod = new SpatialDataPeriod(startUtc.getMJD(), periodDuration, minDataHour);

        System.out.println("startUtc       = " + startUtc);
        System.out.println("minDataHour    = " + minDataHour);
        System.out.println("periodDuration = " + periodDuration);
        System.out.println("num files      = " + filesList.size());
        System.out.println();

        if (mode == 1) {
            DataPeriod.Membership[] memberships = DataPeriod.Membership.values();
            System.out.format("%s\t%s\t%s\t%s\t%s\n", "Product_Name", "num_pixels", memberships[0], memberships[1], memberships[2]);
        } else if (mode == 2) {
            System.out.format("%s\t%s\t%s\t%s\t%s\n", "Product_Name", "top_left", "top_right", "bottom_left", "bottom_right");
        } else if (mode == 3) {
            DataPeriod.Membership[] memberships = DataPeriod.Membership.values();
            System.out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                              "Product_Name", "num_pixels", memberships[0], memberships[1], memberships[2],
                              "top_left", "top_right", "bottom_left", "bottom_right");
        }
        for (File file : filesList) {
            Product product = ProductIO.readProduct(file);
            if (mode == 1) {
                int width = product.getSceneRasterWidth();
                int height = product.getSceneRasterHeight();
                int[] distribution = analysePrecise(dataPeriod, product);
                printPrecise(file.getName(), width, height, distribution);
            } else if (mode == 2) {
                DataPeriod.Membership[] basic = analyseBasic(dataPeriod, product);
                printBasic(file.getName(), basic);
            } else if (mode == 3) {
                int width = product.getSceneRasterWidth();
                int height = product.getSceneRasterHeight();
                DataPeriod.Membership[] basic = analyseBasic(dataPeriod, product);
                int[] distribution = analysePrecise(dataPeriod, product);

                printBooth(file.getName(), width, height, distribution, basic);
            }
            product.dispose();
        }
    }

    private static void printBooth(String productName, int width, int height, int[] distribution, DataPeriod.Membership[] basic) {
        System.out.format("%s\t%d\t%d\t%d\t%d", productName, (width * height), distribution[0], distribution[1], distribution[2]);
        System.out.format("\t%s\t%s\t%s\t%s\n", basic[0], basic[1], basic[2], basic[3]);
    }

    private static void printBasic(String productName, DataPeriod.Membership[] basic) {
        System.out.format("%s\t%s\t%s\t%s\t%s\n", productName, basic[0], basic[1], basic[2], basic[3]);
    }

    private static void printPrecise(String productName, int width, int height, int[] distribution) {
        System.out.format("%s\t%d\t%d\t%d\t%d\n", productName, (width * height), distribution[0], distribution[1], distribution[2]);
    }

    static DataPeriod.Membership[] analyseBasic(DataPeriod dataPeriod, Product product) {
        GeoCoding geoCoding = product.getGeoCoding();
        ProductData.UTC firstScanLineTime = ProductUtils.getScanLineTime(product, 0);
        float firstLon = geoCoding.getGeoPos(new PixelPos(0, 0), null).lon;
        DataPeriod.Membership fl = dataPeriod.getObservationMembership(firstLon, firstScanLineTime.getMJD());
        float lastLon = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, 0), null).lon;
        DataPeriod.Membership fr = dataPeriod.getObservationMembership(lastLon, firstScanLineTime.getMJD());


        ProductData.UTC lastScanLineTime = ProductUtils.getScanLineTime(product, product.getSceneRasterHeight() - 1);
        firstLon = geoCoding.getGeoPos(new PixelPos(0, product.getSceneRasterHeight() - 1), null).lon;
        DataPeriod.Membership ll = dataPeriod.getObservationMembership(firstLon, lastScanLineTime.getMJD());
        lastLon = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null).lon;
        DataPeriod.Membership lr = dataPeriod.getObservationMembership(lastLon, lastScanLineTime.getMJD());

        return new DataPeriod.Membership[]{fl, fr, ll, lr};
    }

    static int[] analysePrecise(DataPeriod dataPeriod, Product product) {
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();
        GeoCoding geoCoding = product.getGeoCoding();
        PixelPos pixelPos = new PixelPos();
        GeoPos geoPos = new GeoPos();
        int[] distribution = new int[DataPeriod.Membership.values().length];
        for (int y = 0; y < height; y++) {
            ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, y);
            double mjd = scanLineTime.getMJD();

            for (int x = 0; x < width; x++) {
                pixelPos.setLocation(x, y);
                geoCoding.getGeoPos(pixelPos, geoPos);
                DataPeriod.Membership membership = dataPeriod.getObservationMembership(geoPos.lon, mjd);
                distribution[membership.ordinal()]++;
            }
        }
        return distribution;
    }

    private static File[] getProductFiles(File directory, final String extension) {
        return directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        });
    }

    private static File[] getDirectories(File directory) {
        return directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
    }
}

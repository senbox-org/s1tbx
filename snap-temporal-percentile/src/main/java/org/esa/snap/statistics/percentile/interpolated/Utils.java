package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DateTimeUtils;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

class Utils {

    static Area createProductArea(Product product) {
        GeneralPath[] boundary = ProductUtils.createGeoBoundaryPaths(product);
        Area area = new Area();
        for (GeneralPath generalPath : boundary) {
            area.add(new Area(generalPath));
        }
        return area;
    }

    static TreeMap<Long, List<Product>> groupProductsDaily(Product[] products) {
        final TreeMap<Long, List<Product>> groupedProducts = new TreeMap<Long, List<Product>>();
        for (Product product : products) {
            final long productMJD = getCenterDateAsModifiedJulianDay(product);
            List<Product> productList = groupedProducts.get(productMJD);
            if (productList == null) {
                productList = new ArrayList<Product>();
                groupedProducts.put(productMJD, productList);
            }
            productList.add(product);
        }
        return groupedProducts;
    }

    static long utcToModifiedJulianDay(Date utc) {
        final double julianDate = DateTimeUtils.utcToJD(utc);
        final double modifiedJulianDate = DateTimeUtils.jdToMJD(julianDate);
        final double modifiedJulianDay = Math.floor(modifiedJulianDate);
        return (long) modifiedJulianDay;
    }

    private static long getCenterDateAsModifiedJulianDay(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        final long endMillies = endTime.getAsDate().getTime();
        final long startMillies = startTime.getAsDate().getTime();
        final long centerMillies = (endMillies - startMillies) / 2 + startMillies;

        final Date centerUTCDate = new Date(centerMillies);
        return utcToModifiedJulianDay(centerUTCDate);
    }

    static void safelyDeleteTree(File tree) {
        Guardian.assertNotNull("tree", tree);

        File[] files = tree.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    safelyDeleteTree(file);
                } else {
                    file.delete();
                    file.deleteOnExit();
                }
            }
        }

        tree.delete();
        tree.deleteOnExit();
    }

}

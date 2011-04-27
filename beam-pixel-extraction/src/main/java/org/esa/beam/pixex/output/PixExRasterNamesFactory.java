package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PixExRasterNamesFactory implements RasterNamesFactory {

    private final boolean exportBands;
    private final boolean exportTiePoints;
    private final boolean exportMasks;
    private final Map<String, String[]> rasterNamesMap;

    public PixExRasterNamesFactory(boolean exportBands, boolean exportTiePoints, boolean exportMasks) {
        this.exportBands = exportBands;
        this.exportTiePoints = exportTiePoints;
        this.exportMasks = exportMasks;
        rasterNamesMap = new HashMap<String, String[]>(37);
    }

    @Override
    public String[] getRasterNames(Product product) {
        final String productType = product.getProductType();
        if (rasterNamesMap.containsKey(productType)) {
            return rasterNamesMap.get(productType);
        } else {
            final String[] rasterNames = extractRasterNames(product);
            rasterNamesMap.put(productType, rasterNames);
            return rasterNames;
        }
    }

    private String[] extractRasterNames(Product product) {

        final List<String> allNamesList = new ArrayList<String>();
        if (exportBands) {
            Collections.addAll(allNamesList, product.getBandNames());
        }
        if (exportTiePoints) {
            Collections.addAll(allNamesList, product.getTiePointGridNames());
        }
        if (exportMasks) {
            Collections.addAll(allNamesList, product.getMaskGroup().getNodeNames());
        }
        return allNamesList.toArray(new String[allNamesList.size()]);
    }

}

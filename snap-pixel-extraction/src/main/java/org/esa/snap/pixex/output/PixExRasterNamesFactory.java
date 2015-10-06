package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.pixex.aggregators.AggregatorStrategy;

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
    private final AggregatorStrategy aggregatorStrategy;

    public PixExRasterNamesFactory(boolean exportBands, boolean exportTiePoints, boolean exportMasks,
                                   AggregatorStrategy aggregatorStrategy) {
        this.exportBands = exportBands;
        this.exportTiePoints = exportTiePoints;
        this.exportMasks = exportMasks;
        this.rasterNamesMap = new HashMap<>(37);
        this.aggregatorStrategy = aggregatorStrategy;
    }

    @Override
    public String[] getRasterNames(Product product) {
        final String productType = product.getProductType();
        if (rasterNamesMap.containsKey(productType)) {
            return rasterNamesMap.get(productType);
        } else {
            final String[] rasterNames = extractRasterNames(product, aggregatorStrategy);
            rasterNamesMap.put(productType, rasterNames);
            return rasterNames;
        }
    }

    @Override
    public String[] getUniqueRasterNames(Product product) {
        return extractRasterNames(product, null);
    }


    private String[] extractRasterNames(Product product, AggregatorStrategy strategy) {
        final List<String> allNamesList = new ArrayList<>();
        if (exportBands) {
            Collections.addAll(allNamesList, product.getBandNames());
        }
        if (exportTiePoints) {
            Collections.addAll(allNamesList, product.getTiePointGridNames());
        }
        if (exportMasks) {
            Collections.addAll(allNamesList, product.getMaskGroup().getNodeNames());
        }
        if (strategy == null) {
            return allNamesList.toArray(new String[allNamesList.size()]);
        }
        String[] allNamesWithSuffixes = new String[allNamesList.size() * strategy.getValueCount()];
        int index = 0;
        for (String name : allNamesList) {
            for (String suffix : aggregatorStrategy.getSuffixes()) {
                allNamesWithSuffixes[index++] = name + "_" + suffix;
            }
        }
        return allNamesWithSuffixes;
    }

}

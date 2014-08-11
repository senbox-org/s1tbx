package org.esa.beam.framework.dataio;

import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * a cache of recently used Products
 */
public final class ProductCache {
    private final static int cacheSize = 200;
    private final Map<File, Product> productMap = new HashMap<File, Product>(cacheSize);
    private final Map<File, Long> timeStampMap = new HashMap<File, Long>(cacheSize);
    private final ArrayList<File> fileList = new ArrayList<File>(cacheSize);
    private static final ProductCache theInstance = new ProductCache();

    private ProductCache() {
    }

    public static ProductCache instance() {
        return theInstance;
    }

    public synchronized void addProduct(final File file, final Product p) {
        productMap.put(file, p);
        timeStampMap.put(file, file.lastModified());

        fileList.remove(file);
        fileList.add(0, file);
        if (fileList.size() > cacheSize) {
            final int index = fileList.size() - 1;
            final File lastFile = fileList.get(index);
            productMap.remove(lastFile);
            fileList.remove(index);
        }
    }

    public Product getProduct(final File file) {
        final Product prod = productMap.get(file);
        if(prod != null && prod.getFileLocation() != null && file.lastModified() == timeStampMap.get(file))
            return prod;
        return null;
    }

    public synchronized void removeProduct(final File file) {
        if(file == null) return;
        productMap.remove(file);
        fileList.remove(file);
        timeStampMap.remove(file);
    }

    public synchronized void clearCache() {
        final ArrayList<File> toDelete = (ArrayList<File>)fileList.clone();
        for(File f : toDelete) {
            final Product prod = productMap.get(f);
            if(prod != null)
                prod.dispose();
        }
        toDelete.clear();
        productMap.clear();
        fileList.clear();
        timeStampMap.clear();
    }

}

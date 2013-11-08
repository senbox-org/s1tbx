package org.esa.nest.db;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.db.DBQuery;
import org.esa.nest.db.ProductDB;
import org.esa.nest.db.ProductEntry;

import java.io.File;
import java.util.*;

/**

 */
public class DBSearch {

    public static ProductEntry[] search(final File srcFile) throws Exception {

        return search(ProductIO.readProduct(srcFile));
    }

    public static ProductEntry[] search(final Product srcProduct) throws Exception {

        final GeoPos centerGeoPos = srcProduct.getGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth()/2, srcProduct.getSceneRasterHeight()/2), null);
        final ProductEntry masterEntry = new ProductEntry(srcProduct);
        return findCCDPairs(ProductDB.instance(), masterEntry, centerGeoPos, 1, false);
    }

    private static ProductEntry[] findCCDPairs(final ProductDB db, final ProductEntry master, final GeoPos centerGeoPos,
                                               final int maxSlaves, final boolean anyDate) {

        final DBQuery dbQuery = new DBQuery();
        dbQuery.setFreeQuery(AbstractMetadata.PRODUCT+" <> '"+master.getName()+ '\'');
        dbQuery.setSelectionRect(new GeoPos[] { centerGeoPos });
        dbQuery.setSelectedPass(master.getPass());
        dbQuery.setSelectedProductTypes(new String[] {master.getProductType()});
        try {
            final ProductEntry[] entries = dbQuery.queryDatabase(db);
            return getClosestDatePairs(entries, master, dbQuery, maxSlaves, anyDate);
        } catch(Throwable t) {
            VisatApp.getApp().showErrorDialog("Query database error:"+t.getMessage());
            return null;
        }
    }

    private static ProductEntry[] getClosestDatePairs(final ProductEntry[] entries,
                                                      final ProductEntry master, DBQuery dbQuery,
                                                      final int maxSlaves, final boolean anyDate) {
        final double masterTime = master.getFirstLineTime().getMJD();
        double cutoffTime = masterTime;
        if(dbQuery != null && dbQuery.getEndDate() != null) {
            final double endTime = ProductData.UTC.create(dbQuery.getEndDate().getTime(), 0).getMJD();
            if(endTime > masterTime)
                cutoffTime = endTime;
        }

        final List<ProductEntry> resultList = new ArrayList<ProductEntry>(maxSlaves);
        final Map<Double, ProductEntry> timesMap = new HashMap<Double, ProductEntry>();
        final List<Double> diffList = new ArrayList<Double>();
        // find all before masterTime
        for(ProductEntry entry : entries) {
            final double entryTime = entry.getFirstLineTime().getMJD();
            if(anyDate || entryTime < cutoffTime) {
                final double diff = Math.abs(masterTime - entryTime);
                timesMap.put(diff, entry);
                diffList.add(diff);
            }
        }
        Collections.sort(diffList);
        // select only the closest up to maxPairs
        for(Double diff : diffList) {
            resultList.add(timesMap.get(diff));
            if(resultList.size() >= maxSlaves)
                break;
        }

        return resultList.toArray(new ProductEntry[resultList.size()]);
    }
}

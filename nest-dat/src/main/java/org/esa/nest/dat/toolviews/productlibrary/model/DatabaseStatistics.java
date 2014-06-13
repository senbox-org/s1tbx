package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.db.ProductEntry;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates statistic on a product entry list
 */
public class DatabaseStatistics implements DatabaseQueryListener {

    final Map<Integer, Integer> yearMap = new HashMap<>();
    final Map<Integer, Integer> monthMap = new HashMap<>();

    public void notifyNewProductEntryListAvailable(final ProductEntry[] entryList) {
        updateStats(entryList);
    }

    public void notifyNewMapSelectionAvailable() {

    }

    private void updateStats(final ProductEntry[] entryList) {
        for (ProductEntry entry : entryList) {
            final ProductData.UTC utc = entry.getFirstLineTime();

            int year = utc.getAsCalendar().get(Calendar.YEAR);
            Integer yearCnt = yearMap.get(year);
            if (yearCnt == null) {
                yearMap.put(year, 1);
            } else {
                yearMap.put(year, yearCnt + 1);
            }

            int month = utc.getAsCalendar().get(Calendar.MONTH);
            Integer monthCnt = monthMap.get(month);
            if (monthCnt == null) {
                monthMap.put(month, 1);
            } else {
                monthMap.put(month, monthCnt + 1);
            }
        }

        showStats();
    }

    private void showStats() {
        final Set<Integer> years = yearMap.keySet();
        for (Integer y : years) {
            System.out.println(y + " : " + yearMap.get(y));
        }

        final Set<Integer> months = monthMap.keySet();
        for (Integer m : months) {
            System.out.println(m + " : " + monthMap.get(m));
        }
    }
}

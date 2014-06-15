package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dat.toolviews.productlibrary.DatabasePane;
import org.esa.nest.db.ProductEntry;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates statistic on a product entry list
 */
public class DatabaseStatistics implements DatabaseQueryListener {

    private final DatabasePane dbPane;
    private final Map<Integer, YearData> yearDataMap = new HashMap<>(30);
    private final Map<Integer, Integer> yearMap = new HashMap<>(30);
    private final Map<Integer, Integer> monthMap = new HashMap<>(12);

    public DatabaseStatistics(final DatabasePane dbPane) {
        this.dbPane = dbPane;
        dbPane.addListener(this);
    }

    public void notifyNewEntryListAvailable() {
        updateStats(dbPane.getProductEntryList());
    }

    public void notifyNewMapSelectionAvailable() {

    }

    private void updateStats(final ProductEntry[] entryList) {
        yearDataMap.clear();
        yearMap.clear();
        monthMap.clear();

        for (ProductEntry entry : entryList) {
            final ProductData.UTC utc = entry.getFirstLineTime();

            final int year = utc.getAsCalendar().get(Calendar.YEAR);
            final Integer yearCnt = yearMap.get(year);
            if (yearCnt == null) {
                yearMap.put(year, 1);
            } else {
                yearMap.put(year, yearCnt + 1);
            }

            YearData data = yearDataMap.get(year);
            if (data == null) {
                data = new YearData(year);
                yearDataMap.put(year, data);
            }
            data.cnt += 1;
            final int dayOfYear = utc.getAsCalendar().get(Calendar.DAY_OF_YEAR);
            final Integer dayOfYearCnt = data.dayOfYearMap.get(year);
            if (dayOfYearCnt == null) {
                data.dayOfYearMap.put(dayOfYear, 1);
            } else {
                data.dayOfYearMap.put(dayOfYear, dayOfYearCnt + 1);
            }

            final int month = utc.getAsCalendar().get(Calendar.MONTH);
            final Integer monthCnt = monthMap.get(month);
            if (monthCnt == null) {
                monthMap.put(month, 1);
            } else {
                monthMap.put(month, monthCnt + 1);
            }
        }

        showStats();
    }

    public Map<Integer, YearData> getYearData() {
        return yearDataMap;
    }

    public Map<Integer, Integer> getYearStats() {
        return yearMap;
    }

    public Map<Integer, Integer> getMonthStats() {
        return monthMap;
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

    public static class YearData {
        public final int year;
        public int cnt = 0;
        public final Map<Integer, Integer> dayOfYearMap = new HashMap<>(365);

        YearData(final int year) {
            this.year = year;

            // init dayOfYear
            for (int d = 1; d < 366; ++d) {
                dayOfYearMap.put(d, 0);
            }
        }
    }
}

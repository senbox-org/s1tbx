package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dat.toolviews.productlibrary.DatabasePane;
import org.esa.nest.db.ProductEntry;

import java.util.*;

/**
 * Calculates statistic on a product entry list
 */
public class DatabaseStatistics implements DatabaseQueryListener {

    private final DatabasePane dbPane;
    private final Map<Integer, YearData> yearDataMap = new HashMap<>(30);
    private Integer overallMaxYearCnt = 0;
    private Integer overallMaxDayCnt = 0;
    private MonthData monthData;

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
        if(entryList == null)
            return;
        yearDataMap.clear();
        monthData = new MonthData();

        for (ProductEntry entry : entryList) {
            final ProductData.UTC utc = entry.getFirstLineTime();

            final int year = utc.getAsCalendar().get(Calendar.YEAR);
            YearData yData = yearDataMap.get(year);
            if (yData == null) {
                yData = new YearData(year);
                yearDataMap.put(year, yData);
            }
            final int dayOfYear = utc.getAsCalendar().get(Calendar.DAY_OF_YEAR);
            yData.addDayOfYear(dayOfYear);

            final int month = utc.getAsCalendar().get(Calendar.MONTH);
            monthData.add(month);
        }

        // find highest year count
        overallMaxYearCnt = 0;
        overallMaxDayCnt = 0;
        for(Integer year : yearDataMap.keySet()) {
            final YearData yData = yearDataMap.get(year);
            int cnt = yData.yearCnt;
            int dayCnt = yData.maxDayCnt;
            if(cnt > overallMaxYearCnt) {
                overallMaxYearCnt = cnt;
            }
            if(dayCnt > overallMaxDayCnt) {
                overallMaxDayCnt = dayCnt;
            }
        }

        //showStats();
    }

    public Map<Integer, YearData> getYearData() {
        return yearDataMap;
    }

    public MonthData getMonthData() {
        return monthData;
    }

    public int getOverallMaxYearCnt() {
        return overallMaxYearCnt;
    }

    public int getOverallMaxDayCnt() {
        return overallMaxDayCnt;
    }

    private void showStats() {
        final SortedSet<Integer> years = new TreeSet<>(yearDataMap.keySet());
        System.out.print("Year: ");
        for (Integer y : years) {
            System.out.print(y + "= " + yearDataMap.get(y).yearCnt + "  ");
        }
        System.out.println();

        final Set<Integer> months = monthData.getMonthSet();
        System.out.print("Month: ");
        for (Integer m : months) {
            System.out.print(m + "= " + monthData.get(m) + "  ");
        }
        System.out.println();

        for (Integer y : years) {
            final Map<Integer, Integer> dayOfYear = yearDataMap.get(y).dayOfYearMap;
            final Set<Integer> days = dayOfYear.keySet();
            System.out.print(y+ ": ");
            for(Integer d : days) {
                Integer dayCnt = dayOfYear.get(d);
                if(dayCnt != 0) {
                    System.out.print(d + "=" + dayCnt + " ");
                }
            }
            System.out.println();
        }
    }

    public static class YearData {
        public final int year;
        public int yearCnt = 0;
        public int maxDayCnt = 0;
        public final Map<Integer, Integer> dayOfYearMap = new HashMap<>(365); // starts from 1

        YearData(final int year) {
            this.year = year;

            // init dayOfYear
            for (int d = 1; d < 366; ++d) {
                dayOfYearMap.put(d, 0);
            }
        }

        void addDayOfYear(final int dayOfYear) {
            Integer dayOfYearCnt = dayOfYearMap.get(dayOfYear);
            if (dayOfYearCnt == null) {
                dayOfYearCnt = 1;
            } else {
                dayOfYearCnt += 1;
            }
            dayOfYearMap.put(dayOfYear, dayOfYearCnt);

            // save max day cnt per year
            if(dayOfYearCnt > maxDayCnt) {
                maxDayCnt = dayOfYearCnt;
            }
            yearCnt += 1;
        }
    }

    public static class MonthData {
        private int maxMonthCnt = 0;
        private final Map<Integer, Integer> monthMap = new HashMap<>(12); // starts from 0

        MonthData() {
            //init months to 0
            for (int m = 0; m < 12; ++m) {
                monthMap.put(m, 0);
            }
        }

        public void add(final Integer month) {
            Integer monthCnt = monthMap.get(month);
            if(monthCnt != null) {
                monthCnt += 1;
                if(monthCnt > maxMonthCnt ) {
                    maxMonthCnt = monthCnt;
                }
                monthMap.put(month, monthCnt);
            }
        }

        public Set<Integer> getMonthSet() {
            return monthMap.keySet();
        }

        public Integer get(Integer m) {
            return monthMap.get(m);
        }

        public int getMaxMonthCnt() {
            return maxMonthCnt;
        }
    }
}

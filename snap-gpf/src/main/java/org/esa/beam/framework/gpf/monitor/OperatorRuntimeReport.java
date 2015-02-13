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

package org.esa.beam.framework.gpf.monitor;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.internal.OperatorContext;
import org.esa.beam.framework.gpf.internal.OperatorImage;
import org.esa.beam.framework.gpf.internal.OperatorImageTileStack;
import org.esa.beam.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Print  a report on a per image basis.
 */
public class OperatorRuntimeReport extends TileComputationObserver {

    private final List<TileComputationEvent> recordedEventList = Collections.synchronizedList(new LinkedList<TileComputationEvent>());
    private final Map<StatKey, StatValue> allStats = new HashMap<>();
    private final Map<OperatorContext, Band> firstImage = new HashMap<>();

    @Override
    public void start() {
    }

    @Override
    public void tileComputed(TileComputationEvent event) {
        recordedEventList.add(event);
    }

    @Override
    public void stop() {
        if (recordedEventList.size() == 0) {
            return;
        }
        long totalNettoTime = 0L;
        long startNanosMin = Long.MAX_VALUE;
        long endNanosMax = Long.MIN_VALUE;

        for (TileComputationEvent event : recordedEventList) {
            startNanosMin = Math.min(startNanosMin, event.getStartNanos());
            endNanosMax = Math.max(endNanosMax, event.getEndNanos());

            OperatorImage opImage = event.getImage();
            OperatorContext operatorContext = opImage.getOperatorContext();
            Band targetBand = opImage.getTargetBand();
            String bandName = targetBand.getName();

            long bruttoNanos = event.getEndNanos() - event.getStartNanos();
            long nettoNanos = event.getNettoNanos();

            final StatKey statKey;
            boolean addTime = true;
            if (opImage instanceof OperatorImageTileStack) {

                Band firstBand = firstImage.get(operatorContext);
                if (firstBand == null) {
                    firstImage.put(operatorContext, targetBand);
                    firstBand = targetBand;
                }
                if (targetBand != firstBand) {
                    addTime = false;
                }
                statKey = new StatKey(operatorContext, "ALL");
            } else {
                statKey = new StatKey(operatorContext, "ALL");
//                statKey = new StatKey(operatorContext, bandName);
            }

            StatValue imageStat = allStats.get(statKey);
            if (imageStat == null) {
                String opType = getOpType(opImage);
                String opAlias = getOpAlias(operatorContext);
                String opClass = getOpClass(operatorContext);
                imageStat = new StatValue(opType, opAlias, opClass);
                allStats.put(statKey, imageStat);
            }
            if (addTime) {
                imageStat.add(bruttoNanos, nettoNanos, bandName);
                totalNettoTime += nettoNanos;
            } else {
                imageStat.add(0, 0, bandName);
            }
        }
        List<StatValue> values = new ArrayList<>(allStats.values());
        Collections.sort(values, new Comparator<StatValue>() {
            @Override
            public int compare(StatValue o1, StatValue o2) {
                return Long.compare(o1.nettoNanos, o2.nettoNanos);
            }
        });

        int[] colWidth = new int[7];

        for (StatValue stat : values) {
            String[] strings = stat.toStringArray(totalNettoTime);
            for (int i = 0; i < strings.length; i++) {
                colWidth[i] = Math.max(colWidth[i], strings[i].length());
            }
        }
        int colWidthSum = 5 * 3 + 1 + 10;
        for (int i = 0; i < colWidth.length - 1; i++) {
            colWidthSum += colWidth[i];
        }
        String headerFormat= "%-" + colWidth[0] + "s | %-" + colWidth[1] + "s | %-" + colWidth[2] + "s | %-" + colWidth[3] + "s | %-" + colWidth[4] + "s | %-" + colWidth[5] + "s  | %-" + colWidth[6] + "s\n";
        String rowFormat= "%-" + colWidth[0] + "s | %-" + colWidth[1] + "s | %-" + colWidth[2] + "s | %" + colWidth[3] + "s | %" + colWidth[4] + "s | %" + colWidth[5] + "s%% | %-" + colWidth[6] + "s\n";

        System.out.println();
        System.out.format(headerFormat, "op Alias", "op Class", "", "brutto", "netto", "", "bands");
        for (int i = 0; i < colWidthSum; i++) {
            System.out.print("=");
        }
        System.out.println();
        for (StatValue stat : values) {
            System.out.format(rowFormat, stat.toStringArray(totalNettoTime));
        }
        System.out.println();
        System.out.format("computation time: %,8d ms\n", asMillis(totalNettoTime));
        System.out.format("wall clock time:  %,8d ms\n", asMillis(endNanosMax - startNanosMin));
    }

    private static String getOpAlias(OperatorContext operatorContext) {
        return operatorContext.getOperatorSpi().getOperatorDescriptor().getAlias();
    }

    private static String getOpClass(OperatorContext operatorContext) {
        return operatorContext.getOperator().getClass().getName();
    }

    private static String getOpType(OperatorImage opImage) {
        if (opImage instanceof OperatorImageTileStack) {
            return "STACK";
        } else {
            return "TILE";
        }
    }

    private static long asMillis(long bruttoNanos) {
        return bruttoNanos / (1000 * 1000);
    }

    private static final class StatValue {
        private final Set<String> bandNameSet = new HashSet<>();
        private final String opType;
        private final String opAlias;
        private final String opClass;
        private long bruttoNanos = 0L;
        private long nettoNanos = 0L;

        StatValue(String opType, String opAlias, String opClass) {
            this.opType = opType;
            this.opAlias = opAlias;
            this.opClass = opClass;
        }

        void add(long bruttoNanos, long nettoNanos, String bandName) {
            this.bruttoNanos += bruttoNanos;
            this.nettoNanos += nettoNanos;
            bandNameSet.add(bandName);
        }

        String[] toStringArray(long totalNettoTime) {
            String[] strings = new String[7];
            strings[0] = opAlias;
            strings[1] = opClass;
            strings[2] = opType;
            strings[3] = String.format("%d", asMillis(bruttoNanos));
            strings[4] = String.format("%d", asMillis(nettoNanos));
            strings[5] = String.format("%.2f", 100.0f * nettoNanos / totalNettoTime);
            ArrayList<String> nameList = new ArrayList<>(bandNameSet);
            Collections.sort(nameList);
            strings[6] = StringUtils.join(nameList, ",");
            return strings;
        }
    }

    private static final class StatKey {
        private final OperatorContext operatorContext;
        private final String bandname;

        public StatKey(OperatorContext operatorContext, String bandname) {
            this.operatorContext = operatorContext;
            this.bandname = bandname;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StatKey statKey = (StatKey) o;

            if (!bandname.equals(statKey.bandname)) return false;
            if (!operatorContext.equals(statKey.operatorContext)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = operatorContext.hashCode();
            result = 31 * result + bandname.hashCode();
            return result;
        }
    }

}

/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.internal;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Operator;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * A recorder for tile computation events.
 * May be used as a value for the 'beam.config' variable 'beam.gpf.tileComputationHandler'.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public class SimpleTileComputationHandler extends TileComputationHandler {
    public static final int CHART_WIDTH = 1500;
    private final List<TileComputationEvent> recordedEventList = Collections.synchronizedList(new LinkedList<TileComputationEvent>());

    @Override
    public void start() {
        getLogger().info("Starting observation of tile computation events.");
    }

    @Override
    public void tileComputed(TileComputationEvent event) {
        recordedEventList.add(event);
    }

    @Override
    public void stop() {
        getLogger().info("Stopping observation of tile computation events.");
        File cwd = new File(".");
        File[] files = cwd.listFiles(new VmFilenameFilter());
        if (files == null || files.length == 0) {
            getLogger().warning(String.format("No Velocity template files (*.vm) found in %s", cwd.getAbsolutePath()));
            return;
        }

        TileComputationEvent[] events = recordedEventList.toArray(new TileComputationEvent[0]);
        Arrays.sort(events, new GroupingComparator(
                new StartTimeComparator(),
                new TileIndicesComparator()
        ));
        long startNanosMin = Long.MAX_VALUE;
        long endNanosMax = Long.MIN_VALUE;
        for (TileComputationEvent event : events) {
            startNanosMin = Math.min(startNanosMin, event.getStartNanos());
            endNanosMax = Math.max(endNanosMax, event.getEndNanos());
        }
        Task[] tasks = new Task[events.length];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new Task(events[i], startNanosMin, endNanosMax);
        }

        VelocityEngine ve = new VelocityEngine();
        try {
            ve.init();
        } catch (Exception e) {
            getLogger().warning(String.format("Failed to initialise Velocity engine: %s", e.getMessage()));
            return;
        }

        VelocityContext context = new VelocityContext();
        context.put("docTitle", SimpleTileComputationHandler.class.getName());
        context.put("chartWidth", CHART_WIDTH);
        context.put("totalTime", nanosToRoundedSecs(endNanosMax - startNanosMin));
        context.put("events", events);
        context.put("tasks", tasks);

        for (File file : files) {
            String templateName = file.getName();
            String outputName = templateName.substring(0, templateName.lastIndexOf('.'));
            try {
                Template temp = ve.getTemplate(templateName);
                Writer writer = new FileWriter(outputName);
                try {
                    temp.merge(context, writer);
                } finally {
                    writer.close();
                }
                getLogger().info(String.format("%s written.", outputName));
            } catch (Exception e) {
                getLogger().warning(String.format("Failed to process Velocity template %s: %s", templateName, e.getMessage()));
            }
        }

    }

    private static double nanosToRoundedSecs(long nanos) {
        double secs = nanos * 1.0E-9;
        return Math.round(1000.0 * secs) / 1000.0;
    }

    public static class GroupingComparator implements Comparator<TileComputationEvent> {
        private final Comparator<TileComputationEvent>[] comparators;

        public GroupingComparator(Comparator<TileComputationEvent>... comparators) {
            this.comparators = comparators;
        }

        @Override
        public int compare(TileComputationEvent o1, TileComputationEvent o2) {
            for (Comparator<TileComputationEvent> comparator : comparators) {
                int result = comparator.compare(o1, o2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }

    public static class ThreadNamesComparator implements Comparator<TileComputationEvent> {
        @Override
        public int compare(TileComputationEvent o1, TileComputationEvent o2) {
            return o1.getThreadName().compareTo(o2.getThreadName());
        }
    }

    public static class TileIndicesComparator implements Comparator<TileComputationEvent> {
        @Override
        public int compare(TileComputationEvent o1, TileComputationEvent o2) {
            int deltaX = o1.getTileX() - o2.getTileX();
            int deltaY = o1.getTileY() - o2.getTileY();
            if (deltaX == 0 && deltaY == 0) {
                return 0;
            } else if (deltaY == 0) {
                return deltaX;
            }
            return deltaY;
        }
    }

    public static class ImagesComparator implements Comparator<TileComputationEvent> {
        @Override
        public int compare(TileComputationEvent o1, TileComputationEvent o2) {
            if (o1.getImage() == o2.getImage()) {
                return 0;
            }
            int delta = o1.getClass().getName().compareTo(o2.getClass().getName());
            if (delta == 0) {
                return System.identityHashCode(o1.getImage()) - System.identityHashCode(o2.getImage());
            }
            return delta;
        }
    }

    public static class StartTimeComparator implements Comparator<TileComputationEvent> {
        @Override
        public int compare(TileComputationEvent o1, TileComputationEvent o2) {
            long delta = o1.getStartNanos() - o2.getStartNanos();
            return delta < 0 ? -1 : delta > 0 ? 1 : 0;
        }

    }


    private static class VmFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("SimpleTileComputationHandler.") && name.endsWith(".vm");
        }
    }

    public static class Task {
        private final TileComputationEvent event;
        private final Band band;
        private final Operator operator;
        private String imageId;
        private double start;
        private double duration;
        private final int barX;
        private final int barWidth;

        public Task(TileComputationEvent event, long startNanosMin, long endNanosMax) {
            this.event = event;
            start = nanosToRoundedSecs(this.event.getStartNanos() - startNanosMin);
            duration = nanosToRoundedSecs((this.event.getEndNanos() - this.event.getStartNanos()));
            final OperatorImage image = event.getImage();
            imageId = String.format("%s@%s",
                                    image.getClass().getSimpleName(),
                                    Integer.toHexString(System.identityHashCode(image)));
            band = image.getTargetBand();
            operator = image.getOperatorContext().getOperator();
            double scale = CHART_WIDTH / ((endNanosMax - startNanosMin) * 1.0E-9);
            barX = (int) Math.round(start * scale);
            barWidth = (int) Math.floor(1 + duration * scale);
        }

        public TileComputationEvent getEvent() {
            return event;
        }

        public Band getBand() {
            return band;
        }

        public Operator getOperator() {
            return operator;
        }

        public String getImageId() {
            return imageId;
        }

        public double getStart() {
            return start;
        }

        public double getDuration() {
            return duration;
        }

        public int getBarX() {
            return barX;
        }

        public int getBarWidth() {
            return barWidth;
        }
    }
}

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

package org.esa.snap.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.SpatialBinConsumer;
import org.esa.snap.binning.SpatialBinner;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.TemporalBinner;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Ignore;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * Usage: <code>TestBinner <i>sourceDir</i> <i>regionWkt</i> <i>binnerConfig</i> <i>formatterConfig</i> [<i>formatterConfig</i> ...]</code>
 *
 * with
 * <ul>
 * <li><code><i>sourceDir</i></code> Directory containing input product files</li>
 * <li><code><i>regionWkt</i></code> File with region geometry WKT, e.g. "POLYGON((1 47,27 47,27 33,1 33,1 47))"</li>
 * <li><code><i>binnerConfig</i></code> File with binning configuration XML (see /org/esa/snap/binning/BinningConfigTest.xml)</li>
 * <li><code><i>formatterConfig</i></code> File with formatter configuration XML (see /org/esa/snap/binning/FormatterConfigTest.xml)</li>
 * </ul>
 * <p>
 * The test demonstrates the usage of various binning API classes such as
 * <ul>
 * <li>{@link SpatialBinner}</li>
 * <li>{@link TemporalBinner}</li>
 * <li>{@link org.esa.snap.binning.operator.Formatter}</li>
 * </ul>
 *
 * @author Norman Fomferra
 */
@Ignore
public class TestBinner {

    public static void main(String[] args) throws Exception {

        String sourceDirFile = args[0];
        String regionWktFile = args[1];
        String binnerConfigFile = args[2];
        String[] outputterConfigFiles = new String[args.length - 3];
        System.arraycopy(args, 3, outputterConfigFiles, 0, outputterConfigFiles.length);

        File[] sourceFiles = new File(sourceDirFile).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".N1");
            }
        });
        String regionWkt = FileUtils.readText(new File(regionWktFile));
        BinningConfig binningConfig = BinningConfig.fromXml(FileUtils.readText(new File(binnerConfigFile)));

        Debug.setEnabled(true);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        BinningContext binningContext = binningConfig.createBinningContext(null, null, null);

        // Step 1: Spatial binning - creates time-series of spatial bins for each bin ID ordered by ID. The tree map structure is <ID, time-series>
        SortedMap<Long, List<SpatialBin>> spatialBinMap = doSpatialBinning(binningContext, sourceFiles);
        // Step 2: Temporal binning - creates a list of temporal bins, sorted by bin ID
        List<TemporalBin> temporalBins = doTemporalBinning(binningContext, spatialBinMap);
        // Step 3: Formatting
        for (String outputterConfigFile : outputterConfigFiles) {
            FormatterConfig formatterConfig = FormatterConfig.fromXml(FileUtils.readText(new File(outputterConfigFile)));
            doOutputting(regionWkt, formatterConfig, binningContext, temporalBins);
        }

        stopWatch.stopAndTrace(String.format("Total time for binning %d product(s)", sourceFiles.length));
    }

    private static SortedMap<Long, List<SpatialBin>> doSpatialBinning(BinningContext binningContext, File[] sourceFiles) throws IOException {
        final SpatialBinStore spatialBinStore = new SpatialBinStore();
        final SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinStore);
        for (File sourceFile : sourceFiles) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            System.out.println("reading " + sourceFile);
            final Product product = ProductIO.readProduct(sourceFile);
            System.out.println("processing " + sourceFile);
            final long numObs = SpatialProductBinner.processProduct(product, spatialBinner,
                                                                    new HashMap<Product, List<Band>>(), ProgressMonitor.NULL);
            System.out.println("done, " + numObs + " observations processed");

            stopWatch.stopAndTrace("Spatial binning of product took");
        }
        return spatialBinStore.getSpatialBinMap();
    }

    private static List<TemporalBin> doTemporalBinning(BinningContext binningContext, SortedMap<Long, List<SpatialBin>> spatialBinMap) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final TemporalBinner temporalBinner = new TemporalBinner(binningContext);
        final ArrayList<TemporalBin> temporalBins = new ArrayList<TemporalBin>();
        for (Map.Entry<Long, List<SpatialBin>> entry : spatialBinMap.entrySet()) {
            final TemporalBin temporalBin = temporalBinner.processSpatialBins(entry.getKey(), entry.getValue());
            temporalBins.add(temporalBin);
        }

        stopWatch.stopAndTrace("Temporal binning took");

        return temporalBins;
    }

    private static void doOutputting(String regionWKT, FormatterConfig formatterConfig, BinningContext binningContext, List<TemporalBin> temporalBins) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        PlanetaryGrid planetaryGrid = binningContext.getPlanetaryGrid();
        String[] resultFeatureNames = binningContext.getBinManager().getResultFeatureNames();
        org.esa.snap.binning.operator.Formatter.format(planetaryGrid,
                                                       new MyTemporalBinSource(temporalBins),
                                                       resultFeatureNames,
                                                       formatterConfig,
                                                       new WKTReader().read(regionWKT),
                                                       new ProductData.UTC(),
                                                       new ProductData.UTC(),
                                                       new MetadataElement("TODO_add_metadata_here")
        );

        stopWatch.stopAndTrace("Writing output took");
    }

    private static class SpatialBinStore implements SpatialBinConsumer {

        // Note, we use a sorted map in order to sort entries on-the-fly
        final private SortedMap<Long, List<SpatialBin>> spatialBinMap = new TreeMap<Long, List<SpatialBin>>();

        public SortedMap<Long, List<SpatialBin>> getSpatialBinMap() {
            return spatialBinMap;
        }

        @Override
        public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {

            for (SpatialBin spatialBin : spatialBins) {
                List<SpatialBin> spatialBinList = spatialBinMap.get(spatialBin.getIndex());
                if (spatialBinList == null) {
                    spatialBinList = new ArrayList<SpatialBin>();
                    spatialBinMap.put(spatialBin.getIndex(), spatialBinList);
                }
                spatialBinList.add(spatialBin);
            }
        }
    }

    private static class MyTemporalBinSource implements TemporalBinSource {

        private final List<TemporalBin> temporalBins;

        public MyTemporalBinSource(List<TemporalBin> temporalBins) {
            this.temporalBins = temporalBins;
        }

        @Override
        public int open() throws IOException {
            return 1;
        }

        @Override
        public Iterator<? extends TemporalBin> getPart(int index) throws IOException {
            return temporalBins.iterator();
        }

        @Override
        public void partProcessed(int index, Iterator<? extends TemporalBin> part) throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}

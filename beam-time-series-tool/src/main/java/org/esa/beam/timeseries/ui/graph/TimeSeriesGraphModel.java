/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.timeseries.ui.graph;

import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.AxisMapping;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

class TimeSeriesGraphModel implements TimeSeriesGraphUpdater.TimeSeriesDataHandler, TimeSeriesGraphDisplayController.PinSupport {

    private static final String QUALIFIER_RASTER = "_r_";
    private static final String QUALIFIER_INSITU = "_i_";

    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(225, 225, 225);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final int CURSOR_COLLECTION_INDEX_OFFSET = 0;
    private static final int PIN_COLLECTION_INDEX_OFFSET = 1;
    private static final int INSITU_COLLECTION_INDEX_OFFSET = 2;
    private static final Stroke PIN_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[]{10.0f}, 0.0f);
    private static final Stroke CURSOR_STROKE = new BasicStroke();

    private final Map<AbstractTimeSeries, TimeSeriesGraphDisplayController> displayControllerMap;
    private final XYPlot timeSeriesPlot;
    private final List<List<Band>> eoVariableBands;
    private final AtomicInteger version = new AtomicInteger(0);
    private final TimeSeriesGraphUpdater.WorkerChainSupport workerChainSupport;
    private final Validation validation;
    private final WorkerChain workerChain;
    private final Map<String, Paint> paintMap = new HashMap<String, Paint>();

    private TimeSeriesGraphDisplayController displayController;
    private boolean isShowingSelectedPins;
    private boolean isShowingAllPins;
    private AxisMapping displayAxisMapping;
    private boolean showCursorTimeSeries = true;

    TimeSeriesGraphModel(XYPlot plot, Validation validation) {
        timeSeriesPlot = plot;
        this.validation = validation;
        validation.addValidationListener(new ValidationListener() {
            @Override
            public void expressionChanged() {
                updateTimeSeries(null, TimeSeriesType.INSITU);
                updateTimeSeries(null, TimeSeriesType.PIN);
            }
        });
        eoVariableBands = new ArrayList<List<Band>>();
        displayControllerMap = new WeakHashMap<AbstractTimeSeries, TimeSeriesGraphDisplayController>();
        workerChainSupport = createWorkerChainSupport();
        workerChain = new WorkerChain();
        initPlot();
    }

    void adaptToTimeSeries(AbstractTimeSeries timeSeries) {
        version.incrementAndGet();
        eoVariableBands.clear();

        final boolean hasData = timeSeries != null;
        if (hasData) {
            displayController = displayControllerMap.get(timeSeries);
            if (displayController == null) {
                displayController = new TimeSeriesGraphDisplayController(this);
                displayControllerMap.put(timeSeries, displayController);
            }
            displayController.adaptTo(timeSeries);
            for (String eoVariableName : displayController.getEoVariablesToDisplay()) {
                eoVariableBands.add(timeSeries.getBandsForVariable(eoVariableName));
            }
            displayAxisMapping = createDisplayAxisMapping(timeSeries);
        } else {
            displayAxisMapping = new AxisMapping();
        }
        validation.adaptTo(timeSeries, displayAxisMapping);
        updatePlot(hasData);
    }

    AtomicInteger getVersion() {
        return version;
    }

    void updateAnnotation(RasterDataNode raster) {
        removeAnnotation();

        final AbstractTimeSeries timeSeries = getTimeSeries();
        TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(raster);
        if (timeCoding != null) {
            final ProductData.UTC startTime = timeCoding.getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                    ProductData.UTC.UTC_TIME_ZONE,
                    Locale.getDefault());

            double millisecond = timePeriod.getFirstMillisecond();
            Range valueRange = null;
            for (int i = 0; i < timeSeriesPlot.getRangeAxisCount(); i++) {
                valueRange = Range.combine(valueRange, timeSeriesPlot.getRangeAxis(i).getRange());
            }
            if (valueRange != null) {
                XYAnnotation annotation = new XYLineAnnotation(millisecond, valueRange.getLowerBound(), millisecond,
                        valueRange.getUpperBound());
                timeSeriesPlot.addAnnotation(annotation, true);
            }
        }
    }

    void removeAnnotation() {
        timeSeriesPlot.clearAnnotations();
    }

    void setIsShowingSelectedPins(boolean isShowingSelectedPins) {
        if (isShowingSelectedPins && isShowingAllPins) {
            throw new IllegalStateException("isShowingSelectedPins && isShowingAllPins");
        }
        this.isShowingSelectedPins = isShowingSelectedPins;
        updateTimeSeries(null, TimeSeriesType.PIN);
        updateTimeSeries(null, TimeSeriesType.INSITU);
    }

    void setIsShowingAllPins(boolean isShowingAllPins) {
        if (isShowingAllPins && isShowingSelectedPins) {
            throw new IllegalStateException("isShowingAllPins && isShowingSelectedPins");
        }
        this.isShowingAllPins = isShowingAllPins;
        updateTimeSeries(null, TimeSeriesType.PIN);
        updateTimeSeries(null, TimeSeriesType.INSITU);
    }

    void setIsShowingCursorTimeSeries(boolean showCursorTimeSeries) {
        this.showCursorTimeSeries = showCursorTimeSeries;
    }

    synchronized void updateTimeSeries(TimeSeriesGraphUpdater.Position cursorPosition, TimeSeriesType type) {
        if(getTimeSeries() == null) {
            return;
        }
        final TimeSeriesGraphUpdater.PositionSupport positionSupport = createPositionSupport();
        final TimeSeriesGraphUpdater w = new TimeSeriesGraphUpdater(getTimeSeries(), createVersionSafeDataSources(),
                this, displayAxisMapping, workerChainSupport, cursorPosition, positionSupport, type,
                showCursorTimeSeries, version.get());
        final boolean chained = type != TimeSeriesType.CURSOR;
        workerChain.setOrExecuteNextWorker(w, chained);
    }

    boolean isShowCursorTimeSeries() {
        return showCursorTimeSeries;
    }

    @Override
    public void addTimeSeries(List<TimeSeries> timeSeriesList, TimeSeriesType type) {
        final int timeSeriesCount;
        final int collectionOffset;
        if (TimeSeriesType.INSITU.equals(type)) {
            timeSeriesCount = displayAxisMapping.getInsituCount();
            collectionOffset = INSITU_COLLECTION_INDEX_OFFSET;
        } else {
            timeSeriesCount = displayAxisMapping.getRasterCount();
            if (TimeSeriesType.CURSOR.equals(type)) {
                collectionOffset = CURSOR_COLLECTION_INDEX_OFFSET;
            } else {
                collectionOffset = PIN_COLLECTION_INDEX_OFFSET;
            }
        }
        final String[] aliasNames = getAliasNames();

        for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
            final int targetCollectionIndex = collectionOffset + aliasIdx * 3;
            final TimeSeriesCollection targetTimeSeriesCollection = (TimeSeriesCollection) timeSeriesPlot.getDataset(targetCollectionIndex);
            if(targetTimeSeriesCollection != null) {
                targetTimeSeriesCollection.removeAllSeries();
            }
        }
        if(timeSeriesCount == 0) {
            return;
        }
        final int numPositions = timeSeriesList.size() / timeSeriesCount;
        int timeSeriesIndexOffset = 0;
        for (int posIdx = 0; posIdx < numPositions; posIdx++) {
            final Shape posShape = getShapeForPosition(type, posIdx);
            for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
                final int targetCollectionIndex = collectionOffset + aliasIdx * 3;
                final TimeSeriesCollection targetTimeSeriesCollection = (TimeSeriesCollection) timeSeriesPlot.getDataset(targetCollectionIndex);
                if (targetTimeSeriesCollection == null) {
                    continue;
                }
                final XYItemRenderer renderer = timeSeriesPlot.getRenderer(targetCollectionIndex);
                final int dataSourceCount = getDataSourceCount(type, aliasNames[aliasIdx]);
                for (int ignoredIndex = 0; ignoredIndex < dataSourceCount; ignoredIndex++) {
                    final TimeSeries currentTimeSeries = timeSeriesList.get(timeSeriesIndexOffset);
                    targetTimeSeriesCollection.addSeries(currentTimeSeries);
                    final int timeSeriesTargetIdx = targetTimeSeriesCollection.getSeriesCount() - 1;
                    renderer.setSeriesShape(timeSeriesTargetIdx, posShape);
                    renderer.setSeriesPaint(timeSeriesTargetIdx, renderer.getSeriesPaint(timeSeriesTargetIdx % dataSourceCount));
                    renderer.setSeriesVisibleInLegend(timeSeriesTargetIdx, !currentTimeSeries.isEmpty());
                    timeSeriesIndexOffset++;
                }
                final ValueAxis axisForDataset = timeSeriesPlot.getDomainAxisForDataset(targetCollectionIndex);
                axisForDataset.configure();
            }
        }
        updateAnnotation(getCurrentView().getRaster());
    }

    @Override
    public TimeSeries getValidatedTimeSeries(TimeSeries timeSeries, String dataSourceName, TimeSeriesType type) {
        try {
            return validation.validate(timeSeries, dataSourceName, type);
        } catch (ParseException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isShowingSelectedPins() {
        return isShowingSelectedPins;
    }

    @Override
    public Placemark[] getSelectedPins() {
        return getCurrentView().getSelectedPins();
    }

    @Override
    public boolean isShowingAllPins() {
        return isShowingAllPins;
    }

    private TimeSeriesGraphUpdater.WorkerChainSupport createWorkerChainSupport() {
        return new TimeSeriesGraphUpdater.WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker) {
                workerChain.removeCurrentWorkerAndExecuteNext(worker);
            }
        };
    }

    private TimeSeriesGraphUpdater.PositionSupport createPositionSupport() {
        return new TimeSeriesGraphUpdater.PositionSupport() {

            private final GeoCoding geoCoding = getTimeSeries().getTsProduct().getGeoCoding();
            private final PixelPos pixelPos = new PixelPos();

            @Override
            public TimeSeriesGraphUpdater.Position transformGeoPos(GeoPos geoPos) {
                geoCoding.getPixelPos(geoPos, pixelPos);
                return new TimeSeriesGraphUpdater.Position((int) pixelPos.getX(), (int) pixelPos.getY(), 0);
            }
        };
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYLineAndShapeRenderer xyRenderer = new XYLineAndShapeRenderer(true, true);
        xyRenderer.setBaseLegendTextPaint(DEFAULT_FOREGROUND_COLOR);
        timeSeriesPlot.setRenderer(xyRenderer);
        timeSeriesPlot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
        timeSeriesPlot.setDrawingSupplier(null);
    }

    private void updatePlot(boolean hasData) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();

        if (!hasData) {
            return;
        }

        paintMap.clear();
        final Set<String> aliasNamesSet = displayAxisMapping.getAliasNames();
        final String[] aliasNames = aliasNamesSet.toArray(new String[aliasNamesSet.size()]);

        for (String aliasName : aliasNamesSet) {
            consumeColors(aliasName, displayAxisMapping.getRasterNames(aliasName), QUALIFIER_RASTER);
            consumeColors(aliasName, displayAxisMapping.getInsituNames(aliasName), QUALIFIER_INSITU);
        }

        for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
            String aliasName = aliasNames[aliasIdx];

            timeSeriesPlot.setRangeAxis(aliasIdx, createValueAxis(aliasName));

            final int aliasIndexOffset = aliasIdx * 3;
            final int cursorCollectionIndex = aliasIndexOffset + CURSOR_COLLECTION_INDEX_OFFSET;
            final int pinCollectionIndex = aliasIndexOffset + PIN_COLLECTION_INDEX_OFFSET;
            final int insituCollectionIndex = aliasIndexOffset + INSITU_COLLECTION_INDEX_OFFSET;

            TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(cursorCollectionIndex, cursorDataset);

            TimeSeriesCollection pinDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(pinCollectionIndex, pinDataset);

            TimeSeriesCollection insituDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(insituCollectionIndex, insituDataset);

            timeSeriesPlot.mapDatasetToRangeAxis(cursorCollectionIndex, aliasIdx);
            timeSeriesPlot.mapDatasetToRangeAxis(pinCollectionIndex, aliasIdx);
            timeSeriesPlot.mapDatasetToRangeAxis(insituCollectionIndex, aliasIdx);

            final XYErrorRenderer pinRenderer = createXYErrorRenderer();
            final XYErrorRenderer cursorRenderer = createXYErrorRenderer();
            final XYErrorRenderer insituRenderer = createXYErrorRenderer();

            pinRenderer.setBaseStroke(PIN_STROKE);
            cursorRenderer.setBaseStroke(CURSOR_STROKE);

            insituRenderer.setBaseLinesVisible(false);
            insituRenderer.setBaseShapesFilled(false);


            final List<String> rasterNamesSet = displayAxisMapping.getRasterNames(aliasName);
            final String[] rasterNames = rasterNamesSet.toArray(new String[rasterNamesSet.size()]);

            for (int i = 0; i < rasterNames.length; i++) {
                final String paintKey = aliasName + QUALIFIER_RASTER + rasterNames[i];
                final Paint paint = paintMap.get(paintKey);
                cursorRenderer.setSeriesPaint(i, paint);
                pinRenderer.setSeriesPaint(i, paint);
            }

            final List<String> insituNamesSet = displayAxisMapping.getInsituNames(aliasName);
            final String[] insituNames = insituNamesSet.toArray(new String[insituNamesSet.size()]);

            for (int i = 0; i < insituNames.length; i++) {
                final String paintKey = aliasName + QUALIFIER_INSITU + insituNames[i];
                final Paint paint = paintMap.get(paintKey);
                insituRenderer.setSeriesPaint(i, paint);
            }

            timeSeriesPlot.setRenderer(cursorCollectionIndex, cursorRenderer);
            timeSeriesPlot.setRenderer(pinCollectionIndex, pinRenderer);
            timeSeriesPlot.setRenderer(insituCollectionIndex, insituRenderer);
        }
    }

    private void consumeColors(String aliasName, List<String> names, String identifier) {
        final int registeredPaints = paintMap.size();
        for (int i = 0; i < names.size(); i++) {
            final Paint paint = displayController.getPaint(registeredPaints + i);
            paintMap.put(aliasName + identifier + names.get(i), paint);
        }
    }

    private XYErrorRenderer createXYErrorRenderer() {
        final XYErrorRenderer renderer = new XYErrorRenderer();
        renderer.setDrawXError(false);
        renderer.setDrawYError(false);
        renderer.setBaseLinesVisible(true);
        renderer.setAutoPopulateSeriesStroke(false);
        renderer.setAutoPopulateSeriesPaint(false);
        renderer.setAutoPopulateSeriesFillPaint(false);
        renderer.setAutoPopulateSeriesOutlinePaint(false);
        renderer.setAutoPopulateSeriesOutlineStroke(false);
        renderer.setAutoPopulateSeriesShape(false);
        final StandardXYToolTipGenerator tipGenerator;
        tipGenerator = new StandardXYToolTipGenerator("Value: {2}   Date: {1}", new SimpleDateFormat(), new DecimalFormat());
        renderer.setBaseToolTipGenerator(tipGenerator);
        return renderer;
    }

    private NumberAxis createValueAxis(String aliasName) {
        String unit = getUnit(displayAxisMapping, aliasName);
        String axisLabel = getAxisLabel(aliasName, unit);
        NumberAxis valueAxis = new NumberAxis(axisLabel);
        valueAxis.setAutoRange(true);
        return valueAxis;
    }

    private AxisMapping createDisplayAxisMapping(AbstractTimeSeries timeSeries) {
        final List<String> eoVariables = displayController.getEoVariablesToDisplay();
        if (eoVariables.size() == 0) {
            final Product.AutoGrouping autoGrouping = this.getCurrentView().getProduct().getAutoGrouping();
            for (String[] strings : autoGrouping) {
                eoVariables.add(strings[0]);
            }
        }
        final List<String> insituVariables = displayController.getInsituVariablesToDisplay();
        final AxisMapping axisMapping = timeSeries.getAxisMapping();
        return createDisplayAxisMapping(eoVariables, insituVariables, axisMapping);
    }

    private AxisMapping createDisplayAxisMapping(List<String> eoVariables, List<String> insituVariables, AxisMapping axisMapping) {
        final AxisMapping displayAxisMapping = new AxisMapping();

        for (String eoVariable : eoVariables) {
            final String aliasName = axisMapping.getRasterAlias(eoVariable);
            if (aliasName == null) {
                displayAxisMapping.addRasterName(eoVariable, eoVariable);
            } else {
                displayAxisMapping.addRasterName(aliasName, eoVariable);
            }
        }

        for (String insituVariable : insituVariables) {
            final String aliasName = axisMapping.getInsituAlias(insituVariable);
            if (aliasName == null) {
                displayAxisMapping.addInsituName(insituVariable, insituVariable);
            } else {
                displayAxisMapping.addInsituName(aliasName, insituVariable);
            }
        }
        return displayAxisMapping;
    }

    private String getUnit(AxisMapping axisMapping, String aliasName) {
        final List<String> rasterNames = axisMapping.getRasterNames(aliasName);
        for (List<Band> eoVariableBandList : eoVariableBands) {
            for (String rasterName : rasterNames) {
                final Band raster = eoVariableBandList.get(0);
                if (raster.getName().startsWith(rasterName)) {
                    return raster.getUnit();
                }
            }
        }
        return "";
    }

    private static String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private String[] getAliasNames() {
        final Set<String> aliasNamesSet = displayAxisMapping.getAliasNames();
        return aliasNamesSet.toArray(new String[aliasNamesSet.size()]);
    }

    private Shape getShapeForPosition(TimeSeriesType type, int posIdx) {
        final Shape posShape;
        if (TimeSeriesType.CURSOR.equals(type)) {
            posShape = TimeSeriesGraphDisplayController.CURSOR_SHAPE;
        } else {
            posShape = displayController.getShape(posIdx);
        }
        return posShape;
    }

    private int getDataSourceCount(TimeSeriesType type, String aliasName) {
        if (TimeSeriesType.INSITU.equals(type)) {
            return displayAxisMapping.getInsituNames(aliasName).size();
        } else {
            return displayAxisMapping.getRasterNames(aliasName).size();
        }
    }

    private TimeSeriesGraphUpdater.VersionSafeDataSources createVersionSafeDataSources() {
        return new TimeSeriesGraphUpdater.VersionSafeDataSources(
                displayController.getPinPositionsToDisplay(), getVersion().get()) {
            @Override
            public int getCurrentVersion() {
                return version.get();
            }
        };
    }

    private AbstractTimeSeries getTimeSeries() {
        final ProductSceneView sceneView = getCurrentView();
        if(sceneView == null) {
            return null;
        }
        final Product sceneViewProduct = sceneView.getProduct();
        return TimeSeriesMapper.getInstance().getTimeSeries(sceneViewProduct);
    }

    private ProductSceneView getCurrentView() {
        return VisatApp.getApp().getSelectedProductSceneView();
    }

    static interface ValidationListener {
        void expressionChanged();
    }

    static interface Validation {

        TimeSeries validate(TimeSeries timeSeries, String sourceName, TimeSeriesType type) throws ParseException;

        void adaptTo(Object timeSeriesKey, AxisMapping axisMapping);

        void addValidationListener(ValidationListener listener);
    }
}

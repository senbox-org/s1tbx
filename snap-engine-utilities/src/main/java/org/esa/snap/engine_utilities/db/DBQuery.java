/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.db;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.jdom2.Attribute;
import org.jdom2.Element;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**

 */
public class DBQuery {

    public static final String ALL_MISSIONS = "All_Missions";
    public static final String ALL_PRODUCT_TYPES = "All_Types";
    public static final String ALL_PASSES = "All_Passes";
    public static final String ALL_MODES = "All_Modes";
    public static final String ASCENDING_PASS = "ASCENDING";
    public static final String DESCENDING_PASS = "DESCENDING";
    public static final String ALL_FOLDERS = "All_Folders";
    public static final String ANY = "Any";
    public static final String DUALPOL = "Dual-Pol";
    public static final String QUADPOL = "Quad-Pol";
    public static final String HHVV = "HH+VV";
    public static final String HHHV = "HH+HV";
    public static final String VVVH = "VV+VH";
    public static final String CALIBRATED = "Calibrated";
    public static final String NOT_CALIBRATED = "Not_Calibrated";
    public static final String ORBIT_PRELIMINARY = "Preliminary";
    public static final String ORBIT_PRECISE = "Precise";
    public static final String ORBIT_VERIFIED = "Verified";
    public static final String DB_QUERY = "dbQuery";

    private static final String NoData = AbstractMetadata.NO_METADATA_STRING;

    private String selectedMissions[] = {};
    private String selectedProductTypes[] = {};
    private String selectedAcquisitionMode = "";
    private String selectedPass = "";
    private String selectedName = "";
    private String selectedTrack = "";
    private String selectedCloudCover = "";
    private String selectedSampleType = "";
    private String selectedPolarization = ANY;
    private String selectedCalibration = ANY;
    private String selectedOrbitCorrection = ANY;
    private Rectangle.Double selectionRectangle = null;
    private boolean insideSelRect = true; // user can choose between either completely inside or intersect selection rectangle
    private File baseDir = null;
    private File excludeDir = null;
    private Calendar startDate = null;
    private Calendar endDate = null;
    private boolean[] selectedMonths = new boolean[12];
    private String freeQuery = "";
    private boolean returnAllIfNoIntersection = true;

    private final Map<String, String> metadataQueryMap = new HashMap<>();

    public DBQuery() {
        Arrays.fill(selectedMonths, true);
    }

    public String getSelectedName() {
        return selectedName;
    }

    public void setSelectedMissions(final String[] missions) {
        selectedMissions = missions;
    }

    public String[] getSelectedMissions() {
        return selectedMissions;
    }

    public void setSelectedProductTypes(final String[] productTypes) {
        selectedProductTypes = productTypes;
    }

    public String[] getSelectedProductTypes() {
        return selectedProductTypes;
    }

    public void setSelectedAcquisitionMode(final String mode) {
        if (mode != null) {
            selectedAcquisitionMode = mode;
        }
    }

    public String getSelectedAcquisitionMode() {
        return selectedAcquisitionMode;
    }

    public void setSelectedPass(final String pass) {
        if (pass != null) {
            selectedPass = pass;
        }
    }

    public String getSelectedPass() {
        return selectedPass;
    }

    public void setSelectedName(final String name) {
        if (name != null) {
            selectedName = name;
        }
    }

    public void setSelectedTrack(final String track) {
        if (track != null) {
            selectedTrack = track;
        }
    }

    public String getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedCloudCover(final String cloudCover) {
        if (cloudCover != null) {
            selectedCloudCover = cloudCover;
        }
    }

    public String getSelectedCloudCover() {
        return selectedCloudCover;
    }

    public void setSelectedSampleType(final String sampleType) {
        if (sampleType != null) {
            selectedSampleType = sampleType;
        }
    }

    public String getSelectedSampleType() {
        return selectedSampleType;
    }

    public void setSelectedPolarization(final String pol) {
        if (pol != null) {
            selectedPolarization = pol;
        }
    }

    public String getSelectedPolarization() {
        return selectedPolarization;
    }

    public void setSelectedCalibration(final String calib) {
        if (calib != null) {
            selectedCalibration = calib;
        }
    }

    public String getSelectedCalibration() {
        return selectedCalibration;
    }

    public void setSelectedOrbitCorrection(final String orbitCor) {
        if (orbitCor != null) {
            selectedOrbitCorrection = orbitCor;
        }
    }

    public String getSelectedOrbitCorrection() {
        return selectedOrbitCorrection;
    }

    public void setBaseDir(final File dir) {
        baseDir = dir;
    }

    public void setExcludeDir(final File dir) {
        excludeDir = dir;
    }

    public void setStartEndDate(final Calendar start, final Calendar end) {
        startDate = start;
        endDate = end;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public Calendar getEndDate() {
        return endDate;
    }

    public boolean isMonthSelected(final int month) {
        assert (month < 12);
        return selectedMonths[month];
    }

    public void setMonthSelected(final int month, final boolean selected) {
        assert (month < 12);
        selectedMonths[month] = selected;
    }

    public void clearMetadataQuery() {
        metadataQueryMap.clear();
    }

    public void addMetadataQuery(final String name, final String value) {
        metadataQueryMap.put(name, value);
    }

    public void setFreeQuery(final String queryStr) {
        freeQuery = queryStr;
    }

    public String getFreeQuery() {
        return freeQuery;
    }

    public void setReturnAllIfNoIntersection(final boolean flag) {
        returnAllIfNoIntersection = flag;
    }

    public ProductEntry[] queryDatabase(final ProductDB db) throws SQLException {

        if (StringUtils.contains(selectedMissions, ALL_MISSIONS)) {
            selectedMissions = new String[]{};
        }
        if (StringUtils.contains(selectedProductTypes, ALL_PRODUCT_TYPES)) {
            selectedProductTypes = new String[]{};
        }

        final StringBuilder queryStr = new StringBuilder(1000);
        if (selectedMissions.length > 0) {
            queryStr.append(SQLUtils.getOrList(ProductTable.TABLE + '.' + AbstractMetadata.MISSION, selectedMissions));
        }
        if (selectedProductTypes.length > 0) {
            SQLUtils.addAND(queryStr);
            queryStr.append(SQLUtils.getOrList(ProductTable.TABLE + '.' + AbstractMetadata.PRODUCT_TYPE, selectedProductTypes));
        }
        if (!selectedAcquisitionMode.isEmpty() && !selectedAcquisitionMode.equals(ALL_MODES)) {
            SQLUtils.addAND(queryStr);
            queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.ACQUISITION_MODE + "='" + selectedAcquisitionMode + '\'');
        }
        if (!selectedPass.isEmpty() && !selectedPass.equals(ALL_PASSES)) {
            SQLUtils.addAND(queryStr);
            queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.PASS + "='" + selectedPass + '\'');
        }
        if (!selectedName.isEmpty()) {
            SQLUtils.addAND(queryStr);
            queryStr.append("( " + MetadataTable.TABLE + '.' + AbstractMetadata.PRODUCT + " LIKE '%" + selectedName + "%' )");
        }
        if (!selectedTrack.isEmpty()) {
            SQLUtils.addAND(queryStr);
            queryStr.append("( " + MetadataTable.TABLE + '.' + AbstractMetadata.REL_ORBIT + '=' + selectedTrack + " )");
        }
        if (!selectedSampleType.isEmpty() && !selectedSampleType.equals(ANY)) {
            SQLUtils.addAND(queryStr);
            queryStr.append("( " + MetadataTable.TABLE + '.' + AbstractMetadata.SAMPLE_TYPE + "='" + selectedSampleType + "' )");
        }
        if (!selectedPolarization.isEmpty() && !selectedPolarization.equals(ANY)) {
            formPolorizationQuery(queryStr);
        }
        if (!selectedCalibration.isEmpty() && !selectedCalibration.equals(ANY)) {
            SQLUtils.addAND(queryStr);
            if (selectedCalibration.equals(CALIBRATED))
                queryStr.append(MetadataTable.TABLE + '.' + AbstractMetadata.abs_calibration_flag + "=1");
            else if (selectedCalibration.equals(NOT_CALIBRATED))
                queryStr.append(MetadataTable.TABLE + '.' + AbstractMetadata.abs_calibration_flag + "=0");
        }
        if (!selectedOrbitCorrection.isEmpty() && !selectedOrbitCorrection.equals(ANY)) {
            formOrbitCorrectionQuery(queryStr);
        }

        if (startDate != null) {
            SQLUtils.addAND(queryStr);
            final Date start = SQLUtils.toSQLDate(startDate);
            if (endDate != null) {
                final Date end = SQLUtils.toSQLDate(endDate);
                queryStr.append("( " + ProductTable.TABLE + '.' + AbstractMetadata.first_line_time
                        + " BETWEEN '" + start.toString() + "' AND '" + end.toString() + "' )");
            } else {
                queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.first_line_time + ">='" + start.toString() + '\'');
            }
        } else if (endDate != null) {
            SQLUtils.addAND(queryStr);
            final Date end = SQLUtils.toSQLDate(endDate);
            queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.first_line_time + "<='" + end.toString() + '\'');
        }

        if (selectedMonths != null && monthSelectionMade()) {
            SQLUtils.addAND(queryStr);
            final StringBuilder monthSelectionStr = new StringBuilder();
            for (int m = 0; m < selectedMonths.length; ++m) {
                if (!selectedMonths[m]) {
                    if (monthSelectionStr.length() > 0) {
                        monthSelectionStr.append(" OR ");
                    }
                    monthSelectionStr.append("MONTH(" + ProductTable.TABLE + '.' + AbstractMetadata.first_line_time + ") = " + (m + 1));
                }
            }
            queryStr.append("NOT (" + monthSelectionStr + ')');
        }

        final Set<String> metadataNames = metadataQueryMap.keySet();
        for (String name : metadataNames) {
            final String value = metadataQueryMap.get(name);
            if (value != null && !value.isEmpty()) {
                SQLUtils.addAND(queryStr);
                queryStr.append(MetadataTable.TABLE + '.' + name + "='" + value + '\'');
            }
        }

        if (!freeQuery.isEmpty()) {
            SQLUtils.addAND(queryStr);
            final String metadataFreeQuery = SQLUtils.insertTableName(db.getMetadataNames(), MetadataTable.TABLE, freeQuery);
            queryStr.append("( " + metadataFreeQuery + " )");
        }

        if (baseDir != null) {
            SQLUtils.addAND(queryStr);
            queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.PATH + " LIKE '" + baseDir.getAbsolutePath() + File.separator+"%'");
        }
        if (excludeDir != null) {
            SQLUtils.addAND(queryStr);
            queryStr.append(ProductTable.TABLE + '.' + AbstractMetadata.PATH + " NOT LIKE '" + excludeDir.getAbsolutePath() + File.separator+"%'");
        }

        if (queryStr.length() > 0) {
            SystemUtils.LOG.info("Query=" + queryStr);
            return intersectMapSelection(db.queryProduct(queryStr.toString()), returnAllIfNoIntersection);
        } else {
            SystemUtils.LOG.info("Query=empty");
            return intersectMapSelection(db.getProductEntryList(false), returnAllIfNoIntersection);
        }
    }

    private void formOrbitCorrectionQuery(final StringBuilder queryStr) {
        SQLUtils.addAND(queryStr);
        switch (selectedOrbitCorrection) {
            case ORBIT_VERIFIED:
                queryStr.append(MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " LIKE 'DORIS Verified%'");
                break;
            case ORBIT_PRECISE:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " LIKE 'DORIS Precise%' OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " LIKE 'DELFT Precise%' OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " LIKE 'PRARE Precise%'" + " )");
                break;
            case ORBIT_PRELIMINARY:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " NOT LIKE 'DORIS%' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " NOT LIKE 'DELFT%' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.orbit_state_vector_file + " NOT LIKE 'PRARE%'" + " )");
                break;
        }
    }

    private void formPolorizationQuery(final StringBuilder queryStr) {

        SQLUtils.addAND(queryStr);
        switch (selectedPolarization) {
            case HHVV:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "!='"+NoData+"' AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'HH'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'VV'" + " ) " + " AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'HH'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'VV'" + " ) )");
                break;
            case HHHV:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "!='"+NoData+"' AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'HH'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'HV'" + " ) " + " AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'HH'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'HV'" + " ) )");
                break;
            case VVVH:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "!='"+NoData+"' AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'VV'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + '=' + "'VH'" + " ) " + " AND " +
                        " ( " + MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'VV'" + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + '=' + "'VH'" + " ) )");
                break;
            case DUALPOL:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds3_tx_rx_polar + "='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds4_tx_rx_polar + "='"+NoData+"' )");
                break;
            case QUADPOL:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds3_tx_rx_polar + "!='"+NoData+"' AND " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds4_tx_rx_polar + "!='"+NoData+"' )");
                break;
            default:
                queryStr.append("( " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds1_tx_rx_polar + "='" + selectedPolarization + '\'' + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds2_tx_rx_polar + "='" + selectedPolarization + '\'' + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds3_tx_rx_polar + "='" + selectedPolarization + '\'' + " OR " +
                        MetadataTable.TABLE + '.' + AbstractMetadata.mds4_tx_rx_polar + "='" + selectedPolarization + '\'' + " )");
                break;
        }
    }

    private boolean monthSelectionMade() {
        for (boolean b : selectedMonths) {
            if (b == false) {
                return true;
            }
        }
        return false;
    }

    public void setSelectionRect(final GeoPos[] selectionBox) {
        if(selectionBox == null) {
            selectionRectangle = null;
        } else {
            selectionRectangle = getBoundingRect(selectionBox);
        }
    }

    public void insideSelectionRectangle(final boolean inside) {
        insideSelRect = inside;
    }

    public Rectangle.Double getSelectionRectangle() {
        return selectionRectangle;
    }

    public ProductEntry[] intersectMapSelection(final ProductEntry[] resultsList, final boolean returnAllIfNoIntersection) {

        if (selectionRectangle == null || resultsList == null) {
            //System.out.println("DBQuery.intersectMapSelection: null rect returns " + resultsList.length + " products");
            return resultsList;
        }

        final List<ProductEntry> intersectList = new ArrayList<>(resultsList.length);
        final int mult = 100000; //float to integer
        final Rectangle selRect = new Rectangle((int) (selectionRectangle.x * mult), (int) (selectionRectangle.y * mult),
                (int) (selectionRectangle.width * mult), (int) (selectionRectangle.height * mult));

        /*
        System.out.println("DBQuery.intersectMapSelection: selectionRectangle: x = " + selectionRectangle.x + " y = " + selectionRectangle.y
            + " width = " + selectionRectangle.width + " height = " + selectionRectangle.height
            + "; selRect: x = " + selRect.x + " y = " + selRect.y + " width = " + selRect.width + " height = " + selRect.height
            + "; insideSelRect = " + insideSelRect);
        */

        final boolean singlePointSelection = selectionRectangle.getWidth() == 0 && selectionRectangle.getHeight() == 0;

        final Polygon p = new Polygon();
        for (final ProductEntry entry : resultsList) {
            p.reset();
            final GeoPos[] geoBox = entry.getBox();
            for (GeoPos geo : geoBox) {
                p.addPoint((int) (geo.getLat() * mult), (int) (geo.getLon() * mult));
                //System.out.println("DBQuery.intersectMapSelection: product geoPoint: " + (int)(geo.getLat() * mult) + ", " + (int)(geo.getLon() * mult));
            }
            //System.out.println("DBQuery.intersectMapSelection: product geoPoint: add first pt again");
            p.addPoint((int) (geoBox[0].getLat() * mult), (int) (geoBox[0].getLon() * mult));

            if (singlePointSelection) {
                if (p.contains(selRect.x, selRect.y)) {
                    intersectList.add(entry);
                }
            } else {
                if (p.contains(selRect)) {
                    intersectList.add(entry);
                } else if (insideSelRect) {
                    // Check if all points are in rectangle
                    boolean onePoint = false;
                    for (GeoPos geo : geoBox) {
                        if (!selRect.contains((int) (geo.getLat() * mult), (int) (geo.getLon() * mult))) {
                            //System.out.println("DBQuery.intersectMapSelection: product this pt is NOT in rec: " + (int)(geo.getLat() * mult) + ", " + (int)(geo.getLon() * mult));
                            onePoint = true;
                            break;
                        }
                    }
                    if (!onePoint) {
                        intersectList.add(entry);
                    }
                } else {
                    boolean onePtInside = false;
                    for (GeoPos geo : geoBox) {
                        if (selRect.contains((int) (geo.getLat() * mult), (int) (geo.getLon() * mult))) {
                            //System.out.println("DBQuery.intersectMapSelection: product this pt is in rec: " + (int)(geo.getLat() * mult) + ", " + (int)(geo.getLon() * mult));
                            onePtInside = true;
                            break;
                        }
                    }
                    if (onePtInside) {
                        intersectList.add(entry);
                    }
                }
            }
        }

        // if nothing selected then return all
        if (singlePointSelection && returnAllIfNoIntersection && intersectList.isEmpty()) {
            //System.out.println("DBQuery.intersectMapSelection: nothing selected returns " + resultsList.length + " products; singlePt = " + singlePointSelection);
            return resultsList;
        }

        //System.out.println("DBQuery.intersectMapSelection: returns " + intersectList.size() + " products; singlePt = " + singlePointSelection);
        return intersectList.toArray(new ProductEntry[intersectList.size()]);
    }

    public static Rectangle.Double getBoundingRect(final GeoPos[] geoPositions) {
        double minX = Float.MAX_VALUE;
        double maxX = -Float.MAX_VALUE;
        double minY = Float.MAX_VALUE;
        double maxY = -Float.MAX_VALUE;

        for (final GeoPos pos : geoPositions) {
            final double x = pos.getLat();
            final double y = pos.getLon();

            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        if (minX >= maxX || minY >= maxY) {
            return new Rectangle.Double(minX, minY, 0, 0);
        }

        return new Rectangle.Double(minX, minY, maxX - minX, maxY - minY);
    }

    private static GregorianCalendar getCalendarDate(final Element elem) {
        final Attribute y = elem.getAttribute("year");
        final Attribute m = elem.getAttribute("month");
        final Attribute d = elem.getAttribute("day");
        if (y != null && m != null && d != null) {
            return new GregorianCalendar(Integer.parseInt(y.getValue()),
                    Integer.parseInt(m.getValue()),
                    Integer.parseInt(d.getValue()));
        }
        return null;
    }
}

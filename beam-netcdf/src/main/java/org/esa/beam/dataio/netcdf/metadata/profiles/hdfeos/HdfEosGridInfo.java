/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds information describing a HDF-EOS grid structure.
 */
class HdfEosGridInfo {

    final String gridName;
    final double upperLeftLon;
    final double upperLeftLat;
    final double lowerRightLon;
    final double lowerRightLat;
    final String projection;
    private double[] projectionParameter;

    HdfEosGridInfo(String gridName,
                   double upperLeftLon, double upperLeftLat,
                   double lowerRightLon, double lowerRightLat,
                   String projection) {
        this.gridName = gridName;
        this.upperLeftLon = upperLeftLon;
        this.upperLeftLat = upperLeftLat;
        this.lowerRightLon = lowerRightLon;
        this.lowerRightLat = lowerRightLat;
        this.projection = projection;
    }

    public boolean equalProjections(HdfEosGridInfo that) {
        if (this == that) return true;

        if (Double.compare(that.lowerRightLat, lowerRightLat) != 0) return false;
        if (Double.compare(that.lowerRightLon, lowerRightLon) != 0) return false;
        if (Double.compare(that.upperLeftLat, upperLeftLat) != 0) return false;
        if (Double.compare(that.upperLeftLon, upperLeftLon) != 0) return false;
        if (!Arrays.equals(that.projectionParameter, projectionParameter)) return false;
        if (!projection.equals(that.projection)) return false;

        return true;
    }

    static List<HdfEosGridInfo> getCompatibleGridInfos(List<HdfEosGridInfo> allGridInfos) {
        List<HdfEosGridInfo> compatibleGridInfos = new ArrayList<HdfEosGridInfo>();
        if (allGridInfos.isEmpty()) {
            return compatibleGridInfos;
        }
        HdfEosGridInfo first = allGridInfos.get(0);
        for (HdfEosGridInfo gridInfo : allGridInfos) {
            if (first.equalProjections(gridInfo)) {
                compatibleGridInfos.add(gridInfo);
            }
        }
        return compatibleGridInfos;
    }

    static List<HdfEosGridInfo> createGridInfos(Element eosStructElement) {
        List<HdfEosGridInfo> gridInfos = new ArrayList<HdfEosGridInfo>();
        if (eosStructElement != null) {
            Element gridStructureElem = eosStructElement.getChild("GridStructure");
            if (gridStructureElem != null) {
                for (Element gridElem : (List<Element>) gridStructureElem.getChildren()) {
                    if (gridElem != null) {
                        HdfEosGridInfo gridInfo = createGridInfo(gridElem);
                        if (gridInfo != null) {
                            gridInfos.add(gridInfo);
                        }
                    }
                }
            }
        }
        return gridInfos;
    }

    static HdfEosGridInfo createGridInfo(Element gridElem) {
        Element gridNameElem = gridElem.getChild("GridName");
        Element projectionElem = gridElem.getChild("Projection");
        Element ulPointElem = gridElem.getChild("UpperLeftPointMtrs");
        Element lrPointElem = gridElem.getChild("LowerRightMtrs");
        Element projParamsElem = gridElem.getChild("ProjParams");

        if (gridNameElem == null || projectionElem == null || ulPointElem == null || lrPointElem == null) {
            return null;
        }
        String gridName = gridNameElem.getValue();
        if (gridName.isEmpty()) {
            return null;
        }
        List<Element> ulList = ulPointElem.getChildren();
        String ulLon = ulList.get(0).getValue();
        String ulLat = ulList.get(1).getValue();
        double upperLeftLon = Double.parseDouble(ulLon);
        double upperLeftLat = Double.parseDouble(ulLat);

        List<Element> lrList = lrPointElem.getChildren();
        String lrLon = lrList.get(0).getValue();
        String lrLat = lrList.get(1).getValue();
        double lowerRightLon = Double.parseDouble(lrLon);
        double lowerRightLat = Double.parseDouble(lrLat);

        String projection = projectionElem.getValue();
        HdfEosGridInfo hdfEosGridInfo = new HdfEosGridInfo(gridName, upperLeftLon, upperLeftLat, lowerRightLon, lowerRightLat, projection);
        if (projParamsElem != null) {
            List<Element> children = projParamsElem.getChildren();
            double[] projParameterValues = new double[children.size()];
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                projParameterValues[i] = Double.parseDouble(child.getValue());
            }
            hdfEosGridInfo.setProjectionParameter(projParameterValues);
        }
        return hdfEosGridInfo;
    }

    public void setProjectionParameter(double[] projectionParameter) {
        if (projectionParameter != null) {
            this.projectionParameter = projectionParameter.clone();
        }else {
            this.projectionParameter = null;
        }
    }

    public double[] getProjectionParameter() {
        return projectionParameter;
    }
}

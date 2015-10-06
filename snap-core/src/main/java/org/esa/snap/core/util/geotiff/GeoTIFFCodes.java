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
package org.esa.snap.core.util.geotiff;

public class GeoTIFFCodes extends IntMap {

    public static final int GTUserDefinedGeoKey = 32767;

    /* Generated from file geokey.properties */
    public static final int GTModelTypeGeoKey = 1024;
    public static final int GTRasterTypeGeoKey = 1025;
    public static final int GTCitationGeoKey = 1026;
    public static final int GeographicTypeGeoKey = 2048;
    public static final int GeogCitationGeoKey = 2049;
    public static final int GeogGeodeticDatumGeoKey = 2050;
    public static final int GeogPrimeMeridianGeoKey = 2051;
    public static final int GeogLinearUnitsGeoKey = 2052;
    public static final int GeogLinearUnitSizeGeoKey = 2053;
    public static final int GeogAngularUnitsGeoKey = 2054;
    public static final int GeogAngularUnitSizeGeoKey = 2055;
    public static final int GeogEllipsoidGeoKey = 2056;
    public static final int GeogSemiMajorAxisGeoKey = 2057;
    public static final int GeogSemiMinorAxisGeoKey = 2058;
    public static final int GeogInvFlatteningGeoKey = 2059;
    public static final int GeogAzimuthUnitsGeoKey = 2060;
    public static final int GeogPrimeMeridianLongGeoKey = 2061;
    public static final int GeogTOWGS84GeoKey = 2062;
    public static final int ProjectedCSTypeGeoKey = 3072;
    public static final int PCSCitationGeoKey = 3073;
    public static final int ProjectionGeoKey = 3074;
    public static final int ProjCoordTransGeoKey = 3075;
    public static final int ProjLinearUnitsGeoKey = 3076;
    public static final int ProjLinearUnitSizeGeoKey = 3077;
    public static final int ProjStdParallel1GeoKey = 3078;
    public static final int ProjStdParallelGeoKey = ProjStdParallel1GeoKey;
    public static final int ProjStdParallel2GeoKey = 3079;
    public static final int ProjNatOriginLongGeoKey = 3080;
    public static final int ProjOriginLongGeoKey = ProjNatOriginLongGeoKey;
    public static final int ProjNatOriginLatGeoKey = 3081;
    public static final int ProjOriginLatGeoKey = ProjNatOriginLatGeoKey;
    public static final int ProjFalseEastingGeoKey = 3082;
    public static final int ProjFalseNorthingGeoKey = 3083;
    public static final int ProjFalseOriginLongGeoKey = 3084;
    public static final int ProjFalseOriginLatGeoKey = 3085;
    public static final int ProjFalseOriginEastingGeoKey = 3086;
    public static final int ProjFalseOriginNorthingGeoKey = 3087;
    public static final int ProjCenterLongGeoKey = 3088;
    public static final int ProjCenterLatGeoKey = 3089;
    public static final int ProjCenterEastingGeoKey = 3090;
    public static final int ProjCenterNorthingGeoKey = 3091;
    public static final int ProjScaleAtNatOriginGeoKey = 3092;
    public static final int ProjScaleAtOriginGeoKey = ProjScaleAtNatOriginGeoKey;
    public static final int ProjScaleAtCenterGeoKey = 3093;
    public static final int ProjAzimuthAngleGeoKey = 3094;
    public static final int ProjStraightVertPoleLongGeoKey = 3095;
    public static final int VerticalCSTypeGeoKey = 4096;
    public static final int VerticalCitationGeoKey = 4097;
    public static final int VerticalDatumGeoKey = 4098;
    public static final int VerticalUnitsGeoKey = 4099;
    /* Generated from file geo_rasters.properties */
    public static final int RasterPixelIsArea = 1;
    public static final int RasterPixelIsPoint = 2;
    /* Generated from file geo_models.properties */
    public static final int ModelTypeProjected = 1;
    public static final int ModelTypeGeographic = 2;
    public static final int ModelTypeGeocentric = 3;
    public static final int ModelProjected = 1;
    public static final int ModelGeographic = 2;
    public static final int ModelGeocentric = 3;
    /* Generated from file geo_ctrans.properties */
    public static final int CT_TransverseMercator = 1;
    public static final int CT_TransvMercator_Modified_Alaska = 2;
    public static final int CT_ObliqueMercator = 3;
    public static final int CT_ObliqueMercator_Laborde = 4;
    public static final int CT_ObliqueMercator_Rosenmund = 5;
    public static final int CT_ObliqueMercator_Spherical = 6;
    public static final int CT_Mercator = 7;
    public static final int CT_LambertConfConic_2SP = 8;
    public static final int CT_LambertConfConic = CT_LambertConfConic_2SP;
    public static final int CT_LambertConfConic_1SP = 9;
    public static final int CT_LambertConfConic_Helmert = CT_LambertConfConic_1SP;
    public static final int CT_LambertAzimEqualArea = 10;
    public static final int CT_AlbersEqualArea = 11;
    public static final int CT_AzimuthalEquidistant = 12;
    public static final int CT_EquidistantConic = 13;
    public static final int CT_Stereographic = 14;
    public static final int CT_PolarStereographic = 15;
    public static final int CT_ObliqueStereographic = 16;
    public static final int CT_Equirectangular = 17;
    public static final int CT_CassiniSoldner = 18;
    public static final int CT_Gnomonic = 19;
    public static final int CT_MillerCylindrical = 20;
    public static final int CT_Orthographic = 21;
    public static final int CT_Polyconic = 22;
    public static final int CT_Robinson = 23;
    public static final int CT_Sinusoidal = 24;
    public static final int CT_VanDerGrinten = 25;
    public static final int CT_NewZealandMapGrid = 26;
    public static final int CT_TransvMercator_SouthOriented = 27;
    public static final int CT_SouthOrientedGaussConformal = CT_TransvMercator_SouthOriented;
    public static final int CT_AlaskaConformal = CT_TransvMercator_Modified_Alaska;
    public static final int CT_TransvEquidistCylindrical = CT_CassiniSoldner;
    public static final int CT_ObliqueMercator_Hotine = CT_ObliqueMercator;
    public static final int CT_SwissObliqueCylindrical = CT_ObliqueMercator_Rosenmund;
    public static final int CT_GaussBoaga = CT_TransverseMercator;
    public static final int CT_GaussKruger = CT_TransverseMercator;

    public static GeoTIFFCodes getInstance(){
        return Holder.instance;
    }

    private GeoTIFFCodes() {
        init(GeoTIFFCodes.class.getFields());
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final GeoTIFFCodes instance = new GeoTIFFCodes();
    }
}

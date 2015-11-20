/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/**
 * Contains SNAP's central classes and interfaces used for an in-memory presentation
 * of remote sensing data products.
 * <p>
 * The {@link org.esa.snap.core.datamodel.Product} class plays a central role in SNAP this package and is used
 * throughout the SNAP software in order represent data products of many different types.
 * <p>
 * Many of the classes in this package are superclasses of the {@link org.esa.snap.core.datamodel.ProductNode},
 * which acts as a base class for all data and metadata items contained in a product.
 * The inheritance hierarchy of the abstract {@link org.esa.snap.core.datamodel.ProductNode} is as follows:
 *
 * <ul>
 *      <li>{@link org.esa.snap.core.datamodel.Product}</li>
 *      <li>{@link org.esa.snap.core.datamodel.DataNode}</li>
 *      <ul>
 *          <li>{@link org.esa.snap.core.datamodel.RasterDataNode}</li>
 *          <ul>
 *              <li>{@link org.esa.snap.core.datamodel.TiePointGrid}</li>
 *              <li>{@link org.esa.snap.core.datamodel.AbstractBand}</li>
 *              <ul>
 *                  <li>{@link org.esa.snap.core.datamodel.Band}</li>
 *                  <li>{@link org.esa.snap.core.datamodel.VirtualBand}</li>
 *                  <li>{@link org.esa.snap.core.datamodel.Mask}</li>
 *                  <li>{@link org.esa.snap.core.datamodel.FilterBand}</li>
 *                  <ul>
 *                      <li>{@link org.esa.snap.core.datamodel.ConvolutionFilterBand}</li>
 *                      <li>{@link org.esa.snap.core.datamodel.GeneralFilterBand}</li>
 *                  </ul>
 *              </ul>
 *          </ul>
 *          <li>{@link org.esa.snap.core.datamodel.MetadataAttribute}</li>
 *          <ul>
 *              <li>{@link org.esa.snap.core.datamodel.SampleCoding}</li>
 *              <ul>
 *                  <li>{@link org.esa.snap.core.datamodel.FlagCoding}</li>
 *                  <li>{@link org.esa.snap.core.datamodel.IndexCoding}</li>
 *              </ul>
 *          </ul>
 *      </ul>
 *      <li>{@link org.esa.snap.core.datamodel.MetadataElement}</li>
 *      <li>{@link org.esa.snap.core.datamodel.VectorDataNode}</li>
 *  </ul>
 *
 * {@link org.esa.snap.core.datamodel.Product} instances are composed as follows:
 * <ul>
 *      <li>Always one {@link org.esa.snap.core.datamodel.MetadataElement} named "metadata"</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.TiePointGrid}s</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.Band}s</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.Mask}s</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.SampleCoding}s</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.VectorDataNode}s</li>
 *      <li>Zero or one <i>scene</i> {@link org.esa.snap.core.datamodel.GeoCoding}s</li>
 * </ul>
 *
 * {@link org.esa.snap.core.datamodel.MetadataElement} instances are composed as follows:
 * <ul>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.MetadataElement}s</li>
 *      <li>Zero or more {@link org.esa.snap.core.datamodel.MetadataAttribute}s</li>
 * </ul>
 *
 * <p>
 * {@code Product}s and {@code RasterDataNode}s can have an associated {@link org.esa.snap.core.datamodel.GeoCoding}
 * allowing them to geo-locate raster pixel positions with geographic positions and vice versa. There are various
 * possible {@code GeoCoding} implementations which all derive from {@link org.esa.snap.core.datamodel.AbstractGeoCoding}:
 *
 * <ul>
 *     <li>{@link org.esa.snap.core.datamodel.TiePointGeoCoding} - geo-coding based on latitude/longitude {@code TiePointGrid}s</li>
 *     <li>{@link org.esa.snap.core.datamodel.PixelGeoCoding} - geo-coding based on per-pixel latitude/longitude {@code Band}s</li>
 *     <li>{@link org.esa.snap.core.datamodel.PixelGeoCoding2} - alternative geo-coding based on per-pixel latitude/longitude {@code Band}s (current default)</li>
 *     <li>{@link org.esa.snap.core.datamodel.CrsGeoCoding} - geo-coding for reprojected products, i.e. products having raster data rectified w.r.t. geodetic coordinate reference system (CRS)</li>
 *     <li>{@link org.esa.snap.core.datamodel.GcpGeoCoding} - geo-coding based on ground control point {@link org.esa.snap.core.datamodel.Placemark}s</li>
 *     <li>{@link org.esa.snap.core.datamodel.FXYGeoCoding}- geo-coding based on X,Y-functions</li>
 * </ul>
 *
 * <p>
 * There are various places in the SNAP API that provide utility functions that deal the above mentioned classes and
 * interfaces.
 * <ul>
 * <li>{@link org.esa.snap.core.util.ProductUtils} - Static utilities methods around the
 * {@link org.esa.snap.core.datamodel.Product Product} and its components</li>
 * <li>{@link org.esa.snap.core.util.FeatureUtils} - Static utilities methods around the
 * {@link org.opengis.feature.simple.SimpleFeature SimpleFeature} and
 * {@link com.vividsolutions.jts.geom.Geometry Geometry} used in conjunction with
 * {@link org.esa.snap.core.datamodel.VectorDataNode}</li>
 * </ul>
 * <p>
 * A strongly related packages are {@link org.esa.snap.core.dataio} which provides the data product I/O framework
 * and {@link org.esa.snap.core.gpf} which is <i>graph processing framework</i> GPF used for developing and executing
 * raster data operators and graphs of such operators.
 * <p>
 */
package org.esa.snap.core.datamodel;
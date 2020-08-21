/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac;

import com.bc.ceres.core.ProgressMonitor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s1tbx.stac.extensions.EO;
import org.esa.s1tbx.stac.extensions.Sat;
import org.esa.s1tbx.stac.extensions.View;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeWriter;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;
import org.geotools.feature.*;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class STACProductWriter extends AbstractProductWriter {

    private static final GeoTiffProductWriterPlugIn geoTiffProductWriterPlugIn = new GeoTiffProductWriterPlugIn();

    private final Map<Band, ProductWriter> bandWriterMap = new HashMap<>();
    private final Map<Band, Band> bandMap = new HashMap<>();
    private final boolean singleBand;

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn) {
        this(writerPlugIn, false);
    }

    public STACProductWriter(final ProductWriterPlugIn writerPlugIn, final boolean singleBand) {
        super(writerPlugIn);
        this.singleBand = singleBand;
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {

        final Product srcProduct = getSourceProduct();
        if (getOutput() instanceof ImageOutputStream) {
            //writeGeoTIFFProduct((ImageOutputStream)getOutput(), srcProduct);
        } else {
            File imageFile;
            if (getOutput() instanceof String) {
                imageFile = new File((String) getOutput());
            } else {
                imageFile = (File) getOutput();
            }
            imageFile.getParentFile().mkdirs();

            writeVectorData(imageFile, srcProduct);
            writeProductMetadata(imageFile, srcProduct);

            String baseName = imageFile.getName();
            if(baseName.endsWith(STACProductConstants.IMAGE_GEOTIFF_EXT)) {
                baseName = baseName.substring(0, baseName.length()-4);
            }

            if(singleBand) {
                for(Band srcBand : srcProduct.getBands()) {
                    ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
                    imageFile = new File(imageFile.getParentFile(), baseName + "_" + srcBand.getName() + STACProductConstants.IMAGE_GEOTIFF_EXT);

                    Product trgProduct = new Product(srcProduct.getName(), srcProduct.getProductType(), srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight());
                    ProductUtils.copyMetadata(srcProduct, trgProduct);
                    ProductUtils.copyTiePointGrids(srcProduct, trgProduct);
                    ProductUtils.copyFlagCodings(srcProduct, trgProduct);
                    //ProductUtils.copyFlagBands(srcProduct, trgProduct, true);
                    ProductUtils.copyGeoCoding(srcProduct, trgProduct);
                    ProductUtils.copyMasks(srcProduct, trgProduct);
                    ProductUtils.copyVectorData(srcProduct, trgProduct);
                    ProductUtils.copyIndexCodings(srcProduct, trgProduct);
                    ProductUtils.copyQuicklookBandName(srcProduct, trgProduct);
                    trgProduct.setStartTime(srcProduct.getStartTime());
                    trgProduct.setEndTime(srcProduct.getEndTime());
                    trgProduct.setDescription(srcProduct.getDescription());
                    trgProduct.setAutoGrouping(srcProduct.getAutoGrouping());

                    Band trgBand = ProductUtils.copyBand(srcBand.getName(), srcProduct, trgProduct, true);

                    bandWriter.writeProductNodes(trgProduct, imageFile);

                    bandWriterMap.put(srcBand, bandWriter);
                    bandMap.put(srcBand, trgBand);
                }
            } else {
                ProductWriter bandWriter = geoTiffProductWriterPlugIn.createWriterInstance();
                bandWriter.writeProductNodes(srcProduct, imageFile);

                for(Band srcBand : srcProduct.getBands()) {
                    bandWriterMap.put(srcBand, bandWriter);
                    bandMap.put(srcBand, srcBand);
                }
            }
        }
    }

    private void writeProductMetadata(final File imageFile, final Product product) throws IOException {

        final File metadataFile = FileUtils.exchangeExtension(imageFile, STACProductConstants.METADATA_EXT);
        final FileWriter metaStringWriter = new FileWriter(metadataFile);

        final JSONObject json = new JSONObject();

        final StacItem stacItem = new StacItem(json, product.getName());
        stacItem.addExtension(EO.eo, View.view, Sat.sat);
        stacItem.addKeywords(EO.KeyWords.earth_observation, EO.KeyWords.satellite);
        json.put(StacItem.description, product.getDescription());

        metaStringWriter.write(prettyPrint(json));
        metaStringWriter.close();
    }

    private String prettyPrint(final JSONObject json) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(json);
        } catch (Exception e) {
            System.out.println("Unable to pretty print " + json.toJSONString());
            return json.toJSONString();
        }
    }

    private void writeVectorData(final File imageFile, final Product srcProduct) {
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = srcProduct.getVectorDataGroup();
        if (vectorDataGroup.getNodeCount() > 0) {
            final File vectorFile = FileUtils.exchangeExtension(imageFile, ".json");
            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                writeVectorData(vectorFile, vectorDataGroup.get(i));
            }
        }
    }

    private void writeVectorData(final File vectorFile0, final VectorDataNode vectorDataNode) {
        try {
            if (!vectorDataNode.getFeatureCollection().isEmpty()) {
                final File vectorFile = FileUtils.exchangeExtension(vectorFile0,
                        "_" + vectorDataNode.getName() + "_vector" + VectorDataNodeIO.FILENAME_EXTENSION);

                VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
                vectorDataNodeWriter.write(vectorDataNode, vectorFile);

                //writeVectorJSON(vectorFile0, vectorDataNode);
            }
        } catch (IOException e) {
            SystemUtils.LOG.throwing("SkyWatchProductWriter", "writeVectorData", e);
        }
    }

    public static File writeVectorJSON(final File vectorFile0, final VectorDataNode vectorDataNode) throws Exception {
        final File vectorFile = FileUtils.exchangeExtension(vectorFile0, "_" + vectorDataNode.getName() + ".json");
        try (FileWriter fileWriter = new FileWriter(vectorFile)) {
            final FeatureJSON fjson = new FeatureJSON();
            fjson.setEncodeFeatureCRS(true);
            fjson.setEncodeFeatureBounds(true);

            FeatureCollection fc = vectorDataNode.getFeatureCollection();
            Map<Class<?>, List<SimpleFeature>> featureListMap = createGeometryToFeaturesListMap(vectorDataNode);

            DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(fc.getID(), vectorDataNode.getFeatureType());
            for(List<SimpleFeature> polygonFeatures : featureListMap.values()) {
                featureCollection.addAll(polygonFeatures);
            }

            fjson.writeFeatureCollection(featureCollection, fileWriter);
            fileWriter.flush();
        }
        return vectorFile;
    }

    private static Map<Class<?>, List<SimpleFeature>> createGeometryToFeaturesListMap(VectorDataNode vectorNode)
            throws TransformException, SchemaException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorNode.getFeatureCollection();
        CoordinateReferenceSystem crs = vectorNode.getFeatureType().getCoordinateReferenceSystem();
        if (crs == null) {   // for pins and GCPs crs is null
            crs = vectorNode.getProduct().getSceneCRS();
        }
        final CoordinateReferenceSystem modelCrs;
        if (vectorNode.getProduct().getSceneGeoCoding() instanceof CrsGeoCoding) {
            modelCrs = vectorNode.getProduct().getSceneCRS();
        } else {
            modelCrs = DefaultGeographicCRS.WGS84;
        }

        // Not using ReprojectingFeatureCollection - it is reprojecting all geometries of a feature
        // but we want to reproject the default geometry only
        GeometryCoordinateSequenceTransformer transformer = createTransformer(crs, modelCrs);

        Map<Class<?>, List<SimpleFeature>> featureListMap = new HashMap<>();
        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        // The schema needs to be reprojected. We need to build a new feature be cause we can't change the schema.
        SimpleFeatureType schema = featureCollection.getSchema();
        SimpleFeatureType transformedSchema = FeatureTypes.transform(schema, modelCrs);
        while (featureIterator.hasNext()) {
            SimpleFeature feature = featureIterator.next();
            Object defaultGeometry = feature.getDefaultGeometry();
            feature.setDefaultGeometry(transformer.transform((Geometry) defaultGeometry));

            Class<?> geometryType = defaultGeometry.getClass();
            List<SimpleFeature> featureList = featureListMap.computeIfAbsent(geometryType, k -> new ArrayList<>());
            SimpleFeature exportFeature = SimpleFeatureBuilder.build(transformedSchema, feature.getAttributes(), feature.getID());
            featureList.add(exportFeature);
        }
        return featureListMap;
    }

    private static GeometryCoordinateSequenceTransformer createTransformer(CoordinateReferenceSystem crs, CoordinateReferenceSystem modelCrs) {
        GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
        try {
            MathTransform reprojTransform = CRS.findMathTransform(crs, modelCrs, true);
            transformer.setMathTransform(reprojTransform);
            return transformer;
        } catch (FactoryException e) {
            throw new IllegalStateException("Could not create math transform", e);
        }
    }

    @Override
    public void writeBandRasterData(final Band sourceBand,
                                                 final int sourceOffsetX,
                                                 final int sourceOffsetY,
                                                 final int sourceWidth,
                                                 final int sourceHeight,
                                                 final ProductData sourceBuffer,
                                                 ProgressMonitor pm) throws IOException {
        if(sourceBand instanceof VirtualBand) {

        } else {
            ProductWriter bandWriter = bandWriterMap.get(sourceBand);
            bandWriter.writeBandRasterData(bandMap.get(sourceBand),
                    sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceBuffer, pm);
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && !(node instanceof FilterBand);
    }

    @Override
    public void flush() throws IOException {
        for(ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for(ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.close();
        }
    }

    @Override
    public void deleteOutput() throws IOException {
        for(ProductWriter bandWriter : bandWriterMap.values()) {
            bandWriter.deleteOutput();
        }
    }
}

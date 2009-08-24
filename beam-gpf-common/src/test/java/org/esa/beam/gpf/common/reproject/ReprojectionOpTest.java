package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import java.io.File;

public class ReprojectionOpTest {

    private static final String epsgCode = "EPSG:4326";
    private static final String wkt = "GEOGCS[\"Hu Tzu Shan\"," +
                                      "DATUM[\"Hu_Tzu_Shan\"," +
                                      "SPHEROID[\"International 1924\",6378388,297,AUTHORITY[\"EPSG\",\"7022\"]]," +
                                      "TOWGS84[-637,-549,-203,0,0,0,0],AUTHORITY[\"EPSG\",\"6236\"]]," +
                                      "PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
                                      "UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]]," +
                                      "AUTHORITY[\"EPSG\",\"4236\"]]";
    private static final File wktFile = new File("file.wkt");
    private static final Product collocateProduct = new Product("P", "T", 2,2);

    @Test
    public void testParameterAmbigouity_epsgCode() {
        final ReprojectionOp op = new ReprojectionOp();
        op.setEpsgCode(epsgCode);
        op.validateParameters();
    }

    @Test
    public void testParameterAmbigouity_wktFile() {
        final ReprojectionOp op = new ReprojectionOp();
        op.setWktFile(wktFile);
        op.validateParameters();
    }

    @Test
    public void testParameterAmbigouity_wkt() {
        final ReprojectionOp op = new ReprojectionOp();
        op.setWkt(wkt);

        op.validateParameters();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_AllNull() {
        final ReprojectionOp op = new ReprojectionOp();
        op.validateParameters();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_epsgCode_wkt() {
        final ReprojectionOp op = new ReprojectionOp();
        op.setEpsgCode(epsgCode);
        op.setWkt(wkt);
        op.validateParameters();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_wktFile() {
        final ReprojectionOp op = new ReprojectionOp();
        op.setWkt(wkt);
        op.setWktFile(wktFile);
        op.validateParameters();
    }
}

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.*;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.table.TableModel;
import java.awt.geom.Path2D;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class ProfileDataTableModelTest {
    private Product product;
    private Band band;
    private Path2D path;
    private ProfilePlotPanel.DataSourceConfig dataSourceConfig;

    @Before
    public void setUp() throws Exception {
        // Draw a "Z"
        path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(3, 0);
        path.lineTo(0, 3);
        path.lineTo(3, 3);

        product = new Product("p", "t", 4, 4);
        band = product.addBand("b", "4 * (Y-0.5) + (X-0.5) + 0.1");

        dataSourceConfig = new ProfilePlotPanel.DataSourceConfig();
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("ft");
        ftb.add("lat", Double.class);
        ftb.add("lon", Double.class);
        ftb.add("data", Double.class);
        SimpleFeatureType ft = ftb.buildFeatureType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection("id", ft);
        fc.add(new SimpleFeatureImpl(new Object[]{0, 0, 0.3}, ft, new FeatureIdImpl("id1"), false));
        fc.add(new SimpleFeatureImpl(new Object[]{0, 0, 0.5}, ft, new FeatureIdImpl("id2"), false));
        fc.add(new SimpleFeatureImpl(new Object[]{0, 0, 0.7}, ft, new FeatureIdImpl("id3"), false));
        fc.add(new SimpleFeatureImpl(new Object[]{0, 0, 0.1}, ft, new FeatureIdImpl("id4"), false));
        dataSourceConfig.pointDataSource = new VectorDataNode("vd", fc);
        dataSourceConfig.dataField = ft.getDescriptor("data");
        dataSourceConfig.boxSize = 1;
        dataSourceConfig.computeInBetweenPoints = true;
    }

    @Test
    public void testModelWithCorrData() throws Exception {

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        TableModel tableModel = new ProfileDataTableModel("some Type", band.getName(), profileData, dataSourceConfig);

        assertEquals(12, tableModel.getColumnCount());

        assertEquals("raster", tableModel.getColumnName(0));
        assertEquals("pixel_no", tableModel.getColumnName(1));
        assertEquals("pixel_x", tableModel.getColumnName(2));
        assertEquals("pixel_y", tableModel.getColumnName(3));
        assertEquals("latitude", tableModel.getColumnName(4));
        assertEquals("longitude", tableModel.getColumnName(5));
        assertEquals("b_mean", tableModel.getColumnName(6));
        assertEquals("b_sigma", tableModel.getColumnName(7));
        assertEquals("reference", tableModel.getColumnName(8));
        assertEquals("data", tableModel.getColumnName(9));
        assertEquals("lat", tableModel.getColumnName(10));
        assertEquals("lon", tableModel.getColumnName(11));

        assertEquals(String.class, tableModel.getColumnClass(0));
        assertEquals(Integer.class, tableModel.getColumnClass(1));
        assertEquals(Integer.class, tableModel.getColumnClass(2));
        assertEquals(Integer.class, tableModel.getColumnClass(3));
        assertEquals(Float.class, tableModel.getColumnClass(4));
        assertEquals(Float.class, tableModel.getColumnClass(5));
        assertEquals(Float.class, tableModel.getColumnClass(6));
        assertEquals(Float.class, tableModel.getColumnClass(7));
        assertEquals(String.class, tableModel.getColumnClass(8));
        assertEquals(Double.class, tableModel.getColumnClass(9));
        assertEquals(Object.class, tableModel.getColumnClass(10));
        assertEquals(Object.class, tableModel.getColumnClass(11));

        assertEquals(10, tableModel.getRowCount());

        assertEquals("some Type", tableModel.getValueAt(0, 0));
        assertEquals(1, tableModel.getValueAt(0, 1));
        assertEquals(0.0, tableModel.getValueAt(0, 2));
        assertEquals(0.0, tableModel.getValueAt(0, 3));
        assertEquals(null, tableModel.getValueAt(0, 4));
        assertEquals(null, tableModel.getValueAt(0, 5));
        assertEquals(0.1F, tableModel.getValueAt(0, 6));
        assertEquals(0.0F, tableModel.getValueAt(0, 7));
        assertEquals("ref data", tableModel.getValueAt(0, 8));
        assertEquals(0.3, tableModel.getValueAt(0, 9));
        assertEquals(0, tableModel.getValueAt(0, 10));
        assertEquals(0, tableModel.getValueAt(0, 11));

        assertEquals("some Type", tableModel.getValueAt(1, 0));
        assertEquals(2, tableModel.getValueAt(1, 1));
        assertEquals(1.0, tableModel.getValueAt(1, 2));
        assertEquals(0.0, tableModel.getValueAt(1, 3));
        assertEquals(null, tableModel.getValueAt(1, 4));
        assertEquals(null, tableModel.getValueAt(1, 5));
        assertEquals(1.1F, tableModel.getValueAt(1, 6));
        assertEquals(0.0F, tableModel.getValueAt(1, 7));
        assertEquals("ref data", tableModel.getValueAt(1, 8));
        assertEquals(null, tableModel.getValueAt(1, 9));
        assertEquals(null, tableModel.getValueAt(1, 10));
        assertEquals(null, tableModel.getValueAt(1, 11));

        assertEquals("some Type", tableModel.getValueAt(9, 0));
        assertEquals(10, tableModel.getValueAt(9, 1));
        assertEquals(3.0, tableModel.getValueAt(9, 2));
        assertEquals(3.0, tableModel.getValueAt(9, 3));
        assertEquals(null, tableModel.getValueAt(9, 4));
        assertEquals(null, tableModel.getValueAt(9, 5));
        assertEquals(15.1F, tableModel.getValueAt(9, 6));
        assertEquals(0.0F, tableModel.getValueAt(9, 7));
        assertEquals("ref data", tableModel.getValueAt(9, 8));
        assertEquals(0.1, tableModel.getValueAt(9, 9));
        assertEquals(0, tableModel.getValueAt(9, 10));
        assertEquals(0, tableModel.getValueAt(9, 11));

    }

    @Test
    public void testModelWithoutCorrData() throws Exception {
        dataSourceConfig.dataField = null;
        dataSourceConfig.boxSize = 1;

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        TableModel tableModel = new ProfileDataTableModel(null, band.getName(), profileData, dataSourceConfig);

        assertEquals(10, tableModel.getColumnCount());

        assertEquals("raster", tableModel.getColumnName(0));
        assertEquals("pixel_no", tableModel.getColumnName(1));
        assertEquals("pixel_x", tableModel.getColumnName(2));
        assertEquals("pixel_y", tableModel.getColumnName(3));
        assertEquals("latitude", tableModel.getColumnName(4));
        assertEquals("longitude", tableModel.getColumnName(5));
        assertEquals("b_mean", tableModel.getColumnName(6));
        assertEquals("b_sigma", tableModel.getColumnName(7));
        assertEquals("reference", tableModel.getColumnName(8));
        assertEquals("", tableModel.getColumnName(9));

        assertEquals(String.class, tableModel.getColumnClass(0));
        assertEquals(Integer.class, tableModel.getColumnClass(1));
        assertEquals(Integer.class, tableModel.getColumnClass(2));
        assertEquals(Integer.class, tableModel.getColumnClass(3));
        assertEquals(Float.class, tableModel.getColumnClass(4));
        assertEquals(Float.class, tableModel.getColumnClass(5));
        assertEquals(Float.class, tableModel.getColumnClass(6));
        assertEquals(Float.class, tableModel.getColumnClass(7));
        assertEquals(String.class, tableModel.getColumnClass(8));
        assertEquals(Object.class, tableModel.getColumnClass(9));

        assertEquals(10, tableModel.getRowCount());

        assertEquals("raster", tableModel.getValueAt(0, 0));
        assertEquals(1, tableModel.getValueAt(0, 1));
        assertEquals(0.0, tableModel.getValueAt(0, 2));
        assertEquals(0.0, tableModel.getValueAt(0, 3));
        assertEquals(null, tableModel.getValueAt(0, 4));
        assertEquals(null, tableModel.getValueAt(0, 5));
        assertEquals(0.1F, tableModel.getValueAt(0, 6));
        assertEquals(0.0F, tableModel.getValueAt(0, 7));
        assertEquals("ref data", tableModel.getValueAt(0, 8));
        assertEquals(null, tableModel.getValueAt(0, 9));

        assertEquals("raster", tableModel.getValueAt(1, 0));
        assertEquals(2, tableModel.getValueAt(1, 1));
        assertEquals(1.0, tableModel.getValueAt(1, 2));
        assertEquals(0.0, tableModel.getValueAt(1, 3));
        assertEquals(null, tableModel.getValueAt(1, 4));
        assertEquals(null, tableModel.getValueAt(1, 5));
        assertEquals(1.1F, tableModel.getValueAt(1, 6));
        assertEquals(0.0F, tableModel.getValueAt(1, 7));
        assertEquals("ref data", tableModel.getValueAt(1, 8));
        assertEquals(null, tableModel.getValueAt(1, 9));

        assertEquals("raster", tableModel.getValueAt(9, 0));
        assertEquals(10, tableModel.getValueAt(9, 1));
        assertEquals(3.0, tableModel.getValueAt(9, 2));
        assertEquals(3.0, tableModel.getValueAt(9, 3));
        assertEquals(null, tableModel.getValueAt(9, 4));
        assertEquals(null, tableModel.getValueAt(9, 5));
        assertEquals(15.1F, tableModel.getValueAt(9, 6));
        assertEquals(0.0F, tableModel.getValueAt(9, 7));
        assertEquals("ref data", tableModel.getValueAt(9, 8));
        assertEquals(null, tableModel.getValueAt(9, 9));
    }


    @Test
    public void testModelCsv() throws Exception {

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        ProfileDataTableModel tableModel = new ProfileDataTableModel(" ", band.getName(), profileData, dataSourceConfig);
        String csv = tableModel.toCsv();
        assertEquals("raster\tpixel_no\tpixel_x\tpixel_y\tlatitude\tlongitude\tb_mean\tb_sigma\treference\tdata\tlat\tlon\n" +
                             "raster\t1\t0.0\t0.0\t\t\t0.1\t0.0\tref data\t0.3\t0\t0\n" +
                             "raster\t2\t1.0\t0.0\t\t\t1.1\t0.0\tref data\t\t\t\n" +
                             "raster\t3\t2.0\t0.0\t\t\t2.1\t0.0\tref data\t\t\t\n" +
                             "raster\t4\t3.0\t0.0\t\t\t3.1\t0.0\tref data\t0.5\t0\t0\n" +
                             "raster\t5\t2.0\t1.0\t\t\t6.1\t0.0\tref data\t\t\t\n" +
                             "raster\t6\t1.0\t2.0\t\t\t9.1\t0.0\tref data\t\t\t\n" +
                             "raster\t7\t0.0\t3.0\t\t\t12.1\t0.0\tref data\t0.7\t0\t0\n" +
                             "raster\t8\t1.0\t3.0\t\t\t13.1\t0.0\tref data\t\t\t\n" +
                             "raster\t9\t2.0\t3.0\t\t\t14.1\t0.0\tref data\t\t\t\n" +
                             "raster\t10\t3.0\t3.0\t\t\t15.1\t0.0\tref data\t0.1\t0\t0\n", csv);
    }

    @Test
    public void testModelCsvNoInBetweenPoints() throws Exception {

        dataSourceConfig.computeInBetweenPoints = false;

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        ProfileDataTableModel tableModel = new ProfileDataTableModel("some Type", band.getName(), profileData, dataSourceConfig);
        String csv = tableModel.toCsv();
        assertEquals("raster\tpixel_no\tpixel_x\tpixel_y\tlatitude\tlongitude\tb_mean\tb_sigma\treference\tdata\tlat\tlon\n" +
                             "some Type\t1\t0.0\t0.0\t\t\t0.1\t0.0\tref data\t0.3\t0\t0\n" +
                             "some Type\t4\t3.0\t0.0\t\t\t3.1\t0.0\tref data\t0.5\t0\t0\n" +
                             "some Type\t7\t0.0\t3.0\t\t\t12.1\t0.0\tref data\t0.7\t0\t0\n" +
                             "some Type\t10\t3.0\t3.0\t\t\t15.1\t0.0\tref data\t0.1\t0\t0\n", csv);
    }
}
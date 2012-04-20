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

        TableModel tableModel = new ProfileDataTableModel(band.getName(), profileData, dataSourceConfig);

        assertEquals(8, tableModel.getColumnCount());

        assertEquals("pixel_no", tableModel.getColumnName(0));
        assertEquals("pixel_x", tableModel.getColumnName(1));
        assertEquals("pixel_y", tableModel.getColumnName(2));
        assertEquals("latitude", tableModel.getColumnName(3));
        assertEquals("longitude", tableModel.getColumnName(4));
        assertEquals("b_mean", tableModel.getColumnName(5));
        assertEquals("b_sigma", tableModel.getColumnName(6));
        assertEquals("data", tableModel.getColumnName(7));

        assertEquals(Integer.class, tableModel.getColumnClass(0));
        assertEquals(Integer.class, tableModel.getColumnClass(1));
        assertEquals(Integer.class, tableModel.getColumnClass(2));
        assertEquals(Float.class, tableModel.getColumnClass(3));
        assertEquals(Float.class, tableModel.getColumnClass(4));
        assertEquals(Float.class, tableModel.getColumnClass(5));
        assertEquals(Float.class, tableModel.getColumnClass(6));
        assertEquals(Double.class, tableModel.getColumnClass(7));

        assertEquals(10, tableModel.getRowCount());

        assertEquals(1, tableModel.getValueAt(0, 0));
        assertEquals(0.0, tableModel.getValueAt(0, 1));
        assertEquals(0.0, tableModel.getValueAt(0, 2));
        assertEquals(null, tableModel.getValueAt(0, 3));
        assertEquals(null, tableModel.getValueAt(0, 4));
        assertEquals(0.1F, tableModel.getValueAt(0, 5));
        assertEquals(0.0F, tableModel.getValueAt(0, 6));
        assertEquals(0.3, tableModel.getValueAt(0, 7));

        assertEquals(2, tableModel.getValueAt(1, 0));
        assertEquals(1.0, tableModel.getValueAt(1, 1));
        assertEquals(0.0, tableModel.getValueAt(1, 2));
        assertEquals(null, tableModel.getValueAt(1, 3));
        assertEquals(null, tableModel.getValueAt(1, 4));
        assertEquals(1.1F, tableModel.getValueAt(1, 5));
        assertEquals(0.0F, tableModel.getValueAt(1, 6));
        assertEquals(null, tableModel.getValueAt(1, 7));

        assertEquals(10, tableModel.getValueAt(9, 0));
        assertEquals(3.0, tableModel.getValueAt(9, 1));
        assertEquals(3.0, tableModel.getValueAt(9, 2));
        assertEquals(null, tableModel.getValueAt(9, 3));
        assertEquals(null, tableModel.getValueAt(9, 4));
        assertEquals(15.1F, tableModel.getValueAt(9, 5));
        assertEquals(0.0F, tableModel.getValueAt(9, 6));
        assertEquals(0.1, tableModel.getValueAt(9, 7));

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

        TableModel tableModel = new ProfileDataTableModel(band.getName(), profileData, dataSourceConfig);

        assertEquals(8, tableModel.getColumnCount());

        assertEquals("pixel_no", tableModel.getColumnName(0));
        assertEquals("pixel_x", tableModel.getColumnName(1));
        assertEquals("pixel_y", tableModel.getColumnName(2));
        assertEquals("latitude", tableModel.getColumnName(3));
        assertEquals("longitude", tableModel.getColumnName(4));
        assertEquals("b_mean", tableModel.getColumnName(5));
        assertEquals("b_sigma", tableModel.getColumnName(6));
        assertEquals("", tableModel.getColumnName(7));

        assertEquals(Integer.class, tableModel.getColumnClass(0));
        assertEquals(Integer.class, tableModel.getColumnClass(1));
        assertEquals(Integer.class, tableModel.getColumnClass(2));
        assertEquals(Float.class, tableModel.getColumnClass(3));
        assertEquals(Float.class, tableModel.getColumnClass(4));
        assertEquals(Float.class, tableModel.getColumnClass(5));
        assertEquals(Float.class, tableModel.getColumnClass(6));
        assertEquals(Object.class, tableModel.getColumnClass(7));

        assertEquals(10, tableModel.getRowCount());

        assertEquals(1, tableModel.getValueAt(0, 0));
        assertEquals(0.0, tableModel.getValueAt(0, 1));
        assertEquals(0.0, tableModel.getValueAt(0, 2));
        assertEquals(null, tableModel.getValueAt(0, 3));
        assertEquals(null, tableModel.getValueAt(0, 4));
        assertEquals(0.1F, tableModel.getValueAt(0, 5));
        assertEquals(0.0F, tableModel.getValueAt(0, 6));
        assertEquals(null, tableModel.getValueAt(0, 7));

        assertEquals(2, tableModel.getValueAt(1, 0));
        assertEquals(1.0, tableModel.getValueAt(1, 1));
        assertEquals(0.0, tableModel.getValueAt(1, 2));
        assertEquals(null, tableModel.getValueAt(1, 3));
        assertEquals(null, tableModel.getValueAt(1, 4));
        assertEquals(1.1F, tableModel.getValueAt(1, 5));
        assertEquals(0.0F, tableModel.getValueAt(1, 6));
        assertEquals(null, tableModel.getValueAt(1, 7));

        assertEquals(10, tableModel.getValueAt(9, 0));
        assertEquals(3.0, tableModel.getValueAt(9, 1));
        assertEquals(3.0, tableModel.getValueAt(9, 2));
        assertEquals(null, tableModel.getValueAt(9, 3));
        assertEquals(null, tableModel.getValueAt(9, 4));
        assertEquals(15.1F, tableModel.getValueAt(9, 5));
        assertEquals(0.0F, tableModel.getValueAt(9, 6));
        assertEquals(null, tableModel.getValueAt(9, 7));
    }


    @Test
    public void testModelCsv() throws Exception {

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        ProfileDataTableModel tableModel = new ProfileDataTableModel(band.getName(), profileData, dataSourceConfig);
        String csv = tableModel.toCsv();
        assertEquals("pixel_no\tpixel_x\tpixel_y\tlatitude\tlongitude\tb_mean\tb_sigma\tdata\n" +
                             "1\t0.0\t0.0\t\t\t0.1\t0.0\t0.3\n" +
                             "2\t1.0\t0.0\t\t\t1.1\t0.0\t\n" +
                             "3\t2.0\t0.0\t\t\t2.1\t0.0\t\n" +
                             "4\t3.0\t0.0\t\t\t3.1\t0.0\t0.5\n" +
                             "5\t2.0\t1.0\t\t\t6.1\t0.0\t\n" +
                             "6\t1.0\t2.0\t\t\t9.1\t0.0\t\n" +
                             "7\t0.0\t3.0\t\t\t12.1\t0.0\t0.7\n" +
                             "8\t1.0\t3.0\t\t\t13.1\t0.0\t\n" +
                             "9\t2.0\t3.0\t\t\t14.1\t0.0\t\n" +
                             "10\t3.0\t3.0\t\t\t15.1\t0.0\t0.1\n", csv);
    }

    @Test
    public void testModelCsvNoInBetweenPoints() throws Exception {

        dataSourceConfig.computeInBetweenPoints = false;

        TransectProfileData profileData = new TransectProfileDataBuilder()
                .raster(band)
                .path(path)
                .boxSize(dataSourceConfig.boxSize)
                .build();

        ProfileDataTableModel tableModel = new ProfileDataTableModel(band.getName(), profileData, dataSourceConfig);
        String csv = tableModel.toCsv();
        assertEquals("pixel_no\tpixel_x\tpixel_y\tlatitude\tlongitude\tb_mean\tb_sigma\tdata\n" +
                             "1\t0.0\t0.0\t\t\t0.1\t0.0\t0.3\n" +
                             "4\t3.0\t0.0\t\t\t3.1\t0.0\t0.5\n" +
                             "7\t0.0\t3.0\t\t\t12.1\t0.0\t0.7\n" +
                             "10\t3.0\t3.0\t\t\t15.1\t0.0\t0.1\n", csv);
    }
}
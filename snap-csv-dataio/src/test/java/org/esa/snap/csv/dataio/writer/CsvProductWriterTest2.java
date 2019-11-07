/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class CsvProductWriterTest2 {


    @Test
    public void testWriteFeatures_WhenReadingFromProduct() throws Exception {
        StringWriter result = new StringWriter();

        Product product = ProductIO.readProduct(getClass().getResource("CSV_export_test.dim").getPath());
        ProductWriter writer = new CsvProductWriterPlugIn(result, CsvProductWriter.WRITE_FEATURES |
                CsvProductWriter.WRITE_PROPERTIES).createWriterInstance();
        writer.writeProductNodes(product, "");

        // parameters doesn't matter. They are ignored in this writer.
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);
        String exportedText = result.toString().trim();
        LineNumberReader reader = new LineNumberReader(new StringReader(exportedText));
        Stream<String> lineStream = reader.lines();
        String[] lines = lineStream.toArray(String[]::new);
        assertEquals("featureId\tcurrent_pixel_state:short\tclear_land_count:short\tclear_water_count:short\tclear_snow_ice_count:short\tcloud_count:short\tcloud_shadow_count:short\tsr_1_mean:float\tsr_1_uncertainty:float\tsr_2_mean:float\tsr_2_uncertainty:float\tsr_3_mean:float\tsr_3_uncertainty:float\tsr_4_mean:float\tsr_4_uncertainty:float\tsr_5_mean:float\tsr_5_uncertainty:float\tsr_6_mean:float\tsr_6_uncertainty:float\tsr_7_mean:float\tsr_7_uncertainty:float\tsr_8_mean:float\tsr_8_uncertainty:float\tsr_9_mean:float\tsr_9_uncertainty:float\tsr_10_mean:float\tsr_10_uncertainty:float\tsr_12_mean:float\tsr_12_uncertainty:float\tsr_13_mean:float\tsr_13_uncertainty:float\tsr_14_mean:float\tsr_14_uncertainty:float\tvegetation_index_mean:float",
                     lines[1]);
        assertEquals("2\t1\t4\t0\t0\t0\t0\t0.05378438\t1.7889735E-4\t0.07834123\t1.3025949E-4\t0.11367677\t9.031142E-5\t0.1292348\t8.123871E-5\t0.17648439\t7.3810435E-5\t0.22502273\t8.8285175E-5\t0.2544481\t9.857157E-5\t0.26456913\t1.03908686E-4\t0.2800094\t9.959906E-5\t0.32049733\t1.02617225E-4\t0.33433527\t1.017785E-4\t0.37184396\t1.04131585E-4\t0.37633574\t1.02862345E-4\t0.18757367",
                     lines[4]);
        assertEquals("13\t1\t4\t0\t0\t0\t0\t0.054162458\t1.7895128E-4\t0.08019432\t1.2876527E-4\t0.11752841\t8.5652115E-5\t0.1341443\t7.564074E-5\t0.18326177\t6.115365E-5\t0.23406163\t5.6439323E-5\t0.2644052\t5.6184123E-5\t0.27516747\t5.7208836E-5\t0.28775653\t5.481409E-5\t0.32481524\t5.8434456E-5\t0.33827224\t5.706589E-5\t0.37585214\t5.9813072E-5\t0.38064936\t6.0536404E-5\t0.17349176",
                     lines[15]);
        assertEquals("22\t1\t4\t0\t0\t0\t0\t0.050334845\t1.8025596E-4\t0.077108\t1.2962687E-4\t0.115701646\t8.593441E-5\t0.13190617\t7.586992E-5\t0.17970552\t6.296577E-5\t0.22976173\t5.9484228E-5\t0.26037893\t5.65644E-5\t0.271074\t5.7522808E-5\t0.28324825\t5.6378332E-5\t0.31894296\t6.0959588E-5\t0.33182308\t5.8424677E-5\t0.36930043\t5.4918466E-5\t0.37388557\t5.501938E-5\t0.17260607",
                     lines[24]);

    }

}

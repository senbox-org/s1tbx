package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.io.File;


public class WriteVirtualBandWithWriteOpTest {


    @Test
    public void testWritingVirtualBand() throws Exception {

        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        ProductWriterPlugIn mockPlugin = Mockito.mock(ProductWriterPlugIn.class);
        try {
            registry.addWriterPlugIn(mockPlugin);

            Mockito.when(mockPlugin.getFormatNames()).thenReturn(new String[]{"Dummy-Mock-Format"});
            Mockito.when(mockPlugin.getOutputTypes()).thenReturn(new Class[]{File.class});

            ProductWriter mockedWriter = Mockito.mock(ProductWriter.class);
            Mockito.when(mockedWriter.shouldWrite(Mockito.any(Band.class))).thenReturn(true);
            Mockito.when(mockedWriter.getWriterPlugIn()).thenReturn(mockPlugin);
            Mockito.when(mockPlugin.createWriterInstance()).thenReturn(mockedWriter);
            Mockito.when(mockPlugin.getEncodeQualification(Mockito.any(Product.class))).thenReturn(EncodeQualification.FULL);

            Product productToWrite = new Product("TODO", "TBD", 10, 10);
            productToWrite.addBand(new VirtualBand("virtband", ProductData.TYPE_FLOAT32, 10, 10, "3 * 4"));
            WriteOp writeOp = new WriteOp(productToWrite, new File("notUsed.nc"), "Dummy-Mock-Format");
            writeOp.writeProduct(ProgressMonitor.NULL);
            writeOp.dispose();

            Mockito.verify(mockedWriter, Mockito.times(10)).writeBandRasterData(Mockito.argThat(new IsBand("virtband")),
                                                                                Mockito.eq(0),
                                                                                Mockito.anyInt(),
                                                                                Mockito.eq(10),
                                                                                Mockito.anyInt(),
                                                                                Mockito.argThat(new HasValidValue(12, 3)),
                                                                                Mockito.any(ProgressMonitor.class));

        } finally {
            registry.removeWriterPlugIn(mockPlugin);
        }
    }

    private class IsBand extends ArgumentMatcher<Band> {

        private final String bandName;

        IsBand(String bandName) {
            this.bandName = bandName;
        }

        @Override
        public boolean matches(Object argument) {
            return bandName.equals(((Band) argument).getName());
        }
    }

    private class HasValidValue extends ArgumentMatcher<ProductData> {


        private final float expectedValue;
        private final int atIndex;

        HasValidValue(float expectedValue, int atIndex) {
            this.expectedValue = expectedValue;
            this.atIndex = atIndex;
        }

        @Override
        public boolean matches(Object argument) {
            float actualValue = ((ProductData) argument).getElemFloatAt(atIndex);
            return Float.compare(expectedValue, actualValue) == 0;
        }
    }

}

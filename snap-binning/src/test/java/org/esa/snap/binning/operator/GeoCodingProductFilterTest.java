package org.esa.snap.binning.operator;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GeoCodingProductFilterTest {

    private GeoCoding geoCoding;
    private Product product;
    private GeoCodingProductFilter filter;

    @Before
    public void setUp() throws Exception {
        geoCoding = mock(GeoCoding.class);
        product = mock(Product.class);
        filter = new GeoCodingProductFilter();
    }

    @Test
    public void testAcceptProduct_WithProperGeoCoding() throws Exception {
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.canGetGeoPos()).thenReturn(true);

        assertThat(filter.accept(product), is(true));
        assertThat(filter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_WhenGeoCodingCanNotGetGeoPos() throws Exception {
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.canGetGeoPos()).thenReturn(false); // reject condition

        assertThat(filter.accept(product), is(false));
        assertThat(filter.getReason(), is("Rejected because it does not contain a proper geo coding."));
    }

    @Test
    public void testRejectProduct_WhenProductContainsNoGeoCoding() throws Exception {
        when(product.getSceneGeoCoding()).thenReturn(null); // reject condition

        assertThat(filter.accept(product), is(false));
        assertThat(filter.getReason(), is("Rejected because it does not contain a proper geo coding."));
    }
}

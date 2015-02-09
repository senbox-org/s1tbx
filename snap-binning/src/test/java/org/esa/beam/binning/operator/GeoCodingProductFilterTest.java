package org.esa.beam.binning.operator;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.junit.*;

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
        when(product.getGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.canGetGeoPos()).thenReturn(true);

        assertThat(filter.accept(product), is(true));
        assertThat(filter.getReason(), is(nullValue()));
    }

    @Test
    public void testRejectProduct_WhenGeoCodingCanNotGetGeoPos() throws Exception {
        when(product.getGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.canGetGeoPos()).thenReturn(false); // reject condition

        assertThat(filter.accept(product), is(false));
        assertThat(filter.getReason(), is("Rejected because it does not contain a proper geo coding."));
    }

    @Test
    public void testRejectProduct_WhenProductContainsNoGeoCoding() throws Exception {
        when(product.getGeoCoding()).thenReturn(null); // reject condition

        assertThat(filter.accept(product), is(false));
        assertThat(filter.getReason(), is("Rejected because it does not contain a proper geo coding."));
    }
}

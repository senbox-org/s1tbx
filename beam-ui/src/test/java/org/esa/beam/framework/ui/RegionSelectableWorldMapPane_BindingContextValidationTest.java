package org.esa.beam.framework.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RegionSelectableWorldMapPane_BindingContextValidationTest {

    private final static String NORTH_BOUND = RegionSelectableWorldMapPane.NORTH_BOUND;
    private final static String SOUTH_BOUND = RegionSelectableWorldMapPane.SOUTH_BOUND;
    private final static String WEST_BOUND = RegionSelectableWorldMapPane.WEST_BOUND;
    private final static String EAST_BOUND = RegionSelectableWorldMapPane.EAST_BOUND;

    private PropertiesObject propertiesObject;
    private BindingContext bindingContext;

    @Before
    public void setUp() throws Exception {
        propertiesObject = new PropertiesObject();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(propertiesObject));
    }

    @Test
    public void testThatBindingContextMustBeNotNull() {
        try {
            RegionSelectableWorldMapPane.ensureValidBindingContext(null);
            fail("should not come here");
        } catch (IllegalArgumentException expected) {
            assertEquals("bindingContext must be not null", expected.getMessage());
        } catch (Exception notExpected) {
            fail("Exception '" + notExpected.getClass().getName() + "' is not expected");
        }
    }

    @Test
    public void testThatPropertiesObjectAreFilledWithDefaultValuesIfAllPropertyValuesAreNull() {
        //preparation
        propertiesObject.northBound = null;
        propertiesObject.eastBound = null;
        propertiesObject.southBound = null;
        propertiesObject.westBound = null;

        //execution
        RegionSelectableWorldMapPane.ensureValidBindingContext(bindingContext);

        //verification
        assertEquals(Double.valueOf(75.0), propertiesObject.northBound);
        assertEquals(Double.valueOf(30.0), propertiesObject.eastBound);
        assertEquals(Double.valueOf(35.0), propertiesObject.southBound);
        assertEquals(Double.valueOf(-15.0), propertiesObject.westBound);
    }

    @Test
    public void testValidValuesAreNotChanged() {
        //preparation
        propertiesObject.northBound = 15.0;
        propertiesObject.eastBound = 15.0;
        propertiesObject.southBound = -15.0;
        propertiesObject.westBound = -15.0;
        final PropertyContainer objectBacked = PropertyContainer.createObjectBacked(propertiesObject);

        //execution
        RegionSelectableWorldMapPane.ensureValidBindingContext(new BindingContext(objectBacked));

        //verification
        assertEquals(Double.valueOf(15.0), propertiesObject.northBound);
        assertEquals(Double.valueOf(15.0), propertiesObject.eastBound);
        assertEquals(Double.valueOf(-15.0), propertiesObject.southBound);
        assertEquals(Double.valueOf(-15.0), propertiesObject.westBound);
    }

    @Test
    public void testThatBindingContextWithoutNorthBoundPropertyThrowsIllegalArgumentException() {
        assertIllegalArgumentExceptionIfPropertyIsMissing(NORTH_BOUND);
    }

    @Test
    public void testThatBindingContextWithoutSouthBoundPropertyThrowsIllegalArgumentException() {
        assertIllegalArgumentExceptionIfPropertyIsMissing(SOUTH_BOUND);
    }

    @Test
    public void testThatBindingContextWithoutEastBoundPropertyThrowsIllegalArgumentException() {
        assertIllegalArgumentExceptionIfPropertyIsMissing(EAST_BOUND);
    }

    @Test
    public void testThatBindingContextWithoutWestBoundPropertyThrowsIllegalArgumentException() {
        assertIllegalArgumentExceptionIfPropertyIsMissing(WEST_BOUND);
    }

    @Test
    /**
     * It suffices to test one invalid property value, since the implementation responsible for testing whether
     * the property values are valid is well tested in {@link RegionSelectableWorldMapPane_BoundingValuesValidation}
     */
    public void testThatIllegalArgumentExceptionIsThrownIfAnyPropertyValueIsInvalid() {
        //preparation
        bindingContext.getPropertySet().setValue(NORTH_BOUND, null);
        try {
            //execution
            RegionSelectableWorldMapPane.ensureValidBindingContext(bindingContext);
        } catch (IllegalArgumentException expected) {
            //verification
            assertEquals("Given geo-bounds (" + null + ", " + EAST_BOUND + ", " + SOUTH_BOUND + ", " + WEST_BOUND +
                                 " are invalid.", expected.getMessage());
        } catch (Exception notExpected) {
            fail("Exception '" + notExpected.getClass().getName() + "' not expected");
        }
    }

    private void assertIllegalArgumentExceptionIfPropertyIsMissing(String propertyName) {
        //preparation
        bindingContext.getPropertySet().removeProperty(bindingContext.getPropertySet().getProperty(propertyName));

        //execution
        try {
            RegionSelectableWorldMapPane.ensureValidBindingContext(bindingContext);
            fail("should not come here");
        } catch (IllegalArgumentException expected) {
            //verification
            assertEquals("bindingContext must contain a property named " + propertyName, expected.getMessage());
        } catch (Exception notExpected) {
            fail("Exception '" + notExpected.getClass().getName() + "' not expected");
        }
    }

    private static class PropertiesObject {
        private Double northBound;
        private Double eastBound;
        private Double southBound;
        private Double westBound;
    }
}

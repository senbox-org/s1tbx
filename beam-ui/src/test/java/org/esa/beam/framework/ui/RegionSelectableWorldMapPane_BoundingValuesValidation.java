package org.esa.beam.framework.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegionSelectableWorldMapPane_BoundingValuesValidation {

    private final double validNorthBound = 75.0;
    private final double validEastBound = 30.0;
    private final double validSouthBound = 20.0;
    private final double validWestBound = 10.0;

    @Test
    public void testValidBounds() {
        assertTrue(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, validWestBound));
    }

    @Test
    public void testThatReturnValueIsFalseIfAllBoundingValuesAreNull() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(null, null, null, null));
    }

    @Test
    public void testThatEachValueMustBeNotNull() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(null, validEastBound, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, null, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, null, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, null));
    }

    @Test
    public void testThatNorthValueMustBeBiggerThanSouthValue() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(10.0, validEastBound, 10.0, validWestBound));
    }

    @Test
    public void testThatEastValueMustBeBiggerThanWestValue() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, 10.0, validSouthBound, 10.0));
    }

    @Test
    public void testThatValuesAreInsideValidBounds() {
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(91.0, validEastBound, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, 181.0, validSouthBound, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, -91.0, validWestBound));
        assertFalse(RegionSelectableWorldMapPane.geoBoundsAreValid(validNorthBound, validEastBound, validSouthBound, -181.0));
    }
}

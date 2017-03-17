package org.esa.snap.binning.operator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VariableConfigTest {

    private VariableConfig config;

    @Before
    public void setUp() {
        config = new VariableConfig();
    }

    @Test
    public void testParameterConstruction() {
        final String name = "name";
        final String expression = "expression";

        final VariableConfig config = new VariableConfig(name, expression);

        assertEquals(name, config.getName());
        assertEquals(expression, config.getExpr());
    }

    @Test
    public void testSetGetName() {
        final String name = "a_name";

        config.setName(name);
        assertEquals(name, config.getName());
    }

    @Test
    public void testSetGetExpr() {
        final String expression = "an _expr";

        config.setExpr(expression);
        assertEquals(expression, config.getExpr());
    }

    @Test
    public void testEquals_identity() {
        assertTrue(config.equals(config));
    }

    @Test
    public void testEquals_wrongClass() {
        assertFalse(config.equals(new Double(4)));
    }

    @Test
    public void testEquals_nullInput() {
        assertFalse(config.equals(null));
    }

    @Test
    public void testEquals_differentName() {
        final String expression = "a>b";

        config.setName("Claire");
        config.setExpr(expression);

        final VariableConfig other = new VariableConfig();
        other.setName("Paul");
        other.setExpr(expression);

        assertFalse(config.equals(other));
    }

    @Test
    public void testEquals_differentExpression() {
        final String name = "Claire";
        config.setName(name);
        config.setExpr("a>b");

        final VariableConfig other = new VariableConfig();
        other.setName(name);
        other.setExpr("a=b+c");

        assertFalse(config.equals(other));
    }

    @Test
    public void testEquals_equal() {
        final String name = "Claire";
        final String expression = "a>b";

        config.setName(name);
        config.setExpr(expression);

        final VariableConfig other = new VariableConfig();
        other.setName(name);
        other.setExpr(expression);

        assertTrue(config.equals(other));
    }
}

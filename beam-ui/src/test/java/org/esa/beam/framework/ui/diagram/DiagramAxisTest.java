/*
 * $Id: DiagramAxisTest.java,v 1.1 2006/10/10 14:47:39 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import junit.framework.TestCase;

public class DiagramAxisTest extends TestCase {

    DiagramAxis _diagramAxis;
    DiagramAxisPropertyChangeListener _axisPropertyChangeListener;

    public DiagramAxisTest(String s) {
        super(s);
    }

    public void setUp() {
        _axisPropertyChangeListener = new DiagramAxisPropertyChangeListener();
        _diagramAxis = new DiagramAxis();
        _diagramAxis.addPropertyChangeListener(_axisPropertyChangeListener);
    }

    public void tearDown() {
        if (_diagramAxis != null) {
            _diagramAxis.removePropertyChangeListener(_axisPropertyChangeListener);
            _diagramAxis = null;
        }
        _axisPropertyChangeListener = null;
    }

    public void testDiagramAxisPCL() {
        _axisPropertyChangeListener.propertyChange(new PropertyChangeEvent(this, "PCL", "testold", "testnew"));
        assertNotNull(_axisPropertyChangeListener.getEvents());
        _axisPropertyChangeListener.reset();
        assertNull(_axisPropertyChangeListener.getEvents());
    }

    public void testProperties() {

        _axisPropertyChangeListener.reset();
        _diagramAxis.setName("bibo");
        assertEquals("bibo", _diagramAxis.getName());
        PropertyChangeEvent[] events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("name", events[0].getPropertyName());
        assertEquals(null, events[0].getOldValue());
        assertEquals("bibo", events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setName(null);
        assertEquals(null, _diagramAxis.getName());
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("name", events[0].getPropertyName());
        assertEquals("bibo", events[0].getOldValue());
        assertEquals(null, events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setUnit("bibo");
        assertEquals("bibo", _diagramAxis.getUnit());
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("unit", events[0].getPropertyName());
        assertEquals(null, events[0].getOldValue());
        assertEquals("bibo", events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setUnit(null);
        assertEquals(null, _diagramAxis.getUnit());
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("unit", events[0].getPropertyName());
        assertEquals("bibo", events[0].getOldValue());
        assertEquals(null, events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        assertEquals(1.0, _diagramAxis.getUnitFactor(), 1e-10);
        _diagramAxis.setUnitFactor(0.5);
        assertEquals(0.5, _diagramAxis.getUnitFactor(), 1e-10);
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("unitFactor", events[0].getPropertyName());
        assertEquals(new Double(1.0), events[0].getOldValue());
        assertEquals(new Double(0.5), events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setNumMajorTicks(5);
        assertEquals(5, _diagramAxis.getNumMajorTicks());
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("numMajorTicks", events[0].getPropertyName());
        assertEquals(new Integer(3), events[0].getOldValue());
        assertEquals(new Integer(5), events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setNumMinorTicks(3);
        assertEquals(3, _diagramAxis.getNumMinorTicks());
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(1, events.length);
        assertEquals("numMinorTicks", events[0].getPropertyName());
        assertEquals(new Integer(5), events[0].getOldValue());
        assertEquals(new Integer(3), events[0].getNewValue());

        _axisPropertyChangeListener.reset();
        _diagramAxis.setValueRange(13.1, 16.2);
        assertEquals(16.2, _diagramAxis.getMaxValue(), 1e-9);
        assertEquals(13.1, _diagramAxis.getMinValue(), 1e-9);
        events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(2, events.length);
        assertEquals("minValue", events[0].getPropertyName());
        assertEquals(new Double(0), events[0].getOldValue());
        assertEquals(new Double(13.1), events[0].getNewValue());
        assertEquals("maxValue", events[1].getPropertyName());
        assertEquals(new Double(100), events[1].getOldValue());
        assertEquals(new Double(16.2), events[1].getNewValue());

        _axisPropertyChangeListener.reset();
        try {
            _diagramAxis.setValueRange(14.1, 11.2);
            fail();
        } catch (Exception e) {
            assertEquals("java.lang.IllegalArgumentException", e.getClass().getName());
        }
        assertNull(_axisPropertyChangeListener.getEvents());
    }

    public void testSetSubDivision() {
        _axisPropertyChangeListener.reset();
        _diagramAxis.setSubDivision(12.4, 83.6, 7, 4);
        assertEquals(12.4, _diagramAxis.getMinValue(), 1e-9);
        assertEquals(83.6, _diagramAxis.getMaxValue(), 1e-9);
        assertEquals(7, _diagramAxis.getNumMajorTicks());
        assertEquals(4, _diagramAxis.getNumMinorTicks());
        PropertyChangeEvent[] events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(4, events.length);
        assertEquals("minValue", events[0].getPropertyName());
        assertEquals(new Double(0), events[0].getOldValue());
        assertEquals(new Double(12.4), events[0].getNewValue());
        assertEquals("maxValue", events[1].getPropertyName());
        assertEquals(new Double(100), events[1].getOldValue());
        assertEquals(new Double(83.6), events[1].getNewValue());
        assertEquals("numMajorTicks", events[2].getPropertyName());
        assertEquals(new Integer(3), events[2].getOldValue());
        assertEquals(new Integer(7), events[2].getNewValue());
        assertEquals("numMinorTicks", events[3].getPropertyName());
        assertEquals(new Integer(5), events[3].getOldValue());
        assertEquals(new Integer(4), events[3].getNewValue());
    }

    public void testSetOptimalSubDivision() {
        _diagramAxis.setValueRange(13.1, 16.2);

        _axisPropertyChangeListener.reset();
        _diagramAxis.setOptimalSubDivision(4, 6, 8);
        assertEquals(16.5, _diagramAxis.getMaxValue(), 1e-9);
        assertEquals(12.75, _diagramAxis.getMinValue(), 1e-9);
        assertEquals(6, _diagramAxis.getNumMajorTicks());
        assertEquals(8, _diagramAxis.getNumMinorTicks());
        PropertyChangeEvent[] events = _axisPropertyChangeListener.getEvents();
        assertNotNull(events);
        assertEquals(4, events.length);
        assertEquals("minValue", events[0].getPropertyName());
        assertEquals(new Double(13.1), events[0].getOldValue());
        assertEquals(new Double(12.75), events[0].getNewValue());
        assertEquals("maxValue", events[1].getPropertyName());
        assertEquals(new Double(16.2), events[1].getOldValue());
        assertEquals(new Double(16.5), events[1].getNewValue());
        assertEquals("numMajorTicks", events[2].getPropertyName());
        assertEquals(new Integer(3), events[2].getOldValue());
        assertEquals(new Integer(6), events[2].getNewValue());
        assertEquals("numMinorTicks", events[3].getPropertyName());
        assertEquals(new Integer(5), events[3].getOldValue());
        assertEquals(new Integer(8), events[3].getNewValue());
    }

    public void testGetOptimalTickDistance() {
        assertEquals(10d, DiagramAxis.getOptimalTickDistance(12, 31, 3), 1e-6);
        assertEquals(0.1d, DiagramAxis.getOptimalTickDistance(0.12, 0.31, 3), 1e-9);
        assertEquals(2.5d, DiagramAxis.getOptimalTickDistance(52, 57, 3), 1e-6);
        assertEquals(2.5d, DiagramAxis.getOptimalTickDistance(15, 20, 3), 1e-6);
        assertEquals(4.0d, DiagramAxis.getOptimalTickDistance(14.8, 20.3, 3), 1e-6);
        assertEquals(5.0d, DiagramAxis.getOptimalTickDistance(14.8, 24.8, 3), 1e-6);
        assertEquals(2.0d, DiagramAxis.getOptimalTickDistance(14.8, 18.8, 3), 1e-6);
        assertEquals(2.0d, DiagramAxis.getOptimalTickDistance(10.2, 13.8, 3), 1e-6);
        assertEquals(2.0d, DiagramAxis.getOptimalTickDistance(10.2, 13.8, 3), 1e-6);
        assertEquals(7500.0d, DiagramAxis.getOptimalTickDistance(-10200, 5.0, 3), 1e-6);
    }

    /**
     * A listener used to check the bean properties of DiagramAxis.
     */
    static class DiagramAxisPropertyChangeListener implements PropertyChangeListener {

        Vector _events;

        public void propertyChange(PropertyChangeEvent evt) {
            if (_events == null) {
                _events = new Vector();
            }
            _events.add(evt);
        }

        public PropertyChangeEvent[] getEvents() {
            if (_events == null || _events.size() == 0) {
                return null;
            }
            return (PropertyChangeEvent[]) _events.toArray(new PropertyChangeEvent[_events.size()]);
        }

        public void reset() {
            if (_events != null) {
                _events.clear();
            }
        }
    }
}

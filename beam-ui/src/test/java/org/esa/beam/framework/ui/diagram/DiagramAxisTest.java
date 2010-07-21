/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.ui.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import junit.framework.TestCase;

public class DiagramAxisTest extends TestCase {

    DiagramAxis diagramAxis;
    private EventCounter eventCounter;

    public DiagramAxisTest(String s) {
        super(s);
    }

    @Override
    public void setUp() {
        Diagram diagram = new Diagram();
        diagramAxis = new DiagramAxis();
        diagram.setXAxis(diagramAxis);
        eventCounter = new EventCounter();
        diagram.addChangeListener(eventCounter);
    }

    public void testProperties() {

        eventCounter.reset();
        diagramAxis.setName("bibo");
        assertEquals("bibo", diagramAxis.getName());
        assertEquals(1, eventCounter.counts);

        diagramAxis.setName(null);
        assertEquals(null, diagramAxis.getName());
        assertEquals(2, eventCounter.counts);

        diagramAxis.setUnit("bibo");
        assertEquals("bibo", diagramAxis.getUnit());
        assertEquals(3, eventCounter.counts);

        diagramAxis.setUnit(null);
        assertEquals(null, diagramAxis.getUnit());
        assertEquals(4, eventCounter.counts);

        assertEquals(1.0, diagramAxis.getUnitFactor(), 1e-10);
        diagramAxis.setUnitFactor(0.5);
        assertEquals(0.5, diagramAxis.getUnitFactor(), 1e-10);
        assertEquals(5, eventCounter.counts);

        diagramAxis.setNumMajorTicks(5);
        assertEquals(5, diagramAxis.getNumMajorTicks());
        assertEquals(6, eventCounter.counts);

        diagramAxis.setNumMinorTicks(3);
        assertEquals(3, diagramAxis.getNumMinorTicks());
        assertEquals(7, eventCounter.counts);

        diagramAxis.setValueRange(13.1, 16.2);
        assertEquals(16.2, diagramAxis.getMaxValue(), 1e-9);
        assertEquals(13.1, diagramAxis.getMinValue(), 1e-9);
        assertEquals(8, eventCounter.counts);

        try {
            diagramAxis.setValueRange(14.1, 11.2);
            fail();
        } catch (Exception e) {
            assertEquals("java.lang.IllegalArgumentException", e.getClass().getName());
        }
        assertEquals(8, eventCounter.counts);
    }

    public void testSetSubDivision() {
        diagramAxis.setSubDivision(12.4, 83.6, 7, 4);
        assertEquals(12.4, diagramAxis.getMinValue(), 1e-9);
        assertEquals(83.6, diagramAxis.getMaxValue(), 1e-9);
        assertEquals(7, diagramAxis.getNumMajorTicks());
        assertEquals(4, diagramAxis.getNumMinorTicks());
        assertEquals(3, eventCounter.counts);
    }

    public void testSetOptimalSubDivision() {
        diagramAxis.setValueRange(13.1, 16.2);
        assertEquals(1, eventCounter.counts);
        diagramAxis.setOptimalSubDivision(4, 6, 8);
        assertEquals(16.5, diagramAxis.getMaxValue(), 1e-9);
        assertEquals(12.75, diagramAxis.getMinValue(), 1e-9);
        assertEquals(6, diagramAxis.getNumMajorTicks());
        assertEquals(8, diagramAxis.getNumMinorTicks());
        assertEquals(4, eventCounter.counts);
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

    private static class EventCounter implements DiagramChangeListener {
        int counts = 0;
        public void reset() {
                       counts = 0;
        }
        public void diagramChanged(Diagram diagram) {
            counts++;
        }
    }
}

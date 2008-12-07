package org.esa.beam.smos.visat;

import junit.framework.TestCase;

public class GridPointSelectionServiceTest extends TestCase {

    public void testIt() {
        GridPointSelectionService gpss = new GridPointSelectionService();

        assertEquals(-1, gpss.getSelectedGridPointId());

        gpss.setSelectedGridPointId(1234567);
        assertEquals(1234567, gpss.getSelectedGridPointId());

        MySelectionListener l = new MySelectionListener();
        gpss.addGridPointSelectionListener(l);

        // select same id, no change expected
        gpss.setSelectedGridPointId(1234567);
        assertEquals(1234567, gpss.getSelectedGridPointId());
        assertEquals(-1, l.oldId);
        assertEquals(-1, l.newId);

        // select new id, expected change from old --> new id
        gpss.setSelectedGridPointId(7654321);
        assertEquals(7654321, gpss.getSelectedGridPointId());
        assertEquals(1234567, l.oldId);
        assertEquals(7654321, l.newId);

        // remove selection, notification expected id --> -1
        l.reset();
        gpss.setSelectedGridPointId(-1);
        assertEquals(-1, gpss.getSelectedGridPointId());
        assertEquals(7654321, l.oldId);
        assertEquals(-1, l.newId);
    }

    private static class MySelectionListener implements GridPointSelectionService.SelectionListener {
        int oldId = -1;
        int newId = -1;

        public void handleGridPointSelectionChanged(int oldId, int newId) {
            this.oldId = oldId;
            this.newId = newId;
        }

        void reset() {
            oldId = -1;
            newId = -1;
        }
    }
}
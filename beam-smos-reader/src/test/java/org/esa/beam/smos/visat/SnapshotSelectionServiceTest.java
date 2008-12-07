package org.esa.beam.smos.visat;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

public class SnapshotSelectionServiceTest extends TestCase {

    public void testIt() {
        ProductManager pm = new ProductManager();
        SnapshotSelectionService sss = new SnapshotSelectionService(pm);

        Product p0 = new Product("p0", "t0", 10, 10);
        pm.addProduct(p0);

        assertEquals(-1, sss.getSelectedSnapshotId(p0));

        Product p1 = new Product("p1", "t1", 10, 10);
        pm.addProduct(p1);
        sss.setSelectedSnapshotId(p1, 10001);

        assertEquals(-1, sss.getSelectedSnapshotId(p0));
        assertEquals(10001, sss.getSelectedSnapshotId(p1));

        MyListener l = new MyListener();
        sss.addSnapshotIdChangeListener(l);

        // select same id for p1, no change expected
        sss.setSelectedSnapshotId(p1, 10001);
        assertEquals(10001, sss.getSelectedSnapshotId(p1));
        assertEquals(null, l.product);
        assertEquals(-1, l.oldSnapshotId);
        assertEquals(-1, l.newSnapshotId);

        // select new id for p1, expected change from old --> new id
        sss.setSelectedSnapshotId(p1, 10023);
        assertEquals(10023, sss.getSelectedSnapshotId(p1));
        assertEquals(p1, l.product);
        assertEquals(10001, l.oldSnapshotId);
        assertEquals(10023, l.newSnapshotId);

        // select id for p0, expected change from -1 --> id
        sss.setSelectedSnapshotId(p0, 10023);
        assertEquals(10023, sss.getSelectedSnapshotId(p1));
        assertEquals(p0, l.product);
        assertEquals(-1, l.oldSnapshotId);
        assertEquals(10023, l.newSnapshotId);

        // removing p0, deselection expected
        l.reset();
        pm.removeProduct(p0);
        assertEquals(p0, l.product);
        assertEquals(10023, l.oldSnapshotId);
        assertEquals(-1, l.newSnapshotId);

        // remove selection for p1, notification expected 20023 --> -1
                l.reset();
        sss.setSelectedSnapshotId(p1, -1);
        assertEquals(-1, sss.getSelectedSnapshotId(p1));
        assertEquals(p1, l.product);
        assertEquals(10023, l.oldSnapshotId);
        assertEquals(-1, l.newSnapshotId);
    }

    private static class MyListener implements SnapshotSelectionService.Listener {
        Product product = null;
        int oldSnapshotId = -1;
        int newSnapshotId = -1;

        public void handleSnapshotIdChanged(Product product, int oldSnapshotId, int newSnapshotId) {
            this.product = product;
            this.oldSnapshotId = oldSnapshotId;
            this.newSnapshotId = newSnapshotId;
        }

        void reset() {
            this.product = null;
            this.oldSnapshotId = -1;
            this.newSnapshotId = -1;
        }
    }
}

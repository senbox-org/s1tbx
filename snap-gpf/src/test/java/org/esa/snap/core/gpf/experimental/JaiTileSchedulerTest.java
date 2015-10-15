package org.esa.snap.core.gpf.experimental;

import org.junit.Ignore;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.Raster;


@Ignore
public class JaiTileSchedulerTest {

    public static void main(String[] args) {
        scheduleTiles(0);
        scheduleTiles(1);
        scheduleTiles(2);
        scheduleTiles(4);
        scheduleTiles(8);
    }

    private static void scheduleTiles(int sleepTime) {
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(512);
        imageLayout.setTileHeight(512);

        RenderedOp op = ConstantDescriptor.create(1024f, 1024f,
                                                  new Short[]{1},
                                                  new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        MyTileComputationListener listener = new MyTileComputationListener();
        for (int i = 0; i < 1000; i++) {
            tileScheduler.scheduleTiles(op,
                                        new Point[]{
                                                new Point(0, 0)
                                        },
                                        new TileComputationListener[]{
                                                listener
                                        });
            if (sleepTime > 0) {
                sleep(sleepTime);
            }
        }

        sleep(100);
        System.out.println("scheduleTiles(sleepTime = " + sleepTime + "):");
        System.out.println("  Tiles computed: " + listener.computed);
        System.out.println("  Tiles cancelled: " + listener.cancelled);
        System.out.println("  Tile computation failures: " + listener.computationFailures);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // ok
        }
    }

    private static class MyTileComputationListener implements TileComputationListener {

        int computed;
        int cancelled;
        int computationFailures;

        public void tileComputed(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int i, int i1,
                                 Raster raster) {
            computed++;
        }

        public void tileCancelled(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int i, int i1) {
            cancelled++;
        }

        public void tileComputationFailure(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int i, int i1,
                                           Throwable throwable) {
            computationFailures++;
        }
    }
}

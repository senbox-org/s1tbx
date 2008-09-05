package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import javax.media.jai.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.*;
import java.util.List;

public class ConcurrentMultiLevelRenderer implements MultiLevelRenderer {

    private int lastLevel;
    private boolean debug;
    private final Map<TileIndex, TileRequest> scheduledTileRequests;
    private final TileImageCache localTileCache;

    public ConcurrentMultiLevelRenderer() {
        lastLevel = -1;
        scheduledTileRequests = Collections.synchronizedMap(new HashMap<TileIndex, TileRequest>(37));
        localTileCache = new TileImageCache();

        if (debug) {
            final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
            final TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
            System.out.println("jai.tileScheduler.priority = " + tileScheduler.getPriority());
            System.out.println("jai.tileScheduler.parallelism = " + tileScheduler.getParallelism());
            System.out.println("jai.tileScheduler.prefetchPriority = " + tileScheduler.getPrefetchPriority());
            System.out.println("jai.tileScheduler.prefetchParallelism = " + tileScheduler.getPrefetchParallelism());
            System.out.println("jai.tileCache.memoryCapacity = " + tileCache.getMemoryCapacity());
            System.out.println("jai.tileCache.memoryThreshold = " + tileCache.getMemoryThreshold());
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public synchronized void reset() {
        cancelTileRequests(-1);
        localTileCache.clear();
    }

    @Override
    public void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int currentLevel) {
        final long t0 = System.nanoTime();
        renderImpl((InteractiveRendering) rendering, multiLevelSource, currentLevel);
        if (debug) {
            final long t1 = System.nanoTime();
            double time = (t1 - t0) / (1000.0 * 1000.0);
            System.out.printf("ConcurrentMultiLevelRenderer: render: time=%f ms, clip=%s\n", time, rendering.getGraphics().getClip());
        }
    }

    private void renderImpl(InteractiveRendering rendering, MultiLevelSource multiLevelSource, int currentLevel) {

        // On level change, cancel all pending tile requests
        if (this.lastLevel != currentLevel) {
            cancelTileRequests(currentLevel);
            this.lastLevel = currentLevel;
        }

        final PlanarImage planarImage = (PlanarImage) multiLevelSource.getImage(currentLevel);
        final Graphics2D graphics = rendering.getGraphics();
        final Viewport viewport = rendering.getViewport();

        // Check clipping rectangle, required for this renderer
        final Rectangle clipBounds = graphics.getClipBounds();
        if (clipBounds == null) {
            throw new IllegalStateException("clipBounds == null");
        }

        // Check that color model is available, required for this renderer
        final ColorModel colorModel = planarImage.getColorModel();
        if (colorModel == null) {
            throw new IllegalStateException("colorModel == null");
        }

        // Create set of required tile indexes
        final Rectangle clippedImageRegion = getImageRegion(viewport, multiLevelSource, currentLevel, clipBounds);
        final Set<TileIndex> requiredTileIndexes = getTileIndexes(planarImage, currentLevel, clippedImageRegion);
        if (requiredTileIndexes.isEmpty()) {
            return; // nothing to render
        }

        // Create lists of available and missing tile indexes
        final ArrayList<TileIndex> availableTileIndexList = new ArrayList<TileIndex>(requiredTileIndexes.size());
        final ArrayList<TileIndex> missingTileIndexList = new ArrayList<TileIndex>(requiredTileIndexes.size());
        final ArrayList<TileIndex> notScheduledTileIndexList = new ArrayList<TileIndex>(requiredTileIndexes.size());
        for (TileIndex requiredTileIndex : requiredTileIndexes) {
            if (localTileCache.contains(requiredTileIndex)) {
                availableTileIndexList.add(requiredTileIndex);
            } else {
                missingTileIndexList.add(requiredTileIndex);
                if (!scheduledTileRequests.containsKey(requiredTileIndex)) {
                    notScheduledTileIndexList.add(requiredTileIndex);
                }
            }
        }

        // Schedule missing tiles, if any
        if (!notScheduledTileIndexList.isEmpty()) {
            final TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
            final TileComputationHandler tileComputationHandler = new TileComputationHandler(rendering,
                                                                                             multiLevelSource,
                                                                                             currentLevel);
            final TileRequest tileRequest = tileScheduler.scheduleTiles(planarImage,
                                                                        getPoints(notScheduledTileIndexList),
                                                                        new TileComputationListener[]{
                                                                                tileComputationHandler
                                                                        });
            for (TileIndex tileIndex : notScheduledTileIndexList) {
                scheduledTileRequests.put(tileIndex, tileRequest);
            }
        }

        // Draw missing tiles from other levels (if any)
        drawTentativeTileImages(graphics, viewport,
                                multiLevelSource, currentLevel, planarImage, missingTileIndexList);

        // Draw available tiles
        for (final TileIndex tileIndex : availableTileIndexList) {
            final TileImage tileImage = localTileCache.get(tileIndex);
            drawTileImage(graphics, viewport, tileImage);
        }

        // Draw tile frames
        final AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(currentLevel);
        drawTileFrames(graphics, viewport, planarImage, missingTileIndexList, i2m, Color.RED);
        if (debug) {
            drawTileFrames(graphics, viewport, planarImage, availableTileIndexList, i2m, Color.BLUE);
        }

        // Cancel any pending tile requests that are not in the visible region
        final Rectangle visibleImageRegion = getImageRegion(viewport, multiLevelSource, currentLevel, rendering.getBounds());
        final Set<TileIndex> visibleTileIndexSet = getTileIndexes(planarImage, currentLevel, visibleImageRegion);
        if (!visibleTileIndexSet.isEmpty()) {
            cancelTileRequests(visibleTileIndexSet);
        }

        // Remove any tile images that are older than the retention period.
        localTileCache.trim(currentLevel);
    }

    private void drawTentativeTileImages(Graphics2D g,
                                         Viewport vp,
                                         MultiLevelSource multiLevelSource,
                                         int level,
                                         PlanarImage planarImage,
                                         List<TileIndex> missingTileIndexList) {
        final AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(level);
        for (final TileIndex tileIndex : missingTileIndexList) {

            final Rectangle tileRect = planarImage.getTileRect(tileIndex.tileX, tileIndex.tileY);
            final Rectangle2D bounds = i2m.createTransformedShape(tileRect).getBounds2D();

            final TreeSet<TileImage> tentativeTileImageSet = new TreeSet<TileImage>(new DescendingLevelsComparator());
            final Collection<TileImage> tileImages = localTileCache.getAll();

            // Search for a tile image at the nearest higher resolution which is contained by bounds
            TileImage containedTileImage = null;
            int containedLevel = Integer.MAX_VALUE;
            for (TileImage tileImage : tileImages) {
                final int someLevel = tileImage.tileIndex.level;
                if (someLevel > level
                        && someLevel < containedLevel
                        && tileImage.bounds.contains(bounds)) {
                    containedTileImage = tileImage;
                    containedLevel = someLevel;
                }
            }
            if (containedTileImage != null) {
                tentativeTileImageSet.add(containedTileImage);
                // Search for intersecting tile images at a higher resolution
                for (TileImage tileImage : tileImages) {
                    if (tileImage.tileIndex.level < level
                            && tileImage.bounds.intersects(bounds)) {
                        tentativeTileImageSet.add(tileImage);
                    }
                }
            } else {
                // Search for intersecting tile images at any resolution
                for (TileImage tileImage : tileImages) {
                    if (tileImage.tileIndex.level != level
                            && tileImage.bounds.intersects(bounds)) {
                        tentativeTileImageSet.add(tileImage);
                    }
                }
            }

            final Shape oldClip = g.getClip();
            g.setClip(vp.getModelToViewTransform().createTransformedShape(bounds));
            for (TileImage tileImage : tentativeTileImageSet) {
                drawTileImage(g, vp, tileImage);
            }
            g.setClip(oldClip);
        }
    }

    private static Point[] getPoints(List<TileIndex> tileIndexList) {
        final Point[] points = new Point[tileIndexList.size()];
        for (int i = 0; i < tileIndexList.size(); i++) {
            TileIndex tileIndex = tileIndexList.get(i);
            points[i] = new Point(tileIndex.tileX, tileIndex.tileY);
        }
        return points;
    }

    private static Set<TileIndex> getTileIndexes(PlanarImage planarImage, int level, Rectangle clippedImageRegion) {
        final Point[] indices = planarImage.getTileIndices(clippedImageRegion);
        if (indices == null || indices.length == 0) {
            return Collections.emptySet();
        }
        final Set<TileIndex> indexes = new HashSet<TileIndex>((3 * indices.length) / 2);
        for (Point point : indices) {
            indexes.add(new TileIndex(point.x, point.y, level));
        }
        return indexes;
    }

    private static void drawTileImage(Graphics2D g, Viewport vp, TileImage ti) {
        final AffineTransform t = AffineTransform.getTranslateInstance(ti.x, ti.y);
        t.preConcatenate(ti.i2m);
        t.preConcatenate(vp.getModelToViewTransform());
        g.drawRenderedImage(ti.image, t);
        ti.timeStamp = System.currentTimeMillis();
    }

    private static void drawTileFrames(Graphics2D g, Viewport vp, PlanarImage planarImage, List<TileIndex> tileIndices,
                                       AffineTransform i2m, Color frameColor) {
        final AffineTransform m2v = vp.getModelToViewTransform();
        final AffineTransform oldTransform = g.getTransform();
        final Color oldColor = g.getColor();
        final Stroke oldStroke = g.getStroke();
        final AffineTransform t = new AffineTransform();
        t.preConcatenate(i2m);
        t.preConcatenate(m2v);
        g.setTransform(t);
        g.setColor(frameColor);
        g.setStroke(new BasicStroke(0.0f));
        for (TileIndex tileIndice : tileIndices) {
            g.draw(planarImage.getTileRect(tileIndice.tileX, tileIndice.tileY));
        }
        g.setStroke(oldStroke);
        g.setColor(oldColor);
        g.setTransform(oldTransform);
    }

    // Called from EDT.
    // Cancels any tiles that are in the scheduled list and not in the visibleTileIndexSet list.
    private void cancelTileRequests(Set<TileIndex> visibleTileIndexSet) {
        final Map<TileIndex, TileRequest> scheduledTileRequestsCopy = new HashMap<TileIndex, TileRequest>(scheduledTileRequests);
        // scan through the scheduled tiles list cancelling any that are no longer in view
        for (TileIndex tileIndex : scheduledTileRequestsCopy.keySet()) {
            if (!visibleTileIndexSet.contains(tileIndex)) {
                TileRequest request = scheduledTileRequestsCopy.get(tileIndex);
                // if tile not already removed (concurrently)
                if (request != null) {
                    request.cancelTiles(new Point[]{new Point(tileIndex.tileX, tileIndex.tileY)});
                    scheduledTileRequests.remove(tileIndex);
                }
            }
        }
    }

    private void cancelTileRequests(int currentLevel) {
        final Map<TileIndex, TileRequest> scheduledTileRequestsCopy = new HashMap<TileIndex, TileRequest>(scheduledTileRequests);
        for (TileIndex tileIndex : scheduledTileRequestsCopy.keySet()) {
            if (tileIndex.level != currentLevel) {
                final TileRequest tileRequest = scheduledTileRequestsCopy.get(tileIndex);
                tileRequest.cancelTiles(null);
                scheduledTileRequests.remove(tileIndex);
            }
        }
    }

    private static TileImage createTileImage(GraphicsConfiguration deviceConfiguration,
                                             PlanarImage planarImage,
                                             TileIndex tileIndex,
                                             Raster tile,
                                             AffineTransform i2m) {
        final RenderedImage image = createDeviceCompatibleImageForTile(deviceConfiguration,
                                                                       planarImage,
                                                                       tileIndex,
                                                                       tile);
        return new TileImage(image,
                             tileIndex,
                             planarImage.tileXToX(tileIndex.tileX),
                             planarImage.tileYToY(tileIndex.tileY),
                             i2m);
    }

    private static RenderedImage createDeviceCompatibleImageForTile(GraphicsConfiguration deviceConfiguration,
                                                                    PlanarImage planarImage,
                                                                    TileIndex tileIndex,
                                                                    Raster tile) {
        final SampleModel sm = planarImage.getSampleModel();
        final ColorModel cm = planarImage.getColorModel();
        final Rectangle r = planarImage.getTileRect(tileIndex.tileX, tileIndex.tileY);
        final DataBuffer db = tile.getDataBuffer();
        final WritableRaster wr = Raster.createWritableRaster(sm, db, null);
        final BufferedImage bi = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        //System.out.println("bi = " + bi);
        if (r.width == tile.getWidth()
                && r.height == tile.getHeight()
                && deviceConfiguration.getColorModel().isCompatibleRaster(wr)) {
            return bi;
        }
        // todo <optimize>
        // The following code is still too slow. Try to use use JAI "format" and "crop" operations instead of
        // matching color model and tile bounds via a BufferedImage. We don't need to create a BufferedImage
        // then, because the resulting RenderedOp can be drawn directly using g.drawRenderedImage()
        final BufferedImage bi2 = deviceConfiguration.createCompatibleImage(r.width, r.height, bi.getTransparency());
        final Graphics2D g = bi2.createGraphics();
        g.drawRenderedImage(bi, null);
        g.dispose();
        return bi2;
        // todo </optimize>
    }

    private static Rectangle getImageRegion(Viewport vp, MultiLevelSource multiLevelSource, int level, Rectangle2D viewRegion) {
        return getViewToImageTransform(vp, multiLevelSource, level).createTransformedShape(viewRegion).getBounds();
    }

    private static Rectangle getViewRegion(Viewport vp, MultiLevelSource multiLevelSource, int level, Rectangle2D imageRegion) {
        return getImageToViewTransform(vp, multiLevelSource, level).createTransformedShape(imageRegion).getBounds();
    }

    private static AffineTransform getViewToImageTransform(Viewport vp, MultiLevelSource multiLevelSource, int level) {
        final AffineTransform t = vp.getViewToModelTransform();
        t.preConcatenate(multiLevelSource.getModel().getModelToImageTransform(level));
        return t;
    }

    private static AffineTransform getImageToViewTransform(Viewport vp, MultiLevelSource multiLevelSource, int level) {
        final AffineTransform t = new AffineTransform(multiLevelSource.getModel().getImageToModelTransform(level));
        t.preConcatenate(vp.getModelToViewTransform());
        return t;
    }

    private static int compareAscending(TileImage ti1, TileImage ti2) {
        int d = ti1.tileIndex.level - ti2.tileIndex.level;
        if (d != 0) {
            return d;
        }
        d = ti1.tileIndex.tileY - ti2.tileIndex.tileY;
        if (d != 0) {
            return d;
        }
        d = ti1.tileIndex.tileX - ti2.tileIndex.tileX;
        if (d != 0) {
            return d;
        }
        return 0;
    }

    private class TileComputationHandler implements TileComputationListener {
        private final InteractiveRendering rendering;
        private final GraphicsConfiguration deviceConfiguration;
        private final MultiLevelSource multiLevelSource;
        private final int level;

        private TileComputationHandler(InteractiveRendering rendering, MultiLevelSource multiLevelSource, int level) {
            this.rendering = rendering;
            this.deviceConfiguration = rendering.getGraphics().getDeviceConfiguration();
            this.multiLevelSource = multiLevelSource;
            this.level = level;
        }

        // Called from worker threads of the tile scheduler.
        @Override
        public void tileComputed(Object object,
                                 TileRequest[] tileRequests,
                                 PlanarImage planarImage,
                                 int tileX, int tileY,
                                 Raster tile) {
            if (tile == null) {
                System.out.println("WARNING: tileComputed: tile == null!");
                return;
            }

            // add to the cache, just in case it did not happen before!
            JAI.getDefaultInstance().getTileCache().add(planarImage, tileX, tileY, tile);

            TileIndex tileIndex = new TileIndex(tileX, tileY, level);
            final TileImage tileImage = createTileImage(deviceConfiguration,
                                                        planarImage,
                                                        tileIndex,
                                                        tile,
                                                        multiLevelSource.getModel().getImageToModelTransform(level));
            synchronized (ConcurrentMultiLevelRenderer.this) {
                scheduledTileRequests.remove(tileIndex);
                localTileCache.add(tileImage);
            }

            if (debug) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // don't care
                }
            }

            final Rectangle tileBounds = tile.getBounds();

            // Invoke in the EDT in order to obtain the
            // viewRegion for the currently valid viewport settings since the model
            // may have changed the viewport between the time the tile request was
            // created and the tile was computed, which is now. The EDT is the only safe
            // place to access the viewport.
            rendering.invokeLater(new Runnable() {
                // Called from EDT.
                @Override
                public void run() {
                    final Rectangle viewRegion = getViewRegion(rendering.getViewport(), multiLevelSource, level, tileBounds);
                    rendering.invalidateRegion(viewRegion);
                }
            });
        }

        // Called from worker threads of the tile scheduler.
        @Override
        public void tileCancelled(Object object,
                                  TileRequest[] tileRequests,
                                  PlanarImage planarImage,
                                  int tileX, int tileY) {
            TileIndex tileIndex = new TileIndex(tileX, tileY, level);
            dropTile(tileIndex);
            if (debug) {
                System.out.printf("ConcurrentMultiLevelRenderer: tileCancelled: %s\n", tileIndex);
            }
        }

        // Called from worker threads of the tile scheduler.
        @Override
        public void tileComputationFailure(Object object,
                                           TileRequest[] tileRequests,
                                           PlanarImage planarImage,
                                           int tileX, int tileY,
                                           Throwable error) {
            TileIndex tileIndex = new TileIndex(tileX, tileY, level);
            dropTile(tileIndex);
            if (debug) {
                System.out.printf("ConcurrentMultiLevelRenderer: tileComputationFailure: %s\n", tileIndex);
                error.printStackTrace();
            }
        }

        private void dropTile(TileIndex tileIndex) {
            synchronized (ConcurrentMultiLevelRenderer.this) {
                scheduledTileRequests.remove(tileIndex);
                localTileCache.remove(tileIndex);
            }
        }

    }

    private final class TileImageCache {
        private final Map<TileIndex, TileImage> cache;
        private long size;
        private long capacity;
        private long maxSize;
        private long retentionPeriod;

        public TileImageCache() {
            cache = new HashMap<TileIndex, TileImage>(37);
            retentionPeriod = 10000L;
            capacity = 16L * (1024 * 1024);
            maxSize = Math.round(0.75 * capacity);
        }

        public synchronized boolean contains(TileIndex tileIndex) {
            return cache.containsKey(tileIndex);
        }

        public synchronized TileImage get(TileIndex tileIndex) {
            return cache.get(tileIndex);
        }

        public synchronized Collection<TileImage> getAll() {
            return new ArrayList<TileImage>(cache.values());
        }

        public synchronized void add(TileImage tileImage) {
            final TileImage oldTileImage = cache.put(tileImage.tileIndex, tileImage);
            if (oldTileImage != null) {
                size -= oldTileImage.size;
            }
            size += tileImage.size;
            if (debug) {
                System.out.printf("ConcurrentMultiLevelRenderer$TileImageCache: add: tileIndex=%s, size=%d\n", tileImage.tileIndex, size);
            }
        }

        public synchronized void remove(TileIndex tileIndex) {
            final TileImage oldTileImage = cache.remove(tileIndex);
            if (oldTileImage != null) {
                size -= oldTileImage.size;
                if (debug) {
                    System.out.printf("ConcurrentMultiLevelRenderer$TileImageCache: remove: tileIndex=%s, size=%d\n", tileIndex, size);
                }
            }
        }

        public synchronized void clear() {
            cache.clear();
            size = 0L;
        }

        public synchronized void trim(int currentLevel) {
            if (size > capacity) {
                final long now = System.currentTimeMillis();
                final TreeSet<TileImage> treeSet = new TreeSet<TileImage>(new AscendingLevelsComparator());
                treeSet.addAll(cache.values());
                // try to remove "old" tiles from other levels first
                for (TileImage image : treeSet) {
                    if (image.tileIndex.level != currentLevel) {
                        maybeRemove(image, now);
                    }
                }
                // If we still need clean up, remove "old" tiles from current level as well
                for (TileImage image : treeSet) {
                    if (image.tileIndex.level == currentLevel) {
                        maybeRemove(image, now);
                    }
                }
            }
        }

        private void maybeRemove(TileImage image, long now) {
            if (size > maxSize) {
                final long age = now - image.timeStamp;
                if (age > retentionPeriod) {
                    remove(image.tileIndex);
                }
            }
        }
    }

    private final static class TileImage {
        private final RenderedImage image;
        private final TileIndex tileIndex;
        /**
         * x offset in image CS
         */
        private final int x;
        /**
         * y offset in image CS
         */
        private final int y;
        private final AffineTransform i2m;
        /**
         * tile bounds in model CS
         */
        private final Rectangle2D bounds;
        /**
         * tile size in bytes
         */
        private final long size;
        /**
         * last access time stamp
         */
        private long timeStamp;

        private TileImage(RenderedImage image, TileIndex tileIndex, int x, int y, AffineTransform i2m) {
            this.image = image;
            this.tileIndex = tileIndex;
            this.x = x;
            this.y = y;
            this.i2m = new AffineTransform(i2m);
            this.bounds = i2m.createTransformedShape(new Rectangle(x, y, image.getWidth(), image.getHeight())).getBounds2D();
            this.size = image.getWidth() * image.getHeight() * (image.getSampleModel().getNumBands() * image.getSampleModel().getSampleSize(0)) / 8;
            this.timeStamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("TileImage[tileIndex=%s,size=%d,bounds=%s]", String.valueOf(tileIndex), size, String.valueOf(bounds));
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            final TileImage tileImage = (TileImage) object;
            return tileIndex.equals(tileImage.tileIndex);
        }

        @Override
        public int hashCode() {
            return tileIndex.hashCode();
        }

    }

    private final static class TileIndex {
        private final int tileX;
        private final int tileY;
        private final int level;

        private TileIndex(int tileX, int tileY, int level) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.level = level;
        }

        Point getPoint() {
            return new Point(tileX, tileY);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            TileIndex tileIndex = (TileIndex) object;
            return level == tileIndex.level
                    && tileY == tileIndex.tileY
                    && tileX == tileIndex.tileX;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * level + tileY) + tileX;
        }

        @Override
        public String toString() {
            return String.format("TileIndex[tileX=%d,tileY=%d,level=%d]", tileX, tileY, level);
        }
    }

    private static class AscendingLevelsComparator implements Comparator<TileImage> {
        @Override
        public int compare(TileImage ti1, TileImage ti2) {
            return compareAscending(ti1, ti2);
        }
    }

    private static class DescendingLevelsComparator implements Comparator<TileImage> {
        @Override
        public int compare(TileImage ti1, TileImage ti2) {
            return compareAscending(ti2, ti1);
        }
    }
}
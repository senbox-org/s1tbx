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

package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ConcurrentMultiLevelRenderer implements MultiLevelRenderer {

    private final static boolean DEBUG = Boolean.getBoolean("snap.renderer.debug");

    private final Map<TileIndex, TileRequest> scheduledTileRequests;
    private final TileImageCache localTileCache;
    private final DescendingLevelsComparator descendingLevelsComparator = new DescendingLevelsComparator();

    public ConcurrentMultiLevelRenderer() {
        scheduledTileRequests = Collections.synchronizedMap(new HashMap<TileIndex, TileRequest>(37));
        localTileCache = new TileImageCache();

        if (DEBUG) {
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

    @Override
    public synchronized void reset() {
        cancelTileRequests(-1);
        localTileCache.clear();
    }

    @Override
    public void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int currentLevel) {
        final long t0 = System.nanoTime();
        renderImpl((InteractiveRendering) rendering, multiLevelSource, currentLevel);
        if (DEBUG) {
            final long t1 = System.nanoTime();
            double time = (t1 - t0) / (1000.0 * 1000.0);
            System.out.printf("ConcurrentMultiLevelRenderer: render: time=%f ms, clip=%s\n", time, rendering.getGraphics().getClip());
        }
    }

    private void renderImpl(InteractiveRendering rendering, MultiLevelSource multiLevelSource, int currentLevel) {

        final PlanarImage planarImage = (PlanarImage) multiLevelSource.getImage(currentLevel);
        final Graphics2D graphics = rendering.getGraphics();
        final Viewport viewport = rendering.getViewport();

        // Check that color model is available, required for this renderer
        final ColorModel colorModel = planarImage.getColorModel();
        if (colorModel == null) {
            throw new IllegalStateException("colorModel == null");
        }

        // Current view's bounds in view (pixel) coordinates
        final Rectangle viewBounds = viewport.getViewBounds();
        // Check clipping rectangle in view (pixel) coordinates, required for this renderer
        final Rectangle clipBounds = graphics.getClipBounds();
        // Create set of required tile indexes
        final Rectangle clippedImageRegion = getImageRegion(viewport, multiLevelSource, currentLevel, clipBounds != null ? clipBounds : viewBounds);
        final Set<TileIndex> requiredTileIndexes = getTileIndexes(planarImage, multiLevelSource.getImageShape(currentLevel), currentLevel, clippedImageRegion);
        if (requiredTileIndexes.isEmpty()) {
            return; // nothing to render
        }

        // Create lists of available and missing tile indexes
        final List<TileIndex> availableTileIndexList = new ArrayList<>(requiredTileIndexes.size());
        final List<TileIndex> missingTileIndexList = new ArrayList<>(requiredTileIndexes.size());
        final List<TileIndex> notScheduledTileIndexList = new ArrayList<>(requiredTileIndexes.size());
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
                                                                        }
            );
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

        if (DEBUG) {
            // Draw tile frames
            final AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(currentLevel);
            drawTileImageFrames(graphics, viewport, availableTileIndexList, i2m, Color.YELLOW);
            drawTileFrames(graphics, viewport, planarImage, missingTileIndexList, i2m, Color.RED);
            drawTileFrames(graphics, viewport, planarImage, availableTileIndexList, i2m, Color.BLUE);
        }

        // Cancel any pending tile requests that are not in the visible region
        final Rectangle visibleImageRegion = getImageRegion(viewport, multiLevelSource, currentLevel, viewBounds);
        final Set<TileIndex> visibleTileIndexSet = getTileIndexes(planarImage, multiLevelSource.getImageShape(currentLevel), currentLevel, visibleImageRegion);
        if (!visibleTileIndexSet.isEmpty()) {
            cancelTileRequests(visibleTileIndexSet);
        }

        localTileCache.adjustTrimSize(planarImage, visibleTileIndexSet.size());

        // Remove any tile images that are older than the retention period.
        localTileCache.trim(currentLevel, visibleTileIndexSet);
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

            final TreeSet<TileImage> tentativeTileImageSet = new TreeSet<>(descendingLevelsComparator);
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
            Rectangle newClip = vp.getModelToViewTransform().createTransformedShape(bounds).getBounds();
            newClip = newClip.intersection(vp.getViewBounds());
            g.setClip(newClip);
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

    private static Set<TileIndex> getTileIndexes(PlanarImage planarImage, Shape imageShape, int level, Rectangle clippedImageRegion) {
        final Point[] indices = planarImage.getTileIndices(clippedImageRegion);
        if (indices == null || indices.length == 0) {
            return Collections.emptySet();
        }
        final Set<TileIndex> indexes = new HashSet<>((3 * indices.length) / 2);
        for (Point point : indices) {
            Rectangle tileRect = planarImage.getTileRect(point.x, point.y);
            if (imageShape == null || imageShape.intersects(tileRect)) {
                indexes.add(new TileIndex(point.x, point.y, level));
            }
        }
        return indexes;
    }

    private static void drawTileImage(Graphics2D g, Viewport vp, TileImage ti) {
        final AffineTransform t = AffineTransform.getTranslateInstance(ti.x, ti.y);
        t.preConcatenate(ti.i2m);
        t.preConcatenate(vp.getModelToViewTransform());
        g.drawRenderedImage(ti.image, t);
        ti.lastAccessTime = System.currentTimeMillis();
    }

    private void drawTileImageFrames(Graphics2D g, Viewport vp, List<TileIndex> tileIndices,
                                     AffineTransform i2m, Color frameColor) {
        final AffineTransform m2v = vp.getModelToViewTransform();
        final AffineTransform oldTransform = g.getTransform();
        final Color oldColor = g.getColor();
        final Stroke oldStroke = g.getStroke();
        final AffineTransform t = new AffineTransform();
        t.preConcatenate(i2m);
        t.preConcatenate(m2v);
        g.setTransform(t);
        g.setColor(new Color(frameColor.getRed(), frameColor.getGreen(), frameColor.getBlue(), 127));
        g.setStroke(new BasicStroke(5.0f));
        for (final TileIndex tileIndex : tileIndices) {
            final TileImage tileImage = localTileCache.get(tileIndex);
            final Rectangle tileRect = new Rectangle(tileImage.x, tileImage.y, tileImage.image.getWidth(), tileImage.image.getHeight());
            g.draw(tileRect);
            System.out.println("Tile image bounds: " + tileRect);
        }
        g.setStroke(oldStroke);
        g.setColor(oldColor);
        g.setTransform(oldTransform);
    }


    private static void drawTileFrames(Graphics2D g, Viewport vp, PlanarImage planarImage,
                                       List<TileIndex> tileIndices,
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
        g.setStroke(new BasicStroke(1.0f));
        for (TileIndex tileIndex : tileIndices) {
            g.draw(planarImage.getTileRect(tileIndex.tileX, tileIndex.tileY));
        }
        g.setStroke(oldStroke);
        g.setColor(oldColor);
        g.setTransform(oldTransform);
    }

    // Called from EDT.
    // Cancels any tiles that are in the scheduled list and not in the visibleTileIndexSet list.
    private void cancelTileRequests(Set<TileIndex> visibleTileIndexSet) {
        final Map<TileIndex, TileRequest> scheduledTileRequestsCopy;
        synchronized (scheduledTileRequests) {
            scheduledTileRequestsCopy = new HashMap<>(scheduledTileRequests);
        }
        // scan through the scheduled tiles list cancelling any that are no longer in view
        for (Map.Entry<TileIndex, TileRequest> scheduledTileEntry : scheduledTileRequestsCopy.entrySet()) {
            TileIndex scheduledTileIndex = scheduledTileEntry.getKey();
            if (!visibleTileIndexSet.contains(scheduledTileIndex)) {
                TileRequest request = scheduledTileEntry.getValue();
                // if tile not already removed (concurrently)
                if (request != null) {
                    scheduledTileRequests.remove(scheduledTileIndex);
                    request.cancelTiles(new Point[]{new Point(scheduledTileIndex.tileX, scheduledTileIndex.tileY)});
                }
            }
        }
    }

    private void cancelTileRequests(int currentLevel) {
        final Map<TileIndex, TileRequest> scheduledTileRequestsCopy;
        synchronized (scheduledTileRequests) {
            scheduledTileRequestsCopy = new HashMap<>(scheduledTileRequests);
        }
        for (Map.Entry<TileIndex, TileRequest> entry : scheduledTileRequestsCopy.entrySet()) {
            TileIndex tileIndex = entry.getKey();
            if (tileIndex.level != currentLevel) {
                scheduledTileRequests.remove(tileIndex);
                entry.getValue().cancelTiles(null);
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
        // todo: Optimize me!
        // The following code might still be too slow. Try to use use JAI "format" and "crop" operations instead of
        // matching color model and tile bounds via a BufferedImage. We don't need to create a BufferedImage
        // then, because the resulting RenderedOp can be drawn directly using g.drawRenderedImage()
        final BufferedImage bi2 = deviceConfiguration.createCompatibleImage(r.width, r.height, bi.getTransparency());
        final Graphics2D g = bi2.createGraphics();
        g.drawRenderedImage(bi, null);
        g.dispose();
        return bi2;
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
                if (DEBUG) {
                    System.out.println("WARNING: tileComputed: tile == null!");
                }
                return;
            }

            TileIndex tileIndex = new TileIndex(tileX, tileY, level);
            // Check whether tile is still required or has been canceled already
            if (!scheduledTileRequests.containsKey(tileIndex)) {
                // todo - problem here if this renderer is shared by multiple views (nf, 20081216)
                //return;
            }

            final TileImage tileImage = createTileImage(deviceConfiguration,
                                                        planarImage,
                                                        tileIndex,
                                                        tile,
                                                        multiLevelSource.getModel().getImageToModelTransform(level));
            synchronized (ConcurrentMultiLevelRenderer.this) {
                scheduledTileRequests.remove(tileIndex);
                localTileCache.add(tileImage);
            }

// Uncomment for debugging
//            if (DEBUG) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    // don't care
//                }
//            }

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
            if (DEBUG) {
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
            if (DEBUG) {
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
        private long currentSize;
        private long trimSize;
        private final boolean adaptive;
        private final double tileFactor;
        private final long minSize;
        private final long maxSize;
        private final long retentionPeriod;

        public TileImageCache() {
            cache = new HashMap<>(37);
            retentionPeriod = Long.parseLong(System.getProperty("ceres.renderer.cache.retentionPeriod", "10000"));
            adaptive = Boolean.parseBoolean(System.getProperty("ceres.renderer.cache.adaptive", "true"));
            tileFactor = Double.parseDouble(System.getProperty("ceres.renderer.cache.tileFactor", "2.5"));
            minSize = Long.parseLong(System.getProperty("ceres.renderer.cache.minSize", "0")) * (1024 * 1024);
            maxSize = Long.parseLong(System.getProperty("ceres.renderer.cache.maxSize", System.getProperty("ceres.renderer.cache.capacity", "64"))) * (1024 * 1024);
            currentSize = 0;
            trimSize = maxSize;
        }

        public synchronized boolean contains(TileIndex tileIndex) {
            return cache.containsKey(tileIndex);
        }

        public synchronized TileImage get(TileIndex tileIndex) {
            return cache.get(tileIndex);
        }

        public synchronized Collection<TileImage> getAll() {
            return new ArrayList<>(cache.values());
        }

        public synchronized void add(TileImage tileImage) {
            final TileImage oldTileImage = cache.put(tileImage.tileIndex, tileImage);
            if (oldTileImage != null) {
                currentSize -= oldTileImage.size;
            }
            currentSize += tileImage.size;
            if (DEBUG) {
                System.out.printf("ConcurrentMultiLevelRenderer$TileImageCache: add: tileIndex=%s, size=%d\n", tileImage.tileIndex, currentSize);
            }
        }

        public synchronized void remove(TileIndex tileIndex) {
            final TileImage oldTileImage = cache.remove(tileIndex);
            if (oldTileImage != null) {
                currentSize -= oldTileImage.size;
                if (DEBUG) {
                    System.out.printf("ConcurrentMultiLevelRenderer$TileImageCache: remove: tileIndex=%s, size=%d\n", tileIndex, currentSize);
                }
            }
        }

        public synchronized void clear() {
            cache.clear();
            currentSize = 0L;
        }

        public void adjustTrimSize(PlanarImage image, int numRequiredTiles) {
            if (adaptive) {
                SampleModel sm = image.getSampleModel();
                long pixelSize = (long) (sm.getNumBands() * sm.getSampleSize(0)) / 8;
                long tileSize = (long) sm.getWidth() * (long) sm.getHeight() * pixelSize;
                long trimSize = Math.round(tileFactor * numRequiredTiles) * tileSize;
                if (minSize >= 0 && trimSize < minSize) {
                    trimSize = minSize;
                }
                if (maxSize >= 0 && trimSize > maxSize) {
                    trimSize = maxSize;
                }
                this.trimSize = trimSize;
            } else {
                this.trimSize = maxSize;
            }
        }

        public synchronized void trim(int currentLevel, Set<TileIndex> visibleTileIndexes) {

            if (DEBUG) {
                long oneMiB = 1024L * 1024L;
                System.out.println("ConcurrentMultiLevelRenderer.TileImageCache:");
                System.out.printf("    currentSize     = %10d%n", localTileCache.currentSize / oneMiB);
                System.out.printf("    trimSize        = %10d%n", localTileCache.trimSize / oneMiB);
                System.out.printf("    minSize         = %10d%n", localTileCache.minSize / oneMiB);
                System.out.printf("    maxSize         = %10d%n", localTileCache.maxSize / oneMiB);
                System.out.printf("    tileFactor      = %f%n", localTileCache.tileFactor);
                System.out.printf("    retentionPeriod = %d%n", localTileCache.retentionPeriod);
            }

            if (currentSize > trimSize) {
                long now = System.currentTimeMillis();
                Collection<TileImage> tileImages = new ArrayList<>(cache.values());
                for (TileImage tileImage : tileImages) {
                    if (!visibleTileIndexes.contains(tileImage.tileIndex)
                        && tileImage.tileIndex.level != currentLevel) {
                        maybeRemove(tileImage, now);
                    }
                }
                for (TileImage tileImage : tileImages) {
                    if (!visibleTileIndexes.contains(tileImage.tileIndex)
                        && tileImage.tileIndex.level == currentLevel) {
                        maybeRemove(tileImage, now);
                    }
                }
            }
        }

        private void maybeRemove(TileImage image, long now) {
            final long age = now - image.lastAccessTime;
            if (age > retentionPeriod) {
                remove(image.tileIndex);
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
        /**
         * image-to-model transformation
         */
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
        private long lastAccessTime;

        private TileImage(RenderedImage image, TileIndex tileIndex, int x, int y, AffineTransform i2m) {
            this.image = image;
            this.tileIndex = tileIndex;
            this.x = x;
            this.y = y;
            this.i2m = new AffineTransform(i2m);
            this.bounds = i2m.createTransformedShape(new Rectangle(x, y, image.getWidth(), image.getHeight())).getBounds2D();
            this.size = image.getWidth() * image.getHeight() * (image.getSampleModel().getNumBands() * image.getSampleModel().getSampleSize(0)) / 8;
            this.lastAccessTime = System.currentTimeMillis();
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

    private static class DescendingLevelsComparator implements Comparator<TileImage> {
        @Override
        public int compare(TileImage ti1, TileImage ti2) {
            return compareAscending(ti2, ti1);
        }
    }
}
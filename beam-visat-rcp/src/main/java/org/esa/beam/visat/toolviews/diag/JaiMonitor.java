package org.esa.beam.visat.toolviews.diag;

import org.esa.beam.jai.RasterDataNodeOpImage;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.jai.VirtualBandOpImage;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.UnitType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.media.jai.CachedTile;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import com.bc.swing.desktop.TabbedDesktopPane;
import com.sun.media.jai.util.CacheDiagnostics;
import com.sun.media.jai.util.SunTileCache;


public class JaiMonitor {

    private static class CachedTileInfo {
        Object uid;
        String imageName;
        int level;
        int numTiles;
        long size;
        String comment;
    }
    private static class TileCacheTableModel extends AbstractTableModel {
        private final static String[] COLUM_NAMES = {"Image", "#Tiles", "Size [kB]", "Level", "Comment"};
        private final static Class[] COLUM_CLASSES = {String.class, Integer.class, Long.class, Integer.class, String.class};
        List<CachedTileInfo> data = new ArrayList<CachedTileInfo>(50);
        
        @Override
        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public int getColumnCount() {
            return COLUM_NAMES.length;
        }
        
        @Override
        public String getColumnName(int columnIndex) {
            return COLUM_NAMES[columnIndex];
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUM_CLASSES[columnIndex];
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            CachedTileInfo cachedTileInfo = data.get(row);
            switch (column) {
                case 0:
                    return cachedTileInfo.imageName;
                case 1:
                    return cachedTileInfo.numTiles;
                case 2:
                    return cachedTileInfo.size/1024;
                case 3:
                    return cachedTileInfo.level;
                case 4:
                    return cachedTileInfo.comment;
            }
            return null;
        }

        public void reset() {
            for (CachedTileInfo tileInfo : data) {
                tileInfo.numTiles = 0;
                tileInfo.size = 0;
            }
        }
        
        public void cleanUp() {
            Iterator<CachedTileInfo> iterator = data.iterator();
            while (iterator.hasNext()) {
                CachedTileInfo tileInfo = iterator.next();
                if (tileInfo.numTiles == 0) {
                    iterator.remove();
                }
            }
        }
        
        public void addRow(CachedTileInfo tileInfo) {
            data.add(tileInfo);
        }
    }

    /**
     * The datasets.
     */
    private TimeSeriesCollection[] datasets;
    private TileCacheTableModel tableModel;
    private JTextArea textarea;

    /**
     * Creates a new monitor panel.
     *
     * @return the monitor panel
     */
    public JPanel createPanel() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(
                new DateAxis("Time")
        );
        this.datasets = new TimeSeriesCollection[4];
        this.datasets[0] = addSubPlot(plot, "#Tiles");
        this.datasets[1] = addSubPlot(plot, "#Hits");
        this.datasets[2] = addSubPlot(plot, "#Misses");
        this.datasets[3] = addSubPlot(plot, "Memory [kB]");

//        JFreeChart chart = new JFreeChart("Tile cache usage", plot);
        JFreeChart chart = new JFreeChart(plot);
        LegendTitle legend = (LegendTitle) chart.getSubtitle(0);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setMargin(
                new RectangleInsets(UnitType.ABSOLUTE, 0, 4, 0, 4)
        );
        chart.setBorderPaint(Color.black);
        chart.setBorderVisible(true);
        chart.setBackgroundPaint(Color.white);

        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));
        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds

        final JTabbedPane jTabbedPane = new JTabbedPane();
        ChartPanel chartPanel = new ChartPanel(chart);
        jTabbedPane.add("Chart", chartPanel);
        tableModel = new TileCacheTableModel();
        JScrollPane tableScrollPane = new JScrollPane(new JTable(tableModel));
        jTabbedPane.add("Table", tableScrollPane);
        textarea = new JTextArea();
        jTabbedPane.add("Info", textarea);
        mainPanel.add(jTabbedPane);

        chartPanel.setPreferredSize(new java.awt.Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return mainPanel;
    }

    public synchronized void updateState() {
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        if (tileCache instanceof CacheDiagnostics) {
            CacheDiagnostics cacheDiagnostics = (CacheDiagnostics) tileCache;
            cacheDiagnostics.enableDiagnostics();
            final Millisecond t = new Millisecond();
            update(0, t, cacheDiagnostics.getCacheTileCount());
            update(1, t, cacheDiagnostics.getCacheHitCount());
            update(2, t, cacheDiagnostics.getCacheMissCount());
            update(3, t, cacheDiagnostics.getCacheMemoryUsed()/1024);
        } else {
            // todo - ?
        }
        if (tileCache instanceof SunTileCache) {
            SunTileCache stc = (SunTileCache) tileCache;
            synchronized (stc) {
                Hashtable cachedObject = (Hashtable) stc.getCachedObject();
                Collection<CachedTile> tiles = cachedObject.values();
//                dumpImageTree(tiles);
                updateTableModel(tiles);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("MemoryCapacity: \t");
            sb.append(stc.getMemoryCapacity()/1024);
            sb.append(" kB\n");
            sb.append("MemoryThreshold: \t");
            sb.append(stc.getMemoryThreshold());
            sb.append("\n");
            sb.append("#tilesInCache: \t");
            sb.append(((Hashtable)stc.getCachedObject()).size());
            sb.append("\n");
            textarea.setText(sb.toString());
        }
    }
    
    private void updateTableModel(Collection<CachedTile> tiles) {
        tableModel.reset();
        for (CachedTile  sct : tiles) {
            RenderedImage owner = sct.getOwner();
            if (owner == null || !(owner instanceof PlanarImage)) {
                continue;
            }
            PlanarImage image = (PlanarImage) owner;
            Object imageID = image.getImageID();
            CachedTileInfo cachedTileInfo = null;
            for (CachedTileInfo info : tableModel.data) {
                if (info.uid.equals(imageID)) {
                    cachedTileInfo = info;
                    break;
                }
            }
            if (cachedTileInfo == null) {
                cachedTileInfo = new CachedTileInfo();
                cachedTileInfo.uid = imageID;
                cachedTileInfo.imageName = getImageName(image);
                cachedTileInfo.level = getImageLevel(image);
                cachedTileInfo.comment = getImageComment(image);
                tableModel.data.add(cachedTileInfo);
            }
            cachedTileInfo.numTiles++;
            cachedTileInfo.size += sct.getTileSize();
        }
        tableModel.cleanUp();
        tableModel.fireTableDataChanged();
        
    }


    private String getImageName(RenderedImage image) {
        return image.getClass().getSimpleName();
    }

    private int getImageLevel(RenderedImage image) {
        if (image instanceof SingleBandedOpImage) {
            SingleBandedOpImage sboi = (SingleBandedOpImage) image;
            return sboi.getLevel();
        }
        return -1;
    }
    
    private String getImageComment(RenderedImage image) {
        if (image instanceof RasterDataNodeOpImage) {
            RasterDataNodeOpImage rdnoi = (RasterDataNodeOpImage) image;
            return "rdn="+rdnoi.getRasterDataNode().getName();
        } else if (image instanceof VirtualBandOpImage) {
            VirtualBandOpImage vboi = (VirtualBandOpImage) image;
            return "expression="+vboi.getExpression();
        }
        return "";
    }
    
    private void dumpImageTree(Collection<CachedTile> tiles) {
        final Map<RenderedImage, Integer> ownerMap = new HashMap<RenderedImage, Integer>(100);
        final Map<RenderedImage, Long> sizeMap = new HashMap<RenderedImage, Long>(100);
        for (CachedTile  sct : tiles) {
            RenderedImage owner = sct.getOwner();
            if (owner == null) {
                continue;
            }
            Integer count = ownerMap.get(owner);
            if (count == null) {
                ownerMap.put(owner, Integer.valueOf(1));
            } else {
                ownerMap.put(owner, Integer.valueOf(count.intValue()+1));
            }
            Long size = sizeMap.get(owner);
            if (size == null) {
                sizeMap.put(owner, Long.valueOf(sct.getTileSize()));
            } else {
                sizeMap.put(owner, Long.valueOf(size + sct.getTileSize()));
            }
        }

        Set<RenderedImage> rootEntries = new HashSet<RenderedImage>(ownerMap.keySet());
        for (RenderedImage image : ownerMap.keySet()) {
            Vector<RenderedImage> sources = image.getSources();
            eleminateSources(sources, rootEntries);
        }
        for (RenderedImage image : rootEntries) {
            printImage(image, ownerMap.get(image), sizeMap.get(image));
            printSources("", image.getSources(), ownerMap, sizeMap);
        }
        System.out.println("======================================");
    }
        
    private void eleminateSources(Vector<RenderedImage> sources,
                                  Set<RenderedImage> rootEntries) {
        if (sources != null) {
            for (RenderedImage renderedImage : sources) {
                if (rootEntries.contains(renderedImage)) {
                    rootEntries.remove(renderedImage);
                }
                eleminateSources(renderedImage.getSources(), rootEntries);
            }
        }
    }

    private void printSources(String prefix, 
                              Vector<RenderedImage> sources, 
                              Map<RenderedImage, Integer> ownerMap,
                              Map<RenderedImage, Long> sizeMap) {
        if (sources != null) {
            for (RenderedImage image : sources) {
                System.out.print(prefix+"->");
                if (ownerMap.containsKey(image)) {
                    printImage(image, ownerMap.get(image), sizeMap.get(image));
                } else {
                    printImage(image, 0, 0L);
                }
                printSources("--"+prefix, image.getSources(), ownerMap, sizeMap);
            }
        }
    }
    
    private void printImage(RenderedImage image, int numTiles, Long size) {
        System.out.print("("+getImageName(image)+")  ");
        if (numTiles > 0) {
            System.out.print("#tiles="+numTiles+"  ");
            System.out.printf("size=%8.2fMB  ",(size/(1024.0*1024.0)));
        }
        final int level = getImageLevel(image);
        if (level >= 0) {
            System.out.print("level="+level+"  ");
        }
        System.out.print(getImageComment(image));
        System.out.println();
    }

    private static TimeSeriesCollection addSubPlot(CombinedDomainXYPlot plot, String label) {
        final TimeSeriesCollection seriesCollection = new TimeSeriesCollection(new TimeSeries(
                label, Millisecond.class
        ));
        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        XYPlot subplot = new XYPlot(
                seriesCollection, null, rangeAxis,
                new StandardXYItemRenderer()
        );
        subplot.setBackgroundPaint(Color.lightGray);
        subplot.setDomainGridlinePaint(Color.white);
        subplot.setRangeGridlinePaint(Color.white);
        plot.add(subplot);
        return seriesCollection;
    }

    private void update(int i, Millisecond t, double value) {
        this.datasets[i].getSeries(0).add(t, value);
    }
}


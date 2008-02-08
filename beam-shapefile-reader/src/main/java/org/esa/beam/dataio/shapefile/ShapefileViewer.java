package org.esa.beam.dataio.shapefile;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public class ShapefileViewer {
    private Shapefile shapefile;
    private JFrame frame;
    private Shapefile.Header header;
    private ArrayList<Shapefile.Geometry> geometries;
    private JPanel panel;

    public ShapefileViewer(Shapefile shapefile) throws IOException {
        this.shapefile = shapefile;
        header = shapefile.readHeader();
        geometries = new ArrayList<Shapefile.Geometry>(100);

        frame = new JFrame("Shapefile Viewer - [" + shapefile.getMainFile().getName() + "]");
        panel = createPanel();
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(new Runnable() {
            public void run() {
                loadGeometry();
            }

        }).start();
    }

    private void loadGeometry() {
        try {
            while (true) {
                try {
                    Shapefile.Record record = shapefile.readRecord();
                    geometries.add(record.geometry);
                } catch (EOFException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            try {
                shapefile.close();
            } catch (IOException e1) {
                // ok
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.repaint();
            }
        });
    }

    public static void main(String[] args) {
        try {
            for (String arg : args) {
                new ShapefileViewer(Shapefile.getShapefile(arg));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }

    private JPanel createPanel() {
        return new MyJPanel();
    }

    private class MyJPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics2D = (Graphics2D) g;


            Shapefile.Geometry[] geometries = ShapefileViewer.this.geometries.toArray(new Shapefile.Geometry[0]);
            int i = 0;
            for (Shapefile.Geometry geometry : geometries) {
                // System.out.println("geometry = " + geometry);
                Shape shape = getShape(geometry);
                if (shape != null) {
                    Color[] c = new Color[]{Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.ORANGE, Color.PINK, Color.GRAY};
                    graphics2D.setColor(c[i % c.length]);
                    graphics2D.fill(shape);
                    graphics2D.setColor(Color.BLACK);
                    graphics2D.draw(shape);
                    i++;
                }
            }
        }

        private Shape getShape(Shapefile.Geometry geometry) {
            Shape shape = null;
            if (geometry instanceof Shapefile.Point) {
                Shapefile.Point point = (Shapefile.Point) geometry;
                double vx = normX(point.x);
                double vy = normY(point.y);
                shape = new Ellipse2D.Double(vx - 2, vy - 2, 4, 4);
            } else if (geometry instanceof Shapefile.Polyline) {
                Shapefile.Polyline polyline = (Shapefile.Polyline) geometry;
                int[] parts = polyline.parts;
                Shapefile.Point[] points = polyline.points;
                GeneralPath path = new GeneralPath();
                for (int j = 0; j < parts.length; j++) {
                    GeneralPath subPath = new GeneralPath();
                    int iFirst = parts[j];
                    int iNext = j < parts.length - 1 ? parts[j + 1] : points.length;
                    // System.out.println("i = " + iFirst + " ... " + (iNext-1));
                    for (int i = iFirst; i < iNext; i++) {
                        Shapefile.Point point = points[i];
                        double vx = normX(point.x);
                        double vy = normY(point.y);
                        if (i == iFirst) {
                            subPath.moveTo(vx, vy);
                        } else {
                            subPath.lineTo(vx, vy);
                        }
                    }
                    path.append(subPath, false);
                }
                shape = path;
            } else if (geometry instanceof Shapefile.Polygon) {
                Shapefile.Polygon polygon = (Shapefile.Polygon) geometry;
                int[] parts = polygon.parts;
                Shapefile.Point[] points = polygon.points;
                GeneralPath path = new GeneralPath();
                for (int j = 0; j < parts.length; j++) {
                    GeneralPath subPath = new GeneralPath();
                    int iFirst = parts[j];
                    int iNext = j < parts.length - 1 ? parts[j + 1] : points.length;
                    // System.out.println("i = " + iFirst + " ... " + (iNext-1));
                    for (int i = iFirst; i < iNext; i++) {
                        Shapefile.Point point = points[i];
                        double vx = normX(point.x);
                        double vy = normY(point.y);
                        if (i == iFirst) {
                            subPath.moveTo(vx, vy);
                        } else {
                            subPath.lineTo(vx, vy);
                        }
                    }
                    subPath.closePath();
                    path.append(subPath, false);
                }
                shape = path;
            } else {
                System.out.println("unhandled geometry: " + geometry);
            }
            return shape;
        }

        private double normY(double y) {
            double ny = (y - header.ymin) / (header.ymax - header.ymin);
            return getHeight() - ny * getHeight();
        }

        private double normX(double x) {
            double nx = (x - header.xmin) / (header.xmax - header.xmin);
            return nx * getWidth();
        }
    }
}

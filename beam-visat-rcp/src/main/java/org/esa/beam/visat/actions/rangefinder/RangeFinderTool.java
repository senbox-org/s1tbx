package org.esa.beam.visat.actions.rangefinder;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A tool representing the range finder.
 *
 * @author Sabine Embacher
 */
public class RangeFinderTool extends AbstractTool {

    public static final String TITLE = "Range Finder Tool";

    private final List<Point> _pointList;
    private final Point _currPoint;
    private Cursor _cursor;
    private ImageIcon cursorIcon;

    public RangeFinderTool() {
        this.cursorIcon = UIUtils.loadImageIcon("cursors/RangeFinder.gif");
        _pointList = new ArrayList<Point>();
        _currPoint = new Point();
        _cursor = createRangeFinderCursor();
    }

    @Override
    public Cursor getCursor() {
        return _cursor;
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (_pointList.size() == 0) {
            return;
        }

        final Stroke strokeOld = g2d.getStroke();
        g2d.setStroke(new BasicStroke(0.1f));
        final Color colorOld = g2d.getColor();
        g2d.setColor(Color.red);
        g2d.translate(0.5, 0.5);

        final int r = 3;
        final int r2 = r * 2;

        Point p1 = null;
        Point p2 = null;
        for (int i = 0; i < _pointList.size(); i++) {
            p1 = _pointList.get(i);

            g2d.drawOval(p1.x - r, p1.y - r, r2, r2);
            g2d.drawLine(p1.x, p1.y - r2, p1.x, p1.y - r);
            g2d.drawLine(p1.x, p1.y + r2, p1.x, p1.y + r);
            g2d.drawLine(p1.x - r2, p1.y, p1.x - r, p1.y);
            g2d.drawLine(p1.x + r2, p1.y, p1.x + r, p1.y);

            if (p2 != null) {
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            p2 = p1;
        }

        p2 = _currPoint;
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

        g2d.translate(-0.5, -0.5);
        g2d.setStroke(strokeOld);
        g2d.setColor(colorOld);
    }

    @Override
    public void mouseDragged(ToolInputEvent e) {
        super.mouseDragged(e);
        handleDragAndMove(e);
    }

    @Override
    public void mouseMoved(ToolInputEvent e) {
        super.mouseMoved(e);
        handleDragAndMove(e);
    }

    @Override
    public void mouseClicked(ToolInputEvent e) {
        super.mouseClicked(e);
        _pointList.add(e.getPixelPos());
        _currPoint.setLocation(e.getPixelPos());
        repaint();
        if (e.getMouseEvent().getClickCount() > 1 && _pointList.size() > 0) {
            ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
            GeoCoding geoCoding = view.getProduct().getGeoCoding();

            float distance = 0;
            float distanceError = 0;

            final DistanceData[] distanceDatas = new DistanceData[_pointList.size() - 1];
            for (int i = 0; i < distanceDatas.length; i++) {
                final Point p1 = _pointList.get(i);
                final Point p2 = _pointList.get(i + 1);

                final DistanceData distanceData = new DistanceData(geoCoding, p1, p2);
                distance += distanceData.distance;
                distanceError += distanceData.distanceError;
                distanceDatas[i] = distanceData;
            }


            final JButton detailsButton = new JButton("Details...");
            detailsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final Window parentWindow = SwingUtilities.getWindowAncestor(detailsButton);
                    ModalDialog detailsDialog = org.esa.beam.visat.actions.rangefinder.RangeFinderTool.createDetailsDialog(
                                parentWindow, distanceDatas);
                    detailsDialog.show();
                }
            });

            final JPanel buttonPane = new JPanel(new BorderLayout());
            buttonPane.add(detailsButton, BorderLayout.EAST);

            final JPanel messagePane = new JPanel(new BorderLayout(0, 6));
            messagePane.add(new JLabel("Distance: " + distance + " +/- " + distanceError + " km"));
            messagePane.add(buttonPane, BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(), messagePane,
                                          org.esa.beam.visat.actions.rangefinder.RangeFinderTool.TITLE,
                                          JOptionPane.INFORMATION_MESSAGE);
            finish();
            _pointList.clear();
        }
    }

    private void handleDragAndMove(ToolInputEvent e) {
        if (_pointList.size() > 0) {
            _currPoint.setLocation(e.getPixelPos());
            repaint();
        }
    }

    private static ModalDialog createDetailsDialog(final Window parentWindow, final DistanceData[] dds) {
        float distance = 0;
        float distanceError = 0;

        final StringBuffer message = new StringBuffer();
        for (int i = 0; i < dds.length; i++) {
            final DistanceData dd = dds[i];
            distance += dd.distance;
            distanceError += dd.distanceError;
            message.append(
                        "Distance between points " + i + " to " + (i + 1) + " in pixels:\n" +
                        "XH[" + dd.xH + "] to XN[" + dd.xN + "]: " + Math.abs(dd.xH - dd.xN) + "\n" +
                        "YH[" + dd.yH + "] to YN[" + dd.yN + "]: " + Math.abs(dd.yH - dd.yN) + "\n" +
                        "\n" +
                        "LonH: " + dd.lonH + "   LatH: " + dd.latH + "\n" +
                        "LonN: " + dd.lonN + "   LatN: " + dd.latN + "\n" +
                        "\n" +
                        "LamH: " + dd.lamH + "   PhiH: " + dd.phiH + "\n" +
                        "LamN: " + dd.lamN + "   PhiN: " + dd.phiN + "\n" +
                        "\n" +
                        "Mean earth radius used: " + DistanceData.MEAN_EARTH_RADIUS_KM + " km" + "\n" +
                        "\n" +
                        "Distance: " + dd.distance + " +/- " + dd.distanceError + " km\n" +
                        "\n\n"           /*I18N*/
            );
        }
        message.insert(0, "Total distance: " + distance + " +/- " + distanceError + " km\n" +
                          "\n" +
                          "computed as described below:\n\n");

        final JScrollPane content = new JScrollPane(new JTextArea(message.toString()));
        content.setPreferredSize(new Dimension(300, 150));

        final ModalDialog detailsWindow = new ModalDialog(parentWindow,
                                                          org.esa.beam.visat.actions.rangefinder.RangeFinderTool.TITLE + " - Details",
                                                          /*I18N*/
                                                          ModalDialog.ID_OK,
                                                          null);
        detailsWindow.setContent(content);
        return detailsWindow;
    }

    private void repaint() {
        getDrawingEditor().repaintTool();
    }

    private Cursor createRangeFinderCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "rangeFinder";

        // this is necessary because on some systems the cursor is scaled but not the 'hot spot'
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(cursorIcon.getIconWidth(),
                                                                    cursorIcon.getIconHeight());
        Point hotSpot = new Point((7 * bestCursorSize.width) / cursorIcon.getIconWidth(),
                                  (7 * bestCursorSize.height) / cursorIcon.getIconHeight());

        return defaultToolkit.createCustomCursor(cursorIcon.getImage(), hotSpot, cursorName);
    }

    private static class DistanceData {

        final static float MIN_EARTH_RADIUS = (float) Ellipsoid.WGS_84.getSemiMinor();
        final static float MAX_EARTH_RADIUS = (float) Ellipsoid.WGS_84.getSemiMajor();
        final static float MEAN_EARTH_RADIUS_M = 6371000;
        final static float MEAN_EARTH_RADIUS_KM = MEAN_EARTH_RADIUS_M * 0.001f;
        final static float MEAN_ERROR_FACTOR = MIN_EARTH_RADIUS / MAX_EARTH_RADIUS;

        final int xH;
        final int yH;
        final int xN;
        final int yN;
        final float lonH;
        final float latH;
        final float lonN;
        final float latN;
        final float lamH;
        final float phiH;
        final float lamN;
        final float phiN;
        final float distance;
        final float distanceError;

        public DistanceData(GeoCoding geoCoding, final Point pH, final Point pN) {
            this.xH = pH.x;
            this.yH = pH.y;
            this.xN = pN.x;
            this.yN = pN.y;
            final GeoPos geoPosH = geoCoding.getGeoPos(new PixelPos(xH, yH), null);
            final GeoPos geoPosN = geoCoding.getGeoPos(new PixelPos(xN, yN), null);
            this.lonH = geoPosH.getLon();
            this.latH = geoPosH.getLat();
            this.lonN = geoPosN.getLon();
            this.latN = geoPosN.getLat();
            this.lamH = (float) (MathUtils.DTOR * lonH);
            this.phiH = (float) (MathUtils.DTOR * latH);
            this.lamN = (float) (MathUtils.DTOR * lonN);
            this.phiN = (float) (MathUtils.DTOR * latN);
            this.distance = (float) MathUtils.sphereDistance(MEAN_EARTH_RADIUS_KM, lamH, phiH, lamN, phiN);
            this.distanceError = distance * (1 - MEAN_ERROR_FACTOR);
        }
    }
}
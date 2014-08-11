/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.graphbuilder;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;
import org.esa.snap.gpf.ui.OperatorUI;
import org.esa.snap.gpf.ui.UIValidation;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node of the graph for the GraphBuilder
 * Stores, saves and loads the display position for the node
 * User: lveci
 * Date: Jan 17, 2008
 */
public class GraphNode {

    private final Node node;
    private final Map<String, Object> parameterMap = new HashMap<>(10);
    private OperatorUI operatorUI = null;

    private int nodeWidth = 60;
    private int nodeHeight = 25;
    private int halfNodeHeight = 0;
    private int halfNodeWidth = 0;
    private static final int hotSpotSize = 10;
    private static final int halfHotSpotSize = hotSpotSize / 2;
    private int hotSpotOffset = 0;

    private Point displayPosition = new Point(0, 0);

    private XppDom displayParameters;
    private static Color shadowColor = new Color(0, 0, 0, 64);

    GraphNode(final Node n) throws IllegalArgumentException {
        node = n;
        displayParameters = new XppDom("node");
        displayParameters.setAttribute("id", node.getId());

        initParameters();
    }

    public void setOperatorUI(final OperatorUI ui) {
        operatorUI = ui;
    }

    public OperatorUI GetOperatorUI() {
        return operatorUI;
    }

    private void initParameters() throws IllegalArgumentException {

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) return;

        final ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer valueContainer = PropertyContainer.createMapBacked(parameterMap,
                operatorSpi.getOperatorClass(), parameterDescriptorFactory);

        final DomElement config = node.getConfiguration();
        final int count = config.getChildCount();
        for (int i = 0; i < count; ++i) {
            final DomElement child = config.getChild(i);
            final String name = child.getName();
            final String value = child.getValue();
            if (name == null || value == null)
                continue;

            try {
                if (child.getChildCount() == 0) {
                    final Converter converter = getConverter(valueContainer, name);
                    if (converter == null) {
                        final String msg = "Graph parameter " + name + " not found for Operator " + operatorSpi.getOperatorAlias();
                        //throw new IllegalArgumentException(msg);
                        System.out.println(msg);
                    } else {
                        parameterMap.put(name, converter.parse(value));
                    }
                } else {
                    final Converter converter = getConverter(valueContainer, name);
                    final Object[] objArray = new Object[child.getChildCount()];
                    int c = 0;
                    for (DomElement ch : child.getChildren()) {
                        final String v = ch.getValue();

                        if (converter != null) {
                            objArray[c++] = converter.parse(v);
                        } else {
                            objArray[c++] = v;
                        }
                    }
                    parameterMap.put(name, objArray);
                }

            } catch (ConversionException e) {
                throw new IllegalArgumentException(name);
            }
        }
    }

    private static Converter getConverter(final PropertyContainer valueContainer, final String name) {
        final Property[] properties = valueContainer.getProperties();

        for (Property p : properties) {

            final PropertyDescriptor descriptor = p.getDescriptor();
            if (descriptor != null && (descriptor.getName().equals(name) ||
                    (descriptor.getAlias() != null && descriptor.getAlias().equals(name)))) {
                return descriptor.getConverter();
            }
        }
        return null;
    }

    void setDisplayParameters(final XppDom presentationXML) {
        for (XppDom params : presentationXML.getChildren()) {
            final String id = params.getAttribute("id");
            if (id != null && id.equals(node.getId())) {
                displayParameters = params;
                final XppDom dpElem = displayParameters.getChild("displayPosition");
                if (dpElem != null) {
                    displayPosition.x = (int) Float.parseFloat(dpElem.getAttribute("x"));
                    displayPosition.y = (int) Float.parseFloat(dpElem.getAttribute("y"));
                }
                return;
            }
        }
    }

    void updateParameters() throws GraphException {
        final XppDomElement config = new XppDomElement("parameters");
        updateParameterMap(config);
        node.setConfiguration(config);
    }

    void AssignParameters(final XppDom presentationXML) throws GraphException {

        updateParameters();
        AssignDisplayParameters(presentationXML);
    }

    void AssignDisplayParameters(final XppDom presentationXML) {
        XppDom nodeElem = null;
        for (XppDom elem : presentationXML.getChildren()) {
            final String id = elem.getAttribute("id");
            if (id != null && id.equals(node.getId())) {
                nodeElem = elem;
                break;
            }
        }
        if (nodeElem == null) {
            presentationXML.addChild(displayParameters);
        }

        XppDom dpElem = displayParameters.getChild("displayPosition");
        if (dpElem == null) {
            dpElem = new XppDom("displayPosition");
            displayParameters.addChild(dpElem);
        }

        dpElem.setAttribute("y", String.valueOf(displayPosition.getY()));
        dpElem.setAttribute("x", String.valueOf(displayPosition.getX()));
    }

    /**
     * Gets the display position of a node
     *
     * @return Point The position of the node
     */
    public Point getPos() {
        return displayPosition;
    }

    /**
     * Sets the display position of a node and writes it to the xml
     *
     * @param p The position of the node
     */
    public void setPos(Point p) {
        displayPosition = p;
    }

    public Node getNode() {
        return node;
    }

    public int getWidth() {
        return nodeWidth;
    }

    public int getHeight() {
        return nodeHeight;
    }

    public static int getHotSpotSize() {
        return hotSpotSize;
    }

    int getHalfNodeWidth() {
        return halfNodeWidth;
    }

    public int getHalfNodeHeight() {
        return halfNodeHeight;
    }

    private void setSize(final int width, final int height) {
        nodeWidth = width;
        nodeHeight = height;
        halfNodeHeight = nodeHeight / 2;
        halfNodeWidth = nodeWidth / 2;
        hotSpotOffset = halfNodeHeight - halfHotSpotSize;
    }

    public int getHotSpotOffset() {
        return hotSpotOffset;
    }

    /**
     * Gets the uniqe node identifier.
     *
     * @return the identifier
     */
    public String getID() {
        return node.getId();
    }

    /**
     * Gets the name of the operator.
     *
     * @return the name of the operator.
     */
    public String getOperatorName() {
        return node.getOperatorName();
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void connectOperatorSource(final String id) {
        // check if already a source for this node
        disconnectOperatorSources(id);

        final NodeSource ns = new NodeSource("sourceProduct", id);
        node.addSource(ns);
    }

    void disconnectOperatorSources(final String id) {

        for (NodeSource ns : node.getSources()) {
            if (ns.getSourceNodeId().equals(id)) {
                node.removeSource(ns);
            }
        }
    }

    void disconnectAllSources() {
        final NodeSource[] sources = node.getSources();
        for (NodeSource source : sources) {
            node.removeSource(source);
        }
    }

    boolean isNodeSource(final GraphNode source) {

        for (NodeSource ns : node.getSources()) {
            if (ns.getSourceNodeId().equals(source.getID())) {
                return true;
            }
        }
        return false;
    }

    boolean HasSources() {
        return node.getSources().length > 0;
    }

    public UIValidation validateParameterMap() {
        if (operatorUI != null)
            return operatorUI.validateParameters();
        return new UIValidation(UIValidation.State.OK, "");
    }

    void setSourceProducts(final Product[] products) {
        if (operatorUI != null) {
            operatorUI.setSourceProducts(products);
        }
    }

    void updateParameterMap(final XppDomElement parentElement) throws GraphException {
        if (operatorUI != null) {
            try {
                //if(operatorUI.hasSourceProducts())
                operatorUI.updateParameters();
                operatorUI.convertToDOM(parentElement);
            } catch (Exception e) {
                throw new GraphException(operatorUI.getOperatorName() + " error setting parameter " + e.getMessage());
            }
        }
    }

    /**
     * Draw a GraphNode as a rectangle with a name
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    public void drawNode(final Graphics2D g, final Color col) {
        final int x = displayPosition.x;
        final int y = displayPosition.y;

        g.setFont(g.getFont().deriveFont(Font.BOLD, 11));
        final FontMetrics metrics = g.getFontMetrics();
        final String name = node.getId();
        final Rectangle2D rect = metrics.getStringBounds(name, g);
        final int stringWidth = (int) rect.getWidth();
        setSize(Math.max(stringWidth, 50) + 10, 25);

        int step = 4;
        int alpha = 96;
        for (int i = 0; i < step; ++i) {
            g.setColor(new Color(0, 0, 0, alpha - (32 * i)));
            g.drawLine(x + i + 1, y + nodeHeight + i, x + nodeWidth + i - 1, y + nodeHeight + i);
            g.drawLine(x + nodeWidth + i, y + i, x + nodeWidth + i, y + nodeHeight + i);
        }

        Shape clipShape = new Rectangle(x, y, nodeWidth, nodeHeight);

        g.setComposite(AlphaComposite.SrcAtop);
        g.setPaint(new GradientPaint(x, y, col, x + nodeWidth, y + nodeHeight, col.darker()));
        g.fill(clipShape);

        g.setColor(Color.blue);
        g.draw3DRect(x, y, nodeWidth - 1, nodeHeight - 1, true);

        g.setColor(Color.BLACK);
        g.drawString(name, x + (nodeWidth - stringWidth) / 2, y + 15);
    }

    /**
     * Draws the hotspot where the user can join the node to a source node
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    public void drawHeadHotspot(final Graphics g, final Color col) {
        final Point p = displayPosition;
        g.setColor(col);
        g.drawOval(p.x - halfHotSpotSize, p.y + hotSpotOffset, hotSpotSize, hotSpotSize);
    }

    /**
     * Draws the hotspot where the user can join the node to a source node
     *
     * @param g   The Java2D Graphics
     * @param col The color to draw
     */
    public void drawTailHotspot(final Graphics g, final Color col) {
        final Point p = displayPosition;
        g.setColor(col);

        final int x = p.x + nodeWidth;
        final int y = p.y + halfNodeHeight;
        final int[] xpoints = {x, x + hotSpotOffset, x, x};
        final int[] ypoints = {y - halfHotSpotSize, y, y + halfHotSpotSize, y - halfHotSpotSize};
        g.fillPolygon(xpoints, ypoints, xpoints.length);
    }

    /**
     * Draw a line between source and target nodes
     *
     * @param g   The Java2D Graphics
     * @param src the source GraphNode
     */
    public void drawConnectionLine(final Graphics2D g, final GraphNode src) {

        final Point nodePos = displayPosition;
        final Point srcPos = src.displayPosition;

        final int nodeEndX = nodePos.x + nodeWidth;
        final int nodeMidY = nodePos.y + halfNodeHeight;
        final int srcEndX = srcPos.x + src.getWidth();
        final int srcMidY = srcPos.y + src.getHalfNodeHeight();

        if (srcEndX <= nodePos.x) {
            if (srcPos.y > nodePos.y + nodeHeight) {
                // to UR
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y + nodeHeight,
                        srcEndX, srcMidY);
            } else if (srcPos.y + src.getHeight() < nodePos.y) {
                // to DR
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y,
                        srcEndX, srcMidY);
            } else {
                // to R
                drawArrow(g, nodePos.x, nodeMidY,
                        srcEndX, srcMidY);
            }
        } else if (srcPos.x >= nodeEndX) {
            if (srcPos.y > nodePos.y + nodeHeight) {
                // to UL
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y + nodeHeight,
                        srcPos.x, srcPos.y + halfNodeHeight);
            } else if (srcPos.y + src.getHeight() < nodePos.y) {
                // to DL
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y,
                        srcPos.x, srcPos.y + halfNodeHeight);
            } else {
                // to L
                drawArrow(g, nodeEndX, nodeMidY,
                        srcPos.x, srcPos.y + halfNodeHeight);
            }
        } else {
            if (srcPos.y > nodePos.y + nodeHeight) {
                // U
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y + nodeHeight,
                        srcPos.x + src.getHalfNodeWidth(), srcPos.y);
            } else {
                // D
                drawArrow(g, nodePos.x + halfNodeWidth, nodePos.y,
                        srcPos.x + src.getHalfNodeWidth(), srcPos.y + src.getHeight());
            }
        }
    }

    /**
     * Draws an arrow head at the correct angle
     *
     * @param g     The Java2D Graphics
     * @param tailX position X on target node
     * @param tailY position Y on target node
     * @param headX position X on source node
     * @param headY position Y on source node
     */
    private static void drawArrow(final Graphics2D g, final int tailX, final int tailY, final int headX, final int headY) {

        final double t1 = Math.abs(headY - tailY);
        final double t2 = Math.abs(headX - tailX);
        double theta = Math.atan(t1 / t2);
        if (headX >= tailX) {
            if (headY > tailY)
                theta = Math.PI + theta;
            else
                theta = -(Math.PI + theta);
        } else if (headX < tailX && headY > tailY)
            theta = 2 * Math.PI - theta;
        final double cosTheta = Math.cos(theta);
        final double sinTheta = Math.sin(theta);

        final Point p2 = new Point(-8, -3);
        final Point p3 = new Point(-8, +3);

        int x = (int) Math.round((cosTheta * p2.x) - (sinTheta * p2.y));
        p2.y = (int) Math.round((sinTheta * p2.x) + (cosTheta * p2.y));
        p2.x = x;
        x = (int) Math.round((cosTheta * p3.x) - (sinTheta * p3.y));
        p3.y = (int) Math.round((sinTheta * p3.x) + (cosTheta * p3.y));
        p3.x = x;

        p2.translate(tailX, tailY);
        p3.translate(tailX, tailY);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.drawLine(tailX, tailY, headX, headY);
        g.drawLine(tailX, tailY, p2.x, p2.y);
        g.drawLine(p3.x, p3.y, tailX, tailY);
        g.setStroke(oldStroke);
    }

}

/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.nestwwview;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.examples.util.LayerManagerLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.ScreenAnnotation;

import java.awt.*;

/**
 * Displays the layer list in a viewport corner.
 *
 */
public class LayerPanelLayer extends LayerManagerLayer
{
    private Layer virtualEarthAerialLayer = null;
    private Layer virtualEarthRoadsLayer = null;
    private Layer virtualEarthHybridLayer = null;

    public LayerPanelLayer(WorldWindow wwd)
    {
        super(wwd);
    }

    private LayerList getValidLayers() {
        final LayerList validLayers = new LayerList();
        final LayerList allLayers = wwd.getModel().getLayers();
        for(Layer l : allLayers) {
            if(l.getName().equalsIgnoreCase("Atmosphere") || l.getName().equalsIgnoreCase("World Map") ||
               l.getName().equalsIgnoreCase("Scale bar") || l.getName().equalsIgnoreCase("Compass") ||
               l.getName().equalsIgnoreCase("NASA Blue Marble Image"))
                continue;
            if(l.getName().equalsIgnoreCase("MS Bing Aerial"))
                virtualEarthAerialLayer = l;
            else if(l.getName().equalsIgnoreCase("MS Bing Roads"))
                virtualEarthRoadsLayer = l;
            else if(l.getName().equalsIgnoreCase("MS Bing Hybrid"))
                virtualEarthHybridLayer = l;

            validLayers.add(l);
        }
        return validLayers;
    }

    /**
     * <code>SelectListener</code> implementation.
     *
     * @param event the current <code>SelectEvent</code>
     */
    @Override
    public void selected(SelectEvent event)
    {
        final ScreenAnnotation annotation = getAnnotation();
        if (event.hasObjects() && event.getTopObject() == annotation)
        {
            boolean update = false;
            if (event.getEventAction().equals(SelectEvent.ROLLOVER)
                || event.getEventAction().equals(SelectEvent.LEFT_CLICK))
            {
                // Highlight annotation
                if (!annotation.getAttributes().isHighlighted())
                {
                    annotation.getAttributes().setHighlighted(true);
                    update = true;
                }
                // Check for text or url
                final PickedObject po = event.getTopPickedObject();
                if(po.getValue(AVKey.URL) != null)
                {
                    // Set cursor hand on hyperlinks
                    ((Component)this.wwd).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    int i = Integer.parseInt((String)po.getValue(AVKey.URL));
                    // Select current hyperlink
                    if (getSelectedIndex() != i)
                    {
                        setSelectedIndex(i);
                        update = true;
                    }
                    // Enable/disable layer on left click
                    if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
                    {
                        final LayerList layers = getValidLayers();
                        if (i >= 0 && i < layers.size())
                        {
                            final Layer layer = layers.get(i);
                            final boolean enable = !layer.isEnabled();
                            layer.setEnabled(enable);
                            updateVirtualEarthLayers(layer, enable);
                            update = true;
                        }
                    }
                } else {
                    // Unselect if not on an hyperlink
                    if (getSelectedIndex() != -1)
                    {
                        setSelectedIndex(-1);
                        update = true;
                    }
                    // Set cursor
                    if (this.isComponentDragEnabled())
                        ((Component)this.wwd).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    else
                        ((Component)this.wwd).setCursor(Cursor.getDefaultCursor());
                }
            }
            if (event.getEventAction().equals(SelectEvent.DRAG)
                || event.getEventAction().equals(SelectEvent.DRAG_END))
            {
                // Handle dragging
                if (this.isComponentDragEnabled() || this.isLayerDragEnabled())
                {
                    final boolean wasDraggingLayer = this.draggingLayer;
                    this.drag(event);
                    // Update list if dragging a layer, otherwise just redraw the world window
                    if(this.draggingLayer || wasDraggingLayer)
                        update = true;
                    else
                        this.wwd.redraw();
                }
            }
            // Redraw annotation if needed
            if (update)
                this.update();
        } else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && annotation.getAttributes().isHighlighted())
        {
            // de-highlight annotation
            annotation.getAttributes().setHighlighted(false);
            ((Component)this.wwd).setCursor(Cursor.getDefaultCursor());
            this.update();
        }
    }

    private void updateVirtualEarthLayers(Layer layer, boolean enable) {
        if (enable && (layer == virtualEarthAerialLayer ||
                       layer == virtualEarthRoadsLayer ||
                       layer == virtualEarthHybridLayer)) {
            virtualEarthAerialLayer.setEnabled(layer == virtualEarthAerialLayer);
            virtualEarthRoadsLayer.setEnabled(layer == virtualEarthRoadsLayer);
            virtualEarthHybridLayer.setEnabled(layer == virtualEarthHybridLayer);
        }
    }

    @Override
    protected void drag(SelectEvent event)
    {
        if (event.getEventAction().equals(SelectEvent.DRAG))
        {
            if ((this.isComponentDragEnabled() && getSelectedIndex() == -1 && this.dragRefIndex == -1)
                || this.draggingComponent)
            {
                // Dragging the whole list
                if (!this.draggingComponent)
                {
                    this.dragRefCursorPoint = event.getMouseEvent().getPoint();
                    this.dragRefPoint = getAnnotation().getScreenPoint();
                    this.draggingComponent = true;
                }
                final Point cursorOffset = new Point(event.getMouseEvent().getPoint().x - this.dragRefCursorPoint.x,
                    event.getMouseEvent().getPoint().y - this.dragRefCursorPoint.y);
                final Point targetPoint = new Point(this.dragRefPoint.x + cursorOffset.x,
                    this.dragRefPoint.y - cursorOffset.y);
                this.moveTo(targetPoint);
            } else if (this.isLayerDragEnabled()) {
                // Dragging a layer inside the list
                if (!this.draggingLayer)
                {
                    this.dragRefIndex = getSelectedIndex();
                    this.draggingLayer = true;
                }
                if (getSelectedIndex() != -1 && this.dragRefIndex != -1 && this.dragRefIndex != getSelectedIndex())
                {
                    // Move dragged layer
                    final LayerList layers = getValidLayers();
                    final int insertIndex = this.dragRefIndex > getSelectedIndex() ?
                        getSelectedIndex() : getSelectedIndex() + 1;
                    final int removeIndex = this.dragRefIndex > getSelectedIndex() ?
                        this.dragRefIndex + 1 : this.dragRefIndex;
                    layers.add(insertIndex, layers.get(this.dragRefIndex));
                    layers.remove(removeIndex);
                    this.dragRefIndex = getSelectedIndex();
                }
            }
        } else if (event.getEventAction().equals(SelectEvent.DRAG_END)) {
            this.draggingComponent = false;
            this.draggingLayer = false;
            this.dragRefIndex = -1;
        }
    }

    /**
     * Compose the annotation text from the given <code>LayerList</code>.
     *
     * @param layers the <code>LayerList</code> to draw names from.
     * @return the annotation text to be displayed.
     */
    @Override
    protected String makeAnnotationText(LayerList layers)
    {
        // Compose html text
        final StringBuilder text = new StringBuilder(255);
        Color color;
        int i = 0;
        final LayerList validLayers = getValidLayers();
        for (Layer layer : validLayers)
        {
            if (!this.isMinimized() || layer == this)
            {
                color = (i == getSelectedIndex()) ? getHighlightColor() : getColor();
                color = (i == this.dragRefIndex) ? dragColor : color;
                text.append("<a href=\"");
                text.append(i);
                text.append("\"><font color=\"");
                text.append(encodeHTMLColor(color));
                text.append("\">");
                text.append((layer.isEnabled() ? getLayerEnabledSymbol() : getLayerDisabledSymbol()));
                text.append(' ');
                text.append((layer.isEnabled() ? "<b>" : "<i>"));
                text.append(layer.getName());
                text.append((layer.isEnabled() ? "</b>" : "</i>"));
                text.append((layer.isMultiResolution() && layer.isAtMaxResolution() ? "*" : ""));
                text .append("</a><br />");
            }
            i++;
        }
        return text.toString();
    }
}

/*
 * $Id: PinTool.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.visat.VisatApp;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

/**
 * A tool used to create (single click), select (single click on a pin) or edit (double click on a pin) the pins
 * displayed in product scene view.
 */
public class PinTool extends AbstractTool {

    private boolean _draw;
    private ExecCommand _pinOverlayCommand;
    public static final String CMD_ID_SHOW_PIN_OVERLAY = "showPinOverlay";

    public PinTool() {
    }

    /**
     * Gets a thing that can be drawn while the tool is working.
     *
     * @return always <code>null</code>
     */
    public Drawable getDrawable() {
        return null;
    }

    public void mousePressed(ToolInputEvent e) {
        activatePinOverlay();
        _draw = true;
        if (isPinOverlayActive()) {
            selectPin(e);
        }
    }

    public void mouseReleased(ToolInputEvent e) {
        _draw = false;
    }

    public void mouseClicked(ToolInputEvent e) {
        _draw = false;
        if (isDoubleLeftClick(e)) {
            if (isPinOverlayActive()) {
                editPin(e);
            }
        } else if (isSingleLeftClick(e)) {
            setPin(e);
        } else if (e.getMouseEvent().isPopupTrigger()) {
            showPopupMenu(e);
        }
    }

    public Cursor getCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "pinCursor";
        ImageIcon icon = UIUtils.loadImageIcon("cursors/pin.gif");

        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((7 * bestCursorSize.width) / icon.getIconWidth(),
                                  (7 * bestCursorSize.height) / icon.getIconHeight());

        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

    private void setPin(ToolInputEvent e) {
        if (!e.isPixelPosValid()) {
            return;
        }
        ProductSceneView view = getProductSceneView();
        if (view != null) {
            Product product = view.getProduct();
            showPinOverlayOn();
            if (product.getPinForPixelPos(e.getPixelX(), e.getPixelY()) != null) {
                return;
            }
            final GeoPos geoPos = PinManagerToolView.getGeoPos(product,
                                                               new PixelPos(0.5f + e.getPixelX(),
                                                                            0.5f + e.getPixelY()));
            if (geoPos != null) {
                final String[] uniquePinNameAndLabel = PinManagerToolView.createUniquePinNameAndLabel(product,
                                                                                                      e.getPixelX(),
                                                                                                      e.getPixelY());
                final String name = uniquePinNameAndLabel[0];
                final String label = uniquePinNameAndLabel[1];
                final Pin pin = new Pin(name, label, null, geoPos.getLat(), geoPos.getLon(), null);
                product.addPin(pin);
                updateState();
            }
        }
    }

    private void showPinOverlayOn() {
        if (!isPinOverlayActive()) {
            if (_pinOverlayCommand != null) {
                _pinOverlayCommand.execute();
            }
        }
    }

    private void ensurePinOverlayCommand() {
        if (_pinOverlayCommand == null) {
            _pinOverlayCommand = getPinOverlayCommand();
        }
    }

    private boolean isPinOverlayActive() {
        ensurePinOverlayCommand();
        return _pinOverlayCommand != null && _pinOverlayCommand.isSelected();
    }

    private ExecCommand getPinOverlayCommand() {
        Command command = VisatApp.getApp().getCommandManager().getCommand(CMD_ID_SHOW_PIN_OVERLAY);
        if (command != null) {
            if (command instanceof ExecCommand) {
                return (ExecCommand) command;
            }
        }
        return null;
    }

    private void editPin(ToolInputEvent e) {
//      Mouseclic outside the product boundaries must be able to detect the clicked pin.
//        if (!e.isPixelPosValid()) {
//            return;
//        }
        ProductSceneView view = getProductSceneView();
        if (view != null) {
            final Product product = view.getProduct();
            Pin pin = product.getPinForPixelPos(e.getPixelX(), e.getPixelY());
            if (pin != null) {
                Pin newPin = PinManagerToolView.editPin(pin, product, VisatApp.getApp().getMainFrame());
                if (newPin != null) {
                    product.replacePin(pin, newPin);
                    updateState();
                }
            }
        }
    }

    private void showPopupMenu(ToolInputEvent e) {
        //@todo 1 he/nf - add Popupmenu here
        JPopupMenu popup = new JPopupMenu();
        VisatApp.getApp().getCommandUIFactory().addContextDependentMenuItems("pin", popup);
        UIUtils.showPopup(popup, e.getMouseEvent());
    }

    private ProductSceneView getProductSceneView() {
        DrawingEditor drawingEditor = getDrawingEditor();
        if (drawingEditor instanceof ProductSceneView) {
            return (ProductSceneView) drawingEditor;
        }
        return null;
    }

    private void selectPin(ToolInputEvent e) {
        ProductSceneView productSceneView = getProductSceneView();
        Product product = productSceneView.getProduct();
        Pin clickedPin = product.getPinForPixelPos(e.getPixelX(), e.getPixelY());
        if (clickedPin != null) {
            product.setSelectedPin(clickedPin.getName());
            updateState();
        }
    }

    private void updateState() {
        VisatApp.getApp().updateState();
    }

    private void activatePinOverlay() {
        ensurePinOverlayCommand();
        _pinOverlayCommand.setSelected(true);
        _pinOverlayCommand.execute();
    }
}

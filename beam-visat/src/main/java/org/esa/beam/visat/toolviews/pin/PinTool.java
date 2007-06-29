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

import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinSymbol;
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
import java.awt.*;

/**
 * A tool used to create (single click), select (single click on a pin) or edit (double click on a pin) the pins
 * displayed in product scene view.
 */
public class PinTool extends AbstractTool {

    public static final String CMD_ID_SHOW_PIN_OVERLAY = "showPinOverlay";

    public PinTool() {
    }

    /**
     * Gets a thing that can be drawn while the tool is working.
     *
     * @return always <code>null</code>
     */
    @Override
    public Drawable getDrawable() {
        return null;
    }

    @Override
    public void mouseClicked(ToolInputEvent e) {
        activatePinOverlay();
        if (isSingleLeftClick(e)) {
            selectOrInsertPin(e);
        } else if (isDoubleLeftClick(e)) {
            selectAndEditPin(e);
        } else if (e.getMouseEvent().isPopupTrigger()) {
            showPopupMenu(e);
        }
    }

    @Override
    public Cursor getCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "pinCursor";
        ImageIcon icon = UIUtils.loadImageIcon("cursors/pin.gif");

        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((7 * bestCursorSize.width) / icon.getIconWidth(),
                                  (7 * bestCursorSize.height) / icon.getIconHeight());

        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

    private void selectOrInsertPin(ToolInputEvent e) {
        if (!e.isPixelPosValid()) {
            return;
        }
        ProductSceneView view = getProductSceneView();
        if (view == null) {
            return;
        }
        Product product = view.getProduct();
        Pin clickedPin = getPinForPixelPos(e.getPixelX(), e.getPixelY());
        if (clickedPin != null) {
            setPinSelected(product, clickedPin, false);
        } else {
            final String[] uniquePinNameAndLabel = PinManagerToolView.createUniquePinNameAndLabel(product,
                                                                                                  e.getPixelX(),
                                                                                                  e.getPixelY());
            final String name = uniquePinNameAndLabel[0];
            final String label = uniquePinNameAndLabel[1];
            final Pin newPin = new Pin(name, label, new PixelPos(0.5f + e.getPixelX(),
                                                                 0.5f + e.getPixelY()));
            product.addPin(newPin);
            setPinSelected(product, newPin, true);
        }
    }

    private void selectAndEditPin(ToolInputEvent e) {
        if (!e.isPixelPosValid()) {
            return;
        }
        ProductSceneView view = getProductSceneView();
        if (view == null) {
            return;
        }
        final Product product = view.getProduct();
        int pixelX = e.getPixelX();
        int pixelY = e.getPixelY();
        Pin clickedPin = getPinForPixelPos(pixelX, pixelY);
        if (clickedPin != null) {
            setPinSelected(product, clickedPin, true);
            boolean ok = PinManagerToolView.showEditPinDialog(VisatApp.getApp().getMainFrame(), product, clickedPin);
            if (ok) {
                updateState();
            }
        }
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

    public static void setPinSelected(Product product, Pin clickedPin, boolean forceSelection) {
        boolean select = true;
        Pin[] selectedPins = product.getSelectedPins();
        for (Pin pin : selectedPins) {
            if (pin.isSelected()) {
                if (pin == clickedPin) {
                    select = false;
                }
                pin.setSelected(false);
            }
        }
        if (forceSelection || select) {
            clickedPin.setSelected(true);
        }
        VisatApp.getApp().updateState();
    }

    private void updateState() {
        VisatApp.getApp().updateState();
    }

    private void activatePinOverlay() {
        ExecCommand pinOverlayCommand = getPinOverlayCommand();
        if (pinOverlayCommand != null) {
            pinOverlayCommand.setSelected(true);
            pinOverlayCommand.execute();
        }
    }


    public Pin getPinForPixelPos(final float pixelX, final float pixelY) {
        ProductSceneView productSceneView = getProductSceneView();
        ViewModel viewModel = productSceneView.getImageDisplay().getViewModel();
        double viewScale = viewModel.getViewScale();
        double epsilon = 4.0; // view units!
        final Pin[] pins = productSceneView.getProduct().getPins();
        for (final Pin pin : pins) {
            PixelPos pixelPos = pin.getPixelPos();
            double dx = viewScale * (pixelX - pixelPos.getX()); // view units!
            double dy = viewScale * (pixelY - pixelPos.getY()); // view units!
            if (dx * dx + dy * dy < epsilon * epsilon) {
                return pin;
            }
        }

        for (final Pin pin : pins) {
            PinSymbol symbol = pin.getSymbol();
            PixelPos pixelPos = pin.getPixelPos();
            double dx = viewScale * (pixelX - pixelPos.getX()); // view units!
            double dy = viewScale * (pixelY - pixelPos.getY()); // view units!
            PixelPos refPoint = symbol.getRefPoint();
            if (refPoint != null) {
                dx += refPoint.getX();
                dy += refPoint.getY();
            }
            Shape shape = symbol.getShape();
            if (shape.contains(dx, dy)) {
                return pin;
            }
        }
        return null;
    }
}

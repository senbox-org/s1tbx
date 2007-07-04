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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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


    public Pin getPinForPixelPos(final int selPixelX, final int selPixelY) {
        ProductSceneView productSceneView = getProductSceneView();
        ViewModel viewModel = productSceneView.getImageDisplay().getViewModel();

        double pixelX0 = viewModel.getModelOffsetX();
        double pixelY0 = viewModel.getModelOffsetY();
        double viewScale = viewModel.getViewScale();
        double selViewX = (selPixelX - pixelX0) * viewScale;
        double selViewY = (selPixelY - pixelY0) * viewScale;
        Rectangle2D.Double selViewRect = new Rectangle2D.Double(selViewX, selViewY, viewScale, viewScale);

        // Compare with pin insertion points (which are in product raster pixel coordinates)
        //
        double epsilon = 4.0; // view units!
        Pin[] pins = productSceneView.getProduct().getPins();
        for (final Pin pin : pins) {
            // Convert pin pixel to view coordinates
            PixelPos pinPixelPos = pin.getPixelPos();
            double pinViewX = (pinPixelPos.getX() - pixelX0) * viewScale;
            double pinViewY = (pinPixelPos.getY() - pixelY0) * viewScale;
            // Use a rectangular region around the insertion point for comparision
            final Rectangle2D.Double pinViewRect = new Rectangle2D.Double(pinViewX - 0.5 * epsilon,
                                                                          pinViewY - 0.5 * epsilon,
                                                                          epsilon, epsilon);
            if (pinViewRect.intersects(selViewRect)) {
                return pin;
            }
        }

        // Now compare against pin symbols (which are in view coordinates).
        // Depending on the symbol used, this may be more or less expensive.
        //
        for (final Pin pin : pins) {
            PinSymbol symbol = pin.getSymbol();

            // Convert pin pixel to view coordinates
            PixelPos pinPixelPos = pin.getPixelPos();
            double pinViewX = (pinPixelPos.getX() - pixelX0) * viewScale;
            double pinViewY = (pinPixelPos.getY() - pixelY0) * viewScale;
            // Use the pixel dimension in view units (=viewScale) relative to the pin insertion point
            final Rectangle2D.Double relViewRect = new Rectangle2D.Double(selViewX - pinViewX,
                                                 selViewY - pinViewY,
                                                 viewScale, viewScale);
            // Move using symbol insertion point.
            PixelPos refPoint = symbol.getRefPoint();
            if (refPoint != null) {
                relViewRect.x +=  refPoint.getX();
                relViewRect.y +=  refPoint.getY();
            }

            if (symbol.getShape().intersects(relViewRect)) {
                return pin;
            }
        }
        return null;
    }
}

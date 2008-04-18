package org.esa.beam.visat.toolviews.pin;

import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.visat.VisatApp;

import javax.swing.JPopupMenu;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public abstract class PlacemarkTool extends AbstractTool {

    private final PlacemarkDescriptor placemarkDescriptor;
    private Pin draggedPlacemark;
    private Cursor cursor;

    protected PlacemarkTool(PlacemarkDescriptor placemarkDescriptor) {
        this.placemarkDescriptor = placemarkDescriptor;
        cursor =  createCursor(placemarkDescriptor);
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
        activateOverlay();
        if (isSingleLeftClick(e)) {
            selectOrInsertPlacemark(e);
        } else if (isDoubleLeftClick(e)) {
            selectAndEditPlacemark(e);
        } else if (e.getMouseEvent().isPopupTrigger()) {
            showPopupMenu(e);
        }
    }

    @Override
    public void mousePressed(ToolInputEvent e) {
        Pin draggedPlacemark = getPlacemarkForPixelPos(e.getPixelX(), e.getPixelY());
        setDraggedPlacemark(draggedPlacemark);
    }


    @Override
    public void mouseDragged(ToolInputEvent e) {
        if (getDraggedPlacemark() != null && e.isPixelPosValid()) {
            PixelPos pixelPos = new PixelPos((float) e.getPixelPos().getX() + 0.5f,
                                             (float) e.getPixelPos().getY() + 0.5f);
            if (pixelPos.isValid()) {
                getDraggedPlacemark().setPixelPos(pixelPos);
            }
        }
    }

    private void selectOrInsertPlacemark(ToolInputEvent e) {
        if (!e.isPixelPosValid()) {
            return;
        }
        ProductSceneView view = getProductSceneView();
        if (view == null) {
            return;
        }
        Product product = view.getProduct();
        Pin clickedPin = getPlacemarkForPixelPos(e.getPixelX(), e.getPixelY());
        if (clickedPin != null) {
            setPlacemarkSelected(getPlacemarkGroup(product), clickedPin, false);
        } else {
            final String[] uniqueNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor,
                                                                                              product);
            final String name = uniqueNameAndLabel[0];
            final String label = uniqueNameAndLabel[1];
            final Pin newPlacemark = new Pin(name, label, "",
                                             new PixelPos(0.5f + e.getPixelX(), 0.5f + e.getPixelY()),
                                             null,
                                             placemarkDescriptor.createDefaultSymbol());
            getPlacemarkGroup(product).add(newPlacemark);
        }
    }

    private void selectAndEditPlacemark(ToolInputEvent e) {
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
        Pin clickedPlacemark = getPlacemarkForPixelPos(pixelX, pixelY);
        if (clickedPlacemark != null) {
            setPlacemarkSelected(getPlacemarkGroup(product), clickedPlacemark, true);
            boolean ok = PinDialog.showEditPinDialog(VisatApp.getApp().getMainFrame(), product,
                                                     clickedPlacemark, placemarkDescriptor);
            if (ok) {
                updateState();
            }
        }
    }

    private ExecCommand getShowOverlayCommand() {
        Command command = VisatApp.getApp().getCommandManager().getCommand(getShowOverlayCommandId());
        if (command != null) {
            if (command instanceof ExecCommand) {
                return (ExecCommand) command;
            }
        }
        return null;
    }

    protected String getShowOverlayCommandId() {
        return placemarkDescriptor.getShowLayerCommandId();
    }

    protected ProductNodeGroup<Pin> getPlacemarkGroup(Product product) {
        return placemarkDescriptor.getPlacemarkGroup(product);
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    private static Cursor createCursor(PlacemarkDescriptor placemarkDescriptor) {
        final Image cursorImage = placemarkDescriptor.getCursorImage();
        if (cursorImage == null) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,
                                                              placemarkDescriptor.getCursorHotSpot(),
                                                              placemarkDescriptor.getRoleName());
    }

    protected void showPopupMenu(ToolInputEvent e) {
        JPopupMenu popup = new JPopupMenu();
        VisatApp.getApp().getCommandUIFactory().addContextDependentMenuItems("pin", popup);
        UIUtils.showPopup(popup, e.getMouseEvent());
    }

    protected ProductSceneView getProductSceneView() {
        DrawingEditor drawingEditor = getDrawingEditor();
        if (drawingEditor instanceof ProductSceneView) {
            return (ProductSceneView) drawingEditor;
        }
        return null;
    }

    public static void setPlacemarkSelected(ProductNodeGroup<Pin> placemarkGroup, Pin clickedPlacemark,
                                            boolean forceSelection) {
        boolean select = true;
        Collection<Pin> selectedPlacemark = placemarkGroup.getSelectedNodes();
        for (Pin placemark : selectedPlacemark) {
            if (placemark.isSelected()) {
                if (placemark == clickedPlacemark) {
                    select = false;
                }
                placemark.setSelected(false);
            }
        }
        if (forceSelection || select) {
            clickedPlacemark.setSelected(true);
        }
        updateState();
    }

    private static void updateState() {
        VisatApp.getApp().updateState();
    }

    private void activateOverlay() {
        ExecCommand overlayCommand = getShowOverlayCommand();
        if (overlayCommand != null) {
            overlayCommand.setSelected(true);
            overlayCommand.execute();
        }
    }

    private Pin getPlacemarkForPixelPos(final int selPixelX, final int selPixelY) {
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
        ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup(getProductSceneView().getProduct());
        Pin[] placemarks = placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]);
        for (final Pin placemark : placemarks) {
            // Convert pin pixel to view coordinates
            PixelPos placemarkPixelPos = placemark.getPixelPos();
            double placemarkViewX = (placemarkPixelPos.getX() - pixelX0) * viewScale;
            double placemarkViewY = (placemarkPixelPos.getY() - pixelY0) * viewScale;
            // Use a rectangular region around the insertion point for comparision
            final Rectangle2D.Double pinViewRect = new Rectangle2D.Double(placemarkViewX - 0.5 * epsilon,
                                                                          placemarkViewY - 0.5 * epsilon,
                                                                          epsilon, epsilon);
            if (pinViewRect.intersects(selViewRect)) {
                return placemark;
            }
        }

        // Now compare against pin symbols (which are in view coordinates).
        // Depending on the symbol used, this may be more or less expensive.
        //
        for (final Pin placemark : placemarks) {
            PinSymbol symbol = placemark.getSymbol();

            // Convert pin pixel to view coordinates
            PixelPos pinPixelPos = placemark.getPixelPos();
            double pinViewX = (pinPixelPos.getX() - pixelX0) * viewScale;
            double pinViewY = (pinPixelPos.getY() - pixelY0) * viewScale;
            // Use the pixel dimension in view units (=viewScale) relative to the pin insertion point
            final Rectangle2D.Double relViewRect = new Rectangle2D.Double(selViewX - pinViewX,
                                                                          selViewY - pinViewY,
                                                                          viewScale, viewScale);
            // Move using symbol insertion point.
            PixelPos refPoint = symbol.getRefPoint();
            if (refPoint != null) {
                relViewRect.x += refPoint.getX();
                relViewRect.y += refPoint.getY();
            }

            if (symbol.getShape().intersects(relViewRect)) {
                return placemark;
            }
        }
        return null;
    }

    protected Pin getDraggedPlacemark() {
        return draggedPlacemark;
    }

    protected void setDraggedPlacemark(Pin draggedPlacemark) {
        this.draggedPlacemark = draggedPlacemark;
    }
}

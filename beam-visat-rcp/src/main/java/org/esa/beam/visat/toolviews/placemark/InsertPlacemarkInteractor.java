package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.swing.figure.FigureEditorInteractor;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JPopupMenu;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;


public abstract class InsertPlacemarkInteractor extends FigureEditorInteractor {

    private final PlacemarkDescriptor placemarkDescriptor;
    private Pin selectedPlacemark;
    private Cursor cursor;
    private boolean started;

    protected InsertPlacemarkInteractor(PlacemarkDescriptor placemarkDescriptor) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.cursor = createCursor(placemarkDescriptor);
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        started = false;
        System.out.println("InsertPlacemarkInteractor.mousePressed: event = " + event);
        ProductSceneView sceneView = getProductSceneView(event);
        if (sceneView != null) {
            started = startInteraction(event);
            if (started) {
                Pin selectedPlacemark = getPlacemarkForPixelPos(event, sceneView.getCurrentPixelX(), sceneView.getCurrentPixelY());
                setSelectedPlacemark(selectedPlacemark);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (started) {
            ProductSceneView sceneView = getProductSceneView(event);
            if (sceneView != null) {
                completeInteraction(sceneView);
                stopInteraction(event);
                setSelectedPlacemark(null);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (started) {
            System.out.println("InsertPlacemarkInteractor.mouseClicked: event = " + event);
            activateOverlay();
            if (isSingleButton1Click(event)) {
                selectOrInsertPlacemark(event);
            } else if (isMultiButton1Click(event)) {
                selectAndEditPlacemark(event);
            } else if (event.isPopupTrigger()) {
                showPopupMenu(event);
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (started) {
            ProductSceneView sceneView = getProductSceneView(event);
            if (sceneView != null
                    && getSelectedPlacemark() != null
                    && sceneView.isCurrentPixelPosValid()) {
                PixelPos pixelPos = new PixelPos((float) sceneView.getCurrentPixelX() + 0.5f,
                                                 (float) sceneView.getCurrentPixelY() + 0.5f);
                if (pixelPos.isValid()) {
                    getSelectedPlacemark().setPixelPos(pixelPos);
                }
            }
        }
    }

    protected abstract void completeInteraction(ProductSceneView sceneView);

    private void selectOrInsertPlacemark(MouseEvent event) {
        ProductSceneView view = getProductSceneView(event);
        if (view == null) {
            return;
        }
        if (!view.isCurrentPixelPosValid()) {
            return;
        }
        Product product = view.getProduct();
        Pin clickedPlacemark = getPlacemarkForPixelPos(event,
                                                       view.getCurrentPixelX(),
                                                       view.getCurrentPixelY());
        if (clickedPlacemark != null) {
            setPlacemarkSelected(getPlacemarkGroup(product), clickedPlacemark, false);
        } else {
            final String[] uniqueNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor,
                                                                                              product);
            final String name = uniqueNameAndLabel[0];
            final String label = uniqueNameAndLabel[1];
            final Pin newPlacemark = new Pin(name, label, "",
                                             new PixelPos(view.getCurrentPixelX() + 0.5f,
                                                          view.getCurrentPixelY() + 0.5f),
                                             null,
                                             placemarkDescriptor.createDefaultSymbol());
            getPlacemarkGroup(product).add(newPlacemark);
        }
    }

    private void selectAndEditPlacemark(MouseEvent event) {
        ProductSceneView view = getProductSceneView(event);
        if (view == null) {
            return;
        }
        if (!view.isCurrentPixelPosValid()) {
            return;
        }
        final Product product = view.getProduct();
        int pixelX = view.getCurrentPixelX();
        int pixelY = view.getCurrentPixelY();
        Pin clickedPlacemark = getPlacemarkForPixelPos(event, pixelX, pixelY);
        if (clickedPlacemark != null) {
            setPlacemarkSelected(getPlacemarkGroup(product), clickedPlacemark, true);
            boolean ok = PlacemarkDialog.showEditPlacemarkDialog(VisatApp.getApp().getMainFrame(), product,
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

    private static Cursor createCursor(PlacemarkDescriptor placemarkDescriptor) {
        final Image cursorImage = placemarkDescriptor.getCursorImage();
        if (cursorImage == null) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,
                                                              placemarkDescriptor.getCursorHotSpot(),
                                                              placemarkDescriptor.getRoleName());
    }

    protected void showPopupMenu(MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();
        VisatApp.getApp().getCommandUIFactory().addContextDependentMenuItems("pin", popup);
        UIUtils.showPopup(popup, event);
    }

    protected ProductSceneView getProductSceneView(MouseEvent event) {
        if (event.getComponent() instanceof ProductSceneView) {
            return (ProductSceneView) event.getComponent();
        }
        if (event.getComponent().getParent() instanceof ProductSceneView) {
            return (ProductSceneView) event.getComponent().getParent();
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

    private Pin getPlacemarkForPixelPos(MouseEvent event, final int selPixelX, final int selPixelY) {
        ProductSceneView view = getProductSceneView(event);

        final AffineTransform i2v = view.getBaseImageToViewTransform();

        final Rectangle2D selViewRect = i2v.createTransformedShape(new Rectangle(selPixelX, selPixelY, 1, 1)).getBounds2D();

        // Compare with pin insertion points (which are in product raster pixel coordinates)
        //
        ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup(getProductSceneView(event).getProduct());
        Pin[] placemarks = placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]);
        for (final Pin placemark : placemarks) {
            // Convert pin pixel to view coordinates
            PixelPos placemarkPixelPos = placemark.getPixelPos();
            if (placemarkPixelPos == null) {
                return null;
            }
            final Rectangle2D placemarkViewRect = i2v.createTransformedShape(new Rectangle2D.Double(Math.floor(placemarkPixelPos.getX()),
                                                                                                    Math.floor(placemarkPixelPos.getY()),
                                                                                                    1, 1)).getBounds2D();
            // Use a rectangular region around the insertion point for comparision
            if (selViewRect.intersects(placemarkViewRect)) {
                return placemark;
            }
        }

        // Now compare against pin symbols (which are in view coordinates).
        // Depending on the symbol used, this may be more or less expensive.
        //
        for (final Pin placemark : placemarks) {
            PlacemarkSymbol symbol = placemark.getSymbol();

            // Convert pin pixel to view coordinates
            PixelPos placemarkPixelPos = placemark.getPixelPos();
            final Point2D placemarkViewPos = i2v.transform(new Point2D.Double(placemarkPixelPos.getX(),
                                                                              placemarkPixelPos.getY()),
                                                           null);

            PixelPos refPoint = symbol.getRefPoint();
            if (refPoint == null) {
                refPoint = new PixelPos(0, 0);
            }
            final Rectangle2D.Double relViewRect = new Rectangle2D.Double(selViewRect.getX() - placemarkViewPos.getX() + refPoint.getX(),
                                                                          selViewRect.getY() - placemarkViewPos.getY() + refPoint.getY(),
                                                                          selViewRect.getWidth(),
                                                                          selViewRect.getHeight());

            if (symbol.getShape().intersects(relViewRect)) {
                return placemark;
            }
        }
        return null;
    }

    protected Pin getSelectedPlacemark() {
        return selectedPlacemark;
    }

    protected void setSelectedPlacemark(Pin selectedPlacemark) {
        this.selectedPlacemark = selectedPlacemark;
    }

}
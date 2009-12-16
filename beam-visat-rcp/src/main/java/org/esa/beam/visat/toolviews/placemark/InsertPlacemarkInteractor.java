package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.swing.figure.FigureEditorInteractor;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;


public abstract class InsertPlacemarkInteractor extends FigureEditorInteractor {

    private final PlacemarkDescriptor placemarkDescriptor;
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
        ProductSceneView sceneView = getProductSceneView(event);
        if (sceneView != null) {
            started = startInteraction(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (started) {
            ProductSceneView sceneView = getProductSceneView(event);
            if (sceneView != null) {
                stopInteraction(event);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (started) {
            activateOverlay();
            if (isSingleButton1Click(event)) {
                selectOrInsertPlacemark(event);
            }
        }
    }


    private void selectOrInsertPlacemark(MouseEvent event) {
        ProductSceneView view = getProductSceneView(event);
        if (view == null) {
            return;
        }
        if (!view.isCurrentPixelPosValid()) {
            return;
        }
        Product product = view.getProduct();
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

    protected ProductSceneView getProductSceneView(MouseEvent event) {
        if (event.getComponent() instanceof ProductSceneView) {
            return (ProductSceneView) event.getComponent();
        }
        if (event.getComponent().getParent() instanceof ProductSceneView) {
            return (ProductSceneView) event.getComponent().getParent();
        }
        return null;
    }

    private void activateOverlay() {
        ExecCommand overlayCommand = getShowOverlayCommand();
        if (overlayCommand != null) {
            overlayCommand.setSelected(true);
            overlayCommand.execute();
        }
    }

}
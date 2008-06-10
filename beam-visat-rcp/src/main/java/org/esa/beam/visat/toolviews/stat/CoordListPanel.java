package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.layer.FigureLayer;

import javax.swing.JOptionPane;
import java.awt.Cursor;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 19.10.2005
 * Time: 13:31:52
 */

/**
 * A pane within the statistcs window which displays a co-oordinate list as a text table.
 *
 * @author Marco Peters
 */
class CoordListPanel extends TextPagePanel {

    private static final String _TITLE_PREFIX = "Co-ordinate List";     /*I18N*/
    private static final String _DEFAULT_COORDLIST_TEXT = "Co-ordinate list not available.\n" +
            "No shape (line, polyline or polygon) contained in the image view.";  /*I18N*/

    private static final LayerObserver _figureLayerObserver = LayerObserver.getInstance(FigureLayer.class);


    public CoordListPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, _DEFAULT_COORDLIST_TEXT, helpID);
        _figureLayerObserver.addLayerObserverListener(new LayerObserver.LayerObserverListener() {
            public void layerChanged() {
                updateContent();
            }
        });
        _figureLayerObserver.setRaster(getRaster());
    }

    @Override
    protected String getTitlePrefix() {
        return _TITLE_PREFIX;
    }


    @Override
    protected void setRaster(final RasterDataNode raster) {
        final RasterDataNode oldRaster = super.getRaster();
        if (oldRaster != raster) {
            _figureLayerObserver.setRaster(raster);
        }
        super.setRaster(raster);
    }


    @Override
    protected void ensureValidData() {
        final Cursor oldCursor = UIUtils.setRootFrameWaitCursor(getParentDialogContentPane());
        try {
            StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occured:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);       /*I18N*/
        } finally {
            UIUtils.setRootFrameCursor(getParentDialogContentPane(), oldCursor);
        }
    }

    @Override
    protected String createText() {
        final Cursor oldCursor = UIUtils.setRootFrameWaitCursor(getParentDialog().getControl());
        try {
            if (getRaster() == null) {
                return _DEFAULT_COORDLIST_TEXT;
            }
            final String transectProfileText = StatisticsUtils.TransectProfile.createTransectProfileText(getRaster());
            if (transectProfileText != null) {
                return transectProfileText;
            } else {
                return _DEFAULT_COORDLIST_TEXT;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occured:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);   /*I18N*/
            return _DEFAULT_COORDLIST_TEXT;
        } finally {
            UIUtils.setRootFrameCursor(getParentDialogContentPane(), oldCursor);
        }
    }

}
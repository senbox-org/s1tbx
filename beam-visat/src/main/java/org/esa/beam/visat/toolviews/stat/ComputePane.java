package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.layer.ROILayer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;


/**
 * A pane which has some 'compute' buttons.
 *
 * @author Marco Peters
 */
class ComputePane extends JPanel {

    private static final LayerObserver _roiLayerObserver = LayerObserver.getInstance(ROILayer.class);
    private final JButton _computeAllPixelsButton;
    private final JButton _computeROIButton;
    private RasterDataNode _raster;

    static ComputePane createComputePane(final ActionListener allPixelsActionListener,
                                         final ActionListener roiActionListener,
                                         final RasterDataNode raster) {
        return new ComputePane(allPixelsActionListener, roiActionListener, raster);
    }


    private ComputePane(final ActionListener allPixelsActionListener,
                        final ActionListener roiActionListener,
                        final RasterDataNode raster) {

        final Icon icon = UIUtils.loadImageIcon("icons/Gears20.gif");

        _computeAllPixelsButton = new JButton("Compute for scene");     /*I18N*/
        _computeAllPixelsButton.setMnemonic('A');
        _computeAllPixelsButton.setEnabled(raster != null);
        _computeAllPixelsButton.addActionListener(allPixelsActionListener);
        _computeAllPixelsButton.setIcon(icon);

        _computeROIButton = new JButton("Compute for ROI");     /*I18N*/
        _computeROIButton.setMnemonic('R');
        _computeROIButton.setEnabled(raster != null && raster.isROIUsable());
        _computeROIButton.addActionListener(roiActionListener);
        _computeROIButton.setIcon(icon);

        _roiLayerObserver.addLayerObserverListener(new LayerObserver.LayerObserverListener() {
            public void layerChanged() {
                _computeROIButton.setEnabled(_raster != null && _raster.isROIUsable());
            }
        });
        setRaster(raster);

        setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        add(_computeAllPixelsButton, gbc);
        add(_computeROIButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 4000;
        add(new JLabel(""), gbc);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
    }

    public void setRaster(final RasterDataNode raster) {
        if (_raster != raster) {

            _raster = raster;
            _roiLayerObserver.setRaster(_raster);


            _computeAllPixelsButton.setEnabled(_raster != null);
            _computeROIButton.setEnabled(_raster != null && _raster.isROIUsable());
        }
    }

}

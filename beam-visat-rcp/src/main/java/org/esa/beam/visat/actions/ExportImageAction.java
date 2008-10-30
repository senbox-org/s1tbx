/*
 * $Id: ExportImageAction.java,v 1.2 2007/02/09 11:05:57 marcop Exp $
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
package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Layer.RenderFilter;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.math.MathUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ExportImageAction extends AbstractExportImageAction {

    private JRadioButton buttonEntireImage;

    @Override
    public void actionPerformed(CommandEvent event) {
        exportImage(getVisatApp(), getSceneImageFileFilters(), event.getSelectableCommand());
    }

    @Override
    public void updateState(final CommandEvent event) {
        boolean enabled = getVisatApp().getSelectedProductSceneView() != null;
        event.getSelectableCommand().setEnabled(enabled);

    }

    @Override
    protected void configureFileChooser(BeamFileChooser fileChooser, ProductSceneView view, String imageBaseName) {
        fileChooser.setDialogTitle(getVisatApp().getAppName() + " - " + "Export Image"); /*I18N*/
        if (view.isRGB()) {
            fileChooser.setCurrentFilename(imageBaseName + "_RGB");
        } else {
            fileChooser.setCurrentFilename(imageBaseName + "_" + view.getRaster().getName());
        }
        final JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Options")); /*I18N*/
        buttonEntireImage = new JRadioButton("Entire image", true);
        final JRadioButton buttonClippingOnly = new JRadioButton("Clipping only", false); /*I18N*/
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(buttonEntireImage);
        buttonGroup.add(buttonClippingOnly);
        panel.add(buttonEntireImage);
        panel.add(buttonClippingOnly);
        final JPanel accessory = new JPanel(new BorderLayout());
        accessory.add(panel, BorderLayout.NORTH);
        fileChooser.setAccessory(accessory);
    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        final boolean useAlpha = !"BMP".equals(imageFormat);
        final boolean entireImage = isEntireImageSelected();
        final RenderFilter renderFilter = new Layer.RenderFilter() {
            @Override
            public boolean canRender(Layer layer) {
                return layer instanceof ImageLayer;
            }
        };
        return createImage(view, entireImage, useAlpha, renderFilter);
    }

    static RenderedImage createImage(ProductSceneView view, boolean entireImage, boolean useAlpha, RenderFilter renderFilter) {
        Rectangle2D modelBounds;
        if (entireImage) {
            modelBounds = view.getBaseImageLayer().getModelBounds();
        } else {
            modelBounds = view.getVisibleModelBounds();
        }

        Rectangle2D imageBounds = view.getBaseImageLayer().getModelToImageTransform().createTransformedShape(modelBounds).getBounds2D() ;
        final int imageWidth = MathUtils.floorInt(imageBounds.getWidth());
        final int imageHeight = MathUtils.floorInt(imageBounds.getHeight());
        final int imageType = useAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        final BufferedImage bi = new BufferedImage(imageWidth, imageHeight, imageType);
        boolean isModelYAxisDown = view.getLayerCanvas().getViewport().isModelYAxisDown();
        Viewport snapshotVp = new DefaultViewport(isModelYAxisDown);
        final BufferedImageRendering imageRendering = new BufferedImageRendering(bi, snapshotVp);

        if (!useAlpha) {
            final Graphics2D graphics = imageRendering.getGraphics();
            graphics.setColor(view.getBackground());
            graphics.fillRect(0, 0, imageWidth, imageHeight);
        }

        snapshotVp.zoom(modelBounds);
        snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);

        view.getRootLayer().render(imageRendering, renderFilter);
        return bi;
    }

    @Override
    protected boolean isEntireImageSelected() {
        return buttonEntireImage.isSelected();
    }
}

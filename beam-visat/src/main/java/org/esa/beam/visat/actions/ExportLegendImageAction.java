/*
 * $Id: ExportLegendImageAction.java,v 1.2 2007/02/09 11:05:57 marcop Exp $
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

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ImageLegend;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.2 $ $Date: 2007/02/09 11:05:57 $
 */
public class ExportLegendImageAction extends AbstractExportImageAction {

    private static final String HORIZONTAL_STR = "Horizontal";
    private static final String VERTICAL_STR = "Vertical";

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;

    @Override
    public void actionPerformed(CommandEvent event) {
        exportImage(getVisatApp(), getImageFileFilters(), event.getSelectableCommand());
    }

    @Override
    public void updateState(final CommandEvent event) {
        ProductSceneView view = getVisatApp().getSelectedProductSceneView();
        boolean enabled = view != null && !view.isRGB();
        event.getSelectableCommand().setEnabled(enabled);

    }

    @Override
    protected void configureFileChooser(BeamFileChooser fileChooser, ProductSceneView view, String imageBaseName) {
        legendParamGroup = createLegendParamGroup();
        legendParamGroup.setParameterValues(getVisatApp().getPreferences(), null);
        modifyHeaderText(legendParamGroup, view.getRaster());
        fileChooser.setDialogTitle(getVisatApp().getAppName() + " - Export Color Legend Image"); /*I18N*/
        fileChooser.setCurrentFilename(imageBaseName + "_legend");
        imageLegend = new ImageLegend(view.getRaster().getImageInfo());
        fileChooser.setAccessory(createImageLegendAccessory(getVisatApp(),
                                                            fileChooser,
                                                            legendParamGroup,
                                                            imageLegend));
    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);
        imageLegend.setBackgroundTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        return imageLegend.createImage();
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup() {
        ParamGroup paramGroup = new ParamGroup();
        Parameter param;

        param = new Parameter("legend.usingHeader", Boolean.TRUE);
        param.getProperties().setLabel("Show header text");
        paramGroup.addParameter(param);

        param = new Parameter("legend.headerText", "");
        param.getProperties().setLabel("Header text");
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);

        param = new Parameter("legend.orientation", HORIZONTAL_STR);
        param.getProperties().setLabel("Orientation");
        param.getProperties().setValueSet(new String[]{HORIZONTAL_STR, VERTICAL_STR});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);

        param = new Parameter("legend.fontSize", new Integer(14));
        param.getProperties().setLabel("Font size");
        param.getProperties().setMinValue(new Integer(4));
        param.getProperties().setMaxValue(new Integer(100));
        paramGroup.addParameter(param);

        param = new Parameter("legend.foregroundColor", Color.black);
        param.getProperties().setLabel("Foreground color");
        paramGroup.addParameter(param);

        param = new Parameter("legend.backgroundColor", Color.white);
        param.getProperties().setLabel("Background color");
        paramGroup.addParameter(param);

        param = new Parameter("legend.backgroundTransparency", new Float(0.0f));
        param.getProperties().setLabel("Background transparency");
        param.getProperties().setMinValue(new Float(0.0f));
        param.getProperties().setMaxValue(new Float(1.0f));
        paramGroup.addParameter(param);

        param = new Parameter("legend.antialiasing", Boolean.TRUE);
        param.getProperties().setLabel("Perform anti-aliasing");
        paramGroup.addParameter(param);

        return paramGroup;
    }

    private static void modifyHeaderText(ParamGroup legendParamGroup, RasterDataNode raster) {
        String name = raster.getName();
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        String headerText = name + " [" + unit + "]";
        legendParamGroup.getParameter("legend.headerText").setValue(headerText, null);
    }

    private static JComponent createImageLegendAccessory(final VisatApp visatApp,
                                                         final JFileChooser fileChooser,
                                                         final ParamGroup legendParamGroup,
                                                         final ImageLegend imageLegend) {
        final JButton button = new JButton("Properties...");
        button.setMnemonic('P');
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final BeamFileFilter fileFilter = (BeamFileFilter) fileChooser.getFileFilter();
                final ImageLegendDialog dialog = new ImageLegendDialog(visatApp,
                                                                       legendParamGroup,
                                                                       imageLegend.getImageInfo(),
                                                                       isTransparencySupportedByFormat(
                                                                               fileFilter.getFormatName()));
                dialog.show();
            }
        });
        final JPanel accessory = new JPanel(new BorderLayout());
        accessory.setBorder(new EmptyBorder(3, 3, 3, 3));
        accessory.add(button, BorderLayout.NORTH);
        return accessory;
    }

    private static void transferParamsToImageLegend(ParamGroup legendParamGroup, ImageLegend imageLegend) {
        Object value;

        value = legendParamGroup.getParameter("legend.usingHeader").getValue();
        imageLegend.setUsingHeader(((Boolean) value).booleanValue());

        value = legendParamGroup.getParameter("legend.headerText").getValue();
        imageLegend.setHeaderText((String) value);

        value = legendParamGroup.getParameter("legend.orientation").getValue();
        imageLegend.setOrientation(HORIZONTAL_STR.equals(value) ? ImageLegend.HORIZONTAL : ImageLegend.VERTICAL);

        value = legendParamGroup.getParameter("legend.fontSize").getValue();
        imageLegend.setFont(imageLegend.getFont().deriveFont(((Number) value).floatValue()));

        value = legendParamGroup.getParameter("legend.backgroundColor").getValue();
        imageLegend.setBackgroundColor((Color) value);

        value = legendParamGroup.getParameter("legend.foregroundColor").getValue();
        imageLegend.setForegroundColor((Color) value);

        value = legendParamGroup.getParameter("legend.backgroundTransparency").getValue();
        imageLegend.setBackgroundTransparency(((Number) value).floatValue());

        value = legendParamGroup.getParameter("legend.antialiasing").getValue();
        imageLegend.setAntialiasing(((Boolean) value).booleanValue());
    }


    public static class ImageLegendDialog extends ModalDialog {

        private static final String _HELP_ID = "";

        private VisatApp _visatApp;
        private ImageInfo _imageInfo;
        private boolean _transparencyEnabled;

        private ParamGroup _paramGroup;

        private Parameter _usingHeaderParam;
        private Parameter _headerTextParam;
        private Parameter _orientationParam;
        private Parameter _fontSizeParam;
        private Parameter _backgroundColorParam;
        private Parameter _foregroundColorParam;
        private Parameter _antialiasingParam;
        private Parameter _backgroundTransparencyParam;

        public ImageLegendDialog(VisatApp visatApp, ParamGroup paramGroup, ImageInfo imageInfo,
                                 boolean transparencyEnabled) {
            super(visatApp.getMainFrame(), "VISAT - Color Legend Properties", ModalDialog.ID_OK_CANCEL, _HELP_ID);
            _visatApp = visatApp;
            _imageInfo = imageInfo;
            _transparencyEnabled = transparencyEnabled;
            _paramGroup = paramGroup;
            initParams();
            initUI();
            updateUIState();
            _paramGroup.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(ParamChangeEvent event) {
                    updateUIState();
                }
            });
        }

        private void updateUIState() {
            boolean headerTextEnabled = ((Boolean) _usingHeaderParam.getValue()).booleanValue();
            _headerTextParam.setUIEnabled(headerTextEnabled);
            _backgroundTransparencyParam.setUIEnabled(_transparencyEnabled);
        }

        public ParamGroup getParamGroup() {
            return _paramGroup;
        }

        public void setHeaderText(String text) {
            _headerTextParam.setValue(text, null);
        }

        public boolean isTransparencyEnabled() {
            return _transparencyEnabled;
        }

        public void setTransparencyEnabled(boolean transparencyEnabled) {
            _transparencyEnabled = transparencyEnabled;
            updateUIState();
        }

        public void getImageLegend(ImageLegend imageLegend) {
            transferParamsToImageLegend(getParamGroup(), imageLegend);
        }

        public ImageInfo getImageInfo() {
            return _imageInfo;
        }

        @Override
        protected void onOK() {
            getParamGroup().getParameterValues(_visatApp.getPreferences());
            super.onOK();
        }

        private void initUI() {
            final JButton previewButton = new JButton("Preview...");
            previewButton.setMnemonic('v');
            previewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPreview();
                }
            });

            final GridBagConstraints gbc = new GridBagConstraints();
            final JPanel p = GridBagUtils.createPanel();

            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets.top = 0;

            gbc.gridy = 0;
            gbc.gridwidth = 2;
            p.add(_usingHeaderParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(_headerTextParam.getEditor().getLabelComponent(), gbc);
            p.add(_headerTextParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(_orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(_orientationParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(_fontSizeParam.getEditor().getLabelComponent(), gbc);
            p.add(_fontSizeParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(_foregroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(_foregroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(_backgroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(_backgroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(_backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(_backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;

            gbc.insets.top = 10;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            p.add(_antialiasingParam.getEditor().getEditorComponent(), gbc);

            gbc.insets.top = 10;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            p.add(previewButton, gbc);

            p.setBorder(new EmptyBorder(7, 7, 7, 7));

            setContent(p);
        }

        private void initParams() {
            _usingHeaderParam = _paramGroup.getParameter("legend.usingHeader");
            _headerTextParam = _paramGroup.getParameter("legend.headerText");
            _orientationParam = _paramGroup.getParameter("legend.orientation");
            _fontSizeParam = _paramGroup.getParameter("legend.fontSize");
            _foregroundColorParam = _paramGroup.getParameter("legend.foregroundColor");
            _backgroundColorParam = _paramGroup.getParameter("legend.backgroundColor");
            _backgroundTransparencyParam = _paramGroup.getParameter("legend.backgroundTransparency");
            _antialiasingParam = _paramGroup.getParameter("legend.antialiasing");
        }

        private void showPreview() {
            final ImageLegend imageLegend = new ImageLegend(getImageInfo());
            getImageLegend(imageLegend);
            final BufferedImage image = imageLegend.createImage();
            final ImageDisplay imageDisplay = new ImageDisplay(image);
            imageDisplay.setOpaque(true);
            imageDisplay.addMouseListener(new MouseAdapter() {
                // Both events (releases & pressed) must be checked, otherwise it won't work on all
                // platforms

                /**
                 * Invoked when a mouse button has been released on a component.
                 */
                @Override
                public void mouseReleased(MouseEvent e) {
                    // On Windows
                    showPopup(e, image, imageDisplay);
                }

                /**
                 * Invoked when a mouse button has been pressed on a component.
                 */
                @Override
                public void mousePressed(MouseEvent e) {
                    // On Linux
                    // todo - clipboard does not work on linux.
                    // todo - better not to show popup until it works correctly
//                    showPopup(e, image, imageDisplay);
                }
            });
            final ModalDialog dialog = new ModalDialog(getParent(), "VISAT - Color Legend Preview", imageDisplay,
                                                       ModalDialog.ID_OK, null);
            dialog.getJDialog().setResizable(false);
            dialog.show();
        }

        private static void showPopup(final MouseEvent e, final BufferedImage image, final ImageDisplay imageDisplay) {
            if (e.isPopupTrigger()) {
                final JPopupMenu popupMenu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Copy image to clipboard");
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        SystemUtils.copyToClipboard(image);
                    }
                });
                popupMenu.add(menuItem);
                popupMenu.show(imageDisplay, e.getX(), e.getY());
            }
        }
    }

}

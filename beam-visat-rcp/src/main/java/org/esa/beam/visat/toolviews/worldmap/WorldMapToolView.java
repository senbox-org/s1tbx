/*
 * $Id: WorldMapToolView.java,v 1.1 2007/04/19 10:41:39 norman Exp $
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
package org.esa.beam.visat.toolviews.worldmap;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.WorldMapImageLoader;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The window displaying the world map.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapToolView extends AbstractToolView {

    public static final String ID = WorldMapToolView.class.getName();

    private VisatApp visatApp;

    private WorldMapPane _worldMapPane;
    private JScrollPane _scrollPane;
    private JCheckBox _checkBoxAuto;
    private int _currentCursorType;
    private static final float SCALE_1 = 0.125f;
    private static final float SCALE_2 = 0.25f;
    private static final float SCALE_4 = 0.5f;
    private static final float SCALE_8 = 1.0f;
    private static final float SCALE_16 = 2.0f;
    private static final float SCALE_32 = 4.0f;

    public WorldMapToolView() {
        visatApp = VisatApp.getApp();
    }

    public JComponent createControl() {
        _worldMapPane = new WorldMapPane(WorldMapImageLoader.getWorldMapImage(false));

        final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"),
                                                                         false);
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");

        final JRadioButton zoom1 = new JRadioButton("1 x");
        zoom1.setName("zoom1");
        final JRadioButton zoom2 = new JRadioButton("2 x");
        zoom2.setName("zoom2");
        final JRadioButton zoom4 = new JRadioButton("4 x");
        zoom4.setName("zoom4");
        final JRadioButton zoom8 = new JRadioButton("8 x");
        zoom8.setName("zoom8");
        final JRadioButton zoom16 = new JRadioButton("16 x");
        zoom16.setName("zoom16");
        final JRadioButton zoom32 = new JRadioButton("32 x");
        zoom32.setName("zoom32");
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCursor(Cursor.WAIT_CURSOR);
                try {
                    if (zoom1.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_1);
                    } else if (zoom2.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_2);
                    } else if (zoom4.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_4);
                    } else if (zoom8.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_8);
                    } else if (zoom16.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_16);
                    } else if (zoom32.isSelected()) {
                        _worldMapPane.setScale(WorldMapToolView.SCALE_32);
                    }
                    packIfNeeded();
                    final Product selectedProduct = getSelectedProduct();
                    if (selectedProduct != null && selectedProduct.getGeoCoding() != null) {
                        if (_checkBoxAuto.isSelected()) {
                            centerSelectedProduct();
                        }
                    }
                } finally {
                    setCursor(Cursor.DEFAULT_CURSOR);
                }
            }
        };
        zoom1.addActionListener(listener);
        zoom2.addActionListener(listener);
        zoom4.addActionListener(listener);
        zoom8.addActionListener(listener);
        zoom16.addActionListener(listener);
        zoom32.addActionListener(listener);

        zoom2.setSelected(true);
        _worldMapPane.setScale(WorldMapToolView.SCALE_2);

        final ButtonGroup zoomGroup = new ButtonGroup();
        zoomGroup.add(zoom1);
        zoomGroup.add(zoom2);
        zoomGroup.add(zoom4);
        zoomGroup.add(zoom8);
        zoomGroup.add(zoom16);
        zoomGroup.add(zoom32);

        _checkBoxAuto = new JCheckBox("Autoc.");
        _checkBoxAuto.setName("checkBoxAuto");
        _checkBoxAuto.setToolTipText("Automatically center selected product");/*I18N*/
        _checkBoxAuto.setSelected(true);
        _checkBoxAuto.setEnabled(false);

        final JCheckBox checkBoxHighRes = new JCheckBox("High-Res.");
        checkBoxHighRes.setName("checkBoxHighRes");
        checkBoxHighRes.setToolTipText("Use high resolution image");/*I18N*/
        checkBoxHighRes.setSelected(false);
        checkBoxHighRes.setEnabled(true);
        checkBoxHighRes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _worldMapPane.setWorldMapImage(WorldMapImageLoader.getWorldMapImage(checkBoxHighRes.isSelected()));
                if (_checkBoxAuto.isSelected()) {
                    centerSelectedProduct();
                }
                packIfNeeded();
            }
        });

        final JButton buttonCenter = new JButton("Center");
        buttonCenter.setName("buttonCenter");
        buttonCenter.setToolTipText("Center selected product"); /*I18N*/
        buttonCenter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                centerSelectedProduct();
            }
        });

        final JPanel zoomPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 0;
        GridBagUtils.addToPanel(zoomPane, zoom1, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, zoom2, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, zoom4, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, zoom8, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, zoom16, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, zoom32, gbc);
        gbc.gridy ++;
        gbc.insets.top = 4;
        GridBagUtils.addToPanel(zoomPane, checkBoxHighRes, gbc);
        gbc.gridy ++;
        GridBagUtils.addToPanel(zoomPane, _checkBoxAuto, gbc);
        gbc.gridy ++;
        gbc.insets.top = 10;
        GridBagUtils.addToPanel(zoomPane, buttonCenter, gbc);

        final JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(zoomPane, BorderLayout.NORTH);
        buttons.add(helpButton, BorderLayout.SOUTH);

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        _scrollPane = new JScrollPane(_worldMapPane);
        final JPanel scrollPanePanel = new JPanel(new BorderLayout());
        scrollPanePanel.add(_scrollPane, BorderLayout.CENTER);

        mainPane.add(scrollPanePanel, BorderLayout.CENTER);
        mainPane.add(buttons, BorderLayout.EAST);
        mainPane.setPreferredSize(new Dimension(320, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }


        visatApp.addProductTreeListener(new WorldMapPTL());

        // Add an internal frame listener to VISAT so that we can update our
        // world map window with the information of the currently activated
        // product scene view.
        //
        visatApp.addInternalFrameListener(new WorldMapIFL());
        setProducts(visatApp);
        setSelectedProduct(visatApp.getSelectedProduct());

        return mainPane;
    }

    public void setSelectedProduct(Product product) {
        _worldMapPane.setSelectedProduct(product);
        if (_checkBoxAuto.isSelected()) {
            centerSelectedProduct();
        }
    }

    public Product getSelectedProduct() {
        return _worldMapPane.getSelectedProduct();
    }

    private void setProducts(final VisatApp visatApp) {
        setProducts(visatApp.getProductManager().getProducts());
    }


    public void setProducts(Product[] products) {
        _worldMapPane.setProducts(products);
        _checkBoxAuto.setEnabled(products != null && products.length > 0);
    }

    public void setPathesToDisplay(GeoPos[][] geoBoundaries) {
        _worldMapPane.setPathesToDisplay(geoBoundaries);
    }

    private void setCursor(int cursorType) {

        if (_currentCursorType != cursorType) {
            _currentCursorType = cursorType;
            getWindowAncestor().setCursor(Cursor.getPredefinedCursor(_currentCursorType));
        }
    }

    private void centerSelectedProduct() {
        final PixelPos center = _worldMapPane.getCurrentProductCenter();
        if (center != null) {
            final Dimension currentimageSize = _worldMapPane.getCurrentimageSize();
            final float scale = _worldMapPane.getScale();
            final JViewport viewport = _scrollPane.getViewport();
            final Dimension visibleSize = viewport.getExtentSize();
            final int viewVisWidth = visibleSize.width;
            final int viewVisHeight = visibleSize.height;
            final int maxX = currentimageSize.width - viewVisWidth;
            final int maxY = currentimageSize.height - viewVisHeight;
            if (currentimageSize.width <= visibleSize.width && currentimageSize.height <= visibleSize.height) {
                return;
            }
            center.x *= scale;
            center.y *= scale;
            if (center.x < 0) {
                center.x += currentimageSize.width;
            } else if (center.x > currentimageSize.width) {
                center.x -= currentimageSize.width;
            }
            int viewPosX = 0;
            if (_scrollPane.getHorizontalScrollBar() != null) {
                viewPosX = (int) center.x - viewVisWidth / 2;
            }
            if (viewPosX > maxX) {
                viewPosX = maxX;
            } else if (viewPosX < 0) {
                viewPosX = 0;
            }
            int viewPosY = 0;
            if (_scrollPane.getVerticalScrollBar() != null) {
                viewPosY = (int) center.y - viewVisHeight / 2;
            }
            if (viewPosY > maxY) {
                viewPosY = maxY;
            } else if (viewPosY < 0) {
                viewPosY = 0;
            }
            viewport.setViewPosition(new Point(viewPosX, viewPosY));
        }
    }

    public void packIfNeeded() {
        Dimension vpSize = _scrollPane.getSize();
        Dimension imageSize = _worldMapPane.getCurrentimageSize();
        boolean packNeeded = vpSize.getWidth() > imageSize.getWidth() || vpSize.getHeight() > imageSize.getHeight();
        if (packNeeded) {
            _scrollPane.getViewport().setViewSize(imageSize);
//            pack();   // todo - what about packing
        }
    }

    private class WorldMapPTL implements ProductTreeListener {

        public WorldMapPTL() {
        }

        public void productAdded(final Product product) {
            setSelectedProduct(product);
            setProducts(visatApp);
        }

        public void productRemoved(final Product product) {
            if (getSelectedProduct() == product) {
                setSelectedProduct(product);
            }
            setProducts(visatApp);
        }

        public void productSelected(final Product product, final int clickCount) {
            setSelectedProduct(product);
        }

        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            final Product product = group.getProduct();
            setSelectedProduct(product);
        }

        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            final Product product = tiePointGrid.getProduct();
            setSelectedProduct(product);
        }

        public void bandSelected(final Band band, final int clickCount) {
            final Product product = band.getProduct();
            setSelectedProduct(product);
        }
    }

    private class WorldMapIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            Product product = null;
            if (contentPane instanceof ProductSceneView) {
                product = ((ProductSceneView) contentPane).getProduct();
            }
            setSelectedProduct(product);
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
        }
    }
}

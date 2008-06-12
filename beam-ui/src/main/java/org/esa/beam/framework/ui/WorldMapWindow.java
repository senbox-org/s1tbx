/*
 * $Id: WorldMapWindow.java,v 1.3 2007/04/18 13:01:13 norman Exp $
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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.help.HelpSys;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
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
public class WorldMapWindow extends JDialog {

    private static String _TITLE = "World Map";

    private String _helpId;
    private WorldMapPane _worldMapPane;
    private JScrollPane _scrollPane;
    private JCheckBox _checkBoxAuto;
    private JComponent _accessory;
    private int _currentCursorType;
    private static final float SCALE_1 = 0.125f;
    private static final float SCALE_2 = 0.25f;
    private static final float SCALE_4 = 0.5f;
    private static final float SCALE_8 = 1.0f;
    private static final float SCALE_16 = 2.0f;
    private static final float SCALE_32 = 4.0f;

    public WorldMapWindow(Frame owner, String title, String helpId, JComponent accessory) {
        super(owner, title != null && title.length() > 0 ? title : _TITLE, false);
        if (title != null) {
            _TITLE = title;
        }
        _helpId = helpId;
        _accessory = accessory;
        createUI();
        if (_helpId != null) {
            HelpSys.enableHelpKey(this.getContentPane(), _helpId);
        }
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

    public void setProducts(Product[] products) {
        _worldMapPane.setProducts(products);
        _checkBoxAuto.setEnabled(products != null && products.length > 0);
    }

    public void setPathesToDisplay(GeoPos[][] geoBoundaries) {
        _worldMapPane.setPathesToDisplay(geoBoundaries);
    }

    private void createUI() {
        _worldMapPane = new WorldMapPane(WorldMapImageLoader.getWorldMapImage(false));

        final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"),
                                                                         false);
        helpButton.setToolTipText("Help."); /*I18N*/
        if (_helpId != null) {
            HelpSys.enableHelpOnButton(helpButton, _helpId);
        }

        final JRadioButton zoom1 = new JRadioButton("1 x");
        final JRadioButton zoom2 = new JRadioButton("2 x");
        final JRadioButton zoom4 = new JRadioButton("4 x");
        final JRadioButton zoom8 = new JRadioButton("8 x");
        final JRadioButton zoom16 = new JRadioButton("16 x");
        final JRadioButton zoom32 = new JRadioButton("32 x");
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCursor(Cursor.WAIT_CURSOR);
                try {
                    if (zoom1.isSelected()) {
                        _worldMapPane.setScale(SCALE_1);
                    } else if (zoom2.isSelected()) {
                        _worldMapPane.setScale(SCALE_2);
                    } else if (zoom4.isSelected()) {
                        _worldMapPane.setScale(SCALE_4);
                    } else if (zoom8.isSelected()) {
                        _worldMapPane.setScale(SCALE_8);
                    } else if (zoom16.isSelected()) {
                        _worldMapPane.setScale(SCALE_16);
                    } else if (zoom32.isSelected()) {
                        _worldMapPane.setScale(SCALE_32);
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
        _worldMapPane.setScale(SCALE_2);

        final ButtonGroup zoomGroup = new ButtonGroup();
        zoomGroup.add(zoom1);
        zoomGroup.add(zoom2);
        zoomGroup.add(zoom4);
        zoomGroup.add(zoom8);
        zoomGroup.add(zoom16);
        zoomGroup.add(zoom32);

        _checkBoxAuto = new JCheckBox("Autoc.");
        _checkBoxAuto.setToolTipText("Automatically center selected product");/*I18N*/
        _checkBoxAuto.setSelected(true);
        _checkBoxAuto.setEnabled(false);

        final JCheckBox checkBoxHighRes = new JCheckBox("High-Res.");
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

        final JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        _scrollPane = new JScrollPane(_worldMapPane);
        final JPanel scrollPanePanel = new JPanel(new BorderLayout());
        scrollPanePanel.add(_scrollPane, BorderLayout.CENTER);

        if (_accessory != null) {
            scrollPanePanel.add(_accessory, BorderLayout.SOUTH);
        }

        contentPane.add(scrollPanePanel, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.EAST);
        setContentPane(contentPane);
    }

    private void setCursor(int cursorType) {

        if (_currentCursorType != cursorType) {
            _currentCursorType = cursorType;
            setCursor(Cursor.getPredefinedCursor(_currentCursorType));
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
            pack();
        }
    }
}

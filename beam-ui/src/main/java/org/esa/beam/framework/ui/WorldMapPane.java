/*
 * $Id: WorldMapPane.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
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
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * This class displays a world map with the given products on top.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public final class WorldMapPane extends JPanel {

    private final WorldMapPainter _painter;

    public WorldMapPane(final BufferedImage image) {
        Guardian.assertNotNull("image", image);
        this.setDoubleBuffered(true);
        _painter = new WorldMapPainter(image);
        _painter.setScale(1.0f);
        setPreferredSize(_painter.getCurrentImageSize());
        setSize(_painter.getCurrentImageSize());
        setOpaque(true);
        Debug.trace("WorldMapPane.constructor -> exit");
    }

    public final Product getSelectedProduct() {
        return _painter.getSelectedProduct();
    }

    public final void setSelectedProduct(final Product product) {
        if (getSelectedProduct() != product) {
            final Product oldProduct = getSelectedProduct();
            _painter.setSelectedProduct(product);
            repaint();
            firePropertyChange("product", oldProduct, product);
        }
    }

    public void setWorldMapImage(BufferedImage bufferedImage) {
        _painter.setWorldMapImage(bufferedImage);
        setPreferredSize(_painter.getCurrentImageSize());
        setSize(_painter.getCurrentImageSize());
        repaint();
    }

    public final Product[] getProducts() {
        return _painter.getProducts();
    }

    public final void setProducts(final Product[] products) {
        if (getProducts() != products) {
            final Product[] oldProducts = getProducts();
            _painter.setProducts(products);
            repaint();
            firePropertyChange("products", oldProducts, products);
        }
    }

    public void setPathesToDisplay(final GeoPos[][] geoBoundaries) {
        if (_painter.getPathesToDisplay() != geoBoundaries) {
            final GeoPos[][] oldGeoBoundaries = _painter.getPathesToDisplay();
            _painter.setPathesToDisplay(geoBoundaries);
            repaint();
            firePropertyChange("extraGeoBoundaries", oldGeoBoundaries, geoBoundaries);
        }
    }

    public final float getScale() {
        return _painter.getScale();
    }

    public final void setScale(final float scale) {
        if (getScale() != scale) {
            final float oldValue = getScale();
            _painter.setScale(scale);
            setPreferredSize(_painter.getCurrentImageSize());
            setSize(_painter.getCurrentImageSize());
            firePropertyChange("scale", oldValue, scale);
        }
    }

    public final Dimension getCurrentimageSize() {
        return _painter.getCurrentImageSize();
    }

    public PixelPos getCurrentProductCenter() {
        return _painter.getCurrentProductCenter();
    }

    @Override
    protected final void paintComponent(final Graphics g) {
        _painter.paint(g);
    }
}

/*
 * $Id: StatusBarPosDisplayVPI.java,v 1.1 2007/04/19 10:28:50 norman Exp $
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
package org.esa.beam.visat.plugins;

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.AbstractVisatPlugIn;
import com.jidesoft.status.LabelStatusBarItem;

public class StatusBarPosDisplayVPI extends AbstractVisatPlugIn {

    private VisatApp _visatApp;
    private LabelStatusBarItem _positionStatusBarItem;
    private HashMap<Product, PixelPositionListener> _pixelPosListeners;
    private float _pixelOffsetX;
    private float _pixelOffsetY;
    private boolean _showPixelOffsetDecimals;

    public void start(final VisatApp visatApp) {
        _visatApp = visatApp;
        final PropertyMap preferences = visatApp.getPreferences();
        preferences.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String propertyName = evt.getPropertyName();
                if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)) {
                    setPixelOffsetX(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)) {
                    setPixelOffsetY(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
                    setShowPixelOffsetDecimals(preferences);
                }
            }
        });
        setPixelOffsetX(preferences);
        setPixelOffsetY(preferences);
        setShowPixelOffsetDecimals(preferences);

        visatApp.addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameOpened(InternalFrameEvent e) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView view = (ProductSceneView) contentPane;
                    view.addPixelPositionListener(registerPixelPositionListener(view.getProduct()));
                }
            }

            public void internalFrameClosing(InternalFrameEvent e) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView view = (ProductSceneView) contentPane;
                    view.removePixelPositionListener(getPixelPositionListener(view.getProduct()));
                }
            }
        });
    }

    private void setPixelOffsetY(final PropertyMap preferences) {
        _pixelOffsetY = (float) preferences.getPropertyDouble(VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                                                              VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY);
    }

    private void setPixelOffsetX(final PropertyMap preferences) {
        _pixelOffsetX = (float) preferences.getPropertyDouble(VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                                                              VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY);
    }

    private void setShowPixelOffsetDecimals(final PropertyMap preferences) {
        _showPixelOffsetDecimals = preferences.getPropertyBool(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS);
    }

    private PixelPositionListener registerPixelPositionListener(Product product) {
        if (_pixelPosListeners == null) {
            _pixelPosListeners = new HashMap<Product, PixelPositionListener>();
        }
        PixelPositionListener listener = new PixelPosHandler(product.getProductRefString());
        _pixelPosListeners.put(product, listener);
        return listener;
    }

    private PixelPositionListener getPixelPositionListener(Product product) {
        if (_pixelPosListeners != null) {
            return _pixelPosListeners.get(product);
        }
        return null;
    }

    private LabelStatusBarItem getPositionStatusBarItem() {
        if (_positionStatusBarItem == null) {
            _positionStatusBarItem = (LabelStatusBarItem) _visatApp.getStatusBar().getItemByName(VisatApp.POSITION_STATUS_BAR_ITEM_KEY);
        }
        return _positionStatusBarItem;
    }

    private class PixelPosHandler implements PixelPositionListener {

        private final String _refString;
        private StringBuilder _text;
        private final String _POS_NOT_AVAILABLE = "No pos.";
        private final String _INVALID_POS = "Invalid pos.";

        public PixelPosHandler(String refString) {
            _refString = refString;
            _text = new StringBuilder(64);
        }

        public void pixelPosChanged(RenderedImage sourceImage,
                                    int pixelX,
                                    int pixelY,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            LabelStatusBarItem positionStatusBarItem = getPositionStatusBarItem();
            if (positionStatusBarItem == null) {
                return;
            }
            if (pixelPosValid) {
                _text.setLength(0);
                _text.append(_refString);
                _text.append(' ');
                if (_showPixelOffsetDecimals) {
                    _text.append(pixelX + _pixelOffsetX);
                    _text.append(',');
                    _text.append(pixelY + _pixelOffsetY);
                } else {
                    _text.append(pixelX);
                    _text.append(',');
                    _text.append(pixelY);
                }
                if (positionStatusBarItem.isVisible()) {
                    positionStatusBarItem.setText(_text.toString());
                }
            } else {
                positionStatusBarItem.setText(_INVALID_POS);
            }
        }

        public void pixelPosNotAvailable(RenderedImage sourceImage) {
            LabelStatusBarItem positionStatusBarItem = getPositionStatusBarItem();
            if (positionStatusBarItem != null) {
                positionStatusBarItem.setText(_POS_NOT_AVAILABLE);
            }
        }
    }
}

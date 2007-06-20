/*
 * $Id: SliderBoxImageDisplay.java,v 1.1 2006/10/10 14:47:39 norman Exp $
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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.RenderedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A component representing an image display with a draggable slider box in it.
 */
public class SliderBoxImageDisplay extends ImageDisplay {

    private JComponent _sliderBox;
    private int _sliderSectionX;
    private int _sliderSectionY;
    private Rectangle _sliderRectOld;
    private Point _clickPos;
    private boolean _imageWidthFixed;
    private boolean _imageHeightFixed;
    private SliderBoxChangeListener _sliderBoxChangeListener;
    private static final int _HANDLE_SIZE = 6;

    public SliderBoxImageDisplay(RenderedImage renderedImage, SliderBoxChangeListener sliderBoxChangeListener) {
        super(renderedImage);
        createUI(sliderBoxChangeListener);
    }

    public SliderBoxImageDisplay(int imgWidth, int imgHeight, SliderBoxChangeListener sliderBoxChangeListener) {
        super(imgWidth, imgHeight);
        createUI(sliderBoxChangeListener);
    }

    private void createUI(SliderBoxChangeListener sliderBoxChangeListener) {

        _sliderBoxChangeListener = sliderBoxChangeListener;

        _sliderBox = new JLabel();
        _sliderBox.setBounds(0, 0, getImageWidth(), getImageHeight());
        _sliderBox.setOpaque(false);
        _sliderBox.setBorder(UIDefaults.SLIDER_BOX_BORDER);

        setLayout(null);
        add(_sliderBox);
        clearSliderSections();

        addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                _sliderRectOld = new Rectangle(_sliderBox.getBounds());
                _clickPos = new Point(e.getPoint());
                computeSliderSections(e);
            }

            public void mouseReleased(MouseEvent e) {
                _sliderRectOld = null;
                _clickPos = null;
                clearSliderSections();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseDragged(MouseEvent e) {
                if (_sliderRectOld == null || _clickPos == null) {
                    return;
                }
                modifySliderBox(e);
            }
        });
    }

    public boolean isImageWidthFixed() {
        return _imageWidthFixed;
    }

    public void setImageWidthFixed(boolean imageWidthFixed) {
        if (_imageWidthFixed == imageWidthFixed) {
            return;
        }
        _imageWidthFixed = imageWidthFixed;
        if (_imageWidthFixed) {
            setSliderBoxBounds(0, _sliderBox.getY(), getImageWidth(), _sliderBox.getHeight(), true);
        }
    }

    public boolean isImageHeightFixed() {
        return _imageHeightFixed;
    }

    public void setImageHeightFixed(boolean imageHeightFixed) {
        if (_imageHeightFixed == imageHeightFixed) {
            return;
        }
        _imageHeightFixed = imageHeightFixed;
        if (_imageHeightFixed) {
            setSliderBoxBounds(_sliderBox.getX(), 0, _sliderBox.getWidth(), getImageHeight(), true);
        }
    }

    public SliderBoxChangeListener getSliderBoxChangeListener() {
        return _sliderBoxChangeListener;
    }

    public void setSliderBoxChangeListener(SliderBoxChangeListener sliderBoxChangeListener) {
        _sliderBoxChangeListener = sliderBoxChangeListener;
    }

    public Rectangle getSliderBoxBounds() {
        return _sliderBox.getBounds();
    }

    public void setSliderBoxBounds(Rectangle rectangle) {
        setSliderBoxBounds(rectangle, false);
    }

    public void setSliderBoxBounds(Rectangle rectangle, boolean fireEvent) {
        setSliderBoxBounds(rectangle.x, rectangle.y, rectangle.width, rectangle.height, fireEvent);
    }

    public void setSliderBoxBounds(int x, int y, int width, int height) {
        setSliderBoxBounds(x, y, width, height, false);
    }

    public void setSliderBoxBounds(int x, int y, int width, int height, boolean fireEvent) {
        if (isImageWidthFixed()) {
            x = 0;
            width = getImageWidth();
        }
        if (isImageHeightFixed()) {
            y = 0;
            height = getImageHeight();
        }
        if (_sliderBox.getX() == x
            && _sliderBox.getY() == y
            && _sliderBox.getWidth() == width
            && _sliderBox.getHeight() == height) {
            return;
        }
        _sliderBox.setBounds(x, y, width, height); // also repaints!
        if (_sliderBoxChangeListener != null && fireEvent) {
            _sliderBoxChangeListener.sliderBoxChanged(_sliderBox.getBounds());
        }
    }


    private void clearSliderSections() {
        _sliderSectionX = -1;
        _sliderSectionY = -1;
    }

    private void computeSliderSections(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();
        int x1 = _sliderBox.getX();
        int y1 = _sliderBox.getY();
        int x2 = _sliderBox.getX() + _sliderBox.getWidth();
        int y2 = _sliderBox.getY() + _sliderBox.getHeight();
        int dx1 = Math.abs(x1 - x);
        int dy1 = Math.abs(y1 - y);
        int dx2 = Math.abs(x2 - x);
        int dy2 = Math.abs(y2 - y);

        _sliderSectionX = -1;
        if (dx1 <= _HANDLE_SIZE) {
            _sliderSectionX = 0;   // left slider handle selected
        } else if (dx2 <= _HANDLE_SIZE) {
            _sliderSectionX = 2;   // right slider handle selected
        } else if (x >= x1 && x < x2) {
            _sliderSectionX = 1;   // center slioder handle selected
        }

        _sliderSectionY = -1;
        if (dy1 <= _HANDLE_SIZE) {
            _sliderSectionY = 0; // upper slider handle selected
        } else if (dy2 <= _HANDLE_SIZE) {
            _sliderSectionY = 2; // lower slider handle selected
        } else if (y > y1 && y < y2) {
            _sliderSectionY = 1; // center slider handle selected
        }
    }

    private void modifySliderBox(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dx = x - _clickPos.x;
        int dy = y - _clickPos.y;

        int sbx = 0;
        int sby = 0;
        int sbw = 0;
        int sbh = 0;
        boolean validMode = false;

        if (_sliderSectionX == 0 && _sliderSectionY == 0) {
            sbx = _sliderRectOld.x + dx;
            sby = _sliderRectOld.y + dy;
            sbw = _sliderRectOld.width - dx;
            sbh = _sliderRectOld.height - dy;
            validMode = true;
        } else if (_sliderSectionX == 1 && _sliderSectionY == 0) {
            sbx = _sliderRectOld.x;
            sby = _sliderRectOld.y + dy;
            sbw = _sliderRectOld.width;
            sbh = _sliderRectOld.height - dy;
            validMode = true;
        } else if (_sliderSectionX == 2 && _sliderSectionY == 0) {
            sbx = _sliderRectOld.x;
            sby = _sliderRectOld.y + dy;
            sbw = _sliderRectOld.width + dx;
            sbh = _sliderRectOld.height - dy;
            validMode = true;
        } else if (_sliderSectionX == 0 && _sliderSectionY == 1) {
            sbx = _sliderRectOld.x + dx;
            sby = _sliderRectOld.y;
            sbw = _sliderRectOld.width - dx;
            sbh = _sliderRectOld.height;
            validMode = true;
        } else if (_sliderSectionX == 1 && _sliderSectionY == 1) {
            sbx = _sliderRectOld.x + dx;
            sby = _sliderRectOld.y + dy;
            sbw = _sliderRectOld.width;
            sbh = _sliderRectOld.height;
            validMode = true;
        } else if (_sliderSectionX == 2 && _sliderSectionY == 1) {
            sbx = _sliderRectOld.x;
            sby = _sliderRectOld.y;
            sbw = _sliderRectOld.width + dx;
            sbh = _sliderRectOld.height;
            validMode = true;
        } else if (_sliderSectionX == 0 && _sliderSectionY == 2) {
            sbx = _sliderRectOld.x + dx;
            sby = _sliderRectOld.y;
            sbw = _sliderRectOld.width - dx;
            sbh = _sliderRectOld.height + dy;
            validMode = true;
        } else if (_sliderSectionX == 1 && _sliderSectionY == 2) {
            sbx = _sliderRectOld.x;
            sby = _sliderRectOld.y;
            sbw = _sliderRectOld.width;
            sbh = _sliderRectOld.height + dy;
            validMode = true;
        } else if (_sliderSectionX == 2 && _sliderSectionY == 2) {
            sbx = _sliderRectOld.x;
            sby = _sliderRectOld.y;
            sbw = _sliderRectOld.width + dx;
            sbh = _sliderRectOld.height + dy;
            validMode = true;
        }

        if (validMode && sbw > 2 && sbh > 2) {
            setSliderBoxBounds(sbx, sby, sbw, sbh, true);
        }
    }

    public static interface SliderBoxChangeListener {

        void sliderBoxChanged(Rectangle sliderBoxBounds);
    }
}

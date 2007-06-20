/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;

// @todo 2 nf/** - if orientation is vertical, sample values should increase from bottom to top
// @todo 1 nf/** - make PALETTE_HEIGHT a fixed value, fill space into gaps instead
// @todo 2 nf/** - draw header text vertically for vertical orientations
// @todo 3 nf/** - also draw legend into product scene view
//                 make "color legend properties" dialog a preferences page


/**
 * The <code>ImageLegend</code> class is used to generate an image legend from a <code>{@link
 * org.esa.beam.framework.datamodel.ImageInfo}</code> instance.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class ImageLegend {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private static final int _GAP = 10;
    private static final int _LABEL_GAP = 12;
    private static final int _SLIDER_WIDTH = 10;
    private static final int _SLIDER_HEIGHT = 14;

    private static final int _MIN_PALETTE_WIDTH = 256;
    private static final int _MIN_PALETTE_HEIGHT = 32;
    private static final int _MIN_LEGEND_WIDTH = 320;
    private static final int _MIN_LEGEND_HEIGHT = 48;

    private static final Font _DEFAULT_FONT = new Font("Arial", Font.BOLD, 14);

    // Independent attributes (Properties)
    private ImageInfo _imageInfo;
    private boolean _usingHeader;
    private String _headerText;
    private int _orientation;
    private Font _font;
    private Color _foregroundColor;
    private Color _backgroundColor;
    private boolean _backgroundTransparencyEnabled;
    private float _backgroundTransparency;
    private boolean _antialiasing;

    // Dependent, internal attributes
    private Rectangle _paletteRect;
    private Dimension _legendSize;
    private double _roundFactor;
    private Shape _sliderShape;
    private String[] _labels;
    private int[] _labelWidths;
    private int _palettePos1;
    private int _palettePos2;

    public ImageLegend(ImageInfo imageInfo) {
        setImageInfo(imageInfo);
        _usingHeader = true;
        _headerText = "";
        _orientation = HORIZONTAL;
        _font = _DEFAULT_FONT;
        _antialiasing = false;
        _backgroundColor = Color.white;
        _foregroundColor = Color.black;
        _backgroundTransparency = 0.0f;
    }

    public ImageInfo getImageInfo() {
        return _imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        _imageInfo = imageInfo;
        _roundFactor = _imageInfo.getRoundFactor(2);
    }

    public boolean isUsingHeader() {
        return _usingHeader;
    }

    public void setUsingHeader(boolean usingHeader) {
        _usingHeader = usingHeader;
    }

    public String getHeaderText() {
        return _headerText;
    }

    public void setHeaderText(String headerText) {
        _headerText = headerText;
    }

    public int getOrientation() {
        return _orientation;
    }

    public void setOrientation(int orientation) {
        _orientation = orientation;
    }

    public Font getFont() {
        return _font;
    }

    public void setFont(Font font) {
        _font = font;
    }

    public Color getBackgroundColor() {
        return _backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        _backgroundColor = backgroundColor;
    }

    public Color getForegroundColor() {
        return _foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        _foregroundColor = foregroundColor;
    }

    public boolean isAntialiasing() {
        return _antialiasing;
    }

    public void setAntialiasing(boolean antialiasing) {
        _antialiasing = antialiasing;
    }

    public boolean isBackgroundTransparencyEnabled() {
        return _backgroundTransparencyEnabled;
    }

    public void setBackgroundTransparencyEnabled(boolean backgroundTransparencyEnabled) {
        _backgroundTransparencyEnabled = backgroundTransparencyEnabled;
    }

    public float getBackgroundTransparency() {
        return _backgroundTransparency;
    }

    public void setBackgroundTransparency(float backgroundTransparency) {
        _backgroundTransparency = backgroundTransparency;
    }

    public boolean isAlphaUsed() {
        return _backgroundTransparencyEnabled && _backgroundTransparency > 0.0f && _backgroundTransparency <= 1.0f;
    }

    public int getBackgroundAlpha() {
        return isAlphaUsed() ? Math.round(255f * (1f - _backgroundTransparency)) : 255;
    }

    public BufferedImage createImage() {
        initDrawing();
        final BufferedImage bi = createBufferedImage(_legendSize.width, _legendSize.height);
        final Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        if (_antialiasing) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        if (_font != null) {
            g2d.setFont(_font);
        }
        draw(g2d);
        return bi;
    }

    private void initDrawing() {
        final FontMetrics fontMetrics = createFontMetrics();
        final int n = getNumGradationCurvePoints();
        _labels = new String[n];
        _labelWidths = new int[n];
        int textHeight = fontMetrics.getHeight();
        for (int i = 0; i < n; i++) {
            ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
            _labels[i] = String.valueOf(round(slider.getSample()));
            _labelWidths[i] = fontMetrics.stringWidth(_labels[i]);
        }

        int headerTextVSpace = 0;
        int headerTextWidth = 0;
        if (hasHeaderText()) {
            headerTextVSpace = textHeight + _GAP;
            headerTextWidth = fontMetrics.stringWidth(_headerText);
        }

        int legendWidth = 0;
        int legendHeight = 0;
        int maxLabelWidth = 0;
        for (int i = 0; i < n; i++) {
            legendWidth += _LABEL_GAP + _labelWidths[i];
            legendHeight += 2 * textHeight;
            maxLabelWidth = Math.max(_labelWidths[i], maxLabelWidth);
        }

        if (_orientation == HORIZONTAL) {
            legendWidth = Math.max(legendWidth, _MIN_PALETTE_HEIGHT);
            legendWidth = _GAP + Math.max(legendWidth, headerTextWidth) + _GAP;
            legendHeight = _GAP + headerTextVSpace + _MIN_PALETTE_HEIGHT + _LABEL_GAP + textHeight + _GAP;
            legendWidth = Math.max(_MIN_LEGEND_WIDTH, adjust(legendWidth, 16));
            legendHeight = Math.max(_MIN_LEGEND_HEIGHT, adjust(legendHeight, 16));
        } else {
            legendWidth = _MIN_PALETTE_HEIGHT + _LABEL_GAP + maxLabelWidth;
            legendWidth = _GAP + Math.max(legendWidth, headerTextWidth) + _GAP;
            legendHeight = _GAP + headerTextVSpace + Math.max(legendHeight, _MIN_PALETTE_WIDTH) + _LABEL_GAP + textHeight + _GAP;
            legendWidth = Math.max(_MIN_LEGEND_HEIGHT, adjust(legendWidth, 16));
            legendHeight = Math.max(_MIN_LEGEND_WIDTH, adjust(legendHeight, 16));
        }

        _legendSize = new Dimension(legendWidth, legendHeight);


        final int headerTextSpace = _headerText != null ? textHeight + _GAP : 0;
        final int labelTextSpace = _LABEL_GAP + textHeight;
        if (_orientation == HORIZONTAL) {
            _paletteRect = new Rectangle(_GAP,
                                         _GAP + headerTextSpace,
                                         _legendSize.width - (_GAP + _GAP),
                                         _legendSize.height - (_GAP + headerTextSpace + labelTextSpace + _GAP));
            int paletteGap = Math.max(_labelWidths[0], _labelWidths[n - 1]) / 2;
            _palettePos1 = _paletteRect.x + paletteGap;
            _palettePos2 = _paletteRect.x + _paletteRect.width - paletteGap;
        } else {
            _paletteRect = new Rectangle(_GAP,
                                         _GAP + headerTextSpace,
                                         _legendSize.width - (_GAP + labelTextSpace + maxLabelWidth + _GAP),
                                         _legendSize.height - (_GAP + headerTextSpace + _GAP));
            int paletteGap = Math.max(textHeight, _SLIDER_WIDTH) / 2;
            _palettePos1 = _paletteRect.y + paletteGap;
            _palettePos2 = _paletteRect.y + _paletteRect.height - paletteGap;
        }
        _sliderShape = createSliderShape();
    }

    private boolean hasHeaderText() {
        return _usingHeader && StringUtils.isNotNullAndNotEmpty(_headerText);
    }

    private void draw(Graphics2D g2d) {
        fillBackground(g2d);
        drawHeaderText(g2d);
        drawPalette(g2d);
        drawLabels(g2d);
    }

    private void fillBackground(Graphics2D g2d) {
        Color c = _backgroundColor;
        if (isAlphaUsed()) {
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), getBackgroundAlpha());
        }
        g2d.setColor(c);
        g2d.fillRect(0, 0, _legendSize.width + 1, _legendSize.height + 1);
    }

    private void drawHeaderText(Graphics2D g2d) {
        if (hasHeaderText()) {
            final FontMetrics fontMetrics = g2d.getFontMetrics();
            int x0 = _GAP;
            int y0 = _GAP + fontMetrics.getMaxAscent();
            g2d.setPaint(_foregroundColor);
            g2d.drawString(_headerText, x0, y0);
        }
    }

    private void drawPalette(Graphics2D g2d) {
        _imageInfo.computeColorPalette();
        final Color[] palette = _imageInfo.getColorPalette();
        final int x1 = _paletteRect.x;
        final int x2 = _paletteRect.x + _paletteRect.width;
        final int y1 = _paletteRect.y;
        final int y2 = _paletteRect.y + _paletteRect.height;
        final int i1, i2;
        if (_orientation == HORIZONTAL) {
            i1 = x1;
            i2 = x2;
        } else {
            i1 = y1;
            i2 = y2;
        }
        g2d.setStroke(new BasicStroke(1));
        for (int i = i1; i < i2; i++) {
            long divisor = _palettePos2 - _palettePos1;
            int palIndex = 0;
            if (divisor == 0) {
                palIndex = i < _palettePos1 ? 0 : palette.length - 1;
            } else {
                palIndex = (int) ((palette.length * (i - _palettePos1)) / divisor);
            }
            if (palIndex < 0) {
                palIndex = 0;
            }
            if (palIndex > palette.length - 1) {
                palIndex = palette.length - 1;
            }
            g2d.setColor(palette[palIndex]);
            if (_orientation == HORIZONTAL) {
                g2d.drawLine(i, y1, i, y2);
            } else {
                g2d.drawLine(x1, i, x2, i);
            }
        }
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(_foregroundColor);
        g2d.draw(_paletteRect);
    }

    private void drawLabels(Graphics2D g2d) {
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int n = getNumGradationCurvePoints();
        g2d.setStroke(new BasicStroke(2));
        Color c1 = (_foregroundColor != null ? _foregroundColor : Color.black).brighter();
        Color c2 = (_backgroundColor != null ? _backgroundColor : Color.white).darker();
        for (int i = 0; i < n; i++) {

            ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
            final double normalizedSample = getImageInfo().getNormalizedDisplaySampleValue(slider.getSample());
            double sliderPos = normalizedSample * (_palettePos2 - _palettePos1);
            Debug.trace("ImageLegend: normalizedSample = " + normalizedSample + ", sliderPos = " + sliderPos);

            double tx, ty;
            if (_orientation == HORIZONTAL) {
                tx = _palettePos1 + sliderPos;
                ty = _paletteRect.y + _paletteRect.height;
            } else {
                tx = _paletteRect.x + _paletteRect.width;
                ty = _palettePos1 + sliderPos;
            }
            g2d.translate(tx, ty);

            g2d.setPaint(slider.getColor());
            g2d.fill(_sliderShape);

            int gray = (slider.getColor().getRed() + slider.getColor().getGreen() + slider.getColor().getBlue()) / 3;
            g2d.setColor(gray < 128 ? c2 : c1);
            g2d.draw(_sliderShape);

            float x0, y0;
            if (_orientation == HORIZONTAL) {
                x0 = -0.5f * _labelWidths[i];
                y0 = _LABEL_GAP + fontMetrics.getMaxAscent();
            } else {
                x0 = _LABEL_GAP;
                y0 = fontMetrics.getMaxAscent();
            }
            g2d.setPaint(_foregroundColor);
            g2d.drawString(_labels[i], x0, y0);

            g2d.translate(-tx, -ty);
        }
    }

    private Shape createSliderShape() {
        GeneralPath path = new GeneralPath();
        if (_orientation == HORIZONTAL) {
            path.moveTo(0.0F, -0.5F * _SLIDER_HEIGHT);
            path.lineTo(+0.5F * _SLIDER_WIDTH, +0.5F * _SLIDER_HEIGHT);
            path.lineTo(-0.5F * _SLIDER_WIDTH, +0.5F * _SLIDER_HEIGHT);
        } else {
            path.moveTo(-0.5F * _SLIDER_HEIGHT, 0.0F);
            path.lineTo(+0.5F * _SLIDER_HEIGHT, +0.5F * _SLIDER_WIDTH);
            path.lineTo(+0.5F * _SLIDER_HEIGHT, -0.5F * _SLIDER_WIDTH);
        }
        path.closePath();
        return path;
    }

    private int getNumGradationCurvePoints() {
        return getImageInfo().getColorPaletteDef().getNumPoints();
    }

    private ColorPaletteDef.Point getGradationCurvePointAt(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index);
    }

    private double round(double value) {
        return MathUtils.round(value, _roundFactor);
    }

    private int adjust(int imageWidth, final int blockSize) {
        return blockSize * (imageWidth / blockSize) + (imageWidth % blockSize == 0 ? 0 : blockSize);
    }

    private FontMetrics createFontMetrics() {
        BufferedImage bi = createBufferedImage(32, 32);
        final Graphics2D g2d = bi.createGraphics();
        if (_font != null) {
            g2d.setFont(_font);
        }
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        g2d.dispose();
        return fontMetrics;
    }

    private BufferedImage createBufferedImage(final int width, final int height) {
        return new BufferedImage(width, height,
                                 isAlphaUsed() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    }

}

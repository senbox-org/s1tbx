/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.MathUtils;

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

// @todo 2 nf/** - if orientation is vertical, sample values should increase from bottom to top
// @todo 1 nf/** - make PALETTE_HEIGHT a fixed value, fill space into gaps instead
// @todo 2 nf/** - draw header text vertically for vertical orientations
// @todo 3 nf/** - also draw legend into product scene view
//                 make "color legend properties" dialog a preferences page


/**
 * The <code>ImageLegend</code> class is used to generate an image legend from a <code>{@link
 * ImageInfo}</code> instance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ImageLegend {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private static final int GAP = 10;
    private static final int LABEL_GAP = 12;
    private static final int SLIDER_WIDTH = 10;
    private static final int SLIDER_HEIGHT = 14;

    private static final int MIN_PALETTE_WIDTH = 256;
    private static final int MIN_PALETTE_HEIGHT = 32;
    private static final int MIN_LEGEND_WIDTH = 320;
    private static final int MIN_LEGEND_HEIGHT = 48;

    private static final Font _DEFAULT_FONT = new Font("Arial", Font.BOLD, 14);

    // Independent attributes (Properties)
    private final ImageInfo imageInfo;
    private final RasterDataNode raster;
    private boolean usingHeader;
    private String headerText;
    private int orientation;
    private Font font;
    private Color foregroundColor;
    private Color backgroundColor;
    private boolean backgroundTransparencyEnabled;
    private float backgroundTransparency;
    private boolean antialiasing;

    // Dependent, internal attributes
    private Rectangle paletteRect;
    private Dimension legendSize;
    private Shape sliderShape;
    private String[] labels;
    private int[] labelWidths;
    private int palettePos1;
    private int palettePos2;

    public ImageLegend(ImageInfo imageInfo, RasterDataNode raster) {
        this.imageInfo = imageInfo;
        this.raster = raster;
        usingHeader = true;
        headerText = "";
        orientation = HORIZONTAL;
        font = _DEFAULT_FONT;
        antialiasing = false;
        backgroundColor = Color.white;
        foregroundColor = Color.black;
        backgroundTransparency = 0.0f;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public RasterDataNode getRaster() {
        return raster;
    }

    public boolean isUsingHeader() {
        return usingHeader;
    }

    public void setUsingHeader(boolean usingHeader) {
        this.usingHeader = usingHeader;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    public boolean isBackgroundTransparencyEnabled() {
        return backgroundTransparencyEnabled;
    }

    public void setBackgroundTransparencyEnabled(boolean backgroundTransparencyEnabled) {
        this.backgroundTransparencyEnabled = backgroundTransparencyEnabled;
    }

    public float getBackgroundTransparency() {
        return backgroundTransparency;
    }

    public void setBackgroundTransparency(float backgroundTransparency) {
        this.backgroundTransparency = backgroundTransparency;
    }

    public boolean isAlphaUsed() {
        return backgroundTransparencyEnabled && backgroundTransparency > 0.0f && backgroundTransparency <= 1.0f;
    }

    public int getBackgroundAlpha() {
        return isAlphaUsed() ? Math.round(255f * (1f - backgroundTransparency)) : 255;
    }

    public BufferedImage createImage() {
        initDrawing();
        final BufferedImage bi = createBufferedImage(legendSize.width, legendSize.height);
        final Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        if (antialiasing) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        if (font != null) {
            g2d.setFont(font);
        }
        draw(g2d);
        return bi;
    }

    private void initDrawing() {
        final FontMetrics fontMetrics = createFontMetrics();
        final int n = getNumGradationCurvePoints();
        labels = new String[n];
        labelWidths = new int[n];
        int textHeight = fontMetrics.getHeight();
        final double minValue = imageInfo.getColorPaletteDef().getMinDisplaySample();
        final double maxValue = imageInfo.getColorPaletteDef().getMaxDisplaySample();
        double roundFactor = MathUtils.computeRoundFactor(minValue, maxValue, 2);
        for (int i = 0; i < n; i++) {
            ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
            labels[i] = String.valueOf(MathUtils.round(slider.getSample(), roundFactor));
            labelWidths[i] = fontMetrics.stringWidth(labels[i]);
        }

        int headerTextVSpace = 0;
        int headerTextWidth = 0;
        if (hasHeaderText()) {
            headerTextVSpace = textHeight + GAP;
            headerTextWidth = fontMetrics.stringWidth(headerText);
        }

        int legendWidth = 0;
        int legendHeight = 0;
        int maxLabelWidth = 0;
        for (int i = 0; i < n; i++) {
            legendWidth += LABEL_GAP + labelWidths[i];
            legendHeight += 2 * textHeight;
            maxLabelWidth = Math.max(labelWidths[i], maxLabelWidth);
        }

        if (orientation == HORIZONTAL) {
            legendWidth = Math.max(legendWidth, MIN_PALETTE_HEIGHT);
            legendWidth = GAP + Math.max(legendWidth, headerTextWidth) + GAP;
            legendHeight = GAP + headerTextVSpace + MIN_PALETTE_HEIGHT + LABEL_GAP + textHeight + GAP;
            legendWidth = Math.max(MIN_LEGEND_WIDTH, adjust(legendWidth, 16));
            legendHeight = Math.max(MIN_LEGEND_HEIGHT, adjust(legendHeight, 16));
        } else {
            legendWidth = MIN_PALETTE_HEIGHT + LABEL_GAP + maxLabelWidth;
            legendWidth = GAP + Math.max(legendWidth, headerTextWidth) + GAP;
            legendHeight = GAP + headerTextVSpace + Math.max(legendHeight, MIN_PALETTE_WIDTH) + LABEL_GAP + textHeight + GAP;
            legendWidth = Math.max(MIN_LEGEND_HEIGHT, adjust(legendWidth, 16));
            legendHeight = Math.max(MIN_LEGEND_WIDTH, adjust(legendHeight, 16));
        }

        legendSize = new Dimension(legendWidth, legendHeight);


        final int headerTextSpace = headerText != null ? textHeight + GAP : 0;
        final int labelTextSpace = LABEL_GAP + textHeight;
        if (orientation == HORIZONTAL) {
            paletteRect = new Rectangle(GAP,
                                        GAP + headerTextSpace,
                                        legendSize.width - (GAP + GAP),
                                        legendSize.height - (GAP + headerTextSpace + labelTextSpace + GAP));
            int paletteGap = Math.max(labelWidths[0], labelWidths[n - 1]) / 2;
            palettePos1 = paletteRect.x + paletteGap;
            palettePos2 = paletteRect.x + paletteRect.width - paletteGap;
        } else {
            paletteRect = new Rectangle(GAP,
                                        GAP + headerTextSpace,
                                        legendSize.width - (GAP + labelTextSpace + maxLabelWidth + GAP),
                                        legendSize.height - (GAP + headerTextSpace + GAP));
            int paletteGap = Math.max(textHeight, SLIDER_WIDTH) / 2;
            palettePos1 = paletteRect.y + paletteGap;
            palettePos2 = paletteRect.y + paletteRect.height - paletteGap;
        }
        sliderShape = createSliderShape();
    }

    private boolean hasHeaderText() {
        return usingHeader && StringUtils.isNotNullAndNotEmpty(headerText);
    }

    private void draw(Graphics2D g2d) {
        fillBackground(g2d);
        drawHeaderText(g2d);
        drawPalette(g2d);
        drawLabels(g2d);
    }

    private void fillBackground(Graphics2D g2d) {
        Color c = backgroundColor;
        if (isAlphaUsed()) {
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), getBackgroundAlpha());
        }
        g2d.setColor(c);
        g2d.fillRect(0, 0, legendSize.width + 1, legendSize.height + 1);
    }

    private void drawHeaderText(Graphics2D g2d) {
        if (hasHeaderText()) {
            final FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.setPaint(foregroundColor);
            int x0 = GAP;
            int y0 = GAP + fontMetrics.getMaxAscent();
            g2d.drawString(headerText, x0, y0);
        }
    }

    private void drawPalette(Graphics2D g2d) {
        final Color[] palette = ImageManager.createColorPalette(getRaster().getImageInfo());
//        final Color[] palette = imageInfo.getColorPaletteDef().createColorPalette(getRaster());
        final int x1 = paletteRect.x;
        final int x2 = paletteRect.x + paletteRect.width;
        final int y1 = paletteRect.y;
        final int y2 = paletteRect.y + paletteRect.height;
        final int i1;
        final int i2;
        if (orientation == HORIZONTAL) {
            i1 = x1;
            i2 = x2;
        } else {
            i1 = y1;
            i2 = y2;
        }
        g2d.setStroke(new BasicStroke(1));
        for (int i = i1; i < i2; i++) {
            int divisor = palettePos2 - palettePos1;
            int palIndex;
            if (divisor == 0) {
                palIndex = i < palettePos1 ? 0 : palette.length - 1;
            } else {
                palIndex = (palette.length * (i - palettePos1)) / divisor;
            }
            if (palIndex < 0) {
                palIndex = 0;
            }
            if (palIndex > palette.length - 1) {
                palIndex = palette.length - 1;
            }
            g2d.setColor(palette[palIndex]);
            if (orientation == HORIZONTAL) {
                g2d.drawLine(i, y1, i, y2);
            } else {
                g2d.drawLine(x1, i, x2, i);
            }
        }
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(foregroundColor);
        g2d.draw(paletteRect);
    }

    private void drawLabels(Graphics2D g2d) {
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int n = getNumGradationCurvePoints();
        g2d.setStroke(new BasicStroke(2));
        Color c1 = (foregroundColor != null ? foregroundColor : Color.black).brighter();
        Color c2 = (backgroundColor != null ? backgroundColor : Color.white).darker();
        for (int i = 0; i < n; i++) {

            ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
            final double normalizedSample = normalizeSample(slider.getSample());
            double sliderPos = normalizedSample * (palettePos2 - palettePos1);

            double tx;
            double ty;
            if (orientation == HORIZONTAL) {
                tx = palettePos1 + sliderPos;
                ty = paletteRect.y + paletteRect.height;
            } else {
                tx = paletteRect.x + paletteRect.width;
                ty = palettePos1 + sliderPos;
            }
            g2d.translate(tx, ty);

            g2d.setPaint(slider.getColor());
            g2d.fill(sliderShape);

            int gray = (slider.getColor().getRed() + slider.getColor().getGreen() + slider.getColor().getBlue()) / 3;
            g2d.setColor(gray < 128 ? c2 : c1);
            g2d.draw(sliderShape);

            float x0;
            float y0;
            if (orientation == HORIZONTAL) {
                x0 = -0.5f * labelWidths[i];
                y0 = LABEL_GAP + fontMetrics.getMaxAscent();
            } else {
                x0 = LABEL_GAP;
                y0 = fontMetrics.getMaxAscent();
            }
            g2d.setPaint(foregroundColor);
            g2d.drawString(labels[i], x0, y0);

            g2d.translate(-tx, -ty);
        }
    }

    private double normalizeSample(double sample) {
        double minDisplaySample = getImageInfo().getColorPaletteDef().getMinDisplaySample();
        double maxDisplaySample = getImageInfo().getColorPaletteDef().getMaxDisplaySample();
        if (imageInfo.isLogScaled()) {
            minDisplaySample = Math.log10(imageInfo.getColorPaletteDef().getMinDisplaySample());
            maxDisplaySample = Math.log10(imageInfo.getColorPaletteDef().getMaxDisplaySample());
            sample = Math.log10(sample);
        }

        double delta = maxDisplaySample - minDisplaySample;
        if (delta == 0 || Double.isNaN(delta)) {
            delta = 1;
        }
        return (sample - minDisplaySample) / delta;
    }

    private Shape createSliderShape() {
        GeneralPath path = new GeneralPath();
        if (orientation == HORIZONTAL) {
            path.moveTo(0.0F, -0.5F * SLIDER_HEIGHT);
            path.lineTo(+0.5F * SLIDER_WIDTH, +0.5F * SLIDER_HEIGHT);
            path.lineTo(-0.5F * SLIDER_WIDTH, +0.5F * SLIDER_HEIGHT);
        } else {
            path.moveTo(-0.5F * SLIDER_HEIGHT, 0.0F);
            path.lineTo(+0.5F * SLIDER_HEIGHT, +0.5F * SLIDER_WIDTH);
            path.lineTo(+0.5F * SLIDER_HEIGHT, -0.5F * SLIDER_WIDTH);
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

    private static int adjust(int size, final int blockSize) {
        return blockSize * (size / blockSize) + (size % blockSize == 0 ? 0 : blockSize);
    }

    private FontMetrics createFontMetrics() {
        BufferedImage bi = createBufferedImage(32, 32);
        final Graphics2D g2d = bi.createGraphics();
        if (font != null) {
            g2d.setFont(font);
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

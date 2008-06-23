/*
 * $Id: ContrastStretchPane.java,v 1.1 2007/04/19 10:41:38 norman Exp $
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
package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.BorderLayoutUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import com.bc.ceres.core.Assert;


class GraphicalPaletteEditor extends JPanel {
    public static final String PROPERTY_NAME_IMAGE_INFO = "imageInfo";

    public static final String NO_DISPLAY_INFO_TEXT = "No image information available.";
    public static final String FONT_NAME = "Verdana";
    public static final int FONT_SIZE = 9;
    public static final int INVALID_INDEX = -1;
    public static final int PALETTE_HEIGHT = 16;
    public static final int SLIDER_WIDTH = 12;
    public static final int SLIDER_HEIGHT = 10;
    public static final int SLIDER_VALUES_AREA_HEIGHT = 40;
    public static final int HOR_BORDER_SIZE = 10;
    public static final int VER_BORDER_SIZE = 4;
    public static final int PREF_HISTO_WIDTH = 256; //196;
    public static final int PREF_HISTO_HEIGHT = 196; //128;
    public static final Dimension PREF_COMPONENT_SIZE
            = new Dimension(PREF_HISTO_WIDTH + 2 * HOR_BORDER_SIZE,
                            PREF_HISTO_HEIGHT + PALETTE_HEIGHT + SLIDER_HEIGHT / 2
                                    + 2 * HOR_BORDER_SIZE + FONT_SIZE);
    public static final BasicStroke STROKE_1 = new BasicStroke(1.0f);
    public static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
    public static final BasicStroke DASHED_STROKE = new BasicStroke(0.75F, BasicStroke.CAP_SQUARE,
                                                                    BasicStroke.JOIN_MITER, 1.0F, new float[]{5.0F},
                                                                    0.0F);
    private ImageInfo imageInfo;

    private Font labelFont;
    private final Shape sliderShape;
    private int sliderTextBaseLineY;
    private final Rectangle sliderBaseLineRect;
    private final Rectangle paletteRect;
    private final Rectangle histoRect;
    private float roundFactor;
    private final InternalMouseListener internalMouseListener;
    private String unit;
    private double[] factors;
    private Color rgbColor;
    private Color[] rgbColorPal;
    private byte[] gammaCurve;

    public GraphicalPaletteEditor() {
        labelFont = createLabelFont();
        sliderShape = createSliderShape();
        sliderBaseLineRect = new Rectangle();
        paletteRect = new Rectangle();
        histoRect = new Rectangle();
        internalMouseListener = new InternalMouseListener();
        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(final ImageInfo imageInfo) {
        Assert.notNull(imageInfo, "imageInfo");
        final ImageInfo oldImageInfo = this.imageInfo;
        if (this.imageInfo != imageInfo) {
            this.imageInfo = imageInfo;
            deinstallMouseListener();
            if (this.imageInfo != null) {
                roundFactor = (float) this.imageInfo.getRoundFactor(2);
                installMouseListener();
            }
            updateGamma();
            firePropertyChange(PROPERTY_NAME_IMAGE_INFO, oldImageInfo, this.imageInfo);
            fireStateChanged();
        }
        if (isShowing()) {
            repaint();
        }
    }

    /**
     * Adds a ChangeListener to the slider.
     *
     * @param l the ChangeListener to add
     * @see #fireStateChanged
     * @see #removeChangeListener
     * @since 4.2
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Removes a ChangeListener from the slider.
     *
     * @param l the ChangeListener to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     * @since 4.2
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this JSlider with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 4.2
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }


    /**
     * Send a {@code ChangeEvent}, whose source is this {@code JSlider}, to
     * all {@code ChangeListener}s that have registered interest in
     * {@code ChangeEvent}s.
     * This method is called each time a {@code ChangeEvent} is received from
     * the model.
     * <p>
     * The event instance is created if necessary, and stored in
     * {@code changeEvent}.
     *
     * @see #addChangeListener
     * @see javax.swing.event.EventListenerList
     * @since 4.2
     */
    protected void fireStateChanged() {
        final ChangeEvent event = new ChangeEvent(this);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==ChangeListener.class) {
                ((ChangeListener)listeners[i+1]).stateChanged(event);
            }
        }
    }

    public void updateGamma() {
        if (imageInfo.isGammaActive() && isRGBMode()) {
            gammaCurve = MathUtils.createGammaCurve(imageInfo.getGamma(), null);
        } else {
            gammaCurve = null;
        }
        fireStateChanged();
    }

    public void setRGBColor(Color rgbColor) {
        this.rgbColor = rgbColor;
    }

    public void compute95Percent() {
        final Histogram histogram = new Histogram(getImageInfo().getHistogramBins(),
                                                  scaleInverse(getImageInfo().getMinSample()),
                                                  scaleInverse(getImageInfo().getMaxSample()));
        final Range autoStretchRange = histogram.findRangeFor95Percent(true);
        computeFactors();
        getFirstGradationCurvePoint().setSample(scale(autoStretchRange.getMin()));
        getLastGradationCurvePoint().setSample(scale(autoStretchRange.getMax()));
        computePartitioning();
        computeZoomInToSliderLimits();
    }

    public void compute100Percent() {
        computeFactors();
        getFirstGradationCurvePoint().setSample(getImageInfo().getMinSample());
        getLastGradationCurvePoint().setSample(getImageInfo().getMaxSample());
        computePartitioning();
        computeZoomInToSliderLimits();
    }

    public void distributeSlidersEvenly() {
        final double firstSliderPos = scaleInverse(getFirstGradationCurvePoint().getSample());
        final double lastSliderPos = scaleInverse(getLastGradationCurvePoint().getSample());
        final double maxDistance = lastSliderPos - firstSliderPos;
        final double evenSpace = maxDistance / (getNumGradationCurvePoints() - 2 + 1);
        for (int i = 0; i < getNumGradationCurvePoints(); i++) {
            final double value = scale(firstSliderPos + evenSpace * i);
            setGradationPointSampleAt(i, value);
        }
    }

    private void computePartitioning() {
        final double firstPS = scaleInverse(getFirstGradationCurvePoint().getSample());
        final double lastPS = scaleInverse(getLastGradationCurvePoint().getSample());
        final double dsn = lastPS - firstPS;
        for (int i = 0; i < (getNumGradationCurvePoints() - 1); i++) {
            final double value = scale(firstPS + factors[i] * dsn);
            setGradationPointSampleAt(i, value);
        }
    }

    private void computeFactors() {
        factors = new double[getNumGradationCurvePoints()];
        final double firstPS = scaleInverse(getFirstGradationCurvePoint().getSample());
        final double lastPS = scaleInverse(getLastGradationCurvePoint().getSample());
        double dsn = lastPS - firstPS;
        if (dsn == 0 || Double.isNaN(dsn)) {
            dsn = Double.MIN_VALUE;
        }
        for (int i = 0; i < getNumGradationCurvePoints(); i++) {
            final double sample = scaleInverse(getGradationCurvePointAt(i).getSample());
            final double dsi = sample - firstPS;
            factors[i] = dsi / dsn;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return PREF_COMPONENT_SIZE;
    }

    @Override
    public void setBounds(int x, int y, int width, int heigth) {
        super.setBounds(x, y, width, heigth);
        computeSizeAttributes();
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void computeZoomInToSliderLimits() {
        final double firstSliderValue;
        final double lastSliderValue;
        firstSliderValue = scaleInverse(getFirstGradationCurvePoint().getSample());
        lastSliderValue = scaleInverse(getLastGradationCurvePoint().getSample());
        final double tenPercentOffset = (lastSliderValue - firstSliderValue) / 80 * 10;
        final float minViewSample;
        final float maxViewSample;
        minViewSample = (float) scale(firstSliderValue - tenPercentOffset);
        maxViewSample = (float) scale(lastSliderValue + tenPercentOffset);

        getImageInfo().setMinHistogramViewSample(Math.max(minViewSample, getMinSample()));
        getImageInfo().setMaxHistogramViewSample(Math.min(maxViewSample, getMaxSample()));
        repaint();
    }

    public void computeZoomOutToFullHistogramm() {
        getImageInfo().setMinHistogramViewSample(getMinSample());
        getImageInfo().setMaxHistogramViewSample(getMaxSample());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        if (!(g instanceof Graphics2D)) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setFont(labelFont);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        computeSizeAttributes();

        if (getImageInfo() != null && isValidHistogramm()) {
            drawPalette(g2d);
            drawSliders(g2d);
            drawHistogramPane(g2d);
        } else {
            drawMissingBasicDisplayInfoMessage(g2d, fontMetrics);
        }
    }

    private boolean isValidHistogramm() {
        Histogram histogram = getImageInfo().getHistogram();
        return histogram.getMin() <= histogram.getMax();
    }

    public void computeZoomOutVertical() {
        getImageInfo().setHistogramViewGain(getImageInfo().getHistogramViewGain() * (1f / 1.4f));
        repaint();
    }

    public void computeZoomInVertical() {
        getImageInfo().setHistogramViewGain(getImageInfo().getHistogramViewGain() * (1f * 1.4f));
        repaint();
    }

    private void drawMissingBasicDisplayInfoMessage(Graphics2D g2d, FontMetrics fontMetrics) {
        int totWidth = getWidth();
        int totHeight = getHeight();
        g2d.drawString(NO_DISPLAY_INFO_TEXT,
                       (totWidth - fontMetrics.stringWidth(NO_DISPLAY_INFO_TEXT)) / 2,
                       (totHeight + fontMetrics.getHeight()) / 2);
    }

    private void drawPalette(Graphics2D g2d) {
        long paletteX1 = paletteRect.x + Math.round(getRelativeSliderPos(getFirstGradationCurvePoint()));
        long paletteX2 = paletteRect.x + Math.round(getRelativeSliderPos(getLastGradationCurvePoint()));
        g2d.setStroke(STROKE_1);
        computeColorPalette();
        Color[] palette = getColorPalette();
        if (palette != null) {
            for (int x = paletteRect.x; x < paletteRect.x + paletteRect.width; x++) {
                long divisor = paletteX2 - paletteX1;
                int palIndex;
                if (divisor == 0) {
                    palIndex = x < paletteX1 ? 0 : palette.length - 1;
                } else {
                    palIndex = (int) ((palette.length * (x - paletteX1)) / divisor);
                }
                if (palIndex < 0) {
                    palIndex = 0;
                }
                if (palIndex > palette.length - 1) {
                    palIndex = palette.length - 1;
                }
                g2d.setColor(palette[palIndex]);
                g2d.drawLine(x, paletteRect.y, x, paletteRect.y + paletteRect.height);
            }
        }
        g2d.setStroke(STROKE_1);
        g2d.setColor(Color.darkGray);
        g2d.draw(paletteRect);
    }

    private Color[] getColorPalette() {
        if (isRGBMode()) {
            return rgbColorPal;
        } else {
            return getImageInfo().getColorPalette();
        }
    }

    private void drawSliders(Graphics2D g2d) {
        g2d.translate(sliderBaseLineRect.x, sliderBaseLineRect.y);
        g2d.setStroke(STROKE_1);
        if (isRGBMode()) {
            drawSlidersForRGBDisplay(g2d);
        } else {
            drawSlidersForMonoDisplay(g2d);
        }
        g2d.translate(-sliderBaseLineRect.x, -sliderBaseLineRect.y);
    }

    private void drawSlidersForMonoDisplay(Graphics2D g2d) {
        for (int i = 0; i < getNumGradationCurvePoints(); i++) {

            ColorPaletteDef.Point slider = getGradationCurvePointAt(i);

            double sliderPos = getRelativeSliderPos(slider);
            g2d.translate(sliderPos, 0.0d);

            g2d.setPaint(slider.getColor());
            g2d.fill(sliderShape);

            int gray = (slider.getColor().getRed() + slider.getColor().getGreen() + slider.getColor().getBlue()) / 3;
            g2d.setColor(gray < 128 ? Color.white : Color.black);
            g2d.draw(sliderShape);

            String text = String.valueOf(round(slider.getSample()));
            g2d.setColor(Color.black);
            // save the old transformation
            final AffineTransform oldTransform = g2d.getTransform();
            g2d.transform(AffineTransform.getRotateInstance(Math.PI / 2));
            g2d.drawString(text, 3 + 0.5f * SLIDER_HEIGHT, 0.35f * FONT_SIZE);
            // restore the old transformation
            g2d.setTransform(oldTransform);

            g2d.translate(-sliderPos, 0.0d);
        }
    }

    private void drawSlidersForRGBDisplay(Graphics2D g2d) {
        ColorPaletteDef.Point slider;
        double sliderPos;
        for (int i = 0; i < 2; i++) {
            Color triangleColor;
            if (i == 0) {
                slider = getFirstGradationCurvePoint();
                g2d.setPaint(Color.black);
                triangleColor = Color.white;
            } else {
                slider = getLastGradationCurvePoint();
                g2d.setPaint(rgbColor);
                triangleColor = Color.black;
            }
            sliderPos = getRelativeSliderPos(slider);
            g2d.translate(sliderPos, 0.0F);
            g2d.fill(sliderShape);
            g2d.setColor(triangleColor);
            g2d.draw(sliderShape);
            g2d.setColor(Color.black);

            String text = String.valueOf(round(slider.getSample()));

            // save the old transformation
            AffineTransform oldTransform = g2d.getTransform();
            g2d.transform(AffineTransform.getRotateInstance(Math.PI / 2));
            g2d.drawString(text, 3 + 0.5f * SLIDER_HEIGHT, 0.35f * FONT_SIZE);
            // restore the old transformation
            g2d.setTransform(oldTransform);

            g2d.translate(-sliderPos, 0.0);
        }
    }

    private void drawHistogramPane(Graphics2D g2d) {
        Shape oldClip = g2d.getClip();
        g2d.setClip(histoRect.x - 1, histoRect.y - 1, histoRect.width + 2, histoRect.height + 2);

        drawHistogram(g2d);
        drawGradationCurve(g2d);
        drawHistogramText(g2d);
        drawHistogramBorder(g2d);

        g2d.setClip(oldClip);
    }

    private void drawHistogramBorder(Graphics2D g2d) {
        g2d.setStroke(STROKE_1);
        g2d.setColor(Color.darkGray);
        g2d.draw(histoRect);
    }

    private void drawHistogramText(Graphics2D g2d) {
        final FontMetrics metrics = g2d.getFontMetrics();
        final String sMin = "min:";
        final String sMax = "max:";
        final String sUnit = "unit:";
        final int sMinWidth = metrics.stringWidth(sMin);
        final int sMaxWidth = metrics.stringWidth(sMax);
        final int sUnitWidth = metrics.stringWidth(sUnit);

        final String sMinValue = String.valueOf(round(getMinSample()));
        final String sMaxValue = String.valueOf(round(getMaxSample()));
        final int sMinValueWidth = metrics.stringWidth(sMinValue);
        final int sMaxValueWidth = metrics.stringWidth(sMaxValue);

        int maxPrefixWidth = Math.max(sMinWidth, sMaxWidth);
        int maxValueWidth = Math.max(sMinValueWidth, sMaxValueWidth);

        final String sUnitValue;

        if (unit != null && unit.length() > 0) {
            sUnitValue = unit;
            maxPrefixWidth = Math.max(maxPrefixWidth, sUnitWidth);
            final int sUnitValueWidth = metrics.stringWidth(sUnitValue);
            maxValueWidth = Math.max(maxValueWidth, sUnitValueWidth);
        } else {
            sUnitValue = "";
        }
        final int xStartPosPrefix = histoRect.x + histoRect.width - 5 - maxPrefixWidth - maxValueWidth;
        final int xStartPosValues = histoRect.x + histoRect.width - 2 - maxValueWidth;

        String[] strings = new String[]{sMin, sMinValue, sMax, sMaxValue, sUnit, sUnitValue};

        final int yStartPos = histoRect.y + 1;

        g2d.setColor(this.getBackground());
        drawStrings(g2d, strings, xStartPosPrefix - 1, xStartPosValues - 1, yStartPos);
        drawStrings(g2d, strings, xStartPosPrefix - 1, xStartPosValues - 1, yStartPos - 1);
        drawStrings(g2d, strings, xStartPosPrefix - 1, xStartPosValues - 1, yStartPos + 1);
        drawStrings(g2d, strings, xStartPosPrefix + 1, xStartPosValues + 1, yStartPos);
        drawStrings(g2d, strings, xStartPosPrefix + 1, xStartPosValues + 1, yStartPos - 1);
        drawStrings(g2d, strings, xStartPosPrefix + 1, xStartPosValues + 1, yStartPos + 1);
        drawStrings(g2d, strings, xStartPosPrefix, xStartPosValues, yStartPos - 1);
        drawStrings(g2d, strings, xStartPosPrefix, xStartPosValues, yStartPos + 1);
        g2d.setColor(this.getForeground());
        drawStrings(g2d, strings, xStartPosPrefix, xStartPosValues, yStartPos);
    }

    private void drawStrings(Graphics2D g2d, String[] strings, int xStartPosPrefix, int xStartPosValues,
                             int yStartPos) {
        int line = 1;
        g2d.drawString(strings[0], xStartPosPrefix, yStartPos + FONT_SIZE * line);
        g2d.drawString(strings[1], xStartPosValues, yStartPos + FONT_SIZE * line);
        line++;
        g2d.drawString(strings[2], xStartPosPrefix, yStartPos + FONT_SIZE * line);
        g2d.drawString(strings[3], xStartPosValues, yStartPos + FONT_SIZE * line);
        if (!strings[5].equals("")) {
            line++;
            g2d.drawString(strings[4], xStartPosPrefix, yStartPos + FONT_SIZE * line);
            g2d.drawString(strings[5], xStartPosValues, yStartPos + FONT_SIZE * line);
        }
    }

    private void drawGradationCurve(Graphics2D g2d) {
        g2d.setColor(Color.white);
        g2d.setStroke(STROKE_2);

        ColorPaletteDef.Point point;
        int x1, y1, x2, y2;

        point = getFirstGradationCurvePoint();
        x1 = histoRect.x;
        y1 = histoRect.y + histoRect.height - 1;
        x2 = (int) getAbsoluteSliderPos(point);
        y2 = y1;
        g2d.drawLine(x1, y1, x2, y2);

        point = getLastGradationCurvePoint();
        x1 = x2;
        y1 = y2;
        x2 = (int) getAbsoluteSliderPos(point);
        y2 = histoRect.y + 1;
        if (gammaCurve != null) {
            int xx = x1;
            int yy = y1;
            for (int x = x1 + 1; x <= x2; x++) {
                int i = MathUtils.roundAndCrop((255.9f * (x - x1)) / (x2 - x1), 0, 255);
                int y = y1 + ((y2 - y1) * (gammaCurve[i] & 0xff)) / 256;
                g2d.drawLine(xx, yy, x, y);
                //System.out.println("x=" + x + ", y=" + y);
                xx = x;
                yy = y;
            }
        } else {
            g2d.drawLine(x1, y1, x2, y2);
        }

        x1 = x2;
        y1 = y2;
        x2 = histoRect.x + histoRect.width;
        y2 = histoRect.y + 1;
        g2d.drawLine(x1, y1, x2, y2);

        // Vertical lines
        g2d.setStroke(DASHED_STROKE);
        for (int i = 0; i < getNumGradationCurvePoints(); i++) {
            point = getGradationCurvePointAt(i);
            x1 = (int) getAbsoluteSliderPos(point);
            y1 = histoRect.y + histoRect.height - 1;
            x2 = x1;
            y2 = histoRect.y + 1;
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawHistogram(Graphics2D g2d) {
        g2d.setColor(Color.black);
        g2d.setPaint(Color.black);
        final ImageInfo imageInfo = getImageInfo();

        if (imageInfo.isHistogramAvailable()) {
            final int[] histogramBins = imageInfo.getHistogramBins();
            final float maxHistogramCounts = getMaxVisibleHistogramCounts(histogramBins, 1f / 16f);
            final float gain = imageInfo.getHistogramViewGain();
            final float firstVisibleBinIndexFloat = imageInfo.getFirstHistogramViewBinIndex();
            final int firstVisibleBinIndex = MathUtils.floorAndCrop(firstVisibleBinIndexFloat, 0,
                                                                    histogramBins.length - 1);
            final float indexDelta = firstVisibleBinIndexFloat - firstVisibleBinIndex;
            final float maxHistoRectHeight = 0.9f * histoRect.height;
            float numVisibleBins = imageInfo.getHistogramViewBinCount();
            if (numVisibleBins > 0 && maxHistogramCounts > 0) {
                g2d.setStroke(new BasicStroke(1.0F));
                final float binWidth = (float) histoRect.width / numVisibleBins;
                final float pixelOffs = indexDelta * binWidth;
                final float countsScale = (gain * maxHistoRectHeight) / maxHistogramCounts;
                if ((numVisibleBins + firstVisibleBinIndex) < histogramBins.length) {
                    numVisibleBins++;
                }
                Rectangle2D.Double r = new Rectangle2D.Double();
                for (int i = 0; i < (int) numVisibleBins; i++) {
                    final float counts = histogramBins[i + firstVisibleBinIndex];
                    float binHeight = countsScale * counts;
                    if (binHeight >= histoRect.height) {
                        // must crop here because on highly centered histograms this value is FAR beyond the rectangle
                        // and then triggers an exception when trying to draw it.
                        binHeight = histoRect.height - 1;
                    }
                    final float y1 = histoRect.y + histoRect.height - 1 - binHeight;

                    final float x1 = histoRect.x + binWidth * i - pixelOffs - 0.5f * binWidth;
                    r.setRect(x1, y1, binWidth, binHeight);
                    g2d.fill(r);
                }
            }
        }
    }

    private float getMaxVisibleHistogramCounts(final int[] histogramBins, float ratio) {
        float totalHistogramCounts = 0;
        for (int histogramBin : histogramBins) {
            totalHistogramCounts += histogramBin;
        }
        final float limitHistogramCounts = totalHistogramCounts * ratio;
        float maxHistogramCounts = 0f;
        for (int histogramBin : histogramBins) {
            if (histogramBin < limitHistogramCounts) {
                maxHistogramCounts = Math.max(maxHistogramCounts, histogramBin);
            }
        }
        return maxHistogramCounts;
    }

    private void installMouseListener() {
        addMouseListener(internalMouseListener);
        addMouseMotionListener(internalMouseListener);
    }

    private void deinstallMouseListener() {
        removeMouseListener(internalMouseListener);
        removeMouseMotionListener(internalMouseListener);
    }

    private int getNumGradationCurvePoints() {
        final ImageInfo imageInfo = getImageInfo();
        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        return colorPaletteDef.getNumPoints();
    }

    private ColorPaletteDef.Point getFirstGradationCurvePoint() {
        final ImageInfo imageInfo = getImageInfo();
        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        return colorPaletteDef.getFirstPoint();
    }

    private ColorPaletteDef.Point getLastGradationCurvePoint() {
        final ImageInfo imageInfo = getImageInfo();
        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        return colorPaletteDef.getLastPoint();
    }

    private ColorPaletteDef.Point getGradationCurvePointAt(int index) {
        final ImageInfo imageInfo = getImageInfo();
        final ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        return colorPaletteDef.getPointAt(index);
    }

    private void setGradationPointSampleAt(int index, double newValue) {
        ColorPaletteDef.Point p = getGradationCurvePointAt(index);
        double minValue = Double.NEGATIVE_INFINITY;
        if (index > 0 && index < (getNumGradationCurvePoints() - 1)) {
            ColorPaletteDef.Point lowerPoint = getGradationCurvePointAt(index - 1);
            minValue = lowerPoint.getSample() + Float.MIN_VALUE;
        }
        if (newValue < minValue) {
            newValue = minValue;
        }
        p.setSample(newValue);
        fireStateChanged();
    }

    private void setGradationPointColorAt(int index, Color newValue) {
        ColorPaletteDef.Point p = getGradationCurvePointAt(index);
        p.setColor(newValue);
        fireStateChanged();
    }

    private static Font createLabelFont() {
        return new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);
    }

    public static Shape createSliderShape() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0.0F, -0.5F * SLIDER_HEIGHT);
        path.lineTo(+0.5F * SLIDER_WIDTH, 0.5F * SLIDER_HEIGHT);
        path.lineTo(-0.5F * SLIDER_WIDTH, 0.5F * SLIDER_HEIGHT);
        path.closePath();
        return path;
    }

    private boolean isRGBMode() {
        return rgbColor != null;
    }

    private double computeSliderValueForX(int sliderIndex, int x) {
        final double minVS = scaleInverse(getImageInfo().getMinHistogramViewSample());
        final double maxVS = scaleInverse(getImageInfo().getMaxHistogramViewSample());
        final double value = scale(minVS + (x - sliderBaseLineRect.x) * (maxVS - minVS) / sliderBaseLineRect.width);
        if (isFirstSliderIndex(sliderIndex)) {
            return Math.min(value, getLastGradationCurvePoint().getSample());
        }
        if (isLastSliderIndex(sliderIndex)) {
            return Math.max(value, getFirstGradationCurvePoint().getSample());
        }
        return computeAdjustedSliderValue(sliderIndex, value);
    }

    private double computeAdjustedSliderValue(int sliderIndex, double value) {
        double valueD = value;
        double minSliderValue = getMinSliderValue(sliderIndex);
        minSliderValue += minSliderValue * 1e-8;
        double maxSliderValue = getMaxSliderValue(sliderIndex);
        maxSliderValue -= maxSliderValue * 1e-8;
        if (valueD < minSliderValue) {
            valueD = minSliderValue;
        }
        if (valueD > maxSliderValue) {
            valueD = maxSliderValue;
        }
        return valueD;
    }

    private boolean isLastSliderIndex(int sliderIndex) {
        return (getNumGradationCurvePoints() - 1) == sliderIndex;
    }

    private boolean isFirstSliderIndex(int sliderIndex) {
        return sliderIndex == 0;
    }

    private double round(double value) {
        return MathUtils.round(value, roundFactor);
    }

    private double getMinSliderValue(int sliderIndex) {
        if (sliderIndex == 0) {
            return getMinSample();
        } else {
            return getGradationCurvePointAt(sliderIndex - 1).getSample();
        }
    }

    private double getMaxSliderValue(int sliderIndex) {
        if (sliderIndex == getNumGradationCurvePoints() - 1) {
            return getMaxSample();
        } else {
            return getGradationCurvePointAt(sliderIndex + 1).getSample();
        }
    }

    private double getAbsoluteSliderPos(final ColorPaletteDef.Point point) {
        return sliderBaseLineRect.x + getRelativeSliderPos(point);
    }

    private double getRelativeSliderPos(final ColorPaletteDef.Point point) {
        return getImageInfo().getNormalizedHistogramViewSampleValue(point.getSample()) * sliderBaseLineRect.width;
    }

    private void computeSizeAttributes() {
        int totWidth = getWidth();
        int totHeight = getHeight();

        int imageWidth = totWidth - 2 * HOR_BORDER_SIZE;

        sliderTextBaseLineY = totHeight - VER_BORDER_SIZE - SLIDER_VALUES_AREA_HEIGHT;

        sliderBaseLineRect.x = HOR_BORDER_SIZE;
        sliderBaseLineRect.y = sliderTextBaseLineY - SLIDER_HEIGHT / 2;
        sliderBaseLineRect.width = imageWidth;
        sliderBaseLineRect.height = 1;

        paletteRect.x = HOR_BORDER_SIZE;
        paletteRect.y = sliderBaseLineRect.y - PALETTE_HEIGHT;
        paletteRect.width = imageWidth;
        paletteRect.height = PALETTE_HEIGHT;

        histoRect.x = HOR_BORDER_SIZE;
        histoRect.y = VER_BORDER_SIZE;
        histoRect.width = imageWidth;
        histoRect.height = paletteRect.y - histoRect.y - 3;
    }

    private ColorPaletteDef getColorPaletteDef() {
        return getImageInfo().getColorPaletteDef();
    }

    private void editSliderValue(int sliderIndex) {
        final ColorPaletteDef.Point point = getColorPaletteDef().getPointAt(sliderIndex);
        final ParamProperties paramProps = new ParamProperties(Double.class, point.getSample());
        paramProps.setLabel("Position");
        paramProps.setPhysicalUnit(unit);
        paramProps.setMaxValue(getMaxSliderValue(sliderIndex));
        paramProps.setMinValue(getMinSliderValue(sliderIndex));
        final Parameter parameter = new Parameter("position", paramProps);
        final JComponent labelComponent = parameter.getEditor().getLabelComponent();
        final JComponent editorComponent = parameter.getEditor().getEditorComponent();
        final JLabel unitComponent = parameter.getEditor().getPhysUnitLabelComponent();

        final ModalDialog modalDialog = new ModalDialog(SwingUtilities.getWindowAncestor(this),
                                                        "Adjust Gradient Slider " + sliderIndex, /*I18N*/
                                                        ModalDialog.ID_OK_CANCEL, null);
        final JPanel content = BorderLayoutUtils.createPanel(editorComponent, labelComponent, BorderLayout.WEST);
        if (unitComponent != null) {
            content.add(unitComponent, BorderLayout.EAST);
        }

        modalDialog.setContent(content);
        if (editorComponent instanceof JTextComponent) {
            ((JTextComponent) editorComponent).selectAll();
        }
        if (modalDialog.show() == ModalDialog.ID_OK) {
            Double newValue = (Double) parameter.getValue();
            point.setSample(newValue);
            fireStateChanged();
        }
    }

    private float getMaxSample() {
        return getImageInfo().getMaxSample();
    }

    private float getMinSample() {
        return getImageInfo().getMinSample();
    }

    public void computeColorPalette() {
        if (isRGBMode()) {
            final int numColors = 256;
            if (rgbColorPal == null) {
                rgbColorPal = new Color[numColors];
            }
            final int redMult = rgbColor.getRed() / 255;
            final int greenMult = rgbColor.getGreen() / 255;
            final int blueMult = rgbColor.getBlue() / 255;
            for (int i = 0; i < numColors; i++) {
                int j = i;
                if (gammaCurve != null) {
                    j = gammaCurve[i] & 0xff;
                }
                final int r = j * redMult;
                final int g = j * greenMult;
                final int b = j * blueMult;
                rgbColorPal[i] = new Color(r, g, b);
            }
        } else {
            getImageInfo().computeColorPalette();
        }
    }

    private class InternalMouseListener implements MouseListener, MouseMotionListener {

        private int _draggedSliderIndex;
        private JPopupMenu _contextMenu;
        private boolean _contextMenueIsCreatedNow;
        private boolean _dragging;

        public InternalMouseListener() {
            resetState();
        }

        public boolean isDragging() {
            return _dragging;
        }

        public void setDragging(boolean dragging) {
            _dragging = dragging;
        }

        public void mousePressed(MouseEvent mouseEvent) {
            Debug.trace("mousePressed");
            if (!maybeShowContextMenu(mouseEvent)) {
                resetState();
                Debug.trace("ContrastStretchPane.mouseDragged: mark 1");
                setDraggedSliderIndex(getNearestSliderIndex(mouseEvent.getX(), mouseEvent.getY()));
                if (isFirstSliderDragged() || isLastSliderDragged()) {
                    computeFactors();
                }
            }
        }

        public void mouseReleased(MouseEvent mouseEvent) {
            setDragging(false);
            if (!maybeShowContextMenu(mouseEvent)) {
                setDraggedSliderIndex(INVALID_INDEX);
            }
        }


        public void mouseClicked(MouseEvent evt) {
            int sliderIndex;
            if (!maybeShowContextMenu(evt)) {
                if (SwingUtilities.isLeftMouseButton(evt)) {
                    if (!isRGBMode()) {
                        sliderIndex = getSelectedSliderIndex(evt);
                        if (sliderIndex != INVALID_INDEX) {
                            Color newColor = JColorChooser.showDialog(GraphicalPaletteEditor.this,
                                                                      "Gradation point color",
                                                                      getGradationCurvePointAt(sliderIndex).getColor());
                            if (newColor != null) {
                                setGradationPointColorAt(sliderIndex, newColor);
                            }
                            return;
                        }
                    }
                    sliderIndex = getSelectedSliderTextIndex(evt);
                    if (isRGBMode()
                            && !isFirstSliderSelected(sliderIndex)
                            && !isLastSliderSelected(sliderIndex)) {
                        sliderIndex = INVALID_INDEX;
                    }
                    if (sliderIndex != INVALID_INDEX) {
                        editSliderValue(sliderIndex);
                        repaint();
                    }
                }
                if (_contextMenu != null && !_contextMenueIsCreatedNow) {
                    _contextMenu.setVisible(false);
                    repaint();
                    _contextMenu = null;
                }
                _contextMenueIsCreatedNow = false;
            }
        }

        public void mouseEntered(MouseEvent mouseEvent) {
            resetState();
        }

        public void mouseExited(MouseEvent mouseEvent) {
            resetState();
        }

        public void mouseDragged(MouseEvent mouseEvent) {
            setDragging(true);
            if (getDraggedSliderIndex() != INVALID_INDEX) {
                int x = mouseEvent.getX();
                x = Math.max(x, sliderBaseLineRect.x);
                x = Math.min(x, sliderBaseLineRect.x + sliderBaseLineRect.width);
                final double newSample = computeSliderValueForX(getDraggedSliderIndex(), x);
                setGradationPointSampleAt(getDraggedSliderIndex(), newSample);

                if (isFirstSliderDragged() || isLastSliderDragged()) {
                    computePartitioning();
                }
            }
        }

        public void mouseMoved(MouseEvent mouseEvent) {
            if (isDragging()) {
                mouseDragged(mouseEvent);
            }
        }

        private boolean maybeShowContextMenu(MouseEvent mouseEvent) {
            if (isRGBMode()) {
                return false;
            }
            if (mouseEvent.isPopupTrigger()) {
                final int sliderIndex = getSelectedSliderIndex(mouseEvent);
                _contextMenu = createContextMenu(sliderIndex, mouseEvent);
                if (_contextMenu != null && _contextMenu.getComponentCount() > 0) {
                    UIUtils.showPopup(_contextMenu, mouseEvent);
                    _contextMenueIsCreatedNow = true;
                    return true;
                }
            }
            return false;
        }

        private void setDraggedSliderIndex(final int draggedSliderIndex) {
            if (_draggedSliderIndex != draggedSliderIndex) {
                Debug.trace("ContrastStretchPane.setDraggedSliderIndex(" + draggedSliderIndex + ") called");
                _draggedSliderIndex = draggedSliderIndex;
            }
        }

        public int getDraggedSliderIndex() {
            return _draggedSliderIndex;
        }

        private JPopupMenu createContextMenu(final int sliderIndex, MouseEvent mouseEvent) {
            JPopupMenu contextMenu = new JPopupMenu("Gradation curve points"); /* I18N */
            contextMenu.setInvoker(GraphicalPaletteEditor.this);
            JMenuItem menuItem = createMenuItemAddNewSlider(sliderIndex, mouseEvent);
            if (menuItem != null) {
                contextMenu.add(menuItem);
            }
            if (getNumGradationCurvePoints() > 3 && sliderIndex != INVALID_INDEX) {
                menuItem = createMenuItemDeleteSlider(sliderIndex);
                contextMenu.add(menuItem);
            }
            if (getNumGradationCurvePoints() > 2 && sliderIndex > 0 && sliderIndex < getNumGradationCurvePoints() - 1) {
                menuItem = createMenuItemCenterSampleValue(sliderIndex);
                contextMenu.add(menuItem);
                menuItem = createMenuItemCenterColorValue(sliderIndex);
                contextMenu.add(menuItem);
                contextMenu.setVisible(true);
            }
            return contextMenu;
        }

        private JMenuItem createMenuItemCenterColorValue(final int sliderIndex) {
            JMenuItem menuItem;
            menuItem = new JMenuItem();
            menuItem.setText("Center gradient"); /* I18N */
            menuItem.setMnemonic('c');
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent actionEvent) {
                    ColorPaletteDef.Point p1 = getGradationCurvePointAt(sliderIndex - 1);
                    ColorPaletteDef.Point p2 = getGradationCurvePointAt(sliderIndex + 1);
                    final Color newColor = ColorPaletteDef.createCenterColor(p1, p2);
                    setGradationPointColorAt(sliderIndex, newColor);
                }
            });
            return menuItem;
        }

        private JMenuItem createMenuItemCenterSampleValue(final int sliderIndex) {
            JMenuItem menuItem = new JMenuItem();
            menuItem.setText("Center position"); /* I18N */
            menuItem.setMnemonic('s');
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent actionEvent) {
                    final ColorPaletteDef.Point p1 = getGradationCurvePointAt(sliderIndex - 1);
                    final ColorPaletteDef.Point p3 = getGradationCurvePointAt(sliderIndex + 1);
                    final double center = scale(0.5d * (scaleInverse(p1.getSample()) + scaleInverse(p3.getSample())));
                    setGradationPointSampleAt(sliderIndex, center);
                }
            });
            return menuItem;
        }

        private JMenuItem createMenuItemDeleteSlider(final int removeIndex) {
            JMenuItem menuItem = new JMenuItem("Remove gradient slider");
            menuItem.setMnemonic('D');
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    getColorPaletteDef().removePointAt(removeIndex);
                    fireStateChanged();
                }
            });
            return menuItem;
        }

        private JMenuItem createMenuItemAddNewSlider(int insertIndex, final MouseEvent evt) {
            if (isLastSliderSelected(insertIndex)) {
                return null;
            }
            if (insertIndex == INVALID_INDEX && isClickOutsideExistingSliders(evt.getX())) {
                return null;
            }
            if (insertIndex == INVALID_INDEX && !isVerticalInColorBarArea(evt.getY())) {
                return null;
            }
            if (insertIndex == INVALID_INDEX) {
                insertIndex = getNearestLeftSliderIndex(evt.getX());
            }
            if (insertIndex == INVALID_INDEX) {
                return null;
            }
            final int index = insertIndex;
            JMenuItem menuItem = new JMenuItem("Add new gradient slider");
            menuItem.setMnemonic('A');
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    assert getImageInfo() != null;
                    if (index != INVALID_INDEX && index < getColorPaletteDef().getNumPoints() - 1) {
                        if (getColorPaletteDef().createPointAfter(index, getImageInfo().getScaling())) {
                            fireStateChanged();
                        }
                    }
                }
            });
            return menuItem;
        }

        private boolean isClickOutsideExistingSliders(int x) {
            return x < getAbsoluteSliderPos(getFirstGradationCurvePoint()) || x > getAbsoluteSliderPos(getLastGradationCurvePoint());
        }

        private boolean isFirstSliderSelected(int index) {
            return index == 0;
        }

        private boolean isLastSliderSelected(int index) {
            return index == getImageInfo().getColorPaletteDef().getNumPoints() - 1;
        }

        private boolean isFirstSliderDragged() {
            return getDraggedSliderIndex() == 0;
        }

        private boolean isLastSliderDragged() {
            return getDraggedSliderIndex() == (getNumGradationCurvePoints() - 1);
        }


        private boolean isVerticalInColorBarArea(int y) {
            final int dy = Math.abs(paletteRect.y + PALETTE_HEIGHT / 2 - y);
            return dy < PALETTE_HEIGHT / 2;
        }

        private boolean isVerticalInSliderArea(int y) {
            final int dy = Math.abs(sliderBaseLineRect.y - y);
            return dy < SLIDER_HEIGHT / 2;
        }


        private int getSelectedSliderIndex(MouseEvent evt) {
            if (isVerticalInSliderArea(evt.getY())) {
                final int sliderIndex = getNearestSliderIndex(evt.getX());
                final ColorPaletteDef.Point point = getGradationCurvePointAt(sliderIndex);
                final double dx = Math.abs(getAbsoluteSliderPos(point) - evt.getX());
                if (dx < SLIDER_WIDTH / 2) {
                    return sliderIndex;
                }
            }
            return INVALID_INDEX;
        }

        private int getSelectedSliderTextIndex(MouseEvent evt) {
            float dy = Math.abs(sliderTextBaseLineY + SLIDER_VALUES_AREA_HEIGHT - evt.getY());
            if (dy < SLIDER_VALUES_AREA_HEIGHT) {
                final int sliderIndex = getNearestSliderIndex(evt.getX());
                final ColorPaletteDef.Point point = getGradationCurvePointAt(sliderIndex);
                final double dx = Math.abs(getAbsoluteSliderPos(point) - evt.getX());
                if (dx < FONT_SIZE / 2) {
                    return sliderIndex;
                }
            }
            return INVALID_INDEX;
        }

        private int getNearestSliderIndex(int x, int y) {
            if (isVerticalInSliderArea(y)) {
                return getNearestSliderIndex(x);
            }
            return INVALID_INDEX;
        }

        private int getNearestLeftSliderIndex(int x) {
            final int index = getNearestSliderIndex(x);
            final double pos = getRelativeSliderPos(getGradationCurvePointAt(index));
            if (pos > x) {
                if (index > 0) {
                    return index - 1;
                }
                return INVALID_INDEX;
            }
            return index;
        }

        private int getNearestSliderIndex(int x) {
            int nearestIndex = INVALID_INDEX;
            double minDx = Float.MAX_VALUE;
            if (isRGBMode()) {
                ColorPaletteDef.Point slider = getFirstGradationCurvePoint();
                double dx = Math.abs(getAbsoluteSliderPos(slider) - x);
                if (dx < minDx) {
                    nearestIndex = 0;
                    minDx = dx;
                }
                slider = getLastGradationCurvePoint();
                dx = Math.abs(getAbsoluteSliderPos(slider) - x);
                if (dx < minDx) {
                    nearestIndex = getNumGradationCurvePoints() - 1;
                }
            } else {
                double dx = 0.0;
                for (int i = 0; i < getNumGradationCurvePoints(); i++) {
                    ColorPaletteDef.Point slider = getGradationCurvePointAt(i);
                    dx = getAbsoluteSliderPos(slider) - x;
                    if (Math.abs(dx) <= minDx) {
                        nearestIndex = i;
                        minDx = Math.abs(dx);
                    }
                }
                if (nearestIndex == getNumGradationCurvePoints() - 1) {
                    final int i = getNumGradationCurvePoints() - 1;
                    final ColorPaletteDef.Point slider1 = getGradationCurvePointAt(i - 1);
                    final ColorPaletteDef.Point slider2 = getGradationCurvePointAt(i);
                    if (getAbsoluteSliderPos(slider1) == getAbsoluteSliderPos(slider2)) {
                        nearestIndex = (dx <= 0.0) ? i : i - 1;
                    }
                }
            }
            return nearestIndex;
        }

        private void resetState() {
            setDraggedSliderIndex(INVALID_INDEX);
            factors = null;
        }

    }

    private double scale(double value) {
        assert imageInfo != null;
        return imageInfo.getScaling().scale(value);
    }

    private double scaleInverse(double value) {
        assert imageInfo != null;
        return imageInfo.getScaling().scaleInverse(value);
    }

}
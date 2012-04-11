package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.List;

/**
 * A logarithmic axis representation with improved scaling and labelling
 *
 * @author olafd
 *         Date: 10.04.12
 *         Time: 21:06
 */
public class CustomLogarithmicAxis extends LogarithmicAxis {
    static final int VERTICAL = 0;
    static final int HORIZONTAL = 1;

    /**
     * Creates a new axis.
     *
     * @param label the axis label.
     */
    public CustomLogarithmicAxis(String label) {
        super(label);
    }

    @Override
    protected List refreshTicksHorizontal(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        return refreshTicks(edge, HORIZONTAL);
    }

    @Override
    protected List refreshTicksVertical(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        return refreshTicks(edge, VERTICAL);
    }

    List refreshTicks(RectangleEdge edge, int mode) {
        // a much simpler approach compared to LogarithmicAxis...

        List ticks = new java.util.ArrayList();

        double upperBoundVal = 0.0;
        double lowerBoundVal = 0.0;
        boolean NEGATIVE = false;

        if (getRange().getLowerBound() >= 0.0 && getRange().getUpperBound() >= 0.0) {
            upperBoundVal = getRange().getUpperBound();
            lowerBoundVal = getRange().getLowerBound();
            if (getRange().getLowerBound() == 0.0) {
                lowerBoundVal = SMALL_LOG_VALUE;
            }
        } else if (getRange().getLowerBound() < 0.0 && getRange().getUpperBound() < 0.0) {
            upperBoundVal = -1.0 * getRange().getLowerBound();
            lowerBoundVal = -1.0 * getRange().getUpperBound();
            NEGATIVE = true;
        }

        //get log10 version of upper bound and round to integer:
        final int iEndCount = (int) Math.floor(Math.log10(upperBoundVal));
        //get log10 version of lower bound and round to integer:
        int iBegCount;
        if (lowerBoundVal == SMALL_LOG_VALUE) {
            iBegCount = iEndCount - 3;
        } else {
            iBegCount = (int) Math.floor(Math.log10(lowerBoundVal));
        }

        String tickLabel;
        for (int i = iBegCount; i <= iEndCount; i++) {
            int jEndCount = 9;
            if (i == iEndCount) {
                if (iEndCount == 0) {
                    jEndCount = (int) Math.abs(Math.floor(upperBoundVal));
                } else {
                    jEndCount = (int) (upperBoundVal / Math.pow(10.0, iEndCount));
                }
            }

            for (int j = 0; j < jEndCount; j++) {
                final boolean displayTickLabel = (j == 0) || (iEndCount - iBegCount < 2);
                double tickVal = Math.pow(10, i) + (Math.pow(10, i) * j);
                if (NEGATIVE) {
                    tickVal *= -1.0;
                }
                if (displayTickLabel) {
                    //create label for tick:
                    tickLabel = NumberFormat.getNumberInstance().format(tickVal);
                } else {
                    //no label
                    tickLabel = "";
                }

                if (tickValInRange(lowerBoundVal, upperBoundVal, tickVal, NEGATIVE)) {
                    switch (mode) {
                        case VERTICAL:
                            if (NEGATIVE) {
                                addVerticalTicks(edge, ticks, -upperBoundVal, tickLabel, tickVal);
                            } else {
                                addVerticalTicks(edge, ticks, lowerBoundVal, tickLabel, tickVal);
                            }
                            break;
                        case HORIZONTAL:
                            if (NEGATIVE) {
                                addHorizontalTicks(edge, ticks, -upperBoundVal, tickLabel, tickVal);
                            } else {
                                addHorizontalTicks(edge, ticks, lowerBoundVal, tickLabel, tickVal);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Illegal axis orientation - cannot add ticks");
                    }
                }
            }
        }
        return ticks;
    }

    private boolean tickValInRange(double lowerBoundVal, double upperBoundVal, double tickVal, boolean negative) {
        if (negative) {
            return (-upperBoundVal <= tickVal && -lowerBoundVal >= tickVal);
        } else {
            return (lowerBoundVal <= tickVal && upperBoundVal >= tickVal);
        }
    }

    private void addHorizontalTicks(RectangleEdge edge, List ticks, double lowerBoundVal, String tickLabel, double tickVal) {
        if (tickVal >= lowerBoundVal - SMALL_LOG_VALUE) {
            //tick value not below lowest data value
            TextAnchor anchor;
            TextAnchor rotationAnchor;
            double angle = 0.0;
            if (isVerticalTickLabels()) {
                anchor = TextAnchor.CENTER_RIGHT;
                rotationAnchor = TextAnchor.CENTER_RIGHT;
                if (edge == RectangleEdge.TOP) {
                    angle = Math.PI / 2.0;
                } else {
                    angle = -Math.PI / 2.0;
                }
            } else {
                if (edge == RectangleEdge.TOP) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else {
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                }
            }

            ticks.add(new NumberTick(new Double(tickVal), tickLabel, anchor, rotationAnchor, angle));
        }
    }

    private void addVerticalTicks(RectangleEdge edge, List ticks, double lowerBoundVal, String tickLabel, double tickVal) {
        if (tickVal >= lowerBoundVal - SMALL_LOG_VALUE) {
            //tick value not below lowest data value
            TextAnchor anchor;
            TextAnchor rotationAnchor;
            double angle = 0.0;
            if (isVerticalTickLabels()) {
                if (edge == RectangleEdge.LEFT) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                    angle = -Math.PI / 2.0;
                } else {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                    angle = Math.PI / 2.0;
                }
            } else {
                if (edge == RectangleEdge.LEFT) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                }
            }
            //create tick object and add to list:
            ticks.add(new NumberTick(new Double(tickVal), tickLabel, anchor, rotationAnchor, angle));
        }
    }
}

package org.esa.beam.timeseries.ui.matrix;

import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.DecimalFormat;

import static java.lang.Math.*;

class MatrixCellRenderer extends DefaultTableCellRenderer {

    private final MatrixTableModel tableModel;
    private final DecimalFormat valueFormatter;
    private final Font boldFont;
    private Color invalidColor;

    MatrixCellRenderer(MatrixTableModel tableModel) {
        this.tableModel = tableModel;
        valueFormatter = new DecimalFormat("0.0000");
        boldFont = getFont().deriveFont(Font.BOLD);
    }

    public void setInvalidColor(Color invalidColor) {
        this.invalidColor = invalidColor;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                                                                          isSelected, hasFocus,
                                                                          row, column);
        Double rasterValue = (Double) value;
        final RasterDataNode raster = tableModel.getBand();
        final String labelText;
        Color bgColor;
        if (raster != null && rasterValue != null) {
            if (Double.isNaN(rasterValue)) { // value is invalid
                bgColor = invalidColor;
                labelText = "NaN";
            } else {
                bgColor = raster.getImageInfo().getColorPaletteDef().computeColor(raster, rasterValue);
                labelText = valueFormatter.format(rasterValue);
            }
        } else { // outside of image or no raster set
            bgColor = Color.WHITE;
            labelText = "";
        }

        label.setBackground(bgColor);
        label.setForeground(findForegroundColor(bgColor));
        label.setText(labelText);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(boldFont);

        return label;
    }

    // From: http://www.w3.org/TR/AERT#color-contrast
    // Two colors provide good color visibility if the brightness difference and the color difference between
    // the two colors are greater than a set range.
    // Color brightness is determined by the following formula:
    // ((Red value X 299) + (Green value X 587) + (Blue value X 114)) / 1000
    // Note: This algorithm is taken from a formula for converting RGB values to YIQ values. This brightness
    // value gives a perceived brightness for a color.
    //
    // Color difference is determined by the following formula:
    //   (maximum (Red value 1, Red value 2) - minimum (Red value 1, Red value 2)) +
    //   (maximum (Green value 1, Green value 2) - minimum (Green value 1, Green value 2)) +
    //   (maximum (Blue value 1, Blue value 2) - minimum (Blue value 1, Blue value 2))
    //
    // The threshold for color brightness difference is 125. The threshold for color difference is 500.
    // Both values should be over their threshold.
    // Good page to experiment with colors: http://contrast-a.dasplankton.com/
    private Color findForegroundColor(Color bgColor) {
        final int colorDiffWhite = getColorDifference(Color.WHITE, bgColor);
        final int colorBrightDiffWhite = getColorBrightnessDifference(Color.WHITE, bgColor);
        final int colorDiffBlack = getColorDifference(Color.BLACK, bgColor);
        final int colorBrightDiffBlack = getColorBrightnessDifference(Color.BLACK, bgColor);
        final boolean colorDiffWhiteGood = colorDiffWhite >= 500;
        final boolean colorDiffBlackGood = colorDiffBlack >= 500;
        final boolean colorBrightDiffWhiteGood = colorBrightDiffWhite >= 125;
        final boolean colorBrightDiffBlackGood = colorBrightDiffBlack >= 125;

        // test if both values are good
        if (colorDiffWhiteGood && colorBrightDiffWhiteGood) {
            return Color.WHITE;
        }
        if (colorDiffBlackGood && colorBrightDiffBlackGood) {
            return Color.BLACK;
        }
        // test if at least one value is good
        if (colorDiffWhiteGood || colorBrightDiffWhiteGood) {
            return Color.WHITE;
        }
        if (colorDiffBlackGood || colorBrightDiffBlackGood) {
            return Color.BLACK;
        }

        // if non of above applies use the best.
        if (colorDiffWhite + colorBrightDiffWhite > colorDiffBlack + colorBrightDiffBlack) {
            return Color.WHITE;
        }
        return Color.BLACK;
    }

    private int getColorBrightnessDifference(Color color1, Color color2) {
        int colorBright1 = (color1.getRed() * 299 + color1.getGreen() * 587 + color1.getBlue() * 114) / 1000;
        int colorBright2 = (color2.getRed() * 299 + color2.getGreen() * 587 + color2.getBlue() * 114) / 1000;
        return abs(colorBright1 - colorBright2);
    }

    private static int getColorDifference(Color color1, Color color2) {
        return max(color1.getRed(), color2.getRed()) - min(color1.getRed(), color2.getRed()) +
               max(color1.getGreen(), color2.getGreen()) - min(color1.getGreen(), color2.getGreen()) +
               max(color1.getBlue(), color2.getBlue()) - min(color1.getBlue(), color2.getBlue());
    }

}

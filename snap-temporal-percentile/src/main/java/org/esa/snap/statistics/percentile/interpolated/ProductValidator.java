package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.geom.Area;
import java.util.logging.Logger;

public class ProductValidator {

    final ProductData.UTC startDate;
    final ProductData.UTC endDate;
    private final Logger logger;
    private final Area targetArea;
    private final String sourceBandName;
    private final String bandMathExpression;
    private final String validPixelExpression;

    public ProductValidator(String sourceBandName, String bandMathExpression, String validPixelExpression, ProductData.UTC startDate, ProductData.UTC endDate, Area targetArea, Logger logger) {
        this.sourceBandName = sourceBandName;
        this.bandMathExpression = bandMathExpression;
        this.validPixelExpression = validPixelExpression;
        this.startDate = startDate;
        this.endDate = endDate;
        this.logger = logger;
        this.targetArea = targetArea;
    }

    public boolean isValid(Product product) {
        return product != null
               && containsGeoCodingWithReverseOperationSupport(product)
               && fulfillsTimeConditions(product)
               && canHandleExpressionOrSourceBand(product)
               && isInDateRange(product)
               && intersectsTargetArea(product);
    }

    private boolean containsGeoCodingWithReverseOperationSupport(Product product) {
        GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null) {
            logSkipped("The product '" + product.getName() + "' does not contain a geo coding.");
            return false;
        }
        if (!geoCoding.canGetPixelPos()) {
            logSkipped("The geo-coding of the product '" + product.getName() + "' can not determine the pixel position from a geodetic position.");
            return false;
        }
        return true;
    }

    private boolean fulfillsTimeConditions(Product product) {
        return containsStartAndEndDate(product)
               && startTimeMustBeBeforeEnd(product)
               && timeRangeIsLessThan367Days(product);
    }

    private boolean containsStartAndEndDate(Product product) {
        boolean valid = product.getStartTime() != null && product.getEndTime() != null;
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' must contain start and end time.");
        }
        return valid;
    }

    private boolean startTimeMustBeBeforeEnd(Product product) {
        final double startMJD = product.getStartTime().getMJD();
        final double stopMJD = product.getEndTime().getMJD();
        final boolean valid = startMJD < stopMJD;
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' has an end time which is before start time.");
        }
        return valid;
    }

    private boolean timeRangeIsLessThan367Days(Product product) {
        final double startMJD = product.getStartTime().getMJD();
        final double stopMJD = product.getEndTime().getMJD();
        final boolean valid = stopMJD - startMJD < 367;
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' covers more than one year.");
        }
        return valid;
    }

    private boolean canHandleExpressionOrSourceBand(Product product) {
        if (sourceBandName != null) {
            if (!product.containsBand(sourceBandName)) {
                logSkipped("The product '" + product.getName() + "' does not contain the band '" + sourceBandName + "'.");
                return false;
            }
        } else {
            if (!product.isCompatibleBandArithmeticExpression(bandMathExpression)) {
                logSkipped("'" + bandMathExpression + "' is not a compatible band arithmetic expression for product: '" + product.getName() + ".");
                return false;
            }
        }
        if (validPixelExpression != null && validPixelExpression.trim().length() > 0) {
            final String expression = validPixelExpression.trim();
            if (!product.isCompatibleBandArithmeticExpression(expression)) {
                logSkipped("'" + validPixelExpression + "' is not a compatible valid pixel expression for product: '" + product.getName() + ".");
                return false;
            }
        }
        return true;
    }

    private boolean isInDateRange(Product product) {
        if (startDate == null && endDate == null) {
            return true;
        }
        if (startDate != null) {
            final long startDateMillis = startDate.getAsDate().getTime();
            final long productStartDateMillis = product.getStartTime().getAsDate().getTime();
            if (productStartDateMillis < startDateMillis) {
                logSkippedDueToTimeRange(product);
                return false;
            }
        }
        if (endDate != null) {
            final long endDateMillis = endDate.getAsDate().getTime();
            final long productEndDateMillis = product.getEndTime().getAsDate().getTime();
            if (productEndDateMillis > endDateMillis) {
                logSkippedDueToTimeRange(product);
                return false;
            }
        }

        return true;
    }

    private boolean intersectsTargetArea(Product product) {
        Area productArea = Utils.createProductArea(product);
        productArea.intersect(targetArea);
        boolean valid = !productArea.isEmpty();
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' does not intersect the target product.");
        }

        return valid;
    }

    private void logSkippedDueToTimeRange(Product product) {
        logSkipped("The product '" + product.getName() + "' is not inside the date range" + formatDateRange());
    }

    private String formatDateRange() {
        return (startDate != null ? " from " + startDate.format() : " ")
               + (endDate != null ? " to " + endDate.format() : "");
    }

    private void logSkipped(String message) {
        logger.info("Product skipped. " + message);
    }

}

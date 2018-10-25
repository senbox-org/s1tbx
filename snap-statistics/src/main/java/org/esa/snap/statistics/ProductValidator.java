package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.util.List;
import java.util.logging.Logger;

public class ProductValidator {

    final ProductData.UTC startDate;
    final ProductData.UTC endDate;
    final List<BandConfiguration> bandConfigurations;
    private final Logger logger;

    public ProductValidator(List<BandConfiguration> bandConfigurations, ProductData.UTC startDate, ProductData.UTC endDate, Logger logger) {
        this.bandConfigurations = bandConfigurations;
        this.startDate = startDate;
        this.endDate = endDate;
        this.logger = logger;
    }

    public boolean isValid(Product product) {
        return containsGeocoding(product) && canHandleBandConfigurations(product) && isInDateRange(product);
    }

    private boolean containsGeocoding(Product product) {
        final boolean valid = product.getSceneGeoCoding() != null;
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' does not contain a geo coding.");
        }
        return valid;
    }

    private boolean canHandleBandConfigurations(Product product) {
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final String bandName = bandConfiguration.sourceBandName;
            final String expression = bandConfiguration.expression;
            if (bandName != null) {
                if (!product.containsBand(bandName)) {
                    logSkipped("The product '" + product.getName() + "' does not contain the band '" + bandName + "'");
                    return false;
                }
            } else {
                if (!product.isCompatibleBandArithmeticExpression(expression)) {
                    logSkipped("The product '" + product.getName() + "' can not resolve the band arithmetic expression '" + expression + "'");
                    return false;
                } else {
                    final String replacedExpression = expression.replace(" ", "_");
                    if (product.containsBand(replacedExpression)) {
                        logSkipped("The product '" + product.getName() + "' already contains a band '" + replacedExpression + "'");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isInDateRange(Product product) {
        if (startDate == null && endDate == null) {
            return true;
        }
        final ProductData.UTC productStartDate = product.getStartTime();
        final ProductData.UTC productEndDate = product.getEndTime();
        if (productStartDate == null && productEndDate == null) {
            return false;
        }
        if (startDate != null) {
            if (productStartDate != null) {
                final long startDateMillis = startDate.getAsDate().getTime();
                final long productStartDateMillis = productStartDate.getAsDate().getTime();
                if (productStartDateMillis < startDateMillis) {
                    logSkippedDueToTimeRange(product);
                    return false;
                }
            } else {
                logSkippedDueToTimeRange(product);
                return false;
            }
        }
        if (endDate != null) {
            if (productEndDate != null) {
                final long endDateMillis = endDate.getAsDate().getTime();
                final long productEndDateMillis = productEndDate.getAsDate().getTime();
                if (productEndDateMillis > endDateMillis) {
                    logSkippedDueToTimeRange(product);
                    return false;
                }
            } else {
                logSkippedDueToTimeRange(product);
                return false;
            }
        }

        return true;
    }

    private void logSkippedDueToTimeRange(Product product) {
        logSkipped("The product '" + product.getName() + "' is not inside the date range" + formatDateRange());
    }

    private String formatDateRange() {
        return (startDate != null ? " from " + startDate.format() : " ")
                + (endDate != null ? " to " + endDate.format() : "");
    }

    private void logSkipped(String message) {
        logger.warning("Product skipped. " + message);
    }

}

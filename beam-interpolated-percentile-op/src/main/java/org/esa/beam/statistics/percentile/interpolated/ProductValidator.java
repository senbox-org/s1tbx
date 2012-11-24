package org.esa.beam.statistics.percentile.interpolated;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.geom.Area;
import java.util.List;
import java.util.logging.Logger;

public class ProductValidator {

    final ProductData.UTC startDate;
    final ProductData.UTC endDate;
    final List<BandConfiguration> bandConfigurations;
    private final Logger logger;
    private final Area targetArea;

    public ProductValidator(List<BandConfiguration> bandConfigurations, ProductData.UTC startDate, ProductData.UTC endDate, Area targetArea, Logger logger) {
        this.bandConfigurations = bandConfigurations;
        this.startDate = startDate;
        this.endDate = endDate;
        this.logger = logger;
        this.targetArea = targetArea;
    }

    public boolean isValid(Product product) {
        return containsGeocoding(product)
               && containsStartAndEndDate(product)
               && canHandleBandConfigurations(product)
               && isInDateRange(product)
               && intersectsTargetArea(product);
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

    private boolean containsGeocoding(Product product) {
        final boolean valid = product.getGeoCoding() != null;
        if (!valid) {
            logSkipped("The product '" + product.getName() + "' does not contain a geo coding.");
        }
        return valid;
    }

    private boolean containsStartAndEndDate(Product product) {
        return product.getStartTime() != null && product.getEndTime() != null;
    }

    private boolean canHandleBandConfigurations(Product product) {
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final String bandName = bandConfiguration.sourceBandName;
            if (!product.containsBand(bandName)) {
                logSkipped("The product '" + product.getName() + "' does not contain the band '" + bandName + "'");
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

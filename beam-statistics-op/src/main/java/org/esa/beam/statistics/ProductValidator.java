package org.esa.beam.statistics;

import java.util.List;
import java.util.logging.Logger;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

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
        return canHandleBandConfigurations(product) && isInDateRange(product);
    }

    private boolean canHandleBandConfigurations(Product product) {
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final String bandName = bandConfiguration.sourceBandName;
            final String expression = bandConfiguration.expression;
            if (bandName != null) {
                if (!product.containsBand(bandName)) {
                    logger.info("Product skipped. The product '"
                                + product.getName()
                                + "' does not contain the band '" + bandName + "'"
                    );
                    return false;
                }
            } else {
                if (!product.isCompatibleBandArithmeticExpression(expression)) {
                    logger.info("Product skipped. The product '"
                                + product.getName()
                                + "' can not resolve the band arithmetic expression '"+expression+ "'"
                    );
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInDateRange(Product product) {
        if (startDate == null && endDate == null) {
            return true;
        }
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        if (startTime == null && endTime == null) {
            return true;
        }
        final long start_date = startDate.getAsDate().getTime();
        final long end_date = endDate.getAsDate().getTime();
        final long start_time = startTime.getAsDate().getTime();
        final long end_time = endTime.getAsDate().getTime();
        final boolean valid = start_date <= start_time && end_date >= end_time;
        if (!valid) {
            logger.info("Product skipped. The product '"
                        + product.getName()
                        + "' is not inside the date range"
                        + " from: " + startDate.format()
                        + " to: " + endDate.format()
            );
        }
        return valid;
    }

}

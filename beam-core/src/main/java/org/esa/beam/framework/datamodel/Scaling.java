/*
 * $Id: Scaling.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

/**
 * The scaling method used for geophysical value transformation in a {@link Band}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface Scaling {

    /**
     * The identity scaling in=out.
     */
    Scaling IDENTITY = new Scaling() {
        public double scale(final double value) {
            return value;
        }

        public double scaleInverse(final double value) {
            return value;
        }
    };

    /**
     * The forward scaling method.
     * @param value the value to be scaled
     * @return the transformed value
     */
    double scale(double value);

    /**
     * The inverse scaling method.
     * @param value the value to be inverse-scaled
     * @return the transformed value
     */
    double scaleInverse(double value);
}

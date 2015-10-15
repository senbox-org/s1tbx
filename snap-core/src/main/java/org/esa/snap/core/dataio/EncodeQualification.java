package org.esa.snap.core.dataio;

import com.bc.ceres.core.Assert;

/**
 * The encode qualification of a product writer w.r.t. a given product and a given data format.
 *
 * @author Norman Fomferra
 * @see ProductWriterPlugIn#getEncodeQualification
 * @since SNAP 2
 */
public class EncodeQualification {

    public static final EncodeQualification FULL = new EncodeQualification(Preservation.FULL);

    /**
     * Enumerates possible information preservation levels.
     */
    public enum Preservation {
        /**
         * The writer is able to fully encode a given product to a given format.
         */
        FULL,
        /**
         * The writer is able to partially encode a given product to a given format to produce a meaningful external representation.
         */
        PARTIAL,
        /**
         * The writer is unable to encode a given product to a given format in order to produce a meaningful external
         * representation.
         */
        UNABLE
    }

    private final Preservation preservation;
    private final String infoString;

    /**
     * Constructor.
     *
     * @param preservation The information preservation level.
     */
    public EncodeQualification(Preservation preservation) {
        this(preservation, null);
    }

    /**
     * Constructor.
     *
     * @param preservation The information preservation level.
     * @param infoString An optional informal text describing an incompatibility, maybe with a hint for the user to
     *                   avoid the incompatibility. May be {@code null}.
     */
    public EncodeQualification(Preservation preservation, String infoString) {
        Assert.notNull(preservation, "preservation");
        this.preservation = preservation;
        this.infoString = infoString;
    }

    /**
     * @return The information preservation level.
     */
    public Preservation getPreservation() {
        return preservation;
    }

    /**
     * @return An informal text describing an incompatibility, maybe with a hint for the user to
     *                   avoid the incompatibility. May be {@code null}.
     */
    public String getInfoString() {
        return infoString;
    }
}

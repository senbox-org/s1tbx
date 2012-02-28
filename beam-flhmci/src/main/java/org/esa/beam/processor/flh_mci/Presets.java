package org.esa.beam.processor.flh_mci;

public enum Presets {
    NONE("None", "", "", "", "", "", ""),
    MERIS_L1B_MCI("MERIS L1b MCI", "radiance_8", "radiance_10", "radiance_9", "MCI", "MCI_slope",
                  "NOT l1_flags.LAND_OCEAN AND NOT l1_flags.BRIGHT AND NOT l1_flags.INVALID"),
    MERIS_L2_FLH("MERIS L2 FLH", "reflec_7", "reflec_9", "reflec_8", "FLH", "FLH_slope",
                 "l2_flags.WATER"),
    MERIS_L2_MCI("MERIS L2 FLH", "reflec_8", "reflec_10", "reflec_9", "MCI", "MCI_slope",
                 "l2_flags.WATER");

    private final String label;
    private final String lowerBaselineBandName;
    private final String upperBaselineBandName;
    private final String signalBandName;
    private final String lineHeightBandName;
    private final String slopeBandName;
    private final String maskExpression;

    private Presets(String label, String upperBaselineBandName, String lowerBaselineBandName,
                    String signalBandName, String lineHeightBandName, String slopeBandName, String maskExpression) {
        this.label = label;
        this.upperBaselineBandName = upperBaselineBandName;
        this.lowerBaselineBandName = lowerBaselineBandName;
        this.signalBandName = signalBandName;
        this.lineHeightBandName = lineHeightBandName;
        this.slopeBandName = slopeBandName;
        this.maskExpression = maskExpression;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getLowerBaselineBandName() {
        return lowerBaselineBandName;
    }

    public String getUpperBaselineBandName() {
        return upperBaselineBandName;
    }

    public String getSignalBandName() {
        return signalBandName;
    }

    public String getLineHeightBandName() {
        return lineHeightBandName;
    }

    public String getSlopeBandName() {
        return slopeBandName;
    }

    public String getMaskExpression() {
        return maskExpression;
    }
}

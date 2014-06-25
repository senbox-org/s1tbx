package org.esa.beam.dataio.avhrr.noaa.pod;

/**
* @author Ralf Quast
*/
final class AlbedoCalibratorFactory extends AbstractCalibratorFactory {

    AlbedoCalibratorFactory(int channelIndex, CalibrationCoefficientsProvider provider) {
        super(channelIndex, provider);
    }

    @Override
    public String getBandName() {
        return "albedo_" + (getChannelIndex() + 1);
    }

    @Override
    public String getBandUnit() {
        return "%";
    }

    @Override
    public String getBandDescription() {
        return "Albedo";
    }
}

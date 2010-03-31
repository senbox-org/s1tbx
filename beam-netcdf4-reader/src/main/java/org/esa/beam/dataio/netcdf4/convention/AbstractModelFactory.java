package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;

/*
 * User: Thomas Storm
 * Date: 26.03.2010
 * Time: 14:48:07
 */

/**
 * Abstract base class for factories which are able to create models for NetCDF4-IO. Subclasses allow to
 * dynamically add generic {@link ModelPart}s to the model.
 */
public abstract class AbstractModelFactory {

    protected InitialisationPart initPart;
    protected ModelPart bandPart;
    protected ModelPart flagCodingPart;
    protected ModelPart geocodingPart;
    protected ModelPart imageInfoPart;
    protected ModelPart indexCodingPart;
    protected ModelPart maskOverlayPart;
    protected ModelPart stxPart;
    protected ModelPart tiePointGridPart;
    protected ModelPart startTimePart;
    protected ModelPart endTimePart;
    protected ModelPart metaDataPart;
    protected ModelPart descriptionPart;

    protected Model model = new Model();

    public AbstractModelFactory() {
        metaDataPart = getMetadataPart();
        bandPart = getBandPart();
        initPart = getInitialisationPart();
        flagCodingPart = getFlagCodingPart();
        geocodingPart = getGeocodingPart();
        imageInfoPart = getImageInfoPart();
        indexCodingPart = getIndexCodingPart();
        maskOverlayPart = getMaskOverlayPart();
        stxPart = getStxPart();
        tiePointGridPart = getTiePointGridPart();
        startTimePart = getStartTimePart();
        endTimePart = getEndTimePart();
        descriptionPart = getDescriptionPart();
    }

    public abstract ModelPart getMetadataPart();

    public abstract ModelPart getBandPart();

    public abstract InitialisationPart getInitialisationPart();

    public abstract ModelPart getFlagCodingPart();

    public abstract ModelPart getGeocodingPart();

    public abstract ModelPart getImageInfoPart();

    public abstract ModelPart getIndexCodingPart();

    public abstract ModelPart getMaskOverlayPart();

    public abstract ModelPart getStxPart();

    public abstract ModelPart getTiePointGridPart();

    public abstract ModelPart getStartTimePart();

    public abstract ModelPart getEndTimePart();

    public abstract ModelPart getDescriptionPart();

    public Model createModel() {
        if (initPart != null) {
            model.setInitialisationPart(initPart);
        }
        if (metaDataPart != null) {
            model.addModelPart(metaDataPart);
        }
        if (bandPart != null) {
            model.addModelPart(bandPart);
        }
        if (flagCodingPart != null) {
            model.addModelPart(flagCodingPart);
        }
        if (geocodingPart != null) {
            model.addModelPart(geocodingPart);
        }
        if (imageInfoPart != null) {
            model.addModelPart(imageInfoPart);
        }
        if (indexCodingPart != null) {
            model.addModelPart(indexCodingPart);
        }
        if (maskOverlayPart != null) {
            model.addModelPart(maskOverlayPart);
        }
        if (stxPart != null) {
            model.addModelPart(stxPart);
        }
        if (tiePointGridPart != null) {
            model.addModelPart(tiePointGridPart);
        }
        if (startTimePart != null) {
            model.addModelPart(startTimePart);
        }
        if (endTimePart != null) {
            model.addModelPart(endTimePart);
        }
        if (descriptionPart != null) {
            model.addModelPart(descriptionPart);
        }
        return model;
    }

    protected abstract boolean isIntendedFor(Nc4ReaderParameters rp);

}

package org.esa.s1tbx.commons.test;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

public class MetadataValidator {

    private final Product product;
    private final MetadataElement absRoot;
    private final ValidationOptions validationOptions;

    public static class ValidationOptions {
        public boolean validateOrbitStateVectors = true;
    }

    public MetadataValidator(final Product product) {
        this(product, null);
    }

    public MetadataValidator(final Product product, final ValidationOptions options) {
        this.product = product;
        this.absRoot = AbstractMetadata.getAbstractedMetadata(product);
        this.validationOptions = options == null ? new ValidationOptions() : options;
    }

    public void validate() throws Exception {
//        final MetadataAttribute[] attribs = absRoot.getAttributes();
//        for(MetadataAttribute attrib : attribs) {
//            System.out.println(attrib.getName() +" = "+ attrib.getData().toString());
//        }

        verifyStr(AbstractMetadata.PRODUCT);
        verifyStr(AbstractMetadata.PRODUCT_TYPE);
        verifyStr(AbstractMetadata.MISSION);
        verifyStr(AbstractMetadata.ACQUISITION_MODE);
        verifyStr(AbstractMetadata.SPH_DESCRIPTOR);
        verifyStr(AbstractMetadata.PASS);

        final InputProductValidator validator = new InputProductValidator(product);
        if(validator.isSARProduct()) {
            validateSAR();
        } else {
            validateOptical();
        }
    }

    public void validateOptical() throws Exception {
    }

    public void validateSAR() throws Exception {

        verifyStr(AbstractMetadata.SAMPLE_TYPE, new String[]{"COMPLEX","DETECTED"});
        verifyStr(AbstractMetadata.antenna_pointing, new String[]{"right","left"});

        verifyDouble(AbstractMetadata.radar_frequency);
        //verifyDouble(AbstractMetadata.pulse_repetition_frequency);
        //verifyDouble(AbstractMetadata.range_spacing);
        //verifyDouble(AbstractMetadata.azimuth_spacing);
        //verifyDouble(AbstractMetadata.range_looks);
        //verifyDouble(AbstractMetadata.azimuth_looks);
        verifyDouble(AbstractMetadata.line_time_interval);
        //verifyDouble(AbstractMetadata.slant_range_to_first_pixel);

        verifyInt(AbstractMetadata.num_output_lines);
        verifyInt(AbstractMetadata.num_samples_per_line);

        verifyUTC(AbstractMetadata.STATE_VECTOR_TIME);

        //verifySRGR();
        verifyOrbitStateVectors();
        //verifyDopplerCentroids();
    }

    private void verifySRGR() throws Exception {
        final MetadataElement srgrElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        if(srgrElem != null) {
            MetadataElement[] elems = srgrElem.getElements();
            if(elems.length == 0) {
                throw new Exception("SRGR Coefficients not found");
            }
            MetadataElement coefList = elems[0];
            if(!coefList.containsAttribute(AbstractMetadata.srgr_coef_time)) {
                throw new Exception("SRGR "+AbstractMetadata.srgr_coef_time+" not found");
            }
            if(!coefList.containsAttribute(AbstractMetadata.ground_range_origin)) {
                throw new Exception("SRGR "+AbstractMetadata.ground_range_origin+" not found");
            }

            MetadataElement[] srgrList = coefList.getElements();
            if(srgrList.length == 0) {
                throw new Exception("SRGR Coefficients not found");
            }
            MetadataElement srgr = srgrList[0];
            if(!srgr.containsAttribute(AbstractMetadata.srgr_coef)) {
                throw new Exception("SRGR "+AbstractMetadata.srgr_coef+" not found");
            }
        } else {
            throw new Exception("SRGR Coefficients not found");
        }
    }

    private void verifyOrbitStateVectors() throws Exception {
        if(!validationOptions.validateOrbitStateVectors)
            return;

        final MetadataElement orbitElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        if(orbitElem != null) {
            MetadataElement[] elems = orbitElem.getElements();
            if(elems.length == 0) {
                throw new Exception("Orbit State Vectors not found");
            }
        } else {
            throw new Exception("Orbit State Vectors not found");
        }
    }

    private void verifyDopplerCentroids() throws Exception {
        final MetadataElement dopElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        if(dopElem != null) {
            MetadataElement[] elems = dopElem.getElements();
            if(elems.length == 0) {
                throw new Exception("Doppler Centroids not found");
            }
            MetadataElement coefList = elems[0];
            if(!coefList.containsAttribute(AbstractMetadata.dop_coef_time)) {
                throw new Exception("Doppler Centroids "+AbstractMetadata.dop_coef_time+" not found");
            }
            if(!coefList.containsAttribute(AbstractMetadata.slant_range_time)) {
                throw new Exception("Doppler Centroids "+AbstractMetadata.slant_range_time+" not found");
            }
        } else {
            throw new Exception("Doppler Centroids not found");
        }
    }

    private void verifyStr(final String tag) throws Exception {
        String value = absRoot.getAttributeString(tag);
        if(value == null || value.trim().isEmpty() || value.equals(AbstractMetadata.NO_METADATA_STRING)) {
            throw new Exception("Metadata " + tag + " is invalid " + value);
        }
    }

    private void verifyStr(final String tag, final String[] allowedStr) throws Exception {
        verifyStr(tag);

        String value = absRoot.getAttributeString(tag);
        for(String allowed : allowedStr) {
            if(value.equals(allowed))
                return;
        }
        throw new Exception("Metadata " + tag + " is invalid " + value);
    }

    private void verifyDouble(final String tag) throws Exception {
        Double value = absRoot.getAttributeDouble(tag);
        if(value == null || value.equals((double)AbstractMetadata.NO_METADATA)) {
            throw new Exception("Metadata " + tag + " is invalid " + value);
        }
    }

    private void verifyInt(final String tag) throws Exception {
        Integer value = absRoot.getAttributeInt(tag);
        if(value == null || value.equals(AbstractMetadata.NO_METADATA)) {
            throw new Exception("Metadata " + tag + " is invalid " + value);
        }
    }

    private void verifyUTC(final String tag) throws Exception {
        ProductData.UTC value = absRoot.getAttributeUTC(tag);
        if(value == null || value.equals(AbstractMetadata.NO_METADATA_UTC)) {
            throw new Exception("Metadata " + tag + " is invalid " + value);
        }
    }
}

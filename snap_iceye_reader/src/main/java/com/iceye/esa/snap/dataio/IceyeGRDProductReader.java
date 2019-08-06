package com.iceye.esa.snap.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.*;

/**
 * @author Ahmad Hamouda
 */
public class IceyeGRDProductReader extends SARReader {


    private final Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<>(10);
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private Map<String, String> tiffFeilds = null;
    private Product product = null;
    private boolean isComplex = false;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public IceyeGRDProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    private static void addAttribute(MetadataElement meta, String name, String value) {
        MetadataAttribute attribute = new MetadataAttribute(name, 41, 1);
        if (value.isEmpty()) {
            value = " ";
        }
        attribute.getData().setElems(value);
        meta.addAttribute(attribute);
    }

    private static void addGeoCodingFromMetadata(final Product product, Map<String, String> netcdfFile) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        double[] firstNear = convertStringToDoubleArray(netcdfFile.get(IceyeXConstants.FIRST_NEAR.toUpperCase()));
        double[] firstFar = convertStringToDoubleArray(netcdfFile.get(IceyeXConstants.FIRST_FAR.toUpperCase()));
        double[] lastNear = convertStringToDoubleArray(netcdfFile.get(IceyeXConstants.LAST_NEAR.toUpperCase()));
        double[] lastFar = convertStringToDoubleArray(netcdfFile.get(IceyeXConstants.LAST_FAR.toUpperCase()));
        final double latUL = firstNear[2];
        final double lonUL = firstNear[3];
        final double latUR = firstFar[2];
        final double lonUR = firstFar[3];
        final double latLL = lastNear[2];
        final double lonLL = lastNear[3];
        final double latLR = lastFar[2];
        final double lonLR = lastFar[3];

        absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                Double.valueOf(netcdfFile.get(IceyeXConstants.RANGE_SPACING.toUpperCase())));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                Double.valueOf(netcdfFile.get(IceyeXConstants.AZIMUTH_SPACING.toUpperCase())));

        final double[] latCorners = new double[]{latUL, latUR, latLL, latLR};
        final double[] lonCorners = new double[]{lonUL, lonUR, lonLL, lonLR};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

    }

    private static Document convertStringToXMLDocument(String xmlString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return null;
    }

    private static double[] convertStringToDoubleArray(String string) {
        return Arrays.stream(string.substring(1, string.length() - 1).trim().split(",")).mapToDouble(Double::parseDouble).toArray();
    }

    private static double[] convertStringToDoubleArrayBySpace(String string) {
        return Arrays.stream(string.replace("\n", " ").replaceAll("\\s+", " ").replace("  ", " ").replace("[", "").replace("]", "").trim().split(" ")).mapToDouble(Double::parseDouble).toArray();
    }

    private static String[] convertDateStringToStringArray(String string) {
        return string.substring(1, string.length() - 1).replace("'", "").trim().split(",");
    }

    private static String[] convertDateStringToStringArrayBySpace(String string) {
        return string.replace("\n", " ").replaceAll("\\s+", " ").replace("  ", " ").replace("[", "").replace("]", "").trim().split(" ");
    }

    public static ImageReader getTiffIIOReader(final ImageInputStream stream) throws IOException {
        ImageReader reader = null;
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader iioReader = imageReaders.next();
            if (iioReader instanceof TIFFImageReader) {
                reader = iioReader;
                break;
            }
        }
        if (reader == null)
            throw new IOException("Unable to open " + stream.toString());
        reader.setInput(stream, true, true);
        return reader;
    }

    private void initReader() {
        product = null;
        tiffFeilds = null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     */
    @Override
    protected Product readProductNodesImpl() {
        initReader();
        try {

            final File inputFile = ReaderUtils.getFileFromInput(getInput());
            if (inputFile == null) {
                close();
                throw new IllegalFileFormatException("File Could not be interpreted by the reader.");
            }

            TIFFImageMetadata tempNetcdfFile = getTiffMetadata(inputFile);
            if (tempNetcdfFile == null) {
                close();
                throw new IllegalFileFormatException("File metadata Could not be interpreted by the reader.");
            }

            Document document = null;
            for (int i = 0; i < tempNetcdfFile.getRootIFD().getTIFFFields().length; i++) {
                TIFFField tiffFields = tempNetcdfFile.getRootIFD().getTIFFFields()[i];
                if (tiffFields.getType() == 2 && tiffFields.getData() != null && tiffFields.getData() instanceof String[] && ((String[]) tiffFields.getData()).length > 0 && ((String[]) tiffFields.getData())[0].startsWith(IceyeXConstants.GDALMETADATA)) {
                    document = convertStringToXMLDocument(((String[]) tiffFields.getData())[0]);
                }
            }
            if (document == null || document.getFirstChild() == null || document.getFirstChild().getChildNodes() == null) {
                close();
                throw new IllegalFileFormatException("No ICEYE metadata variables found which could\n" +
                        "be interpreted as remote sensing bands.");  /*I18N*/
            }
            NodeList childNodes = document.getFirstChild().getChildNodes();
            this.tiffFeilds = new HashMap<>();
            for (int i = 1; i < childNodes.getLength(); i += 2) {
                this.tiffFeilds.put(childNodes.item(i).getAttributes().item(0).getNodeValue(), childNodes.item(i).getTextContent());
            }
            final String productType = this.tiffFeilds.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase());
            final int rasterWidth = Integer.parseInt(this.tiffFeilds.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(this.tiffFeilds.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));

            product = new Product(inputFile.getName(),
                    productType,
                    rasterWidth, rasterHeight,
                    this);
            product.setFileLocation(inputFile);
            StringBuilder description = new StringBuilder();
            description.append(this.tiffFeilds.get(IceyeXConstants.PRODUCT.toUpperCase())).append(" - ");
            description.append(this.tiffFeilds.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase())).append(" - ");
            description.append(this.tiffFeilds.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase())).append(" - ");
            description.append(this.tiffFeilds.get(IceyeXConstants.MISSION.toUpperCase()));
            product.setDescription(description.toString());
            product.setStartTime(ProductData.UTC.parse(this.tiffFeilds.get(IceyeXConstants.ACQUISITION_START_UTC.toUpperCase()), standardDateFormat));
            product.setEndTime(ProductData.UTC.parse(this.tiffFeilds.get(IceyeXConstants.ACQUISITION_END_UTC.toUpperCase()), standardDateFormat));

            addMetadataToProduct();
            addBandsToProduct();
            addGeoCodingToProduct();
            addTiePointGridsToProduct();
            addCommonSARMetadata(product);
            addDopplerCentroidCoefficients();

            product.getGcpGroup();
            product.setModified(false);
            setQuicklookBandName(product);
            addQuicklook(product, inputFile.getName(), inputFile);

            return product;
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return product;
    }

    private TIFFImageMetadata getTiffMetadata(File inputFile) {
        TIFFImageMetadata iioMetadata = null;
        File file = new File(inputFile.getPath());

        ImageInputStream iis = null;
        try {
            iis = ImageIO.createImageInputStream(file);

            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);

            TIFFImageReader imageReader = null;

            while (imageReaders.hasNext()) {
                final ImageReader reader = imageReaders.next();
                if (reader instanceof TIFFImageReader) {
                    imageReader = (TIFFImageReader) reader;
                    imageReader.setInput(iis);
                    break;
                }
            }
            if (imageReader == null) {
                close();
                throw new IllegalFileFormatException("Image reader Could not be found.");
            }
            iioMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(0);
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return iioMetadata;
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            product = null;
            tiffFeilds = null;
        }
        super.close();
    }

    private void addMetadataToProduct() {
        final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
        for (Map.Entry<String, String> variable : tiffFeilds.entrySet()) {
            addAttribute(origMetadataRoot, variable.getKey(), variable.getValue());
        }
        addAbstractedMetadataHeader(product.getMetadataRoot());
    }

    private void addAbstractedMetadataHeader(MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        try {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, tiffFeilds.get(IceyeXConstants.PRODUCT.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, tiffFeilds.get(IceyeXConstants.PRODUCT_TYPE.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, tiffFeilds.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, tiffFeilds.get(IceyeXConstants.MISSION.toUpperCase()));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, tiffFeilds.get(IceyeXConstants.ACQUISITION_MODE.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, tiffFeilds.get(IceyeXConstants.ANTENNA_POINTING.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS, IceyeXConstants.BEAMS_DEFAULT_VALUE);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, ProductData.UTC.parse(tiffFeilds.get(IceyeXConstants.PROC_TIME_UTC.toUpperCase()), standardDateFormat));


            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, tiffFeilds.get(IceyeXConstants.PROCESSING_SYSTEM_IDENTIFIER.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, Integer.valueOf(tiffFeilds.get(IceyeXConstants.CYCLE.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, Integer.valueOf(tiffFeilds.get(IceyeXConstants.REL_ORBIT.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, Integer.valueOf(tiffFeilds.get(IceyeXConstants.ABS_ORBIT.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, Double.valueOf(tiffFeilds.get(IceyeXConstants.INCIDENCE_NEAR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, Double.valueOf(tiffFeilds.get(IceyeXConstants.INCIDENCE_FAR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slice_num, IceyeXConstants.SLICE_NUM_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.data_take_id, IceyeXConstants.DATA_TAKE_ID_DEFAULT_VALUE);
            String geoRefSystem = IceyeXConstants.GEO_REFERENCE_SYSTEM_DEFAULT_VALUE;
            if (tiffFeilds.get(IceyeXConstants.GEO_REFERENCE_SYSTEM.toUpperCase()) != null) {
                geoRefSystem = tiffFeilds.get(IceyeXConstants.GEO_REFERENCE_SYSTEM.toUpperCase());
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system, geoRefSystem);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, ProductData.UTC.parse(tiffFeilds.get(IceyeXConstants.FIRST_LINE_TIME.toUpperCase()), standardDateFormat));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, ProductData.UTC.parse(tiffFeilds.get(IceyeXConstants.LAST_LINE_TIME.toUpperCase()), standardDateFormat));


            double[] firstNear = convertStringToDoubleArray(tiffFeilds.get(IceyeXConstants.FIRST_NEAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, firstNear[2]);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, firstNear[3]);
            double[] firstFar = convertStringToDoubleArray(tiffFeilds.get(IceyeXConstants.FIRST_FAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, firstFar[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, firstFar[3]);
            double[] lastNear = convertStringToDoubleArray(tiffFeilds.get(IceyeXConstants.LAST_NEAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lastNear[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lastNear[3]);
            double[] lastFar = convertStringToDoubleArray(tiffFeilds.get(IceyeXConstants.LAST_FAR.toUpperCase()));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lastFar[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lastFar[3]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, tiffFeilds.get(IceyeXConstants.PASS.toUpperCase()));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, tiffFeilds.get(IceyeXConstants.MDS1_TX_RX_POLAR.toUpperCase()));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, Float.valueOf(tiffFeilds.get(IceyeXConstants.AZIMUTH_LOOKS.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, Float.valueOf(tiffFeilds.get(IceyeXConstants.RANGE_LOOKS.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, Float.valueOf(tiffFeilds.get(IceyeXConstants.RANGE_SPACING.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, Float.valueOf(tiffFeilds.get(IceyeXConstants.AZIMUTH_SPACING.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, Float.valueOf(tiffFeilds.get(IceyeXConstants.PULSE_REPETITION_FREQUENCY.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, Double.valueOf(tiffFeilds.get(IceyeXConstants.RADAR_FREQUENCY.toUpperCase())) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, Double.valueOf(tiffFeilds.get(IceyeXConstants.LINE_TIME_INTERVAL.toUpperCase())));
            final int rasterWidth = Integer.parseInt(tiffFeilds.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(tiffFeilds.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));
            double totalSize = (rasterHeight * rasterWidth * 2 * 2) / (1024.0f * 1024.0f);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, totalSize);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, Integer.valueOf(tiffFeilds.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, Integer.valueOf(tiffFeilds.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_x, IceyeXConstants.SUBSET_OFFSET_X_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_y, IceyeXConstants.SUBSET_OFFSET_Y_DEFAULT_VALUE);
            if (isComplex) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 0);
            } else {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height, Double.valueOf(tiffFeilds.get(IceyeXConstants.AVG_SCENE_HEIGHT.toUpperCase())));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lat_pixel_res, IceyeXConstants.LAT_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lon_pixel_res, IceyeXConstants.LON_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel, Double.valueOf(tiffFeilds.get(IceyeXConstants.SLANT_RANGE_TO_FIRST_PIXEL.toUpperCase())));

            int antElevCorrFlag = IceyeXConstants.ANT_ELEV_CORR_FLAG_DEFAULT_VALUE;
            if (tiffFeilds.get(IceyeXConstants.ANT_ELEV_CORR_FLAG.toUpperCase()) != null) {
                antElevCorrFlag = Integer.valueOf(tiffFeilds.get(IceyeXConstants.ANT_ELEV_CORR_FLAG.toUpperCase()));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevCorrFlag);

            int rangeSpreadCompFlag = IceyeXConstants.RANGE_SPREAD_COMP_FLAG_DEFAULT_VALUE;
            if (tiffFeilds.get(IceyeXConstants.RANGE_SPREAD_COMP_FLAG.toUpperCase()) != null) {
                rangeSpreadCompFlag = Integer.valueOf(tiffFeilds.get(IceyeXConstants.RANGE_SPREAD_COMP_FLAG.toUpperCase()));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rangeSpreadCompFlag);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, IceyeXConstants.REPLICA_POWER_CORR_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, IceyeXConstants.ABS_CALIBRATION_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor, Double.valueOf(tiffFeilds.get(IceyeXConstants.CALIBRATION_FACTOR.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, IceyeXConstants.INC_ANGLE_COMP_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle, IceyeXConstants.REF_INC_ANGLE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range, IceyeXConstants.REF_SLANT_RANGE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp, IceyeXConstants.REF_SLANT_RANGE_EXP_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor, IceyeXConstants.RESCALING_FACTOR_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, Double.valueOf(tiffFeilds.get(IceyeXConstants.RANGE_SAMPLING_RATE.toUpperCase())) / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, Double.valueOf(tiffFeilds.get(IceyeXConstants.RANGE_BANDWIDTH.toUpperCase())) / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, Double.valueOf(tiffFeilds.get(IceyeXConstants.AZIMUTH_BANDWIDTH.toUpperCase())));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.multilook_flag, IceyeXConstants.MULTI_LOOK_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, IceyeXConstants.CO_REGISTERED_STACK_DEFAULT_VALUE);

            addOrbitStateVectors(absRoot);
            addSRGRCoefficients(absRoot);

        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private void addSRGRCoefficients(final MetadataElement absRoot) {
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);


        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list + ".1");
        srgrCoefficientsElem.addElement(srgrListElem);


        final ProductData.UTC utcTime;
        try {
            utcTime = ProductData.UTC.parse(tiffFeilds.get(IceyeXConstants.GRSR_ZERO_DOPPLER_TIME.toUpperCase()), standardDateFormat);
            srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }


        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, Double.valueOf(tiffFeilds.get(IceyeXConstants.GRSR_GROUND_RANGE_ORIGIN.toUpperCase())));

        final String[] coeffStrArray = convertDateStringToStringArrayBySpace(tiffFeilds.get(IceyeXConstants.GRSR_COEFFICIENTS.toUpperCase()));
        int cnt = 1;
        for (String coeffStr : coeffStrArray) {
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
            srgrListElem.addElement(coefElem);
            ++cnt;
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, Double.parseDouble(coeffStr));
        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot) {

        try {
            final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);


            String[] stateVectorTime = convertDateStringToStringArray(tiffFeilds.get(IceyeXConstants.STATE_VECTOR_TIME.toUpperCase()));
            final int numPoints = stateVectorTime.length;
            final double[] satellitePositionX = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_X_POS.toUpperCase()));
            final double[] satellitePositionY = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_Y_POS.toUpperCase()));
            final double[] satellitePositionZ = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_Z_POS.toUpperCase()));
            final double[] satelliteVelocityX = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_X_VEL.toUpperCase()));
            final double[] satelliteVelocityY = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_Y_VEL.toUpperCase()));
            final double[] satelliteVelocityZ = convertStringToDoubleArrayBySpace(tiffFeilds.get(IceyeXConstants.ORBIT_VECTOR_N_Z_VEL.toUpperCase()));
            ProductData.UTC stateVectorUTC = ProductData.UTC.parse(stateVectorTime[0], standardDateFormat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, stateVectorUTC);
            for (int i = 0; i < numPoints; i++) {
                ProductData.UTC vectorUTC = ProductData.UTC.parse(stateVectorTime[i], standardDateFormat);

                final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i + 1));
                orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, vectorUTC);

                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, satellitePositionX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, satellitePositionY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, satellitePositionZ[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, satelliteVelocityX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, satelliteVelocityY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, satelliteVelocityZ[i]);

                orbitVectorListElem.addElement(orbitVectorElem);
            }
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }

    }

    private void addDopplerCentroidCoefficients() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime;
        try {
            utcTime = ProductData.UTC.parse(convertDateStringToStringArray(tiffFeilds.get(IceyeXConstants.DC_ESTIMATE_TIME_UTC.toUpperCase()))[0], standardDateFormat);
            dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, Double.valueOf(tiffFeilds.get(IceyeXConstants.DC_REFERENCE_PIXEL_TIME.toUpperCase())) * 1e9);

        int dimensionColumn = Integer.valueOf(tiffFeilds.get(IceyeXConstants.DC_ESTIMATE_POLY_ORDER.toUpperCase())) + 1;

        String[] coefValueS = convertDateStringToStringArrayBySpace(tiffFeilds.get(IceyeXConstants.DC_ESTIMATE_COEFFS.toUpperCase()));

        for (int i = 0; i < dimensionColumn; i++) {
            final double coefValue = Double.parseDouble(coefValueS[i]);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    private String getSampleType() {
        if (IceyeXConstants.SLC.equalsIgnoreCase(tiffFeilds.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase()))) {
            isComplex = true;
            return IceyeXConstants.COMPLEX;
        }
        isComplex = false;
        return IceyeXConstants.DETECTED;
    }

    private void addBandsToProduct() {
        try {
            final File inputFile = ReaderUtils.getFileFromInput(getInput());
            String imgPath = inputFile.getPath();
            final String name = imgPath.substring(imgPath.lastIndexOf('/') + 1).toLowerCase();
            final int rasterWidth = Integer.parseInt(this.tiffFeilds.get(IceyeXConstants.NUM_SAMPLES_PER_LINE.toUpperCase()));
            final int rasterHeight = Integer.parseInt(this.tiffFeilds.get(IceyeXConstants.NUM_OUTPUT_LINES.toUpperCase()));
            try (final InputStream inStream = new BufferedInputStream(new FileInputStream(inputFile))) {
                final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, new Dimension(rasterWidth, rasterHeight));
                final ImageIOFile img = new ImageIOFile(name, imgStream, getTiffIIOReader(imgStream), inputFile);
                String polarization = tiffFeilds.get(IceyeXConstants.MDS1_TX_RX_POLAR.toUpperCase());
                String bandName = "Amplitude_" + polarization;
                final Band band = new Band(bandName, ProductData.TYPE_UINT32, rasterWidth, rasterHeight);
                band.setUnit(Unit.AMPLITUDE);
                product.addBand(band);
                bandMap.put(band, new ImageIOFile.BandInfo(band, img, 0, 0));
                SARReader.createVirtualIntensityBand(product, band, '_' + polarization);
            }

        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private void addTiePointGridsToProduct() {
        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();
        final int gridWidth = 11;
        final int gridHeight = 11;
        final int subSamplingX = (int) ((float) sourceImageWidth / (float) (gridWidth - 1));
        final int subSamplingY = (int) ((float) sourceImageHeight / (float) (gridHeight - 1));

        double a = Constants.semiMajorAxis; // WGS 84: equatorial Earth radius in m
        double b = Constants.semiMinorAxis; // WGS 84: polar Earth radius in m

        // get slant range to first pixel and pixel spacing
        final double slantRangeToFirstPixel = Double.parseDouble(tiffFeilds.get(IceyeXConstants.SLANT_RANGE_TO_FIRST_PIXEL.toUpperCase())); // in m
        final double rangeSpacing = Double.parseDouble(tiffFeilds.get(IceyeXConstants.RANGE_SPACING.toUpperCase())); // in m
        final boolean srgrFlag = tiffFeilds.get(IceyeXConstants.SPH_DESCRIPTOR.toUpperCase()).equalsIgnoreCase(IceyeXConstants.GRD);

        // get scene center latitude
        String coordCenter = tiffFeilds.get(IceyeXConstants.COORD_CENTER.toUpperCase());
        double sceneCenterLatitude = Double.parseDouble(coordCenter.substring(1, coordCenter.length() - 1).split(",")[2]); // in deg [3]
        final double nearRangeIncidenceAngle = Double.parseDouble(tiffFeilds.get(IceyeXConstants.INCIDENCE_NEAR.toUpperCase()));


        final double alpha1 = nearRangeIncidenceAngle * Constants.DTOR;
        final double lambda = sceneCenterLatitude * Constants.DTOR;
        final double cos2 = FastMath.cos(lambda) * FastMath.cos(lambda);
        final double sin2 = FastMath.sin(lambda) * FastMath.sin(lambda);
        final double e2 = (b * b) / (a * a);
        final double rt = a * Math.sqrt((cos2 + e2 * e2 * sin2) / (cos2 + e2 * sin2));
        final double rt2 = rt * rt;

        double groundRangeSpacing;
        if (srgrFlag) { // detected
            groundRangeSpacing = rangeSpacing;
        } else {
            groundRangeSpacing = rangeSpacing / FastMath.sin(alpha1);
        }

        double deltaPsi = groundRangeSpacing / rt; // in radian
        final double r1 = slantRangeToFirstPixel;
        final double rtPlusH = Math.sqrt(rt2 + r1 * r1 + 2.0 * rt * r1 * FastMath.cos(alpha1));
        final double rtPlusH2 = rtPlusH * rtPlusH;
        final double theta1 = FastMath.acos((r1 + rt * FastMath.cos(alpha1)) / rtPlusH);
        final double psi1 = alpha1 - theta1;
        double psi = psi1;
        float[] incidenceAngles = new float[gridWidth];
        final int n = gridWidth * subSamplingX;
        int k = 0;
        for (int i = 0; i < n; i++) {
            final double ri = Math.sqrt(rt2 + rtPlusH2 - 2.0 * rt * rtPlusH * FastMath.cos(psi));
            final double alpha = FastMath.acos((rtPlusH2 - ri * ri - rt2) / (2.0 * ri * rt));
            if (i % subSamplingX == 0) {
                int index = k++;
                incidenceAngles[index] = (float) (alpha * Constants.RTOD);
            }

            if (!srgrFlag) { // complex
                groundRangeSpacing = rangeSpacing / FastMath.sin(alpha);
                deltaPsi = groundRangeSpacing / rt;
            }
            psi = psi + deltaPsi;
        }

        float[] incidenceAngleList = new float[gridWidth * gridHeight];
        for (int j = 0; j < gridHeight; j++) {
            System.arraycopy(incidenceAngles, 0, incidenceAngleList, j * gridWidth, gridWidth);
        }

        final TiePointGrid incidentAngleGrid = new TiePointGrid(
                OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, incidenceAngleList);

        incidentAngleGrid.setUnit(Unit.DEGREES);

        product.addTiePointGrid(incidentAngleGrid);

        addSlantRangeTime(product);
    }

    private void addSlantRangeTime(final Product product) {


        final List<CoefList> segmentsArray = new ArrayList<>();

        String[] coeffArray = convertDateStringToStringArrayBySpace(tiffFeilds.get(IceyeXConstants.GRSR_COEFFICIENTS.toUpperCase()));
        final CoefList coefList = new CoefList();

        try {
            coefList.utcSeconds = ProductData.UTC.parse(this.tiffFeilds.get(IceyeXConstants.GRSR_ZERO_DOPPLER_TIME.toUpperCase()), standardDateFormat).getMJD() * 24 * 3600;

            coefList.grOrigin = Double.valueOf(tiffFeilds.get(IceyeXConstants.GRSR_GROUND_RANGE_ORIGIN.toUpperCase()));
            segmentsArray.add(coefList);
            for (String coefString : coeffArray) {
                coefList.coefficients.add(Double.parseDouble(coefString));
            }

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

            final int gridWidth = 11;
            final int gridHeight = 11;
            final int sceneWidth = product.getSceneRasterWidth();
            final int sceneHeight = product.getSceneRasterHeight();
            final int subSamplingX = sceneWidth / (gridWidth - 1);
            final int subSamplingY = sceneHeight / (gridHeight - 1);
            final float[] rangeDist = new float[gridWidth * gridHeight];
            final float[] rangeTime = new float[gridWidth * gridHeight];

            setRangeDist(absRoot, segmentsArray, gridWidth, gridHeight, subSamplingX, rangeDist);
            // get slant range time in nanoseconds from range distance in meters
            setRangeTime(absRoot, rangeDist, rangeTime);

            final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                    gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, rangeTime);

            product.addTiePointGrid(slantRangeGrid);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
        } catch (ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    private void setRangeTime(MetadataElement absRoot, float[] rangeDist, float[] rangeTime) {
        for (int i = 0; i < rangeDist.length; i++) {
            rangeTime[i] = (float) (rangeDist[i] / Constants.halfLightSpeed * Constants.oneBillion); // in ns
        }
    }

    private void setRangeDist(MetadataElement absRoot, List<CoefList> segmentsArray, int gridWidth, int gridHeight, int subSamplingX, float[] rangeDist) throws ParseException {
        final double lineTimeInterval = Double.parseDouble(this.tiffFeilds.get(IceyeXConstants.LINE_TIME_INTERVAL.toUpperCase()));
        final double startSeconds = ProductData.UTC.parse(this.tiffFeilds.get(IceyeXConstants.FIRST_LINE_TIME.toUpperCase()), standardDateFormat).getMJD() * 24 * 3600;
        final double pixelSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 0);

        final CoefList[] segments = segmentsArray.toArray(new CoefList[segmentsArray.size()]);

        int k = 0;
        int c = 0;
        for (int j = 0; j < gridHeight; j++) {
            final double time = startSeconds + (j * lineTimeInterval);
            while (c < segments.length && segments[c].utcSeconds < time)
                ++c;
            if (c >= segments.length)
                c = segments.length - 1;

            final CoefList coef = segments[c];
            final double GR0 = coef.grOrigin;
            final double s0 = coef.coefficients.get(0);
            final double s1 = coef.coefficients.get(1);
            final double s2 = coef.coefficients.get(2);
            final double s3 = coef.coefficients.get(3);
            final double s4 = coef.coefficients.get(4);

            for (int i = 0; i < gridWidth; i++) {
                int x = i * subSamplingX;
                final double GR = x * pixelSpacing;
                final double g = GR - GR0;
                final double g2 = g * g;

                //SlantRange = s0 + s1(GR - GR0) + s2(GR-GR0)^2 + s3(GRGR0)^3 + s4(GR-GR0)^4;
                rangeDist[k++] = (float) (s0 + s1 * g + s2 * g2 + s3 * g2 * g + s4 * g2 * g2);
            }
        }
    }

    private void addGeoCodingToProduct() {
        addGeoCodingFromMetadata(product, this.tiffFeilds);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final ImageIOFile.BandInfo bandInfo = bandMap.get(destBand);
        if (bandInfo != null && bandInfo.img != null) {
            if (tiffFeilds.get(IceyeXConstants.PASS.toUpperCase()).equalsIgnoreCase(IceyeXConstants.ASCENDING)) {
                readAscendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset);
            } else {
                readDescendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset);
            }
        }
    }

    private void readAscendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                         final int sourceStepX, final int sourceStepY,
                                         final ProductData destBuffer,
                                         final int destOffsetX, final int destOffsetY,
                                         final int destWidth, final int destHeight,
                                         final int imageID, final ImageIOFile img,
                                         final int bandSampleOffset) throws IOException {
        final Raster data;

        synchronized (tiffFeilds) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);
            final RenderedImage image = reader.readAsRenderedImage(0, param);
            data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;
        sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
    }

    private void readDescendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                          final int sourceStepX, final int sourceStepY,
                                          final ProductData destBuffer,
                                          final int destOffsetX, final int destOffsetY,
                                          final int destWidth, final int destHeight,
                                          final int imageID, final ImageIOFile img,
                                          final int bandSampleOffset) throws IOException {

        final Raster data;
        synchronized (tiffFeilds) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        if (destBuffer.getType() == ProductData.TYPE_FLOAT32) {
            sampleModel.getSamples(0, 0, w, h, sampleOffset, (float[]) destBuffer.getElems(), dataBuffer);
        } else {
            sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
        }
    }

    class CoefList {
        final java.util.List<Double> coefficients = new ArrayList<>();
        double utcSeconds = 0.0;
        double grOrigin = 0.0;
    }

}

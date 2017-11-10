package org.esa.snap.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.TreeNode;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.envi.Header.BeamProperties;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class EnviProductReader extends AbstractProductReader {

    private final HashMap<Band, Long> bandStreamPositionMap = new HashMap<>();
    private final HashMap<Band, ImageInputStream> imageInputStreamMap = new HashMap<>();
    private final HashMap<Band, Header> headerMap = new HashMap<>(10);
    private ZipFile productZip = null;

    public EnviProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public static File getEnviImageFile(File headerFile) throws IOException {
        final String hdrName = headerFile.getName();
        final String imgName = hdrName.substring(0, hdrName.indexOf('.'));
        final File parentFolder = headerFile.getParentFile();

        for (final String ext : EnviConstants.IMAGE_EXTENSIONS) {
            final File imgFileLowerCase = new File(parentFolder, imgName + ext);
            if (imgFileLowerCase.exists()) {
                return imgFileLowerCase;
            }

            final File imgFileUpperCase = new File(parentFolder, imgName + ext.toUpperCase());
            if (imgFileUpperCase.exists()) {
                return imgFileUpperCase;
            }
        }

        final File[] files = parentFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!Files.isSameFile(file.toPath(), headerFile.toPath()) && file.getName().startsWith(imgName)) {
                    return file;
                }
            }
        }

        throw new IOException("No matching ENVI image file found for header file: " + headerFile.getPath());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            return innerReadProductNodes();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private Product innerReadProductNodes() throws IOException {
        final Object inputObject = getInput();
        final File inputFile = EnviProductReaderPlugIn.getInputFile(inputObject);

        final BufferedReader headerReader = getHeaderReader(inputFile);

        final String inputFileName = inputFile.getName();
        String[] splittedInputFileName = inputFileName.split("!");
        String productName = splittedInputFileName.length > 1 ? splittedInputFileName[1] : splittedInputFileName[0];
        int dotIndex = productName.lastIndexOf('.');
        if (dotIndex > -1) {
            productName = productName.substring(0, dotIndex);
        }

        try {
            final Header header = new Header(headerReader);

            final Product product = new Product(productName, header.getSensorType(), header.getNumSamples(),
                    header.getNumLines());
            product.setProductReader(this);
            product.setFileLocation(inputFile);
            product.setDescription(header.getDescription());
            product.getMetadataRoot().addElement(header.getAsMetadata());

            initGeoCoding(product, header);
            initBands(inputFile, product, header);

            applyBeamProperties(product, header.getBeamProperties());

            return product;
        } finally {
            if (headerReader != null) {
                headerReader.close();
            }
        }
    }

    @Override
    protected void readBandRasterDataImpl(final int sourceOffsetX, final int sourceOffsetY,
                                          final int sourceWidth, final int sourceHeight,
                                          final int sourceStepX, final int sourceStepY,
                                          final Band destBand,
                                          final int destOffsetX, final int destOffsetY,
                                          final int destWidth, final int destHeight,
                                          final ProductData destBuffer,
                                          final ProgressMonitor pm) throws IOException {

        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        Product product = destBand.getProduct();
        final int sourceRasterWidth = product.getSceneRasterWidth();
        final ImageInputStream imageInputStream = imageInputStreamMap.get(destBand);
        final int elemSize = destBuffer.getElemSize();

        Header header = headerMap.get(destBand);
        final int headerOffset = header.getHeaderOffset();
        final int bandIndex = product.getBandIndex(destBand.getName());

        String interleave = header.getInterleave();
        if ("bil".equalsIgnoreCase(interleave)) {
            // band interleaved by line
            final long lineSizeInBytes = header.getNumSamples() * elemSize;
            int numBands = product.getNumBands();

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (imageInputStream) {
                        long lineStartPos = headerOffset + sourceY * numBands * lineSizeInBytes + bandIndex * lineSizeInBytes;
                        imageInputStream.seek(lineStartPos + elemSize * sourceOffsetX);
                        destBuffer.readFrom(destPos, destWidth, imageInputStream);
                        destPos += destWidth;
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        } else if ("bip".equalsIgnoreCase(interleave)) {
            // band interleaved by pixel
            int numBands = product.getNumBands();
            final long lineSizeInBytes = header.getNumSamples() * numBands * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth * numBands);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (imageInputStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        imageInputStream.seek(lineStartPos + elemSize * sourceOffsetX * numBands);
                        lineData.readFrom(0, sourceWidth * numBands, imageInputStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x * numBands + bandIndex));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        } else {
            // band sequential (bsq), the default
            final long bandStartPosition = bandStreamPositionMap.get(destBand);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = (long) sourceY * (long) sourceRasterWidth;
                    synchronized (imageInputStream) {
                        long pos = bandStartPosition + elemSize * (sourcePosY + sourceOffsetX);
                        try {
                            imageInputStream.seek(pos);
                        } catch (IndexOutOfBoundsException e) {
                            System.out.printf("pos=%d%n", pos);
                            throw e;
                        }
                        destBuffer.readFrom(destPos, destWidth, imageInputStream);
                        destPos += destWidth;
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (Band band : imageInputStreamMap.keySet()) {
            final ImageInputStream imageInputStream = imageInputStreamMap.get(band);
            if (imageInputStream != null) {
                imageInputStream.close();
            }
        }
        if (productZip != null) {
            productZip.close();
            productZip = null;
        }
        super.close();
    }

    @Override
    public TreeNode<File> getProductComponents() {
        try {
            final File headerFile = EnviProductReaderPlugIn.getInputFile(getInput());
            File parentDir = headerFile.getParentFile();
            final TreeNode<File> root = new TreeNode<>(parentDir.getCanonicalPath());
            root.setContent(parentDir);

            final TreeNode<File> header = new TreeNode<>(headerFile.getName());
            header.setContent(headerFile);
            root.addChild(header);

            if (productZip == null) {
                final File imageFile = getEnviImageFile(headerFile);
                final TreeNode<File> image = new TreeNode<>(imageFile.getName());
                image.setContent(imageFile);
                root.addChild(image);
            }

            return root;

        } catch (IOException ignored) {
            return null;
        }
    }

    private ImageInputStream initializeInputStreamForBandData(File inputFile, ByteOrder byteOrder) throws IOException {
        ImageInputStream imageInputStream;
        if (EnviProductReaderPlugIn.isCompressedFile(inputFile)) {
            imageInputStream = createImageStreamFromZip(inputFile);
        } else {
            imageInputStream = createImageStreamFromFile(inputFile);
        }
        imageInputStream.setByteOrder(byteOrder);
        return imageInputStream;
    }

    protected static void applyBeamProperties(Product product, BeamProperties beamProperties) throws IOException {
        if (beamProperties == null) {
            return;
        }
        final String sensingStart = beamProperties.getSensingStart();
        if (sensingStart != null) {
            try {
                product.setStartTime(ProductData.UTC.parse(sensingStart));
            } catch (ParseException e) {
                final String message = e.getMessage() + " at property sensingStart in the header file.";
                throw new IOException(message, e);
            }
        }

        final String sensingStop = beamProperties.getSensingStop();
        if (sensingStop != null) {
            try {
                product.setEndTime(ProductData.UTC.parse(sensingStop));
            } catch (ParseException e) {
                final String message = e.getMessage() + " at property sensingStop in the header file.";
                throw new IOException(message, e);
            }
        }
    }

    private ImageInputStream createImageStreamFromZip(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        String innerHdrZipPath;
        if (filePath.contains("!")) {
            // headerFile is in zip
            String[] splittedHeaderFile = filePath.split("!");
            productZip = new ZipFile(new File(splittedHeaderFile[0]));
            innerHdrZipPath = splittedHeaderFile[1].replace("\\", "/");
        } else {
            productZip = new ZipFile(file, ZipFile.OPEN_READ);
            innerHdrZipPath = findFirstHeader(productZip).getName();
        }

        try {

            innerHdrZipPath = innerHdrZipPath.substring(0, innerHdrZipPath.length() - 4);
            List<String> innerZipDataFilePaths = new ArrayList<>();
            for (int i = 0; i < EnviConstants.IMAGE_EXTENSIONS.length; i++) {
                innerZipDataFilePaths.add(FileUtils.ensureExtension(innerHdrZipPath, EnviConstants.IMAGE_EXTENSIONS[i]));
            }
            final Enumeration<? extends ZipEntry> enumeration = productZip.entries();
            // iterating over entries instead of using the path directly in order to compare paths ignoring case
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                for (String innerZipDataFilePath : innerZipDataFilePaths) {
                    if (zipEntry.getName().equalsIgnoreCase(innerZipDataFilePath)) {
                        InputStream inputStream = productZip.getInputStream(productZip.getEntry(zipEntry.getName()));
                        return new FileCacheImageInputStream(inputStream, null);
                    }
                }
            }
        } catch (IOException ioe) {
            try {
                // close stream only if exception occurred, otherwise band data is not readable
                // it will be closed when the reader is closed
                productZip.close();
            } catch (IOException ignored) {
            }
            throw ioe;
        }

        throw new IOException("Not able to initialise band input stream.");
    }

    private static ImageInputStream createImageStreamFromFile(final File file) throws IOException {
        final File imageFile = getEnviImageFile(file);
        return new FileImageInputStream(imageFile);
    }

    protected void initGeoCoding(final Product product, final Header header) {
        final EnviMapInfo enviMapInfo = header.getMapInfo();
        if (enviMapInfo == null) {
            return;
        }
        final EnviProjectionInfo projectionInfo = header.getProjectionInfo();
        CoordinateReferenceSystem crs = null;
        if (projectionInfo != null) {
            try {
                crs = EnviCrsFactory.createCrs(projectionInfo.getProjectionNumber(), projectionInfo.getParameter(),
                        enviMapInfo.getDatum(), enviMapInfo.getUnit());
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (EnviConstants.PROJECTION_NAME_WGS84.equalsIgnoreCase(enviMapInfo.getProjectionName())) {
            crs = DefaultGeographicCRS.WGS84;
        } else if ("UTM".equalsIgnoreCase(enviMapInfo.getProjectionName())) {
            try {
                final int zone = enviMapInfo.getUtmZone();
                final String hemisphere = enviMapInfo.getUtmHemisphere();

                if (zone >= 1 && zone <= 60) {
                    final CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
                    if ("North".equalsIgnoreCase(hemisphere)) {
                        final int WGS84_UTM_zone_N_BASE = 32600;
                        crs = factory.createProjectedCRS("EPSG:" + (WGS84_UTM_zone_N_BASE + zone));
                    } else {
                        final int WGS84_UTM_zone_S_BASE = 32700;
                        crs = factory.createProjectedCRS("EPSG:" + (WGS84_UTM_zone_S_BASE + zone));
                    }
                }
            } catch (FactoryException ignore) {
            }
        }


        if (crs != null) {
            try {
                AffineTransform i2m = new AffineTransform();
                i2m.translate(enviMapInfo.getEasting(), enviMapInfo.getNorthing());
                i2m.scale(enviMapInfo.getPixelSizeX(), -enviMapInfo.getPixelSizeY());
                i2m.rotate(Math.toRadians(-enviMapInfo.getOrientation()));
                i2m.translate(-(enviMapInfo.getReferencePixelX() - 1), -(enviMapInfo.getReferencePixelY() - 1));
                Rectangle rect = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
                GeoCoding geoCoding = new CrsGeoCoding(crs, rect, i2m);

                product.setSceneGeoCoding(geoCoding);
            } catch (FactoryException | TransformException fe) {
                Debug.trace(fe);
            }
        }

    }

    /*
     * Creates a buffered reader that is opened on the *.hdr file to read the header information.
     * This method works for both compressed and uncompressed ENVI files.
     *
     * @param inputFile the input file
     *
     * @return a reader on the header file
     *
     * @throws IOException on disk IO failures
     */
    protected BufferedReader getHeaderReader(File inputFile) throws IOException {
        if (EnviProductReaderPlugIn.isCompressedFile(inputFile)) {
            ZipFile zipFile;
            ZipEntry zipEntry;

            if (inputFile.getPath().toLowerCase().endsWith(EnviConstants.ZIP_EXTENSION)) {
                zipFile = new ZipFile(inputFile);
                zipEntry = findFirstHeader(zipFile);
            } else {
                String[] splittedHeaderFile = inputFile.getAbsolutePath().split("!");
                zipFile = new ZipFile(new File(splittedHeaderFile[0]));
                final String innerZipPath = splittedHeaderFile[1].replace("\\", "/");
                zipEntry = zipFile.getEntry(innerZipPath);
            }
            if (zipEntry == null) {
                throw new IOException("No .hdr file found in zip file.");
            }
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            return new BufferedReader(new InputStreamReader(inputStream));
        } else {
            return new BufferedReader(new FileReader(inputFile));
        }
    }

    private static ZipEntry findFirstHeader(ZipFile zipFile) {
        final Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();
        while (entryEnum.hasMoreElements()) {
            ZipEntry entry = entryEnum.nextElement();
            if (entry.getName().toLowerCase().endsWith(EnviConstants.HDR_EXTENSION)) {
                return entry;
            }
        }
        return null;
    }

    protected void initBands(File inputFile, Product product, Header header) throws IOException {
        final int enviDataType = header.getDataType();
        final int dataType = DataTypeUtils.toBeam(enviDataType);
        Double dataIgnoreValue = header.getDataIgnoreValue();

        final int sizeInBytes = DataTypeUtils.getSizeInBytes(enviDataType);
        final int bandSizeInBytes = header.getNumSamples() * header.getNumLines() * sizeInBytes;
        final int headerOffset = header.getHeaderOffset();

        final String[] bandNames = getBandNames(header);
        float[] wavelength = getWavelength(header, bandNames.length);
        float[] bandwidth = getBandwidth(header, bandNames.length);
        double[] offsets = getOffsetValues(bandNames.length, header);
        final double[] gains = getGainValues(bandNames.length, header);
        for (int i = 0; i < bandNames.length; i++) {
            final String originalBandName = bandNames[i];
            final String validBandName;
            final String description;
            if (ProductNode.isValidNodeName(originalBandName)) {
                validBandName = originalBandName;
                description = "";
            } else {
                validBandName = createValidNodeName(originalBandName);
                description = "non formatted band name: " + originalBandName;
            }
            final Band band = new Band(validBandName,
                    dataType,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight());
            band.setDescription(description);
            band.setSpectralWavelength(wavelength[i]);
            band.setSpectralBandwidth(bandwidth[i]);
            band.setScalingOffset(offsets[i]);
            band.setScalingFactor(gains[i]);
            if (dataIgnoreValue != null) {
                band.setNoDataValueUsed(true);
                band.setNoDataValue(dataIgnoreValue);
            }
            product.addBand(band);

            final long bandStartPosition = headerOffset + bandSizeInBytes * i;
            bandStreamPositionMap.put(band, bandStartPosition);
            imageInputStreamMap.put(band, initializeInputStreamForBandData(inputFile, header.getJavaByteOrder()));
            headerMap.put(band, header);
        }

        int numClasses = header.getNumClasses();
        String[] classNames = header.getClassNmaes();
        if (numClasses > 0 && classNames.length == numClasses) {
            final IndexCoding indexCoding = new IndexCoding("classification");
            for (int i = 0; i < numClasses; i++) {
                indexCoding.addIndex(classNames[i], i, "");
            }
            product.getIndexCodingGroup().add(indexCoding);
            Band[] bands = product.getBands();
            for (Band band : bands) {
                band.setSampleCoding(indexCoding);
            }

            int[] classRGB = header.getClassColorRGB();
            if (classRGB.length == numClasses * 3) {
                final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[numClasses];
                for (int i = 0; i < numClasses; i++) {
                    Color color = new Color(classRGB[i * 3], classRGB[i * 3 + 1], classRGB[i * 3 + 2]);
                    points[i] = new ColorPaletteDef.Point(i, color, classNames[i]);
                }
                ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(points, points.length));
                for (Band band : bands) {
                    band.setImageInfo(imageInfo);
                }
            }
        }
    }

    private double[] getOffsetValues(final int numBands, final Header header) {
        double[] dataOffsetValues = header.getDataOffsetValues();
        if (dataOffsetValues.length == 0) {
            dataOffsetValues = new double[numBands];
            Arrays.fill(dataOffsetValues, 0.0);
            return dataOffsetValues;

        } else {
            return dataOffsetValues;
        }
    }

    private double[] getGainValues(final int numBands, final Header header) {
        double[] dataGainValues = header.getDataGainValues();
        if (dataGainValues.length == 0) {
            dataGainValues = new double[numBands];
            Arrays.fill(dataGainValues, 1.0);
            return dataGainValues;
        } else {
            return dataGainValues;
        }
    }

    static String[] getBandNames(final Header header) {
        String[] bandNames = header.getBandNames();
        // there must be at least 1 bandname because in DIMAP-Files are no bandnames given.
        if (bandNames.length == 0) {
            int numBands = header.getNumBands();
            if (numBands == 0) {
                return new String[]{"Band"};
            } else {
                bandNames = new String[numBands];
                for (int i = 0; i < bandNames.length; i++) {
                    bandNames[i] = "Band_" + (i + 1);
                }
                return bandNames;
            }
        } else {
            return bandNames;
        }
    }

    private float[] getWavelength(Header header, int numBands) {
        return transformWavelength(header.getWavelengths(), header.getWavelengthsUnit(), numBands);
    }

    private float[] getBandwidth(Header header, int numBands) {
        return transformWavelength(header.getFWHM(), header.getWavelengthsUnit(), numBands);
    }

    private float[] transformWavelength(String[] wavelengthsStrings, String wavelengthsUnit, int numBands) {
        float[] wavelengths = new float[numBands];
        int scaleFactor = 1;
        if (wavelengthsUnit != null && wavelengthsUnit.equalsIgnoreCase("Micrometers")) {
            scaleFactor = 1000;
        }
        if (wavelengthsStrings != null && wavelengthsStrings.length == numBands) {
            for (int i = 0; i < wavelengthsStrings.length; i++) {
                wavelengths[i] = Float.parseFloat(wavelengthsStrings[i]) * scaleFactor;
            }
        }
        return wavelengths;
    }

    private static String createValidNodeName(final String originalBandName) {
        String name = StringUtils.createValidName(originalBandName, null, '_');
        while (name.startsWith("_")) {
            name = name.substring(1);
        }
        while (name.endsWith("_")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }
}

package org.esa.beam.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envi.Header.BeamProperties;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


class EnviProductReader extends AbstractProductReader {

    private ImageInputStream imageInputStream;
    private ZipFile productZip;
    private Header header;

    EnviProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    static File getEnviImageFile(File hdrFile) throws FileNotFoundException {
        List<File> possibleImageFiles = new ArrayList<>(20);
        String baseName = FileUtils.getFilenameWithoutExtension(hdrFile);
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.IMG_EXTENSION));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.IMG_EXTENSION.toUpperCase()));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIN_EXTENSION));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIN_EXTENSION.toUpperCase()));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIL_EXTENSION));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIL_EXTENSION.toUpperCase()));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BSQ_EXTENSION));
        possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BSQ_EXTENSION.toUpperCase()));

        String lowerbase = baseName.toLowerCase();
        if (lowerbase.endsWith(EnviConstants.IMG_EXTENSION) ||
                lowerbase.endsWith(EnviConstants.BIN_EXTENSION) ||
                lowerbase.endsWith(EnviConstants.BIL_EXTENSION)
                ) {
            baseName = FileUtils.getFilenameWithoutExtension(baseName);
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.IMG_EXTENSION));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.IMG_EXTENSION.toUpperCase()));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIN_EXTENSION));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIN_EXTENSION.toUpperCase()));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIL_EXTENSION));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BIL_EXTENSION.toUpperCase()));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BSQ_EXTENSION));
            possibleImageFiles.add(new File(hdrFile.getParent(), baseName + EnviConstants.BSQ_EXTENSION.toUpperCase()));
        }

        for (File possibleImageFile : possibleImageFiles) {
            if (possibleImageFile.exists()) {
                return possibleImageFile;
            }
        }
        throw new FileNotFoundException("no image file found for header: <" + hdrFile.toString() + ">");
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
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
            header = new Header(headerReader);

            final Product product = new Product(productName, header.getSensorType(), header.getNumSamples(),
                                                header.getNumLines());
            product.setProductReader(this);
            product.setFileLocation(inputFile);
            product.setDescription(header.getDescription());
            product.getMetadataRoot().addElement(header.getAsMetadata());

            initGeoCoding(product);
            initBands(product);

            applyBeamProperties(product, header.getBeamProperties());

            // imageInputStream must be initialized last
            initializeInputStreamForBandData(inputFile, header.getJavaByteOrder());

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

        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = sourceOffsetX + sourceWidth - 1;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        Product product = destBand.getProduct();
        final int sourceRasterWidth = product.getSceneRasterWidth();
        final int elemSize = destBuffer.getElemSize();

        final int headerOffset = header.getHeaderOffset();
        final int bandIndex = product.getBandIndex(destBand.getName());

        String interleave = header.getInterleave();
        if ("bil".equalsIgnoreCase(interleave)) {
            // band interleaved by line
            final long lineSizeInBytes = header.getNumSamples() * elemSize;
            int numBands = product.getNumBands();

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
            try {
                int destPos = 0;
                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (imageInputStream) {
                        long lineStartPos = headerOffset + sourceY * numBands * lineSizeInBytes + bandIndex * lineSizeInBytes;
                        imageInputStream.seek(lineStartPos + elemSize * sourceMinX);
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

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
            try {
                int destPos = 0;
                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (imageInputStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        imageInputStream.seek(lineStartPos + elemSize * sourceMinX * numBands);
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
            final long bandSizeInBytes = header.getNumSamples() * header.getNumLines() * elemSize;

            long bandStartPosition = headerOffset + bandSizeInBytes * bandIndex;
            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
            try {
                int destPos = 0;
                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = (long)sourceY * (long)sourceRasterWidth;
                    synchronized (imageInputStream) {
                        long pos = bandStartPosition + elemSize * (sourcePosY + sourceMinX);
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
        if (imageInputStream != null) {
            imageInputStream.close();
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

    private void initializeInputStreamForBandData(File inputFile, ByteOrder byteOrder) throws IOException {
        if (EnviProductReaderPlugIn.isCompressedFile(inputFile)) {
            imageInputStream = createImageStreamFromZip(inputFile);
        } else {
            imageInputStream = createImageStreamFromFile(inputFile);
        }
        imageInputStream.setByteOrder(byteOrder);
    }

    private static void applyBeamProperties(Product product, BeamProperties beamProperties) throws IOException {
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
            String innerImgZipPath = FileUtils.ensureExtension(innerHdrZipPath, EnviConstants.IMG_EXTENSION);
            final Enumeration<? extends ZipEntry> enumeration = productZip.entries();
            // iterating over entries instead of using the path directly in order to compare paths ignoring case
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (zipEntry.getName().equalsIgnoreCase(innerImgZipPath)) {
                    InputStream inputStream = productZip.getInputStream(productZip.getEntry(zipEntry.getName()));
                    return new FileCacheImageInputStream(inputStream, null);
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

    private void initGeoCoding(final Product product) {
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
            } catch (NoSuchAuthorityCodeException ignore) {
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

                product.setGeoCoding(geoCoding);
            } catch (FactoryException fe) {
                Debug.trace(fe);
            } catch (TransformException te) {
                Debug.trace(te);
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
    private BufferedReader getHeaderReader(File inputFile) throws IOException {
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

    private void initBands(Product product) {
        final int enviDataType = header.getDataType();
        final int dataType = DataTypeUtils.toBeam(enviDataType);
        Double dataIgnoreValue = header.getDataIgnoreValue();

        final String[] bandNames = getBandNames(header);
        float[] wavelength = getWavelength(header, bandNames.length);
        float[] bandwidth = getBandwidth(header, bandNames.length);
        double[] offsets = getOffsetValues(bandNames.length);
        final double[] gains = getGainValues(bandNames.length);
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
                    Color color = new Color(classRGB[i*3], classRGB[i*3 + 1], classRGB[i*3 + 2]);
                    points[i] = new ColorPaletteDef.Point(i, color, classNames[i]);
                }
                ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(points, points.length));
                for (Band band : bands) {
                    band.setImageInfo(imageInfo);
                }
            }
        }
    }

    private double[] getOffsetValues(int numBands) {
        double[] dataOffsetValues = header.getDataOffsetValues();
        if (dataOffsetValues.length == 0) {
            dataOffsetValues = new double[numBands];
            Arrays.fill(dataOffsetValues, 0.0);
            return dataOffsetValues;

        } else {
            return dataOffsetValues;
        }
    }

    private double[] getGainValues(int numBands) {
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
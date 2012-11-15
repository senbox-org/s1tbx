package org.esa.beam.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envi.Header.BeamProperties;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.ByteOrder;
import java.awt.geom.AffineTransform;
import java.awt.*;

// @todo 2 tb/** use header offset information in file positioning
// @todo 2 tb/** evaluate file type information and react accordingly
// @todo 2 tb/** evaluate data type information and react accordingly

public class EnviProductReader extends AbstractProductReader {

    private final HashMap<Band, Long> bandStreamPositionMap = new HashMap<Band, Long>();
    private final HashMap<Band, ImageInputStream> imageInputStreamMap = new HashMap<Band, ImageInputStream>();
    private final HashMap<Band, Header> headerMap = new HashMap<Band, Header>(10);
    private ZipFile productZip = null;

    public EnviProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public static File getEnviImageFile(File headerFile) {
        final String hdrName = headerFile.getName();
        final String imgName = hdrName.substring(0, hdrName.indexOf('.'));
        final File parentFolder = headerFile.getParentFile();
        for(final String ext : EnviConstants.IMAGE_EXTENSIONS) {
            final File imgFile = new File(parentFolder, imgName + ext);
            if (imgFile.exists())
                return imgFile;
        }
        
        final File[] files = parentFolder.listFiles();
        for(File f : files) {
            if(f != headerFile && f.getName().startsWith(imgName)) {
                return f;
            }
        }
        return new File(parentFolder, imgName);
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
            final Header header = new Header(headerReader);

            final Product product = new Product(productName, header.getSensorType(), header.getNumSamples(),
                                                header.getNumLines());
            product.setProductReader(this);
            product.setDescription(header.getDescription());

            initGeoCoding(product, header);
            initBands(inputFile, product, header);

            applyBeamProperties(product, header.getBeamProperties());

            // imageInputStream must be initialized last
            initializeInputStreamForBandData(inputFile, header.getJavaByteOrder());
	        initMetadata(product, inputFile);
            product.setFileLocation(inputFile);
            
            return product;
        } finally {
            if (headerReader != null) {
                headerReader.close();
            }
        }
    }

    protected void initMetadata(final Product product, final File inputFile) throws IOException {

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
        final Product product = destBand.getProduct();
        final int sourceRasterWidth = product.getSceneRasterWidth();
        final long bandOffset = bandStreamPositionMap.get(destBand);
        final ImageInputStream imageInputStream = imageInputStreamMap.get(destBand);

        final int elemSize = destBuffer.getElemSize();

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
        // For each scan in the data source
        try {
            int destPos = 0;
            Header header = headerMap.get(destBand);
            String interleave = header.getInterleave();
            if (interleave.equalsIgnoreCase("bil")) {
                // band interleaved by line
                final int lineSizeInBytes = header.getNumSamples() * elemSize;
                final int headerOffset = header.getHeaderOffset();
                int numBands = product.getNumBands();
                final int bandIndex = product.getBandIndex(destBand.getName());

                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = sourceY * sourceRasterWidth;
                    synchronized (imageInputStream) {
                        final long lineStartPos = headerOffset + sourceY * numBands * lineSizeInBytes + bandIndex * lineSizeInBytes;
                        imageInputStream.seek(lineStartPos + elemSize * sourceMinX);
                        destBuffer.readFrom(destPos, destWidth, imageInputStream);
                        destPos += destWidth;
                    }
                    pm.worked(1);
                }
            } else if (interleave.equalsIgnoreCase("bip")) {
                // band interleaved by pixel
                throw new UnsupportedOperationException("BIP not supported");
            } else {

                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = sourceY * sourceRasterWidth;
                    synchronized (imageInputStream) {
                        if (sourceStepX == 1) {
                            imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceMinX));
                            destBuffer.readFrom(destPos, destWidth, imageInputStream);
                            destPos += destWidth;
                        } else {
                            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                                imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                                destBuffer.readFrom(destPos, 1, imageInputStream);
                                destPos++;
                            }
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void close() throws IOException {
        for(Band band : imageInputStreamMap.keySet()) {
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
            final TreeNode<File> root = new TreeNode<File>(parentDir.getCanonicalPath());
            root.setContent(parentDir);

            final TreeNode<File> header = new TreeNode<File>(headerFile.getName());
            header.setContent(headerFile);
            root.addChild(header);

            if (productZip == null) {
                final File imageFile = getEnviImageFile(headerFile);
                final TreeNode<File> image = new TreeNode<File>(imageFile.getName());
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

    public static void applyBeamProperties(Product product, BeamProperties beamProperties) throws IOException {
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

        if (!imageFile.exists()) {
            throw new FileNotFoundException("file not found: <" + imageFile + ">");
        }
        return new FileImageInputStream(imageFile);
    }

    public static void initGeoCoding(final Product product, final Header header) {
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
    public static BufferedReader getHeaderReader(File inputFile) throws IOException {
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

    public void initBands(File inputFile, Product product, Header header) throws IOException {
        final int enviDataType = header.getDataType();
        final int dataType = DataTypeUtils.toBeam(enviDataType);
        final int sizeInBytes = DataTypeUtils.getSizeInBytes(enviDataType);
        final int bandSizeInBytes = header.getNumSamples() * header.getNumLines() * sizeInBytes;

        final int headerOffset = header.getHeaderOffset();

        final String[] bandNames = getBandNames(header);
        for (int i = 0; i < bandNames.length; i++) {
            final String originalBandName = bandNames[i];
            String validBandName;
            final String description;
            if (ProductNode.isValidNodeName(originalBandName)) {
                validBandName = originalBandName;
                description = "";
            } else {
                validBandName = createValidNodeName(originalBandName);
                description = "non formatted band name: " + originalBandName;
            }
            validBandName = formatBandName(validBandName);

            final Band band = new Band(validBandName,
                                       dataType,
                                       product.getSceneRasterWidth(),
                                       product.getSceneRasterHeight());
            band.setDescription(description);

            final String name = validBandName.toLowerCase();
            if(name.contains("real")) {
                band.setUnit("real");
            } else if(name.contains("imag") && !name.contains("image") && !name.contains("imagary")) {
                band.setUnit("imaginary");
            } else if(name.contains("phase")) {
                band.setUnit("phase");
            } else {
                band.setUnit("amplitude");
            }
            product.addBand(band);

            final long bandStartPosition = headerOffset + bandSizeInBytes * i;
            bandStreamPositionMap.put(band, bandStartPosition);
            imageInputStreamMap.put(band, initializeInputStreamForBandData(inputFile, header.getJavaByteOrder()));
            headerMap.put(band, header);
        }
    }

    protected static String[] getBandNames(final Header header) {
        String[] bandNames = header.getBandNames();
        // there must be at least 1 bandname because in DIMAP-Files are no bandnames given.
        if (bandNames == null || bandNames.length == 0) {
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

    private static float[] getWavelength(Header header, String[] bandNames) {
        float[] wavelengths = new float[bandNames.length];
        String[] wavelengthsStrings = header.getWavelengths();
        String wavelengthsUnit = header.getWavelengthsUnit();
        int scaleFactor = 1;
        if (wavelengthsUnit != null && wavelengthsUnit.equalsIgnoreCase("Micrometers")) {
            scaleFactor = 1000;
        }
        if (wavelengthsStrings != null && wavelengthsStrings.length == bandNames.length) {
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

    private static String formatBandName(String name) {
        if(name.endsWith(".bin"))
            name = name.substring(0, name.lastIndexOf(".bin"));
        return name;
    }
}

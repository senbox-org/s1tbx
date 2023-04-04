package org.jlinda.nest.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.lang.WordUtils;
import org.esa.s1tbx.commons.io.FileImageOutputStreamExtImpl;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.dataio.dimap.EnviHeader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.unwrapping.snaphu.SnaphuConfigFile;
import org.jlinda.core.unwrapping.snaphu.SnaphuParameters;

import javax.imageio.stream.ImageOutputStream;
import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The SNAPHU product writer based on ENVI products writer.
 */
public class SnaphuWriter extends AbstractProductWriter {

    // TODO: inherit EnviProductWriter of BEAM, and implement/override only new code for SNAPHU export

    private File _outputDir;
    private File _outputFile;
    private Map _bandOutputStreams;
    private boolean _incremental = true;

    public static final String SNAPHU_HEADER_EXTENSION = ".snaphu"+EnviHeader.FILE_EXTENSION;
    public static final String SNAPHU_IMAGE_EXTENSION = ".snaphu"+DimapProductConstants.IMAGE_FILE_EXTENSION;
    private static final String SNAPHU_CONFIG_FILE = "snaphu.conf";
    private static final String UNWRAPPED_PREFIX = "Unw";

    private SnaphuConfigFile snaphuConfigFile = new SnaphuConfigFile();
    private final ByteOrder byteOrder = ByteOrder.nativeOrder();

    /**
     * Construct a new instance of a product writer for the given ENVI product writer plug-in.
     *
     * @param writerPlugIn the given ENVI product writer plug-in, must not be <code>null</code>
     */
    public SnaphuWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        final Object output = getOutput();

        File outputFile = null;
        if (output instanceof String) {
            outputFile = new File((String) output);
        } else if (output instanceof File) {
            outputFile = (File) output;
        }
        Debug.assertNotNull(outputFile); // super.writeProductNodes should have checked this already
        initDirs(outputFile);

        ensureNamingConvention();
        Product sourceProduct = getSourceProduct();

        writeUnwrappedBandHeader(sourceProduct);

        // set up product writer
        sourceProduct.setProductWriter(this);
        deleteRemovedNodes();

        // dump snaphu config file
        createSnaphuConfFile(getSourceProduct());

    }

    private void writeUnwrappedBandHeader(final Product sourceProduct) throws IOException {
        Band phaseBand = null;
        for (Band band : sourceProduct.getBands()) {
            if (band.getUnit()!= null && band.getUnit().contains(Unit.PHASE)) {
                phaseBand = band;
                String bandName = UNWRAPPED_PREFIX + phaseBand.getName() + SNAPHU_HEADER_EXTENSION;
                File unwrappedHeaderFile = new File(_outputDir, bandName);

                Band newBand = new Band(UNWRAPPED_PREFIX+phaseBand.getName(), phaseBand.getDataType(),
                        phaseBand.getRasterWidth(), phaseBand.getRasterHeight());
                newBand.setDescription("Unwrapped "+phaseBand.getDescription());

                EnviHeader.createPhysicalFile(unwrappedHeaderFile,
                        newBand,
                        newBand.getRasterWidth(),
                        newBand.getRasterHeight(),
                        0);
            }
        }
        if(phaseBand == null) {
            throw new IOException("SNAPHU writer requires a wrapped phase band");
        }


    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data shold be continously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     */
    void initDirs(final File outputFile) {
        final String name = FileUtils.getFilenameWithoutExtension(outputFile);
        _outputDir = outputFile.getParentFile();
        if (_outputDir == null) {
            _outputDir = new File(".");
        }
        _outputDir = new File(_outputDir, name);
        if(!_outputDir.exists() && !_outputDir.mkdirs()) {
            SystemUtils.LOG.severe("Unable to create folders in "+_outputDir);
        }
        _outputFile = new File(_outputDir, outputFile.getName());
    }

    private void ensureNamingConvention() {
        if (_outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(_outputFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);
        final int sourceBandWidth = sourceBand.getRasterWidth();
        final int sourceBandHeight = sourceBand.getRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX,
                sourceOffsetY);
        final ImageOutputStream outputStream = getOrCreateImageOutputStream(sourceBand);
        long outputPos = sourceOffsetY * sourceBandWidth + sourceOffsetX;
        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", 1);//sourceHeight);
        try {
            final long max = sourceHeight * sourceWidth;
            for (int sourcePos = 0; sourcePos < max; sourcePos += sourceWidth) {
                sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                outputPos += sourceBandWidth;
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (_outputFile != null && _outputFile.exists() && _outputFile.isFile()) {
            _outputFile.delete();
        }
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final int sourceBandWidth, int sourceHeight,
                                                          final int sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int expectedBufferSize = sourceWidth * sourceHeight;
        final int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (Object o : _bandOutputStreams.values()) {
            ((ImageOutputStream) o).flush();
        }

        // at the very end also save SnaphuConfig file
        try {
            createSnaphuConfFile(getSourceProduct());
        } catch (Exception ignored) {
        }

    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (Object o : _bandOutputStreams.values()) {
            ((ImageOutputStream) o).close();
        }
        _bandOutputStreams.clear();
        _bandOutputStreams = null;
    }

    /**
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */
    private ImageOutputStream getOrCreateImageOutputStream(Band band) throws IOException {
        ImageOutputStream outputStream = getImageOutputStream(band);
        if (outputStream == null) {
            outputStream = createImageOutputStream(band);
            if (_bandOutputStreams == null) {
                _bandOutputStreams = new HashMap();
            }

            outputStream.setByteOrder(byteOrder);
            _bandOutputStreams.put(band, outputStream);
        }
        return outputStream;
    }

    private ImageOutputStream getImageOutputStream(Band band) {
        if (_bandOutputStreams != null) {
            return (ImageOutputStream) _bandOutputStreams.get(band);
        }
        return null;
    }

    /**
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */
    private File getValidImageFile(Band band) throws IOException {
        writeEnviHeader(band); // always (re-)write ENVI header
        final File file = getImageFile(band);
        if (file.exists()) {
            if (file.length() != getImageFileSize(band)) {
                createPhysicalImageFile(band, file);
            }
        } else {
            createPhysicalImageFile(band, file);
        }
        return file;
    }

    private static void createPhysicalImageFile(Band band, File file) throws IOException {
        createPhysicalFile(file, getImageFileSize(band));
    }

    private void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                band,
                band.getRasterWidth(),
                band.getRasterHeight());
    }

    private ImageOutputStream createImageOutputStream(Band band) throws IOException {
        return new FileImageOutputStreamExtImpl(getValidImageFile(band));
    }

    private static long getImageFileSize(RasterDataNode band) {
        return (long) ProductData.getElemSize(band.getDataType()) *
                (long) band.getRasterWidth() *
                (long) band.getRasterHeight();
    }

    private File getEnviHeaderFile(Band band) {
        return new File(_outputDir, createEnviHeaderFilename(band));
    }

    private static String createEnviHeaderFilename(Band band) {
        return band.getName() + SNAPHU_HEADER_EXTENSION;
    }

    private File getImageFile(Band band) {
        return new File(_outputDir, createImageFilename(band));
    }

    protected String createImageFilename(Band band) {
        return band.getName() + SNAPHU_IMAGE_EXTENSION;
    }

    private static void createPhysicalFile(File file, long fileSize) throws IOException {
        final File parentDir = file.getParentFile();
        if (parentDir != null) {
            if(!parentDir.exists() && !parentDir.mkdirs()) {
                SystemUtils.LOG.severe("Unable to create folders in "+parentDir);
            }
        }
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(fileSize);
        randomAccessFile.close();
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        }
        if (node instanceof FilterBand) {
            return false;
        }
        if (node.isModified()) {
            return true;
        }
        if (!isIncrementalMode()) {
            return true;
        }
        if (!(node instanceof Band)) {
            return true;
        }
        final File imageFile = getImageFile((Band) node);
        return !(imageFile != null && imageFile.exists());
    }

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    @Override
    public void setIncrementalMode(boolean enabled) {
        _incremental = enabled;
    }

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean isIncrementalMode() {
        return _incremental;
    }

    private void deleteRemovedNodes() throws IOException {
        final Product product = getSourceProduct();
        final ProductReader productReader = product.getProductReader();
        if (productReader instanceof DimapProductReader) {
            final ProductNode[] removedNodes = product.getRemovedChildNodes();
            if (removedNodes.length > 0) {
                productReader.close();
                for (ProductNode removedNode : removedNodes) {
                    removedNode.removeFromFile(this);
                }
            }
        }
    }

    @Override
    public void removeBand(Band band) {
        if (band != null) {
            final String headerFilename = createEnviHeaderFilename(band);
            final String imageFilename = createImageFilename(band);
            File[] files = null;
            if (_outputDir != null && _outputDir.exists()) {
                files = _outputDir.listFiles();
            }
            if (files == null) {
                return;
            }
            String name;
            for (File file : files) {
                name = file.getName();
                if (file.isFile() && (name.equals(headerFilename) || name.equals(imageFilename))) {
                    file.delete();
                }
            }
        }
    }

    protected File getOutputDir() {
        return _outputDir;
    }

    private Band [] getBands(Product product, String searchTerm){
        int numBands = 0;
        ArrayList<Band> bands = new ArrayList<>();
        for (Band b: product.getBands()){
            if (b.getName().toLowerCase().contains(searchTerm)){
                numBands+=1;
                bands.add(b);
            }
        }
        if(numBands > 0) {
            Band [] bandArray = new Band[bands.size()];
            return bands.toArray(bandArray);
        }else{
            return new Band[]{};
        }
    }
    private Band [] getCoherenceBands(Product product){
        return getBands(product, "coh");
    }
    private Band [] getPhaseBands(Product product){
        return getBands(product, "phase");
    }
    private Band getCorrespondingCoherenceBand(Band phaseBand, Band [] coherenceBands){
        String phaseBandName = phaseBand.getName();
        String phaseBandDateRange = phaseBandName.replace("Phase_ifg_", "");
        for(Band coherenceBand: coherenceBands){
            String coherenceBandName = coherenceBand.getName();
            String coherenceBandDateRange = coherenceBandName.replace("coh_", "");
            if(phaseBandDateRange.equals(coherenceBandDateRange)){
                return coherenceBand;
            }
        }
        return null;
    }

    private String toSentenceCase(String word){
        char [] chars = word.toUpperCase().toCharArray();
        for (int x = 1; x < chars.length; x++){
            chars[x] = Character.toLowerCase(chars[x]);
        }
        return String.valueOf(chars);
    }

    private String convertDate(String date){
        date = date.split(" ")[0];
        String month = date.split("-")[1];
        String day = date.split("-")[0];
        String year = date.split("-")[2];
        month = toSentenceCase(month);
        return day + month + year;
    }

    public void createSnaphuConfFile(Product sourceProduct) throws IOException {

        //final Product sourceProduct = getSourceProduct();

        // prepare snaphu config file
        final MetadataElement primaryRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final MetadataElement[] secondaryS = sourceProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements();
        Band [] phaseBands = getPhaseBands(sourceProduct);
        Band [] coherenceBands = getCoherenceBands(sourceProduct);
        boolean hasCoherence = coherenceBands.length > 0;
        PrimarySecondaryBandMapping [] mappings = new PrimarySecondaryBandMapping[phaseBands.length];

        // Check if we are dealing with a single primary/secondary setup
        if (secondaryS.length == 1){
            MetadataElement secondaryRoot = secondaryS[0];
            final SLCImage primaryMetadata = new SLCImage(primaryRoot, sourceProduct);
            final SLCImage secondaryMetadata = new SLCImage(secondaryRoot, sourceProduct);
            Band phaseBand = phaseBands[0];
            Band coherenceBand = null;

            if (hasCoherence){
                coherenceBand = coherenceBands[0];
            }
        }else{
            //Potentially dealing with a multi primary multi secondary setup.
            MetadataElement [] allRootMetadatas = new MetadataElement[secondaryS.length + 1];

            for (int x = 0; x < allRootMetadatas.length; x++){
                if (x == secondaryS.length){
                    allRootMetadatas[x] = primaryRoot;
                }else{
                    allRootMetadatas[x] = secondaryS[x];
                }
            }
            int count = 0;
            for(Band phaseBand : phaseBands){
                Band coherenceBand = getCorrespondingCoherenceBand(phaseBand, coherenceBands);
                String primaryDate = phaseBand.getName().split("_")[2];
                String secondaryDate = phaseBand.getName().split("_")[3];
                MetadataElement primaryMetadata = null;
                MetadataElement secondaryMetadata = null;
                for (MetadataElement rootElement : allRootMetadatas){
                    String procDate = convertDate(rootElement.getAttributeString("STATE_VECTOR_TIME"));
                    if(procDate.equals(primaryDate)){
                        primaryMetadata = rootElement;
                    }else if (procDate.equals(secondaryDate)){
                        secondaryMetadata = rootElement;
                    }
                }
                PrimarySecondaryBandMapping mapping = new PrimarySecondaryBandMapping(primaryMetadata, secondaryMetadata, coherenceBand, phaseBand);
                mappings[count] = mapping;
                count++;
            }


        }
        for(MetadataElement secondaryRoot : secondaryS){
            final SLCImage primaryMetadata = new SLCImage(primaryRoot, sourceProduct);
            final SLCImage secondaryMetadata = new SLCImage(secondaryRoot, sourceProduct);
            final String referenceDate = secondaryRoot.getName().split("_")[secondaryRoot.getName().split("_").length - 1];



            Orbit primaryOrbit = null;
            Orbit secondaryOrbit = null;
            try {
                primaryOrbit = new Orbit(primaryRoot, 3);
                secondaryOrbit = new Orbit(secondaryRoot, 3);
            } catch (Exception ignored) {
            }

            String cohName = null;
            String phaseName = null;
            if(secondaryS.length > 1){
                for (Band band : sourceProduct.getBands()) {
                    if (band.getUnit().contains(Unit.COHERENCE)) {
                        String curCohName = band.getName();
                        String [] curSplit = curCohName.split("_");
                        if(curSplit.length >=3){
                            String date = curSplit[2];
                            if(secondaryRoot.getName().contains(date)){
                                cohName = curCohName;
                            }
                        }else{
                            cohName = band.getName();
                        }

                    }
                    if (band.getUnit().contains(Unit.PHASE)) {
                        String curPhaseName = band.getName();
                        String [] curSplit = curPhaseName.split("_");
                        if(curSplit.length >=4){
                            String date = curSplit[3];
                            if(secondaryRoot.getName().contains(date)){
                                phaseName = curPhaseName;
                            }
                        }else{
                            phaseName = band.getName();
                        }
                    }
                }
            }else{
                for (Band band : sourceProduct.getBands()) {
                    if (band.getUnit().contains(Unit.COHERENCE)) {
                        cohName = band.getName();
                    }
                    if (band.getUnit().contains(Unit.PHASE)) {
                        phaseName = band.getName();
                    }
                }
            }
            if(phaseName == null){
                for (Band band : sourceProduct.getBands()) {
                    if (band.getUnit().contains(Unit.PHASE)) {
                        phaseName = band.getName();
                    }
                }
            }
            if(cohName == null){
                for (Band band : sourceProduct.getBands()) {
                    if (band.getUnit().contains(Unit.COHERENCE)) {
                        cohName = band.getName();
                    }
                }
            }


            SnaphuParameters parameters = new SnaphuParameters();
            String temp;
            temp = primaryRoot.getAttributeString("snaphu_cost_mode", "DEFO");
            parameters.setUnwrapMode(temp);

            temp = primaryRoot.getAttributeString("snaphu_init_mode", "MST");
            parameters.setSnaphuInit(temp);

            parameters.setnTileRow(primaryRoot.getAttributeInt("snaphu_numberOfTileRows", 10));
            parameters.setnTileCol(primaryRoot.getAttributeInt("snaphu_numberOfTileCols", 10));
            parameters.setNumProcessors(primaryRoot.getAttributeInt("snaphu_numberOfProcessors", 4));
            parameters.setRowOverlap(primaryRoot.getAttributeInt("snaphu_rowOverlap", 200));
            parameters.setColumnOverlap(primaryRoot.getAttributeInt("snaphu_colOverlap", 200));
            parameters.setTileCostThreshold(primaryRoot.getAttributeInt("snaphu_tileCostThreshold", 500));

            parameters.setLogFileName("snaphu.log");
            parameters.setPhaseFileName(phaseName + SNAPHU_IMAGE_EXTENSION);
            parameters.setCoherenceFileName(cohName + SNAPHU_IMAGE_EXTENSION);
            parameters.setVerbosityFlag("true");

            parameters.setOutFileName(UNWRAPPED_PREFIX + phaseName + SNAPHU_IMAGE_EXTENSION);

            Window dataWindow = new Window(primaryMetadata.getCurrentWindow());
            int size = 0;
            for (Band b : sourceProduct.getBands()){
                if (b.getName().toLowerCase().contains("phase")){
                    size = b.getRasterWidth();
                }
            }

            /// initiate snaphuconfig
            try {
                snaphuConfigFile = new SnaphuConfigFile(primaryMetadata, secondaryMetadata, primaryOrbit, secondaryOrbit, dataWindow, parameters, size);
                if(secondaryS.length > 1){
                    snaphuConfigFile.buildConfFile(phaseName);
                }else{
                    snaphuConfigFile.buildConfFile("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // write snaphu.conf file to the target directory
            try {
                if(secondaryS.length > 1){
                    BufferedWriter out = new BufferedWriter(new FileWriter(_outputDir + "/" + phaseName +  SNAPHU_CONFIG_FILE));
                    out.write(snaphuConfigFile.getConfigFileBuffer().toString());
                    out.close();
                    //Write out snaphu.conf file without phase name appended to avoid breaking legacy workflows
                    snaphuConfigFile = null; // Clear reference
                    snaphuConfigFile = new SnaphuConfigFile(primaryMetadata, secondaryMetadata, primaryOrbit, secondaryOrbit, dataWindow, parameters, size);
                    snaphuConfigFile.buildConfFile("");
                    BufferedWriter out2 = new BufferedWriter(new FileWriter(_outputDir + "/" + SNAPHU_CONFIG_FILE));
                    out2.write(snaphuConfigFile.getConfigFileBuffer().toString());
                    out2.close();



                }else{
                    BufferedWriter out = new BufferedWriter(new FileWriter(_outputDir + "/" +  SNAPHU_CONFIG_FILE));
                    out.write(snaphuConfigFile.getConfigFileBuffer().toString());
                    out.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private class PrimarySecondaryBandMapping {

        public final MetadataElement primaryRoot, secondaryRoot;
        public final Band coherenceBand, phaseBand;

        public PrimarySecondaryBandMapping(MetadataElement primaryRoot, MetadataElement secondaryRoot,
                                           Band coherenceBand, Band phaseBand){
            this.primaryRoot = primaryRoot;
            this.secondaryRoot = secondaryRoot;
            this.coherenceBand = coherenceBand;
            this.phaseBand = phaseBand;
        }

    }

}

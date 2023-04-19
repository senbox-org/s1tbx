package org.esa.s1tbx.sar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.FileDownloader;
import org.esa.snap.dataio.envi.EnviProductReaderPlugIn;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.jlinda.nest.dataio.SnaphuExportOp;
import org.jlinda.nest.dataio.SnaphuImportOp;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Locale;

/**
 * Export products into format suitable for import to PyRate.
 * Located within s1tbx-op-sar-processing to access terrain correction.
 */
@OperatorMetadata(alias = "PyrateExport",
        category = "Radar/Interferometric/PSI \\ SBAS",
        authors = "Alex McVittie",
        version = "1.0",
        copyright = "Copyright (C) 2023 SkyWatch Space Applications Inc.",
        autoWriteDisabled = true,
        description = "Export wrapped SBAS interferometric data for PyRate processing")

public class PyRateExportOp extends Operator {
    boolean testingDisableUnwrapStep = true;

    @SourceProducts
    private Product [] sourceProducts;

    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(description = "Include coherence", defaultValue = "true")
    private Boolean includeCoherenceBands = true;

    // For downloading and running Snaphu.
    @Parameter(description = "SNAPHU binary folder", defaultValue = "/tmp/snaphuBinary")
    private String snaphuInstallLocation = "/tmp/snaphuBinary";


    // For the SnaphuExportOp operator.
    @Parameter(description = "Directory to write SNAPHU configuration files, unwrapped interferograms, and PyRate inputs to", defaultValue = "/tmp/snaphuProcessing")
    private String processingLocation = "/tmp/snaphuProcessing";

    @Parameter(valueSet = {"TOPO", "DEFO", "SMOOTH", "NOSTATCOSTS"},
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "TOPO",
            label = "Statistical-cost mode")
    private String statCostMode = "TOPO";

    @Parameter(valueSet = {"MST", "MCF"},
            description = "Algorithm used for initialization of the wrapped phase values",
            defaultValue = "MST",
            label = "Initial method")
    private String initMethod = "MST";

    @Parameter(description = "Divide the image into tiles and process in parallel. Set to 1 for single tiled.",
            defaultValue = "10", label = "Number of Tile Rows")
    private int numberOfTileRows = 10;

    @Parameter(description = "Divide the image into tiles and process in parallel. Set to 1 for single tiled.",
            defaultValue = "10", label = "Number of Tile Columns")
    private int numberOfTileCols = 10;

    @Parameter(description = "Number of concurrent processing threads. Set to 1 for single threaded.",
            defaultValue = "4", label = "Number of Processors")
    private int numberOfProcessors = 4;

    @Parameter(description = "Overlap, in pixels, between neighboring tiles.",
            defaultValue = "200", label = "Row Overlap")
    private int rowOverlap = 200;

    @Parameter(description = "Overlap, in pixels, between neighboring tiles.",
            defaultValue = "200", label = "Column Overlap")
    private int colOverlap = 200;

    @Parameter(description = "Cost threshold to use for determining boundaries of reliable regions\n" +
            " (long, dimensionless; scaled according to other cost constants).\n" +
            " Larger cost threshold implies smaller regions---safer, but more expensive computationally.",
            defaultValue = "500", label = "Tile Cost Threshold")
    private int tileCostThreshold = 500;

    @Override
    public void setSourceProduct(Product sourceProduct) {
        setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME, sourceProduct);
        this.sourceProduct = sourceProduct;
    }

    @Override
    public void initialize() throws OperatorException {

        runValidationChecks();

        try{
            process();
        }catch (Exception e){
            throw new OperatorException(e);
        }
    }
    // Product and input variable validations.
    private void runValidationChecks() throws OperatorException {

        // Validate the product
        if(sourceProduct == null){
            throw new OperatorException("Source product must not be null.");
        }
        InputProductValidator validator = new InputProductValidator(sourceProduct);
        validator.checkIfCoregisteredStack();
        int numPhaseBands = getNumBands(sourceProduct, Unit.PHASE);
        int numCoherenceBands = getNumBands(sourceProduct, Unit.COHERENCE);
        if(numPhaseBands < 2){
            throw new OperatorException("PyRate needs more than 1 wrapped phase band.");
        }
        if(numCoherenceBands == 0){
            throw new OperatorException("PyRate requires coherence bands for processing.");
        }
        if(numPhaseBands != numCoherenceBands){
            throw new OperatorException("Mismatch in number of phase and coherence bands. Each interferogram needs a corresponding coherence band.");
        }

        // Validate the folder locations provided
        if (!Files.exists(new File(processingLocation).toPath())){
            throw new OperatorException("Path provided for Snaphu processing location does not exist. Please provide a valid path.");
        }
        if (!Files.isDirectory(new File(processingLocation).toPath())){
            throw new OperatorException("Path provided for Snaphu processing is not a folder. Please select a folder, not a file.");
        }
        if (!Files.exists(new File(snaphuInstallLocation).toPath())){
            throw new OperatorException("Path provided for the Snaphu installation does not exist. Please provide an existing path.");
        }
        if(!Files.exists(new File(processingLocation).toPath())){
            throw new OperatorException("Path provided for the intermediary processing does not exist. Please provide an existing path.");
        }
        if(!Files.isWritable(new File(processingLocation).toPath())){
            throw new OperatorException("Path provided for intermediary processing is not writeable.");
        }
        if(!isSnaphuBinary(new File(snaphuInstallLocation)) && !Files.isWritable(new File(snaphuInstallLocation).toPath())){
            throw new OperatorException("Folder provided for SNAPHU installation is not writeable.");
        }
    }


    // Simple method to get the number of bands in a product with a specified band unit.
    private int getNumBands(Product product, String unit){
        int numBands = 0;
        for (Band b: product.getBands()){
            if(b.getUnit().contains(unit)){
                numBands++;
            }
        }
        return numBands;
    }

    // Check to see if a passed in file is the SNAPHU executable.
    private boolean isSnaphuBinary(File file){
        if(System.getProperty("os.name").toLowerCase().startsWith("windows")){
            return ! file.isDirectory() &&
                    file.canExecute() &&
                    file.getName().equals("snaphu.exe");
        }else{
            return ! file.isDirectory() &&
                    file.canExecute() &&
                    file.getName().startsWith("snaphu");
        }
    }

    // Iterate through a given directory and locate the SNAPHU binary within it.
    // Returns null if no SNAPHU binary is found.
    private File findSnaphuBinary(File rootDir){
        Collection<File> files = FileUtils.listFilesAndDirs(rootDir, TrueFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY );
        for (File file : files){
            if(isSnaphuBinary(file)){
                return file;
            }
        }
        return null;
    }

    private File downloadSnaphu(String snaphuInstallLocation) throws IOException {
        final String linuxDownloadPath = "http://step.esa.int/thirdparties/snaphu/1.4.2-2/snaphu-v1.4.2_linux.zip";
        final String windowsDownloadPath = "http://step.esa.int/thirdparties/snaphu/2.0.4/snaphu-v2.0.4_win64.zip";
        final String windows32DownloadPath = "http://step.esa.int/thirdparties/snaphu/1.4.2-2/snaphu-v1.4.2_win32.zip";

        File snaphuInstallDir = new File(snaphuInstallLocation);
        boolean isDownloaded;
        File snaphuBinaryLocation;

        // Check if we have just been given the path to the SNAPHU binary
        if (isSnaphuBinary(snaphuInstallDir)){
            isDownloaded = true;
            snaphuBinaryLocation = snaphuInstallDir;
        }else{ // We haven't been just given the binary location.

            // Get parent dir if passed in a file somehow
            if(! snaphuInstallDir.isDirectory()){
                snaphuInstallDir = snaphuInstallDir.getParentFile();
            }
            snaphuBinaryLocation = findSnaphuBinary(snaphuInstallDir);
            isDownloaded = snaphuBinaryLocation != null;
        }
        if (! isDownloaded){
            // We have checked the passed in folder and it does not contain the SNAPHU binary.
            String operatingSystem = System.getProperty("os.name");
            String downloadPath;

            if(operatingSystem.toLowerCase().contains("windows")){
                // Using Windows
                boolean bitDepth64 = System.getProperty("os.arch").equals("amd64");
                if(bitDepth64){
                    downloadPath = windowsDownloadPath;
                }else{
                    downloadPath = windows32DownloadPath;
                }
            }
            else{
                // Using MacOS or Linux
                downloadPath = linuxDownloadPath;
            }
            File zipFile = FileDownloader.downloadFile(new URL(downloadPath), snaphuInstallDir, null);
            ZipUtils.unzip(zipFile.toPath(), snaphuInstallDir.toPath(), true);
            snaphuBinaryLocation = findSnaphuBinary(snaphuInstallDir);
        }

        return snaphuBinaryLocation;
    }

    private void callSnaphuUnwrap(File snaphuBinary, File configFile) throws IOException {
        File workingDir = configFile.getParentFile();
        String command = null;
        try(BufferedReader in = new BufferedReader(new FileReader(configFile), 1024)){
            // SNAPHU command is on the 7th line
            for(int x = 0; x < 6; x++){
                in.readLine();
            }
            command = in.readLine().substring(14);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (command != null){
            Process proc = Runtime.getRuntime().exec(snaphuBinary.toString() + command, null, workingDir);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // Read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            // Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }
    }

    private Product assembleUnwrappedFilesIntoSingularProduct(File directory) throws IOException {
        File [] fileNames = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("UnwPhase") && name.endsWith(".hdr");
            }
        });

        Product [] enviProducts = new Product[fileNames.length];
        EnviProductReaderPlugIn readerPlugIn = new EnviProductReaderPlugIn();
        ProductReader enviProductReader = readerPlugIn.createReaderInstance();
        enviProducts[0] = enviProductReader.readProductNodes(fileNames[0], null);
        for (int x = 1; x < enviProducts.length; x++){
            enviProducts[x] = enviProductReader.readProductNodes(fileNames[x], null);
            ProductUtils.copyBand(enviProducts[x].getBands()[0].getName(), enviProducts[x], enviProducts[0], true);
        }
        return enviProducts[0];
    }

    // Configure the SNAPHU Export operator in its own method to keep the processing method clean.
    private SnaphuExportOp setupSnaphuExportOperator(){
        SnaphuExportOp snaphuExportOp = new SnaphuExportOp();
        snaphuExportOp.setParameter("statCostMode", statCostMode);
        snaphuExportOp.setParameter("initMethod", initMethod);
        snaphuExportOp.setParameter("numberOfTileRows", numberOfTileRows);
        snaphuExportOp.setParameter("numberOfTileCols", numberOfTileCols);
        snaphuExportOp.setParameter("numberOfProcessors", numberOfProcessors);
        snaphuExportOp.setParameter("rowOverlap", rowOverlap);
        snaphuExportOp.setParameter("colOverlap", colOverlap);
        snaphuExportOp.setParameter("tileCostThreshold", tileCostThreshold);
        snaphuExportOp.setParameter("targetFolder", processingLocation);
        snaphuExportOp.setSourceProduct(sourceProduct);
        return snaphuExportOp;

    }

    // All SNAPHU export & snaphu unwrapping method calls occurs in this method.
    private Product processSnaphu( File snaphuProcessingLocation) throws IOException {
        // Perform SNAPHU-Export
        Product product = setupSnaphuExportOperator().getTargetProduct();

        // Bands need to be read in fully  before writing out to avoid data access errors.
        for(Band b: product.getBands()){
            b.readRasterDataFully(ProgressMonitor.NULL);
        }

        // Write out product to the snaphu processing location folder for unwrapping.
        ProductIO.writeProduct(product, snaphuProcessingLocation.getAbsolutePath(), "snaphu");

        // Download, or locate the downloaded SNAPHU binary within the specified SNAPHU installation location.
        File snaphuBinary = downloadSnaphu(snaphuInstallLocation);

        // Find all SNAPHU configuration files and execute them.
        String [] files = snaphuProcessingLocation.list();
        for(String file: files){
            File aFile = new File(snaphuProcessingLocation, file);
            if(file.endsWith("snaphu.conf") && ! file.equals("snaphu.conf") && !testingDisableUnwrapStep){
                callSnaphuUnwrap(snaphuBinary, aFile);
            }
        }

        return assembleUnwrappedFilesIntoSingularProduct(snaphuProcessingLocation);
    }
    private String bandNameDateToPyRateDate(String bandNameDate){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM").withLocale(Locale.ENGLISH);
        TemporalAccessor accessor = formatter.parse(bandNameDate.substring(2, 5));
        int monthNumber = accessor.get(ChronoField.MONTH_OF_YEAR);
        String month = monthNumber + "";
        if(monthNumber < 10){
            month = "0" + month;
        }
        // Formatted as YYYYMMDD
        return bandNameDate.substring(5) + month + bandNameDate.substring(0, 2);
    }

    private String writeBands(Product product, String format, String unit) throws IOException {
        String fileNames = "";
        int x = 0;
        for(Band b: product.getBands()){
            if(b.getUnit().contains(unit)){
                Product productSingleBand = new Product(product.getName(), product.getProductType(), product.getSceneRasterWidth(), product.getSceneRasterHeight());
                productSingleBand.setSceneGeoCoding(product.getSceneGeoCoding());
                b.readRasterDataFully();
                ProductUtils.copyBand(b.getName(), product, productSingleBand, true);
                String [] name = b.getName().split("_");
                int y = 0;
                String firstDate = "";
                String secondDate = "";
                for (String aname : name){
                    if (aname.length() == 9){
                        firstDate = aname;
                        secondDate = name[y + 1];
                        break;
                    }
                    y+= 1;
                }
                String pyRateDate = bandNameDateToPyRateDate(firstDate) + "-" + bandNameDateToPyRateDate(secondDate);
                String pyRateName = pyRateDate + "_" + unit;
                String fileName = new File(processingLocation, pyRateName).getAbsolutePath();
                productSingleBand.setName(pyRateName);
                productSingleBand.getBands()[0].setName(pyRateName);

                ProductIO.writeProduct(productSingleBand, fileName, format);

                if(format.equals("GeoTIFF")){
                    fileName += ".tif";
                }else{
                    adjustGammaHeader(productSingleBand, new File(processingLocation, productSingleBand.getName() + ".par"));
                    new File(processingLocation, productSingleBand.getName() + ".rslc").delete();
                }
                fileNames += "\n" + new File(fileName).getName();
                x++;
            }
        }
        // Cut off trailing newline character.
        return fileNames.substring(1);
    }
    private void writeElevationBand(Product product, String name, String format) throws IOException {
        Product productSingleBand = new Product(product.getName(), product.getProductType(), product.getSceneRasterWidth(), product.getSceneRasterHeight());
        productSingleBand.setSceneGeoCoding(product.getSceneGeoCoding());
        product.getBand("elevation").readRasterDataFully();
        ProductUtils.copyBand("elevation", product, productSingleBand, true);
        String fileName = new File(processingLocation, name).getAbsolutePath();
        ProductIO.writeProduct(productSingleBand, fileName, format);
        if(format.equals("Gamma")){
            adjustGammaHeader(productSingleBand, new File(processingLocation, "DEM.par"));
            new File(processingLocation, "elevation.rslc").delete();
        }

    }
    // PyRate expects a couple extra pieces of metadata in the GAMMA headers. This method adjusts and adds these
    // missing fields.
    private void adjustGammaHeader(Product product, File gammaHeader) throws IOException {
        String contents = FileUtils.readFileToString(gammaHeader, "utf-8");


        GeoPos geoPosUpperLeft = product.getSceneGeoCoding().getGeoPos( new PixelPos(0, 0), null);
        GeoPos geoPosLowerRight = product.getSceneGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null);

        contents += "corner_lat:\t" + geoPosUpperLeft.lat + " decimal degrees";
        contents += "\ncorner_lon:\t" + geoPosUpperLeft.lon + " decimal degrees";
        contents += "\npost_lat:\t" + geoPosLowerRight.lat + " decimal degrees";
        contents += "\npost_lon:\t" + geoPosLowerRight.lon + " decimal degrees";
        contents += "\nellipsoid_name:\t WGS84";
        FileUtils.write(gammaHeader, contents);



    }




    /*
        All main preprocessing and generation of PyRATE inputs happens within this process() method.

        After product validation (Is a coregistered stack, contains ifgs, has more than 2 ifgs, the output paths are valid, etc),
        this method is executed.

        The PyRATE preparation workflow is as follows:

        1) Generate SNAPHU input to unwrap each interferogram in the stack.
        2) Download SNAPHU if it is not present in the installation location.
        3) Loop through the SNAPHU input directory and unwrap each interferogram.
        4) Assemble the unwrapped interferograms into one product, and then use SNAPHU Import to bring them back into the original product.
        5) Add an elevation band if not supplied in the original product.
        6) Write unwrapped interferograms, along with the coherence bands, to the input PyRATE directory.
        7) Generate the needed configuration files for PyRATE.
        8) Create a shell script that allows the user to easily execute PyRATE given the input folder of data.

     */

    private void process() throws Exception {
        // Processing location provided by user is the root directory. We want to save all data in a folder that is named
        // the source product to avoid data overwriting with different products.
        processingLocation = new File(processingLocation, sourceProduct.getName()).getAbsolutePath();
        new File(processingLocation).mkdirs();

        // Create sub folder for SNAPHU processing and intermediary files.
        new File(processingLocation, "snaphu").mkdirs();

        File snaphuProcessingLocation = new File(processingLocation, "snaphu");
        snaphuProcessingLocation.mkdirs();

        // Unwrap interferograms and merge into one multi-band product.
        Product unwrappedInterferograms = processSnaphu(snaphuProcessingLocation);

        // Snaphu import takes an array of the original product with wrapped interferograms, and a product with the unwrapped
        // interferograms. Put them into an array for input.
        Product [] productPair = new Product[]{sourceProduct, unwrappedInterferograms};

        SnaphuImportOp snaphuImportOp = new SnaphuImportOp();
        snaphuImportOp.setSourceProducts(productPair);
        snaphuImportOp.setParameter("doNotKeepWrapped", true);

        Product imported = snaphuImportOp.getTargetProduct();

        // Importing from snaphuImportOp does not preserve the coherence bands. Copy them over from source product.
        for (Band b : sourceProduct.getBands()){
            if (b.getUnit().contains(Unit.COHERENCE)){
                ProductUtils.copyBand(b.getName(), sourceProduct, imported, true);
            }
        }

        // Preserve geocoding
        imported.setSceneGeoCoding(sourceProduct.getSceneGeoCoding());


        // PyRATE input data needs to be projected into a geographic coordinate system. Needs terrain correction.
        RangeDopplerGeocodingOp rangeDopplerGeocodingOp = new RangeDopplerGeocodingOp();
        rangeDopplerGeocodingOp.setSourceProduct(imported);
        Product terrainCorrected = rangeDopplerGeocodingOp.getTargetProduct();


        // Set up PyRATE output directory in our processing directory.
        new File(processingLocation, "pyrateOutputs").mkdirs();

        // Generate PyRATE configuration files
        PyRateConfigurationFileBuilder configBuilder = new PyRateConfigurationFileBuilder();

        configBuilder.coherenceFileList = new File(processingLocation, "coherenceFiles.txt").getName();
        configBuilder.interferogramFileList = new File(processingLocation, "ifgFiles.txt").getName();
        configBuilder.outputDirectory = new File(processingLocation, "pyrateOutputs").getName();

        String mainFileContents = configBuilder.createMainConfigFileContents();

        FileUtils.write(new File(processingLocation, "input_parameters.conf"), mainFileContents);



        // Write coherence and phase bands out to individual GeoTIFFS
        String interferogramFiles = writeBands(terrainCorrected, "GeoTIFF", Unit.PHASE);
        String coherenceFiles = writeBands(terrainCorrected, "GeoTIFF", Unit.COHERENCE);

        writeBands(terrainCorrected, "Gamma", Unit.PHASE);
        writeBands(terrainCorrected, "Gamma", Unit.COHERENCE);

        AddElevationOp addElevationOp = new AddElevationOp();
        addElevationOp.setSourceProduct(terrainCorrected);
        addElevationOp.setParameter("demName", "SRTM 3Sec");
        Product tcWithElevation = addElevationOp.getTargetProduct();

        writeElevationBand(tcWithElevation, configBuilder.demFile, "Gamma");
        writeElevationBand(tcWithElevation, configBuilder.demFile, "GeoTIFF");

        // Populate files containing the coherence and interferograms.
        FileUtils.write(new File(processingLocation, configBuilder.coherenceFileList), coherenceFiles);
        FileUtils.write(new File(processingLocation, configBuilder.interferogramFileList), interferogramFiles);
        FileUtils.write(new File(processingLocation, configBuilder.headerFileList),
                coherenceFiles.replace(".tif", ".par") + "\n" +
                        interferogramFiles.replace(".tif", ".par"));

        // Set the target output product to be the terrain corrected product
        // with elevation, coherence, and unwrapped phase bands.
        setTargetProduct(terrainCorrected);

    }
}

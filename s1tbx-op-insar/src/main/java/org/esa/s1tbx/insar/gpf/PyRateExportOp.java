package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
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
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.jlinda.nest.dataio.SnaphuExportOp;
import org.jlinda.nest.dataio.SnaphuImportOp;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;

/**
 * Export products into format suitable for import to PyRate
 */
@OperatorMetadata(alias = "PyrateExport",
        category = "Radar/Interferometric/PSI \\ SBAS",
        authors = "Alex McVittie",
        version = "1.0",
        copyright = "Copyright (C) 2023 SkyWatch Space Applications Inc.",
        autoWriteDisabled = true,
        description = "Export data for PyRate processing")

public class PyRateExportOp extends Operator {

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
    @Parameter(description = "Directory to write SNAPHU configuration files and unwrapped interferograms to", defaultValue = "/tmp/snaphuProcessing")
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
    public void initialize() throws OperatorException{
        runValidationChecks();

        try{
            process();
        }catch (Exception e){
            throw new OperatorException(e);
        }
    }
    // Product and input variable validations.
    private void runValidationChecks() throws OperatorException {
        if(sourceProduct == null){
            throw new OperatorException("Source product must not be null.");
        }
        InputProductValidator validator = new InputProductValidator(sourceProduct);
        validator.checkIfCoregisteredStack();

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

    // Check to see if a passed in file is the SNAPHU executible.
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
    private void process() throws Exception {

        // Perform SNAPHU-Export

        Product product = setupSnaphuExportOperator().getTargetProduct();

        // Bands need to be read in fully  before writing out to avoid data access errors.
        for(Band b: product.getBands()){
            b.readRasterDataFully(ProgressMonitor.NULL);
        }

        new File(processingLocation, "snaphu").mkdirs();

        // Write out product to the snaphu processing location folder for unwrapping.
        ProductIO.writeProduct(product, new File(processingLocation, "snaphu").getAbsolutePath(), "snaphu");

        // Download, or locate the downloaded SNAPHU binary within the specified SNAPHU installation location.
        File snaphuBinary = downloadSnaphu(snaphuInstallLocation);

        // Find all SNAPHU configuration files and execute them.
        String [] files = new File(processingLocation).list();
        for(String file: files){
            File aFile = new File(processingLocation, file);
            if(file.endsWith("snaphu.conf") && ! file.equals("snaphu.conf")){
                callSnaphuUnwrap(snaphuBinary, aFile);
            }
        }


        // Step 5: Assemble all unwrapped interferograms into singular product
        Product unwrappedInterferograms = assembleUnwrappedFilesIntoSingularProduct(new File(processingLocation));

        // Step 6: Run SNAPHU Import, discarding unwrapped bands
        Product [] productPair = new Product[]{sourceProduct, unwrappedInterferograms};

        SnaphuImportOp snaphuImportOp = new SnaphuImportOp();
        snaphuImportOp.setSourceProducts(productPair);
        snaphuImportOp.setParameter("doNotKeepWrapped", true);

        Product imported = snaphuImportOp.getTargetProduct();
        for (Band b : sourceProduct.getBands()){
            if (b.getUnit().contains(Unit.COHERENCE)){
                ProductUtils.copyBand(b.getName(), sourceProduct, imported, true);
            }
        }






        // Step 7: Generate PyRate configuration files

        // Step 8: write unwrapped phase imagery & coherence bands out to GeoTIFF for processing


    }
}

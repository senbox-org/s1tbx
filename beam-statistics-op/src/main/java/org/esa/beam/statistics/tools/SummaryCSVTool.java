package org.esa.beam.statistics.tools;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SummaryCSVTool {

    private final static String EXE_NAME = "SummaryCSVTool";
    private final static String EXE_VERSION = "1.0";
    private final static String FILENAME_PATTERN_SHAPEFILE = "yyyyMMdd_*.shp";

    private final static int Error_NumberOfParameters = 1;
    private final static int Error_NotADirectory = 2;

    private final Logger logger;
    private final FilenameDateExtractor filenameDateExtractor;
    private final StatisticsDatabase statisticsDatabase;
    private final String TAB = "\t";

    private ShapeFileReader shapeFileReader;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            printUsage("At least one parameter expected.", Error_NumberOfParameters);
        }
        if (args.length > 2) {
            printUsage("Maximum two parameter expected.", Error_NumberOfParameters);
        }
        final File inputDir = new File(args[0]);
        if (!inputDir.isDirectory()) {
            printUsage("<inputDir> does not exist.", Error_NotADirectory);
        }
        final File outputDir;
        if (args.length > 1) {
            outputDir = new File(args[1]);
        } else {
            outputDir = inputDir;
        }
        if (!outputDir.isDirectory()) {
            printUsage("<outputDir> does not exist.", Error_NotADirectory);
        }

        final Logger logger = BeamLogManager.getSystemLogger();

        final ShapeFileReader shapeFileReader = new ShapeFileReader() {
            @Override
            public FeatureCollection<SimpleFeatureType, SimpleFeature> read(File shapeFile) throws IOException {
                return FeatureUtils.loadFeatureCollectionFromShapefile(shapeFile);
            }
        };
        SummaryCSVTool summaryCSVTool = new SummaryCSVTool(logger, shapeFileReader);
        summaryCSVTool.summarize(inputDir);
        summaryCSVTool.putOutSummerizedData(outputDir);
    }

    public SummaryCSVTool(Logger logger,
                          ShapeFileReader shapeFileReader) {
        this.logger = logger;
        this.shapeFileReader = shapeFileReader;
        this.filenameDateExtractor = new FilenameDateExtractor();
        statisticsDatabase = new StatisticsDatabase();
    }

    private void putOutSummerizedData(File outputDir) throws IOException {
        final int[] years = statisticsDatabase.getYears();
        for (int year : years) {
            final String[] parameterNames = statisticsDatabase.getParameterNames(year);
            for (String parameterName : parameterNames) {
                final File outputFile = new File(outputDir, "WFD_stat_" + year + "_" + parameterName + ".txt");
                FileOutputStream fileOutputStream = null;
                PrintWriter writer = null;
                try {
                    fileOutputStream = new FileOutputStream(outputFile);
                    writer = new PrintWriter(fileOutputStream);
                    final DatabaseRecord[] databaseRecords = statisticsDatabase.getData(year, parameterName);
                    printHeader(databaseRecords, writer);
                    printData(databaseRecords, writer);
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                }
            }
        }
    }

    private void printHeader(DatabaseRecord[] records, PrintWriter pw) {
        pw.print("Bod.ID");
        pw.print(TAB);
        pw.print("Bod.Name");

        final TreeMap<Date, TreeSet<String>> dateColNamesMap = new TreeMap<Date, TreeSet<String>>();
        for (DatabaseRecord record : records) {
            final Set<Date> dataDates = record.getDataDates();
            for (Date date : dataDates) {
                final TreeSet<String> colNames;
                if (dateColNamesMap.containsKey(date)) {
                    colNames = dateColNamesMap.get(date);
                } else {
                    colNames = new TreeSet<String>();
                    dateColNamesMap.put(date, colNames);
                }
                final Set<String> statDataColumns = record.getStatDataColumns(date);
                colNames.addAll(statDataColumns);
            }
        }
        final Calendar calendar = Calendar.getInstance();
        for (Map.Entry<Date, TreeSet<String>> dateColNamesEntry : dateColNamesMap.entrySet()) {
            final Date date = dateColNamesEntry.getKey();
            calendar.setTime(date);
            final String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
            final String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
            final String monthPart = month.length() == 1 ? "0" + month : month;
            final String dayPart = day.length() == 1 ? "0" + day : day;
            final String datePart = "_" + monthPart + dayPart;
            final TreeSet<String> colNames = dateColNamesEntry.getValue();
            for (String colName : colNames) {
                pw.print(TAB);
                pw.print(colName + "_" + datePart);
            }
        }

        pw.println();
    }

    private void printData(DatabaseRecord[] databaseRecords, PrintWriter writer) {
        for (DatabaseRecord record : databaseRecords) {
            print(record, writer);
        }
    }

    private void print(DatabaseRecord record, PrintWriter pw) {
        pw.print(record.geomId);
        pw.print(TAB);
        pw.print(record.geomName);
        final Set<Date> dataDates = record.getDataDates();
        for (Date dataDate : dataDates) {
            final Set<String> statDataColumns = record.getStatDataColumns(dataDate);
            for (String statDataColumn : statDataColumns) {
                pw.print(TAB);
                pw.print(record.getValue(dataDate, statDataColumn));
            }
        }
        pw.println();
    }

    public void summarize(File inputDir) {
        final File[] shapeFiles = inputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".shp");
            }
        });
        for (File shapeFile : shapeFiles) {
            if (!isValidDateExtractableFileName(shapeFile)) {
                continue;
            }
            final File mappingFile = new File(inputDir, FileUtils.getFilenameWithoutExtension(shapeFile).concat("_band_mapping.txt"));
            if (!mappingFile.isFile()) {
                continue;
            }

            try {
                final ProductData.UTC date = filenameDateExtractor.getDate(shapeFile);
                final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = shapeFileReader.read(shapeFile);
                final Properties properties = new Properties();
                properties.load(new FileReader(mappingFile));
                statisticsDatabase.append(date, featureCollection, properties);
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isValidDateExtractableFileName(File shapeFile) {
        final boolean validFilename = filenameDateExtractor.isValidFilename(shapeFile);
        if (!validFilename) {
            logger.log(Level.WARNING, "The filename '" + shapeFile.getName() + "' does not match the pattern " + FILENAME_PATTERN_SHAPEFILE + ".");
            logger.log(Level.INFO, "Continuing with next ESRI shapefile.");
        }
        return validFilename;
    }

    private static void printUsage(String message, int errorNumber) {
        System.out.println();
        if (message != null) {
            System.out.println(message);
            System.out.println();
        }
        System.out.println(EXE_NAME + " version " + EXE_VERSION);
        System.out.println("    The tool reads statistical data from ESRI shapefiles and summarizes it in ");
        System.out.println("    *.csv-files. One csv file will be generated per year and parameter.");
        System.out.println("    An example for a file name is WFD_stat_2009_CHL.csv. ");
        System.out.println();
        System.out.println("Usage: " + EXE_NAME + " <inputDir> [<outputDir>]");
        System.out.println("  <inputDir> points to a directory which contains one or more ESRI shapefiles.");
        System.out.println("    The shapefiles must adhere to the name pattern " + FILENAME_PATTERN_SHAPEFILE + ".");
        System.out.println("    For each shapefile a corresponding yyyyMMdd_*_mapping.txt file must be");
        System.out.println("    located in the same directory.");
        System.out.println("  <outputDir> points to an already existing output directory. This ");
        System.out.println("    parameter is optional. If not given, the output will be generated in the");
        System.out.println("    input directory.");
        System.exit(errorNumber);
    }

    public static interface ShapeFileReader {

        FeatureCollection<SimpleFeatureType, SimpleFeature> read(File shapeFile) throws IOException;
    }
}

package org.esa.snap.statistics.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

    private final Logger logger;
    private final FilenameDateExtractor filenameDateExtractor;
    private final StatisticsDatabase statisticsDatabase;
    private final String TAB = "\t";

    private ShapeFileReader shapeFileReader;

    public static void main(String[] args) throws IOException {
        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printHelp(options);
            System.exit(-1);
        }

        final File inputDir = new File(commandLine.getOptionValue("input"));

        ensureDirectory(inputDir);
        final File outputDir;
        if (commandLine.hasOption("output")) {
            outputDir = new File(commandLine.getOptionValue("output"));
            ensureDirectory(outputDir);
        } else {
            outputDir = inputDir;
        }

        initSystem();

        final Logger logger = SystemUtils.LOG;

        final ShapeFileReader shapeFileReader = FeatureUtils::loadFeatureCollectionFromShapefile;

        String waterbodyColumnName = "NAME";
        if (commandLine.hasOption("waterbodyNameColumn")) {
            waterbodyColumnName = commandLine.getOptionValue("waterbodyNameColumn");
        }

        SummaryCSVTool summaryCSVTool = new SummaryCSVTool(logger, shapeFileReader, waterbodyColumnName);
        summaryCSVTool.summarize(inputDir);
        summaryCSVTool.putOutSummerizedData(outputDir);
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException("'" + directory.getAbsolutePath() + "' is not a directory");
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(createOption("i", "input", "FILE", "The directory where the shapefiles reside.", true));
        options.addOption(createOption("o", "output", "FILE", "The output directory. If not provided, output will be written to input directory.", false));
        options.addOption(createOption("n", "waterbodyNameColumn", "STRING", "The name of the column that contains the waterbody name.", false));
        return options;
    }

    private static Option createOption(String shortOpt, String longOpt, String argName, String description, boolean required) {
        Option from = new Option(shortOpt, longOpt, argName != null, description);
        from.setRequired(required);
        from.setArgName(argName);
        return from;
    }

    private static void initSystem() {
        if (System.getProperty("snap.context") == null) {
            System.setProperty("snap.context", "snap");
        }
        Locale.setDefault(Locale.ENGLISH); // Force usage of english locale
        SystemUtils.init3rdPartyLibs(GPT.class);
    }

    public SummaryCSVTool(Logger logger, ShapeFileReader shapeFileReader, String nameColumn) {
        this.logger = logger;
        this.shapeFileReader = shapeFileReader;
        this.filenameDateExtractor = new FilenameDateExtractor();
        statisticsDatabase = new StatisticsDatabase(nameColumn);
    }

    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(120);
        System.out.println(getHeader());
        helpFormatter.printHelp("SummaryCSVTool", options, true);
    }

    private void putOutSummerizedData(File outputDir) throws IOException {
        final ObservationYear[] years = statisticsDatabase.getYears();
        for (ObservationYear year : years) {
            final ParameterName[] parameterNames = statisticsDatabase.getParameterNames(year);
            for (ParameterName parameterName : parameterNames) {
                final File outputFile = new File(outputDir, "WFD_stat_" + year + "_" + parameterName + ".txt");
                FileOutputStream fileOutputStream = null;
                PrintWriter writer = null;
                try {
                    fileOutputStream = new FileOutputStream(outputFile);
                    writer = new PrintWriter(fileOutputStream);
                    final DatabaseRecord[] databaseRecords = statisticsDatabase.getData(year, parameterName);
                    printHeader(databaseRecords, writer, parameterName);
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

    private void printHeader(DatabaseRecord[] records, PrintWriter pw, ParameterName parameterName) {
        pw.print("Bod.ID");
        pw.print(TAB);
        pw.print("Bod.Name");

        final TreeMap<Date, TreeSet<String>> dateColNamesMap = new TreeMap<>();
        for (DatabaseRecord record : records) {
            final Set<Date> dataDates = record.getDataDates();
            for (Date date : dataDates) {
                final TreeSet<String> colNames;
                if (dateColNamesMap.containsKey(date)) {
                    colNames = dateColNamesMap.get(date);
                } else {
                    colNames = new TreeSet<>();
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
            final String year = Integer.toString(calendar.get(Calendar.YEAR));
            final String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
            final String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
            final String monthPart = month.length() == 1 ? "0" + month : month;
            final String dayPart = day.length() == 1 ? "0" + day : day;
            final String datePart = "" + year + monthPart + dayPart;
            final TreeSet<String> colNames = dateColNamesEntry.getValue();
            for (String colName : colNames) {
                pw.print(TAB);
                pw.print(parameterName + "_" + colName + "_" + datePart);
            }
        }

        pw.println();
    }

    private void printData(DatabaseRecord[] databaseRecords, PrintWriter writer) {
        final Map<Date, Integer> columnsPerDay = new TreeMap<>();
        for (DatabaseRecord record : databaseRecords) {
            final Set<Date> dataDates = record.getDataDates();
            for (Date dataDate : dataDates) {
                final Set<String> statDataColumns = record.getStatDataColumns(dataDate);
                if (columnsPerDay.containsKey(dataDate)) {
                    final Integer numCols = columnsPerDay.get(dataDate);
                    columnsPerDay.put(dataDate, Math.max(statDataColumns.size(), numCols));
                } else {
                    columnsPerDay.put(dataDate, statDataColumns.size());
                }
            }
        }
        for (DatabaseRecord record : databaseRecords) {
            print(record, writer, columnsPerDay);
        }
    }

    private void print(DatabaseRecord record, PrintWriter pw, Map<Date, Integer> columnsPerDay) {
        pw.print(record.geomId);
        pw.print(TAB);
        pw.print(record.geomName);
        for (Date date : columnsPerDay.keySet()) {
            final Set<String> statDataColumns = record.getStatDataColumns(date);
            if (statDataColumns != null) {
                for (String statDataColumn : statDataColumns) {
                    pw.print(TAB);
                    pw.print(record.getValue(date, statDataColumn));
                }
            } else {
                final Integer numCols = columnsPerDay.get(date);
                for (int i = 0; i < numCols; i++) {
                    pw.print(TAB);
                }
            }
        }

        pw.println();
    }

    void summarize(File inputDir) {
        final File[] shapeFiles = inputDir.listFiles((dir, name) -> {return name.toLowerCase().endsWith(".shp");});

        for (File shapeFile : shapeFiles) {
            if (!isValidDateExtractableFileName(shapeFile)) {
                continue;
            }
            final File mappingFile = new File(inputDir, FileUtils.getFilenameWithoutExtension(shapeFile).concat("_band_mapping.txt"));
            if (!mappingFile.isFile()) {
                continue;
            }

            try (FileReader fileReader = new FileReader(mappingFile)) {
                final ProductData.UTC date = filenameDateExtractor.getDate(shapeFile);
                final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = shapeFileReader.read(shapeFile);
                final Properties properties = new Properties();
                properties.load(fileReader);
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
            logger.log(Level.WARNING, "The filename '" + shapeFile.getName() + "' does not match the pattern " +
                    FILENAME_PATTERN_SHAPEFILE + ".");
            logger.log(Level.INFO, "Continuing with next ESRI shapefile.");
        }
        return validFilename;
    }

    private static String getHeader() {
        final StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println(EXE_NAME + " version " + EXE_VERSION);
        printWriter.println("    The tool reads statistical data from ESRI shapefiles and summarizes it in ");
        printWriter.println("    *.csv-files. One csv file will be generated per year and parameter.");
        printWriter.println("    An example for a file name is WFD_stat_2009_CHL.csv. ");
        printWriter.close();
        return stringWriter.toString();
    }

    public interface ShapeFileReader {

        FeatureCollection<SimpleFeatureType, SimpleFeature> read(File shapeFile) throws IOException;
    }
}

package org.esa.pfa.db;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.FeatureType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * The PFA Dataset Query Tool.
 *
 * @author Norman
 */
public class DsQuery {
    static final PrintWriter PW = new PrintWriter(new OutputStreamWriter(System.out), true);

    DatasetDescriptor dsDescriptor;

    final Options options;

    // <options>
    static CommonOptions commonOptions = new CommonOptions();
    int maxThreadCount = 1;
    int maxHitCount = 100;
    int precisionStep;
    String defaultField = "product";
    String indexName;
    // </options>

    // <arguments>
    private File datasetDir;
    // </arguments>

    public DsQuery() {
        indexName = DsIndexer.DEFAULT_INDEX_NAME;
        precisionStep = NumericUtils.PRECISION_STEP_DEFAULT;
        maxThreadCount = 1;
        maxHitCount = 20;
        defaultField = "product";

        options = new Options();
        CommonOptions.addOptions(options);
        options.addOption(CommonOptions.opt('i', "index-name", 1, "string", String.format("Name of the input index directory to be queried. Default is '%s'.", indexName)));
        options.addOption(CommonOptions.opt('m', "max-hits", 1, "int", String.format("Maximum number of hits. Default is %d.", maxHitCount)));
        options.addOption(CommonOptions.opt('t', "max-threads", 1, "int", String.format("Maximum number of threads to use for a query. Default is %d.", maxThreadCount)));
        options.addOption(CommonOptions.opt('f', "default-field", 1, "string", String.format("Default field name for query expressions. Default is '%s'.", defaultField)));
        options.addOption(CommonOptions.opt('p', "precision-step", 1, "int", String.format("Precision step used for indexing numeric data. " +
                                                                                           "Lower values consume more disk space but speed up searching. " +
                                                                                           "Suitable values are between 1 and 8. Default is %d.", precisionStep)));
    }


    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        try {
            System.exit(new DsQuery().run(args));
        } catch (Exception e) {
            commonOptions.printError(e);
            System.exit(1);
        }
    }

    private int run(String[] args) throws Exception {
        if (!parseCommandLine(args)) {
            return 1;
        }

        dsDescriptor = DatasetDescriptor.read(new File(datasetDir, "ds-descriptor.xml"));

        StandardQueryParser parser = new StandardQueryParser(DsIndexer.LUCENE_ANALYZER);
        NumericConfiguration numConf = new NumericConfiguration(precisionStep);
        parser.setNumericConfigMap(numConf.getNumericConfigMap(dsDescriptor));

        //try (Directory indexDirectory = new MMapDirectory(new File(datasetDir, indexName))) {
        //try (Directory indexDirectory = new NIOFSDirectory(new File(datasetDir, indexName))) {
        try (Directory indexDirectory = new SimpleFSDirectory(new File(datasetDir, indexName))) {
            try (IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher indexSearcher = new IndexSearcher(indexReader, Executors.newFixedThreadPool(this.maxThreadCount));
                BufferedReader queryReader = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("Type 'help' for help, type 'exit' or 'quit' to leave.");
                System.out.flush();

                String queryExpr;
                do {
                    System.out.print(">>> ");
                    System.out.flush();
                    queryExpr = queryReader.readLine();
                } while (queryExpr != null && processQuery(indexSearcher, parser, queryExpr));
            }
        }
        return 0;
    }

    private boolean parseCommandLine(String[] args) throws ParseException {
        CommandLine commandLine = new PosixParser().parse(options, args);
        commonOptions.configure(commandLine);
        if (commandLine.hasOption("help")) {
            new HelpFormatter().printHelp(PW, 80,
                                          DsQuery.class.getSimpleName() + " [OPTIONS] <index-dir>",
                                          "Interactive query tool for a lucene index. [OPTIONS] are:",
                                          options, 2, 2, "\n");
            return false;
        }
        if (commandLine.getArgList().size() != 1) {
            new HelpFormatter().printUsage(PW, 80, DsQuery.class.getSimpleName(), options);
            return false;
        }

        datasetDir = new File(commandLine.getArgs()[0]);

        String indexName = commandLine.getOptionValue("index-name");
        if (indexName != null) {
            this.indexName = indexName;
        }

        String precisionStep = commandLine.getOptionValue("precision-step");
        if (precisionStep != null) {
            this.precisionStep = Integer.parseInt(precisionStep);
        }

        String maxThreads = commandLine.getOptionValue("max-threads");
        if (maxThreads != null) {
            this.maxThreadCount = Integer.parseInt(maxThreads);
        }

        String maxHits = commandLine.getOptionValue("max-hits");
        if (maxHits != null) {
            this.maxHitCount = Integer.parseInt(maxHits);
        }

        String defaultField = commandLine.getOptionValue("default-field");
        if (defaultField != null) {
            this.defaultField = defaultField;
        }

        return true;
    }

    private boolean processQuery(IndexSearcher indexSearcher, StandardQueryParser parser, String queryExpr) {

        queryExpr = queryExpr.trim();

        if (queryExpr.isEmpty()) {
            return true;
        }

        if (queryExpr.equalsIgnoreCase("help")) {
            printHelp();
            return true;
        }

        if (queryExpr.equalsIgnoreCase("exit") || queryExpr.equalsIgnoreCase("quit")) {
            return false;
        }

        if (queryExpr.contains("=")) {
            String[] split = queryExpr.split("=");
            if (split[0].trim().equals("default")) {
                defaultField = split[1].trim();
                System.out.println("Default field set to '" + defaultField + "'");
            }
            return true;
        }

        try {
            Query query = parser.parse(queryExpr, defaultField);

            long t1 = System.currentTimeMillis();
            TopDocs topDocs = indexSearcher.search(query, maxHitCount);
            long t2 = System.currentTimeMillis();

            if (topDocs.totalHits == 0) {
                System.out.println("no documents found within " + (t2 - t1) + " ms");
            } else {
                System.out.println("found " + topDocs.totalHits + " documents(s) within " + (t2 - t1) + " ms:");
                ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                for (int i = 0; i < scoreDocs.length; i++) {
                    ScoreDoc scoreDoc = scoreDocs[i];
                    int docID = scoreDoc.doc;
                    Document doc = indexSearcher.doc(docID);
                    String productName = doc.getValues("product")[0];
                    String patchX = doc.getValues("px")[0];
                    String patchY = doc.getValues("py")[0];
                    System.out.printf("[%5d]: product:\"%s\", px:%s, py:%s\n", i + 1, productName, patchX, patchY);
                }
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }

        return true;
    }

    private void printHelp() {
        System.out.println("Searchable Fields:");
        printAttrHelp("product", String.class, "EO data product name");
        printAttrHelp("px", Integer.TYPE, "Patch x-coordinate");
        printAttrHelp("py", Integer.TYPE, "Patch y-coordinate");
        for (FeatureType featureType : dsDescriptor.getFeatureTypes()) {
            if (featureType.hasAttributes()) {
                AttributeType[] attributeTypes = featureType.getAttributeTypes();
                for (AttributeType attributeType : attributeTypes) {
                    printAttrHelp(featureType.getName() + "." + attributeType.getName(), attributeType);
                }
            } else {
                printAttrHelp(featureType.getName(), featureType);
            }
        }
        System.out.println();
        System.out.println("Query Parser Syntax: <field>:<term> | <field>:\"<phrase>\" | <field>:[<n1> TO <n2>]");
        System.out.println("If you omit '<field>:' the default field is used which is '" + defaultField + "'.");
        System.out.println("You can change the default field by typing 'default=<field>'.");
        System.out.println("Multiple queries are ORed, you can otherwise combine them using AND, OR, NOT, +, -.");
        System.out.println("See https://lucene.apache.org/core/4_6_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description");
        System.out.println();
    }

    private void printAttrHelp(String fieldName, AttributeType attributeType) {
        printAttrHelp(fieldName, attributeType.getValueType(), attributeType.getDescription());
    }

    private void printAttrHelp(String fieldName, Class<?> valueType, String description) {
        System.out.printf("  %s: %s  --  %s\n", fieldName, valueType, description);
    }
}

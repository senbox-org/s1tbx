package org.esa.senbox.help;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.Executors;


/**
 * The PFA Dataset Query Tool.
 *
 * @author Norman
 */
public class Finder {

    int maxThreadCount = 1;
    int maxHitCount = 100;
    String defaultField = "product";
    String indexName = Indexer.DEFAULT_INDEX_NAME;

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        try {
            System.exit(new Finder().run(args));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int run(String[] args) throws Exception {

        StandardQueryParser parser = new StandardQueryParser(Indexer.LUCENE_ANALYZER);

        //try (Directory indexDirectory = new MMapDirectory(new File(indexName))) {
        //try (Directory indexDirectory = new NIOFSDirectory(new File(indexName))) {
        try (Directory indexDirectory = new SimpleFSDirectory(new File(indexName))) {
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
        printAttrHelp("sensor", String.class, "Sensor name");
        printAttrHelp("productType", String.class, "Product type");
        printAttrHelp("nodeId", String.class, "Node identifier");
        printAttrHelp("nodeName", String.class, "Node name");
        printAttrHelp("uri", String.class, "The link to the (web) resource");
        System.out.println();
        System.out.println("Query Parser Syntax: <field>:<term> | <field>:\"<phrase>\" | <field>:[<n1> TO <n2>]");
        System.out.println("If you omit '<field>:' the default field is used which is '" + defaultField + "'.");
        System.out.println("You can change the default field by typing 'default=<field>'.");
        System.out.println("Multiple queries are ORed, you can otherwise combine them using AND, OR, NOT, +, -.");
        System.out.println("See https://lucene.apache.org/core/4_6_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description");
        System.out.println();
    }

    private void printAttrHelp(String fieldName, Class<?> valueType, String description) {
        System.out.printf("  %s: %s  --  %s\n", fieldName, valueType.getSimpleName(), description);
    }
}

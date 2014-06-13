package org.esa.senbox.help;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * @author Norman Fomferra
 */
public class Indexer {

    public static final Version LUCENE_VERSION = Version.LUCENE_47;
    //public static final Analyzer LUCENE_ANALYZER = new SimpleAnalyzer(LUCENE_VERSION);
    public static final Analyzer LUCENE_ANALYZER = new ProductNameAnalyzer(LUCENE_VERSION);
    public static final String DEFAULT_INDEX_NAME = "lucene-index";

    private IndexWriter indexWriter;
    private long docID;

    private int maxThreadCount = IndexWriterConfig.DEFAULT_MAX_THREAD_STATES;
    private String indexName = DEFAULT_INDEX_NAME;


    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        try {
            System.exit(new Indexer().run(args));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class ProductNameAnalyzer extends Analyzer {

        private Version version;

        private ProductNameAnalyzer(Version version) {
            this.version = version;
        }

        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            return new TokenStreamComponents(new ProductNameTokenizer(version, reader));
        }
    }

    private static class ProductNameTokenizer extends CharTokenizer {

        public ProductNameTokenizer(Version version, Reader in) {
            super(version, in);
        }

        @Override
        protected int normalize(int c) {
            return Character.toLowerCase(c);
        }

        @Override
        protected boolean isTokenChar(int c) {
            return Character.isLetter(c) || Character.isDigit(c);
        }
    }

    private int run(String[] args) throws Exception {
        String csvFilePath = args[0];

        IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, LUCENE_ANALYZER);
        config.setRAMBufferSizeMB(16);
        config.setMaxThreadStates(maxThreadCount);


        long t1, t2;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            try (Directory indexDirectory = FSDirectory.open(new File(indexName))) {
                indexWriter = new IndexWriter(indexDirectory, config);
                try {
                    t1 = System.currentTimeMillis();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split("\\s+");
                        addPatchToIndex(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]);
                    }
                    t2 = System.currentTimeMillis();
                } finally {
                    indexWriter.close();
                }
            }
        }

        System.out.println(docID + "(s) patches added to index within " + ((t2 - t1) / 1000) + " seconds");
        return 0;
    }

    private void addPatchToIndex(String sensor, String productType, String nodeId, String nodeName, String uri) throws IOException {

        Document doc = new Document();
        doc.add(new TextField("sensor", sensor, Field.Store.YES));
        doc.add(new TextField("productType", productType, Field.Store.YES));
        doc.add(new TextField("nodeId", nodeId, Field.Store.YES));
        doc.add(new TextField("nodeName", nodeName, Field.Store.YES));
        doc.add(new TextField("uri", uri, Field.Store.YES));

        indexWriter.addDocument(doc);
        System.out.printf("[%5d]: sensor:\"%s\", productType:\"%s\", nodeId:\"%s\", nodeName:\"%s\", uri:\"%s\"\n", docID, sensor, productType, nodeId, nodeName, uri);
//        System.out.printf("[%5d]: product:\"%s\", px:%d, py:%d\n", docID, productName, patchX, patchY);
        docID++;
    }
}

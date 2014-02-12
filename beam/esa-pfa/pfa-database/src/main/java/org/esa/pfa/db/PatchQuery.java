package org.esa.pfa.db;

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
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * The PFA Dataset Query Tool.
 *
 */
public class PatchQuery implements QueryInterface {

    DatasetDescriptor dsDescriptor;

    int maxThreadCount = 1;
    int maxHitCount = 20;
    int precisionStep;
    String defaultField = "product";
    String indexName;

    private final File datasetDir;

    private StandardQueryParser parser;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;

    public PatchQuery(final File datasetDir) throws IOException {
        this.datasetDir = datasetDir;

        indexName = DsIndexer.DEFAULT_INDEX_NAME;
        precisionStep = NumericUtils.PRECISION_STEP_DEFAULT;
        maxThreadCount = 1;
        maxHitCount = 20;
        defaultField = "product";

        init();
    }

    private void init() throws IOException {

        dsDescriptor = DatasetDescriptor.read(new File(datasetDir, "ds-descriptor.xml"));

        parser = new StandardQueryParser(DsIndexer.LUCENE_ANALYZER);
        NumericConfiguration numConf = new NumericConfiguration(precisionStep);
        parser.setNumericConfigMap(numConf.getNumericConfigMap(dsDescriptor));

        //try (Directory indexDirectory = new MMapDirectory(new File(datasetDir, indexName))) {
        //try (Directory indexDirectory = new NIOFSDirectory(new File(datasetDir, indexName))) {
        try (Directory indexDirectory = new SimpleFSDirectory(new File(datasetDir, indexName))) {
            indexReader = DirectoryReader.open(indexDirectory);
            indexSearcher = new IndexSearcher(indexReader, Executors.newFixedThreadPool(this.maxThreadCount));
        }
    }

    //todo must be specific to the application
    public DatasetDescriptor getDsDescriptor() {
        return dsDescriptor;
    }

    public Patch[] query(String queryExpr, int hitCount) {
        final List<Patch> patchList = new ArrayList<Patch>(100);

        queryExpr = queryExpr.trim();

        try {
            final Query query = parser.parse(queryExpr, defaultField);

            long t1 = System.currentTimeMillis();
            TopDocs topDocs = indexSearcher.search(query, hitCount);
            long t2 = System.currentTimeMillis();

            if (topDocs.totalHits == 0) {
                System.out.println("no documents found within " + (t2 - t1) + " ms");
            } else {
                System.out.println("found " + topDocs.totalHits + " documents(s) within " + (t2 - t1) + " ms:");
                int i=0;
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    final Document doc = indexSearcher.doc(scoreDoc.doc);
                    String productName = doc.getValues("product")[0];
                    int patchX = Integer.parseInt(doc.getValues("px")[0]);
                    int patchY = Integer.parseInt(doc.getValues("py")[0]);

                    Patch patch = new Patch(patchX, patchY, null, null);
                    getFeatures(doc, patch);
                    patchList.add(patch);

                    ++i;
                    System.out.printf("[%5d]: product:\"%s\", px:%d, py:%d\n", i + 1, productName, patchX, patchY);
                }
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }

        return patchList.toArray(new Patch[patchList.size()]);
    }

    private void getFeatures(final Document doc, final Patch patch) {
        for(FeatureType feaType : dsDescriptor.featureTypes) {
            if(feaType.hasAttributes()) {
                for(AttributeType attrib : feaType.getAttributeTypes()) {
                    final String name = feaType.getName()+'.'+attrib.getName();
                    final String[] values = doc.getValues(name);
                    if(values != null && values.length > 0) {
                        FeatureType newFeaType = new FeatureType(name, attrib.getDescription(), attrib.getValueType());
                        patch.addFeature(createFeature(newFeaType, values[0]));
                    }
                }
            } else {
                final String[] values = doc.getValues(feaType.getName());
                if(values != null && values.length > 0) {
                    patch.addFeature(createFeature(feaType, values[0]));
                }
            }
        }
    }

    private static Feature createFeature(FeatureType feaType, final String value) {
        final Class<?> valueType = feaType.getValueType();

        if(Double.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, Double.parseDouble(value));
        } else if(Float.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, Float.parseFloat(value));
        } else if(Integer.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, Integer.parseInt(value));
        } else if(Boolean.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, Boolean.parseBoolean(value));
        } else if(Character.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, value);
        } else if(String.class.isAssignableFrom(valueType)) {
            return new Feature(feaType, value);
        }
        return null;
    }

    public Patch[] getRandomPatches(final int numPatches) {
        return query("product: ENVI*", numPatches);
    }
}

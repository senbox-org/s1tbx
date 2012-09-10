package org.esa.beam.statistics;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.opengis.feature.simple.SimpleFeature;

public class Outputter {

    private MetadataWriter metadataWriter;
    private CsvStatisticsWriter csvStatisticsWriter;
    private FeaturesStatisticsWriter featuresStatisticsWriter;

    private boolean canOutputMetadata;
    private boolean canOutputCsv;
    private boolean canOutputEsriShapefile;
    private File outputEsriShapeFile;

    public void setMetadataWriter(MetadataWriter metadataWriter) {
        this.metadataWriter = metadataWriter;
        canOutputMetadata = metadataWriter != null;
    }

    public void setCsvStatisticsWriter(CsvStatisticsWriter csvStatisticsWriter) {
        this.csvStatisticsWriter = csvStatisticsWriter;
        canOutputCsv = csvStatisticsWriter != null;
    }

    public void setFeaturesStatisticsWriter(FeaturesStatisticsWriter featuresStatisticsWriter, File outputEsriShapeFile) {
        this.outputEsriShapeFile = outputEsriShapeFile;
        this.featuresStatisticsWriter = featuresStatisticsWriter;
        canOutputEsriShapefile = featuresStatisticsWriter != null && outputEsriShapeFile != null;
    }

    public void initialiseOutput(Product[] allSourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        if (canOutputMetadata) {
            metadataWriter.writeMetadata(allSourceProducts, startDate, endDate, regionIds);
        }
        if (canOutputCsv) {
            csvStatisticsWriter.initialiseOutput(algorithmNames);
        }
        if (canOutputEsriShapefile) {
            featuresStatisticsWriter.initialiseOutput(bandNames, algorithmNames);
        }
    }

    public void addToOutput(String bandIdentifier, String regionName, Map<String, Number> stxMap) {
        if (canOutputCsv) {
            csvStatisticsWriter.addToOutput(bandIdentifier, regionName, stxMap);
        }
        if (canOutputEsriShapefile) {
            featuresStatisticsWriter.addToOutput(bandIdentifier, regionName, stxMap);
        }
    }

    public void finaliseOutput() throws IOException {
        if (canOutputCsv) {
            csvStatisticsWriter.finaliseOutput();
        }
        if (canOutputEsriShapefile) {
            final List<SimpleFeature> features = featuresStatisticsWriter.getFeatures();
            EsriShapeFileWriter.write(features, outputEsriShapeFile);
        }
    }
}

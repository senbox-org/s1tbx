package org.esa.pfa.db;

import com.thoughtworks.xstream.XStream;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.FeatureType;

import java.io.*;

/**
 * Created by Norman on 31.01.14.
 */
public class DatasetDescriptor {

    String name;
    String version;
    String description;
    FeatureType[] featureTypes;

    DatasetDescriptor() {
    }

    public DatasetDescriptor(String name, String version, String description, FeatureType... featureTypes) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.featureTypes = featureTypes;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public FeatureType[] getFeatureTypes() {
        return featureTypes;
    }

    public static DatasetDescriptor read(File file) throws IOException {
        try (FileReader fileReader = new FileReader(file)) {
            return read(fileReader);
        }
    }

    public static DatasetDescriptor read(Reader reader) {
        DatasetDescriptor datasetDescriptor = new DatasetDescriptor();
        getXStream().fromXML(reader, datasetDescriptor);
        if (datasetDescriptor.featureTypes == null)
            datasetDescriptor.featureTypes = new FeatureType[0];
         return datasetDescriptor;
    }

    public void write(File file) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            write(fileWriter);
        }
    }

    public void write(Writer writer) {
        getXStream().toXML(this, writer);
    }

    private static XStream getXStream() {
        XStream xStream = new XStream();
        xStream.alias("DatasetDescriptor", DatasetDescriptor.class);
        xStream.alias("FeatureType", FeatureType.class);
        xStream.alias("AttributeType", AttributeType.class);
        xStream.setClassLoader(DatasetDescriptor.class.getClassLoader());
        return xStream;
    }


}

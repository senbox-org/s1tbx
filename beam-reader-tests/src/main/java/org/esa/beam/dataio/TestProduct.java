package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

class TestProduct {

    @JsonProperty(required = true)
    private String id;
    @JsonProperty(required = true)
    private String relativePath;
    @JsonProperty()
    private String description;

    private transient boolean exists = true;
    private transient Class<? extends ProductReaderPlugIn> readerPlugInClass;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getRelativePath() {
        return relativePath;
    }

    String getDescription() {
        return description;
    }

    void exists(boolean exists) {
        this.exists = exists;
    }

    boolean exists() {
        return exists;
    }

    boolean isDifferent(TestProduct other) {
        if (!(id.equals(other.getId()) &&
                relativePath.equals(other.getRelativePath()))) {
            return true;
        }
        return false;
    }
}

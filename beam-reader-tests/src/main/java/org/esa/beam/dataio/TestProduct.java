package org.esa.beam.dataio;


class TestProduct {

    private String id;
    private String relativePath;
    private String description;
    private transient boolean exists = true;

    void setId(String name) {
        this.id = name;
    }

    String getId() {
        return id;
    }

    void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    String getRelativePath() {
        return relativePath;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void exists(boolean exists) {
        this.exists = exists;
    }

    boolean exists() {
        return exists;
    }
}

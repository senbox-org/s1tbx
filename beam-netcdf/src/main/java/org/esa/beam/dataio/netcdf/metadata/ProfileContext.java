package org.esa.beam.dataio.netcdf.metadata;

public interface ProfileContext {
    void setProperty(String name, Object property);

    Object getProperty(String name);
}

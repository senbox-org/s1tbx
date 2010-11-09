package org.esa.beam.dataio.netcdf;

interface ProfileContext {
    void setProperty(String name, Object property);

    Object getProperty(String name);
}

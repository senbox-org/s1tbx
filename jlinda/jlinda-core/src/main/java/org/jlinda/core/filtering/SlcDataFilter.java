package org.jlinda.core.filtering;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;

public abstract class SlcDataFilter implements DataFilter {

    /// fields ///
    ComplexDoubleMatrix data;

    DoubleMatrix filter;
    SLCImage metadata;
    Window tile;

    public void defineFilter() throws Exception {
    }

    public void applyFilter() {
    }

    /// getters and setters ///
    public ComplexDoubleMatrix getData() {
        return data;
    }

    public void setData(ComplexDoubleMatrix data) {
        this.data = data;
    }

    public DoubleMatrix getFilter() {
        return filter;
    }

    public void setFilter(DoubleMatrix filter) {
        this.filter = filter;
    }

    public SLCImage getMetadata() {
        return metadata;
    }

    public void setMetadata(SLCImage metadata) {
        this.metadata = metadata;
    }

    public Window getTile() {
        return tile;
    }

    public void setTile(Window tile) {
        this.tile = tile;
    }

}

package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

/**
 * Created by Norman on 20.03.2014.
 */
public interface FilterEditor {

    Filter getFilter();

    void setFilter(Filter filter);

    void show();

    void hide();
}

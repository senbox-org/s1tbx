package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

/**
 * Filter editors are used to get/set a filter and can be displayed.
 *
 * @author Norman
 */
public interface FilterEditor {

    Filter getFilter();

    void setFilter(Filter filter);

    void show();

    void hide();
}

package org.esa.beam.opendap.ui;

import org.esa.beam.opendap.datamodel.OpendapLeaf;

import javax.swing.JComponent;

/**
 * @author Sabine Embacher
 * @author Tonio Fincke
 * @author Thomas Storm
 */
interface FilterComponent {

    JComponent getUI();

    /**
     * @param leaf The leaf to check.
     * @return true if the leaf passes the filter, i.e. it is not filtered out.
     */
    boolean accept(OpendapLeaf leaf);

    void addFilterChangeListener(FilterChangeListener listener);
}

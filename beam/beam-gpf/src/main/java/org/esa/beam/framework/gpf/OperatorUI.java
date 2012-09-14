package org.esa.beam.framework.gpf;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.*;
import java.util.Map;

import com.bc.ceres.binding.dom.XppDomElement;

/**
* The abstract base class for all operator user interfaces intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 * CreateOpTab() must be implemented in order to create the operator user interface component
 */
public interface OperatorUI {

    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext);

    public void initParameters();

    public UIValidation validateParameters();

    public void updateParameters();

    public void setSourceProducts(Product[] products);

    public void convertToDOM(XppDomElement parentElement);
}

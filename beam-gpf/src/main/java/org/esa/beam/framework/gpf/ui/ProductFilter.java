package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.datamodel.Product;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public interface ProductFilter {

    boolean accept(Product product);


}

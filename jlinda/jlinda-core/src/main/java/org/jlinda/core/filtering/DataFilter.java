package org.jlinda.core.filtering;

/**
 * User: pmar@ppolabs.com
 * Date: 6/3/11
 * Time: 12:39 PM
 */
public interface DataFilter {

    // define filter
    void defineFilter() throws Exception;

    void applyFilter();

}

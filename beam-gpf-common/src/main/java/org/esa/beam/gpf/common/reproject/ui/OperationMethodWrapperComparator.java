package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.IdentifiedObject;

import java.util.Comparator;

/**
 * The code part of the name is used to compare the given {@link IdentifiedObject IdentifiedObjects}.
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class OperationMethodWrapperComparator implements Comparator<IdentifiedObject> {

    @Override
    public int compare(IdentifiedObject o1, IdentifiedObject o2) {
        final String name1 = o1.getName().getCode();
        final String name2 = o2.getName().getCode();
        return name1.compareTo(name2);
    }
}

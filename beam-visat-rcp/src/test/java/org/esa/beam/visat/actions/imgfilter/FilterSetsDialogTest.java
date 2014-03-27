package org.esa.beam.visat.actions.imgfilter;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.StandardFilters;

import javax.swing.*;

/**
 * @author Norman
 */
public class FilterSetsDialogTest {


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        FilterSet userFilterSet = new FilterSet("User", true);
        Filter filter1 = new Filter("conv", "c", 5, 5, new double[5 * 5], 1);
        filter1.setEditable(true);
        Filter filter2 = new Filter("median", "m", Filter.Operation.MEDIAN, 5, 5);
        filter2.setEditable(true);
        userFilterSet.addFilterModels("User", filter1, filter2);

        FilterSet predefFilterSet = new FilterSet("System", false);
        predefFilterSet.addFilterModels("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
        predefFilterSet.addFilterModels("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
        predefFilterSet.addFilterModels("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
        predefFilterSet.addFilterModels("Sharpen", StandardFilters.SHARPENING_FILTERS);
        predefFilterSet.addFilterModels("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
        predefFilterSet.addFilterModels("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
        predefFilterSet.addFilterModels("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);

        XStream xStream = FilterSet.createXStream();
        String xml = xStream.toXML(predefFilterSet);
        System.out.println(xml);

        FilterSetsDialog dialog = new FilterSetsDialog(null, userFilterSet, predefFilterSet);
        dialog.show();


    }
}

package org.esa.beam.visat.actions.imgfilter;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.StandardFilters;

import javax.swing.UIManager;

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
        userFilterSet.addFilter("User", filter1, filter2);

        FilterSet predefFilterSet = new FilterSet("System", false);
        predefFilterSet.addFilter("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
        predefFilterSet.addFilter("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
        predefFilterSet.addFilter("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
        predefFilterSet.addFilter("Sharpen", StandardFilters.SHARPENING_FILTERS);
        predefFilterSet.addFilter("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
        predefFilterSet.addFilter("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
        predefFilterSet.addFilter("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);

        XStream xStream = FilterSet.createXStream();
        String xml = xStream.toXML(predefFilterSet);
        System.out.println(xml);

        FilterSetsDialog dialog = new FilterSetsDialog(null, userFilterSet, predefFilterSet);
        dialog.show();


    }
}

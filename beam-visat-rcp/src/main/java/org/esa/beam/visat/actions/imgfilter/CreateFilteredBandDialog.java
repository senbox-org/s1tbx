package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.StandardFilters;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Norman on 20.03.2014.
 */
public class CreateFilteredBandDialog extends ModalDialog implements FilterSetForm.Listener {

    public static final String TITLE = "Create Filtered Band"; /*I18N*/
    private final Product product;
    private final FilterSetsForm filterSetsForm;
    private final FilterSetFileStore filterSetStore;
    private List<FilterSet> userFilterSets;

    public CreateFilteredBandDialog(Product product, String sourceBandName, String helpId) {
        super(VisatApp.getApp().getMainFrame(),
              TITLE,
              ModalDialog.ID_OK_CANCEL_HELP,
              helpId);
        this.product = product;

        FilterSet systemFilterSet = new FilterSet("System", false);
        systemFilterSet.addFilterModels("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
        systemFilterSet.addFilterModels("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
        systemFilterSet.addFilterModels("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
        systemFilterSet.addFilterModels("Sharpen", StandardFilters.SHARPENING_FILTERS);
        systemFilterSet.addFilterModels("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
        systemFilterSet.addFilterModels("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
        systemFilterSet.addFilterModels("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);

        filterSetStore = new FilterSetFileStore(getFiltersDir());
        try {
            userFilterSets = filterSetStore.loadFilterSetModels();
        } catch (IOException e) {
            userFilterSets = new ArrayList<>();
            // todo
            e.printStackTrace();
        }

        ArrayList<FilterSet> filterSets = new ArrayList<>();
        filterSets.add(systemFilterSet);
        if (userFilterSets.isEmpty()) {
            userFilterSets.add(new FilterSet("User", true));
        }
        filterSets.addAll(userFilterSets);

        filterSetsForm = new FilterSetsForm(sourceBandName,
                                            this,
                                            filterSetStore, new FilterWindow(getJDialog()),
                                            filterSets.toArray(new FilterSet[filterSets.size()]));

        setContent(filterSetsForm);
    }

    @Override
    protected void onOK() {
        super.onOK();
        for (FilterSet userFilterSet : userFilterSets) {
            userFilterSet.setEditable(true);
            try {
                filterSetStore.storeFilterSetModel(userFilterSet);
            } catch (IOException e) {
                // todo
                e.printStackTrace();
            }
        }
    }

    public DialogData getDialogData() {
        return new DialogData(filterSetsForm.getSelectedFilter(), filterSetsForm.getTargetBandName(), filterSetsForm.getIterationCount());
    }

    @Override
    protected boolean verifyUserInput() {
        String message = null;
        final String targetBandName = filterSetsForm.getTargetBandName();
        if (targetBandName.equals("")) {
            message = "Please enter a name for the new filtered band."; /*I18N*/
        } else if (!ProductNode.isValidNodeName(targetBandName)) {
            message = MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                                   "Please choose a different band name.", targetBandName); /*I18N*/
        } else if (product.containsBand(targetBandName)) {
            message = MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                                   "Please choose a different band name.", targetBandName); /*I18N*/
        } else if (filterSetsForm.getSelectedFilter() == null) {
            message = "Please select an image filter.";    /*I18N*/
        }
        if (message != null) {
            VisatApp.getApp().showErrorDialog(TITLE, message);
            return false;
        }
        return true;
    }

    @Override
    public void filterModelSelected(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelSelected: filterModel = " + filter);
    }

    @Override
    public void filterModelAdded(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelAdded: filterModel = " + filter);
    }

    @Override
    public void filterModelRemoved(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelRemoved: filterModel = " + filter);
    }

    @Override
    public void filterModelChanged(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelChanged: filterModel = " + filter);
    }


    private File getFiltersDir() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".beam/beam-ui/auxdata/image-filters");
    }

    public static class DialogData {

        private final Filter filter;
        private final String bandName;
        private final int iterationCount;

        private DialogData(Filter filter, String bandName, int iterationCount) {
            this.filter = filter;
            this.bandName = bandName;
            this.iterationCount = iterationCount;
        }

        public String getBandName() {
            return bandName;
        }

        public Filter getFilter() {
            return filter;
        }

        public int getIterationCount() {
            return iterationCount;
        }
    }


}

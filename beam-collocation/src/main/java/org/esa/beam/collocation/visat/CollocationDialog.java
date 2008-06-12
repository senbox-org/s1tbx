package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;
import javax.swing.JDialog;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationDialog extends SingleTargetProductDialog {

    private CollocationFormModel formModel;
    private CollocationForm form;

    public CollocationDialog(AppContext appContext) {
        super(appContext, "Collocation", "collocation");
        formModel = new CollocationFormModel(getTargetProductSelector().getModel());
        form = new CollocationForm(formModel, getTargetProductSelector(), appContext);
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("master", formModel.getMasterProduct());
        productMap.put("slave", formModel.getSlaveProduct());

        final Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        // collocation parameters
        parameterMap.put("targetProductName", formModel.getTargetProductName());
        parameterMap.put("renameMasterComponents", formModel.isRenameMasterComponentsSelected());
        parameterMap.put("renameSlaveComponents", formModel.isRenameSlaveComponentsSelected());
        parameterMap.put("masterComponentPattern", formModel.getMasterComponentPattern());
        parameterMap.put("slaveComponentPattern", formModel.getSlaveComponentPattern());
        parameterMap.put("resamplingType", formModel.getResamplingType());

        return GPF.createProduct("Collocate", parameterMap, productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }


    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        float[] wl = new float[]{
                412.6395569f,
                442.5160217f,
                489.8732910f,
                509.8299866f,
                559.7575684f,
                619.7247925f,
                664.7286987f,
                680.9848022f,
                708.4989624f,
                753.5312500f,
                761.7092285f,
                778.5520020f,
                864.8800049f,
                884.8975830f,
                899.9100342f
        };
        final Product inputProduct1 = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = inputProduct1.addBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
        }
        inputProduct1.addBand("l1_flags", ProductData.TYPE_UINT32);

        final Product inputProduct2 = new Product("MER_RR_2P", "MER_RR_2P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = inputProduct2.addBand("reflec_" + (i + 1), ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
        }
        inputProduct2.addBand("l2_flags", ProductData.TYPE_UINT32);

        DefaultAppContext context = new DefaultAppContext("dev0");
        context.getProductManager().addProduct(inputProduct1);
        context.getProductManager().addProduct(inputProduct2);
        context.setSelectedProduct(inputProduct1);
        CollocationDialog dialog = new CollocationDialog(context);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }
}

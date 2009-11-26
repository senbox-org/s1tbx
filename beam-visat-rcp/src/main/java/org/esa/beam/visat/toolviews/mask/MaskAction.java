package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.net.URL;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
abstract class MaskAction extends AbstractAction {

    private MaskForm maskForm;

    MaskAction(MaskForm maskForm, String iconPath, String buttonName, String description) {
        this.maskForm = maskForm;
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(LARGE_ICON_KEY, loadIcon(iconPath));
        putValue(SHORT_DESCRIPTION, description);
        putValue("componentName", buttonName);
    }

    protected MaskForm getMaskForm() {
        return maskForm;
    }

    private ImageIcon loadIcon(String iconPath) {
        final ImageIcon icon;
        URL resource = MaskManagerForm.class.getResource(iconPath);
        if (resource != null) {
            icon = new ImageIcon(resource);
        } else {
            icon = UIUtils.loadImageIcon(iconPath);
        }
        return icon;
    }

    JComponent createComponent() {
        AbstractButton button = ToolButtonFactory.createButton(this, false);
        button.setName((String) getValue("componentName"));
        return button;
    }

    void updateState() {
    }

    protected Mask createNewMask(Mask.ImageType type) {
        final Product product = maskForm.getProduct();
        final ProductNodeGroup<Mask> productNodeGroup = product.getMaskGroup();
        String maskName = getNewMaskName(productNodeGroup);
        return new Mask(maskName,
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(),
                        type);
    }

    private String getNewMaskName(ProductNodeGroup<Mask> maskGroup) {
        String possibleName = "new_mask_" + maskGroup.getNodeCount();
        for (int i = 0; i <= maskGroup.getNodeCount(); i++) {
            possibleName = "new_mask_" + (maskGroup.getNodeCount() + i + 1);
            if (!maskGroup.contains(possibleName)) {
                break;
            }
        }
        return possibleName;
    }
}

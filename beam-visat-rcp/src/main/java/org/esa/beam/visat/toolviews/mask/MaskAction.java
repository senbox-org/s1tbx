package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;

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
    private static final String DEFAULT_MASK_NAME_PREFIX = "new_mask_";

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
        final Mask mask = new Mask(maskName,
                                   product.getSceneRasterWidth(),
                                   product.getSceneRasterHeight(),
                                   type);
        final VisatApp visatApp = VisatApp.getApp();
        if(visatApp != null) {
            final PropertyMap preferences = visatApp.getPreferences();
            mask.setImageColor(preferences.getPropertyColor("mask.color",
                                                            Mask.ImageType.DEFAULT_COLOR));
            mask.setImageTransparency(preferences.getPropertyDouble("mask.transparency",
                                                                    Mask.ImageType.DEFAULT_TRANSPARENCY));
        }

        return mask;
    }

    private String getNewMaskName(ProductNodeGroup<Mask> maskGroup) {
        String possibleName = DEFAULT_MASK_NAME_PREFIX + maskGroup.getNodeCount();
        for (int i = 0; i <= maskGroup.getNodeCount(); i++) {
            possibleName = DEFAULT_MASK_NAME_PREFIX + (maskGroup.getNodeCount() + i + 1);
            if (!maskGroup.contains(possibleName)) {
                break;
            }
        }
        return possibleName;
    }
}

package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.BitmaskDef;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class BitmaskDefDomConverter extends SessionElementDomConverter<BitmaskDef> {

    public BitmaskDefDomConverter() {
        super(BitmaskDef.class);
    }

    @Override
    public BitmaskDef convertDomToValue(DomElement parentElement, Object bitmaskDef) throws ConversionException,
                                                                                            ValidationException {
        final Integer refNo = Integer.valueOf(parentElement.getChild("refNo").getValue());
        final String bitmaskName = parentElement.getChild("bitmaskName").getValue();

        bitmaskDef = getProductManager().getProductByRefNo(refNo).getBitmaskDef(bitmaskName);

        return (BitmaskDef) bitmaskDef;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        BitmaskDef bitmaskDef = (BitmaskDef) value;
        final DomElement refNo = parentElement.createChild("refNo");
        final DomElement bitmaskName = parentElement.createChild("bitmaskName");
        refNo.setValue(String.valueOf(bitmaskDef.getProduct().getRefNo()));
        bitmaskName.setValue(bitmaskDef.getName());
    }
}

package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class RasterDataNodeDomConverter extends SessionElementDomConverter<RasterDataNode> {

    public RasterDataNodeDomConverter() {
        super(RasterDataNode.class);
    }

    @Override
    public RasterDataNode convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                           ValidationException {

        final Integer refNo = Integer.valueOf(parentElement.getChild("refNo").getValue());
        final String rasterName = parentElement.getChild("rasterName").getValue();

        value = getProductManager().getProductByRefNo(refNo).getRasterDataNode(rasterName);
        return (RasterDataNode) value;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        final RasterDataNode node = (RasterDataNode) value;
        final DomElement refNo = parentElement.createChild("refNo");
        final DomElement rasterName = parentElement.createChild("rasterName");
        refNo.setValue(String.valueOf(node.getProduct().getRefNo()));
        rasterName.setValue(node.getName());
    }

}

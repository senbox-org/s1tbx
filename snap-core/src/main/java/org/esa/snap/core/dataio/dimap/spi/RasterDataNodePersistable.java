package org.esa.snap.core.dataio.dimap.spi;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.ATTRIB_VALUE;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.TAG_ANCILLARY_RELATION;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.TAG_ANCILLARY_VARIABLE;
import static org.esa.snap.core.dataio.dimap.DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM;

import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public abstract class RasterDataNodePersistable implements DimapPersistable {

    protected void setAncillaryVariables(Element element, RasterDataNode rasterDataNode, Product product) {
        final List<Element> variableElems = element.getChildren(TAG_ANCILLARY_VARIABLE);
        final List<String> variableNames = new ArrayList<>();
        for (Element variableElem : variableElems) {
            variableNames.add(variableElem.getTextTrim());
        }
        for (String variableName : variableNames) {
            final RasterDataNode variable = product.getRasterDataNode(variableName);
            if (variable != null) {
                rasterDataNode.addAncillaryVariable(variable);
            } else {
                product.addProductNodeListener(new ProductNodeListenerAdapter(){
                    @Override
                    public void nodeAdded(ProductNodeEvent event) {
                        final ProductNode sourceNode = event.getSourceNode();
                        final String sourceNodeName = sourceNode.getName();
                        if (!variableName.equals(sourceNodeName)) return;
                        if (!(sourceNode instanceof RasterDataNode)) return;
                        product.removeProductNodeListener(this);
                        rasterDataNode.addAncillaryVariable((RasterDataNode) sourceNode);
                    }
                });
            }
        }
    }

    protected void setAncillaryRelations(Element element, RasterDataNode rasterDataNode) {
        final List<Element> relationElems = element.getChildren(TAG_ANCILLARY_RELATION);
        final TreeSet<String> relations = new TreeSet<>();
        for (Element relation : relationElems) {
            relations.add(relation.getTextTrim());
        }
        if (relations.size() > 0) {
            rasterDataNode.setAncillaryRelations(relations.toArray(new String[relations.size()]));
        }
    }

    protected void addAncillaryElements(Element root, RasterDataNode rasterDataNode) {
        String[] ancillaryRelations = rasterDataNode.getAncillaryRelations();
        for (String ancillaryRelation : ancillaryRelations) {
            root.addContent(createValueAttributeElement(TAG_ANCILLARY_RELATION, ancillaryRelation));
        }
        RasterDataNode[] ancillaryVariables = rasterDataNode.getAncillaryVariables();
        for (RasterDataNode ancillaryVariable : ancillaryVariables) {
            root.addContent(createValueAttributeElement(DimapProductConstants.TAG_ANCILLARY_VARIABLE, ancillaryVariable.getName()));
        }
    }

    protected void setImageToModelTransform(Element element, RasterDataNode rasterDataNode) {
        String matrix = element.getChildTextTrim(TAG_IMAGE_TO_MODEL_TRANSFORM); //Old format??
        if (matrix == null || matrix.length() == 0) {
            //Try new format
            Element child = element.getChild(TAG_IMAGE_TO_MODEL_TRANSFORM);
            if(child == null) {
                return;
            }
            Attribute attribute = child.getAttribute(ATTRIB_VALUE);
            if(attribute == null) {
                return;
            }
            matrix = attribute.getValue().trim();
        }
        if (matrix != null && matrix.length() > 0) {
            final AffineTransform transform = new AffineTransform(StringUtils.toDoubleArray(matrix, null));
            rasterDataNode.setImageToModelTransform(transform);
        }
    }

    protected void addImageToModelTransformElement(Element root, RasterDataNode rasterDataNode) {
        final AffineTransform imageToModelTransform = rasterDataNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            final String csvValue = StringUtils.arrayToCsv(matrix);
            root.addContent(createValueAttributeElement(TAG_IMAGE_TO_MODEL_TRANSFORM, csvValue));
        }
    }

    protected static Element createValueAttributeElement(String elementName, String value) {
        final Element elem = new Element(elementName);
        if (value != null) {
            elem.setAttribute(ATTRIB_VALUE, value);
        } else {
            elem.setAttribute(ATTRIB_VALUE, "");
        }
        return elem;
    }

    protected static Element createElement(String elementName, String value) {
        final Element elem = new Element(elementName);
        elem.setText(value);
        return elem;
    }
}

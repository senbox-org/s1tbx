package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.binding.PropertyContainer;
import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;

import java.awt.Color;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public abstract class MaskPersistable implements DimapPersistable {

    @Override
    public final Mask createObjectFromXml(Element element, Product product) {
        final String name = getChildAttributeValue(element, TAG_NAME, ATTRIB_VALUE);
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final Mask mask = new Mask(name, width, height, createImageType());
        mask.setDescription(getChildAttributeValue(element, TAG_DESCRIPTION, ATTRIB_VALUE));

        mask.setImageTransparency(Double.parseDouble(getChildAttributeValue(element, TAG_TRANSPARENCY, ATTRIB_VALUE)));
        final int r = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_RED));
        final int g = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_GREEN));
        final int b = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_BLUE));
        final int a = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_ALPHA));
        mask.setImageColor(new Color(r, g, b, a));

        configureMask(mask, element);
        return mask;
    }

    @Override
    public final Element createXmlFromObject(Object object) {
        final Mask mask = (Mask) object;
        final Element root = new Element(TAG_MASK);
        root.setAttribute(ATTRIB_TYPE, mask.getImageType().getName());

        final PropertyContainer config = mask.getImageConfig();
        root.addContent(createElement(TAG_NAME, mask.getName()));
        root.addContent(createElement(TAG_DESCRIPTION, mask.getDescription()));
        final Element colorElement = new Element(TAG_COLOR);
        final Color color = (Color) config.getValue(Mask.ImageType.PROPERTY_NAME_COLOR);
        colorElement.setAttribute(ATTRIB_RED, String.valueOf(color.getRed()));
        colorElement.setAttribute(ATTRIB_GREEN, String.valueOf(color.getGreen()));
        colorElement.setAttribute(ATTRIB_BLUE, String.valueOf(color.getBlue()));
        colorElement.setAttribute(ATTRIB_ALPHA, String.valueOf(color.getAlpha()));
        root.addContent(colorElement);
        final String transparency = String.valueOf(config.getValue(Mask.ImageType.PROPERTY_NAME_TRANSPARENCY));
        root.addContent(createElement(TAG_TRANSPARENCY, transparency));

        configureElement(root, mask);
        return root;
    }

    protected abstract Mask.ImageType createImageType();

    protected abstract void configureMask(Mask mask, Element element);

    protected abstract void configureElement(Element root, Mask mask);

    protected static Element createElement(String elementName, String value) {
        final Element elem = new Element(elementName);
        if (value != null) {
            elem.setAttribute(ATTRIB_VALUE, value);
        } else {
            elem.setAttribute(ATTRIB_VALUE, "");
        }
        return elem;
    }

    protected static String getChildAttributeValue(Element element, String childName, String attributeName) {
        return element.getChild(childName).getAttribute(attributeName).getValue();
    }
}

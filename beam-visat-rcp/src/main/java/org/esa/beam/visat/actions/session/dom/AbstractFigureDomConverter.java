package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.MapDomConverter;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.draw.AbstractFigure;
import org.esa.beam.framework.draw.AreaFigure;
import org.esa.beam.framework.draw.LineFigure;
import org.esa.beam.framework.draw.ShapeFigure;

import javax.swing.ImageIcon;
import java.awt.Shape;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class AbstractFigureDomConverter extends SessionElementDomConverter<AbstractFigure> {

    public AbstractFigureDomConverter() {
        super(AbstractFigure.class);
    }

    @Override
    public AbstractFigure convertDomToValue(DomElement parentElement, Object figure) throws ConversionException,
                                                                                            ValidationException {
        final String className = parentElement.getAttribute("class");
        if (LineFigure.class.getName().equals(className)) {
            final DomElement shapeChild = parentElement.getChild("shape");
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            final Object shapeObject = domConverter.convertDomToValue(shapeChild, null);

            final DomElement attrChild = parentElement.getChild("attributes");
            final Object mapObject = new MapDomConverter(domConverter).convertDomToValue(attrChild, null);
            return new LineFigure((Shape) shapeObject, (Map<String, Object>) mapObject);
        } else if (ShapeFigure.class.getName().equals(className)) {
            final DomElement shapeChild = parentElement.getChild("shape");
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            final Object shapeObject = domConverter.convertDomToValue(shapeChild, null);

            final DomElement dimensionalChild = parentElement.getChild("oneDimensional");
            final boolean oneDimensional = Boolean.valueOf(dimensionalChild.getValue());

            final DomElement attrChild = parentElement.getChild("attributes");
            final Object mapObject = new MapDomConverter(domConverter).convertDomToValue(attrChild, null);

            return new ShapeFigure((Shape) shapeObject, oneDimensional, (Map<String, Object>) mapObject);
        } else if (AreaFigure.class.getName().equals(className)) {
            final DomElement shapeChild = parentElement.getChild("shape");
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            final Object shapeObject = domConverter.convertDomToValue(shapeChild, null);

            final DomElement attrChild = parentElement.getChild("attributes");
            final Object mapObject = new MapDomConverter(domConverter).convertDomToValue(attrChild, null);
            return new AreaFigure((Shape) shapeObject, (Map<String, Object>) mapObject);
        } else if (PlacemarkSymbol.class.getName().equals(className)) {

            final DomElement nameChild = parentElement.getChild("name");
            final String name = nameChild.getValue();

            final DomElement shapeChild = parentElement.getChild("shape");
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            final Object shapeObject = domConverter.convertDomToValue(shapeChild, null);

            final DomElement attrChild = parentElement.getChild("attributes");
            final Object mapObject = new MapDomConverter(domConverter).convertDomToValue(attrChild, null);

            final PlacemarkSymbol symbol = new PlacemarkSymbol(name, (Shape) shapeObject);
            symbol.setAttributes((Map<String, Object>) mapObject);

            final DomElement iconLocationChild = parentElement.getChild("iconLocation");
            if (iconLocationChild != null) {
                iconLocationChild.getValue();
                try {
                    symbol.setIcon(new ImageIcon(new URL(iconLocationChild.getValue())));
                } catch (MalformedURLException e) {
                    throw new ConversionException(e);
                }
            }
            return symbol;
        }
        return null;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) {
        final Class<?> valueType = value.getClass();
        parentElement.setAttribute("class", valueType.getName());


        if (valueType == LineFigure.class || valueType == AreaFigure.class) {
            AbstractFigure figure = (AbstractFigure) value;
            final DomElement shapeChild = parentElement.createChild("shape");
            shapeChild.setAttribute("class", figure.getShape().getClass().getName());
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            domConverter.convertValueToDom(figure.getShape(), shapeChild);

            final DomElement attrChild = parentElement.createChild("attributes");

            new MapDomConverter(domConverter).convertValueToDom(figure.getAttributes(), attrChild);
        } else if (valueType == ShapeFigure.class) {
            ShapeFigure figure = (ShapeFigure) value;

            final DomElement shapeChild = parentElement.createChild("shape");
            shapeChild.setAttribute("class", figure.getShape().getClass().getName());
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            domConverter.convertValueToDom(figure.getShape(), shapeChild);

            final DomElement dimensionalChild = parentElement.createChild("oneDimensional");
            dimensionalChild.setValue(String.valueOf(figure.isOneDimensional()));

            final DomElement attrChild = parentElement.createChild("attributes");
            new MapDomConverter(domConverter).convertValueToDom(figure.getAttributes(), attrChild);
        } else if (valueType == PlacemarkSymbol.class) {
            PlacemarkSymbol figure = (PlacemarkSymbol) value;

            final DomElement nameChild = parentElement.createChild("name");
            nameChild.setValue(figure.getName());

            final DomElement shapeChild = parentElement.createChild("shape");
            shapeChild.setAttribute("class", figure.getShape().getClass().getName());
            final DefaultDomConverter domConverter = new DefaultDomConverter(Shape.class);
            domConverter.convertValueToDom(figure.getShape(), shapeChild);
            // remove it, because it's not convertibale by default
            final Object icon = figure.getAttributes().remove("ICON");
            final DomElement attrChild = parentElement.createChild("attributes");
            new MapDomConverter(domConverter).convertValueToDom(figure.getAttributes(), attrChild);

            if (icon != null) {
                final DomElement iconLocationChild = parentElement.createChild("iconLocation");
                try {
                    final Field field = ImageIcon.class.getDeclaredField("location");
                    field.setAccessible(true);
                    try {
                        final Object imageUrl = field.get(icon);
                        iconLocationChild.setValue(String.valueOf(imageUrl));
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
                    } finally {
                        field.setAccessible(false);
                    }
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

    }

}
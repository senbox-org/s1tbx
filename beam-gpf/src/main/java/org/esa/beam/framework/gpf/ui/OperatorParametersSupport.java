package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 * Support for operator parameters input/output.
 */
public class OperatorParametersSupport {
    private final Class<? extends Operator> opType;
    private final PropertySet parameters;
    private final ParameterDescriptorFactory descriptorFactory;

    public OperatorParametersSupport(Class<? extends Operator> opType, Map<String, Object> parameters) {
        this.opType = opType;
        this.descriptorFactory = new ParameterDescriptorFactory();
        this.parameters = PropertyContainer.createMapBacked(parameters, opType, descriptorFactory);
    }

    public PropertySet getParameters() {
        return parameters;
    }

    public Action createStoreParametersAction() {
        AbstractAction action = new AbstractAction("Store Parameters...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // todo
                JOptionPane.showMessageDialog(null, "Not implemented yet.\n(XML output has been dumped to stdout.)", "Store Parameters", JOptionPane.WARNING_MESSAGE);
                try {
                    System.out.println("Storing parameters...");
                    String s = toDomElement().toXml();
                    System.out.println(s);
                } catch (ValidationException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ConversionException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        };
        return action;
    }

    public Action createLoadParametersAction() {
        return new AbstractAction("Load Parameters...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // todo
                JOptionPane.showMessageDialog(null, "Not implemented yet.", "Load Parameters", JOptionPane.WARNING_MESSAGE);
            }
        };
    }

    void fromDomElement(DomElement parametersElement) throws ValidationException, ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        domConverter.convertDomToValue(parametersElement, parameters);
    }

    DomElement toDomElement() throws ValidationException, ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(opType, descriptorFactory);
        DefaultDomElement parametersElement = new DefaultDomElement("parameters");
        domConverter.convertValueToDom(parameters, parametersElement);
        return parametersElement;
    }
}

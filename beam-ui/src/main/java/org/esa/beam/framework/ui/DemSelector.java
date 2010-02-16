package org.esa.beam.framework.ui;

import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.RadioButtonEditor;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 11.03.2005
 * Time: 13:39:11
 */
public class DemSelector extends JPanel {

    private Parameter _paramProductDem;
    private Parameter _paramExternalDem;
    private Parameter _paramDem;

    public DemSelector() {
        this(null);
    }

    public DemSelector(ParamChangeListener paramChangeListener) {
        createParameter(paramChangeListener);
        createUI();
        updateUIState();
    }

    public boolean isUsingProductDem() {
        return (Boolean) _paramProductDem.getValue();
    }

    public void setUsingProductDem(final boolean usingProductDem) throws ParamValidateException {
        _paramProductDem.setValue(usingProductDem);
    }

    public boolean isUsingExternalDem() {
        return (Boolean) _paramExternalDem.getValue();
    }

    public void setUsingExternalDem(final boolean usingExternalDem) throws ParamValidateException {
        _paramExternalDem.setValue(usingExternalDem);
    }

    public String getDemName() {
        return _paramDem.getValueAsText();
    }

    private void createUI() {
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Digital Elevation Model (DEM)"));            /*I18N*/

        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add((AbstractButton) _paramProductDem.getEditor().getComponent());
        buttonGroup.add((AbstractButton) _paramExternalDem.getEditor().getComponent());

        GridBagUtils.setAttributes(gbc, "insets.top=3, fill=HORIZONTAL");
        gbc.gridy++;
        GridBagUtils.addToPanel(this, _paramProductDem.getEditor().getComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.addToPanel(this, _paramExternalDem.getEditor().getComponent(), gbc, "weightx=1");
        GridBagUtils.addToPanel(this, _paramDem.getEditor().getComponent(), gbc, "weightx=999");
    }

    private void updateUIState() {
        _paramDem.setUIEnabled(isUsingExternalDem());
    }

    private void createParameter(final ParamChangeListener delegate) {
        ParamChangeListener paramChangeListener = new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
                if (delegate != null) {
                    delegate.parameterValueChanged(event);
                }
            }
        };

        _paramProductDem = new Parameter("useProductDem", Boolean.FALSE);
        _paramProductDem.getProperties().setLabel("Use elevation from tie-points");        /*I18N*/
        _paramProductDem.getProperties().setEditorClass(RadioButtonEditor.class);
        _paramProductDem.addParamChangeListener(paramChangeListener);

        _paramExternalDem = new Parameter("useExternalDem", Boolean.TRUE);
        _paramExternalDem.getProperties().setLabel("Use external DEM");      /*I18N*/
        _paramExternalDem.getProperties().setEditorClass(RadioButtonEditor.class);
        _paramExternalDem.addParamChangeListener(paramChangeListener);

        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        final String[] demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        _paramDem = new Parameter("dem", "");
        _paramDem.getProperties().setLabel("Elevation Model:");     /*I18N*/
        _paramDem.getProperties().setValueSetBound(true);
        if (demValueSet.length != 0) {
            _paramDem.setValue(demValueSet[0], null);
            _paramDem.getProperties().setValueSet(demValueSet);
        }
        _paramDem.addParamChangeListener(paramChangeListener);
    }

}

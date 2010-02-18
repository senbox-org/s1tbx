package org.esa.beam.framework.ui;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 15.03.2005
 * Time: 08:03:53
 */

import junit.framework.TestCase;

import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamValidateException;

/**
 * Description of DemSelectorTest
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class DemSelectorTest extends TestCase {

    private DemSelector _demSelector;
    private MyParamChangeListener _paramChangeListener;

    @Override
    protected void setUp() throws Exception {
        _paramChangeListener = new MyParamChangeListener();
        _demSelector = new DemSelector(_paramChangeListener);

    }

    public void test_UsingExternalDEM_IsDefault() {
        assertTrue(_demSelector.isUsingExternalDem());
    }

    public void test_SetUsingExternalDem() throws ParamValidateException {
        _demSelector.setUsingExternalDem(true);

        assertTrue(_demSelector.isUsingExternalDem());
        assertFalse(_demSelector.isUsingProductDem());
        assertEquals("", _paramChangeListener.getCallString());
    }

    public void test_SetUsingProductDem() throws ParamValidateException {
        _demSelector.setUsingProductDem(true);

        assertTrue(_demSelector.isUsingProductDem());
        assertFalse(_demSelector.isUsingExternalDem());

        assertEquals("useProductDem|useExternalDem|", _paramChangeListener.getCallString());
    }

    public void test_ToggleUsingDem() throws ParamValidateException {
        _demSelector.setUsingProductDem(true);
        _demSelector.setUsingExternalDem(true);

        assertTrue(_demSelector.isUsingExternalDem());
        assertFalse(_demSelector.isUsingProductDem());
        assertEquals("useProductDem|useExternalDem|useExternalDem|useProductDem|", _paramChangeListener.getCallString());
    }

    private static class MyParamChangeListener implements ParamChangeListener {
    StringBuilder callString = new  StringBuilder();

        public String getCallString() {
            return callString.toString();
        }

        public void parameterValueChanged(ParamChangeEvent event) {
            callString.append(event.getParameter().getName());
            callString.append("|")  ;
        }
    }
}
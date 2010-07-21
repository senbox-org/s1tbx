/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui.config;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.PropertyMap;

/**
 * A convinience implementation of the <code>ConfigPage</code> interface.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public class DefaultConfigPage implements ConfigPage {

    private static int _lastKey;

    private final String _key;
    private final ParamGroup _configParams;
    private Icon _icon;
    private String _title;
    private boolean _modified;
    private Component _pageUI;
    private List<ConfigPage> _subPageList;

    public DefaultConfigPage() {
        _key = getClass().getName().concat(String.valueOf(_lastKey++));
        _configParams = new ParamGroup();
        initConfigParams(_configParams);
        initPageUI();
    }

    protected void initConfigParams(ParamGroup configParams) {
    }

    protected void initPageUI() {
    }

    @Override
    public ParamGroup getConfigParams() {
        return _configParams;
    }

    @Override
    public PropertyMap getConfigParamValues(PropertyMap propertyMap) {
        return getConfigParams().getParameterValues(propertyMap);
    }

    @Override
    public void setConfigParamValues(PropertyMap propertyMap, ParamExceptionHandler errorHandler) {
        getConfigParams().setParameterValues(propertyMap, errorHandler);
    }

    public Parameter getConfigParam(String paramName) {
        return getConfigParams().getParameter(paramName);
    }

    public boolean isConfigParamUIEnabled(String paramName) {
        return getConfigParam(paramName).isUIEnabled();
    }

    public void setConfigParamUIEnabled(String paramName, boolean enabled) {
        getConfigParam(paramName).setUIEnabled(enabled);
    }

    @Override
    public String getKey() {
        return _key;
    }

    @Override
    public Icon getIcon() {
        return _icon;
    }

    public void setIcon(Icon icon) {
        _icon = icon;
    }

    @Override
    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    @Override
    public boolean isModified() {
        return _modified;
    }

    public void setModified(boolean modified) {
        _modified = modified;
    }

    @Override
    public Component getPageUI() {
        return _pageUI;
    }

    public void setPageUI(Component pageUI) {
        _pageUI = pageUI;
    }

    public void addSubPage(ConfigPage subPage) {
        if (_subPageList == null) {
            _subPageList = new ArrayList<ConfigPage>();
        }
        _subPageList.add(subPage);
    }

    public void removeSubPage(ConfigPage subPage) {
        if (_subPageList == null) {
            return;
        }
        _subPageList.remove(subPage);
    }

    @Override
    public ConfigPage[] getSubPages() {
        if (_subPageList == null) {
            return null;
        }
        final ConfigPage[] subPages = new ConfigPage[_subPageList.size()];
        _subPageList.toArray(subPages);
        return subPages;
    }

    @Override
    public void applyPage() {
    }

    @Override
    public void restorePage() {
    }

    @Override
    public void onOK() {
    }

    @Override
    public void updatePageUI() {
    }

    @Override
    public boolean verifyUserInput() {
        return true;
    }
}

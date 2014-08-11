/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import org.esa.beam.framework.ui.AppContext;
import org.esa.snap.dat.dialogs.FileTable;
import org.esa.snap.dat.dialogs.ProductSetPanel;
import org.esa.snap.gpf.ui.BaseOperatorUI;
import org.esa.snap.gpf.ui.UIValidation;

import javax.swing.*;
import java.io.File;
import java.util.Map;

/**
 * Stack Reader Operator User Interface
 * User: lveci
 * Date: Feb 12, 2008
 */
public class ProductSetReaderOpUI extends BaseOperatorUI {

    private final FileTable productSetTable = new FileTable();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        ProductSetPanel panel = new ProductSetPanel(appContext, "", productSetTable, false, true);
        initParameters();
        return panel;
    }

    @Override
    public void initParameters() {
        final String[] fList = (String[]) paramMap.get("fileList");
        productSetTable.setFiles(fList);
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        final File[] fileList = productSetTable.getFileList();
        if (fileList.length == 0) return;

        final String[] fList = new String[fileList.length];
        for (int i = 0; i < fileList.length; ++i) {
            if (fileList[i].getName().isEmpty())
                fList[i] = "";
            else
                fList[i] = fileList[i].getAbsolutePath();
        }
        paramMap.put("fileList", fList);
    }

    public void setProductFileList(final File[] productFileList) {
        productSetTable.setFiles(productFileList);
    }
}
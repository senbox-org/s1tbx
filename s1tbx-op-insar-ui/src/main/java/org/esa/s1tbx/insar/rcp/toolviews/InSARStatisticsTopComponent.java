/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.toolviews;

import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.InSARStatistic;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatBaselines;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatBaselinesChart;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatESDHistogram;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatESDMeasure;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatInSARInfo;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatResiduals;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.tango.TangoIcons;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.AUTO;

/**
 * Displays InSAR Statistics
 */
@TopComponent.Description(
        preferredID = "InSARStatisticsTopComponent",
        iconBase = "org/esa/s1tbx/insar/icons/stack.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false, position = 200)
@ActionID(category = "Window", id = "org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows/Radar"),
        @ActionReference(path = "Toolbars/Radar Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_InSARStatisticsTopComponentName",
        preferredID = "InSARStatisticsTopComponent"
)
@NbBundle.Messages({
        "CTL_InSARStatisticsTopComponentName=InSAR Stack",
        "CTL_InSARStatisticsTopComponentDescription=InSAR related information on a stack",
})
public class InSARStatisticsTopComponent extends TopComponent {

    public static final String EmptyMsg = "This tool window requires a coregistered stack product to be selected";

    private final List<InSARStatistic> statisticList = new ArrayList<>();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private ProductNode oldNode = null;

    private static final ImageIcon copyIcon = TangoIcons.actions_edit_copy(TangoIcons.Res.R22);
    private static final ImageIcon saveIcon = TangoIcons.actions_document_save_as(TangoIcons.Res.R22);
    private static final ImageIcon helpIcon = TangoIcons.apps_help_browser(TangoIcons.Res.R22);

    public InSARStatisticsTopComponent() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_InSARStatisticsTopComponentName());
        setToolTipText(Bundle.CTL_InSARStatisticsTopComponentDescription());
        add(createPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());
        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if (newValue != null && newValue != oldNode) {
                    final Product product = newValue.getProduct();
                    for (InSARStatistic statistic : statisticList) {
                        statistic.update(product);
                    }
                    oldNode = newValue;
                }
            }
        });

        Product product = snapApp.getSelectedProduct(AUTO);
        if(InSARStatistic.isValidProduct(product)) {
            for (InSARStatistic statistic : statisticList) {
                statistic.update(product);
            }
        }
    }

    public JTabbedPane createPanel() {

        statisticList.add(new StatInSARInfo(this));
        statisticList.add(new StatResiduals(this));
        statisticList.add(new StatESDMeasure(this));
        statisticList.add(new StatESDHistogram(this));
        statisticList.add(new StatBaselines(this));
        statisticList.add(new StatBaselinesChart(this));

        for (InSARStatistic statistic : statisticList) {
            tabbedPane.add(statistic.getName(), statistic.createPanel());
        }
        tabbedPane.setSelectedIndex(0);

        return tabbedPane;
    }

    private JPanel createButtonPanel() {

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        final JButton copyBtn = DialogUtils.createButton("copyBtn", "Copy", copyIcon, buttonPanel, DialogUtils.ButtonStyle.Icon);
        copyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InSARStatistic stat = statisticList.get(tabbedPane.getSelectedIndex());
                stat.copyToClipboard();
            }
        });
        final JButton saveBtn = DialogUtils.createButton("saveBtn", "Save", saveIcon, buttonPanel, DialogUtils.ButtonStyle.Icon);
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InSARStatistic stat = statisticList.get(tabbedPane.getSelectedIndex());
                stat.saveToFile();
            }
        });
        final JButton helpBtn = DialogUtils.createButton("helpBtn", "Help", helpIcon, buttonPanel, DialogUtils.ButtonStyle.Icon);
        helpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InSARStatistic stat = statisticList.get(tabbedPane.getSelectedIndex());

                new HelpCtx(stat.getHelpId()).display();
            }
        });

        buttonPanel.add(Box.createRigidArea(new Dimension(10,25)));
        buttonPanel.add(copyBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(helpBtn);

        return buttonPanel;
    }

    public List<InSARStatistic> getStatisticComponents() {
        return statisticList;
    }

    public class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();
            for (InSARStatistic statistic : statisticList) {
                statistic.update(product);
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            for (InSARStatistic statistic : statisticList) {
                statistic.update(null);
            }
        }
    }
}

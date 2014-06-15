package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * shows products on a time line
 */
public class TimelinePane extends WebPanel {

    private final DatabaseStatistics stats;
    private JPanel timelinePanel;
    private JPanel yearsPanel;
    private JPanel monthsPanel;

    public TimelinePane(final DatabaseStatistics stats) {
        this.stats = stats;
        createPanel();
        setMaximumSize(new Dimension(500, 30));
    }

    private void createPanel() {
        setLayout(new BorderLayout());
        setUndecorated(false);

        final JPanel centrePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        timelinePanel = new TimelinePanel(stats);
        yearsPanel = new YearsPanel(stats);
        monthsPanel = new MonthsPanel(stats);
        gbc.weightx = 10;
        gbc.weighty = 10;
        centrePanel.add(timelinePanel, gbc);
        centrePanel.add(yearsPanel, gbc);
        centrePanel.add(monthsPanel, gbc);
        timelinePanel.setVisible(false);
        monthsPanel.setVisible(false);

        this.add(centrePanel, BorderLayout.CENTER);
        this.add(createControlPanel(), BorderLayout.WEST);
    }

    private JPanel createControlPanel() {
        final WebPanel controlPanel = new WebPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
        final JRadioButton timelineButton = new JRadioButton("Timeline", false);
        final JRadioButton yearsButton = new JRadioButton("Years", true);
        final JRadioButton monthsButton = new JRadioButton("Months", false);

        final ButtonGroup group = new ButtonGroup();
        group.add(timelineButton);
        group.add(yearsButton);
        group.add(monthsButton);

        timelineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timelinePanel.setVisible(true);
                yearsPanel.setVisible(false);
                monthsPanel.setVisible(false);
            }
        });

        yearsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timelinePanel.setVisible(false);
                yearsPanel.setVisible(true);
                monthsPanel.setVisible(false);
            }
        });

        monthsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timelinePanel.setVisible(false);
                yearsPanel.setVisible(false);
                monthsPanel.setVisible(true);
            }
        });

        controlPanel.add(timelineButton);
        controlPanel.add(yearsButton);
        controlPanel.add(monthsButton);

        return controlPanel;
    }
}

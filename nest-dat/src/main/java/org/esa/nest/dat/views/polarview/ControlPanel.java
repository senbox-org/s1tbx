package org.esa.nest.dat.views.polarview;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TimerTask;

/**

 */
class ControlPanel extends JPanel {

    private final PolarView polarView;
    private final JButton prevBtn = new JButton("Prev");
    private final JButton nextBtn = new JButton("Next");
    private final JLabel recordLabel = new JLabel();
    private final JButton zoomInBtn = new JButton("Zoom In");
    private final JButton zoomOutBtn = new JButton("Zoom Out");

    private boolean animate = false;
    private final JToggleButton animateBtn = new JToggleButton("Animate", animate);

    private JSlider recordSlider = null;

    ControlPanel(PolarView theView) {
        this.polarView = theView;

        createPanel();
        startUpdateTimer();
    }

    private void createPanel() {

        add(recordLabel);

        add(prevBtn);
        prevBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                polarView.showPreviousPlot();
            }
        });

        add(nextBtn);
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                polarView.showNextPlot();
            }
        });

        recordSlider = new JSlider(0, polarView.getNumRecords(), 0);
        recordSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                polarView.showPlot(recordSlider.getValue());
            }
        });
        add(recordSlider);

        add(animateBtn);
        animateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animate = !animate;
                updateControls();
            }
        });

        /*add(zoomInBtn);
        zoomInBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                polarView.zoomIn();
            }
        });

        add(zoomOutBtn);
        zoomOutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                polarView.zoomOut();
            }
        });   */
    }

    public void updateControls() {
        final int currentRecord = polarView.getCurrentRecord();
        final int numRecords = polarView.getNumRecords();

        prevBtn.setEnabled(currentRecord > 0 && !animate);
        nextBtn.setEnabled(currentRecord < numRecords && !animate);
        recordSlider.setValue(currentRecord);

        recordLabel.setText("Record "+ (currentRecord+1) + " of " + (numRecords+1));
    }

    private void startUpdateTimer() {

        final java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(animate) {
                    if(polarView.getCurrentRecord() >= polarView.getNumRecords())
                        polarView.showPlot(0);
                    else
                        polarView.showNextPlot();
                }
            }
        }, 0, 1000);
    }
}

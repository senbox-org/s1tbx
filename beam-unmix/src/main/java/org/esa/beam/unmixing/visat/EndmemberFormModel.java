package org.esa.beam.unmixing.visat;

import org.esa.beam.framework.ui.diagram.*;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeSupport;

class EndmemberFormModel {

    private DefaultListModel endmemberListModel;
    private DefaultListSelectionModel endmemberListSelectionModel;
    private int selectedEndmemberIndex;

    private Diagram endmemberDiagram;

    private Action testAction = new TestAction();
    private Action addAction = new AddAction();
    private Action removeAction = new RemoveAction();
    private Action clearAction = new ClearAction();
    private Action exportAction = new ExportAction();

    private PropertyMap preferences;

    private PropertyChangeSupport propertyChangeSupport;

    private Color[] defaultColors = new Color[]{Color.BLACK, Color.RED.darker(), Color.GREEN.darker(), Color.BLUE.darker(), Color.YELLOW};

    public EndmemberFormModel() {
        endmemberListModel = new DefaultListModel();
        endmemberListSelectionModel = new DefaultListSelectionModel();
        endmemberListSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        endmemberListModel.addListDataListener(new EndmemberListDataListener());
        endmemberListSelectionModel.addListSelectionListener(new EndmemberListSelectionListener());
        endmemberDiagram = new Diagram();
        endmemberDiagram.setXAxis(new DiagramAxis("Wavelength", ""));
        endmemberDiagram.setYAxis(new DiagramAxis("Radiation", ""));
        endmemberDiagram.setDrawGrid(false);
        propertyChangeSupport = new PropertyChangeSupport(this);
        VisatApp app = VisatApp.getApp();
        preferences = app != null ? app.getPreferences() : new PropertyMap();
    }

    public Endmember[] getEndmembers() {
        Endmember[] endmembers = new Endmember[endmemberListModel.getSize()];
        for (int i = 0; i < endmembers.length; i++) {
            endmembers[i] = (Endmember) endmemberListModel.getElementAt(i);
        }
        return endmembers;
    }

    public ListModel getEndmemberListModel() {
        return endmemberListModel;
    }

    public DefaultListSelectionModel getEndmemberListSelectionModel() {
        return endmemberListSelectionModel;
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public int getSelectedEndmemberIndex() {
        return selectedEndmemberIndex;
    }

    public Action getTestAction() {
        return testAction;
    }

    public Action getAddAction() {
        return addAction;
    }

    public Action getClearAction() {
        return clearAction;
    }

    public Action getExportAction() {
        return exportAction;
    }

    public Action getRemoveAction() {
        return removeAction;
    }

    public Diagram getEndmemberDiagram() {
        return endmemberDiagram;
    }

    private void addEndmember(Endmember endmember) {
        endmemberListModel.addElement(endmember);
        EndmemberGraph endmemberGraph = new EndmemberGraph(endmember);
        Color color = defaultColors[endmemberListModel.getSize() % defaultColors.length];
        DefaultDiagramGraphStyle style = ((DefaultDiagramGraphStyle) endmemberGraph.getStyle());
        style.setOutlineColor(color);
        style.setOutlineStroke(new BasicStroke(1.0f));
        style.setShowingPoints(false);
        endmemberDiagram.addGraph(endmemberGraph);
        endmemberDiagram.adjustAxes(true);
    }

    public void setSelectedEndmemberIndex(int index) {
        int oldIndex = selectedEndmemberIndex;
        if (oldIndex == index) {
            return;
        }
        if (oldIndex >= 0) {
            final DiagramGraph endmemberGraph = endmemberDiagram.getGraph(oldIndex);
            ((DefaultDiagramGraphStyle) endmemberGraph.getStyle()).setOutlineStroke(new BasicStroke(1.0f));
        }
        selectedEndmemberIndex = index;
        if (selectedEndmemberIndex >= 0) {
            final DiagramGraph endmemberGraph = endmemberDiagram.getGraph(selectedEndmemberIndex);
            ((DefaultDiagramGraphStyle) endmemberGraph.getStyle()).setOutlineStroke(new BasicStroke(2.0f));
        }
        endmemberDiagram.invalidate();
        propertyChangeSupport.firePropertyChange("selectedEndmemberIndex", oldIndex, selectedEndmemberIndex);
    }

    private class TestAction extends AbstractAction {

        public TestAction() {
            super("Test");
        }

        public void actionPerformed(ActionEvent e) {
            Endmember[] endmembers = loadTestEndmembers();
            for (Endmember endmember : endmembers) {
                addEndmember(endmember);
            }
        }
    }

    private class AddAction extends AbstractAction {

        public AddAction() {
            super("Add");
        }

        public void actionPerformed(ActionEvent e) {
            DiagramGraph[] diagramGraphs = DiagramGraphIO.readGraphs(null,
                                                                     "Import Endmembers",
                                                                     new BeamFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                                                     preferences);
            Endmember[] endmembers = SpectralUnmixingOp.convertGraphsToEndmembers(diagramGraphs);
            for (Endmember endmember : endmembers) {
                addEndmember(endmember);
            }
        }

    }

    private class RemoveAction extends AbstractAction {
        public RemoveAction() {
            super("Remove");
        }

        public void actionPerformed(ActionEvent e) {
            int index = selectedEndmemberIndex;
            if (index >= 0) {
                setSelectedEndmemberIndex(-1);
                endmemberListModel.removeElementAt(index);
                endmemberDiagram.removeGraph(endmemberDiagram.getGraph(index));
                endmemberDiagram.adjustAxes(true);
            }
        }
    }

    private class ClearAction extends AbstractAction {
        public ClearAction() {
            super("Clear");
        }

        public void actionPerformed(ActionEvent e) {
            setSelectedEndmemberIndex(-1);
            endmemberListModel.removeAllElements();
            endmemberDiagram.removeAllGraphs();
        }
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super("Export");
        }

        public void actionPerformed(ActionEvent e) {
            DiagramGraphIO.writeGraphs(null,
                                       "Export Endmembers",
                                       new BeamFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                       preferences,
                                       endmemberDiagram.getGraphs());
        }
    }

    private class EndmemberListDataListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            endmemberDiagram.invalidate();
        }

        public void intervalRemoved(ListDataEvent e) {
            endmemberDiagram.invalidate();
        }

        public void contentsChanged(ListDataEvent e) {
            endmemberDiagram.invalidate();
        }
    }

    private class EndmemberListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                if (endmemberListSelectionModel.isSelectionEmpty()) {
                    setSelectedEndmemberIndex(-1);
                } else {
                    setSelectedEndmemberIndex(endmemberListSelectionModel.getLeadSelectionIndex());
                }
            }
        }
    }


    private static Endmember[] loadTestEndmembers() {
        double[] wavelengths = new double[]{
                412.691,
                442.559,
                489.882,
                509.819,
                559.694,
                619.601,
                664.573,
                680.821,
                708.329,
                753.371,
                761.508,
                778.409,
                864.876,
                884.94,
                900.0
        };


        return new Endmember[]{
                new Endmember("Forrest", wavelengths, new double[]{
                        53.62759,
                        47.280907,
                        35.135273,
                        31.699518,
                        27.073746,
                        17.776417,
                        13.956376,
                        13.050324,
                        24.333706,
                        63.70239,
                        20.052866,
                        63.483547,
                        56.8588,
                        55.244736,
                        37.145023
                }),
                new Endmember("Cropland", wavelengths, new double[]{
                        66.19122,
                        66.45199,
                        64.50155,
                        64.30902,
                        64.61501,
                        67.19174,
                        71.617165,
                        72.050835,
                        72.08001,
                        78.129005,
                        24.409485,
                        76.208595,
                        70.52879,
                        68.819405,
                        44.563168
                }),

                new Endmember("Cloud", wavelengths, new double[]{
                        76.46195,
                        73.67296,
                        66.50564,
                        64.30902,
                        61.191143,
                        57.071224,
                        57.9758,
                        58.022255,
                        65.75211,
                        94.739105,
                        29.05891,
                        94.22999,
                        87.74958,
                        85.66566,
                        51.633755
                }),
                new Endmember("Ocean", wavelengths, new double[]{
                        70.6823,
                        61.99853,
                        46.74277,
                        41.141926,
                        28.379854,
                        18.218576,
                        14.559007,
                        13.293153,
                        11.375077,
                        9.123859,
                        3.5935445,
                        7.9517903,
                        5.0613823,
                        4.6388245,
                        3.5407245
                })
        };
    }
}

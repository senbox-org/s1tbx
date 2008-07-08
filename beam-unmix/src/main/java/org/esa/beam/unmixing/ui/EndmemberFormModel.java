package org.esa.beam.unmixing.ui;

import org.esa.beam.framework.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.diagram.DiagramGraphIO;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

class EndmemberFormModel {

    private DefaultListModel endmemberListModel;
    private DefaultListSelectionModel endmemberListSelectionModel;
    private int selectedEndmemberIndex;

    private Diagram endmemberDiagram;

    private Action addAction = new AddAction();
    private Action removeAction = new RemoveAction();
    private Action clearAction = new ClearAction();
    private Action exportAction = new ExportAction();

    private AppContext appContext;

    private PropertyChangeSupport propertyChangeSupport;

    private Color[] defaultColors = new Color[]{Color.BLACK, Color.RED.darker(), Color.GREEN.darker(), Color.BLUE.darker(), Color.YELLOW};
    private static File defaultEndmemberDir = new File(SystemUtils.getApplicationDataDir(), "beam-unmix/auxdata");

    public EndmemberFormModel(AppContext appContext) {
        this.appContext = appContext;
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

    public Action getAddAction() {
        return addAction;
    }

    public Action getRemoveAction() {
        return removeAction;
    }

    public Action getClearAction() {
        return clearAction;
    }

    public Action getExportAction() {
        return exportAction;
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
        if (oldIndex >= 0 && endmemberDiagram.getGraphCount() > 0) {
            final DiagramGraph endmemberGraph = endmemberDiagram.getGraph(oldIndex);
            ((DefaultDiagramGraphStyle) endmemberGraph.getStyle()).setOutlineStroke(new BasicStroke(1.0f));
        }
        selectedEndmemberIndex = index;
        if (selectedEndmemberIndex >= 0  && endmemberDiagram.getGraphCount() > 0) {
            final DiagramGraph endmemberGraph = endmemberDiagram.getGraph(selectedEndmemberIndex);
            ((DefaultDiagramGraphStyle) endmemberGraph.getStyle()).setOutlineStroke(new BasicStroke(2.0f));
        }
        endmemberDiagram.invalidate();
        propertyChangeSupport.firePropertyChange("selectedEndmemberIndex", oldIndex, selectedEndmemberIndex);
    }

    private void ensureDefaultDirSet() {
        if (!defaultEndmemberDir.exists()) {
            final ResourceInstaller resourceInstaller = new ResourceInstaller(ResourceInstaller.getSourceUrl(SpectralUnmixingDialog.class),
                                                                              "auxdata/", defaultEndmemberDir);
            try {
                resourceInstaller.install(".*", com.bc.ceres.core.ProgressMonitor.NULL);
            } catch (IOException e) {
                // failed, so what
            }
        }

        final String key = DiagramGraphIO.DIAGRAM_GRAPH_IO_LAST_DIR_KEY;
        final PropertyMap preferences = appContext.getPreferences();
        if (preferences.getPropertyString(key, null) == null) {
            preferences.setPropertyString(key, defaultEndmemberDir.getPath());
        }
    }

    private class AddAction extends AbstractAction {

        public AddAction() {
            super("Add");
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("list-add.png")));
            putValue(SHORT_DESCRIPTION, "Add Endmembers");
        }

        public void actionPerformed(ActionEvent e) {
            ensureDefaultDirSet();
            DiagramGraph[] diagramGraphs = DiagramGraphIO.readGraphs(null,
                                                                     "Add Endmembers",
                                                                     new BeamFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                                                     appContext.getPreferences());
            Endmember[] endmembers = SpectralUnmixingOp.convertGraphsToEndmembers(diagramGraphs);
            for (Endmember endmember : endmembers) {
                addEndmember(endmember);
            }
        }

    }

    private class RemoveAction extends AbstractAction {
        public RemoveAction() {
            super("Remove");
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("list-remove.png")));
            putValue(SHORT_DESCRIPTION, "Remove Endmember");
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
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("edit-clear.png")));
            putValue(SHORT_DESCRIPTION, "Clear List");
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
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("document-save-as.png")));
            putValue(SHORT_DESCRIPTION, "Export Endmembers");
        }

        public void actionPerformed(ActionEvent e) {
            ensureDefaultDirSet();
            DiagramGraphIO.writeGraphs(null,
                                       "Export Endmembers",
                                       new BeamFileFilter[]{DiagramGraphIO.SPECTRA_CSV_FILE_FILTER},
                                       appContext.getPreferences(),
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
}

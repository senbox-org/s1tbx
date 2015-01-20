package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.SystemUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoadSaveRasterDataNodesConfigurationsProvider {

    private final LoadSaveRasterDataNodesConfigurationsComponent component;
    private AbstractButton loadButton;
    private AbstractButton saveButton;

    public LoadSaveRasterDataNodesConfigurationsProvider(LoadSaveRasterDataNodesConfigurationsComponent component) {
        this.component = component;
    }

    public AbstractButton getLoadButton() {
        if(loadButton == null) {
            loadButton = createButton("/com/bc/ceres/swing/actions/icons_22x22/document-open.png");
            loadButton.setToolTipText("Load configuration");
            loadButton.addActionListener(new LoadConfigurationActionListener());
        }
        return loadButton;
    }

    public AbstractButton getSaveButton() {
        if(saveButton == null) {
            saveButton = createButton("/com/bc/ceres/swing/actions/icons_22x22/document-save.png");
            saveButton.setToolTipText("Save configuration");
            saveButton.addActionListener(new SaveConfigurationActionListener());
        }
        return saveButton;
    }

    private static AbstractButton createButton(String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getSystemAuxDataDir() {
        File file = new File(SystemUtils.getApplicationDataDir(), "snap-ui" + File.separator + "auxdata" +
                File.separator + "band-sets");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    private class LoadConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getSystemAuxDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            if (fileChooser.showOpenDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    List<String> bandNameList = new ArrayList<>();
                    String readBandName;
                    while ((readBandName = reader.readLine()) != null) {
                        bandNameList.add(readBandName);
                    }
                    reader.close();
                    String[] bandNames = bandNameList.toArray(new String[bandNameList.size()]);
                    component.setReadRasterDataNodeNames(bandNames);
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(component.getParent(), "Could not load configuration");
                }
            }
        }
    }

    private class SaveConfigurationActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getSystemAuxDataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            File suggestedFile = new File(currentDirectory + File.separator + "config.txt");
            int fileCounter = 1;
            while (suggestedFile.exists()) {
                suggestedFile = new File("config" + fileCounter + ".txt");
            }
            fileChooser.setSelectedFile(suggestedFile);
            if (fileChooser.showSaveDialog(component.getParent()) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    String[] bandNames = component.getRasterDataNodeNamesToWrite();
                    for (String bandName : bandNames) {
                        writer.write(bandName + "\n");
                    }
                    writer.close();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(component.getParent(), "Could not save configuration");
                }
            }
        }
    }

}

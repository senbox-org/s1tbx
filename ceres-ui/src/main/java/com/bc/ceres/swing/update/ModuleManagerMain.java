package com.bc.ceres.swing.update;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeContext;
import com.bc.ceres.core.runtime.RuntimeRunnable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

public class ModuleManagerMain implements RuntimeRunnable {

    /**
     * Executes client code.
     * <p/>
     * If this RuntimeRunnable is an application launched by the Ceres runtime,
     * the <code>argument</code> parameter can safely be casted to a <code>String[]</code>.
     * This array contains all command-line arguments passed to the application.
     *
     * @param argument the argument passed to the RuntimeRunnable, which may be null.
     * @param pm       a progress monitor which may be used by the client code
     *
     * @throws Exception if any error occurs in the client code
     */
    public void run(Object argument, ProgressMonitor pm) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }
        final JFrame mainFrame = new JFrame("Module Manager Test");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setPreferredSize(new Dimension(200, 50));
        JButton managerButton = new JButton("Open Module Manager...");
        managerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultModuleManager moduleManager = new DefaultModuleManager();
                moduleManager.setRepositoryUrl(getDefaultRepositoryUrl());
                final ModuleManagerPane moduleManagerPane = new ModuleManagerPane(moduleManager);
                moduleManagerPane.showDialog(mainFrame, "Module Manager", ModuleManagerPane.NO_DONE_HANDLER, null);
            }
        });
        buttonPanel.add(managerButton, BorderLayout.CENTER);
        mainFrame.add(buttonPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    private static URL getDefaultRepositoryUrl() {
        RuntimeConfig runtimeConfig = RuntimeContext.getModuleContext().getRuntimeConfig();
        try {
            return new URL(runtimeConfig.getContextProperty("repository.url", ""));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}

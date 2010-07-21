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

package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.swing.SwingHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConnectionConfigPane extends JPanel {

    private ConnectionConfigData connectionConfigData;
    private JTextField repositoryUrl;
    private JCheckBox proxyUsed;
    private JTextField proxyPort;
    private JTextField proxyHost;
    private JCheckBox proxyAuthUsed;
    private JTextField username;
    private JPasswordField password;
    private boolean confirmed;
    private ValidtationHandler validationHandler;

    public ConnectionConfigPane(ConnectionConfigData connectionConfigData) {
        initUI();
        setConfigData(connectionConfigData);
    }

    public ValidtationHandler getValidationHandler() {
        return validationHandler;
    }

    public void setValidationHandler(ValidtationHandler validationHandler) {
        this.validationHandler = validationHandler;
    }

    public ConnectionConfigData getConfigData() {
        return connectionConfigData;
    }

    public void setConfigData(ConnectionConfigData connectionConfigData) {
        this.connectionConfigData = connectionConfigData;
        transferConfigDataToUi();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean validateUiState() {
        if (proxyUsed.isSelected()) {
            String ph = proxyHost.getText().trim();
            if (ph.length() == 0) {
                onValidationError("Invalid proxy host.");
                proxyHost.requestFocusInWindow();
                return false;
            }
            String pp = proxyPort.getText().trim();
            if (pp.length() == 0) {
                onValidationError("Invalid proxy port.");
                proxyPort.requestFocusInWindow();
                return false;
            }
            try {
                int port = Integer.parseInt(pp);
                if (port <= 0) {
                    onValidationError("Invalid proxy port.");
                    proxyPort.requestFocusInWindow();
                    return false;
                }
            } catch (NumberFormatException e) {
                onValidationError("Invalid proxy port.");
                proxyPort.requestFocusInWindow();
                return false;
            }

            if (proxyAuthUsed.isSelected()) {
                String u = username.getText().trim();
                if (u.length() == 0) {
                    onValidationError("Invalid username.");
                    username.requestFocusInWindow();
                    return false;
                }
            }
        }
        return true;
    }

    public void updateUiState() {
        boolean pu = proxyUsed.isSelected();
        boolean pau = pu && proxyAuthUsed.isSelected();

        repositoryUrl.setEnabled(true);
        proxyUsed.setEnabled(true);
        proxyHost.setEnabled(pu);
        proxyPort.setEnabled(pu);
        proxyAuthUsed.setEnabled(pu);
        username.setEnabled(pau);
        password.setEnabled(pau);
    }

    public void transferConfigDataToUi() {
        transferConfigDataToUi(connectionConfigData);
    }

    public void transferConfigDataToUi(ConnectionConfigData connectionConfigData) {
        ProxyConfig proxyConfig = connectionConfigData.getProxyConfig();
        repositoryUrl.setText(connectionConfigData.getRepositoryUrl());
        proxyUsed.setSelected(connectionConfigData.isProxyUsed());
        proxyPort.setText(String.valueOf(proxyConfig.getPort()));
        proxyHost.setText(proxyConfig.getHost());
        proxyAuthUsed.setSelected(proxyConfig.isAuthorizationUsed());
        username.setText(proxyConfig.getUsername());
        password.setText(
                new String(proxyConfig.getPassword() != null ? proxyConfig.getPassword() : new char[0]));
        updateUiState();
    }

    public void transferUiToConfigData() {
        transferUiToConfigData(connectionConfigData);
    }

    public void transferUiToConfigData(ConnectionConfigData connectionConfigData) {
        ProxyConfig proxyConfig = connectionConfigData.getProxyConfig();
        connectionConfigData.setProxyUsed(proxyUsed.isSelected());
        proxyConfig.setHost(proxyHost.getText());
        proxyConfig.setPort(Integer.parseInt(proxyPort.getText()));
        proxyConfig.setAuthorizationUsed(proxyAuthUsed.isSelected());
        proxyConfig.setUsername(username.getText());
        proxyConfig.setPassword(password.getPassword());
    }

    public boolean showDialog(Window parent, String title, ActionListener helpHandler) {
        JDialog dialog = createDialog(parent, title, helpHandler);
        dialog.pack();
        SwingHelper.centerComponent(dialog, parent);
        dialog.setVisible(true);
        dialog.dispose();
        return confirmed;
    }

    public JDialog createDialog(Window parent, String title, ActionListener helpHandler) {
        final JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (validateUiState()) {
                    transferUiToConfigData();
                    dialog.setVisible(false);
                    setConfirmed(true);
                }
            }
        });
        buttonPane.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        buttonPane.add(cancelButton);

        if (helpHandler != null) {
            JButton helpButton = new JButton("Help");
            helpButton.addActionListener(helpHandler);
            buttonPane.add(helpButton);
        }

        JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.add(this, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        okButton.setDefaultCapable(true);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.setContentPane(contentPane);
        setConfirmed(false);
        return dialog;
    }

    private void initUI() {
        ChangeListener updateStateChangeHandler = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateUiState();
            }
        };

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.right = 2;

        // Repository

        c.insets.top = 0;
        c.insets.bottom = 2;

        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        panel.add(new JLabel("Module repository URL:"), c);

        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        repositoryUrl = new JTextField(50);
        repositoryUrl.setEditable(false);
        panel.add(repositoryUrl, c);

        // Proxy

        c.insets.top = 10;

        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        proxyUsed = new JCheckBox("Use HTTP proxy");
        proxyUsed.setAlignmentX(LEFT_ALIGNMENT);
        proxyUsed.addChangeListener(updateStateChangeHandler);
        panel.add(proxyUsed, c);

        c.insets.top = 0;
        c.insets.bottom = 2;

        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.left = 12;
        panel.add(new JLabel("Proxy host:"), c);

        c.gridx = 1;
        c.gridwidth = 1;
        c.insets.left = 0;
        proxyHost = new JTextField(50);
        panel.add(proxyHost, c);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.left = 12;
        panel.add(new JLabel("Proxy port:"), c);

        c.gridx = 1;
        c.gridwidth = 1;
        c.insets.left = 0;
        proxyPort = new JTextField(6);
        panel.add(proxyPort, c);

        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets.left = 12;
        proxyAuthUsed = new JCheckBox("Use HTTP proxy authentication");
        proxyAuthUsed.addChangeListener(updateStateChangeHandler);
        panel.add(proxyAuthUsed, c);

        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.left = 24;
        panel.add(new JLabel("User name:"), c);

        c.gridx = 1;
        c.gridwidth = 1;
        c.insets.left = 0;
        username = new JTextField(12);
        panel.add(username, c);

        c.gridy = 7;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.left = 24;
        panel.add(new JLabel("Password:"), c);

        c.gridx = 1;
        c.gridwidth = 1;
        c.insets.left = 0;
        password = new JPasswordField(12);
        panel.add(password, c);

        setLayout(new BorderLayout(2, 2));
        add(panel, BorderLayout.CENTER);
    }

    private void onValidationError(String message) {
        if (validationHandler != null) {
            validationHandler.onError(message);
        } else {
            JOptionPane.showMessageDialog(this, message, "Update Configuration", JOptionPane.ERROR_MESSAGE);
        }
    }


    public interface ValidtationHandler {
        void onError(String message);
    }
}

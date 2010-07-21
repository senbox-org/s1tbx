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
package org.esa.beam.scripting.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Module;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.scripting.visat.ScriptManager;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.script.ScriptEngine;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;


public class ScriptAction extends AbstractVisatAction {
    private String code;
    private String type;
    private String src;
    private ScriptManager scriptManager;
    private Module module;

    public ScriptAction() {
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        if (scriptManager == null) {
            scriptManager = new ScriptManager(module.getClassLoader(), new PrintWriter(new OutputStreamWriter(System.out), true)
            );
        }

        Object eventSource = event.getSource();
        final Component component = eventSource instanceof Component ? (Component) eventSource : null;

        ScriptEngine scriptEngine = getScriptEngine();
        if (scriptEngine == null) {
            JOptionPane.showMessageDialog(component, "Undefined scripting language.",
                                          getText(), JOptionPane.ERROR_MESSAGE);
            return;
        }

        scriptManager.setEngine(scriptEngine);

        if (src != null) {
            try {
                URL resource = module.getResource(src);
                if (resource == null) {
                    resource = new File(src).toURI().toURL();
                }
                scriptManager.execute(resource, new MyObserver(component));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(component, "Error:\n" + e.getMessage(),
                                              getText(), JOptionPane.ERROR_MESSAGE);

            }
        }

        if (code != null) {
            scriptManager.execute(code, new MyObserver(component));
        }
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        module = config.getDeclaringExtension().getDeclaringModule();

        ConfigurationElement script = config.getChild("script");
        if (script != null) {
            String scriptType = script.getAttribute("type");
            if (StringUtils.isNotNullAndNotEmpty(scriptType)) {
                type = scriptType;
            }
            String scriptSrc = script.getAttribute("src");
            if (StringUtils.isNotNullAndNotEmpty(scriptSrc)) {
                src = scriptSrc;
            }
            String scriptCode = script.getValue();
            if (StringUtils.isNotNullAndNotEmpty(scriptCode)) {
                code = scriptCode;
            }


            System.out.printf("ScriptAction [%s] of module [%s]:%n", getText(), module.getSymbolicName());
            System.out.printf("  type = [%s]%n", this.type);
            System.out.printf("  src = [%s]%n", this.src);
            System.out.printf("  code = [%s]%n", this.code);
        }
    }

    private ScriptEngine getScriptEngine() {
        ScriptEngine scriptEngine = null;
        if (type != null) {
            scriptEngine = scriptManager.getEngineByMimeType(type);
        }
        if (scriptEngine == null && src != null) {
            int i = src.lastIndexOf(".");
            if (i > 0) {
                String ext = src.substring(i + 1);
                scriptEngine = scriptManager.getEngineByExtension(ext);
            }
        }
        return scriptEngine;
    }

    private class MyObserver implements ScriptManager.Observer {
        private final Component component;

        public MyObserver(Component component) {
            this.component = component;
        }

        @Override
        public void onSuccess(Object value) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(component, "Success.");
                }
            });
        }

        @Override
        public void onFailure(final Throwable throwable) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(component, "Error:\n" + throwable.getMessage(),
                                                  getText(), JOptionPane.ERROR_MESSAGE);
                    throwable.printStackTrace(System.out);
                }
            });
        }
    }
}
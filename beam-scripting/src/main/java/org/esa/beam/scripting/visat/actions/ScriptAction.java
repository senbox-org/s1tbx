/*
 * $Id: ProcessorAction.java,v 1.6 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
import javax.script.ScriptException;
import javax.swing.JOptionPane;
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
            scriptManager = new ScriptManager(new PrintWriter(new OutputStreamWriter(System.out), true),
                                              module.getClassLoader());
        }

        Object eventSource = event.getSource();
        Component component = null;
        if (eventSource instanceof Component) {
            component = (Component) eventSource;
        }

        ScriptEngine scriptEngine = getScriptEngine();
        if (scriptEngine == null) {
            JOptionPane.showMessageDialog(component, "Undefined scripting language.",
                                          getText(), JOptionPane.ERROR_MESSAGE);
            return;
        }
        scriptManager.setScriptEngine(scriptEngine);

        if (src != null) {
            try {
                URL resource = module.getResource(src);
                if (resource == null) {
                    resource = new File(src).toURI().toURL();
                }
                scriptManager.evalScript(resource);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(component, "I/O error:\n" + e.getMessage(),
                                              getText(), JOptionPane.ERROR_MESSAGE);

            } catch (ScriptException e) {
                JOptionPane.showMessageDialog(component, "Script error:\n" + e.getMessage(),
                                              getText(), JOptionPane.ERROR_MESSAGE);

            }
        }

        if (code != null) {
            try {
                scriptManager.evalScriptCode(code);
            } catch (ScriptException e) {
                JOptionPane.showMessageDialog(component, "Script error:\n" + e.getMessage(),
                                              getText(), JOptionPane.ERROR_MESSAGE);
            }
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
            scriptEngine = scriptManager.getScriptEngineManager().getEngineByMimeType(type);
        }
        if (scriptEngine == null && src != null) {
            int i = src.lastIndexOf(".");
            if (i > 0) {
                String ext = src.substring(i + 1);
                scriptEngine = scriptManager.getScriptEngineManager().getEngineByExtension(ext);
            }
        }
        return scriptEngine;
    }
}
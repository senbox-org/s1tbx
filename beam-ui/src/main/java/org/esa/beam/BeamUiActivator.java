/*
 * $Id: BeamUiActivator.java,v 1.7 2007/04/19 06:56:47 norman Exp $
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
package org.esa.beam;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.*;

import javax.help.HelpSet;
import javax.help.HelpSetException;
import java.net.URL;
import java.util.logging.Level;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.TreeNode;

/**
 * The activator for the BEAM UI module. Registers help set extensions.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $Revision: 1.7 $ $Date: 2007/04/19 06:56:47 $
 */
public class BeamUiActivator implements Activator {

    private ModuleContext moduleContext;
    private TreeNode<HelpSet> helpSetRegistry;
    private int helpSetNo;

    public void start(ModuleContext moduleContext) throws CoreException {
        this.moduleContext = moduleContext;
        registerHelpSets(moduleContext);
    }

    private void registerHelpSets(ModuleContext moduleContext) {
        this.helpSetRegistry = new TreeNode<HelpSet>("");

        ExtensionPoint hsExtensionPoint = moduleContext.getModule().getExtensionPoint("helpSets");
        Extension[] hsExtensions = hsExtensionPoint.getExtensions();
        for (Extension extension : hsExtensions) {
            ConfigurationElement confElem = extension.getConfigurationElement();
            ConfigurationElement[] helpSetElements = confElem.getChildren("helpSet");
            for (ConfigurationElement helpSetElement : helpSetElements) {
                registerHelpSet(helpSetElement, extension.getDeclaringModule());
            }
        }

        addNodeToHelpSys(helpSetRegistry);
    }

    private void addNodeToHelpSys(TreeNode<HelpSet> helpSetNode) {
        if (helpSetNode.getContent() != null) {
            HelpSys.add(helpSetNode.getContent());
        }
        TreeNode<HelpSet>[] children = helpSetNode.getChildren();
        for (TreeNode<HelpSet> child : children) {
            addNodeToHelpSys(child);
        }
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        // todo - disposing the HelpSystem should be done here
        //        currently it's done in BasicApp.handleImminentExit()
        //        because this module is not stopped until the HelpSystem is running
        this.moduleContext = null;
        this.helpSetRegistry = null;
    }

    private void registerHelpSet(ConfigurationElement helpSetElement, Module declaringModule) {
        String helpSetPath = null;

        ConfigurationElement pathElem = helpSetElement.getChild("path");
        if (pathElem != null) {
            helpSetPath = pathElem.getValue();
        }
        // todo - remove
        if (helpSetPath == null) {
            helpSetPath = helpSetElement.getAttribute("path");
        }
        if (helpSetPath == null) {
            String message = String.format("Missing resource [path] element in a help set declared in module [%s].",
                                           declaringModule.getName());
            moduleContext.getLogger().severe(message);
            return;
        }

        URL helpSetUrl = declaringModule.getClassLoader().getResource(helpSetPath);
        if (helpSetUrl == null) {
            String message = String.format("Help set resource path [%s] of module [%s] not found.",
                                           helpSetPath, declaringModule.getName());
            moduleContext.getLogger().severe(message);
            return;
        }

        HelpSet helpSet;
        try {
            helpSet = new HelpSet(declaringModule.getClassLoader(), helpSetUrl);
        } catch (HelpSetException e) {
            String message = String.format("Failed to add help set [%s] of module [%s]: %s.",
                                           helpSetPath, declaringModule.getName(),
                                           e.getMessage());
            moduleContext.getLogger().log(Level.SEVERE, message, e);
            return;
        }

        String helpSetId;
        ConfigurationElement idElem = helpSetElement.getChild("id");
        if (idElem != null) {
            helpSetId = idElem.getValue();
        } else {
            helpSetId = "helpSet$" + helpSetNo;
            helpSetNo++;

            String message = String.format("Missing [id] element in help set [%s] of module [%s].",
                                           helpSetPath,
                                           declaringModule.getSymbolicName());
            moduleContext.getLogger().warning(message);
        }

        String helpSetParent;
        ConfigurationElement parentElem = helpSetElement.getChild("parent");
        if (parentElem != null) {
            helpSetParent = parentElem.getValue();
        } else {
            helpSetParent = ""; // = root
        }

        TreeNode<HelpSet> parentNode = helpSetRegistry.createChild(helpSetParent);
        TreeNode<HelpSet> childNode = parentNode.getChild(helpSetId);
        if (childNode == null) {
            childNode = new TreeNode<HelpSet>(helpSetId, helpSet);
            parentNode.addChild(childNode);
        } else if (childNode.getContent() == null) {
            childNode.setContent(helpSet);
        } else {
            String message = String.format("Help set ignored: Duplicate identifier [%s] in help set [%s] of module [%s] ignored.",
                                           helpSetId,
                                           helpSetPath,
                                           declaringModule.getName());
            moduleContext.getLogger().severe(message);
        }
    }
}

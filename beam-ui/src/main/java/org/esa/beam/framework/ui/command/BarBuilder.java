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
package org.esa.beam.framework.ui.command;

import java.awt.Component;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.esa.beam.util.TreeNode;

public class BarBuilder {
	
//	private final Map<String, TreeNodeM<Command>> mMap;
    private CommandManager commandManager;
    private TreeNode<Command> root;
    
    
	public BarBuilder(CommandManager commandManager) {
		this.commandManager = commandManager;
//		mMap = new HashMap<String, TreeNodeM<Command>>();
	}
    
    public void buildTree() {
    	final int numCommands = commandManager.getNumCommands();
    	root = new TreeNode<Command>("");
    	for (int i=0; i<numCommands; i++) {
    		final Command command = commandManager.getCommandAt(i);
    		TreeNode<Command> node = root.createChild(qualifiedID(command));
    		node.setContent(command);
//    		TreeNodeM<Command> node = new TreeNodeM<Command>(command);
//    		System.out.println("QID: "+qualifiedID(command));
//    		mMap.put(qualifiedID(command), node);
    	}
//    	for (int i=0; i<numCommands; i++) {
//    		final Command command = commandManager.getCommandAt(i);
//    		final String parentID = command.getParent();
//    		Command parentCommand = getParentSafe(parentID);
//    		connectChildParent(command, parentCommand);
//    	}
    	
    	// replace proxies
    	replaceProxies(root);
    }
    
    private void replaceProxies(TreeNode<Command> node) {
    	TreeNode<Command>[] children = node.getChildren();
    	for (TreeNode<Command> childNode : children) {
    		System.out.println("path:"+childNode.getAbsolutePath());
			if (childNode.getContent() == null) {
				String id = childNode.getId();
				CommandGroup commandGroup = new CommandGroup(id, null);
//				commandGroup.setInlined(true);
				childNode.setContent(commandGroup);
			}
			replaceProxies(childNode);
		}
    }
    
    private String qualifiedID(Command command) {
    	final String parentID = command.getParent();
    	final String id = command.getCommandID();
    	if (parentID != null) {
    		return parentID + "/" + id;
    	} else {
    		return id;
    	}
    }
    
//    private Command getParentSafe(String parentID) {
//    	System.out.println("addP SAFE:"+parentID);
//    	
//    	final TreeNodeM<Command> parentNode = mMap.get(parentID);
//		if (parentNode == null) {
//			addParent(parentID);
//		}
//		Command parentCommand = mMap.get(parentID).getElement();
//		return parentCommand;
//    }
//    
//    private void connectChildParent(Command child, Command parent) {
//    	TreeNodeM<Command> parentNode = mMap.get(qualifiedID(parent));
//		TreeNodeM<Command> childNode = mMap.get(qualifiedID(child));
//		parentNode.insert(childNode);
//    }
//    
//    private void addParent(String parentID) {
//    	System.out.println("addP:"+parentID);
//    	
//    	CommandGroup commandGroup = new CommandGroup(parentID, null);
//		TreeNodeM<Command> commandGroupNode = new TreeNodeM<Command>(commandGroup);
//		commandManager.addCommand(commandGroup);
//		mMap.put(parentID, commandGroupNode);
//		
//		if (parentID.contains("/")) {
//			String[] splitParents = parentID.split("/");
//			if (splitParents.length > 2) {
//				throw new IllegalArgumentException(
//						"parentIDs with more than one slash are not allowed. Was: " + parentID);
//			}
//			commandGroup.setInlined(true);
//			Command parentCmd = getParentSafe(splitParents[0]);
//			connectChildParent(commandGroup, parentCmd);
//		}
//    }

    public Command[] getMainMenuBarCommands(){
    	TreeNode<Command>[] children = root.getChildren();
//    	Command[] commands = new Command[children.length];
    	Command[] commands = new Command[1];
    	for (int i = 0; i < 1; i++) {
//    		for (int i = 0; i < children.length; i++) {
    		Command command = children[i].getContent();
			commands[i] = command;
		}
    	return commands;
    }
    
	public void buildMenu(String path, JMenu menu, boolean isRoot) {
		System.out.println("buildMenu: "+ path);
		
		TreeNode<Command> node = root.getChild(path);
		Command command = node.getContent();
		System.out.println("id:"+command.getCommandID());
		
		if (command instanceof CommandGroup) {
			CommandGroup commandGroup = (CommandGroup) command;
			TreeNode<Command>[] children = node.getChildren();
//			if (commandGroup.isSorted()) {
//				sortCommands(children);
//			}
//			if (commandGroup.isInlined()) {
//				if (!isFirstEntry(menu) && !isLastItemSeparator(menu)) {
//					menu.addSeparator();
//				}
//				buildChildren(menu, children);
//				menu.addSeparator();
//			} else {
//				if (isRoot) {
//					buildChildren(menu, children);
//				} else {
//					JMenu subMenu = (JMenu) commandGroup.createMenuItem();
//					menu.add(subMenu);
//					buildChildren(subMenu, children);
//				}
//			}
		} else {
			JMenuItem menuItem = command.createMenuItem();
			menu.add(menuItem);
		}
	}

	private void sortCommands(TreeNode<Command>[] children) {
		Arrays.sort(children, new Comparator<TreeNode<Command>>() {

			public int compare(TreeNode<Command> node1, TreeNode<Command> node2) {
				final Command c1 = node1.getContent();
				final Command c2 = node2.getContent();
				return c1.getText().compareToIgnoreCase(c2.getText());
			}});
	}

	private void buildChildren(JMenu menu, TreeNode<Command>[] children) {
		for (TreeNode<Command> childNode : children) {
//			Command childCmd = childNode.getContent();
//			String childId = childCmd.getCommandID();
			String absolutePath = childNode.getAbsolutePath();
			buildMenu(absolutePath, menu, false);
		}
	}

	private boolean isLastItemSeparator(JMenu menu) {
		final JPopupMenu popupMenu = menu.getPopupMenu();
		final int count = popupMenu.getComponentCount();
		Component component = popupMenu.getComponent(count-1);
		if (component instanceof JSeparator) {
			return true;
		}
		return false;
	}
	
	private boolean isFirstEntry(JMenu menu) {
		int count = menu.getPopupMenu().getComponentCount();
		return (count == 0);
	}
}

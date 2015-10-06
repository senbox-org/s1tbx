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

package org.esa.snap.core.util;

import com.bc.ceres.core.Assert;

import java.util.LinkedList;
import java.util.List;

/**
 * A tree node implementation.
 */
public class TreeNode<T> {
    private TreeNode<T> parent;
    private final String id;
    private T content;
    private List<TreeNode<T>> children;

    public TreeNode(String id) {
        Assert.notNull(id, "id");
        this.id = id;
        this.children = new LinkedList<TreeNode<T>>();
    }

    public TreeNode(String id, T content) {
        this(id);
        this.content = content;
    }

    public TreeNode<T> getRoot() {
        TreeNode<T> node = this;
        while (node.getParent() != null) {
            node = node.getParent();
        }
        return node;
    }

    public String getAbsolutePath() {
        StringBuilder path = new StringBuilder(32);
        TreeNode<T> node = this;
        while (node.getParent() != null) {
            path.insert(0, "/");
            path.insert(0, node.getId());
            node = node.getParent();
        }
        return path.toString();
    }

    public String getId() {
        return id;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    public TreeNode<T> getChild(String path) {
        return getChildForPath(path, false);
    }

    public TreeNode<T> createChild(String path) {
        return getChildForPath(path, true);
    }

    public void addChild(TreeNode<T> child) {
        Assert.notNull(child, "child");
        if (children == null) {
            children = new LinkedList<TreeNode<T>>();
        }
        children.add(child);
        child.setParent(this);
    }

    public boolean removeChild(TreeNode<T> child) {
        Assert.notNull(child, "child");
        boolean suceess = false;
        if (children != null) {
            suceess = children.remove(child);
            if (suceess) {
                child.setParent(null);
            }
        }
        return suceess;
    }

    public TreeNode<T>[] getChildren() {
        if (children != null) {
            return children.toArray(new TreeNode[children.size()]);
        } else {
            return new TreeNode[0];
        }
    }

    private TreeNode<T> getChildForPath(String path, boolean create) {
        int separatorPos = path.indexOf('/');
        if (separatorPos == -1) {
            return getChildForId(path, create);
        } else if (separatorPos == 0) {
            return getRoot().getChildForPath(path.substring(1), create);
        }
        String id = path.substring(0, separatorPos);
        TreeNode<T> child = getChildForId(id, create);
        if (child != null) {
            String remainingPath = path.substring(separatorPos + 1);
            if (remainingPath.equals("")) {
                return child;
            } else {
                return child.getChildForPath(remainingPath, create);
            }
        }
        return null;
    }

    private TreeNode<T> getChildForId(String id, boolean create) {
        if (id.equals("") || id.equals(".")) {
            return this;
        }
        if (id.equals("..")) {
            return getParent();
        }
        if (children != null) {
            for (TreeNode<T> child : children) {
                if (child.getId().equals(id)) {
                    return child;
                }
            }
        }
        if (create) {
            TreeNode<T> proxyChild = new TreeNode<T>(id);
            addChild(proxyChild);
            return proxyChild;
        }
        return null;
    }
}

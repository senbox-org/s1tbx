package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ConfigurationElementBase;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import java.util.ArrayList;

/**
 * A configuration element, with its attributes and children,
 * directly reflects the content and structure of the extension
 * section within the declaring plug-in's manifest (plugin.xml) file.
 * <p/>
 * <p>This interface also provides a way to create executable extension objects.
 * This interface is not intended to be implemented by clients.</p>
 */
public abstract class ConfigurationElementBaseImpl<T extends ConfigurationElementBase>
        implements ConfigurationElementBase<T> {

    private final T parent;
    private final Xpp3Dom dom;
    private T[] children;

    protected ConfigurationElementBaseImpl(T parent, Xpp3Dom dom) {
        this.dom = dom;
        this.parent = parent;
    }

    Xpp3Dom getDom() {
        return dom;
    }

    public T getParent() {
        return parent;
    }

    public T getChild(String elementName) {
        T[] children = getChildren();
        // todo - naive implementation, maybe replace with something faster
        for (T child : children) {
            if (child.getName().equals(elementName)) {
                return (T) child;
            }
        }
        return null;
    }

    public T[] getChildren() {
        if (children == null) {
            children = createChildren(getDom().getChildren());
        }
        return children;
    }

    public T[] getChildren(String elementName) {
        T[] children = getChildren();
        ArrayList<T> list = new ArrayList<T>(children.length);
        // todo - naive implementation, maybe replace with something faster
        for (T child : children) {
            if (child.getName().equals(elementName)) {
                list.add(child);
            }
        }
        int n = list.size();
        return list.toArray(createEmptyArray(n));
    }

    public String getName() {
        return getDom().getName();
    }

    public String getValue() {
        return getDom().getValue();
    }

    public String getAttribute(String attributeName) {
        return getDom().getAttribute(attributeName);
    }

    public String[] getAttributeNames() {
        return getDom().getAttributeNames();
    }

    protected abstract T[] createChildren(Xpp3Dom[] doms);

    protected abstract T[] createEmptyArray(int n);
}

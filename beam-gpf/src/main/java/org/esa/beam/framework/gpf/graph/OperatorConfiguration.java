/*
 * $Id: $
 * 
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.framework.gpf.graph;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by marcoz.
 * 
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class OperatorConfiguration {

    private final Xpp3Dom configuration;
    private Set<Reference> referenceSet;

    public OperatorConfiguration(Xpp3Dom configuration,
            Set<Reference> references) {
        this.configuration = configuration;
        this.referenceSet = references;
    }

    public Xpp3Dom getConfiguration() {
        return configuration;
    }

    public Set<Reference> getReferenceSet() {
        return referenceSet;
    }

    public static OperatorConfiguration extractReferences(Xpp3Dom xpp3Dom,
            GraphContext graphContext, Map<String, Object> map) {
        if (xpp3Dom == null) {
            return null;
        }
        Xpp3Dom config = new Xpp3Dom(xpp3Dom.getName());
        Set<Reference> references = new HashSet<Reference>(17);
        Xpp3Dom[] children = xpp3Dom.getChildren();

        for (Xpp3Dom child : children) {
            String reference = child.getAttribute("refid");
            if (reference != null) {
                String parameterName = child.getName();
                if (reference.contains(".")) {
                    String[] referenceParts = reference.split("\\.");
                    String referenceNodeId = referenceParts[0];
                    String propertyName = referenceParts[1];
                    Node node = graphContext.getGraph().getNode(referenceNodeId);
                    NodeContext referedNodeContext = graphContext.getNodeContext(node);
                    Operator operator = referedNodeContext.getOperator();
                    PropertyReference propertyReference = new PropertyReference(parameterName, propertyName, operator);
                    references.add(propertyReference);
                } else {
                    ParameterReference parameterReference = new ParameterReference(parameterName, map.get(reference));
                    references.add(parameterReference);
                }
            } else {
                config.addChild(child);
            }
        }

        return new OperatorConfiguration(config, references);
    }

    public static interface Reference {
        public Object getValue();

        public String getParameterName();
    }

    public static class PropertyReference implements Reference {
        final String parameterName;
        final String propertyName;
        final Operator operator;

        public PropertyReference(String parameterName, String propertyName,
                Operator operator) {
            this.parameterName = parameterName;
            this.propertyName = propertyName;
            this.operator = operator;
        }

        @Override
        public Object getValue() {
            return operator.getTargetProperty(propertyName);
        }

        @Override
        public String getParameterName() {
            return parameterName;
        }
    }

    public static class ParameterReference implements Reference {

        private final String name;
        private final Object value;

        public ParameterReference(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getParameterName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }

    }
}

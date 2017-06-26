/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.descriptor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.StreamException;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.descriptor.dependency.Bundle;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.core.gpf.descriptor.template.FileTemplate;
import org.esa.snap.core.gpf.descriptor.template.MemoryTemplate;
import org.esa.snap.core.gpf.descriptor.template.Template;
import org.esa.snap.core.gpf.descriptor.template.TemplateConverter;
import org.esa.snap.core.gpf.descriptor.template.TemplateEngine;
import org.esa.snap.core.gpf.descriptor.template.TemplateException;
import org.esa.snap.core.gpf.descriptor.template.TemplateType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Operator descriptor class for ToolAdapterOp.
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
@XStreamAlias("operator")
public class ToolAdapterOperatorDescriptor implements OperatorDescriptor {

    public static final String SOURCE_PACKAGE = "package";
    public static final String SOURCE_USER = "user";
    public static final Class[] annotatedClasses = new Class[] {
            ToolAdapterOperatorDescriptor.class, TemplateParameterDescriptor.class,
            SystemVariable.class, SystemDependentVariable.class, FileTemplate.class,
            MemoryTemplate.class
    };
    private static final Logger logger = Logger.getLogger(ToolAdapterOperatorDescriptor.class.getName());

    private String name;
    private Class<? extends Operator> operatorClass;
    private String alias;
    private String label;
    private String version;
    private String description;
    private String authors;
    private String copyright;
    private Boolean internal;
    private Boolean autoWriteSuppressed;
    private String menuLocation;
    private Boolean preprocessTool = false;
    private String preprocessorExternalTool;
    private Boolean writeForProcessing = false;
    private String processingWriter;
    private String mainToolFileLocation;
    private String workingDir;
    private FileTemplate template;
    private String progressPattern;
    private String errorPattern;
    private String stepPattern;
    @XStreamAlias("variables")
    private List<SystemVariable> variables;
    @XStreamAlias("parameters")
    private List<ToolParameterDescriptor> toolParameterDescriptors = new ArrayList<>();
    private String source;
    private boolean isSystem;
    private boolean isHandlingOutputName;
    @XStreamAlias("windowsBundle")
    private Bundle windowsBundle;
    @XStreamAlias("linuxBundle")
    private Bundle linuxBundle;
    @XStreamAlias("macosxBundle")
    private Bundle macosxBundle;
    private String helpID;

    private SimpleSourceProductDescriptor[] sourceProductDescriptors;
    private DefaultSourceProductsDescriptor sourceProductsDescriptor;
    private DefaultTargetProductDescriptor targetProductDescriptor;
    private DefaultTargetPropertyDescriptor[] targetPropertyDescriptors;

    private int numSourceProducts;

    private TemplateType templateType;

    @XStreamOmitField
    private TemplateEngine templateEngine;

    ToolAdapterOperatorDescriptor() {
        this.sourceProductDescriptors = new SimpleSourceProductDescriptor[] {
                new SimpleSourceProductDescriptor(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID)
        };
        this.variables = new ArrayList<>();
        this.toolParameterDescriptors = new ArrayList<>();
        this.templateType = TemplateType.VELOCITY;
        this.windowsBundle = new Bundle(this, BundleType.NONE, null);
        this.linuxBundle = new Bundle(this, BundleType.NONE, null);
        this.macosxBundle = new Bundle(this, BundleType.NONE, null);
    }

    public ToolAdapterOperatorDescriptor(String name, Class<? extends Operator> operatorClass) {
        this();
        this.name = name;
        this.operatorClass = operatorClass;
    }

    public ToolAdapterOperatorDescriptor(String name, Class<? extends Operator> operatorClass, String alias, String label, String version, String description, String authors, String copyright, String menuLocation) {
        this(name, operatorClass);
        this.alias = alias;
        this.label = label;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.copyright = copyright;
        this.menuLocation = menuLocation;
    }

    /**
     * Copy constructor
     * @param obj   The descriptor to be copied
     */
    public ToolAdapterOperatorDescriptor(ToolAdapterOperatorDescriptor obj) {
        this(obj.getName(), obj.getOperatorClass(), obj.getAlias(), obj.getLabel(), obj.getVersion(), obj.getDescription(), obj.getAuthors(), obj.getCopyright(), obj.getMenuLocation());
        this.internal = obj.isInternal();
        this.autoWriteSuppressed = obj.isAutoWriteDisabled();

        this.preprocessTool = obj.preprocessTool;
        this.preprocessorExternalTool = obj.preprocessorExternalTool;
        this.writeForProcessing = obj.writeForProcessing;
        this.processingWriter = obj.processingWriter;
        this.mainToolFileLocation = obj.mainToolFileLocation;
        this.workingDir = obj.workingDir;
        this.templateType = obj.templateType;
        if (obj.template != null) {
            try {
                this.template = (FileTemplate) obj.template.copy();
                this.template.associateWith(getTemplateEngine());
            } catch (IOException ioex) {
                this.template = new FileTemplate();
                try {
                    this.template.associateWith(getTemplateEngine());
                    this.template.setContents("Error: " + ioex.getMessage(), false);
                } catch (TemplateException e) {
                    logger.warning(e.getMessage());
                }
            } catch (TemplateException e) {
                logger.warning(e.getMessage());
            }
        }
        this.progressPattern = obj.progressPattern;
        this.errorPattern = obj.errorPattern;
        this.stepPattern = obj.stepPattern;

        List<SystemVariable> variableList = obj.getVariables();
        if (variableList != null) {
            this.variables.addAll(variableList.stream()
                    .filter(Objects::nonNull)
                    .map(SystemVariable::createCopy).collect(Collectors.toList()));
        }

        this.sourceProductDescriptors = new SimpleSourceProductDescriptor[obj.getSourceProductDescriptors().length];
        for (int i = 0; i < obj.getSourceProductDescriptors().length; i++) {
            this.sourceProductDescriptors[i] = ((SimpleSourceProductDescriptor) (obj.getSourceProductDescriptors()[i]));
        }

        this.sourceProductsDescriptor = (DefaultSourceProductsDescriptor) obj.getSourceProductsDescriptor();

        this.toolParameterDescriptors.addAll(obj.getToolParameterDescriptors().stream().map(parameter -> !parameter.isTemplateParameter() ?
                new ToolParameterDescriptor(parameter) :
                new TemplateParameterDescriptor((TemplateParameterDescriptor) parameter)).collect(Collectors.toList()));

        this.targetProductDescriptor = (DefaultTargetProductDescriptor) obj.getTargetProductDescriptor();

        this.targetPropertyDescriptors = new DefaultTargetPropertyDescriptor[obj.getTargetPropertyDescriptors().length];
        for (int i = 0; i < obj.getTargetPropertyDescriptors().length; i++) {
            this.targetPropertyDescriptors[i] = ((DefaultTargetPropertyDescriptor) (obj.getTargetPropertyDescriptors()[i]));
        }

        this.isHandlingOutputName = obj.isHandlingOutputName;
        if (obj.windowsBundle != null) {
            this.windowsBundle = new Bundle(obj.windowsBundle);
            this.windowsBundle.setParent(this);
        }
        if (obj.linuxBundle != null) {
            this.linuxBundle = new Bundle(obj.linuxBundle);
            this.linuxBundle.setParent(this);
        }
        if (obj.macosxBundle != null) {
            this.macosxBundle = new Bundle(obj.macosxBundle);
            this.macosxBundle.setParent(this);
        }
        this.helpID = obj.helpID;
    }

    /**
     * Variant of the copy constructor
     * @param obj       The descriptor to be copied
     * @param newName   The new name of the operator
     * @param newAlias  The new alias of the operator
     */
    public ToolAdapterOperatorDescriptor(ToolAdapterOperatorDescriptor obj, String newName, String newAlias) {
        this(obj);
        this.name = newName;
        this.alias = newAlias;
    }

    /**
     * Removes the given parameter descriptor from the internal parameter descriptor list
     * @param descriptor    The descriptor to be removed
     */
    public void removeParamDescriptor(ToolParameterDescriptor descriptor) {
        this.toolParameterDescriptors.remove(descriptor);
    }

    /**
     * Removes the given descriptors from the internal parameter descriptor list
     * @param descriptors    The list of descriptors to be removed
     */
    public void removeParamDescriptors(List<ToolParameterDescriptor> descriptors) {
        if(descriptors != null && descriptors.size() > 0) {
            descriptors.forEach(this.toolParameterDescriptors::remove);
        }
    }

    /**
     * Gets all the parameter descriptors
     * @return  The list of parameter descriptors
     */
    public List<ToolParameterDescriptor> getToolParameterDescriptors() {
        if(this.toolParameterDescriptors == null){
            this.toolParameterDescriptors = new ArrayList<>();
        }
        return this.toolParameterDescriptors;
    }

    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.createInstance(this, getTemplateType());
        }
        return templateEngine;
    }

    TemplateType getTemplateType() {
        if (templateType == null) {
            templateType = TemplateType.VELOCITY;
        }
        return templateType;
    }

    /**
     * Setter for the Alias field
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }
    /**
     * Setter for the Label field
     */
    public void setLabel(String label) {
        this.label = label;
    }
    /**
     * Setter for the Version field
     */
    public void seVersion(String version) {
        this.version = version;
    }
    /**
     * Setter for the Description field
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * Setter for the Authors field
     */
    public void setAuthors(String authors) {
        this.authors = authors;
    }
    /**
     * Setter for the Copyright field
     */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }
    /**
     * Setter for the Name field
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Setter for the operator class
     */
    public void setOperatorClass(Class<? extends Operator> operatorClass) {
        this.operatorClass = operatorClass;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getAuthors() {
        return authors;
    }

    @Override
    public String getCopyright() {
        return copyright;
    }

    @Override
    public boolean isInternal() {
        return internal != null ? internal : false;
    }

    @Override
    public boolean isAutoWriteDisabled() {
        return (autoWriteSuppressed != null && autoWriteSuppressed);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }
    /**
     * Getter for the Menu Location field
     */
    public String getMenuLocation() { return menuLocation; }
    /**
     * Setter for the Menu Location field
     */
    public void setMenuLocation(String value) { menuLocation = value; }
    /**
     * Getter for the source of the descriptor.
     * The source can be either "package" (coming from a nbm package) or "user" (user-defined).
     */
    public String getSource() { return source != null ? source : SOURCE_USER; }
    /**
     * Setter for the Source field.
     */
    public void setSource(String value) {
        source = value;
        if (!(SOURCE_PACKAGE.equals(source) || SOURCE_USER.equals(source))) {
            source = SOURCE_USER;
        }
    }

    /**
     * Determines if the source of this descriptor is from a package or creadet/modified by user.
     */
    public boolean isFromPackage() { return SOURCE_PACKAGE.equals(getSource()); }

    /**
     * Determines if the tool would produce by itself the name of the output product.
     */
    public boolean isHandlingOutputName() { return isHandlingOutputName; }

    /**
     * Setter for the isHandlingOutputName member.
     */
    public void setHandlingOutputName(boolean value) { isHandlingOutputName = value; }

    public String getHelpID() { return helpID; }

    public void setHelpID(String value) { helpID = value; }

    @Override
    public Class<? extends Operator> getOperatorClass() {
        return operatorClass != null ? operatorClass : Operator.class;
    }

    @Override
    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return sourceProductDescriptors != null ? sourceProductDescriptors : new SourceProductDescriptor[0];
    }

    @Override
    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return sourceProductsDescriptor;
    }

    @Override
    public ParameterDescriptor[] getParameterDescriptors() {
        //return parameterDescriptors != null ? parameterDescriptors : new ParameterDescriptor[0];
        ParameterDescriptor[] result = new ParameterDescriptor[0];
        return getToolParameterDescriptors().toArray(result);
    }

    @Override
    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return targetPropertyDescriptors != null ? targetPropertyDescriptors : new TargetPropertyDescriptor[0];
    }

    @Override
    public TargetProductDescriptor getTargetProductDescriptor() {
        return targetProductDescriptor;
    }
    /**
     * Getter for the Template File Location field
     */
    public FileTemplate getTemplate() {
        //return this.template;
        return template;
    }
    /**
     * Setter for the Template File Location field
     */
    public void setTemplate(FileTemplate value) throws TemplateException {
        if (value != null) {
            if (!getTemplateType().equals(value.getType())) {
                throw new TemplateException("Incompatible template type");
            }
            this.template = value;
            this.template.associateWith(getTemplateEngine());
        }
    }
    /**
     * Getter for the Working Directory field
     */
    public String getWorkingDir() {
        return workingDir != null ?
                workingDir.replace("\\", "/") : null;
    }
    /**
     * Setter for the Working Directory field
     */
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }
    /**
     * Getter for the Tool File Location field
     */
    public String getMainToolFileLocation() {
        return mainToolFileLocation != null ?
                mainToolFileLocation.replace("\\", "/") : null;
    }
    /**
     * Setter for the Tool File Location field
     */
    public void setMainToolFileLocation(String mainToolFileLocation) {
        this.mainToolFileLocation = mainToolFileLocation;
    }

    public File resolveVariables(String location) {
        return VariableResolver.newInstance(this).resolve(location);
    }

    public File resolveVariables(File file) {
        return file != null ?
                VariableResolver.newInstance(this).resolve(file.toString()) : null;
    }
    /**
     * Setter for the Progress Pattern field. The pattern is a regular expression.
     */
    public void setProgressPattern(String pattern) { this.progressPattern = pattern; }
    /**
     * Getter for the Progress Pattern field
     */
    public String getProgressPattern() { return progressPattern; }
    /**
     * Setter for the Error Pattern field. The pattern is a regular expression.
     */
    public void setErrorPattern(String pattern) { this.errorPattern = pattern; }
    /**
     * Getter for the Error Pattern field
     */
    public String getErrorPattern() { return errorPattern; }
    /**
     * Setter for the Step progress Pattern field. The pattern is a regular expression.
     */
    public void setStepPattern(String pattern) { this.stepPattern = pattern; }
    /**
     * Getter for the Step progress Pattern field
     */
    public String getStepPattern() { return this.stepPattern ; }
    /**
     * Getter for the Pre-processing Writer field
     */
    public String getProcessingWriter() {
        return processingWriter;
    }
    /**
     * Setter for the Pre-processing Writer field
     */
    public void setProcessingWriter(String processingWriter) {
        this.processingWriter = processingWriter;
    }
    /**
     * Getter for the Write Before Processing field
     */
    public Boolean shouldWriteBeforeProcessing() {
        return writeForProcessing;
    }
    /**
     * Setter for the Write Before Processing field
     */
    public void writeBeforeProcessing(Boolean writeForProcessing) {
        this.writeForProcessing = writeForProcessing;
    }
    /**
     * Getter for the Pre-processing External Tool field
     */
    public String getPreprocessorExternalTool() {
        return preprocessorExternalTool;
    }
    /**
     * Setter for the Pre-processing External Tool field
     */
    public void setPreprocessorExternalTool(String preprocessorExternalTool) {
        this.preprocessorExternalTool = preprocessorExternalTool;
    }
    /**
     * Getter for the Has Pre-processing External Tool field
     */
    public Boolean getPreprocessTool() {
        return preprocessTool;
    }
    /**
     * Setter for the Has Pre-processing External Tool field
     */
    public void setPreprocessTool(Boolean preprocessTool) {
        this.preprocessTool = preprocessTool;
    }

    public Bundle getWindowsBundle() { return this.windowsBundle; }

    public void setWindowsBundle(Bundle bundle) {
        this.windowsBundle = bundle;
        if (bundle != null && !this.equals(bundle.getParent())) {
            this.windowsBundle.setParent(this);
        }
    }

    public Bundle getLinuxBundle() { return this.linuxBundle; }

    public void setLinuxBundle(Bundle bundle) {
        this.linuxBundle = bundle;
        if (bundle != null && !this.equals(bundle.getParent())) {
            this.linuxBundle.setParent(this);
        }
    }

    public Bundle getMacosxBundle() { return this.macosxBundle; }

    public void setMacosxBundle(Bundle bundle) {
        this.macosxBundle = bundle;
        if (bundle != null && !this.equals(bundle.getParent())) {
            this.macosxBundle.setParent(this);
        }
    }

    public Bundle getBundle() {
        return getBundle(Bundle.getCurrentOS());
    }

    public void setBundles(Map<OSFamily, Bundle> bundles) {
        if (bundles != null) {
            for (Map.Entry<OSFamily, Bundle> entry : bundles.entrySet()) {
                switch (entry.getKey()) {
                    case windows:
                        this.windowsBundle = entry.getValue();
                        break;
                    case linux:
                        this.linuxBundle = entry.getValue();
                        break;
                    case macosx:
                        this.macosxBundle = entry.getValue();
                        break;
                }
            }
        }
    }

    public Map<OSFamily, Bundle> getBundles() {
        return new HashMap<OSFamily, Bundle>() {{
            put(OSFamily.windows, windowsBundle);
            put(OSFamily.linux, linuxBundle);
            put(OSFamily.macosx, macosxBundle);
        }};
    }

    public Bundle getBundle(OSFamily osFamily) {
        Bundle bundle;
        switch (osFamily) {
            case windows:
                bundle = windowsBundle;
                break;
            case linux:
                bundle = linuxBundle;
                break;
            case macosx:
                bundle = macosxBundle;
                break;
            default:
                throw new IllegalArgumentException("Operating system not supported");
        }
        if (bundle.getParent() == null) {
            bundle.setParent(this);
        }
        return bundle;
    }

    /**
     * Gets the list of user-defined system variables
     */
    public List<SystemVariable> getVariables() {
        if(this.variables == null){
            this.variables = new ArrayList<>();
        }
        return variables;
    }

    public String getVariableValue(String key) {
        String value = null;
        List<SystemVariable> variables = this.variables.stream().filter(v -> v.getKey().equals(key)).collect(Collectors.toList());
        if (variables != null && variables.size() == 1) {
            value = variables.get(0).getValue();
        }
        return value;
    }

    /**
     *  Returns the number of source products
     */
    public int getSourceProductCount() {
        return numSourceProducts;
    }

    /**
     *  Sets the number of source products.
     *  It re-dimensions internally the <code>sourceProductDescriptors</code> array.
     *  This is useful in the execution dialog, because it dictates how many selectors should
     *  be rendered.
     */
    public void setSourceProductCount(int value) {
        numSourceProducts = value;
        if (sourceProductDescriptors == null) {
            sourceProductDescriptors = new SimpleSourceProductDescriptor[numSourceProducts];
        } else {
            sourceProductDescriptors = Arrays.copyOf(sourceProductDescriptors, numSourceProducts);
        }
        for (int i = 0; i < numSourceProducts; i++) {
            if (sourceProductDescriptors[i] == null) {
                sourceProductDescriptors[i] = new SimpleSourceProductDescriptor(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID + " " + String.valueOf(i+1));
            }
        }
    }
    /**
     * Adds a user-defined system variable
     * @param variable  The variable to be added
     */
    public void addVariable(SystemVariable variable) {
        this.variables.add(variable);
    }

    /**
     * Creates a deep copy of this operator.
     */
    public ToolAdapterOperatorDescriptor createCopy() {
        return new ToolAdapterOperatorDescriptor(this, this.name, this.alias);
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param url         The URL pointing to a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static ToolAdapterOperatorDescriptor fromXml(URL url, ClassLoader classLoader) {
        String resourceName = url.toExternalForm();
        try {
            try (InputStreamReader streamReader = new InputStreamReader(url.openStream())) {
                ToolAdapterOperatorDescriptor operatorDescriptor;
                operatorDescriptor = fromXml(streamReader, resourceName, classLoader);
                return operatorDescriptor;
            }
        } catch (IOException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param file        The file containing a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static ToolAdapterOperatorDescriptor fromXml(File file, ClassLoader classLoader) throws OperatorException {
        String resourceName = file.getPath();
        try {
            try (FileReader reader = new FileReader(file)) {
                return ToolAdapterOperatorDescriptor.fromXml(reader, resourceName, classLoader);
            }
        } catch (Exception e) {
            try {
                ToolAdapterIO.convertAdapter(file.toPath());
                String name = FileUtils.getFilenameWithoutExtension(file.getParentFile() != null ? file.getParentFile() : file);
                logger.fine(String.format("Adapter %s has been automatically converted to the new format", name));
            } catch (IOException e1) {
                throw new OperatorException(formatReadExceptionText(resourceName, e), e);
            }
            try (FileReader reader = new FileReader(file)) {
                return ToolAdapterOperatorDescriptor.fromXml(reader, resourceName, classLoader);
            } catch (IOException e1) {
                throw new OperatorException(formatReadExceptionText(resourceName, e1), e1);
            }
        }
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param reader       The reader providing a valid operator descriptor XML document.
     * @param resourceName Used in error messages
     * @param classLoader  The class loader is used to load classed specified in the xml. For example the
     *                     class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static ToolAdapterOperatorDescriptor fromXml(Reader reader, String resourceName, ClassLoader classLoader) throws OperatorException {
        ToolAdapterOperatorDescriptor descriptor = new ToolAdapterOperatorDescriptor();
        try {
            createXStream(classLoader).fromXML(reader, descriptor);
            if (StringUtils.isNullOrEmpty(descriptor.getName())) {
                throw new OperatorException(formatInvalidExceptionMessage(resourceName, "missing 'name' element"));
            }
            if (StringUtils.isNullOrEmpty(descriptor.getAlias())) {
                throw new OperatorException(formatInvalidExceptionMessage(resourceName, "missing 'alias' element"));
            }
            if (descriptor.template != null) {
                descriptor.template.associateWith(descriptor.getTemplateEngine());
            }
            descriptor.getToolParameterDescriptors().stream().filter(ToolParameterDescriptor::isTemplateParameter).forEach(t -> {
                try {
                    TemplateParameterDescriptor param = (TemplateParameterDescriptor) t;
                    Template template = param.getTemplate();
                    if (template != null) {
                        //param.getTemplate().setType(descriptor.getTemplateType());
                        TemplateType templateType = template.getType();
                        //param.setTemplateEngine(descriptor.getTemplateEngine());
                        param.setTemplateEngine(TemplateEngine.createInstance(descriptor, templateType));
                    }
                } catch (TemplateException e) {
                    logger.severe(e.getMessage());
                }
            });
        } catch (StreamException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        } catch (TemplateException e) {
            throw new OperatorException(e.getMessage());
        }
        return descriptor;
    }

    /**
     * Converts an operator descriptor to XML.
     *
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A string containing valid operator descriptor XML.
     */
    public String toXml(ClassLoader classLoader) {
        return createXStream(classLoader).toXML(this);
    }

    private static XStream createXStream(ClassLoader classLoader) {
        XStream xStream = new XStream();
        xStream.setClassLoader(classLoader);
        xStream.registerConverter(new TemplateConverter(
                xStream.getConverterLookup().lookupConverterForType(Template.class),
                xStream.getReflectionProvider()));
        xStream.addDefaultImplementation(MemoryTemplate.class, Template.class);
        xStream.addDefaultImplementation(FileTemplate.class, Template.class);
        xStream.processAnnotations(annotatedClasses);
        return xStream;
    }

    private static String formatReadExceptionText(String resourceName, Exception e) {
        return String.format("Failed to read operator descriptor from '%s':\nError: %s", resourceName, e.getMessage());
    }

    private static String formatInvalidExceptionMessage(String resourceName, String message) {
        return String.format("Invalid operator descriptor in '%s': %s", resourceName, message);
    }
}

package org.esa.s1tbx.commons.io;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;

public abstract class PropertyMapProductDirectory extends AbstractProductDirectory {

    protected PropertyMap propertyMap = new DefaultPropertyMap();

    public PropertyMapProductDirectory(final File headerFile) {
        super(headerFile);
    }

    public void readProductDirectory() throws IOException {
        final File headerFile = getFile(getRootFolder() + getHeaderFileName());
        propertyMap.load(headerFile.toPath());
    }

    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        AbstractMetadataIO.AddXMLMetadata(propertyMapToXML("ProductMetadata", propertyMap),
                AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }

    protected Element propertyMapToXML(final String elementName, final PropertyMap propertyMap) {
        Element root = new Element(elementName);
        for (String key : propertyMap.getPropertyKeys()) {
            root.setAttribute(key, propertyMap.getPropertyString(key));
        }
        return root;
    }

    protected abstract void addAbstractedMetadataHeader(final MetadataElement root) throws IOException;
}

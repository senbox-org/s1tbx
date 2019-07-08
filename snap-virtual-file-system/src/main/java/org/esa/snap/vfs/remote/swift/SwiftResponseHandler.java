package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Response Handler for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftResponseHandler extends DefaultHandler {

    /**
     * The name of XML element for Name, used on parsing VFS service response XML.
     */
    private static final String NAME_ELEMENT = "name";

    /**
     * The name of XML element for Bytes, used on parsing VFS service response XML.
     */
    private static final String BYTES_ELEMENT = "bytes";

    /**
     * The name of XML element for Object, used on parsing VFS service response XML.
     */
    private static final String OBJECT_ELEMENT = "object";

    /**
     * The name of XML element for Last_modified, used on parsing VFS service response XML.
     */
    private static final String LAST_MODIFIED_ELEMENT = "last_modified";

    /**
     * The name of XML element for Subdir, used on parsing VFS service response XML.
     */
    private static final String SUBDIRECTORY_ELEMENT = "subdir";

    /**
     * The name of XML element for Container, used on parsing VFS service response XML.
     */
    private static final String CONTAINER_ELEMENT = "container";
    private static Logger logger = Logger.getLogger(SwiftResponseHandler.class.getName());
    private LinkedList<String> elementStack = new LinkedList<>();
    private List<BasicFileAttributes> items;
    private String name;
    private long size;
    private String lastModified;
    private String marker;
    private boolean isTruncated;
    private String prefix;
    private String delimiter;

    /**
     * Creates the new response handler for OpenStack Swift VFS.
     *
     * @param prefix    The VFS path to traverse
     * @param items     The list with VFS paths for files and directories
     * @param delimiter The VFS path delimiter
     */
    SwiftResponseHandler(String prefix, List<BasicFileAttributes> items, String delimiter) {
        this.prefix = prefix;
        this.items = items;
        this.delimiter = delimiter;
    }

    /**
     * Gets the text value of XML element data.
     *
     * @param ch     The XML line char array
     * @param start  The index of first char in XML element value
     * @param length The index of last char in XML element value
     * @return The text value of XML element data
     */
    private static String getTextValue(char[] ch, int start, int length) {
        return new String(ch, start, length).trim();
    }

    /**
     * Gets the marker (indicates OpenStack Swift that the list of objects from the current bucket continues).
     *
     * @return The marker
     */
    String getMarker() {
        return this.marker;
    }

    /**
     * Tells whether or not current request response contains more than 10000 objects.
     *
     * @return {@code true} if request response is truncated
     */
    boolean getIsTruncated() {
        return this.isTruncated;
    }

    /**
     * Receive notification of the start of an element.
     * Mark starting of the new XML element by adding it to the stack of XML elements.
     *
     * @param uri        The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
     * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param qName      The qualified name (with prefix), or the empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If there are no attributes, it shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            String currentElement = localName.intern();
            this.elementStack.addLast(currentElement);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to mark starting of the new XML element by adding it to the stack of XML elements, for OpenStack Swift VFS. Details: " + ex.getMessage());
            throw new SAXException(ex);
        }
    }

    /**
     * Receive notification of the end of an element.
     * Remove ending XML element from the stack of XML elements.
     * Adds the new path of OpenStack Swift object to the list of VFS paths for files and directories.
     *
     * @param uri       The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param qName     The qualified name (with prefix), or the empty string if qualified names are not available.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            String currentElement = this.elementStack.removeLast();
            if (currentElement != null && currentElement.equals(localName)) {
                if (currentElement.equals(NAME_ELEMENT) && this.elementStack.size() == 2 && (this.elementStack.get(1).equals(SUBDIRECTORY_ELEMENT) || this.elementStack.get(1).equals(CONTAINER_ELEMENT)) && !this.prefix.endsWith(this.name)) {
                    this.items.add(VFSFileAttributes.newDir(this.prefix + this.name));
                    this.isTruncated = true;
                } else if (currentElement.equals(NAME_ELEMENT) && this.elementStack.size() == 2 && this.elementStack.get(1).equals(OBJECT_ELEMENT) && !this.prefix.endsWith(this.name)) {
                    this.items.add(VFSFileAttributes.newFile(this.prefix + this.name, this.size, this.lastModified));
                    this.isTruncated = true;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to add the new path of OpenStack Swift object to the list of OpenStack Swift VFS paths for files and directories. Details: " + ex.getMessage());
            throw new SAXException(ex);
        }
    }

    /**
     * Receive notification of character data inside an element.
     * Creates the VFS path and file attributes.
     *
     * @param ch     The characters.
     * @param start  The start position in the character array.
     * @param length The number of characters to use from the character array.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            String currentElement = this.elementStack.getLast();
            switch (currentElement) {
                case NAME_ELEMENT:
                    this.marker = getTextValue(ch, start, length);
                    String[] nameParts = this.marker.split(this.delimiter);
                    this.name = this.marker.endsWith(this.delimiter) ? nameParts[nameParts.length - 1] + this.delimiter : nameParts[nameParts.length - 1];
                    break;
                case BYTES_ELEMENT:
                    this.size = getLongValue(ch, start, length);
                    break;
                case LAST_MODIFIED_ELEMENT:
                    this.lastModified = getTextValue(ch, start, length);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create the OpenStack Swift VFS path and file attributes. Details: " + ex.getMessage());
            throw new SAXException(ex);
        }
    }

    /**
     * Gets the long value of XML element data.
     *
     * @param ch     The XML line char array
     * @param start  The index of first char in XML element value
     * @param length The index of last char in XML element value
     * @return The long value of XML element data
     */
    private long getLongValue(char[] ch, int start, int length) {
        return Long.parseLong(getTextValue(ch, start, length));
    }
}

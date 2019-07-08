package org.esa.snap.vfs.remote.s3;

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
 * Response Handler for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3ResponseHandler extends DefaultHandler {

    /**
     * The name of XML element for Key, used on parsing VFS service response XML.
     */
    private static final String KEY_ELEMENT = "Key";

    /**
     * The name of XML element for Size, used on parsing VFS service response XML.
     */
    private static final String SIZE_ELEMENT = "Size";

    /**
     * The name of XML element for Contents, used on parsing VFS service response XML.
     */
    private static final String CONTENTS_ELEMENT = "Contents";

    /**
     * The name of XML element for LastModified, used on parsing VFS service response XML.
     */
    private static final String LAST_MODIFIED_ELEMENT = "LastModified";

    /**
     * The name of XML element for NextContinuationToken, used on parsing VFS service response XML.
     */
    private static final String NEXT_CONTINUATION_TOKEN_ELEMENT = "NextContinuationToken";

    /**
     * The name of XML element for IsTruncated, used on parsing VFS service response XML.
     */
    private static final String IS_TRUNCATED_ELEMENT = "IsTruncated";

    /**
     * The name of XML element for CommonPrefixes, used on parsing VFS service response XML.
     */
    private static final String COMMON_PREFIXES_ELEMENT = "CommonPrefixes";

    /**
     * The name of XML element for Prefix, used on parsing VFS service response XML.
     */
    private static final String PREFIX_ELEMENT = "Prefix";

    private static Logger logger = Logger.getLogger(S3ResponseHandler.class.getName());

    private LinkedList<String> elementStack = new LinkedList<>();
    private List<BasicFileAttributes> items;

    private String key;
    private long size;
    private String lastModified;
    private String nextContinuationToken;
    private boolean isTruncated;
    private String prefix;
    private String delimiter;

    /**
     * Creates the new response handler for S3 VFS.
     *
     * @param prefix    The VFS path to traverse
     * @param items     The list with VFS paths for files and directories
     * @param delimiter The VFS path delimiter
     */
    S3ResponseHandler(String prefix, List<BasicFileAttributes> items, String delimiter) {
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
     * Gets the continuation token (indicates S3 that the list of objects from the current bucket continues).
     *
     * @return The continuation token
     */
    String getNextContinuationToken() {
        return this.nextContinuationToken;
    }

    /**
     * Tells whether or not current request response contains more than 1000 objects.
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
            logger.log(Level.SEVERE, "Unable to mark starting of the new XML element by adding it to the stack of XML elements, for S3 VFS. Details: " + ex.getMessage());
            throw new SAXException(ex);
        }
    }

    /**
     * Receive notification of the end of an element.
     * Remove ending XML element from the stack of XML elements.
     * Adds the new path of S3 object to the list of VFS paths for files and directories.
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
                if (currentElement.equals(PREFIX_ELEMENT) && this.elementStack.size() == 2 && this.elementStack.get(1).equals(COMMON_PREFIXES_ELEMENT)) {
                    this.items.add(VFSFileAttributes.newDir(this.prefix + this.key));
                } else if (currentElement.equals(CONTENTS_ELEMENT) && this.elementStack.size() == 1) {
                    this.items.add(VFSFileAttributes.newFile(this.prefix + this.key, this.size, this.lastModified));
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to add the new path of S3 object to the list of S3 VFS paths for files and directories. Details: " + ex.getMessage());
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
                case KEY_ELEMENT:
                    this.key = getTextValue(ch, start, length);
                    String[] keyParts = this.key.split(this.delimiter);
                    this.key = this.key.endsWith(this.delimiter) ? keyParts[keyParts.length - 1] + this.delimiter : keyParts[keyParts.length - 1];
                    break;
                case SIZE_ELEMENT:
                    this.size = getLongValue(ch, start, length);
                    break;
                case LAST_MODIFIED_ELEMENT:
                    this.lastModified = getTextValue(ch, start, length);
                    break;
                case IS_TRUNCATED_ELEMENT:
                    this.isTruncated = getBooleanValue(ch, start, length);
                    break;
                case NEXT_CONTINUATION_TOKEN_ELEMENT:
                    this.nextContinuationToken = getTextValue(ch, start, length);
                    break;
                case PREFIX_ELEMENT:
                    this.key = getTextValue(ch, start, length);
                    keyParts = this.key.split(this.delimiter);
                    this.key = this.key.endsWith(this.delimiter) ? keyParts[keyParts.length - 1] + this.delimiter : keyParts[keyParts.length - 1];
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create the S3 VFS path and file attributes. Details: " + ex.getMessage());
            throw new SAXException(ex);
        }
    }

    /**
     * Gets the boolean value of XML element data.
     *
     * @param ch     The XML line char array
     * @param start  The index of first char in XML element value
     * @param length The index of last char in XML element value
     * @return The boolean value of XML element data
     */
    private boolean getBooleanValue(char[] ch, int start, int length) {
        return Boolean.parseBoolean(getTextValue(ch, start, length));
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

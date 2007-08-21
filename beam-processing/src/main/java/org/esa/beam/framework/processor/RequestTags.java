/*
 * $Id: RequestTags.java,v 1.2 2007/04/13 13:52:28 norman Exp $
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
package org.esa.beam.framework.processor;

/**
 * This class defines all tags that are valid in a request file and recognized by the <code>RequestLoader</code>.
 */
public class RequestTags {

    // the tags
    public static final String TAG_REQUEST_LIST = "RequestList";
    public static final String TAG_REQUEST = "Request";
    public static final String TAG_INPUT_PRODUCT = "InputProduct";
    public static final String TAG_OUTPUT_PRODUCT = "OutputProduct";
    public static final String TAG_BITMASK = "Bitmask";
    public static final String TAG_PARAMETER = "Parameter";
    public static final String TAG_LOG_LOCATION = "LogFile";
    // the attributes
    public static final String ATTRIB_NAME = "name";
    public static final String ATTRIB_VALUE = "value";
    /**
     * @deprecated in 4.0, use ATTRIB_FILE instead
     */
    public static final String ATTRIB_URL = "URL";
    /**
     * @deprecated in 4.0, use ATTRIB_FILE instead
     */
    public static final String ATTRIB_PATH = "path";
    public static final String ATTRIB_FILE = "file";
    public static final String ATTRIB_FILE_FORMAT = "format";
    public static final String ATTRIB_FILE_TYPE_ID = "typeId";
    public static final String ATTRIB_TYPE = "type";
}

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

package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.XStream;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * A Session I/O implementation which uses {@link XStream} to marshall and unmarshall {@link Session} instances
 * to and from XML.
 * <p>Clients may override the {@link #createXStream()} in order to create an appropriate {@code XStream}
 * for their session.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class XStreamSessionIO extends SessionIO {

    protected XStream createXStream() {
        XStream xStream = new XStream();
        xStream.setClassLoader(XStreamSessionIO.class.getClassLoader());
        xStream.autodetectAnnotations(true);
        xStream.alias("session", Session.class);
        xStream.alias("configuration", DomElement.class, DefaultDomElement.class);
        return xStream;
    }

    @Override
    public Session readSession(Reader reader) throws IOException {
        Assert.notNull(reader, "reader");
        return (Session) createXStream().fromXML(reader);
    }

    @Override
    public void writeSession(Session session, Writer writer) throws IOException {
        Assert.notNull(session, "session");
        Assert.notNull(writer, "writer");
        createXStream().toXML(session, writer);
    }
}
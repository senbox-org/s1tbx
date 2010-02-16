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
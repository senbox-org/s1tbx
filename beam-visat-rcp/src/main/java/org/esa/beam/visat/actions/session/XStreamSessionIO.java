package org.esa.beam.visat.actions.session;

import com.thoughtworks.xstream.XStream;
import com.bc.ceres.core.Assert;

import java.io.Writer;
import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;

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

    public XStreamSessionIO() {
    }

    protected XStream createXStream() {
        XStream xStream = new XStream();
        xStream.setClassLoader(XStreamSessionIO.class.getClassLoader());
        xStream.autodetectAnnotations(true);
        xStream.alias("session", Session.class);
        return xStream;
    }

    @Override
    public Session readSession(Reader reader) throws IOException {
        Assert.notNull(reader, "reader");
        return (Session) createXStream().fromXML(reader);
    }

    @Override
    public void writeSession(Session session, Writer writer)  throws IOException {
        Assert.notNull(session, "session");
        Assert.notNull(writer, "writer");
        createXStream().toXML(session, writer);
    }
}
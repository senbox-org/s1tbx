package org.esa.beam.visat.actions.session;

import com.bc.ceres.core.Assert;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * The Session I/O class is used to store and restore sessions.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class SessionIO {
    static SessionIO instance = new XStreamSessionIO();

    public static SessionIO getInstance() {
        return instance;
    }

    public static void setInstance(SessionIO instance) {
        Assert.notNull(instance, "instance");
        SessionIO.instance = instance;
    }

    public Session readSession(File file)  throws Exception {
        Assert.notNull(file, "file");
        final FileReader reader = new FileReader(file);
        try {
            return readSession(reader);
        } finally {
            reader.close();
        }
    }

    public abstract Session readSession(Reader reader) throws Exception;

    public void writeSession(Session session, File file)  throws Exception {
        Assert.notNull(session, "session");
        Assert.notNull(file, "file");
        final FileWriter writer = new FileWriter(file);
        try {
            writeSession(session, writer);
        } finally {
            writer.close();
        }
    }

    public abstract void writeSession(Session session, Writer writer)  throws Exception;
}

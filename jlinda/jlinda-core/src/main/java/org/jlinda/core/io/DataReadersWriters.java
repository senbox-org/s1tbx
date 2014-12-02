package org.jlinda.core.io;

import java.io.FileNotFoundException;

public interface DataReadersWriters {

    void readFromStream() throws FileNotFoundException;

    void writeToStream() throws FileNotFoundException;

}

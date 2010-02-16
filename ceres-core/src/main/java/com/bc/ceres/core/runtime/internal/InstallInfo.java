package com.bc.ceres.core.runtime.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.BaseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 12.09.2006
 * Time: 13:44:11
 * To change this template use File | Settings | File Templates.
 */
class InstallInfo {

    private Date date;
    private String version;
    private String[] items;

    public InstallInfo(String[] items) {
        this.date = new Date();
        this.version = "1.0";
        this.items = items;
    }

    public Date getDate() {
        return date;
    }

    public String[] getItems() {
        return items;
    }

    public static InstallInfo read(Reader reader) throws IOException {
        try {
            InstallInfo installInfo = (InstallInfo) createXStream().fromXML(reader);
            if (installInfo.date == null) {
                installInfo.date = new Date();
            }
            if (installInfo.version == null) {
                installInfo.version = "1.0";
            }
            if (installInfo.items == null) {
                installInfo.items = new String[0];
            }
            return installInfo;
        } catch (BaseException e) {
            IOException ioe = new IOException("Failed to read install info.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void write(Writer writer) throws IOException {
        try {
            this.date = new Date();
            createXStream().toXML(this, writer);
        } catch (BaseException e) {
            IOException ioe = new IOException("Failed to write install info.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static XStream createXStream() {
        XStream xStream = new XStream();
        xStream.alias("install-info", InstallInfo.class);
        return xStream;
    }
}

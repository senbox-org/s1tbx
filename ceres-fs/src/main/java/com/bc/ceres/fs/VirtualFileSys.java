package com.bc.ceres.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


public class VirtualFileSys implements FileNodeFactory {

    private Dir root = new Dir(null, "/");
    private Dir cwd = root;

    public VirtualFileSys() {
    }

    public FileNode createFileNode(String path) {
        return new VirtualFileNode(this, path);
    }

    public String getCwd() {
        return cwd.toString();
    }

    public void setCwd(String path) {
        Node node = findNode(path);
        if (node.isDirectory()) {
            cwd = (Dir) node;
        }
    }

    boolean isAbsolute(String path) {
        return path.startsWith("/") || path.startsWith("\\");
    }

    boolean isDirectory(String path) {
        return findNode(path).isDirectory();
    }

    boolean isFile(String path) {
        return findNode(path).isFile();
    }

    boolean exists(String path) {
        return findNode(path).exists();
    }

    Node findNode(String path) {
        Dir dir = isAbsolute(path) ? root : cwd;
        Node node = dir;
        List<String> names = tokenizePath(path);
        for (String name : names) {
            node = dir.getNode(name);
            if (node.isDirectory()) {
                dir = (Dir) node;
            } else if (!node.exists()) {
                break;
            }
        }
        return node;
    }

    List<String> tokenizePath(String path) {
        ArrayList<String> paths = new ArrayList<String>(8);
        StringTokenizer st = new StringTokenizer(path, "/\\", false);
        while (st.hasMoreElements()) {
            String name = (String) st.nextElement();
            paths.add(name);
        }
        return paths;
    }

    synchronized InputStream openInputStream(String path) throws IOException {
        return findNode(path).openInputStream();
    }

    synchronized OutputStream openOutputStream(String path) throws IOException {
        return findNode(path).openOutputStream();
    }

    boolean mkdirs(String path) {
        Dir dir = isAbsolute(path) ? root : cwd;
        List<String> names = tokenizePath(path);
        for (String name : names) {
            Node node = dir.getNode(name);
            if (!node.exists()) {
                dir = new Dir(dir, name);
            } else if (node.isDirectory()) {
                dir = (Dir) node;
            } else {
                return false;
            }
        }
        return true;
    }

    boolean mkdir(String path) {
        Node node = findNode(path);
        if (!node.exists()) {
            new Dir(node.getParent(), node.getName());
            return true;
        }
        return false;
    }

    boolean createNewFile(String path) {
        Node node = findNode(path);
        if (!node.exists()) {
            new File(node.getParent(), node.getName());
            return true;
        }
        return false;
    }

    String getName(String path) {
        if (path.equals("/") || path.equals("")) {
            return path;
        }
        int i = path.lastIndexOf('/');
        if (i >= 0) {
            return path.substring(i + 1);
        }
        return path;
    }

    String getParent(String path) {
        if (path.equals("/") || path.equals("")) {
            return null;
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            return path.substring(0, i);
        } else if (i == 0) {
            return "/";
        }
        return null;
    }

    String normalizePath(String path) {
        path = path.replace('\\', '/');
        int p;
        while ((p = path.indexOf("//")) != -1) {
            path = path.substring(0, p) + path.substring(p + 1);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    String[] list(String path) {
        Node node = findNode(path);
        if (node.isDirectory()) {
            Dir dir = (Dir) node;
            return dir.getNodeNames();
        }
        return new String[0];
    }

    public boolean delete(String path) {
        Node node = findNode(path);
        if (node.exists()) {
            return node.getParent().removeNode(node.getName());
        }
        return false;
    }

    public abstract class Node {
        private Dir parent;
        private String name;

        protected Node(Dir parentDir, String name) {
            this.parent = parentDir;
            this.name = name;
        }

        public Dir getParent() {
            return parent;
        }

        public void setParent(Dir parent) {
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean exists() {
            return true;
        }

        public abstract boolean isDirectory();

        public abstract boolean isFile();

        public abstract InputStream openInputStream() throws IOException;

        public abstract OutputStream openOutputStream() throws IOException;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            Node node = this;
            do {
                sb.insert(0, node.getName());
                node = node.getParent();
                if (node != null && node.getParent() != null) {
                    sb.insert(0, '/');
                }
            } while (node != null);
            return sb.toString();
        }
    }

    public class Null extends Node {

        public Null(Dir parentDir, String name) {
            super(parentDir, name);
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            throw new IOException("Failed to create input stream.");
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new IOException("Failed to create output stream.");
        }
    }

    public class Dir extends Node {
        private Map<String, Node> nodeMap;

        public Dir(Dir parentDir, String name) {
            super(parentDir, name);
            nodeMap = new HashMap<String, Node>(8);
            if (parentDir != null) {
                parentDir.addNode(this);
            }
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        public Node getNode(String name) {
            Node node;
            if (name.equals(".")) {
                node = this;
            } else if (name.equals("..")) {
                node = getParent();
            } else {
                node = nodeMap.get(name);
            }
            return node != null ? node : new Null(this, name);
        }

        public void addNode(Node fileNode) {
            nodeMap.put(fileNode.getName(), fileNode);
        }

        public boolean removeNode(String name) {
            Node node = nodeMap.remove(name);
            if (node != null) {
                node.setParent(null);
                return true;
            }
            return false;
        }

        public String[] getNodeNames() {
            return nodeMap.keySet().toArray(new String[0]);
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(getContent());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new IOException("Failed to create output stream.");
        }

        private byte[] getContent() {
            StringBuilder sb = new StringBuilder(32);
            for (Node fileNode : nodeMap.values()) {
                sb.append(fileNode.getName());
                sb.append('\n');
            }
            return sb.toString().getBytes();
        }
    }

    public class File extends Node {

        private int readAccessCount;
        private boolean writeAccessLock;
        private byte[] bytes;

        public File(Dir parentDir, String name) {
            super(parentDir, name);
            bytes = new byte[0];
            if (parentDir != null) {
                parentDir.addNode(this);
            }
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new FIS(this);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return new FOS(this);
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public boolean getWriteAccessLock() {
            return writeAccessLock;
        }

        public void setWriteAccessLock(boolean writeAccessLock) {
            this.writeAccessLock = writeAccessLock;
        }

        public int getReadAccessCount() {
            return readAccessCount;
        }

        public void incReadAccessCount() {
            readAccessCount++;
        }

        public void decReadAccessCount() {
            readAccessCount--;
        }
    }

    public static class FIS extends FilterInputStream {
        private File node;
        private boolean closed;

        public FIS(File node) {
            super(new ByteArrayInputStream(node.getBytes()));
            this.node = node;
            this.node.incReadAccessCount();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                super.close();
                node.decReadAccessCount();
                closed = true;
            }
        }
    }

    public static class FOS extends FilterOutputStream {
        private File node;
        private boolean closed;

        public FOS(File node) {
            super(new ByteArrayOutputStream());
            this.node = node;
            this.node.setWriteAccessLock(true);
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                super.close();
                node.setBytes(((ByteArrayOutputStream) out).toByteArray());
                node.setWriteAccessLock(false);
                closed = true;
            }
        }
    }
}

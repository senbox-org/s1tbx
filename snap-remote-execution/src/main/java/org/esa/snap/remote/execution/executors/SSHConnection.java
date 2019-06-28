package org.esa.snap.remote.execution.executors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jcoravu on 21/1/2019.
 */
public class SSHConnection {

    private final String host;
    private final int portNumber;
    private final String username;
    private final String password;
    private final AtomicBoolean stopped;

    private Session session;
    private Channel channel;

    public SSHConnection(String host, int portNumber, String username, String password) {
        this.host = host;
        this.portNumber = portNumber;
        this.username = username;
        this.password = password;
        this.stopped = new AtomicBoolean(false);
    }

    public void connect() throws JSchException {
        JSch jSch = new JSch();
        this.session = jSch.getSession(this.username, this.host, this.portNumber);
        this.session.setUserInfo(new UserInfo(this.password));
        this.session.setPassword(this.password.getBytes());
        this.session.setConfig("StrictHostKeyChecking", "no");
        this.session.connect();
    }

    public void disconnect() {
        if (this.session != null) {
            try {
                this.session.disconnect();
            } finally {
                this.session = null;
            }
        }
    }

    private void checkConnectedSession() throws JSchException {
        if (this.session == null) {
            throw new NullPointerException("The session is not created.");
        } else if (!this.session.isConnected()) {
            throw new JSchException("The session is not connected.");
        }
    }

    public boolean isStopped() {
        return this.stopped.get();
    }

    public int executeWindowsCommand(String command, OutputConsole outputConsole) throws JSchException, IOException {
        try {
            InputStream inputStream = null;
            try {
                synchronized (this) {
                    this.channel = this.session.openChannel("exec");
                    ChannelExec channelExec = (ChannelExec)channel;
                    channelExec.setCommand(command);
                    this.channel.setInputStream(null);
                    channelExec.setPty(false); // do not allocate the pseudo-terminal
                    channelExec.setErrStream(new ChannelErrorStream(outputConsole));
                    inputStream = this.channel.getInputStream();
                    this.channel.connect();
                }

                return readChannelResults(inputStream, null, outputConsole);
            } finally {
                ProcessExecutor.closeStream(inputStream);
            }
        } finally {
            disconnectChannel();
        }
    }

    public int executeLinuxCommand(String command, String superUserPassword, OutputConsole outputConsole) throws JSchException, IOException {
        checkConnectedSession();

        try {
            boolean asSuperUser = (superUserPassword != null);
            InputStream inputStream = null;
            try {
                synchronized (this) {
                    this.channel = this.session.openChannel("exec");
                    ChannelExec channelExec = (ChannelExec)this.channel;
                    if (asSuperUser) {
                        command = "sudo -S -p '' " + command;
                    }
                    channelExec.setCommand(command);
                    this.channel.setInputStream(null);
                    channelExec.setPty(asSuperUser); // allocate the pseudo-terminal to write the user password
                    channelExec.setErrStream(new ChannelErrorStream(outputConsole));
                    inputStream = this.channel.getInputStream();
                    this.channel.connect();
                }
                if (asSuperUser) {
                    OutputStream outputStream = this.channel.getOutputStream();
                    outputStream.write((superUserPassword + "\n").getBytes());
                    outputStream.flush();
                }
                return readChannelResults(inputStream, superUserPassword, outputConsole);
            } finally {
                ProcessExecutor.closeStream(inputStream);
            }
        } finally {
            disconnectChannel();
        }
    }

    private void disconnectChannel() {
        synchronized (this) {
            if (this.channel != null) {
                try {
                    this.channel.disconnect();
                } finally {
                    this.channel = null;
                }
            }
        }
    }

    public void stopRunningCommand() throws Exception {
        this.stopped.set(true);

        synchronized (this) {
            if (this.channel != null && !this.channel.isClosed() && this.channel.isConnected()) {
                this.channel.sendSignal("2");
            }
        }
    }

    private int readChannelResults(InputStream inputStream, String superUserPassword, OutputConsole outputConsole) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        try {
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            try {
                String line;
                while (!isStopped()) {
                    while (!isStopped() && (line = bufferedReader.readLine()) != null) {
                        if (!"".equals(line.trim())) {
                            if (superUserPassword == null) {
                                outputConsole.appendNormalMessage(line);
                            } else {
                                if (!superUserPassword.equals(line)) {
                                    outputConsole.appendNormalMessage(line);
                                }
                            }
                        }
                    }
                    if (this.channel.isClosed()) {
                        if (inputStream.available() > 0) {
                            continue;
                        }
                        break;
                    }
                }
                return this.channel.getExitStatus();
            } finally {
                ProcessExecutor.closeStream(bufferedReader);
            }
        } finally {
            ProcessExecutor.closeStream(inputStreamReader);
        }
    }

    private static class ChannelErrorStream extends ByteArrayOutputStream {

        private final OutputConsole outputConsole;

        private ChannelErrorStream(OutputConsole outputConsole) {
            this.outputConsole = outputConsole;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            String message = new String(b, off, len).replaceAll("\n", "");
            if (message.length() > 0) {
                this.outputConsole.appendErrorMessage(message);
            }
        }
    }

    private static class UserInfo implements com.jcraft.jsch.UserInfo {

        private final String password;

        private UserInfo(String password) {
            this.password = password;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String s) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return false;
        }

        @Override
        public boolean promptYesNo(String s) {
            return false;
        }

        @Override
        public void showMessage(String s) {
        }
    }
}

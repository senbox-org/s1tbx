package org.esa.snap.remote.execution.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.RemoteExecutionOp;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;

/**
 * Created by jcoravu on 3/1/2019.
 */
public class RemoteMachinePropertiesConverter implements Converter<RemoteMachineProperties[]> {

    public RemoteMachinePropertiesConverter() {
    }

    @Override
    public Class<? extends RemoteMachineProperties[]> getValueType() {
        return RemoteMachineProperties[].class;
    }

    @Override
    public RemoteMachineProperties[] parse(String text) throws ConversionException {
        // <machine host='192.168.61.19' port='22' operating-system='Linux' username='jean' password='jean' shared-folder='/home/jean/shared-ssh' />
        StringBuilder xml = new StringBuilder();
        xml.append("<remote-machines>")
                .append(text)
                .append("</remote-machines>");
        XppDom rootNode = RemoteExecutionOp.buildDom(xml.toString());
        if (rootNode.getName().equalsIgnoreCase("remote-machines")) {
            int childCount = rootNode.getChildCount();
            RemoteMachineProperties[] result = new RemoteMachineProperties[childCount];
            for (int i=0; i<childCount; i++) {
                XppDom childNote = rootNode.getChild(i);
                if (childNote.getName().equalsIgnoreCase("machine")) {
                    String hostName = childNote.getAttribute("host");
                    String portNumberAsString = childNote.getAttribute("port");
                    String operatingSystemName = childNote.getAttribute("operating-system");
                    String username = childNote.getAttribute("username");
                    String password = childNote.getAttribute("password");
                    String sharedFolderPath = childNote.getAttribute("shared-folder");
                    String gptFilePath = childNote.getAttribute("gpt-file");
                    int portNumber = Integer.parseInt(portNumberAsString);
                    result[i] = new RemoteMachineProperties(hostName, portNumber, username, password, operatingSystemName, sharedFolderPath);
                    result[i].setGPTFilePath(gptFilePath);
                } else {
                    throw new IllegalArgumentException("Unknown child tag name '"+childNote.getName()+"'.");
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unknown root tag name '"+rootNode.getName()+"'.");
        }
    }

    @Override
    public String format(RemoteMachineProperties[] value) {
        StringBuilder xml = new StringBuilder();
        for  (int i=0; i<value.length; i++) {
            xml.append("<machine host='")
               .append(value[i].getHostName())
               .append("' port='")
               .append(value[i].getPortNumber())
               .append("' operating-system='")
               .append(value[i].getOperatingSystemName())
               .append("' username='")
               .append(value[i].getUsername())
               .append("' password='")
               .append(value[i].getPassword())
               .append("' shared-folder='")
               .append(value[i].getSharedFolderPath());
            if (!StringUtils.isBlank(value[i].getGPTFilePath())) {
                xml.append("' gpt-file='")
                   .append(value[i].getGPTFilePath());
            }
            xml.append("' />");
        }
        return xml.toString();
    }
}

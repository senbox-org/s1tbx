package org.esa.snap.core.util.kmz;

import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 */
public class ExtendedData {

    private List<Data> dataList;

    public ExtendedData() {
        this.dataList = new ArrayList<>();
    }

    void add(String name, String value) {
        dataList.add(new Data(name, null, value));
    }


    void add(String name, String displayName, String value) {
        dataList.add(new Data(name, displayName, value));
    }

    void add(Data item) {
        dataList.add(item);
    }

    public void createKml(StringBuilder sb) {
        if (!dataList.isEmpty()) {
            sb.append("<ExtendedData>");
            for (Data data : dataList) {
                sb.append(String.format("<Data name=\"%s\">", data.name));
                if (StringUtils.isNotNullAndNotEmpty(data.displayName)) {
                    sb.append(String.format("<displayName>%s</displayName>", data.displayName));
                }
                sb.append(String.format("<value>%s</value>", data.value));
                sb.append("</Data>");
            }
            sb.append("</ExtendedData>");
        }
    }

    public static ExtendedData create(Map<String, Object> extraData) {
        ExtendedData extendedData = new ExtendedData();
        for (Map.Entry<String, Object> entry : extraData.entrySet()) {
            extendedData.add(entry.getKey(), String.valueOf(entry.getValue()));

        }
        return extendedData;
    }

    private static class Data {

        public Data(String name, String displayName, String value) {
            this.name = name;
            this.displayName = displayName;
            this.value = value;
        }

        String name;
        String displayName;
        String value;
    }

}

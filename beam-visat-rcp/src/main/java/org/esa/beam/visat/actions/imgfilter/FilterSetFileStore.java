package org.esa.beam.visat.actions.imgfilter;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;
import org.esa.beam.visat.actions.imgfilter.model.FilterSetStore;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman
 */
public class FilterSetFileStore implements FilterSetStore {

    final File filtersDir;

    public FilterSetFileStore(File filtersDir) {
        this.filtersDir = filtersDir;
    }

    public List<FilterSet> loadFilterSetModels() throws IOException {
        XStream xStream = FilterSet.createXStream();

        File[] files = filtersDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        });
        ArrayList<FilterSet> list = new ArrayList<>();
        if (files == null) {
            return list;
        }
        IOException ioe = null;
        for (File file : files) {
            try {
                FilterSet filterSet = new FilterSet();
                xStream.fromXML(file, filterSet);
                list.add(filterSet);
            } catch (Exception e) {
                ioe = new IOException(e);
            }
        }

        if (ioe != null && list.isEmpty()) {
            throw ioe;
        }

        return list;
    }

    @Override
    public void storeFilterSetModel(FilterSet filterSet) throws IOException {
        if (!filtersDir.exists() && !filtersDir.mkdirs()) {
            throw new IOException("Failed to create directory\n" + filtersDir);
        }
        File file = new File(filtersDir, filterSet.getName().toLowerCase() + "-filters.xml");
        XStream xStream = FilterSet.createXStream();
        try (FileWriter fileWriter = new FileWriter(file)) {
            xStream.toXML(filterSet, fileWriter);
        }
    }


}

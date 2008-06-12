package org.esa.beam.dataio.geotiff;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A directroy entry set implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffDirectoryEntrySet {

    private final SortedMap _entryMap;

    public TiffDirectoryEntrySet() {
        _entryMap = new TreeMap();
    }

    public void set(final TiffDirectoryEntry entry) {
        final Integer key = getKey(entry);
        _entryMap.put(key, entry);
    }

    public TiffDirectoryEntry[] getEntries() {
        return (TiffDirectoryEntry[]) _entryMap.values().toArray(new TiffDirectoryEntry[_entryMap.size()]);
    }

    public TiffDirectoryEntry getEntry(final TiffShort tag) {
        final Integer key = getKey(tag);
        return (TiffDirectoryEntry) _entryMap.get(key);
    }

    private static Integer getKey(final TiffDirectoryEntry entry) {
        return getKey(entry.getTag());
    }

    private static Integer getKey(final TiffShort tag) {
        return tag.getValue();
    }
}

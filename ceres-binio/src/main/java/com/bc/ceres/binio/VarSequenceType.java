package com.bc.ceres.binio;

import java.io.IOException;

public interface VarSequenceType extends SequenceType {
    SequenceType resolve(CollectionData parent) throws IOException;
}

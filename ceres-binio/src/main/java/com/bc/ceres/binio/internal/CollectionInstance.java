package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;

import java.io.IOException;


interface CollectionInstance extends MemberInstance, CollectionData {

    boolean isSizeResolved(int index);

    void resolveSize(int index) throws IOException;
}
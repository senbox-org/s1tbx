/*
 * $Id: InputStreamObserver.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.io;

public interface InputStreamObserver {
    void onReadStarted(long numBytesTotal);

    void onReadProgress(long numBytesRead);

    void onReadEnded();

    boolean isReadingCanceled();
}

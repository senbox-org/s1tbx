package org.esa.pfa.db;

import org.esa.pfa.fe.op.Patch;

/**
 * Created by luis on 09/02/14.
 */
public interface QueryInterface {

     Patch[] getRandomPatches(final int numPatches);
}

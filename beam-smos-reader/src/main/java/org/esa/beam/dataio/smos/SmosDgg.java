package org.esa.beam.dataio.smos;

public class SmosDgg {
    private static final int A = 1000000;
    private static final int B = 262144;

    public static int smosGridPointIdToDggridSeqnum(int smosId) {
        return smosId < A ? smosId : B * ((smosId - 1) / A) + ((smosId - 1) % A) + 2;
    }
}

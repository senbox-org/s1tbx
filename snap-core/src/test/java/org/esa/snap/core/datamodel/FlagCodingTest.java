package org.esa.snap.core.datamodel;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlagCodingTest {

    @Test
    public void mixedSingleFlagsAndBitFieldFlags() throws IOException {
        final String sf1 = "SingleFlag_1";
        final String sf2 = "SingleFlag_2";
        final String bff0 = "BitfieldFlag_0";
        final String bff1 = "BitfieldFlag_1";
        final String bff2 = "BitfieldFlag_2";
        final String bff3 = "BitfieldFlag_3";

        final int bitFieldFlagMask = 0b1010; // = 10

        final FlagCoding flagCoding = new FlagCoding("testCoding");
        flagCoding.addFlag(sf1, 1, ""); // 1 = 0b0001
        flagCoding.addFlag(sf2, 4, ""); // 4 = 0b0100

        final int flagValue0 = 0b0000; // = 0x0 = 0
        final int flagValue1 = 0b0010; // = 0x2 = 2
        final int flagValue2 = 0b1000; // = 0x8 = 8
        final int flagValue3 = 0b1010; // = 0xA = 10

        flagCoding.addFlag(bff0, bitFieldFlagMask, flagValue0, "");
        flagCoding.addFlag(bff1, bitFieldFlagMask, flagValue1, "");
        flagCoding.addFlag(bff2, bitFieldFlagMask, flagValue2, "");
        flagCoding.addFlag(bff3, bitFieldFlagMask, flagValue3, "");

        assertThat(flagCoding.getFlagMask(sf1), is(1));
        assertThat(flagCoding.getFlagMask(sf2), is(4));

        assertThat(flagCoding.getFlagMask(bff0), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlagMask(bff1), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlagMask(bff2), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlagMask(bff3), is(bitFieldFlagMask));

        assertThat(flagCoding.getFlag(sf1).getData().getNumElems(), is(1));
        assertThat(flagCoding.getFlag(sf2).getData().getNumElems(), is(1));

        assertThat(flagCoding.getFlag(bff0).getData().getNumElems(), is(2));
        assertThat(flagCoding.getFlag(bff1).getData().getNumElems(), is(2));
        assertThat(flagCoding.getFlag(bff2).getData().getNumElems(), is(2));
        assertThat(flagCoding.getFlag(bff3).getData().getNumElems(), is(2));

        assertThat(flagCoding.getFlag(sf1).getData().getElemIntAt(0), is(1));
        assertThat(flagCoding.getFlag(sf2).getData().getElemIntAt(0), is(4));

        assertThat(flagCoding.getFlag(bff0).getData().getElemIntAt(0), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlag(bff1).getData().getElemIntAt(0), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlag(bff2).getData().getElemIntAt(0), is(bitFieldFlagMask));
        assertThat(flagCoding.getFlag(bff3).getData().getElemIntAt(0), is(bitFieldFlagMask));

        assertThat(flagCoding.getFlag(bff0).getData().getElemIntAt(1), is(flagValue0));
        assertThat(flagCoding.getFlag(bff1).getData().getElemIntAt(1), is(flagValue1));
        assertThat(flagCoding.getFlag(bff2).getData().getElemIntAt(1), is(flagValue2));
        assertThat(flagCoding.getFlag(bff3).getData().getElemIntAt(1), is(flagValue3));

    }
}
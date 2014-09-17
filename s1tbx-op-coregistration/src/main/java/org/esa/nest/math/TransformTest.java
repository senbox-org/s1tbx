/**
 * JWave - Java implementation of wavelet transform algorithms
 *
 * Copyright 2008-2014 Christian Scheiblich
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * This file is part of JWave.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 23.05.2008 17:42:23
 *
 */
package org.esa.nest.math;

import static org.junit.Assert.*;
import org.esa.nest.math.datatypes.Complex;
import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.exceptions.JWaveFailure;
import org.esa.nest.math.transforms.FastWaveletTransform;
import org.esa.nest.math.transforms.wavelets.Haar1;
import org.esa.nest.math.transforms.wavelets.Haar1Orthogonal;
import org.esa.nest.math.transforms.wavelets.Wavelet;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal11;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal13;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal15;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal22;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal24;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal26;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal28;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal31;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal33;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal35;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal37;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal39;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal44;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal55;
import org.esa.nest.math.transforms.wavelets.biorthogonal.BiOrthogonal68;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet1;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet2;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet3;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet4;
import org.esa.nest.math.transforms.wavelets.coiflet.Coiflet5;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies10;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies11;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies12;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies13;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies14;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies15;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies16;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies17;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies18;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies19;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies2;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies20;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies3;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies4;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies5;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies6;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies7;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies8;
import org.esa.nest.math.transforms.wavelets.daubechies.Daubechies9;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre1;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre2;
import org.esa.nest.math.transforms.wavelets.legendre.Legendre3;
import org.esa.nest.math.transforms.wavelets.other.CDF53;
import org.esa.nest.math.transforms.wavelets.other.DiscreteMayer;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet10;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet11;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet12;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet13;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet14;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet15;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet16;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet17;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet18;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet19;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet2;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet20;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet3;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet4;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet5;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet6;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet7;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet8;
import org.esa.nest.math.transforms.wavelets.symlets.Symlet9;

import org.junit.Test;

/**
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 10.02.2014 21:32:22
 */
public class TransformTest {

  /**
   * Test method for {@link math.jwave.Transform#forward(double[])} and
   * {@link math.jwave.Transform#reverse(double[])}..
   */
  @Test public void testRounding( ) {

    System.out.println( "" );
    System.out.println( "testRounding" );
    System.out.println( "" );

    int arrTimeLength = 1024; // 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, ..

    double[ ] arrTime = new double[ arrTimeLength ];

    for( int i = 0; i < arrTime.length; i++ )
      arrTime[ i ] = 1.; // for calculating rounding error and to allow work for filters ;-) 

    double delta = 1.e-10; // define a request rounding error that the test checks to be held

    try {

      System.out
          .println( "testRounding Haar1 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Haar1( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Haar1Orthogonal - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Haar1Orthogonal( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies2 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies2( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies3 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies3( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies4 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies4( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies5 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies5( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies6 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies6( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies7 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies7( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies8 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies8( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies9 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies9( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies10 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies10( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies11 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies11( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies12 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies12( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies13 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies13( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies14 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies14( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies15 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies15( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies16 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies16( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies17 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies17( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies18 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies18( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies19 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies19( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Daubechies20 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Daubechies20( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Legendre1 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Legendre1( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Legendre2 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Legendre2( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Legendre3 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Legendre3( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Coiflet1 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Coiflet1( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Coiflet2 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Coiflet2( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Coiflet3 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Coiflet3( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Coiflet4 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Coiflet4( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Coiflet5 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Coiflet5( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet2 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet2( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet3 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet3( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet4 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet4( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet5 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet5( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet6 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet6( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet7 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet7( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet8 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet8( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet9 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet9( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet10 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet10( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet11 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet11( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet12 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet12( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet13 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet13( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet14 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet14( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet15 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet15( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet16 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet16( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet17 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet17( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet18 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet18( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet19 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet19( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding Symlet20 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new Symlet20( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal11 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal11( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal13 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal13( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal15 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal15( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal22 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal22( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal24 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal24( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal26 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal26( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal28 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal28( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal31 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal31( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal33 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal33( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal35 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal35( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal37 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal37( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal39 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal39( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal44 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal44( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal55 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal55( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding BiOrthogonal68 - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new BiOrthogonal68( ), delta );
      System.out.println( "" );

      System.out
          .println( "testRounding DiscreteMayer - 1000 transforms => rounding error: "
              + delta );
      testFastWaveletTransformRounding( arrTime, new DiscreteMayer( ), 1.e-2 );
      System.out.println( "" );

      //      System.out
      //          .println( "testRounding LeGall 5/3 - 1000 transforms => rounding error: "
      //              + delta );
      //      testFastWaveletTransformRounding( arrTime, new CDF53( ), 1.e-2 );
      //      System.out.println( "" );      

      //      System.out
      //          .println( "testRounding Battle23 - 1000 transforms => rounding error: "
      //              + delta );
      //      testFastWaveletTransformRounding( arrTime, new Battle23( ), delta );
      //      System.out.println( "" );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testRounding

  /**
   * Test method to check the rounding error of several forward and reverse
   * transforms using the Fast Wavelet Transform algorithm and any given Wavelet
   * object as input.
   * 
   * @date 10.02.2010 10:28:00
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @throws JWaveException
   */
  public void testFastWaveletTransformRounding( double[ ] arr, Wavelet wavelet,
      double delta ) throws JWaveException {

    long noOfSteps = 1000;

    double[ ] arrTime = arr;

    showTime( arrTime );

    double[ ] arrTimeRound = new double[ arrTime.length ];
    for( int c = 0; c < arrTime.length; c++ )
      arrTimeRound[ c ] = arrTime[ c ];

    Transform t = new Transform( new FastWaveletTransform( wavelet ) );

    System.out.println( "" );
    System.out.println( "" );
    System.out.print( "Performing: " + noOfSteps
        + " forward and reverse transforms ..." );

    for( long s = 0; s < noOfSteps; s++ )
      arrTimeRound = t.reverse( t.forward( arrTimeRound ) );

    System.out.println( "" );
    System.out.println( "" );

    assertArray( arrTime, arrTimeRound, delta );

    System.out.println( "Input ..." );
    showTime( arrTime );
    System.out.println( "" );

    System.out.println( "Result ..." );
    showTime( arrTimeRound );
    System.out.println( "" );

    double[ ] arrTimeErrorAbs = new double[ arrTimeRound.length ];
    for( int c = 0; c < arrTimeRound.length; c++ )
      arrTimeErrorAbs[ c ] = Math.abs( arrTimeRound[ c ] - arrTime[ c ] );

    System.out.println( "Absolute error" );
    showTime( arrTimeErrorAbs );
    System.out.println( "" );

    double[ ] arrTimeErrorRel = new double[ arrTimeRound.length ];
    for( int c = 0; c < arrTimeRound.length; c++ )
      arrTimeErrorRel[ c ] =
          Math.abs( ( arrTimeRound[ c ] - arrTime[ c ] ) * 100. / arrTime[ c ] );

    System.out.println( "Relative error [%] ..." );
    showTime( arrTimeErrorRel );
    System.out.println( "" );

  } // testFastWaveletTransformRounding

  /**
   * Test method for {@link math.jwave.Transform#forward(double[])}.
   */
  @Test public void testForwardDoubleArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "forward 1-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      double[ ] arrTime = { 1., 1., 1., 1. };

      showTime( arrTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      // Transform t = new Transform( new FastWaveletTransform( new Haar1Orthogonal( ) ) );
      // Transform t = new Transform( new FastWaveletTransform( new Daubechies20( ) ) );

      double[ ] arrHilb = t.forward( arrTime );

      showHilb( arrHilb );

      double[ ] expected = { 2., 0., 0., 0. }; // orthonormal Hilbert space
      // double[ ] expected = { 4., 0., 0., 0. }; // orthogonal Hilbert space for Haar1Orthogonal
      assertArray( expected, arrHilb, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "forward 1-D method " + "using Haar1 Wavelet " + "and a long array" );

    try {

      double delta = 1.e-12;

      double[ ] arrTime = { // array of length 64
          1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1. };

      showTime( arrTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ] arrHilb = t.forward( arrTime );

      showHilb( arrHilb );

      double[ ] expected = { // array of length 64
          8., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0. }; // orthonormal Hilbert space
      assertArray( expected, arrHilb, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

    System.out.println( "" );
    System.out
        .println( "Testing the Fast Wavelet Transform " + "forward 1-D method "
            + "using Haar1 Wavelet " + "and a random array" );

    try {

      double delta = 1.e-12;

      double[ ] arrTime = { 1.2, 2.3, 3.4, 4.5, 5.4, 4.3, 3.2, 2.1 };

      showTime( arrTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ] arrHilb = t.forward( arrTime );

      showHilb( arrHilb );

      double[ ] expected =
          { 9.333809511662427, -1.2727922061357857, -2.1999999999999997, 2.2,
              -0.7778174593052021, -0.7778174593052025, 0.7778174593052025,
              0.7778174593052023 }; // orthonormal Hilbert space
      assertArray( expected, arrHilb, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testForwardDoubleArray

  /**
   * Test method for {@link math.jwave.Transform#reverse(double[])}.
   */
  @Test public void testReverseDoubleArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 1-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1e-12;

      double[ ] arrHilb = { 2., 0., 0., 0. }; // orthonormal Hilbert space
      // double[ ] arrHilb = { 4., 0., 0., 0. }; // orthogonal Hilbert space for Haar1Orthogonal

      showHilb( arrHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ] arrTime = t.reverse( arrHilb );

      showTime( arrTime );

      double[ ] expected = { 1., 1., 1., 1. };
      assertArray( expected, arrTime, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 1-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1e-12;

      double[ ] arrHilb = {  // array of length 64
          8., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
              0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0. }; // orthonormal Hilbert space

      showHilb( arrHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ] arrTime = t.reverse( arrHilb );

      showTime( arrTime );

      double[ ] expected = {  // array of length 64
          1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.

          };
      assertArray( expected, arrTime, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

    System.out.println( "" );
    System.out
        .println( "Testing the Fast Wavelet Transform " + "reverse 1-D method "
            + "using Haar1 Wavelet " + "and a random array" );

    try {

      double delta = 1e-12;

      double[ ] arrHilb =
          { 9.333809511662427, -1.2727922061357857, -2.1999999999999997, 2.2,
              -0.7778174593052021, -0.7778174593052025, 0.7778174593052025,
              0.7778174593052023 }; // orthonormal Hilbert space

      showHilb( arrHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ] arrTime = t.reverse( arrHilb );

      showTime( arrTime );

      double[ ] expected = { 1.2, 2.3, 3.4, 4.5, 5.4, 4.3, 3.2, 2.1 };
      assertArray( expected, arrTime, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testReverseDoubleArray

  /**
   * Test method for {@link math.jwave.Transform#decompose(double[])}.
   */
  @Test public void testDecomposeDoubleArray( ) {

    // run this part for any wavelet that is available in JWave!

    try {

      double delta = 1.e-8;
      
      int noOfWavelets = 20;

      Wavelet[ ] arrOfWaveletObjects = new Wavelet[ noOfWavelets ];

      arrOfWaveletObjects[ 0 ] = new Haar1( );
      arrOfWaveletObjects[ 1 ] = new Daubechies4( );
      arrOfWaveletObjects[ 2 ] = new Daubechies6( );
      arrOfWaveletObjects[ 3 ] = new Daubechies8( );
      arrOfWaveletObjects[ 4 ] = new Daubechies10( );
      arrOfWaveletObjects[ 5 ] = new Daubechies12( );
      arrOfWaveletObjects[ 6 ] = new Daubechies14( );
      arrOfWaveletObjects[ 7 ] = new Daubechies16( );
      arrOfWaveletObjects[ 8 ] = new Daubechies18( );
      arrOfWaveletObjects[ 9 ] = new Daubechies20( );
      arrOfWaveletObjects[ 10 ] = new Coiflet3( );
      arrOfWaveletObjects[ 11 ] = new Coiflet5( );
      arrOfWaveletObjects[ 12 ] = new Symlet4( );
      arrOfWaveletObjects[ 13 ] = new Symlet8( );
      arrOfWaveletObjects[ 14 ] = new Symlet12( );
      arrOfWaveletObjects[ 15 ] = new Symlet16( );
      arrOfWaveletObjects[ 16 ] = new Symlet20( );
      arrOfWaveletObjects[ 17 ] = new BiOrthogonal13( );
      arrOfWaveletObjects[ 18 ] = new BiOrthogonal39( );
      arrOfWaveletObjects[ 19 ] = new BiOrthogonal68( );

      for( int w = 0; w < noOfWavelets; w++ ) {

        Wavelet wavelet = arrOfWaveletObjects[ w ];

        System.out.println( "" );
        System.out.println( "Testing the Fast Wavelet Transform "
            + "decompse 1-D method " + "using " + wavelet.getName( ) );

        double[ ] arrTime = { 1., 1., 1., 1. };

        showTime( arrTime );

        Transform t = new Transform( new FastWaveletTransform( wavelet ) );

        double[ ][ ] matDeComp = t.decompose( arrTime );

        showHilb( matDeComp );

        double sqrt2 = Math.sqrt( 2. );

        double[ ][ ] expected =
            { { 1., 1., 1., 1., },
              { sqrt2, sqrt2, 0., 0. }, 
              { 2., 0., 0., 0. } 
            }; // orthonormal Hilbert space

        assertMatrix( expected, matDeComp, delta );

        double[ ] arrTimeReComp = t.recompose( matDeComp );

        showTime( arrTimeReComp );

        assertArray( arrTime, arrTimeReComp, delta );

        System.out.println( "" );
        System.out.println( "Testing the Fast Wavelet Transform "
            + "forward 1-D method " + "using " + wavelet.getName( )
            + " and a long array" );

        double[ ] arrTime64 = { // array of length 64
            1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
                1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
                1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
                1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1. };

        showTime( arrTime );

        double[ ][ ] matDeComp64 = t.decompose( arrTime64 );

        showHilb( matDeComp64 );

        double d1sqrt2 = 1 * sqrt2; // symbolic one times square root of 2
        double d2sqrt2 = 2. * d1sqrt2; // 2 times square root of two
        double d4sqrt2 = 2. * d2sqrt2; // 4 times square root of two

        double[ ][ ] expected64 =
            { { // array of length 64
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.,
              1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1. },
                { d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2,
                    d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2,
                    d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2,
                    d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2,
                    d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2, d1sqrt2,
                    d1sqrt2, d1sqrt2, 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0. },

                { 2., 2., 2., 2., 2., 2., 2., 2., 2., 2., 2., 2., 2., 2., 2.,
                    2., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0. },

                { d2sqrt2, d2sqrt2, d2sqrt2, d2sqrt2, d2sqrt2, d2sqrt2,
                    d2sqrt2, d2sqrt2, 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0. },

                { 4., 4., 4., 4., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0. },

                { d4sqrt2, d4sqrt2, 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0. },

                { 8., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.,
                    0., 0., 0., 0. },

            }; // orthonormal Hilbert space

        assertMatrix( expected64, matDeComp64, delta );

        double[ ] arrTimeReComp64 = t.recompose( matDeComp64 );

        showTime( arrTimeReComp64 );

        assertArray( arrTime64, arrTimeReComp64, delta );

      } // w

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testDecomposeDoubleArray

  /**
   * Test method for {@link math.jwave.Transform#forward(Complex[])}.
   */
  @Test public void testForwardComplexArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "forward 1-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      int arrTimeLength = 8;

      Complex[ ] arrTime = new Complex[ arrTimeLength ];

      for( int i = 0; i < arrTimeLength; i++ )
        arrTime[ i ] = new Complex( 1., 1. );

      showTime( arrTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      // Transform t = new Transform( new FastWaveletTransform( new Haar1Orthogonal( ) ) );
      // Transform t = new Transform( new FastWaveletTransform( new Daubechies20( ) ) );

      Complex[ ] arrHilb = t.forward( arrTime );

      showHilb( arrHilb );

      Complex[ ] expected = new Complex[ arrTimeLength ];

      for( int i = 0; i < arrTimeLength; i++ )
        expected[ i ] = new Complex( 0., 0. ); // { 0., 0., 0., .. }

      expected[ 0 ].setReal( 4. );  // { 4., 0., 0., .. }

      assertArray( expected, arrHilb, delta );

    } catch( JWaveFailure e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testForwardComplexArray

  /**
   * Test method for {@link math.jwave.Transform#reverse(Complex[])}.
   */
  @Test public void testReverseComplexArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 1-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1e-12;

      int arrTimeLength = 8;

      Complex[ ] arrHilb = new Complex[ arrTimeLength ];

      for( int i = 0; i < arrTimeLength; i++ )
        arrHilb[ i ] = new Complex( 0., 0. ); // { 0., 0., 0., .. }

      arrHilb[ 0 ].setReal( 4. );  // { 4., 0., 0., .. }

      showHilb( arrHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      Complex[ ] arrTime = t.reverse( arrHilb );

      showTime( arrTime );

      Complex[ ] expected = new Complex[ arrTimeLength ];

      for( int i = 0; i < arrTimeLength; i++ )
        expected[ i ] = new Complex( 1., 1. );

      assertArray( expected, arrTime, delta );

    } catch( JWaveFailure e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 1-D method " + "using Haar1 Wavelet" );

  } // testReverseComplexArray

  /**
   * Test method for {@link math.jwave.Transform#forward(double[][])}.
   */
  @Test public void testForwardDoubleArrayArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "forward 2-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      double[ ][ ] matrixTime =
          { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
              { 1., 1., 1., 1. } };

      showTime( matrixTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ][ ] matrixHilb = t.forward( matrixTime );

      showHilb( matrixHilb );

      double[ ][ ] expected =
          { { 4., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
              { 0., 0., 0., 0. } };
      assertMatrix( expected, matrixHilb, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testForwardDoubleArrayArray

  /**
   * Test method for {@link math.jwave.Transform#reverse(double[][])}.
   */
  @Test public void testReverseDoubleArrayArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 2-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      double[ ][ ] matrixHilb =
          { { 4., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
              { 0., 0., 0., 0. } };

      showHilb( matrixHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ][ ] matrixTime = t.reverse( matrixHilb );

      showTime( matrixTime );

      double[ ][ ] expected =
          { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
              { 1., 1., 1., 1. } };
      assertMatrix( expected, matrixTime, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  }

  /**
   * Test method for {@link math.jwave.Transform#forward(double[][][])}.
   */
  @Test public void testForwardDoubleArrayArrayArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "forward 3-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      double[ ][ ][ ] spaceTime =
          {
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } } };

      showTime( spaceTime );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ][ ][ ] spaceHilb = t.forward( spaceTime );

      showHilb( spaceHilb );

      double[ ][ ][ ] expected =
          {
              { { 8., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } } };
      assertSpace( expected, spaceHilb, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testForwardDoubleArrayArrayArray

  /**
   * Test method for {@link math.jwave.Transform#reverse(double[][][])}.
   */
  @Test public void testReverseDoubleArrayArrayArray( ) {

    System.out.println( "" );
    System.out.println( "Testing the Fast Wavelet Transform "
        + "reverse 3-D method " + "using Haar1 Wavelet" );

    try {

      double delta = 1.e-12;

      double[ ][ ][ ] spaceHilb =
          {
              { { 8., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } },
              { { 0., 0., 0., 0. }, { 0., 0., 0., 0. }, { 0., 0., 0., 0. },
                  { 0., 0., 0., 0. } } };

      showHilb( spaceHilb );

      Transform t = new Transform( new FastWaveletTransform( new Haar1( ) ) );
      double[ ][ ][ ] spaceTime = t.reverse( spaceHilb );

      showTime( spaceTime );

      double[ ][ ][ ] expected =
          {
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } },
              { { 1., 1., 1., 1. }, { 1., 1., 1., 1. }, { 1., 1., 1., 1. },
                  { 1., 1., 1., 1. } } };

      assertSpace( expected, spaceTime, delta );

    } catch( JWaveException e ) {

      e.showMessage( );
      e.printStackTrace( );

    } // try

  } // testReverseDoubleArrayArrayArray

  public void
      assertArray( Complex[ ] expected, Complex[ ] actual, double delta ) {

    int expectedLength = expected.length;
    int actualLength = actual.length;

    assertEquals( expectedLength, actualLength );

    for( int c = 0; c < expectedLength; c++ ) {

      double expectedReal = expected[ c ].getReal( );
      double expectedImag = expected[ c ].getImag( );

      double actualReal = actual[ c ].getReal( );
      double actualImag = actual[ c ].getImag( );

      assertEquals( expectedReal, actualReal, delta );
      assertEquals( expectedImag, actualImag, delta );

    } // c

  } // assertArray

  protected void
      assertArray( double[ ] expected, double[ ] actual, double delta ) {
    for( int i = 0; i < expected.length; i++ )
      assertEquals( expected[ i ], actual[ i ], delta );
  } // assertMatrix

  protected void assertMatrix( double[ ][ ] expected, double[ ][ ] actual,
      double delta ) {
    for( int i = 0; i < expected.length; i++ )
      for( int j = 0; j < expected[ i ].length; j++ )
        assertEquals( expected[ i ][ j ], actual[ i ][ j ], delta );
  } // assertMatrix

  protected void assertSpace( double[ ][ ][ ] expected, double[ ][ ][ ] actual,
      double delta ) {
    for( int i = 0; i < expected.length; i++ )
      for( int j = 0; j < expected[ i ].length; j++ )
        for( int k = 0; k < expected[ i ][ j ].length; k++ )
          assertEquals( expected[ i ][ j ][ k ], actual[ i ][ j ][ k ], delta );
  } // assertSpace

  protected void showTime( double[ ] arrTime ) {
    System.out.print( "time domain: " + "\t" + "\t" );
    for( int c = 0; c < arrTime.length; c++ )
      System.out.print( arrTime[ c ] + " " );
    System.out.println( "" );
  } // showTime

  protected void showFreq( double[ ] arrFreq ) {
    System.out.print( "frequency domain: " + "\t" );
    for( int c = 0; c < arrFreq.length; c++ )
      System.out.print( arrFreq[ c ] + " " );
    System.out.println( "" );
  } // showHilb

  protected void showHilb( double[ ] arrHilb ) {
    System.out.print( "Hilbert domain: " + "\t" );
    for( int c = 0; c < arrHilb.length; c++ )
      System.out.print( arrHilb[ c ] + " " );
    System.out.println( "" );
  } // showHilb

  protected void showTime( Complex[ ] arrTime ) {
    System.out.print( "time domain: " + "\t" + "\t" );
    for( int c = 0; c < arrTime.length; c++ )
      System.out.print( arrTime[ c ].toString( ) + " " );
    System.out.println( "" );
  } // showTime

  protected void showHilb( Complex[ ] arrFreq ) {
    System.out.print( "frequency domain: " + "\t" );
    for( int c = 0; c < arrFreq.length; c++ )
      System.out.print( arrFreq[ c ].toString( ) + " " );
    System.out.println( "" );
  } // showHilb

  protected void showTime( double[ ][ ] matrixTime ) {
    System.out.println( "time domain: " + "\t" );
    for( int i = 0; i < matrixTime.length; i++ ) {
      for( int j = 0; j < matrixTime[ i ].length; j++ )
        System.out.print( matrixTime[ i ][ j ] + " " );
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showTime

  protected void showFreq( double[ ][ ] matrixFreq ) {
    System.out.println( "frequency domain: " + "\t" );
    for( int i = 0; i < matrixFreq.length; i++ ) {
      for( int j = 0; j < matrixFreq[ i ].length; j++ )
        System.out.print( matrixFreq[ i ][ j ] + " " );
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showFreq

  protected void showHilb( double[ ][ ] matrixHilb ) {
    System.out.println( "Hilbert domain: " + "\t" );
    for( int i = 0; i < matrixHilb.length; i++ ) {
      for( int j = 0; j < matrixHilb[ i ].length; j++ )
        System.out.print( matrixHilb[ i ][ j ] + " " );
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showHilb

  protected void showTime( double[ ][ ][ ] spaceTime ) {
    System.out.println( "time domain: " + "\t" );
    for( int i = 0; i < spaceTime.length; i++ ) {
      for( int j = 0; j < spaceTime[ i ].length; j++ ) {
        for( int k = 0; k < spaceTime[ i ][ j ].length; k++ )
          System.out.print( spaceTime[ i ][ j ][ k ] + " " );
        System.out.println( "" );
      }
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showTime

  protected void showFreq( double[ ][ ][ ] spaceTime ) {
    System.out.println( "frequency domain: " + "\t" );
    for( int i = 0; i < spaceTime.length; i++ ) {
      for( int j = 0; j < spaceTime[ i ].length; j++ ) {
        for( int k = 0; k < spaceTime[ i ][ j ].length; k++ )
          System.out.print( spaceTime[ i ][ j ][ k ] + " " );
        System.out.println( "" );
      }
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showFreq

  protected void showHilb( double[ ][ ][ ] spaceTime ) {
    System.out.println( "Hilbert domain: " + "\t" );
    for( int i = 0; i < spaceTime.length; i++ ) {
      for( int j = 0; j < spaceTime[ i ].length; j++ ) {
        for( int k = 0; k < spaceTime[ i ][ j ].length; k++ )
          System.out.print( spaceTime[ i ][ j ][ k ] + " " );
        System.out.println( "" );
      }
      System.out.println( "" );
    }
    System.out.println( "" );
  } // showHilb

} // TransformTest

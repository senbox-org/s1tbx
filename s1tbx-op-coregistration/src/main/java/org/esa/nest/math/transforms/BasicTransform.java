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
package org.esa.nest.math.transforms;

import org.esa.nest.math.datatypes.Complex;
import org.esa.nest.math.exceptions.JWaveError;
import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.tools.MathToolKit;

/**
 * Basic Wave for transformations like Fast Fourier Transform (FFT), Fast
 * Wavelet Transform (FWT), Fast Wavelet Packet Transform (WPT), or Discrete
 * Wavelet Transform (DWT). Naming of this class due to en.wikipedia.org; to
 * write Fourier series in terms of the 'basic waves' of function: e^(2*pi*i*w).
 * 
 * @date 08.02.2010 11:11:59
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public abstract class BasicTransform {

  /**
   * Supplying a various number of little mathematical methods.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.02.2014 18:34:34
   */
  protected MathToolKit _mathToolKit;

  /**
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.02.2014 18:38:21
   */
  public BasicTransform( ) {

    _mathToolKit = new MathToolKit( );

  } // BasicTransform

  /**
   * Performs the forward transform from time domain to frequency or Hilbert
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 10.02.2010 08:23:24
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrTime
   *          coefficients of 1-D time domain
   * @return coefficients of 1-D frequency or Hilbert domain
   */
  public abstract double[ ] forward( double[ ] arrTime );

  /**
   * Performs the reverse transform from frequency or Hilbert domain to time
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 10.02.2010 08:23:24
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrFreq
   *          coefficients of 1-D frequency or Hilbert domain
   * @return matDeComp 2-D Hilbert spaces: [ 0 .. p ][ 0 .. N ] where p is the
   *         exponent of N=2^p
   */
  public abstract double[ ] reverse( double[ ] arrFreq );

  /**
   * Generates from a 2-D decomposition a 1-D time series.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 10:07:19
   * @param matDeComp
   *          2-D Hilbert spaces: [ 0 .. p ][ 0 .. N ] where p is the exponent
   *          of N=2^p
   * @return a 1-D time domain signal
   * @throws JWaveException
   */
  public double[ ][ ] decompose( double[ ] arrTime ) throws JWaveException {

    throw new JWaveError( "method is not working for this transform type!" );

  } // decompose

  public double[ ] recompose( double[ ][ ] matDeComp ) throws JWaveException {

    throw new JWaveError( "method is not working for this transform type!" );

  } // recompose

  /**
   * Performs the forward transform from time domain to frequency or Hilbert
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 16.02.2014 14:42:57
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   *         (cscheiblich@gmail.com)
   * @param arrTime
   *          coefficients of 1-D time domain
   * @return coefficients of 1-D frequency or Hilbert domain
   */
  public Complex[ ] forward( Complex[ ] arrTime ) {

    double[ ] arrTimeBulk = new double[ 2 * arrTime.length ];

    for( int i = 0; i < arrTime.length; i++ ) {

      // TODO rehack this to: { r1, r2, r3, .., c1, c2, c3, .. }
      int k = i * 2;
      arrTimeBulk[ k ] = arrTime[ i ].getReal( );
      arrTimeBulk[ k + 1 ] = arrTime[ i ].getImag( );

    } // i blown to k = 2 * i

    double[ ] arrHilbBulk = forward( arrTimeBulk );

    Complex[ ] arrHilb = new Complex[ arrTime.length ];

    for( int i = 0; i < arrTime.length; i++ ) {

      int k = i * 2;
      arrHilb[ i ] = new Complex( arrHilbBulk[ k ], arrHilbBulk[ k + 1 ] );

    } // k = 2 * i shrink to i 

    return arrHilb;

  } // forward

  /**
   * Performs the reverse transform from frequency or Hilbert domain to time
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 16.02.2014 14:42:57
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   *         (cscheiblich@gmail.com)
   * @param arrFreq
   *          coefficients of 1-D frequency or Hilbert domain
   * @return coefficients of 1-D time domain
   */
  public Complex[ ] reverse( Complex[ ] arrHilb ) {

    double[ ] arrHilbBulk = new double[ 2 * arrHilb.length ];

    for( int i = 0; i < arrHilb.length; i++ ) {

      int k = i * 2;
      arrHilbBulk[ k ] = arrHilb[ i ].getReal( );
      arrHilbBulk[ k + 1 ] = arrHilb[ i ].getImag( );

    } // i blown to k = 2 * i

    double[ ] arrTimeBulk = reverse( arrHilbBulk );

    Complex[ ] arrTime = new Complex[ arrHilb.length ];

    for( int i = 0; i < arrTime.length; i++ ) {

      int k = i * 2;
      arrTime[ i ] = new Complex( arrTimeBulk[ k ], arrTimeBulk[ k + 1 ] );

    } // k = 2 * i shrink to i 

    return arrTime;

  } // reverse

  /**
   * Performs the 2-D forward transform from time domain to frequency or Hilbert
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 10.02.2010 11:00:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param matTime
   *          coefficients of 2-D time domain
   * @return coefficients of 2-D frequency or Hilbert domain
   */
  public double[ ][ ] forward( double[ ][ ] matTime ) {

    int noOfRows = matTime.length;
    int noOfCols = matTime[ 0 ].length;

    double[ ][ ] matHilb = new double[ noOfRows ][ noOfCols ];

    for( int i = 0; i < noOfRows; i++ ) {

      double[ ] arrTime = new double[ noOfCols ];

      for( int j = 0; j < noOfCols; j++ )
        arrTime[ j ] = matTime[ i ][ j ];

      double[ ] arrHilb = forward( arrTime );

      for( int j = 0; j < noOfCols; j++ )
        matHilb[ i ][ j ] = arrHilb[ j ];

    } // rows

    for( int j = 0; j < noOfCols; j++ ) {

      double[ ] arrTime = new double[ noOfRows ];

      for( int i = 0; i < noOfRows; i++ )
        arrTime[ i ] = matHilb[ i ][ j ];

      double[ ] arrHilb = forward( arrTime );

      for( int i = 0; i < noOfRows; i++ )
        matHilb[ i ][ j ] = arrHilb[ i ];

    } // cols

    return matHilb;
  } // forward

  /**
   * Performs the 2-D reverse transform from frequency or Hilbert or time domain
   * to time domain for a given array depending on the used transform algorithm
   * by inheritance.
   * 
   * @date 10.02.2010 11:01:38
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param matFreq
   *          coefficients of 2-D frequency or Hilbert domain
   * @return coefficients of 2-D time domain
   */
  public double[ ][ ] reverse( double[ ][ ] matFreq ) {

    int noOfRows = matFreq.length;
    int noOfCols = matFreq[ 0 ].length;

    double[ ][ ] matTime = new double[ noOfRows ][ noOfCols ];

    for( int j = 0; j < noOfCols; j++ ) {

      double[ ] arrFreq = new double[ noOfRows ];

      for( int i = 0; i < noOfRows; i++ )
        arrFreq[ i ] = matFreq[ i ][ j ];

      double[ ] arrTime = reverse( arrFreq ); // AED 

      for( int i = 0; i < noOfRows; i++ )
        matTime[ i ][ j ] = arrTime[ i ];

    } // cols

    for( int i = 0; i < noOfRows; i++ ) {

      double[ ] arrFreq = new double[ noOfCols ];

      for( int j = 0; j < noOfCols; j++ )
        arrFreq[ j ] = matTime[ i ][ j ];

      double[ ] arrTime = reverse( arrFreq ); // AED 

      for( int j = 0; j < noOfCols; j++ )
        matTime[ i ][ j ] = arrTime[ j ];

    } // rows

    return matTime;
  } // reverse

  /**
   * Performs the 3-D forward transform from time domain to frequency or Hilbert
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 10.07.2010 18:08:17
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param spcTime
   *          coefficients of 3-D time domain domain
   * @return coefficients of 3-D frequency or Hilbert domain
   */
  public double[ ][ ][ ] forward( double[ ][ ][ ] spcTime ) {

    int noOfRows = spcTime.length; // first dimension
    int noOfCols = spcTime[ 0 ].length; // second dimension
    int noOfHigh = spcTime[ 0 ][ 0 ].length; // third dimension

    double[ ][ ][ ] spcHilb = new double[ noOfRows ][ noOfCols ][ noOfHigh ];

    for( int i = 0; i < noOfRows; i++ ) {

      double[ ][ ] matTime = new double[ noOfCols ][ noOfHigh ];

      for( int j = 0; j < noOfCols; j++ ) {

        for( int k = 0; k < noOfHigh; k++ ) {

          matTime[ j ][ k ] = spcTime[ i ][ j ][ k ];

        } // high

      } // cols      

      double[ ][ ] matHilb = forward( matTime ); // 2-D forward

      for( int j = 0; j < noOfCols; j++ ) {

        for( int k = 0; k < noOfHigh; k++ ) {

          spcHilb[ i ][ j ][ k ] = matHilb[ j ][ k ];

        } // high

      } // cols

    } // rows  

    for( int j = 0; j < noOfCols; j++ ) {

      for( int k = 0; k < noOfHigh; k++ ) {

        double[ ] arrTime = new double[ noOfRows ];

        for( int i = 0; i < noOfRows; i++ )
          arrTime[ i ] = spcHilb[ i ][ j ][ k ];

        double[ ] arrHilb = forward( arrTime ); // 1-D forward

        for( int i = 0; i < noOfRows; i++ )
          spcHilb[ i ][ j ][ k ] = arrHilb[ i ];

      } // high

    } // cols

    return spcHilb;

  } // forward

  /**
   * Performs the 3-D reverse transform from frequency or Hilbert domain to time
   * domain for a given array depending on the used transform algorithm by
   * inheritance.
   * 
   * @date 10.07.2010 18:09:54
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param spcHilb
   *          coefficients of 3-D frequency or Hilbert domain
   * @return coefficients of 3-D time domain
   */
  public double[ ][ ][ ] reverse( double[ ][ ][ ] spcHilb ) {

    int noOfRows = spcHilb.length; // first dimension
    int noOfCols = spcHilb[ 0 ].length; // second dimension
    int noOfHigh = spcHilb[ 0 ][ 0 ].length; // third dimension

    double[ ][ ][ ] spcTime = new double[ noOfRows ][ noOfCols ][ noOfHigh ];

    for( int i = 0; i < noOfRows; i++ ) {

      double[ ][ ] matHilb = new double[ noOfCols ][ noOfHigh ];

      for( int j = 0; j < noOfCols; j++ ) {

        for( int k = 0; k < noOfHigh; k++ ) {

          matHilb[ j ][ k ] = spcHilb[ i ][ j ][ k ];

        } // high

      } // cols      

      double[ ][ ] matTime = reverse( matHilb ); // 2-D reverse

      for( int j = 0; j < noOfCols; j++ ) {

        for( int k = 0; k < noOfHigh; k++ ) {

          spcTime[ i ][ j ][ k ] = matTime[ j ][ k ];

        } // high

      } // cols

    } // rows  

    for( int j = 0; j < noOfCols; j++ ) {

      for( int k = 0; k < noOfHigh; k++ ) {

        double[ ] arrHilb = new double[ noOfRows ];

        for( int i = 0; i < noOfRows; i++ )
          arrHilb[ i ] = spcTime[ i ][ j ][ k ];

        double[ ] arrTime = reverse( arrHilb ); // 1-D reverse

        for( int i = 0; i < noOfRows; i++ )
          spcTime[ i ][ j ][ k ] = arrTime[ i ];

      } // high

    } // cols

    return spcTime;

  } // reverse

} // BasicTransform
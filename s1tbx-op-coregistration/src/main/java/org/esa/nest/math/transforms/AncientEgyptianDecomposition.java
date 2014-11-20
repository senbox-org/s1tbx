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

import org.esa.nest.math.exceptions.JWaveException;

/**
 * A wavelet transform method for arrays and signals of arbitrary lengths, even
 * odd lengths. The array is decomposed in several parts of optimal lengths by
 * applying the ancient Egyptian decomposition. Hereby, the array or signal is
 * decomposed to the largest possible sub arrays of two the power of p.
 * Afterwards each sub array is transformed forward and copied back to the
 * discrete position of the input array. The reverse transform applies the same
 * vice versa. In more detail the ancient Egyptian Multiplication can be easily
 * explained by the following example: 42 = 2^5 + 2^3 + 2^1 = 32 + 8 + 2.
 * However, an array or signal of odd length produces the smallest ancient
 * Egyptian multiplier 2^0 which is actually 1. Therefore, the matching sub
 * array or signal is untouched an the coefficient is actually the wavelet
 * coefficient of wavelet space of level 0. For an "orthonormal" wavelet this
 * holds. See: http://en.wikipedia.org/wiki/Ancient_Egyptian_multiplication
 * 
 * @date 14.08.2010 10:43:28
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class AncientEgyptianDecomposition extends BasicTransform {

  /**
   * The selected Transform (FWT or WPT) used for the sub arrays of the ancient
   * Egyptian decomposition. Actually, this displays somehow the Composite
   * Pattern of software design pattern. See:
   * http://en.wikipedia.org/wiki/Composite_pattern#Java
   */
  protected BasicTransform _waveTransform;

  /**
   * the base block size for spitting an array; e. g. 127 with block size of 32
   * ends up as: 32 | 32 | 32 | 16 | 8 | 4 | 2 | 1.
   */
  @SuppressWarnings ( "unused") private int _initialWaveletSpaceSize;

  /**
   * Constructor taking the
   * 
   * @date 14.08.2010 10:43:28
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public AncientEgyptianDecomposition( BasicTransform waveTransform ) {

    _waveTransform = waveTransform;

    _initialWaveletSpaceSize = 0;

  } // FastBasicTransformArbitrary

  public AncientEgyptianDecomposition( BasicTransform waveTransform,
      int initialWaveletSpaceSize ) {

    _waveTransform = waveTransform;

    _initialWaveletSpaceSize = initialWaveletSpaceSize;

  } // AncientEgyptianDecomposition

  /**
   * This forward method decomposes the given array of arbitrary length to sub
   * arrays while applying the ancient Egyptian decomposition. Each sub array is
   * transformed by the selected basic transform and the resulting wavelet
   * coefficients are copied back to their original discrete positions.
   * 
   * @date 14.08.2010 10:43:28
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#forward(double[])
   */
  @Override public double[ ] forward( double[ ] arrTime ) {

    double[ ] arrHilb = new double[ arrTime.length ];

    int[ ] ancientEgyptianMultipliers = null;

    try {

      ancientEgyptianMultipliers = _mathToolKit.decompose( arrTime.length );

    } catch( JWaveException e ) {

      e.printStackTrace( );

    } // try 

    int offSet = 0;
    for( int m = 0; m < ancientEgyptianMultipliers.length; m++ ) {

      int ancientEgyptianMultiplier = ancientEgyptianMultipliers[ m ];

      int arrTimeSubLength =
          (int)_mathToolKit.scalb( 1., ancientEgyptianMultiplier );

      double[ ] arrTimeSub = new double[ arrTimeSubLength ];

      for( int i = 0; i < arrTimeSub.length; i++ )
        arrTimeSub[ i ] = arrTime[ i + offSet ];

      double[ ] arrHilbSub = _waveTransform.forward( arrTimeSub );

      for( int i = 0; i < arrHilbSub.length; i++ )
        arrHilb[ i + offSet ] = arrHilbSub[ i ];

      offSet += arrHilbSub.length;

    } // m - no of sub transforms

    return arrHilb;

  } // forward

  /**
   * This reverse method awaits an array of arbitrary length in wavelet space
   * keeping the wavelet already decomposed by the ancient Egyptian
   * decomposition. Therefore, each of the existing sub arrays of length 2^p is
   * reverse transformed by the selected basic transform and the resulting
   * coefficients of time domain are copied back to their original discrete
   * positions.
   * 
   * @date 14.08.2010 10:43:28
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#reverse(double[])
   */
  @Override public double[ ] reverse( double[ ] arrHilb ) {

    double[ ] arrTime = new double[ arrHilb.length ];

    int[ ] ancientEgyptianMultipliers = null;
    try {

      ancientEgyptianMultipliers = _mathToolKit.decompose( arrHilb.length );

    } catch( JWaveException e ) {

      e.printStackTrace( );

    }

    int offSet = 0;
    for( int m = 0; m < ancientEgyptianMultipliers.length; m++ ) {

      int ancientEgyptianMultiplier = ancientEgyptianMultipliers[ m ];

      int arrHilbSubLength =
          (int)_mathToolKit.scalb( 1., ancientEgyptianMultiplier );

      double[ ] arrHilbSub = new double[ arrHilbSubLength ];

      for( int i = 0; i < arrHilbSub.length; i++ )
        arrHilbSub[ i ] = arrHilb[ i + offSet ];

      double[ ] arrTimeSub = _waveTransform.reverse( arrHilbSub );

      for( int i = 0; i < arrTimeSub.length; i++ )
        arrTime[ i + offSet ] = arrTimeSub[ i ];

      offSet += arrHilbSub.length;

    } // m - no of sub transforms

    return arrTime;

  } // reverse

} // class
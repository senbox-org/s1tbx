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
import org.esa.nest.math.exceptions.JWaveFailure;
import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Base class for the forward and reverse Wavelet Packet Transform (WPT) also
 * called Wavelet Packet Decomposition (WPD) using a specified Wavelet by
 * inheriting class.
 * 
 * @date 23.02.2010 13:44:05
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class WaveletPacketTransform extends WaveletTransform {

  /**
   * Constructor receiving a Wavelet object.
   * 
   * @date 23.02.2010 13:44:05
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param wavelet
   *          object of type Wavelet; Haar1, Daubechies2, Coiflet1, ...
   * @throws JWaveFailure
   *           if object is null or not of type wavelet
   * @throws JWaveException
   */
  public WaveletPacketTransform( Wavelet wavelet ) throws JWaveFailure {

    super( wavelet );

  } // WaveletPacketTransform

  /**
   * Implementation of the 1-D forward wavelet packet transform for arrays of
   * dim N by filtering with the longest wavelet first and then always with both
   * sub bands -- low and high (approximation and details) -- by the next
   * smaller wavelet.
   * 
   * @date 23.02.2010 13:44:05
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#forward(double[])
   */
  @Override public double[ ] forward( double[ ] arrTime ) {

    double[ ] arrHilb = new double[ arrTime.length ];
    for( int i = 0; i < arrTime.length; i++ )
      arrHilb[ i ] = arrTime[ i ];

    int k = arrTime.length;

    int h = arrTime.length;

    int transformWavelength = _wavelet.getTransformWavelength( ); // 2, 4, 8, 16, 32, ...

    if( h >= transformWavelength ) {

      while( h >= transformWavelength ) {

        int g = k / h; // 1 -> 2 -> 4 -> 8 -> ...

        for( int p = 0; p < g; p++ ) {

          double[ ] iBuf = new double[ h ];

          for( int i = 0; i < h; i++ )
            iBuf[ i ] = arrHilb[ i + ( p * h ) ];

          double[ ] oBuf = _wavelet.forward( iBuf, h );

          for( int i = 0; i < h; i++ )
            arrHilb[ i + ( p * h ) ] = oBuf[ i ];

        } // packets

        h = h >> 1;

      } // levels

    } // if

    return arrHilb;
  } // forward

  /**
   * Implementation of the 1-D reverse wavelet packet transform for arrays of
   * dim N by filtering with the smallest wavelet for all sub bands -- low and
   * high bands (approximation and details) -- and the by the next greater
   * wavelet combining two smaller and all other sub bands.
   * 
   * @date 23.02.2010 13:44:05
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#reverse(double[])
   */
  @Override public double[ ] reverse( double[ ] arrHilb ) {

    double[ ] arrTime = new double[ arrHilb.length ];

    for( int i = 0; i < arrHilb.length; i++ )
      arrTime[ i ] = arrHilb[ i ];

    int transformWavelength = _wavelet.getTransformWavelength( ); // 2, 4, 8, 16, 32, ...

    int k = arrTime.length;

    int h = transformWavelength;
    //    if( !_mathToolKit.isBinary( h ) )
    //      for( h = 2; h <= transformWavelength; h *= 2 ) {}
    // fixed h = h << 1; // 6 -> 8, 10 -> 16

    if( arrHilb.length >= transformWavelength ) {

      while( h <= arrTime.length && h >= transformWavelength ) {

        int g = k / h; // ... -> 8 -> 4 -> 2 -> 1

        for( int p = 0; p < g; p++ ) {

          double[ ] iBuf = new double[ h ];

          for( int i = 0; i < h; i++ )
            iBuf[ i ] = arrTime[ i + ( p * h ) ];

          double[ ] oBuf = _wavelet.reverse( iBuf, h );

          for( int i = 0; i < h; i++ )
            arrTime[ i + ( p * h ) ] = oBuf[ i ];

        } // packets

        h = h << 1;

      } // levels

    } // if

    return arrTime;

  } // reverse

} // class

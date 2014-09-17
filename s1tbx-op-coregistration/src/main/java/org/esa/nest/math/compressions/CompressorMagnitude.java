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
 * @date 20.02.2014 23:56:09 
 *
 * CompressorMagnitude.java
 */
package org.esa.nest.math.compressions;

import org.esa.nest.math.exceptions.JWaveException;

/**
 * Compression algorithm is adding up all magnitudes of an array, a matrix, or a
 * space and dividing this absolute value by the number of given data samples.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 20.02.2014 23:56:09
 */
public class CompressorMagnitude extends Compressor {

  /**
   * Member variable for remembering the calculated magnitude.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 21.02.2014 00:02:52
   */
  protected double _magnitude;

  /**
   * Threshold is set to one, which should always guarantee a rather good
   * compression result.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:56:09
   */
  public CompressorMagnitude( ) {

    _magnitude = 0.;

    _threshold = 1.;

  } // CompressorMagnitude

  /**
   * Threshold is set a chosen; value 0 means no compression.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:56:09
   * @param threshold
   *          has to be positive value starting at 0 - 0 means no compression.
   * @throws JWaveException
   */
  public CompressorMagnitude( double threshold ) throws JWaveException {

    super( threshold );

    _magnitude = 0.;

  } // CompressorMagnitude

  /*
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:56:09 (non-Javadoc)
   * @see math.jwave.compressions.Compressor#compress(double[])
   */
  @Override protected double[ ] compress( double[ ] arrHilb ) {

    _magnitude = 0.;

    int arrHilbSize = arrHilb.length;

    double[ ] arrComp = new double[ arrHilbSize ];

    for( int i = 0; i < arrHilbSize; i++ )
      _magnitude += Math.abs( arrHilb[ i ] );

    for( int i = 0; i < arrHilbSize; i++ )
      if( Math.abs( arrHilb[ i ] ) >= _magnitude * _threshold )
        arrComp[ i ] = arrHilb[ i ];
      else
        arrComp[ i ] = 0.;

    return arrComp;

  } // compress

  /*
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:56:09 (non-Javadoc)
   * @see math.jwave.compressions.Compressor#compress(double[][])
   */
  @Override protected double[ ][ ] compress( double[ ][ ] matHilb ) {

    _magnitude = 0.;

    int matHilbNoOfRows = matHilb.length;
    int matHilbNoOfCols = matHilb[ 0 ].length;

    double[ ][ ] matComp = new double[ matHilbNoOfRows ][ matHilbNoOfCols ];

    for( int i = 0; i < matHilbNoOfRows; i++ )
      for( int j = 0; j < matHilbNoOfCols; j++ )
        _magnitude += Math.abs( matHilb[ i ][ j ] );

    for( int i = 0; i < matHilbNoOfRows; i++ )
      for( int j = 0; j < matHilbNoOfCols; j++ )
        if( Math.abs( matHilb[ i ][ j ] ) >= _magnitude * _threshold )
          matComp[ i ][ j ] = matHilb[ i ][ j ];
        else
          matComp[ i ][ j ] = 0.;

    return matComp;

  } // compress

  /*
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:56:09 (non-Javadoc)
   * @see math.jwave.compressions.Compressor#compress(double[][][])
   */
  @Override protected double[ ][ ][ ] compress( double[ ][ ][ ] spcHilb ) {

    _magnitude = 0.;

    int matHilbNoOfRows = spcHilb.length;
    int matHilbNoOfCols = spcHilb[ 0 ].length;
    int matHilbNoOfLvls = spcHilb[ 0 ][ 0 ].length;

    double[ ][ ][ ] spcComp =
        new double[ matHilbNoOfRows ][ matHilbNoOfCols ][ matHilbNoOfLvls ];

    for( int i = 0; i < matHilbNoOfRows; i++ )
      for( int j = 0; j < matHilbNoOfCols; j++ )
        for( int k = 0; k < matHilbNoOfLvls; k++ )
          _magnitude += Math.abs( spcHilb[ i ][ j ][ k ] );

    for( int i = 0; i < matHilbNoOfRows; i++ )
      for( int j = 0; j < matHilbNoOfCols; j++ )
        for( int k = 0; k < matHilbNoOfLvls; k++ )
          if( Math.abs( spcHilb[ i ][ j ][ k ] ) >= _magnitude * _threshold )
            spcComp[ i ][ j ][ k ] = spcHilb[ i ][ j ][ k ];
          else
            spcComp[ i ][ j ][ k ] = 0.;

    return spcComp;

  } // compress

} // CompressorMagnitude

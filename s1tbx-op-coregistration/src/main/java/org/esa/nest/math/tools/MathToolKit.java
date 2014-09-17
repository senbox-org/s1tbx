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
package org.esa.nest.math.tools;

import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.exceptions.JWaveFailure;
import org.esa.nest.math.exceptions.JWaveError;

/**
 * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
 *         1:42:37 PM
 */
public class MathToolKit {

  /**
   * Some how useless ~8>
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
   *         1:42:37 PM
   */
  public MathToolKit( ) {

  } // MathToolKit

  /**
   * The method converts a positive integer to the ancient Egyptian multipliers
   * which are actually the multipliers to display the number by a sum of the
   * largest possible powers of two. E.g. 42 = 2^5 + 2^3 + 2^1 = 32 + 8 + 2.
   * However, odd numbers always 2^0 = 1 as the last entry. Also see:
   * http://en.wikipedia.org/wiki/Ancient_Egyptian_multiplication
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
   *         1:50:42 PM
   * @param number
   * @return
   * @throws JWaveException
   */
  public int[ ] decompose( int number ) throws JWaveException {

    if( number < 1 )
      throw new JWaveFailure(
          "the supported number for decomposition is smaller than one" );

    int power = getExponent( (double)number );

    int[ ] tmpArr = new int[ power + 1 ]; // max no of possible multipliers

    int pos = 0;
    double current = (double)number;
    while( current >= 1. ) {

      power = getExponent( current );
      tmpArr[ pos ] = power;
      current = current - scalb( 1., power ); // 1. * 2 ^ power
      pos++;

    } // while

    int[ ] ancientEgyptianMultipliers = new int[ pos ]; // shrink
    for( int c = 0; c < pos; c++ )
      ancientEgyptianMultipliers[ c ] = tmpArr[ c ];

    return ancientEgyptianMultipliers;

  } // decompose

  /**
   * splits the given length of the data array to a possible number of blocks in
   * block size and then handles the rest as the ancient egyptian decomposition:
   * e. g. 127 by block size 32 ends up as: 32 | 32 | 32 | 16 | 8 | 4 | 2 | 1.
   * 
   * @param number
   *          the number that should be decompose; greater than block size
   * @param blockSize
   *          the block size as a type of 2^p|p={1,2,4,..} that is first used
   *          blocks until a rest is left; smaller than parameter number.
   * @return an array keeping splits by several time the given block size first
   *         and then of a rest split by the ancient egyptian decomposition.
   * @throws JWaveException
   *           if block size is not of type 2^p|p={1,2,4,..}, if block size is
   *           smaller than number or negative input is given.
   */
  public int[ ] decompose( int number, int blockSize ) throws JWaveException {

    int[ ] blockedAncientEgyptianMultipliers = null;

    if( !isBinary( blockSize ) )
      throw new JWaveFailure( "given block size is not 2^p|p={1,2,3,4,..}. "
          + "block size shold be e. g.: 4, 8, 16, 32, .." );

    if( number < blockSize )
      throw new JWaveFailure(
          "Given blockSize is greater than the given number "
              + "to be split by it" );

    int noOfBlocks = number % blockSize; // 127 % 32 = 3

    int rest = number - noOfBlocks * blockSize; // 127 - 3 * 32 = 31

    int[ ] ancientEgyptianMultipliers = decompose( rest );

    int blockedAncientEgyptianMultipliersSize =
        ancientEgyptianMultipliers.length + noOfBlocks;

    blockedAncientEgyptianMultipliers =
        new int[ blockedAncientEgyptianMultipliersSize ];

    int j = 0;
    for( int i = 0; i < blockedAncientEgyptianMultipliersSize; i++ )
      if( i < noOfBlocks )
        blockedAncientEgyptianMultipliers[ i ] = blockSize;
      else {
        blockedAncientEgyptianMultipliers[ i ] = ancientEgyptianMultipliers[ j ];
        j++;
      }

    return blockedAncientEgyptianMultipliers;

  } // decomppse

  /**
   * The method converts a list of ancient Egyptian multipliers to the
   * corresponding integer. The ancient Egyptian multipliers are actually the
   * multipliers to display am integer by a sum of the largest possible powers
   * of two. E.g. 42 = 2^5 + 2^3 + 2^1 = 32 + 8 + 2. Also see:
   * http://en.wikipedia.org/wiki/Ancient_Egyptian_multiplication
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
   *         1:55:54 PM
   * @param ancientEgyptianMultipliers
   *          an integer array keeping the ancient Egyptian multipliers
   * @return resulting integer as sum of powers of two
   * @throws JWaveException
   */
  public int compose( int[ ] ancientEgyptianMultipliers ) throws JWaveException {

    if( ancientEgyptianMultipliers == null )
      throw new JWaveError( "given array is null" );

    int number = 0;

    int noOfAncientEgyptianMultipliers = ancientEgyptianMultipliers.length;
    for( int m = 0; m < noOfAncientEgyptianMultipliers; m++ ) {

      int ancientEgyptianMultiplier = ancientEgyptianMultipliers[ m ];

      number += (int)scalb( 1., ancientEgyptianMultiplier ); // 1. * 2^p

    } // compose

    return number;

  }

  /**
   * Checks if given number is of type 2^p = 1, 2, 4, 8, 18, 32, 64, .., 1024,
   * ..
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) 10.02.2014 20:18:26
   * @param number
   *          any positive integer
   * @return true if is 2^p else false
   */
  public boolean isBinary( int number ) {

    boolean isBinary = false;

    int power = (int)( Math.log( number ) / Math.log( 2. ) );

    double result = 1. * Math.pow( 2., power );

    if( result == number )
      isBinary = true;

    return isBinary;

  } // isBinary

  /**
   * Replaced Math.getExponent due to google's Android OS is not supporting it
   * in Math library.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
   *         1:47:05 PM
   * @author sashi
   * @date 19.04.2011 15:43:16
   * @param f
   * @return p of 2^p <= f < 2^(p+1)
   */
  public int getExponent( double f ) {

    int exp = (int)( Math.log( f ) / Math.log( 2. ) );

    return exp;

  } // exp

  /**
   * Replaced Math.scalb due to google's Android OS is not supporting it in Math
   * library.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com) date Feb 11, 2013
   *         1:46:33 PM
   * @param f
   * @param scaleFactor
   * @return f times 2^(scaleFactor)
   */
  public double scalb( double f, int scaleFactor ) {

    double res = f * Math.pow( 2., scaleFactor );

    return res;

  } // scalb

}

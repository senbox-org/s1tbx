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

/**
 * The Discrete Fourier Transform (DFT) is - as the name says - the discrete
 * version of the Fourier Transform applied to a discrete complex valued series.
 * While the DFT can be applied to any complex valued series; of any length, in
 * practice for large series it can take considerable time to compute, while the
 * time taken being proportional to the square of the number on points in the
 * series.
 * 
 * @date 25.03.2010 19:56:29
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 */
public class DiscreteFourierTransform extends BasicTransform {
  
  /**
   * Constructor; does nothing
   * 
   * @date 25.03.2010 19:56:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   */
  public DiscreteFourierTransform( ) {
  } // DiscreteFourierTransform
  
  /**
   * The 1-D forward version of the Discrete Fourier Transform (DFT); The input
   * array arrTime is organized by real and imaginary parts of a complex number
   * using even and odd places for the index. For example: arrTime[ 0 ] = real1,
   * arrTime[ 1 ] = imag1, arrTime[ 2 ] = real2, arrTime[ 3 ] = imag2, ... The
   * output arrFreq is organized by the same scheme.
   * 
   * @date 25.03.2010 19:56:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#forward(double[])
   */
  @Override
  public double[ ] forward( double[ ] arrTime ) {
    
    int m = arrTime.length;
    double[ ] arrFreq = new double[ m ]; // result
    
    int n = m >> 1; // half of m
    
    for( int i = 0; i < n; i++ ) {
      
      int iR = i * 2;
      int iC = i * 2 + 1;
      
      arrFreq[ iR ] = 0.;
      arrFreq[ iC ] = 0.;
      
      double arg = -2. * Math.PI * (double)i / (double)n;
      
      for( int k = 0; k < n; k++ ) {
        
        int kR = k * 2;
        int kC = k * 2 + 1;
        
        double cos = Math.cos( k * arg );
        double sin = Math.sin( k * arg );
        
        arrFreq[ iR ] += arrTime[ kR ] * cos - arrTime[ kC ] * sin;
        arrFreq[ iC ] += arrTime[ kR ] * sin + arrTime[ kC ] * cos;
        
      } // k
      
      arrFreq[ iR ] /= (double)n;
      arrFreq[ iC ] /= (double)n;
      
    } // i
    
    return arrFreq;
  } // forward
  
  /**
   * The 1-D reverse version of the Discrete Fourier Transform (DFT); The input
   * array arrFreq is organized by real and imaginary parts of a complex number
   * using even and odd places for the index. For example: arrTime[ 0 ] = real1,
   * arrTime[ 1 ] = imag1, arrTime[ 2 ] = real2, arrTime[ 3 ] = imag2, ... The
   * output arrTime is organized by the same scheme.
   * 
   * @date 25.03.2010 19:56:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#reverse(double[])
   */
  @Override
  public double[ ] reverse( double[ ] arrFreq ) {
    
    int m = arrFreq.length;
    double[ ] arrTime = new double[ m ]; // result
    
    int n = m >> 1; // half of m
    
    for( int i = 0; i < n; i++ ) {
      
      int iR = i * 2;
      int iC = i * 2 + 1;
      
      arrTime[ iR ] = 0.;
      arrTime[ iC ] = 0.;
      
      double arg = 2. * Math.PI * (double)i / (double)n;
      
      for( int k = 0; k < n; k++ ) {
        
        int kR = k * 2;
        int kC = k * 2 + 1;
        
        double cos = Math.cos( k * arg );
        double sin = Math.sin( k * arg );
        
        arrTime[ iR ] += arrFreq[ kR ] * cos - arrFreq[ kC ] * sin;
        arrTime[ iC ] += arrFreq[ kR ] * sin + arrFreq[ kC ] * cos;
        
      } // k
      
    } // i
    
    return arrTime;
  } // reverse
  
  /**
   * The 1-D forward version of the Discrete Fourier Transform (DFT); The input
   * array arrTime is organized by a class called Complex keeping real and
   * imaginary part of a complex number. The output arrFreq is organized by the
   * same scheme.
   * 
   * @date 23.11.2010 18:57:34
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrTime
   *          array of type Complex keeping coefficients of complex numbers
   * @return array of type Complex keeping the discrete fourier transform
   *         coefficients
   */
  public Complex[ ] forward( Complex[ ] arrTime ) {
    
    int n = arrTime.length;
    
    Complex[ ] arrFreq = new Complex[ n ]; // result
    
    for( int i = 0; i < n; i++ ) {
      
      arrFreq[ i ] = new Complex( ); // 0. , 0.
      
      double arg = -2. * Math.PI * (double)i / (double)n;
      
      for( int k = 0; k < n; k++ ) {
        
        double cos = Math.cos( k * arg );
        double sin = Math.sin( k * arg );
        
        double real = arrTime[ k ].getReal( );
        double imag = arrTime[ k ].getImag( );
        
        arrFreq[ i ].addReal( real * cos - imag * sin );
        arrFreq[ i ].addImag( real * sin + imag * cos );
        
      } // k
      
      arrFreq[ i ].mulReal( 1. / (double)n );
      arrFreq[ i ].mulImag( 1. / (double)n );
      
    } // i
    
    return arrFreq;
  } // forward
  
  /**
   * The 1-D reverse version of the Discrete Fourier Transform (DFT); The input
   * array arrFreq is organized by a class called Complex keeping real and
   * imaginary part of a complex number. The output arrTime is organized by the
   * same scheme.
   * 
   * @date 23.11.2010 19:02:12
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @param arrFreq
   *          array of type Complex keeping the discrete fourier transform
   *          coefficients
   * @return array of type Complex keeping coefficients of tiem domain
   */
  public Complex[ ] reverse( Complex[ ] arrFreq ) {
    
    int n = arrFreq.length;
    Complex[ ] arrTime = new Complex[ n ]; // result
    
    for( int i = 0; i < n; i++ ) {
      
      arrTime[ i ] = new Complex( ); // 0. , 0. 
      
      double arg = 2. * Math.PI * (double)i / (double)n;
      
      for( int k = 0; k < n; k++ ) {
        
        double cos = Math.cos( k * arg );
        double sin = Math.sin( k * arg );
        
        double real = arrFreq[ k ].getReal( );
        double imag = arrFreq[ k ].getImag( );
        
        arrTime[ i ].addReal( real * cos - imag * sin );
        arrTime[ i ].addImag( real * sin + imag * cos );
        
      } // k
      
    } // i
    
    return arrTime;
  } // reverse
  
  /**
   * The 2-D forward version of the Discrete Fourier Transform (DFT); The input
   * array matTime is organized by real and imaginary parts of a complex number
   * using even and odd places for the indices. For example: matTime[0][0] =
   * real11, matTime[0][1] = imag11, matTime[0][2] = real12, matTime[0][3] =
   * imag12, matTime[1][0] = real21, matTime[1][1] = imag21, matTime[1][2] =
   * real22, matTime[1][3] = imag2... The output matFreq is organized by the
   * same scheme.
   * 
   * @date 25.03.2010 19:56:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#forward(double[][])
   */
  @Override
  public double[ ][ ] forward( double[ ][ ] matTime ) {
    return null;
  } // forward
  
  /**
   * The 2-D reverse version of the Discrete Fourier Transform (DFT); The input
   * array matFreq is organized by real and imaginary parts of a complex number
   * using even and odd places for the indices. For example: matFreq[0][0] =
   * real11, matFreq[0][1] = imag11, matFreq[0][2] = real12, matFreq[0][3] =
   * imag12, matFreq[1][0] = real21, matFreq[1][1] = imag21, matFreq[1][2] =
   * real22, matFreq[1][3] = imag2... The output matTime is organized by the
   * same scheme.
   * 
   * @date 25.03.2010 19:56:29
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#reverse(double[][])
   */
  @Override
  public double[ ][ ] reverse( double[ ][ ] matFreq ) {
    return null;
  } // reverse
  
  /**
   * The 3-D forward version of the Discrete Fourier Transform (DFT);
   * 
   * @date 10.07.2010 18:10:43
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#forward(double[][][])
   */
  @Override
  public double[ ][ ][ ] forward( double[ ][ ][ ] spcTime ) {
    return null;
  } // forward
  
  /**
   * The 3-D reverse version of the Discrete Fourier Transform (DFT);
   * 
   * @date 10.07.2010 18:10:45
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @see math.jwave.transforms.BasicTransform#reverse(double[][][])
   */
  @Override
  public double[ ][ ][ ] reverse( double[ ][ ][ ] spcHilb ) {
    return null;
  } // reverse
  
} // class

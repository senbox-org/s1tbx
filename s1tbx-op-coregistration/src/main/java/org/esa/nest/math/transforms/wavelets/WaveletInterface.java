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
package org.esa.nest.math.transforms.wavelets;

/**
 * Interface for the Wavelet class
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com) 10.02.2014 21:01:32
 */
public interface WaveletInterface {

  /**
   * Returns a String keeping the name of the current Wavelet.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 17.08.2014 10:59:13
   * @return String keeping the name of the wavelet
   */
  public String getName( );

  /**
   * Shifts scaling and wavelet over some hilbert in forward manners.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 21:01:56
   * @param values
   * @return
   */
  public double[ ] forward( double[ ] arrTime, int arrTimeLength );

  /**
   * Shifts scaling and wavelet over some hilbert in reverse manners.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 21:02:23
   * @param values
   * @return
   */
  public double[ ] reverse( double[ ] arrHilb, int arrTimeLength );

  /**
   * Returns the wavelength of the so called mother wavelet or scaling function.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:06:12
   * @return
   */
  public int getMotherWavelength( );

  /**
   * Returns the minimal necessary wavelength for a signal that can be
   * transformed by this wavelet.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:08:43
   * @return integer representing minimal wavelength of the input signal that
   *         should be transformed by this wavelet.
   */
  public int getTransformWavelength( );

  /**
   * Returns a copy of the scaling (low pass filter) coefficients of the
   * decomposition.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2010 22:11:42
   * @return array of length of the mother wavelet wavelength keeping the
   *         decomposition low pass filter coefficients
   */
  public double[ ] getScalingDeComposition( );

  /**
   * Returns a copy of the wavelet (low pass filter) coefficients of the
   * decomposition.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:11:25
   * @return array of length of the mother wavelet wavelength keeping the
   *         decomposition high pass filter coefficients
   */
  public double[ ] getWaveletDeComposition( );

  /**
   * Returns a copy of the scaling (low pass filter) coefficients of the
   * reconstruction.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 10:35:11
   * @return array of length of the mother wavelet wavelength keeping the
   *         reconstruction low pass filter coefficients
   */
  public double[ ] getScalingReConstruction( );

  /**
   * Returns a copy of the wavelet (high pass filter) coefficients of the
   * reconstruction.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 10:35:09
   * @return array of length of the mother wavelet wavelength keeping the
   *         reconstruction high pass filter coefficients
   */
  public double[ ] getWaveletReConstruction( );

} // WaveletInterface
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
package org.esa.nest.math.transforms.wavelets.coiflet;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal Coiflet wavelet of 12 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 15.02.2014 22:33:55
 */
public class Coiflet2 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/coif2/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 22:33:55
   */
  public Coiflet2( ) {

    _name = "Coiflet 2"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 12; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.0007205494453645122;
    _scalingDeCom[ 1 ] = -0.0018232088707029932;
    _scalingDeCom[ 2 ] = 0.0056114348193944995;
    _scalingDeCom[ 3 ] = 0.023680171946334084;
    _scalingDeCom[ 4 ] = -0.0594344186464569;
    _scalingDeCom[ 5 ] = -0.0764885990783064;
    _scalingDeCom[ 6 ] = 0.41700518442169254;
    _scalingDeCom[ 7 ] = 0.8127236354455423;
    _scalingDeCom[ 8 ] = 0.3861100668211622;
    _scalingDeCom[ 9 ] = -0.06737255472196302;
    _scalingDeCom[ 10 ] = -0.04146493678175915;
    _scalingDeCom[ 11 ] = 0.016387336463522112;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Coiflet2

} // Coiflet2

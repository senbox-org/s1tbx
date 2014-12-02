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
package org.esa.nest.math.transforms.wavelets.symlets;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Symlet10 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:23:11
 */
public class Symlet11 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym11/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:11
   */
  public Symlet11( ) {

    _name = "Symlet 11"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 22; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 0.00017172195069934854;
    _scalingDeCom[ 1 ] = -3.8795655736158566e-05;
    _scalingDeCom[ 2 ] = -0.0017343662672978692;
    _scalingDeCom[ 3 ] = 0.0005883527353969915;
    _scalingDeCom[ 4 ] = 0.00651249567477145;
    _scalingDeCom[ 5 ] = -0.009857934828789794;
    _scalingDeCom[ 6 ] = -0.024080841595864003;
    _scalingDeCom[ 7 ] = 0.0370374159788594;
    _scalingDeCom[ 8 ] = 0.06997679961073414;
    _scalingDeCom[ 9 ] = -0.022832651022562687;
    _scalingDeCom[ 10 ] = 0.09719839445890947;
    _scalingDeCom[ 11 ] = 0.5720229780100871;
    _scalingDeCom[ 12 ] = 0.7303435490883957;
    _scalingDeCom[ 13 ] = 0.23768990904924897;
    _scalingDeCom[ 14 ] = -0.2046547944958006;
    _scalingDeCom[ 15 ] = -0.1446023437053156;
    _scalingDeCom[ 16 ] = 0.03526675956446655;
    _scalingDeCom[ 17 ] = 0.04300019068155228;
    _scalingDeCom[ 18 ] = -0.0020034719001093887;
    _scalingDeCom[ 19 ] = -0.006389603666454892;
    _scalingDeCom[ 20 ] = 0.00011053509764272153;
    _scalingDeCom[ 21 ] = 0.0004892636102619239;

    _buildOrthonormalSpace( );

  } // Symlet11

} // Symlet11
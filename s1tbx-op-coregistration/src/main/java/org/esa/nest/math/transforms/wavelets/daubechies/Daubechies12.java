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
package org.esa.nest.math.transforms.wavelets.daubechies;

import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * Ingrid Daubechies' orthonormal Daubechies wavelet of 24 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 19.08.2014 18:10:53
 */
public class Daubechies12 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/db12/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:10:53
   */
  public Daubechies12( ) {

    _name = "Daubechies 12"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 24; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
	
    _scalingDeCom[ 0 ] = -1.5290717580684923e-06;
    _scalingDeCom[ 1 ] = 1.2776952219379579e-05;
    _scalingDeCom[ 2 ] = -2.4241545757030318e-05;
    _scalingDeCom[ 3 ] = -8.850410920820318e-05;
    _scalingDeCom[ 4 ] = 0.0003886530628209267;
    _scalingDeCom[ 5 ] = 6.5451282125215034e-06;
    _scalingDeCom[ 6 ] = -0.0021795036186277044;
    _scalingDeCom[ 7 ] = 0.0022486072409952287;
    _scalingDeCom[ 8 ] = 0.006711499008795549;
    _scalingDeCom[ 9 ] = -0.012840825198299882;
    _scalingDeCom[ 10 ] = -0.01221864906974642;
    _scalingDeCom[ 11 ] = 0.04154627749508764;
    _scalingDeCom[ 12 ] = 0.010849130255828966;
    _scalingDeCom[ 13 ] = -0.09643212009649671;
    _scalingDeCom[ 14 ] = 0.0053595696743599965;
    _scalingDeCom[ 15 ] = 0.18247860592758275;
    _scalingDeCom[ 16 ] = -0.023779257256064865;
    _scalingDeCom[ 17 ] = -0.31617845375277914;
    _scalingDeCom[ 18 ] = -0.04476388565377762;
    _scalingDeCom[ 19 ] = 0.5158864784278007;
    _scalingDeCom[ 20 ] = 0.6571987225792911;
    _scalingDeCom[ 21 ] = 0.3773551352142041;
    _scalingDeCom[ 22 ] = 0.10956627282118277;
    _scalingDeCom[ 23 ] = 0.013112257957229239;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Daubechies12

} // Daubechies12
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
 * Ingrid Daubechies' orthonormal Coiflet wavelet of 30 coefficients.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 01:49:39
 */
public class Coiflet5 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/coif5/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 01:49:39
   */
  public Coiflet5( ) {

    _name = "Coiflet 5"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 30; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -9.517657273819165e-08;
    _scalingDeCom[ 1 ] = -1.6744288576823017e-07;
    _scalingDeCom[ 2 ] = 2.0637618513646814e-06;
    _scalingDeCom[ 3 ] = 3.7346551751414047e-06;
    _scalingDeCom[ 4 ] = -2.1315026809955787e-05;
    _scalingDeCom[ 5 ] = -4.134043227251251e-05;
    _scalingDeCom[ 6 ] = 0.00014054114970203437;
    _scalingDeCom[ 7 ] = 0.00030225958181306315;
    _scalingDeCom[ 8 ] = -0.0006381313430451114;
    _scalingDeCom[ 9 ] = -0.0016628637020130838;
    _scalingDeCom[ 10 ] = 0.0024333732126576722;
    _scalingDeCom[ 11 ] = 0.006764185448053083;
    _scalingDeCom[ 12 ] = -0.009164231162481846;
    _scalingDeCom[ 13 ] = -0.01976177894257264;
    _scalingDeCom[ 14 ] = 0.03268357426711183;
    _scalingDeCom[ 15 ] = 0.0412892087501817;
    _scalingDeCom[ 16 ] = -0.10557420870333893;
    _scalingDeCom[ 17 ] = -0.06203596396290357;
    _scalingDeCom[ 18 ] = 0.4379916261718371;
    _scalingDeCom[ 19 ] = 0.7742896036529562;
    _scalingDeCom[ 20 ] = 0.4215662066908515;
    _scalingDeCom[ 21 ] = -0.05204316317624377;
    _scalingDeCom[ 22 ] = -0.09192001055969624;
    _scalingDeCom[ 23 ] = 0.02816802897093635;
    _scalingDeCom[ 24 ] = 0.023408156785839195;
    _scalingDeCom[ 25 ] = -0.010131117519849788;
    _scalingDeCom[ 26 ] = -0.004159358781386048;
    _scalingDeCom[ 27 ] = 0.0021782363581090178;
    _scalingDeCom[ 28 ] = 0.00035858968789573785;
    _scalingDeCom[ 29 ] = -0.00021208083980379827;

    _buildOrthonormalSpace( ); // build all other coefficients from low pass decomposition

  } // Coiflet5

} // Coiflet5

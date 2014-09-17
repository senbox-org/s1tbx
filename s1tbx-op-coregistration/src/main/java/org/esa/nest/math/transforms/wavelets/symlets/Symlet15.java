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
 * @date 19.08.2014 18:23:15
 */
public class Symlet15 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym15/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:15
   */
  public Symlet15( ) {

    _name = "Symlet 15"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 30; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 9.712419737963348e-06;
    _scalingDeCom[ 1 ] = -7.35966679891947e-06;
    _scalingDeCom[ 2 ] = -0.00016066186637495343;
    _scalingDeCom[ 3 ] = 5.512254785558665e-05;
    _scalingDeCom[ 4 ] = 0.0010705672194623959;
    _scalingDeCom[ 5 ] = -0.0002673164464718057;
    _scalingDeCom[ 6 ] = -0.0035901654473726417;
    _scalingDeCom[ 7 ] = 0.003423450736351241;
    _scalingDeCom[ 8 ] = 0.01007997708790567;
    _scalingDeCom[ 9 ] = -0.01940501143093447;
    _scalingDeCom[ 10 ] = -0.03887671687683349;
    _scalingDeCom[ 11 ] = 0.021937642719753955;
    _scalingDeCom[ 12 ] = 0.04073547969681068;
    _scalingDeCom[ 13 ] = -0.04108266663538248;
    _scalingDeCom[ 14 ] = 0.11153369514261872;
    _scalingDeCom[ 15 ] = 0.5786404152150345;
    _scalingDeCom[ 16 ] = 0.7218430296361812;
    _scalingDeCom[ 17 ] = 0.2439627054321663;
    _scalingDeCom[ 18 ] = -0.1966263587662373;
    _scalingDeCom[ 19 ] = -0.1340562984562539;
    _scalingDeCom[ 20 ] = 0.06839331006048024;
    _scalingDeCom[ 21 ] = 0.06796982904487918;
    _scalingDeCom[ 22 ] = -0.008744788886477952;
    _scalingDeCom[ 23 ] = -0.01717125278163873;
    _scalingDeCom[ 24 ] = 0.0015261382781819983;
    _scalingDeCom[ 25 ] = 0.003481028737064895;
    _scalingDeCom[ 26 ] = -0.00010815440168545525;
    _scalingDeCom[ 27 ] = -0.00040216853760293483;
    _scalingDeCom[ 28 ] = 2.171789015077892e-05;
    _scalingDeCom[ 29 ] = 2.866070852531808e-05;

    _buildOrthonormalSpace( );

  } // Symlet15

} // Symlet15
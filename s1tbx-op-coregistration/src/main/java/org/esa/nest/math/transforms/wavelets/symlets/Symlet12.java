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
 * @date 19.08.2014 18:23:12
 */
public class Symlet12 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym12/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 19.08.2014 18:23:12
   */
  public Symlet12( ) {

    _name = "Symlet 12"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 24; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 0.00011196719424656033;
    _scalingDeCom[ 1 ] = -1.1353928041541452e-05;
    _scalingDeCom[ 2 ] = -0.0013497557555715387;
    _scalingDeCom[ 3 ] = 0.00018021409008538188;
    _scalingDeCom[ 4 ] = 0.007414965517654251;
    _scalingDeCom[ 5 ] = -0.0014089092443297553;
    _scalingDeCom[ 6 ] = -0.024220722675013445;
    _scalingDeCom[ 7 ] = 0.0075537806116804775;
    _scalingDeCom[ 8 ] = 0.04917931829966084;
    _scalingDeCom[ 9 ] = -0.03584883073695439;
    _scalingDeCom[ 10 ] = -0.022162306170337816;
    _scalingDeCom[ 11 ] = 0.39888597239022;
    _scalingDeCom[ 12 ] = 0.7634790977836572;
    _scalingDeCom[ 13 ] = 0.46274103121927235;
    _scalingDeCom[ 14 ] = -0.07833262231634322;
    _scalingDeCom[ 15 ] = -0.17037069723886492;
    _scalingDeCom[ 16 ] = 0.01530174062247884;
    _scalingDeCom[ 17 ] = 0.05780417944550566;
    _scalingDeCom[ 18 ] = -0.0026043910313322326;
    _scalingDeCom[ 19 ] = -0.014589836449234145;
    _scalingDeCom[ 20 ] = 0.00030764779631059454;
    _scalingDeCom[ 21 ] = 0.002350297614183465;
    _scalingDeCom[ 22 ] = -1.8158078862617515e-05;
    _scalingDeCom[ 23 ] = -0.0001790665869750869;

    _buildOrthonormalSpace( );

  } // Symlet12

} // Symlet12
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
 * Symlet2 filter: near symmetric, orthogonal (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 13:40:30
 * @contact cscheiblich@gmail.com
 */
public class Symlet2 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/sym2/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 13:40:30
   */
  public Symlet2( ) {
    
    _name = "Symlet 2"; // name of the wavelet

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 4; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = -0.12940952255092145;
    _scalingDeCom[ 1 ] = 0.22414386804185735;
    _scalingDeCom[ 2 ] = 0.836516303737469;
    _scalingDeCom[ 3 ] = 0.48296291314469025;

    //    _waveletDeCom = new double[ _motherWavelength ];
    //    _waveletDeCom[ 0 ] = -0.48296291314469025;
    //    _waveletDeCom[ 1 ] = 0.836516303737469;
    //    _waveletDeCom[ 2 ] = -0.22414386804185735;
    //    _waveletDeCom[ 3 ] = -0.12940952255092145;
    //
    //    _scalingReCon = new double[ _motherWavelength ];
    //    _scalingReCon[ 0 ] = 0.48296291314469025;
    //    _scalingReCon[ 1 ] = 0.836516303737469;
    //    _scalingReCon[ 2 ] = 0.22414386804185735;
    //    _scalingReCon[ 3 ] = -0.12940952255092145;
    //
    //    _waveletReCon = new double[ _motherWavelength ];
    //    _waveletReCon[ 0 ] = -0.12940952255092145;
    //    _waveletReCon[ 1 ] = -0.22414386804185735;
    //    _waveletReCon[ 2 ] = 0.836516303737469;
    //    _waveletReCon[ 3 ] = -0.48296291314469025;

    _buildOrthonormalSpace( );

  } // Symlet2

} // Symlet2

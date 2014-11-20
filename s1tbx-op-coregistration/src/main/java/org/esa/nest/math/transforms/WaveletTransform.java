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

import org.esa.nest.math.exceptions.JWaveFailure;
import org.esa.nest.math.transforms.wavelets.Wavelet;

/**
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 15.02.2014 21:05:33
 */
public abstract class WaveletTransform extends BasicTransform {

  /**
   * The used wavelet for transforming
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 21:05:33
   */
  protected Wavelet _wavelet;

  /**
   * Constructor checks whether the given object is all right.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 15.02.2014 21:05:33
   * @param wavelet
   *          object of type Wavelet
   * @throws JWaveFailure
   *           if given object is null or not of type wavelet
   */
  protected WaveletTransform( Wavelet wavelet ) throws JWaveFailure {

    if( wavelet == null )
      throw new JWaveFailure( "given object is null!" );

    if( !( wavelet instanceof Wavelet ) )
      throw new JWaveFailure( "given object is not of type Wavelet" );

    _wavelet = wavelet;

  } // check for objects od type Wavelet

} // WaveletTransform
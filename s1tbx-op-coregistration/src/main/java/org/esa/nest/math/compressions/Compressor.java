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
 * @date 20.02.2014 23:41:35 
 *
 * Compressor.java
 */
package org.esa.nest.math.compressions;

import org.esa.nest.math.exceptions.JWaveException;
import org.esa.nest.math.exceptions.JWaveFailure;

/**
 * Some how this class is doing the same as the technical counterpart is doing -
 * compressing data that is transformed to Hilbert space by different methods.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 20.02.2014 23:41:35
 */
public abstract class Compressor {

  /**
   * A threshold that is used in several compression methods.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:44:26
   */
  protected double _threshold = 1.;

  /**
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:41:35
   */
  public Compressor( ) {

    _threshold = 1.;

  } // Compressor

  public Compressor( double threshold ) throws JWaveException {

    if( threshold < 0. )
      throw new JWaveFailure( "given threshold is negative" );

    _threshold = threshold;

  } // Compressor

  /**
   * Interface for arrays for driving the different compression methods.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:48:06
   * @param arrHilb
   * @return
   */
  abstract protected double[ ] compress( double[ ] arrHilb );

  /**
   * Interface for matrices for driving the different compression methods.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:54:11
   * @param matHilb
   * @return
   */
  abstract protected double[ ][ ] compress( double[ ][ ] matHilb );

  /**
   * Interface for spaces for driving the different compression methods.
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 20.02.2014 23:54:52
   * @param spcHilb
   * @return
   */
  abstract protected double[ ][ ][ ] compress( double[ ][ ][ ] spcHilb );

} // Compressor

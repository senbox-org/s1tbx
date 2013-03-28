/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.processor.smac;

import org.esa.beam.util.Guardian;

/**
 * Implements the SMAC algorithm as specified by the original source code of H.Rahman and G.Dedieu.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
final class SmacAlgorithm {

    private static final double _cdr = Math.PI / 180.0;
    private static final double _crd = 180.0 / Math.PI;
    private static final double _invMaxPressure = 1.0 / 1013.0;
    private static final double _oneQuarter = 1.0 / 4.0;
    private static final double _twoThird = 2.0 / 3.0;

    // sensor calibration variables
    // ----------------------------
    private double _A0taup;
    private double _A1taup;
    private double _a0P;
    private double _a1P;
    private double _a2P;
    private double _a3P;
    private double _a4P;
    private double _ao3;
    private double _no3;
    private double _ah2o;
    private double _nh2o;
    private double _ao2;
    private double _no2;
    private double _po2;
    private double _aco2;
    private double _nco2;
    private double _pco2;
    private double _ach4;
    private double _nch4;
    private double _pch4;
    private double _ano2;
    private double _nno2;
    private double _pno2;
    private double _aco;
    private double _nco;
    private double _pco;
    private double _a0T;
    private double _a1T;
    private double _a2T;
    private double _a3T;
    private double _a0s;
    private double _a1s;
    private double _a2s;
    private double _a3s;
    private double _taur;
    private double _resr1;
    private double _resr2;
    private double _resr3;
    private double _resa1;
    private double _resa2;
    private double _resa3;
    private double _resa4;
    private double _rest1;
    private double _rest2;
    private double _rest3;
    private double _rest4;
    private double _wo, _onemwo;
    private double _gc;
    private double _ak, _ak2, _pfac;
    private double _b, _onepb, _onemb, _onepb2, _onemb2;
    private double _ww;

    private boolean _calcTo3;
    private boolean _calcTh2o;
    private boolean _calcTo2;
    private boolean _calcUo2;
    private boolean _calcTco2;
    private boolean _calcUco2;
    private boolean _calcTch4;
    private boolean _calcUch4;
    private boolean _calcTno2;
    private boolean _calcUno2;
    private boolean _calcTco;
    private boolean _calcUco;

    /**
     * Constructs the object with default parameters
     */
    public SmacAlgorithm() {
    }

    /**
     * Sets the sensor adjustment coefficients. Must be set BEFORE running the algorithm.
     *
     * @param coeffs a class implementing the <code>SmacSensorCoefficients</code>
     */
    public final void setSensorCoefficients(SmacSensorCoefficients coeffs) {
        Guardian.assertNotNull("coefficients", coeffs);

        _A0taup = coeffs.getA0taup();
        _A1taup = coeffs.getA1taup();

        // ozone coefficients
        _ao3 = coeffs.getAo3();
        _no3 = coeffs.getNo3();
        _calcTo3 = true;
        if (_ao3 == 0.0) {
            _calcTo3 = false;
        }

        // water vapour coefficients
        _ah2o = coeffs.getAh2o();
        _nh2o = coeffs.getNh2o();
        _calcTh2o = true;
        if (_ah2o == 0.0) {
            _calcTh2o = false;
        }

        // oxygene coefficients
        _ao2 = coeffs.getAo2();
        _no2 = coeffs.getNo2();
        _calcTo2 = true;
        if (_ao2 == 0.0) {
            _calcTo2 = false;
        }
        _po2 = coeffs.getPo2();
        _calcUo2 = true;
        if (_po2 == 0.0) {
            _calcUo2 = false;
        }

        // co2 coefficients
        _aco2 = coeffs.getAco2();
        _nco2 = coeffs.getNco2();
        _calcTco2 = true;
        if (_aco2 == 0.0) {
            _calcTco2 = false;
        }
        _pco2 = coeffs.getPco2();
        _calcUco2 = true;
        if (_pco2 == 0.0) {
            _calcUco2 = false;
        }

        // methane coefficients
        _ach4 = coeffs.getAch4();
        _nch4 = coeffs.getNch4();
        _calcTch4 = true;
        if (_ach4 == 0.0) {
            _calcTch4 = false;
        }
        _pch4 = coeffs.getPch4();
        _calcUch4 = true;
        if (_pch4 == 0.0) {
            _calcUch4 = false;
        }

        // no2 coefficients
        _ano2 = coeffs.getAno2();
        _nno2 = coeffs.getNno2();
        _calcTno2 = true;
        if (_ano2 == 0.0) {
            _calcTno2 = false;
        }
        _pno2 = coeffs.getPno2();
        _calcUno2 = true;
        if (_pno2 == 0.0) {
            _calcUno2 = false;
        }

        // co coefficients
        _aco = coeffs.getAco();
        _nco = coeffs.getNco();
        _calcTco = true;
        if (_aco == 0.0) {
            _calcTco = false;
        }
        _pco = coeffs.getPco();
        _calcUco = true;
        if (_pco == 0.0) {
            _calcUco = false;
        }

        // scattering transmission coefficients
        _a0T = coeffs.getA0T();
        _a1T = coeffs.getA1T();
        _a2T = coeffs.getA2T();
        _a3T = coeffs.getA3T();

        // spherical albedo coefficients
        _a0s = coeffs.getA0s();
        _a1s = coeffs.getA1s();
        _a2s = coeffs.getA2s();
        _a3s = coeffs.getA3s();

        // molecular optical depth
        _taur = coeffs.getTaur();

        // residual rayleigh
        _resr1 = coeffs.getResr1();
        _resr2 = coeffs.getResr2();
        _resr3 = coeffs.getResr3();

        // aerosol reflectance
        _a0P = coeffs.getA0P();
        _a1P = coeffs.getA1P();
        _a2P = coeffs.getA2P();
        _a3P = coeffs.getA3P();
        _a4P = coeffs.getA4P();

        _wo = coeffs.getWo();
        _gc = coeffs.getGc();

        // residual aerosols
        _resa1 = coeffs.getResa1();
        _resa2 = coeffs.getResa2();
        _resa3 = coeffs.getResa3();
        _resa4 = coeffs.getResa4();

        // residual transmission
        _rest1 = coeffs.getRest1();
        _rest2 = coeffs.getRest2();
        _rest3 = coeffs.getRest3();
        _rest4 = coeffs.getRest4();

        // do some calculations which are NOT product dependent
        // ----------------------------------------------------
        _ak2 = (1.0 - _wo) * 3.0 * (1.0 - _wo * _gc);
        _ak = Math.sqrt(_ak2);
        _b = _twoThird * _ak / (1.0 - _wo * _gc);
        _onepb = 1.0 + _b;
        _onepb2 = _onepb * _onepb;
        _onemb = 1.0 - _b;
        _onemb2 = _onemb * _onemb;
        _ww = _wo * _oneQuarter;
        _onemwo = 1.0 - _wo;
        _pfac = _ak / (3.0 * (1.0 - _wo * _gc));
    }

    /**
     * Performs the SMAC algorithm.
     *
     * @param sza           array of sun zenith angles in decimal degrees
     * @param saa           array of sun azimuth angles in decimal degrees
     * @param vza           array of view zenith angles in decimal degrees
     * @param vaa           array of view azimuth angles in decimal degrees
     * @param taup550       array of aerosol optical thickness at 550nm
     * @param uh2o          array of water vapour concentrations
     * @param uo3           array of ozone concentrations
     * @param airPressure   array of air pressure in hPa
     * @param process       boolean array indicating whether a pixel has to be processed or not
     * @param invalid       the value set for invalid pixels, i.e. the ones excluded by the process parameter
     * @param r_toa         array of top of atmosphere reflectances to be corrected
     * @param r_surfRecycle if not <code>null</code> and of correct size this array will be reused for the return
     *                      values
     *
     * @return array of corrected surface reflectances
     */
    public final float[] run(float[] sza, float[] saa, float[] vza, float[] vaa,
                             float[] taup550, float[] uh2o, float[] uo3,
                             float[] airPressure, boolean[] process, float invalid, float[] r_toa,
                             float[] r_surfRecycle) {
        // array to be returned
        float[] r_return;
        double us, invUs, us2, uv, invUv, usTimesuv, invUsTimesUv;
        double dphi, Peq, m, s, cksi, ksiD;
        double taup, tautot, Res_6s;
        double uo2, uco2, uch4, uno2, uco;
        double to3, th2o, to2, tco2, tch4, tno2, tco;
        double ttetas, ttetav;
        double ray_phase, ray_ref, taurz, Res_ray;
        double aer_phase, aer_ref, Res_aer;
        double atm_ref, tg;
        double d, del, dp, e, f, ss;
        double q1, q2, q3;
        double c1, c2, cp1, cp2;
        double x, y, z;
        double aa1, aa2, aa3;
        double temp;

        // try to reuse the recyle array to prevent memory waste. We can reuse if
        // a) it's present and
        // b) has the same size as the input vector
        if ((r_surfRecycle == null) || (r_surfRecycle.length != r_toa.length)) {
            r_return = new float[r_toa.length];
        } else {
            r_return = r_surfRecycle;
        }

        // loop over vectors
        // -----------------
        int n;
        for (n = 0; n < r_toa.length; n++) {
            // check for process flag. If set to false we must set the default value for
            // invalid pixels and process the next pixel
            if (!process[n]) {
                r_return[n] = invalid;
                continue;
            }
            // parameter setup
            us = Math.cos(sza[n] * _cdr);
            invUs = 1.0 / us;
            us2 = us * us;

            uv = Math.cos(vza[n] * _cdr);
            invUv = 1.0 / uv;
            usTimesuv = us * uv;
            invUsTimesUv = 1.0 / usTimesuv;

            dphi = (saa[n] - vaa[n]) * _cdr;
            Peq = airPressure[n] * _invMaxPressure;

            /*------ 1) air mass */
            m = invUs + invUv;

            /*------ 2) aerosol optical depth in the spectral band, taup  */
            taup = _A0taup + _A1taup * taup550[n];

            /*------ 3) gaseous transmissions (downward and upward paths)*/
            uo2 = 1.0;
            if (_calcUo2) {
                uo2 = Math.pow(Peq, _po2);
            }
            uco2 = 1.0;
            if (_calcUco2) {
                uco2 = Math.pow(Peq, _pco2);
            }
            uch4 = 1.0;
            if (_calcUch4) {
                uch4 = Math.pow(Peq, _pch4);
            }
            uno2 = 1.0;
            if (_calcUno2) {
                uno2 = Math.pow(Peq, _pno2);
            }
            uco = 1.0;
            if (_calcUco) {
                uco = Math.pow(Peq, _pco);
            }

            /*------ 4) if uh2o <= 0 and uo3 <= 0 no gaseous absorption is computed*/
            to3 = 1.0;
            th2o = 1.0;
            to2 = 1.0;
            tco2 = 1.0;
            tch4 = 1.0;
            tno2 = 1.0;
            tco = 1.0;
            if ((uh2o[n] > 0.) || (uo3[n] > 0.)) {
                if (_calcTo3) {
                    to3 = Math.exp(_ao3 * Math.pow((uo3[n] * m), _no3));
                }
                if (_calcTh2o) {
                    th2o = Math.exp(_ah2o * Math.pow((uh2o[n] * m), _nh2o));
                }
                if (_calcTo2) {
                    to2 = Math.exp(_ao2 * Math.pow((uo2 * m), _no2));
                }
                if (_calcTco2) {
                    tco2 = Math.exp(_aco2 * Math.pow((uco2 * m), _nco2));
                }
                if (_calcTch4) {
                    tch4 = Math.exp(_ach4 * Math.pow((uch4 * m), _nch4));
                }
                if (_calcTno2) {
                    tno2 = Math.exp(_ano2 * Math.pow((uno2 * m), _nno2));
                }
                if (_calcTco) {
                    tco = Math.exp(_aco * Math.pow((uco * m), _nco));
                }
            }

            /*------  5) Total scattering transmission */
            temp = _a2T * Peq + _a3T;
            /* downward */
            ttetas = _a0T + _a1T * taup550[n] * invUs + temp / (1.0 + us);
            /* upward   */
            ttetav = _a0T + _a1T * taup550[n] * invUv + temp / (1.0 + uv);

            /*------ 6) spherical albedo of the atmosphere */
            s = _a0s * Peq + _a3s + _a1s * taup550[n] + _a2s * taup550[n] * taup550[n];

            /*------ 7) scattering angle cosine */
            cksi = -(usTimesuv + (Math.sqrt(1.0 - us2) * Math.sqrt(1.0 - uv * uv) * Math.cos(dphi)));
            if (cksi < -1) {
                cksi = -1.0;
            }

            /*------ 8) scattering angle in degree */
            ksiD = _crd * Math.acos(cksi);

            /*------ 9) rayleigh atmospheric reflectance */
            /* pour 6s on a delta = 0.0279 */
            ray_phase = 0.7190443 * (1.0 + (cksi * cksi)) + 0.0412742;
            taurz = _taur * Peq;
            ray_ref = (taurz * ray_phase) * _oneQuarter * invUsTimesUv;

            /*-----------------Residu Rayleigh ---------*/
            temp = taurz * ray_phase * invUsTimesUv;
            Res_ray = _resr1 + _resr2 * temp + _resr3 * temp * temp;

            /*------ 10) aerosol atmospheric reflectance */
            temp = ksiD * ksiD;
            aer_phase = _a0P + _a1P * ksiD + _a2P * temp + _a3P * temp * ksiD + _a4P * temp * temp;

            // now the uncommented block :-)
            // -----------------------------
            temp = 1.0 / (4.0 * (1.0 - _ak2 * us2));
            e = -3.0 * us2 * _wo * temp;
            f = -_onemwo * 3.0 * _gc * us2 * _wo * temp;
            dp = e / (3.0 * us) + us * f;
            d = e + f;
            del = Math.exp(_ak * taup) * _onepb2 - Math.exp(-_ak * taup) * _onemb2;
            ss = us / (1.0 - _ak2 * us2);
            temp = 3.0 * us;
            q1 = 2.0 + temp + _onemwo * temp * _gc * (1.0 + 2.0 * us);
            q2 = 2.0 - temp - _onemwo * temp * _gc * (1.0 - 2.0 * us);
            q3 = q2 * Math.exp(-taup * invUs);
            temp = (_ww * ss) / del;
            c1 = temp * (q1 * Math.exp(_ak * taup) * _onepb + q3 * _onemb);
            c2 = -temp * (q1 * Math.exp(-_ak * taup) * _onemb + q3 * _onepb);
            cp1 = c1 * _pfac;
            cp2 = -c2 * _pfac;
            temp = _wo * 3.0 * _gc * uv;
            z = d - temp * dp + _wo * aer_phase * _oneQuarter;
            x = c1 - temp * cp1;
            y = c2 - temp * cp2;
            temp = _ak * uv;
            aa1 = uv / (1.0 + temp);
            aa2 = uv / (1.0 - temp);
            aa3 = usTimesuv / (us + uv);

            aer_ref = x * aa1 * (1.0 - Math.exp(-taup / aa1));
            aer_ref += y * aa2 * (1.0 - Math.exp(-taup / aa2));
            aer_ref += z * aa3 * (1.0 - Math.exp(-taup / aa3));
            aer_ref *= invUsTimesUv;

            /*--------Residu Aerosol --------*/
            temp = taup * m * cksi;
            Res_aer = (_resa1 + _resa2 * temp + _resa3 * temp * temp)
                      + _resa4 * temp * temp * temp;

            /*---------Residu 6s-----------*/
            tautot = taup + taurz;
            temp = tautot * m * cksi;
            Res_6s = (_rest1 + _rest2 * temp + _rest3 * temp * temp)
                     + _rest4 * temp * temp * temp;

            /*------ 11) total atmospheric reflectance */
            atm_ref = ray_ref - Res_ray + aer_ref - Res_aer + Res_6s;

            /*-------- reflectance at toa*/
            tg = th2o * to3 * to2 * tco2 * tch4 * tco * tno2;

            /* reflectance at surface */
            /*------------------------ */
            temp = r_toa[n] - (atm_ref * tg);
            temp = temp / ((tg * ttetas * ttetav) + (temp * s));
            r_return[n] = (float) temp;
        }

        return r_return;
    }
}
package org.esa.snap.core.nn;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

/**
 * This class is for using a Neural Net (NN) of type ffbp in a Java program. The
 * program for training such a NN "ffbp1.0" was written in C by
 *
 * @author H.Schiller. You can get this program (including documentation) <a
 *         href="http://gfesun1.gkss.de/software/ffbp/">here </a>. The class
 *         only works for NN's (i.e. ".net"-files) generated with "ffbp1.0".
 * @author H. Schiller modified by K.Schiller Copyright GKSS/KOF Created on
 *         04.11.2003
 */
public class NNffbpAlphaTabFast {

    /**
     * Specifies the cutting of the activation function. For values below
     * alphaStart alphaTab[0] is used; for values greater (-alphaStart)
     * alphaTab[nAlpha - 1] is used.
     */
    private static final double ALPHA_START = -10.0;
    /**
     * Specifies the length of the table containing the tabulated activation
     * function.
     */
    private static final int NUM_ALPHA = 100000;

    /**
     * The vector contains the smallest value for each input varible to the NN
     * seen during the training phase.
     */
    private double[] inmin;
    /**
     * The vector contains the biggest value for each input varible to the NN
     * seen during the training phase.
     */
    private double[] inmax;
    /**
     * The vector contains the smallest value for each output varible to the NN
     * seen during the training phase.
     */
    private double[] outmin;
    /**
     * The vector contains the biggest value for each output varible to the NN
     * seen during the training phase.
     */
    private double[] outmax;
    /**
     * The number of planes of the NN.
     */
    private int nplanes;
    /**
     * A vector of length {@link #nplanes}containing the number of neurons in
     * each plane.
     */
    private int[] size;
    /**
     * Contains the weight ("connection strength") between each pair of neurons
     * when going from ine plane to the next.
     */
    private double[][][] wgt;
    /**
     * A matrix containing the biases for each neuron in each plane.
     */
    private double[][] bias;
    /**
     * A matrix containing the activation signal of each neuron in each plane.
     */
    private double[][] act;
    /**
     * The number of input variables to the NN.
     */
    private int nn_in;
    /**
     * The number of output variables of the NN.
     */
    private int nn_out;
    /**
     * The table containing the tabulated activation function as used during the
     * training of the NN.
     */
    private double[] alphaTab = new double[NUM_ALPHA];
    /**
     * The reciprocal of the increment of the entries of {@link #alphaTab}.
     */
    private double recDeltaAlpha;

    private double[][][] dActDX;
    private double[][] help;
    private NNCalc NNresjacob;

    /**
     * Creates a neural net by reading the definition from the string.
     *
     * @param neuralNet the neural net definition as a string
     * @throws java.io.IOException if the neural net could not be read
     */
    public NNffbpAlphaTabFast(String neuralNet) throws IOException {
        readNeuralNetFromString(neuralNet);
        makeAlphaTab();
        NNresjacob = new NNCalc();
        declareArrays();
    }

    /**
     * Creates a neural net by reading the definition from the input stream.
     *
     * @param neuralNetStream the neural net definition as a input stream
     * @throws java.io.IOException if the neural net could not be read
     */
    public NNffbpAlphaTabFast(InputStream neuralNetStream) throws IOException {
        this(readNeuralNet(neuralNetStream));
    }

    public double[] getInmin() {
        return inmin;
    }

    public void setInmin(double[] inmin) {
        this.inmin = inmin;
    }

    public double[] getInmax() {
        return inmax;
    }

    public void setInmax(double[] inmax) {
        this.inmax = inmax;
    }

    public double[] getOutmin() {
        return outmin;
    }

    public double[] getOutmax() {
        return outmax;
    }

    /**
     * Method makeAlphaTab When an instance of this class is initialized this
     * method is called and fills the {@link #alphaTab}with the activation
     * function used during the training of the NN.
     */
    private void makeAlphaTab() {
        double delta = (-2.0 * ALPHA_START) / (NUM_ALPHA - 1.0);
        double sum = ALPHA_START + (0.5 * delta);
        for (int i = 0; i < NUM_ALPHA; i++) {
            this.alphaTab[i] = 1.0 / (1.0 + Math.exp(-sum));
            sum += delta;
        }
        this.recDeltaAlpha = 1.0 / delta;
    }

    private static String readNeuralNet(InputStream neuralNetStream) throws IOException {
        String neuralNet;
        BufferedReader reader = new BufferedReader(new InputStreamReader(neuralNetStream));
        try {
            String line = reader.readLine();
            final StringBuilder sb = new StringBuilder();
            while (line != null) {
                // have to append line terminator, cause it's not included in line
                sb.append(line).append('\n');
                line = reader.readLine();
            }
            neuralNet = sb.toString();
        } finally {
            reader.close();
        }
        return neuralNet;
    }

    private void readNeuralNetFromString(String net) throws IOException {
        StringReader in = null;
        try {
            in = new StringReader(net);
            FormattedStringReader inf = new FormattedStringReader(in);
            double[] h;
            inf.noComments();
            char ch = '0';
            while (ch != '#') {
                ch = (char) in.read();
            }
            inf.rString();            //read the rest of the line which
            // has the #
            nn_in = (int) inf.rlong();
            inmin = new double[nn_in];
            inmax = new double[nn_in];
            for (int i = 0; i < nn_in; i++) {
                h = inf.rdouble(2);
                inmin[i] = h[0];
                inmax[i] = h[1];
            }
            nn_out = (int) inf.rlong();
            outmin = new double[nn_out];
            outmax = new double[nn_out];
            for (int i = 0; i < nn_out; i++) {
                h = inf.rdouble(2);
                outmin[i] = h[0];
                outmax[i] = h[1];
            }
            while (ch != '=') {
                ch = (char) in.read();
            }
            in.mark(1000000);
            nplanes = (int) inf.rlong();
            in.reset();
            long[] hh = inf.rlong(nplanes + 1);
            size = new int[nplanes];
            for (int i = 0; i < nplanes; i++) {
                size[i] = (int) hh[i + 1];
            }
            wgt = new double[nplanes - 1][][];
            for (int i = 0; i < nplanes - 1; i++) {
                wgt[i] = new double[size[i + 1]][size[i]];
            }
            bias = new double[nplanes - 1][];
            for (int i = 0; i < nplanes - 1; i++) {
                bias[i] = new double[size[i + 1]];
            }
            act = new double[nplanes][];
            for (int i = 0; i < nplanes; i++) {
                act[i] = new double[size[i]];
            }
            for (int pl = 0; pl < nplanes - 1; pl++) {
                inf.rString();
                for (int i = 0; i < size[pl + 1]; i++) {
                    bias[pl][i] = inf.rdouble();
                }

            }
            for (int pl = 0; pl < nplanes - 1; pl++) {
                inf.rString();
                for (int i = 0; i < size[pl + 1]; i++) {
                    for (int j = 0; j < size[pl]; j++) {
                        wgt[pl][i][j] = inf.rdouble();
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }


    /**
     * Method activation The output signal is found by consulting
     * {@link #alphaTab}for the index associated with incoming signal x.
     *
     * @param x The signal incoming to the neuron for which the response is
     *          calculated.
     * @return The output signal.
     */
    private double activation(double x) {
        int index = (int) ((x - ALPHA_START) * recDeltaAlpha);
        if (index < 0) {
            index = 0;
        }
        if (index >= NUM_ALPHA) {
            index = NUM_ALPHA - 1;
        }

        return this.alphaTab[index];
    }

    /**
     * Method scp The scalar product of two vectors (same lengths) is
     * calculated.
     *
     * @param x The first vector.
     * @param y The second vector.
     * @return The scalar product of these two vector.
     */
    private static double scp(double[] x, double[] y) {
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }
        return sum;
    }


    /**
     * Method calcJacobi The NN is used. For a given input vector the
     * corresponding output vector together with the corresponding Jacobi matrix
     * is returned as an instance of class {@link NNCalc}.
     *
     * @param nnInp The vector contains the {@link #nn_in}input parameters (must
     *              be in right order).
     * @return The output and corresponding Jacobi matrix of the NN.
     */
    public NNCalc calcJacobi(double[] nnInp) {

        final NNCalc res = NNresjacob;

        for (int i = 0; i < nn_in; i++) {
            act[0][i] = (nnInp[i] - inmin[i]) / (inmax[i] - inmin[i]);
        }

        for (int pl = 0; pl < nplanes - 1; pl++) {
            final double[] act_pl_1 = act[pl + 1];
            final double[] help_pl = help[pl];
            final double[][] wgt_pl = wgt[pl];
            final double[] bias_pl = bias[pl];
            final double[] act_pl = act[pl];

            for (int i = 0; i < size[pl + 1]; i++) {
                act_pl_1[i] = activation(bias_pl[i] + scp(wgt_pl[i], act_pl));
                help_pl[i] = act_pl_1[i] * (1.0 - act_pl_1[i]);
            }

            final double[][] dActDX_pl = dActDX[pl];
            final double[][] dActDX_pl1 = dActDX[pl + 1];

            for (int i = 0; i < size[pl + 1]; i++) {
                for (int j = 0; j < nn_in; j++) {
                    double sum = 0.0;
                    final double help_pl_i = help_pl[i];
                    final double[] wgt_pl_i = wgt_pl[i];

                    for (int k = 0; k < size[pl]; k++) {
                        sum += help_pl_i * wgt_pl_i[k] * dActDX_pl[k][j];
                    }

                    dActDX_pl1[i][j] = sum;
                }
            }
        }


        final double[] act_nplanes_1 = act[nplanes - 1];
        final double[][] dActDX_nplanes_1 = dActDX[nplanes - 1];

        final double[][] jacobiMatrix = res.getJacobiMatrix();
        final double[] nnOutput = res.getNnOutput();
        for (int i = 0; i < nn_out; i++) {
            final double diff = outmax[i] - outmin[i];
            nnOutput[i] = act_nplanes_1[i] * diff + outmin[i];
            final double[] res_jacobiMatrix_i = jacobiMatrix[i];
            final double[] dActDX_nplanes_1_i = dActDX_nplanes_1[i];
            for (int k = 0; k < nn_in; k++) {
                res_jacobiMatrix_i[k] = dActDX_nplanes_1_i[k] * diff;
            }
        }
        return res;


    }

    private void declareArrays() {
        NNresjacob.setNnOutput(new double[nn_out]);
        NNresjacob.setJacobiMatrix(new double[nn_out][nn_in]);
        dActDX = new double[nplanes][][];
        dActDX[0] = new double[nn_in][nn_in];
        for (int i = 0; i < nn_in; i++) {
            for (int j = 0; j < nn_in; j++) {
                dActDX[0][i][j] = 0.0;
            }
            dActDX[0][i][i] = 1.0 / (inmax[i] - inmin[i]);
        }
        help = new double[nplanes - 1][];
        for (int pl = 0; pl < nplanes - 1; pl++) {
            help[pl] = new double[size[pl + 1]];
            dActDX[pl + 1] = new double[size[pl + 1]][nn_in];
        }
    }

    /**
     * Method calc The NN is used. For a given input vector the corresponding
     * output vector is returned.
     *
     * @param nninp The vector contains the {@link #nn_in}input parameters (must
     *              be in right order).
     * @return The {@link #nn_out}-long output vector.
     */
    public double[] calc(double[] nninp) {
        double[] res = new double[nn_out];

        for (int i = 0; i < nn_in; i++) {
            act[0][i] = (nninp[i] - inmin[i]) / (inmax[i] - inmin[i]);
        }
        for (int pl = 0; pl < nplanes - 1; pl++) {
            final double[] bias_pl = bias[pl];
            final double[][] wgt_pl = wgt[pl];
            final double[] act_pl = act[pl];
            final double[] act_pl1 = act[pl + 1];
            final int size_pl1 = size[pl + 1];
            for (int i = 0; i < size_pl1; i++) {
                act_pl1[i] = activation(bias_pl[i] + scp(wgt_pl[i], act_pl));
            }
        }
        final double[] act_nnplanes1 = act[nplanes - 1];
        for (int i = 0; i < nn_out; i++) {
            res[i] = act_nnplanes1[i] * (outmax[i] - outmin[i]) + outmin[i];
        }
        return res;
    }


}

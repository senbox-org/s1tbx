package org.esa.snap.core.nn;

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

/**
 * Handles formatted input in a FORTRAN like manner: if only a part of a line is read, the rest of the line is lost.<p>
 * There might be comment-lines, which will be skipped.
 * Data items can be separated by delimiters, which could change within the file.
 *
 * @author H. Schiller / GKSS
 */
class FormattedStringReader {

    private StringReader inp;
    private String delimiters;
    private boolean file_has_comments;
    private boolean echo_comments;
    private String comment_begin;

    public FormattedStringReader(StringReader inp) {
        this(inp, " \t\n\r,;:");
    }

    public FormattedStringReader(StringReader inp, String delimiters) {
        this.inp = inp;
        this.delimiters = delimiters;
        this.file_has_comments = true;
        this.echo_comments = false;
        this.comment_begin = "#";
    }

    public void commentStart(String comment_begin) {
        this.comment_begin = comment_begin;
        this.file_has_comments = true;
    }

    public void noComments() {
        this.file_has_comments = false;
    }

    public void setEcho(boolean echo_comments) {
        this.echo_comments = echo_comments;
    }

    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }

    private static String readLine(StringReader sr) throws IOException {
        String res = "";
        char[] helper = new char[1];
        int readCount = 0;
        while (helper[0] != '\n') {
            readCount = sr.read(helper, 0, 1);
            if (readCount == -1) {
                break;
            }
            res = res.concat(String.copyValueOf(helper));
        }
        return res.trim();
    }

    public long rlong() throws IOException {
        boolean ready = false;
        long res = 0;
        while (!ready) {
            String eing = readLine(inp);
            if (this.file_has_comments) {
                if (eing.startsWith(this.comment_begin)) {
                    if (this.echo_comments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            StringTokenizer st = new StringTokenizer(eing, this.delimiters);
            res = Long.parseLong(st.nextToken());
            ready = true;
        }
        return res;
    }

    public double rdouble() throws IOException {
        boolean ready = false;
        double res = 0;
        while (!ready) {
            String eing = readLine(inp);
            if (this.file_has_comments) {
                if (eing.startsWith(this.comment_begin)) {
                    if (this.echo_comments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            StringTokenizer st = new StringTokenizer(eing, this.delimiters);
            res = Double.valueOf(st.nextToken());
            ready = true;
        }
        return res;
    }

    public String rString() throws IOException {
        String eing = null;
        boolean ready = false;
        while (!ready) {
            eing = readLine(inp);
            if (this.file_has_comments) {
                if (eing.startsWith(this.comment_begin)) {
                    if (this.echo_comments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            ready = true;
        }
        return eing;
    }

    public long[] rlong(int how_many) throws IOException {
        boolean ready = false;
        long[] res = new long[how_many];
        int got = 0;
        while (!ready) {
            String eing = readLine(inp);
            if (this.file_has_comments) {
                if (eing.startsWith(this.comment_begin)) {
                    if (this.echo_comments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            StringTokenizer st = new StringTokenizer(eing, this.delimiters);
            int nn = st.countTokens();
            for (int i = 0; i < nn; i++) {
                res[got] = Long.parseLong(st.nextToken());
                got++;
                if (got == how_many) {
                    ready = true;
                    break;
                }
            }
        }
        if (got == how_many) {
            return res;
        } else {
            long[] less = new long[got];
            System.arraycopy(res, 0, less, 0, got);
            return less;
        }
    }

    public double[] rdouble(int how_many) throws IOException {
        boolean ready = false;
        double[] res = new double[how_many];
        int got = 0;
        while (!ready) {
            String eing = readLine(inp);

            if (this.file_has_comments) {
                if (eing.startsWith(this.comment_begin)) {
                    if (this.echo_comments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            StringTokenizer st = new StringTokenizer(eing, this.delimiters);
            int nn = st.countTokens();
            if (nn == 0) {
                break;
            }
            for (int i = 0; i < nn; i++) {
                res[got] = Double.valueOf(st.nextToken());
                got++;
                if (got == how_many) {
                    ready = true;
                    break;
                }
            }
        }
        if (got == how_many) {
            return res;
        } else {
            double[] less = new double[got];
            System.arraycopy(res, 0, less, 0, got);
            return less;
        }
    }

    public double[][] rdoubleAll(int dimension) throws IOException {
        double[][] points;
        int npoints = 0;
        this.inp.mark(100000000);
        double[] p;
        while (true) {
            p = this.rdouble(dimension);
            if (p.length < dimension) {
                break;
            }
            npoints++;
        }
        this.inp.reset();
        points = new double[npoints][];
        for (int i = 0; i < npoints; i++) {
            points[i] = this.rdouble(dimension);
        }
        return points;
    }

    public double[][] rdoubleAll() throws IOException {
        this.inp.mark(100000000);
        String eing = this.rString();
        this.inp.reset();
        StringTokenizer st = new StringTokenizer(eing, this.delimiters);
        return this.rdoubleAll(st.countTokens());
    }
}

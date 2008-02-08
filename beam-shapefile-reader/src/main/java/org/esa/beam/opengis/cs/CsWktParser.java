package org.esa.beam.opengis.cs;

import org.esa.beam.opengis.ct.PassthroughMathTransform;
import org.esa.beam.opengis.ct.ParamMathTransform;
import org.esa.beam.opengis.ct.Parameter;
import org.esa.beam.opengis.ct.MathTransform;
import org.esa.beam.opengis.ct.InverseMathTransform;
import org.esa.beam.opengis.ct.ConcatMathTransform;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Stack;

public class CsWktParser {
    private StreamTokenizer st;
    private ElementNode currentNode;
    private int currentIndex;
    private Stack<ElementNode> nodeStack;
    private Stack<Integer> indexStack;

    public static CoordinateSystem parseCs(String wkt) throws ParseException {
        try {
            return parseCs(new StringReader(wkt));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CoordinateSystem parseCs(Reader r) throws IOException, ParseException {
        CsWktParser csWktParser = new CsWktParser(r);
        return csWktParser.nextCs();
    }

    public static MathTransform parseMt(String wkt) throws ParseException {
        try {
            return parseMt(new StringReader(wkt));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MathTransform parseMt(Reader r) throws IOException, ParseException {
        return new CsWktParser(r).nextMt();
    }

    /**
     * <coordinate system> = <horz cs> | <geocentric cs> | <vert cs> | <compd cs> | <fitted cs> | <local cs>
     * <horz cs> = <geographic cs> | <projected cs>
     * <projected cs> = PROJCS["<name>", <geographic cs>, <projection>, {<parameter>,}* <linear unit> {,<twin axes>}{,<authority>}]
     * <projection> = PROJECTION["<name>" {,<authority>}]
     * <geographic cs> = GEOGCS["<name>", <datum>, <prime meridian>, <angular unit> {,<twin axes>} {,<authority>}]
     * <datum> = DATUM["<name>", <spheroid> {,<to wgs84>} {,<authority>}]
     * <spheroid> = SPHEROID["<name>", <semi-major axis>, <inverse flattening> {,<authority>}]
     * <semi-major axis> = <number>
     * <inverse flattening> = <number>
     * <prime meridian> = PRIMEM["<name>", <longitude> {,<authority>}]
     * <longitude> = <number>
     * <angular unit> = <unit>
     * <linear unit> = <unit>
     * <unit> = UNIT["<name>", <conversion factor> {,<authority>}]
     * <conversion factor> = <number>
     * <geocentric cs> = GEOCCS["<name>", <datum>, <prime meridian>, <linear unit> {,<axis>, <axis>, <axis>} {,<authority>}]
     * <authority> = AUTHORITY["<name>", "<code>"]
     * <vert cs> = VERT_CS["<name>", <vert datum>, <linear unit>, {<axis>,} {,<authority>}]
     * <vert datum> = VERT_DATUM["<name>", <datum type> {,<authority>}]
     * <datum type> = <number>
     * <compd cs> = COMPD_CS["<name>", <head cs>, <tail cs> {,<authority>}]
     * <head cs> = <coordinate system>
     * <tail cs> = <coordinate system>
     * <twin axes> = <axis>, <axis>
     * <axis> = AXIS["<name>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER]
     * <to wgs84s> = TOWGS84[<seven param>]
     * <seven param> = <dx>, <dy>, <dz>, <ex>, <ey>, <ez>, <ppm>
     * <dx> = <number>
     * <dy> = <number>
     * <dz> = <number>
     * <ex> = <number>
     * <ey> = <number>
     * <ez> = <number>
     * <ppm> = <number>
     * <fitted cs> = FITTED_CS["<name>", <to base>, <base cs>]
     * <to base> = <math transform>
     * <base cs> = <coordinate system>
     * <local cs> = LOCAL_CS["<name>", <local datum>, <unit>, <axis>, {,<axis>}* {,<authority>}]
     * <local datum> = LOCAL_DATUM["<name>", <datum type> {,<authority>}]
     */
    private CoordinateSystem nextCs() throws ParseException {
        CoordinateSystem cs;
        cs = nextGeogCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextProjCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextGeocCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextVertCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextCompdCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextFittedCs(false);
        if (cs != null) {
            return cs;
        }
        cs = nextLocalCs(false);
        if (cs != null) {
            return cs;
        }
        throw new ParseException("CS expected.", 0);
    }

    // <local cs> = LOCAL_CS["<name>", <local datum>, <unit>, <axis>, {,<axis>}* {,<authority>}]
    private LocalCoordinateSystem nextLocalCs(boolean required) throws ParseException {
        if (startElement("LOCAL_CS", required)) {
            String name = nextString();
            LocalDatum localDatum = nextLocalDatum();
            Unit unit = nextUnit();
            ArrayList<AxisInfo> axes = new ArrayList<AxisInfo>(3);
            axes.add(nextAxis(false));
            while (true) {
                AxisInfo axisInfo = nextAxis(false);
                if (axisInfo != null) {
                    axes.add(nextAxis(false));
                } else {
                    break;
                }
            }
            Authority authority = nextAuthority(false);
            endElement();
            return new LocalCoordinateSystem(name, localDatum, unit, axes.toArray(new AxisInfo[0]), authority);
        }
        return null;
    }

    // <local datum> = LOCAL_DATUM["<name>", <datum type> {,<authority>}]
    private LocalDatum nextLocalDatum() throws ParseException {
        if (startElement("LOCAL_DATUM", true)) {
            String name = nextString();
            int datumType = nextInteger();
            Authority authority = nextAuthority(false);
            endElement();
            return new LocalDatum(name, datumType, authority);
        }
        return null;
    }


    // <fitted cs> = FITTED_CS["<name>", <to base>, <base cs>]
    private FittedCoordinateSystem nextFittedCs(boolean required) throws ParseException {
        if (startElement("FITTED_CS", required)) {
            String name = nextString();
            MathTransform toBase = nextMt();
            CoordinateSystem baseCs = nextCs();
            endElement();
            return new FittedCoordinateSystem(name, toBase, baseCs);
        }
        return null;
    }

    // <compd cs> = COMPD_CS["<name>", <head cs>, <tail cs> {,<authority>}]
    private CompoundCoordinateSystem nextCompdCs(boolean required) throws ParseException {
        if (startElement("COMPD_CS", required)) {
            String name = nextString();
            CoordinateSystem headCs = nextCs();
            CoordinateSystem tailCs = nextCs();
            Authority authority = nextAuthority(false);
            endElement();
            return new CompoundCoordinateSystem(name, headCs, tailCs, authority);
        }
        return null;
    }

    // <vert cs> = VERT_CS["<name>", <vert datum>, <linear unit>, {<axis>,} {,<authority>}]
    private VerticalCoordinateSystem nextVertCs(boolean required) throws ParseException {
        if (startElement("VERT_CS", required)) {
            String name = nextString();
            VerticalDatum verticalDatum = nextVertDatum();
            Unit linearUnit = nextUnit();
            AxisInfo axisInfo = nextAxis(false);
            Authority authority = nextAuthority(false);
            endElement();
            return new VerticalCoordinateSystem(name, verticalDatum, linearUnit, axisInfo, authority);
        }
        return null;
    }

    // <vert datum> = VERT_DATUM["<name>", <datum type> {,<authority>}]
    private VerticalDatum nextVertDatum() throws ParseException {
        if (startElement("VERT_DATUM", true)) {
            String name = nextString();
            int datumType = nextInteger();
            Authority authority = nextAuthority(false);
            endElement();
            return new VerticalDatum(name, datumType, authority);
        }
        return null;
    }

    // <geocentric cs> = GEOCCS["<name>", <datum>, <prime meridian>, <linear unit> {,<axis>, <axis>, <axis>} {,<authority>}]
    private GeocentricCoordinateSystem nextGeocCs(boolean required) throws ParseException {
        if (startElement("GEOCCS", required)) {
            String name = nextString();
            HorizontalDatum datum = nextDatum();
            PrimeMeridian primem = nextPrimem();
            Unit linearUnit = nextUnit();
            AxisInfo axis1 = nextAxis(false);
            AxisInfo axis2 = null;
            AxisInfo axis3 = null;
            if (axis1 != null) {
                axis2 = nextAxis(true);
                axis3 = nextAxis(true);
            } else {
                axis1 = new AxisInfo("X", AxisInfo.Orientation.OTHER);
                axis2 = new AxisInfo("Y", AxisInfo.Orientation.EAST);
                axis3 = new AxisInfo("Z", AxisInfo.Orientation.NORTH);
            }
            Authority authority = nextAuthority(false);
            endElement();
            return new GeocentricCoordinateSystem(name, datum, primem, linearUnit, axis1, axis2, axis3, authority);
        }
        return null;
    }

    // <geographic cs> = GEOGCS["<name>", <datum>, <prime meridian>, <angular unit> {,<twin axes>} {,<authority>}]
    private GeographicCoordinateSystem nextGeogCs(boolean required) throws ParseException {
        if (startElement("GEOGCS", required)) {
            String name = nextString();
            HorizontalDatum datum = nextDatum();
            PrimeMeridian primem = nextPrimem();
            Unit angularUnit = nextUnit();
            AxisInfo axis1 = nextAxis(false);
            AxisInfo axis2 = null;
            if (axis1 != null) {
                axis2 = nextAxis(true);
            } else {
                axis1 = new AxisInfo("Lon", AxisInfo.Orientation.EAST);
                axis2 = new AxisInfo("Lat", AxisInfo.Orientation.NORTH);
            }
            Authority authority = nextAuthority(false);
            endElement();
            return new GeographicCoordinateSystem(name, datum, primem, angularUnit, axis1, axis2, authority);
        }
        return null;
    }

    // <projected cs> = PROJCS["<name>", <geographic cs>, <projection>, {<parameter>,}* <linear unit> {,<twin axes>}{,<authority>}]
    private ProjectedCoordinateSystem nextProjCs(boolean required) throws ParseException {
        if (startElement("PROJCS", required)) {
            String name = nextString();
            GeographicCoordinateSystem geogCs = nextGeogCs(true);
            Projection projection = nextProjection();
            ArrayList<Parameter> parameters = new ArrayList<Parameter>(10);
            while (true) {
                Parameter parameter = nextParameter(false);
                if (parameter != null) {
                    parameters.add(parameter);
                } else {
                    break;
                }
            }
            projection.parameters = parameters.toArray(new Parameter[0]);
            Unit linearUnit = nextUnit();
            AxisInfo axis1 = nextAxis(false);
            AxisInfo axis2 = null;
            if (axis1 != null) {
                axis2 = nextAxis(true);
            } else {
                axis1 = new AxisInfo("X", AxisInfo.Orientation.EAST);
                axis2 = new AxisInfo("Y", AxisInfo.Orientation.NORTH);
            }
            Authority authority = nextAuthority(false);
            endElement();
            return new ProjectedCoordinateSystem(name, geogCs,
                                                 projection,
                                                 linearUnit,
                                                 axis1,
                                                 axis2,
                                                 authority);
        }
        return null;
    }


    // <projection> = PROJECTION["<name>" {,<authority>}]
    private Projection nextProjection() throws ParseException {
        if (startElement("PROJECTION", true)) {
            String name = nextString();
            Authority authority = nextAuthority(false);
            endElement();
            return new Projection(name, authority);
        }
        return null;
    }

    // <axis> = AXIS["<name>", NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER]
    private AxisInfo nextAxis(boolean required) throws ParseException {
        if (startElement("AXIS", required)) {
            String name = nextString();
            String keyword = nextKeyword();
            AxisInfo.Orientation orientation = AxisInfo.Orientation.valueOf(keyword);
            if (orientation == null) {
                throw new ParseException("Illegal orientation.", 0);
            }
            endElement();
            return new AxisInfo(name, orientation);
        }
        return null;
    }

    private Unit nextUnit() throws ParseException {
        if (startElement("UNIT", true)) {
            String name = nextString();
            double conversionFactor = nextDouble();
            Authority authority = nextAuthority(false);
            endElement();
            return new Unit(name, conversionFactor, authority);
        }
        return null;
    }

    // <prime meridian> = PRIMEM["<name>", <longitude> {,<authority>}]
    private PrimeMeridian nextPrimem() throws ParseException {
        if (startElement("PRIMEM", true)) {
            String name = nextString();
            double longitude = nextDouble();
            Authority authority = nextAuthority(false);
            endElement();
            return new PrimeMeridian(name, longitude, authority);
        }
        return null;
    }

    // <datum> = DATUM["<name>", <spheroid> {,<to wgs84>} {,<authority>}]
    private HorizontalDatum nextDatum() throws ParseException {
        if (startElement("DATUM", true)) {
            String name = nextString();
            Ellipsoid ellipsoid = nextSpheroid();
            WGS84ConversionInfo toWgs84 = nextToWgs84(false);
            if (toWgs84 == null) {
                toWgs84 = new WGS84ConversionInfo();
            }
            Authority authority = nextAuthority(false);
            endElement();
            return new HorizontalDatum(name, ellipsoid, toWgs84, authority);
        }
        return null;
    }

    private WGS84ConversionInfo nextToWgs84(boolean required) throws ParseException {
        if (startElement("TOWGS84", required)) {
            double dx = nextDouble();
            double dy = nextDouble();
            double dz = nextDouble();
            double ex = nextDouble();
            double ey = nextDouble();
            double ez = nextDouble();
            double ppm = nextDouble();
            endElement();
            return new WGS84ConversionInfo(dx, dy, dz, ex, ey, ez, ppm);
        }
        return null;
    }

    // <spheroid> = SPHEROID["<name>", <semi-major axis>, <inverse flattening> {,<authority>}]
    private Ellipsoid nextSpheroid() throws ParseException {
        if (startElement("SPHEROID", true)) {
            String name = nextString();
            double semiMajorAxis = nextDouble();
            double inverseFlattening = nextDouble();
            Authority authority = nextAuthority(false);
            endElement();
            return new Ellipsoid(name, semiMajorAxis, inverseFlattening, authority);
        }
        return null;
    }

    // <authority> = AUTHORITY["<name>", "<code>"]
    private Authority nextAuthority(boolean required) throws ParseException {
        if (startElement("AUTHORITY", required)) {
            String name = nextString();
            String code = nextString();
            endElement();
            return new Authority(name, code);
        }
        return null;
    }


    /**
     * <math transform> = <param mt> | <concat mt> | <inv mt> | <passthrough mt>
     * <param mt> = PARAM_MT["<classification name>" {,<parameter>}* ]
     * <parameter> = PARAMETER["<name>", <value>]
     * <value> = <number>
     * <concat mt> = CONCAT_MT[<math transform> {,<math transform>}* ]
     * <inv mt> = INVERSE_MT[<math transform>]
     * <passthrough mt> = PASSTHROUGH_MT[<integer>, <math transform>]
     */
    private MathTransform nextMt() throws ParseException {
        if (startElement("PARAM_MT", false)) {
            String name = nextString();
            ArrayList<Parameter> parameters = new ArrayList<Parameter>(10);
            while (hasNext()) {
                parameters.add(nextParameter(false));
            }
            endElement();
            return new ParamMathTransform(name, parameters.toArray(new Parameter[0]));
        } else if (startElement("CONCAT_MT", false)) {
            MathTransform mt1 = nextMt();
            ArrayList<MathTransform> mts = new ArrayList<MathTransform>(10);
            mts.add(mt1);
            while (hasNext()) {
                mts.add(nextMt());
            }
            endElement();
            return new ConcatMathTransform(mts.toArray(new MathTransform[0]));
        } else if (startElement("INVERSE_MT", false)) {
            MathTransform mt = nextMt();
            endElement();
            return new InverseMathTransform(mt);
        } else if (startElement("PASSTHROUGH_MT", false)) {
            int value = nextInteger();
            MathTransform mt = nextMt();
            endElement();
            return new PassthroughMathTransform(value, mt);
        }
        throw new ParseException("PARAM_MT, CONCAT_MT, INVERSE_MT or PASSTHROUGH_MT expected.", 0);
    }

    // <parameter> = PARAMETER["<name>", <value>]
    private Parameter nextParameter(boolean required) throws ParseException {
        if (startElement("PARAMETER", required)) {
            String name = nextString();
            double value = nextDouble();
            endElement();
            return new Parameter(name, value);
        }
        return null;
    }

    private int nextInteger() throws ParseException {
        Node node = peekNode();
        if (node instanceof NumberNode) {
            nextNode();
            return (int) ((NumberNode) node).value;
        }
        throw new ParseException("Integer value expected", 0);
    }

    private double nextDouble() throws ParseException {
        Node node = peekNode();
        if (node instanceof NumberNode) {
            nextNode();
            return ((NumberNode) node).value;
        }
        throw new ParseException("Numeric value expected", 0);
    }

    private Node peekNode() {
        if (hasNext()) {
            return currentNode.nodes.get(currentIndex);
        }
        return null;
    }

    private boolean hasNext() {
        return currentIndex < currentNode.nodes.size();
    }

    private void nextNode() {
        currentIndex++;
    }

    private String nextString() throws ParseException {
        Node node = peekNode();
        if (node instanceof StringNode) {
            nextNode();
            return ((StringNode) node).value;
        }
        throw new ParseException("String value expected", 0);
    }

    private String nextKeyword() throws ParseException {
        Node node = peekNode();
        if (node instanceof KeywordNode) {
            nextNode();
            return ((KeywordNode) node).name;
        }
        throw new ParseException("Keyword expected", 0);
    }

    private boolean startElement(String name, boolean required) throws ParseException {
        Node node = peekNode();
        if (node instanceof ElementNode) {
            ElementNode elementNode = (ElementNode) node;
            if (name.equalsIgnoreCase(elementNode.name)) {
                nodeStack.push(currentNode);
                indexStack.push(currentIndex);
                currentNode = elementNode;
                currentIndex = 0;
                return true;
            }
        }
        if (required) {
            throw new ParseException(name + " expected.", 0);
        }
        return false;
    }

    private void endElement() {
        currentNode = nodeStack.pop();
        currentIndex = indexStack.pop();
        currentIndex++;
    }

    private CsWktParser(Reader r) throws IOException, ParseException {
        st = new StreamTokenizer(r);
        st.resetSyntax();
        st.whitespaceChars(0, 32);
        st.quoteChar('"');
        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars('0', '9');
        st.wordChars('_', '_');
        st.slashSlashComments(true);
        st.parseNumbers();
        Node node = parseNodeTree();
        currentNode = new ElementNode("TREE");
        currentNode.addNode(node);
        currentIndex = 0;
        nodeStack = new Stack<ElementNode>();
        indexStack = new Stack<Integer>();
    }

    private static abstract class Node {
    }

    private static class KeywordNode extends Node {
        String name;

        public KeywordNode(String key) {
            this.name = key;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class NumberNode extends Node {
        double value;

        public NumberNode(double value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private static class StringNode extends Node {
        String value;

        public StringNode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }
    }

    private static class ElementNode extends Node {
        String name;
        ArrayList<Node> nodes;

        public ElementNode(String name) {
            this.name = name;
            this.nodes = new ArrayList<Node>(8);
        }

        public void addNode(Node node) {
            nodes.add(node);
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(256);
            s.append(name);
            s.append('[');
            Node[] nodes = this.nodes.toArray(new Node[0]);
            s.append(nodes[0].toString());
            for (int i = 1; i < nodes.length; i++) {
                s.append(',');
                s.append(nodes[i].toString());
            }
            s.append(']');
            return s.toString();
        }
    }

    private Node parseNodeTree() throws IOException, ParseException {
        int tt = st.nextToken();
        if (tt == StreamTokenizer.TT_WORD) {
            String name = st.sval;
            tt = st.nextToken();
            if (tt == (int) '[') {
                ElementNode eNode = new ElementNode(name);
                eNode.addNode(parseNodeTree());
                while (true) {
                    tt = st.nextToken();
                    if (tt == (int) ']') {
                        break;
                    } else if (tt != (int) ',') {
                        throw new ParseException("Missing ',' or ']'.", 0);
                    }
                    eNode.addNode(parseNodeTree());
                }
                return eNode;
            } else {
                st.pushBack();
                return new KeywordNode(name);
            }
        } else if (tt == StreamTokenizer.TT_NUMBER) {
            return new NumberNode(st.nval);
        } else if (tt == (int) '"') {
            return new StringNode(st.sval);
        } else if (tt == StreamTokenizer.TT_EOF) {
            throw new ParseException("EOF", 0);
        } else {
            throw new ParseException("Illegal character '" + (char) tt + "'", 0);
        }
    }
}


package org.esa.s1tbx.fex.gpf.decisiontree;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

/**
 * a tree node
 */
public class DecisionTreeNode {

    private String id;
    private String expression = "";
    private String trueNodeID = null;
    private String falseNodeID = null;
    private DecisionTreeNode parentNode = null;
    private DecisionTreeNode trueNode = null;
    private DecisionTreeNode falseNode = null;

    private int x, y, w, h;
    private int depth = 0, breadth = 0, expLength = 0;
    private DecisionTreeNode[] array = null;
    private boolean positionValid = false;

    public DecisionTreeNode() {
    }

    public void addBranch(final DecisionTreeNode trueNode, final DecisionTreeNode falseNode) {
        this.trueNode = trueNode;
        this.falseNode = falseNode;
        trueNode.parentNode = this;
        falseNode.parentNode = this;
        invalidatePosition();
        if (parentNode != null) {
            // parentNode.invalidatePosition();
        }
    }

    public void update() {
        array = null;
        array = toArray();
    }

    public DecisionTreeNode getTrueNode() {
        return trueNode;
    }

    public DecisionTreeNode getFalseNode() {
        return falseNode;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
        invalidatePosition();
    }

    public boolean isConnected() {
        return !(trueNode == null && trueNodeID != null);
    }

    public boolean isLeaf() {
        return trueNode == null;
    }

    public boolean isTwig() {
        return trueNode != null && trueNode.isLeaf();
    }

    public int getDepth() {
        return depth;
    }

    public int getBreadth() {
        return breadth;
    }

    public int getExpressionLength() {
        return expLength;
    }

    public void deleteBranch() {
        this.trueNode = null;
        this.falseNode = null;
        update();
    }

    public void setPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
        positionValid = true;
    }

    public void setDimension(final int w, final int h) {
        this.w = w;
        this.h = h;
    }

    public void invalidatePosition() {
        positionValid = false;
        if (trueNode != null)
            trueNode.invalidatePosition();
        if (falseNode != null)
            falseNode.invalidatePosition();
    }

    public void moveTree(final int dx, final int dy) {
        setPosition(this.x + dx, this.y + dy);
        if (trueNode != null)
            trueNode.moveTree(dx, dy);
        if (falseNode != null)
            falseNode.moveTree(dx, dy);
    }

    public boolean isPositionSet() {
        return positionValid;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public boolean isWithin(final Point p) {
        return p.x > x && p.y > y && p.x < x + w && p.y < y + h;
    }

    public DecisionTreeNode[] toArray() {
        if (array != null)
            return array;

        final ArrayList<DecisionTreeNode> list = new ArrayList<>(20);
        breadth = -1;
        depth = -1;
        expLength = 0;
        addNode(this, list, 0, 0, 0);
        array = list.toArray(new DecisionTreeNode[list.size()]);
        return array;
    }

    private int addNode(final DecisionTreeNode n, final ArrayList<DecisionTreeNode> list,
                        final int nodeDepth, int nodeCount, final int nodeExpLength) {
        if (n == null) return nodeCount;
        list.add(n);
        n.id = "node" + nodeCount;
        ++nodeCount;

        if (n.isLeaf()) {
            breadth += 1;
        } else {
            nodeCount = addNode(n.getTrueNode(), list, nodeDepth + 1, nodeCount, nodeExpLength + n.expression.length());
            nodeCount = addNode(n.getFalseNode(), list, nodeDepth + 1, nodeCount, nodeExpLength + n.expression.length());
        }
        if (depth < nodeDepth)
            depth = nodeDepth;
        if (expLength < nodeExpLength)
            expLength = nodeExpLength;

        return nodeCount;
    }

    public String toString() {
        final String trueNodeID = trueNode != null ? trueNode.id : "null";
        final String falseNodeID = falseNode != null ? falseNode.id : "null";

        return "id=" + id +
                " expression=" + expression +
                " trueNode=" + trueNodeID +
                " falseNode=" + falseNodeID;
    }

    public static DecisionTreeNode parse(String str) throws IOException {
        str = str.replace(" expression", "\nexpression");
        str = str.replace(" trueNode", "\ntrueNode");
        str = str.replace(" falseNode", "\nfalseNode");

        final Properties p = new Properties();
        p.load(new StringReader(str));

        final DecisionTreeNode node = new DecisionTreeNode();
        node.id = p.getProperty("id");
        node.expression = p.getProperty("expression");
        node.trueNodeID = p.getProperty("trueNode");
        node.falseNodeID = p.getProperty("falseNode");

        return node;
    }

    public static DecisionTreeNode createDefaultTree() {
        final DecisionTreeNode root = new DecisionTreeNode();
        root.addBranch(new DecisionTreeNode(), new DecisionTreeNode());
        return root;
    }

    public static void connectNodes(final Object[] array) {
        for (Object o : array) {
            final DecisionTreeNode n = (DecisionTreeNode) o;

            if (n.trueNode == null) {
                n.trueNode = findNode(array, n.trueNodeID);
                n.trueNodeID = null;
            }
            if (n.falseNode == null) {
                n.falseNode = findNode(array, n.falseNodeID);
                n.falseNodeID = null;
            }
        }
    }

    public static DecisionTreeNode findNode(final Object[] array, final String id) {
        for (Object o : array) {
            final DecisionTreeNode n = (DecisionTreeNode) o;
            if (n.id.equals(id))
                return n;
        }
        return null;
    }
}

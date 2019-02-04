
package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import org.esa.s1tbx.fex.gpf.decisiontree.DecisionTreeNode;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Draws the decision tree
 */
class DecisionTreePanel extends JPanel implements ActionListener, PopupMenuListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private Point lastMousePos = null;

    private static final Font font = new Font("Ariel", Font.BOLD, 10);
    private static final Color nodeColor = new Color(226, 239, 255, 255);
    private static final Color leafColor = new Color(221, 250, 255, 255);
    private static final Color shadowColor = new Color(0, 0, 0, 128);
    private static final Color selColor = new Color(200, 255, 200, 150);

    private final EditNodePanel editPanel;
    private DecisionTreeNode selectedNode = null;
    private boolean showHelp = false;
    private double scale = 1.0;

    private enum HALF {TOP, BOTTOM}

    private final AffineTransform transform = new AffineTransform();

    private DecisionTreeNode treeRoot = null;

    DecisionTreePanel(final EditNodePanel editPanel) {
        this.editPanel = editPanel;

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void createNewTree(final Object[] decisionTreeArray) {
        if (decisionTreeArray == null || decisionTreeArray.length == 0) {
            this.treeRoot = DecisionTreeNode.createDefaultTree();
            showHelp(true);
        } else {
            DecisionTreeNode.connectNodes(decisionTreeArray);
            this.treeRoot = (DecisionTreeNode) decisionTreeArray[0];
        }
        repaint();
    }

    public DecisionTreeNode[] getTreeAsArray() {
        return treeRoot.toArray();
    }

    public void showHelp(final boolean flag) {
        showHelp = flag;
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (treeRoot == null)
            return;

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        //transform.setToIdentity();
        //transform.scale(scale, scale);
        // ((Graphics2D) g).setTransform(transform);

        DrawGraph(g);
    }

    /**
     * Draw the graphical representation of the Graph
     *
     * @param g the Graphics
     */
    private void DrawGraph(final Graphics g) {

        g.setFont(font);
        if (showHelp) {
            drawHelp(g);
        }

        Rectangle rect = g.getClipBounds();
        int depth = Math.max(2, treeRoot.getDepth());
        int breadth = Math.max(5, treeRoot.getBreadth());
        int expLength = treeRoot.getExpressionLength();
        float incX = Math.max(20, 100 / depth);
        float incY = 60;//120;
        float level = 1;

        int x = 5;
        int y = rect.height / 2;

        drawNode(g, treeRoot, x, y);

        drawBranch(g, treeRoot, x, y, incX, incY, HALF.TOP, level, treeRoot.getTrueNode(), -1);
        drawBranch(g, treeRoot, x, y, incX, incY, HALF.BOTTOM, level, treeRoot.getFalseNode(), 1);
    }

    private static void drawTree(final Graphics g, final DecisionTreeNode n,
                                 final int x, final int y, final float incX, final float incY,
                                 final HALF half, final float level) {
        drawNode(g, n, x, y);

        drawBranch(g, n, n.getX(), n.getY(), incX, incY, half, level, n.getTrueNode(), -1);
        drawBranch(g, n, n.getX(), n.getY(), incX, incY, half, level, n.getFalseNode(), 1);
    }

    private static void drawBranch(final Graphics g, final DecisionTreeNode n,
                                   final int x, final int y, final float incX, final float incY,
                                   final HALF half, final float level,
                                   final DecisionTreeNode node, final int sign) {
        if (node == null) return;

        int yInc;
        if (node.isLeaf()) {
            yInc = 18;//11;
        } else {
            yInc = 40;
        }
            /*
        } else if (node.isTwig()) {
            yInc = 36;//12;
        } else {
            yInc = 70;//Math.max(50, (int) (incY / level));
        }     */

        int deltaY = yInc;
        if (sign < 0) {
            if (half == HALF.BOTTOM)
                deltaY /= 4;
            drawTree(g, node, x + n.getWidth() + (int) incX, y - deltaY, incX, incY, half, level + 1);
        } else {
            if (half == HALF.TOP)
                deltaY /= 4;
            drawTree(g, node, x + n.getWidth() + (int) incX, y + deltaY, incX, incY, half, level + 1);
        }

        // connect after position has been set
        g.drawLine(n.getX() + n.getWidth(), n.getY() + n.getHeight() / 2, node.getX(), node.getY() + node.getHeight() / 2);
    }

    private static void drawNode(final Graphics g, final DecisionTreeNode n, final int posX, final int posY) {

        String expression = n.getExpression();
        if (expression == null || expression.isEmpty())
            expression = "  ";
        final Rectangle2D rect = g.getFontMetrics().getStringBounds(expression, g);
        final int w = (int) rect.getWidth() + 10;
        final int h = 20;
        n.setDimension(w, h);

        if (!n.isPositionSet()) {
            n.setPosition(posX, posY);
        }

        int x = n.getX();
        int y = n.getY();
        if (n.getTrueNode() == null) {
            g.setColor(Color.black);
            g.fillRect(x + 1, y + 1, w, h);
            g.setColor(shadowColor);
            g.fillRect(x + 2, y + 2, w, h);
            g.setColor(leafColor);
            g.fillRect(x, y, w, h);
            g.setColor(Color.lightGray);
            g.drawRect(x, y, w, h);
        } else {
            g.setColor(Color.black);
            g.fillRoundRect(x + 1, y + 1, w, h, 20, 20);
            g.setColor(shadowColor);
            g.fillRoundRect(x + 2, y + 2, w, h, 20, 20);
            g.setColor(nodeColor);
            g.fillRoundRect(x, y, w, h, 20, 20);
            g.setColor(Color.blue);
            g.drawRoundRect(x, y, w, h, 20, 20);
        }

        g.setColor(Color.black);
        g.drawString(expression, x + w / 2 - ((int) rect.getWidth() / 2), y + h / 2 + 3);
    }

    private static void drawHelp(final Graphics g) {
        final int x = (int) (g.getClipBounds().getWidth() / 2);
        final int y = 20;

        final String str = "Click to edit a node";
        final Rectangle2D rect = g.getFontMetrics().getStringBounds(str, g);
        final int stringWidth = (int) rect.getWidth();

        g.setColor(Color.black);
        g.drawString(str, x - stringWidth / 2, y);
    }

    /**
     * Handles menu item pressed events
     *
     * @param event the action event
     */
    public void actionPerformed(ActionEvent event) {

        final String name = event.getActionCommand();
        if (name.equals("Delete")) {
            selectedNode.deleteBranch();
            treeRoot.update();
            repaint();
        } else if (name.equals("Add Decision Node")) {
            selectedNode.addBranch(new DecisionTreeNode(), new DecisionTreeNode());
            treeRoot.update();
            repaint();
        }
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {

            final JPopupMenu popup = new JPopupMenu();

            if (selectedNode != null && selectedNode != treeRoot) {

                if (!selectedNode.isLeaf()) {
                    final JMenuItem item = new JMenuItem("Delete");
                    popup.add(item);
                    item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    item.addActionListener(this);
                } else {
                    final JMenuItem item = new JMenuItem("Add Decision Node");
                    popup.add(item);
                    item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    item.addActionListener(this);
                }
            }

            popup.setLabel("Justification");
            //popup.setBorder(new BevelBorder(BevelBorder.RAISED));
            popup.addPopupMenuListener(this);

            //Point2D scaledPnt = transform.deltaTransform(e.getPoint(), null);
            popup.show(this, e.getX(), e.getY());
            //popup.show(this, (int)scaledPnt.getX(), (int)scaledPnt.getY());
            showHelp = false;
        }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    /**
     * Handle mouse pressed event
     *
     * @param e the mouse event
     */
    public void mousePressed(MouseEvent e) {
        final DecisionTreeNode n = findNode(e.getPoint());
        if (selectedNode != n) {
            selectedNode = n;
            editPanel.setSelected(selectedNode);

            repaint();
        }

        checkPopup(e);

        lastMousePos = e.getPoint();
    }

    /**
     * Handle mouse clicked event
     *
     * @param e the mouse event
     */
    public void mouseClicked(MouseEvent e) {
        checkPopup(e);
        showHelp = false;

        if (e.getClickCount() == 2) {
            if (selectedNode != null) {
                editPanel.openExpressionEditor();
                treeRoot.update();
                repaint();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * Handle mouse released event
     *
     * @param e the mouse event
     */
    public void mouseReleased(MouseEvent e) {
        checkPopup(e);

        repaint();
    }

    /**
     * Handle mouse dragged event
     *
     * @param e the mouse event
     */
    public void mouseDragged(MouseEvent e) {
        if (selectedNode != null) {
            int dx = e.getX() - (int) lastMousePos.getX();
            int dy = e.getY() - (int) lastMousePos.getY();
            selectedNode.moveTree(dx, dy);
            lastMousePos = e.getPoint();
            repaint();
        }
    }

    /**
     * Handle mouse moved event
     *
     * @param e the mouse event
     */
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Invoked when the mouse wheel is rotated.
     *
     * @see MouseWheelEvent
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        final int notches = e.getWheelRotation();
        if (notches < 0) {
            scale -= 0.1;
        } else {
            if (scale < 1)
                scale += 0.1;
        }
        repaint();
    }

    private DecisionTreeNode findNode(Point p) {

        final DecisionTreeNode[] array = treeRoot.toArray();
        for (DecisionTreeNode n : array) {
            if (n.isWithin(p))
                return n;
        }
        return null;
    }
}

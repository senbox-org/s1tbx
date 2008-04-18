/*
 * $Id: PinSymbolDialog.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.pin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.esa.beam.framework.datamodel.PinSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.ui.ModalDialog;

//@todo 1 se/se - class dokumentation

public class PinSymbolDialog extends ModalDialog {

    private PinSymbol _pinSymbol;
    private JButton _symbolButton;

    public PinSymbolDialog(Window parent) {
        super(parent, "Pin symbol", ModalDialog.ID_OK_CANCEL, null);/*I18N*/
        createParameter();
        creatUI();
    }

    private void createParameter() {
    }

    private void creatUI() {
        final JPanel content = new JPanel(new BorderLayout());
        _symbolButton = new JButton() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (_pinSymbol != null) {
                    final Graphics2D g2d = ((Graphics2D) g);
                    PixelPos refPoint = _pinSymbol.getRefPoint();
                    if (refPoint != null) {
                        g2d.translate(refPoint.getX(), refPoint.getY());
                        _pinSymbol.draw(g2d);
                        g2d.translate(-refPoint.getX(), -refPoint.getY());
                    } else {
                        _pinSymbol.draw(g2d);
                    }
                }
            }
        };
        _symbolButton.setPreferredSize(new Dimension(100, 100));
        final JPanel button = new JPanel();
        button.add(_symbolButton);
        content.add(button);
        content.add(new JLabel("Not implemented!"), BorderLayout.SOUTH);
        setContent(content);
    }

    public PinSymbol getPinSymbol() {
        return _pinSymbol;
    }

    public void setPinSymbol(PinSymbol pinSymbol) {
        _pinSymbol = pinSymbol;
        final Rectangle bounds = pinSymbol.getShape().getBounds();
        _symbolButton.setPreferredSize(new Dimension(bounds.width + 10, bounds.height + 10));
    }
}

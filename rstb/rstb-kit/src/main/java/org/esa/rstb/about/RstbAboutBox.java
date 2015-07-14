/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.rstb.about;

import javax.swing.JLabel;
import org.esa.snap.rcp.about.AboutBox;

/**
 * @author Norman
 */
@AboutBox(displayName = "RSTB", position = 100)
public class RstbAboutBox extends JLabel {
    
    public RstbAboutBox() {
        super("<html>This is the shining <b>Radarsat-2 Toolbox</b>");
    }
}

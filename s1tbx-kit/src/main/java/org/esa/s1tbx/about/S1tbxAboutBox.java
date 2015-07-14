/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s1tbx.about;

import javax.swing.JLabel;
import org.esa.snap.rcp.about.AboutBox;

/**
 * @author Norman
 */
@AboutBox(displayName = "S1TBX", position = 10)
public class S1tbxAboutBox extends JLabel {
    
    public S1tbxAboutBox() {
        super("<html>This is the incredible <b>Sentinel-1 Toolbox</b>");
    }
}

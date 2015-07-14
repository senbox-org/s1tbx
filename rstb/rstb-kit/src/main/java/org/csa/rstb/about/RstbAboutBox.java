/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.about;

import javax.swing.JLabel;
import org.esa.snap.rcp.about.AboutBox;

/**
 * @author Norman
 */
@AboutBox(displayName = "RSTB", position = 100)
public class RstbAboutBox extends JLabel {
    
    public RstbAboutBox() {
        super("<html>" +
                      "<b>RSTB</b>" +
                      "<br>(c) Copyright 2015 by Array Systems Computing Inc. and contributors. All rights reserved." +
                      "<br>" +
                      "<br>This program has been developed under contract to CSA." +
                      "<br><br>" +
                      "<u><b>The RSTB team</b></u>" +
                      "<br>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Array Systems Computing</b>:" +
                      "</td></tr>" +
                      "&nbsp;&nbsp;<b>Rajesh Jha</b><br>" +
                      "&nbsp;&nbsp;<b>Ali Mahmoodi</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Luis Veci</b><br>" +
                      "&nbsp;&nbsp;<b>Jun Lu</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Cecilia Wong</b><br>" +
                      "&nbsp;&nbsp;<b>Roberta Manners</b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Canadian Space Agency</b> (CSA):<br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Stephane Chalifoux</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Cristobal Colon</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Robert Saint-Jean</b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Canada Centre for Mapping and Earth Observation</b> (CCMEO):<br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Francois Charboneau</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b></b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Canadian Forest Service</b> (CFS):<br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Hao Chen</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>David Hill</b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Geological Survey of Canada</b> (GSC):<br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Paul Fraser</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Dustin Whalen</b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<table border=0>" +
                      "<tr><td>" +
                      "<b>Agriculture and Agri-Food Canada</b> (AAFC):<br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Heather McNairn</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Amine Merzouki</b><br>" +
                      "</td><td>" +
                      "&nbsp;&nbsp;<b>Catherine Champagne</b><br>" +
                      "</td></tr>" +
                      "</table>" +
                      "<br>This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License" +
                      "<br>as published by the Free Software Foundation. This program is distributed in the hope it will be useful, but WITHOUT ANY" +
                      "<br>WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
                      "<br>See the GNU General Public License for more details." +
                      "</html>"
                );
    }
}

/*
 *    GeoTools - The Open Source Java GIS Tookit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This file is hereby placed into the Public Domain. This means anyone is
 *    free to do whatever they wish with this file. Use it well and enjoy!
 */
package org.esa.beam.visat.actions;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;

public class WMSChooser extends JDialog implements ActionListener{
	private  String server = "http://localhost:8080/geoserver/wms";
	private String request = "?REQUEST=GetCapabilities";
	URL url;
	WebMapServer wms;
	WMSCapabilities caps;
	JList list;
	ArrayList layers = new ArrayList();
	ArrayList layerNames = new ArrayList();
	public WMSChooser() throws HeadlessException {
		super();
		init();
	}

	public WMSChooser(Frame owner, boolean modal) throws HeadlessException {
		super(owner, modal);
		init();
	}

	public WMSChooser(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
		super(owner, title, modal, gc);
		init();
	}

	public WMSChooser(Frame owner, String title, boolean modal) throws HeadlessException {
		super(owner, title, modal);
		init();
	}

	public WMSChooser(Frame owner, String title) throws HeadlessException {
		super(owner, title);
		init();
	}

	public WMSChooser(Frame owner) throws HeadlessException {
		super(owner);
		init();
	}

	private void init() {
		try {
			this.setSize(400,200);
			url = new URL(server+request);
			wms = new WebMapServer(url);



			setupLayersList();
//			Create and initialize the buttons.
	        JButton cancelButton = new JButton("Cancel");
	        cancelButton.addActionListener(this);
	        //
	        final JButton setButton = new JButton("Set");
	        setButton.setActionCommand("Set");
	        setButton.addActionListener(this);
	        getRootPane().setDefaultButton(setButton);
			list = new JList(layerNames.toArray());
			list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			list.setLayoutOrientation(JList.VERTICAL);
			list.setVisibleRowCount(-1);
			list.addMouseListener(new MouseAdapter() {
	            public void mouseClicked(MouseEvent e) {
	                if (e.getClickCount() == 2) {
	                    setButton.doClick(); //emulate button click
	                }
	            }
	        });
			JScrollPane listScroller = new JScrollPane(list);
			listScroller.setPreferredSize(new Dimension(400, 280));
			JPanel listPane = new JPanel();
	        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
	        JLabel label = new JLabel("Layers");
	        label.setLabelFor(list);
	        listPane.add(label);
	        listPane.add(Box.createRigidArea(new Dimension(0,5)));
	        listPane.add(listScroller);
	        listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	        //Lay out the buttons from left to right.
	        JPanel buttonPane = new JPanel();
	        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
	        buttonPane.add(Box.createHorizontalGlue());
	        buttonPane.add(cancelButton);
	        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	        buttonPane.add(setButton);

	        //Put everything together, using the content pane's BorderLayout.
	        Container contentPane = getContentPane();
	        contentPane.add(listPane, BorderLayout.CENTER);
	        contentPane.add(buttonPane, BorderLayout.PAGE_END);


		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void setupLayersList() {
		caps = wms.getCapabilities();
		layers.clear();
		layerNames.clear();
		for( Iterator i = caps.getLayerList().iterator(); i.hasNext();){
			Layer layer = (Layer) i.next();
			layerNames.add(layer.getTitle());
			layers.add(layer);


		}
	}

	public int getLayer() {
		// TODO Auto-generated method stub
		return list.getSelectedIndex();
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("cancel")) {
			list.clearSelection();
		}
		this.setVisible(false);
	}

	public ArrayList getLayers() {
		return layers;
	}

	public WebMapServer getWms() {
		return wms;
	}

	public void setWms(WebMapServer wms) {
		this.wms = wms;
		setupLayersList();
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
		System.out.println("setting server to "+server);
		try {
			url = new URL(server+request);
			System.out.println("url "+url.toString());
			wms = new WebMapServer(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setupLayersList();

	}
}

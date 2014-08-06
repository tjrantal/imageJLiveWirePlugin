/*
 Image/J Plugins
 Copyright (C) 2012 - 2014 Timo Rantalainen
 Author's email: tjrantal at gmail dot com
 The code is licensed under GPL 3.0 or newer
 */
package	edu.deakin.timo;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.measure.*;	/*For obtaining pixel dimensions from original stack...*/
import ij.gui.*;			/*For creating the output stack images*/
import ij.process.*;		/*For setting output stack image properties*/
import ij.io.*;			/*For setting image voxel dimensions...*/
import java.util.*;				/*For enumeration*/
import java.awt.*;
import java.awt.event.*;		/**MouseListener*/
import edu.deakin.timo.liveWireEngine.*;	/**Live wire implementation*/

/*
	LiveWire ImageJ plug-in modified from ivus snakes (http://ivussnakes.sourceforge.net/) ImageJ plugin 
	Changed the implementation back to the one suggested in Barret & Mortensen 1997.
	Interactive live-wire boundary extraction. Medical Image Analysis (1996/7) volume 1, number 4, pp 331�341.
 */

public class LiveWirePlugin implements PlugIn, MouseListener, MouseMotionListener {
	ImageCanvas canvas;
	ImagePlus imp;
	PolygonRoi roi;
	Polygon polygon;
	int width;
	int height;
	LiveWireCosts lwc;
	
	/**Implement the PlugIn interface*/
    public void run(String arg) {
        imp = WindowManager.getCurrentImage();
        /*Check that an image was open*/
		if (imp == null) {
            IJ.noImage();
            return;
        }
		/*Get image size and stack depth*/
		width = imp.getWidth();
		height = imp.getHeight();
		
		/*Register listener*/
		ImageWindow win = imp.getWindow();
		canvas = win.getCanvas();
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		polygon = new Polygon();
		double[][] pixels = new double[width][height];
		short[] tempPointer = (short[]) imp.getProcessor().getPixels();	
		for (int r = 0;r<height;++r){
			for (int c = 0;c<width;++c){
				pixels[c][r] = (double) tempPointer[c+r*width];
			}
		}
		
		/*Init livewire*/
		lwc = new LiveWireCosts(pixels);
    }
	
	/*Implement the MouseListener, and MouseMotionListener interfaces*/
	public void mousePressed(MouseEvent e) {
		/*If alt is pressed when a button is clicked, disconnect listeners, i.e. stop the plugin*/
		if (e.getClickCount() > 1 || e.getButton() == MouseEvent.BUTTON2 ||  (e.getModifiersEx() & InputEvent.ALT_MASK) != 0||  (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0){
			if (false){
				//Remove the last point added with the first click of the double click
				int[] pX = new int[polygon.npoints];
				int[] pY = new int[polygon.npoints];
				for (int i = 0;i< polygon.npoints-1;++i){
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				//Connect the first, and the last digitized point
				pX[polygon.npoints-1] = polygon.xpoints[0];
				pY[polygon.npoints-1] = polygon.ypoints[0];
				//re-create the polygon
				polygon = new Polygon(pX, pY, pX.length);
			}else{
				//Do not remove the last point, simply connect last point, and initial point
				polygon.addPoint(polygon.xpoints[0],polygon.ypoints[0]);
			}
			
			//IJ.log("Appended, length after "+polygon.npoints);
			roi = new PolygonRoi(polygon,Roi.POLYGON);
			imp.setRoi(roi,true);
			/**Remove listeners*/
			//IJ.log("Start removing listeners");
			((ImageCanvas) e.getSource()).removeMouseListener(this);
			((ImageCanvas) e.getSource()).removeMouseMotionListener(this);
			//IJ.log("All done");
		}
		
		
		//IJ.log("Right button: "+((e.getModifiers()&Event.META_MASK)!=0));
	}

	public void mouseReleased(MouseEvent e) {
		int screenX = e.getX();
		int screeY = e.getY();
		int x = canvas.offScreenX(screenX);
		int y = canvas.offScreenY(screeY);
		
		
		//IJ.log("Mouse released: "+x+","+y+" polygon length "+polygon.npoints);
		/*Draw the latest Polygon*/
		if (polygon.npoints > 0){
			if ((e.getModifiersEx() & InputEvent.SHIFT_MASK) != 0||  (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0){
				//Get a straight line
				polygon.addPoint(x,y);
			}else{
				//Get polygon from livewire
				int[][] fromSeedToCursor = lwc.returnPath(x,y);
				int[] pX = new int[polygon.npoints+fromSeedToCursor.length];
				int[] pY = new int[polygon.npoints+fromSeedToCursor.length];
				for (int i = 0;i< polygon.npoints;++i){
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				for (int i = 0;i< fromSeedToCursor.length;++i){
					pX[polygon.npoints+i] = fromSeedToCursor[i][0];
					pY[polygon.npoints+i] = fromSeedToCursor[i][1];
				}
				polygon = new Polygon(pX, pY, pX.length);
			}
			/**Get, and set the ROI*/
			roi = new PolygonRoi(polygon,Roi.POLYLINE);
			imp.setRoi(roi,true);
		}else{
			polygon.addPoint(x,y);
		}
		lwc.setSeed(x,y);
	}
	
	public void mouseDragged(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		if (polygon.npoints > 0){
			int screenX = e.getX();
			int screeY = e.getY();
			int x = canvas.offScreenX(screenX);
			int y = canvas.offScreenY(screeY);
			int[] pX;
			int[] pY;
			if ((e.getModifiersEx() & InputEvent.SHIFT_MASK) != 0||  (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0){
				pX = new int[polygon.npoints+1];
				pY = new int[polygon.npoints+1];
				for (int i = 0;i< polygon.npoints;++i){
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				pX[polygon.npoints] = x;
				pY[polygon.npoints] = y;				
			} else {
				//Use livewire
				int[][] fromSeedToCursor = lwc.returnPath(x,y);
				//IJ.log("Return path length "+fromSeedToCursor.length);
				pX = new int[polygon.npoints+fromSeedToCursor.length];
				pY = new int[polygon.npoints+fromSeedToCursor.length];
				for (int i = 0;i< polygon.npoints;++i){
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				for (int i = 0;i< fromSeedToCursor.length;++i){
					pX[polygon.npoints+i] = fromSeedToCursor[i][0];
					pY[polygon.npoints+i] = fromSeedToCursor[i][1];
				}
			}
			imp.setRoi(new PolygonRoi(pX, pY, pX.length, Roi.POLYLINE),true);			
		}
	}
	
}

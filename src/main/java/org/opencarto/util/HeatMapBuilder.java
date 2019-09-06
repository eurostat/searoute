package org.opencarto.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Heat map builder algorithm
 * 
 * @author julien Gaffuri
 *
 */
public class HeatMapBuilder {

	private int w, h;
	private int rad;
	private int[][] pts;
	private float[] kernel;
	private Color[] colors;
	private Color backColor;
	private WritableRaster r;
	private BufferedImage img;
	private ArrayList<Integer> vals;
	private int[] quantileValues;

	public HeatMapBuilder(int w, int h, int[][] pts){
		this(w,h,pts,4);
	}

	public HeatMapBuilder(int w, int h, int[][] pts, int kernelRadius){
		this(w,h,pts,kernelRadius,null);
		colors = ColorUtil.getColors(new Color[]{Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED}, 6);
	}

	public HeatMapBuilder(int w, int h, int[][] pts, int kernelRadius, Color[] colors){
		this(w, h, pts, kernelRadius, colors, new Color(255,255,255,0));
	}

	public HeatMapBuilder(int w, int h, int[][] pts, int kernelRadius, Color[] colors, Color backColor){
		this.w = w;
		this.h = h;
		this.pts = pts;

		this.rad = kernelRadius;
		if(rad<=0)
			System.out.println("Warning: gaussian kernel with weird radius: " + rad);

		buildKernel();
		this.colors = colors;
		this.backColor = backColor;
	}

	private void buildKernel() {
		int size=rad*2+1;
		kernel=new float[size];
		float sig=rad/3;
		float sa=2*sig*sig;
		float sb=(float) (2*Math.PI*sig);
		float sc=(float)Math.sqrt(sb);
		float sum=0;
		int j=0;
		for (int i=-rad; i<=rad; i++) {
			float d=i*i;
			if (d>rad*rad)
				kernel[j]=0;
			else
				kernel[j]=(float)Math.exp(-(d)/sa) / sc;
			sum += kernel[j];
			j++;
		}
		for (int i=0; i<size; i++)
			kernel[i]/=sum;
	}

	public WritableRaster getRaster(){
		if(r==null){
			//make raster (a bit bigger than the initial one, according to the kernel radius)
			r = new BufferedImage(w+2*rad, h+2*rad, BufferedImage.TYPE_INT_ARGB).getAlphaRaster();

			//fill initial raster
			for(int k=0; k<pts.length; k++){
				int i = pts[k][0];
				int j = pts[k][1];
				try {
					r.setDataElements(i+rad, j+rad, new int[]{ ((int[])r.getDataElements(i, j, null))[0]+1000 });
				} catch (ArrayIndexOutOfBoundsException e) {}
			}

			//apply convolution
			if(rad>0){
				ConvolveOp f;
				f = new ConvolveOp(new Kernel(1, kernel.length, kernel),ConvolveOp.EDGE_ZERO_FILL,null);
				r = f.filter(r, null);
				f = new ConvolveOp(new Kernel(kernel.length, 1, kernel),ConvolveOp.EDGE_ZERO_FILL,null);
				r = f.filter(r, null);
			}
		}
		return r;
	}

	public ArrayList<Integer> getValues(){
		if(vals == null){
			//retrieve all non null values
			vals = new ArrayList<Integer>();
			for(int i=0; i<w; i++){
				for(int j=0; j<h; j++){
					int v = ((int[])getRaster().getDataElements(i+rad, j+rad, null))[0];
					if(v==0) continue;
					vals.add(new Integer(v));
				}
			}
		}
		return vals;
	}

	public int[] getQuantileValues(){
		if(quantileValues == null){
			//sort values
			Collections.sort(getValues());
			int nbPerColor = getValues().size()/colors.length;
			quantileValues = new int[colors.length-1];
			for(int i=0;i<quantileValues.length; i++){
				quantileValues[i] = getValues().get((i+1)*nbPerColor);
			}
		}
		return quantileValues;
	}

	public BufferedImage getImage(){
		if(img == null){
			//may happen...
			if(getValues().size() == 0){
				BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
				Graphics g = img.getGraphics();
				g.setColor(backColor);
				g.fillRect(0, 0, w, h);
				return img;
			}

			//build image
			img = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D)img.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//draw image pixel by pixel
			for(int i=0; i<w; i++){
				for(int j=0; j<h; j++){
					double v = ((int[])getRaster().getDataElements(i+rad, j+rad, null))[0];
					g.setColor(getColor(v,getQuantileValues()));
					g.fillRect(i, j, 1, 1);
				}
			}
		}
		return img;
	}

	private Color getColor(double v, int[] indexValues){
		if(v==0) return backColor;
		return colors[getColorIndex(v,indexValues)];
	}

	private int getColorIndex(double v, int[] indexValues){
		int i=0;
		while(i<indexValues.length && v>indexValues[i])
			i++;
		return i;
	}

}

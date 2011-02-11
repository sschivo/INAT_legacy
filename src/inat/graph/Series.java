package inat.graph;


import java.awt.*;

public class Series {
	protected static int seriesCounter = 0;
	
	private P[] data = null;
	private String name = "";
	private boolean enabled = true;
	private Scale scale = null;
	private Series master = null, slave = null; //ideally, the slave series should be used to represent confidence intervals for the corresponding master series
	public static String SLAVE_SUFFIX = "_stddev"; //for a series to be a representation of confidence intervals of series ABC, its name should be "ABC" + SLAVE_SUFFIX (suffix can have any capitalization)
	private Color myColor = null;
	private boolean changeColor = false;
	
	public Series(P[] data) {
		this(data, new Scale());
	}
	
	public Series(P[] data, Scale scale) {
		this(data, scale, "Series " + (++seriesCounter)); 
	}
	
	public Series(P[] data, Scale scale, String name) {
		this.data = data;
		this.setScale(scale);
		this.name = name;
	}
	
	public void setScale(Scale scale) {
		this.scale = scale;
		if (!isSlave()) {
			this.scale.addData(data);
		} else {
			P[] dataLow = new P[data.length];
			P[] dataHigh = new P[data.length];
			for (int i=0;i<data.length;i++) {
				dataLow[i] = new P(data[i].x, master.data[i].y - data[i].y);
				dataHigh[i] = new P(data[i].x, master.data[i].y + data[i].y);
			}
			this.scale.addData(dataLow);
			this.scale.addData(dataHigh);
		}
	}
	
	public Scale getScale() {
		return this.scale;
	}
	
	public String getName() {
		return this.name;
	}
	
	public P[] getData() {
		return this.data;
	}
	
	public void setSlave(Series s) {
		this.setSlave(s, true);
	}
	
	private void setSlave(Series s, boolean propagate) {
		this.slave = s;
		if (propagate) {
			s.setMaster(this, false);
		}
	}
	
	public void setMaster(Series s) {
		this.setMaster(s, true);
	}
	
	private void setMaster(Series s, boolean propagate) {
		this.master = s;
		this.setScale(this.master.getScale());
		if (propagate) {
			s.setSlave(this, false);
		}
	}
	
	public boolean isSlave() {
		return this.master != null;
	}
	
	public boolean isMaster() {
		return this.slave != null; 
	}
	
	//wether to show this series or not
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (slave != null) {
			this.slave.setEnabled(enabled);
		}
	}
	
	public boolean getEnabled() {
		return this.enabled;
	}
	
	public void setChangeColor(boolean changeColor) {
		this.changeColor = changeColor;
	}
	
	public boolean getChangeColor() {
		return this.changeColor;
	}
	
	public Color getColor() {
		return this.myColor;
	}
	
	public void plot(Graphics2D g, Rectangle bounds) {
		scale.computeScale(bounds);
		double scaleX = scale.getXScale(),
			   scaleY = scale.getYScale(),
			   minX = scale.getMinX(),
			   minY = scale.getMinY();
		if (!enabled) return;
		
		if (master != null) {
			myColor = master.myColor;
			P[] masterData = master.getData();
			int i = 0;
			for (P punto : data) {
				for (;i<masterData.length && masterData[i].x<punto.x;i++);
				if (i < masterData.length) {
					g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)), 
							(int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  + punto.y - minY)));
					g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)) - 3, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)), 
							(int)(bounds.x + scaleX * (masterData[i].x - minX)) + 3, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  - punto.y - minY)));
					g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)) - 3, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)), 
							(int)(bounds.x + scaleX * (masterData[i].x - minX)) + 3, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  + punto.y - minY)));
				}
			}
		} else {
			myColor = g.getColor();
			if (slave != null) {
				slave.myColor = myColor;
			}
			P vecchio = data[0];
			for (int j=1;j<data.length;j++) {
				P punto = data[j];
				g.drawLine((int)(bounds.x + scaleX * (vecchio.x - minX)), (int)(bounds.y + bounds.height - scaleY * (vecchio.y - minY)), 
						(int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)));
				/*g.drawString("" + punto.x + ", " + punto.y, (int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)));
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine((int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)), (int)(bounds.x + scaleX * (punto.x - minX)), bounds.height + bounds.y);
				g.drawLine((int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)), bounds.x, (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)));
				g.setColor(myColor);*/
				vecchio = punto;
			}
		}
	}
}
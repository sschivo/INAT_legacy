package inat.graph;
import javax.swing.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class Graph extends JPanel implements MouseListener, MouseMotionListener, ActionListener, ComponentListener {
	private static final long serialVersionUID = 8185951065715897260L;
	private static final String AUTOGRAPH_WINDOW_TITLE = "AutoGraph",
								OPEN_LABEL = "Add data from CSV...",
								SAVE_LABEL = "Save as PNG...",
								EXPORT_VISIBLE_LABEL = "Export visible as CSV...",
								CLEAR_LABEL = "Clear Data",
								INTERVAL_LABEL = "Graph interval...",
								CLOSE_LABEL = "Close",
								CSV_FILE_EXTENSION = ".csv",
								CSV_FILE_DESCRIPTION = "CSV file",
								DEFAULT_CSV_FILE = "/local/schivos/aData1_0-1440_normalized_MK2_JNK1_IKK_with_stddev.csv", //"/local/schivos/aData1_0-1440_times5_normalized_better_onlyMK2_JNK1_IKK_con_stddev.csv",
								CSV_IO_PROBLEM = "Problem reading the CSV file!",
								GENERIC_ERROR_S = "There has been a problem: ";
	private static final java.awt.Color BACKGROUND_COLOR = Color.WHITE, FOREGROUND_COLOR = Color.BLACK, DISABLED_COLOR = Color.LIGHT_GRAY;
	
	private Vector<Series> data = null;
	private Scale scale = null;
	private String xSeriesName = null;
	private JPopupMenu popupMenu = null;
	private boolean showLegend = true;
	private double maxLabelLength = 0;
	private Rectangle legendBounds = null;
	private boolean customLegendPosition = false;
	private boolean movingLegend = false;
	private int oldLegendX = 0, oldLegendY = 0;

	public Graph() {
		data = new Vector<Series>();
		scale = new Scale();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addComponentListener(this);
		popupMenu = new JPopupMenu();
		JMenuItem open = new JMenuItem(OPEN_LABEL);
		JMenuItem save = new JMenuItem(SAVE_LABEL);
		JMenuItem export = new JMenuItem(EXPORT_VISIBLE_LABEL);
		JMenuItem clear = new JMenuItem(CLEAR_LABEL);
		JMenuItem newInterval = new JMenuItem(INTERVAL_LABEL);
		JMenuItem close = new JMenuItem(CLOSE_LABEL);
		open.addActionListener(this);
		save.addActionListener(this);
		export.addActionListener(this);
		clear.addActionListener(this);
		newInterval.addActionListener(this);
		close.addActionListener(this);
		popupMenu.add(open);
		popupMenu.add(save);
		popupMenu.add(export);
		popupMenu.add(clear);
		popupMenu.add(newInterval);
		popupMenu.addSeparator();
		popupMenu.add(close);
		this.add(popupMenu);
		customLegendPosition = false;
		movingLegend = false;
		legendBounds = null;
	}
	
	//reset the scale in order to plot another different graph, and remove all data
	public void reset() {
		data = new Vector<Series>();
		scale.reset();
		setXSeriesName(null);
	}
	
	public void addSeries(P[] series) {
		data.add(new Series(series));
	}
	
	public void addSeries(P[] series, String name) {
		data.add(new Series(series, scale, name));
	}
	
	public void changeEnabledSeries(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		data.elementAt(seriesIdx).setEnabled(!data.elementAt(seriesIdx).getEnabled());
	}
	
	public void changeSeriesColor(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		data.elementAt(seriesIdx).setChangeColor(true);
	}
	
	public void setXSeriesName(String name) {
		this.xSeriesName = name;
	}
	
	public String getXSeriesName() {
		return this.xSeriesName;
	}
	
	private Color colori[] = {/*Color.RED, Color.BLUE, Color.GREEN, 
							   Color.ORANGE, Color.CYAN, Color.GRAY, 
							   Color.MAGENTA, Color.YELLOW, Color.PINK*/
								/*Color.RED, Color.BLUE, Color.GREEN.darker(),
								Color.ORANGE, Color.CYAN.darker(), new Color(184, 46, 46),
								Color.GREEN, Color.ORANGE.darker()*/
								new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138),
								new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28),
								new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214)
								/*new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218),
								new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98),
								new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217),
								new Color(188, 128, 189), new Color(204, 235, 197), new Color(255, 237, 111)*/
							};
	int idxColore = 0;
	Random random = new Random();
	
	private void resetCol() {
		idxColore = 0;
	}
	
	private Color nextCol() {
		Color c = colori[idxColore];
		idxColore++;
		if (idxColore > colori.length-1) {
			idxColore = 0;
		}
		return c;
	}
	
	private Color randomCol() {
		return colori[0 + random.nextInt(colori.length-1 - 0)];
	}
	
	public int findSeriesInLegend(int x, int y) {
		if (x >= 5 && x <= legendBounds.width - 5
			&& y >= 5 && y <= legendBounds.height - 5) {
			int seriesIdx = y / 20;
			int countShownSeries = 0,
				countExistingSeries = 0;
			while (countExistingSeries<data.size()) {
				if (data.elementAt(countExistingSeries).isSlave()) {
					countShownSeries--;
				}
				if (countShownSeries == seriesIdx) break;
				countExistingSeries++;
				countShownSeries++;
			}
			return countExistingSeries;
		} else {
			return -1;
		}
	}
	
	//draw axes, arrow points, ticks and label X axis with the label found in the first column of the CSV datafile
	public void drawAxes(Graphics2D g, Rectangle bounds) {
		FontMetrics fm = g.getFontMetrics();
		g.setPaint(FOREGROUND_COLOR);
		g.drawLine(bounds.x - 10, bounds.height + bounds.y, bounds.x + bounds.width + 10, bounds.height + bounds.y);
		g.drawLine(bounds.x, bounds.height + bounds.y + 10, bounds.x, bounds.y - 10);
		g.drawLine(bounds.x + bounds.width + 10, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height - 5);
		g.drawLine(bounds.x + bounds.width + 10, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height + 5);
		g.drawLine(bounds.x, bounds.y - 10, bounds.x - 5, bounds.y);
		g.drawLine(bounds.x, bounds.y - 10, bounds.x + 5, bounds.y);
		
		int xTick = bounds.x,
		yTick = bounds.y + bounds.height;
		double minX = scale.getMinX(),
			   maxX = scale.getMaxX(),
			   scaleX = scale.getXScale();
		int interval = (int)(maxX - minX + 1);
		int increase = 1;
		//awful heuristic in order to get some ticks 
		while (interval > 0) {
			interval = interval / 10;
			increase = increase * 10;
		}
		while ((maxX - minX + 1) / increase < 8) {
			increase = increase / 10;
		}
		if (increase < 1) increase = 1;
		//questa condizione dice: se le due etichette pi� lunghe si sovrappongono perch� sono troppo vicine..
		while (increase * scaleX < fm.stringWidth(new Integer((int)maxX).toString())) {
		//if ((maxX - minX + 1) / increase > 20) { //questa invece si limitava a vedere se venivano troppe (in assoluto) tick: ma non sappiamo quanto � largo il grafico!
			increase = increase * 2;
		}
		int xStartString = bounds.x + bounds.width;
		if (xSeriesName != null) {
			xStartString -= fm.stringWidth(xSeriesName) - 5;
		}
		for (int i=increase; i<maxX && (xTick + increase * scaleX + fm.stringWidth(new Integer(i).toString())) < xStartString; i+=increase) {
			xTick = (int) (bounds.x + scaleX * (i - minX));
			if (xTick < bounds.x) continue;
			if (xTick > bounds.x + bounds.width) break;
			g.drawLine(xTick, yTick - 5, xTick, yTick + 5);
			String label = new Integer(i).toString();
			g.drawString(label, xTick - fm.stringWidth(label)/2, yTick + 3 + fm.getHeight());
		}
		
		xTick = bounds.x;
		yTick = bounds.y + bounds.height;
		double minY = scale.getMinY(),
			   maxY = scale.getMaxY(),
			   scaleY = scale.getYScale();
		interval = (int)(maxY - minY + 1);
		increase = 1;
		while (interval > 0) {
			interval = interval / 10;
			increase = increase * 10;
		}
		while ((maxY - minY + 1) / increase < 8) {
			increase = increase / 10;
		}
		if (increase < 1) increase = 1;
		while (increase * scaleY < fm.getHeight()) {
			increase = increase * 2;
		}
		for (int i=increase; i < maxY; i+=increase) {
			yTick = (int)(bounds.y + bounds.height - scaleY * (i - minY));
			if (yTick > bounds.y + bounds.height) continue;
			if (yTick < bounds.y) break;
			g.drawLine(xTick - 5, yTick, xTick + 5, yTick);
			String label = new Integer(i).toString();
			g.drawString(label, xTick - fm.stringWidth(label) - 5, yTick - 3 + fm.getHeight()/2);
		}
		
		if (xSeriesName != null) {
			g.drawString(xSeriesName, xStartString, bounds.y + bounds.height + 3 + fm.getHeight());
		}
	}
	
	public void drawLegend(Graphics2D g, Rectangle bounds) {
		//g.clearRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g.setPaint(BACKGROUND_COLOR);
		g.fill(bounds);
		g.setPaint(FOREGROUND_COLOR);
		g.draw(bounds);
		resetCol();
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(3));
		int nLegend = 0;
		for (Series series : data) {
			if (series.isSlave()) continue;
			g.setPaint(series.getColor());
			g.drawLine(bounds.x + 5, bounds.y + 10 + nLegend * 20, bounds.x + 25, bounds.y + 10 + nLegend * 20);
			if (series.getEnabled()) {
				g.setPaint(FOREGROUND_COLOR);
			} else {
				g.setPaint(DISABLED_COLOR);
			}
			g.drawString(series.getName(), bounds.x + 30, bounds.y + 15 + nLegend * 20);
			nLegend++;
		}
		g.setStroke(oldStroke);
	}
	
	public void paint(Graphics g1) {
		Graphics2D g = (Graphics2D)g1;
		//g.clearRect(0, 0, this.getWidth(), this.getHeight());
		g.setPaint(BACKGROUND_COLOR);
		g.fill(this.getBounds());
		FontMetrics fm = g.getFontMetrics();
		maxLabelLength = 0;
		Rectangle bounds = new Rectangle(this.getBounds());
		bounds.setBounds(bounds.x + 20, bounds.y + 20, bounds.width - 40, bounds.height - 40);
		
		//Assi + freccine: li ho spostati dopo, senn� il disegno del grafico me li sovrascriveva
		/*g.setPaint(java.awt.Color.BLACK);
		g.drawLine(bounds.x - 5, bounds.height + bounds.y, bounds.x + bounds.width, bounds.height + bounds.y);
		g.drawLine(bounds.x, bounds.height + bounds.y + 5, bounds.x, bounds.y);
		g.drawLine(bounds.x + bounds.width, bounds.y + bounds.height, bounds.x + bounds.width - 10, bounds.y + bounds.height - 5);
		g.drawLine(bounds.x + bounds.width, bounds.y + bounds.height, bounds.x + bounds.width - 10, bounds.y + bounds.height + 5);
		g.drawLine(bounds.x, bounds.y, bounds.x - 5, bounds.y + 10);
		g.drawLine(bounds.x, bounds.y, bounds.x + 5, bounds.y + 10);*/
		
		resetCol();
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2));
		for (int i=0;i<data.size();i++) {
			Series series = data.elementAt(i);
			if (!series.isSlave()) {
				if (series.getColor() == null || series.getChangeColor()) {
					if (!series.getChangeColor()) {
						g.setPaint(nextCol());
					} else {
						g.setPaint(randomCol());
						series.setChangeColor(false);
					}
				} else {
					g.setPaint(series.getColor());
				}
			}
			series.plot(g, bounds);
			if (!series.isSlave()) {
				double labelLength = fm.stringWidth(series.getName());
				if (labelLength > maxLabelLength) {
					maxLabelLength = labelLength;
				}
			}
		}
		g.setStroke(oldStroke);
		if (legendBounds == null || !customLegendPosition) {
			int nGraphs = 0;
			for (Series s : data) {
				if (!s.isSlave()) nGraphs++;
			}
			legendBounds = new Rectangle(bounds.width - 35 - (int)maxLabelLength, bounds.y + 20, 35 + (int)maxLabelLength, 20 * nGraphs);
		}
		
		if (showLegend) {
			drawLegend(g, legendBounds);
		}
		
		
		drawAxes(g, bounds);
		
	}
	
	public void parseCSV(String fileName) throws FileNotFoundException, IOException {
		File f = new File(fileName);
		BufferedReader is = new BufferedReader(new FileReader(f));
		String firstLine = is.readLine();
		StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
		int nColonne = tritatutto.countTokens();
		String[] graphNames = new String[nColonne - 1];
		Vector<Vector<P>> grafici = new Vector<Vector<P>>(graphNames.length);
		xSeriesName = tritatutto.nextToken().replace('\"', ' '); //il primo � la X (tempo)
		for (int i=0;i<graphNames.length;i++) {
			graphNames[i] = tritatutto.nextToken();
			graphNames[i] = graphNames[i].replace('\"',' ');
			grafici.add(new Vector<P>());
		}
		while (true) {
			String result = is.readLine();
			if (result == null || result.length() < 2) {
				break;
			}
			StringTokenizer rigaSpezzata = new StringTokenizer(result, ",");
			String s = rigaSpezzata.nextToken();
			double xValue = Double.parseDouble(s); //here s can't be null (differently from below) because there is absolutely no sense in not giving the x value for the entire line
			int lungRiga = rigaSpezzata.countTokens();
			for (int i=0;i<lungRiga;i++) {
				s = rigaSpezzata.nextToken();
				if (s == null || s.trim().length() < 1) continue; //there could be one of the series which does not have a point in this line: we skip it
				grafici.elementAt(i).add(new P(xValue, Double.parseDouble(s)));
			}
		}
		for (int i=0;i<graphNames.length;i++) {
			P[] grafico = new P[1];
			grafico = grafici.elementAt(i).toArray(grafico);
			if (grafico != null && grafico.length > 1) {
				addSeries(grafico, graphNames[i]);
			}
		}
		for (Series s : data) {
			if (s.getName().toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX)) {
				for (Series s2 : data) {
					if (s2.getName().trim().equals(s.getName().trim().substring(0, s.getName().toLowerCase().trim().lastIndexOf(Series.SLAVE_SUFFIX)))) {
						s.setMaster(s2);
					}
				}
			}
		}
		customLegendPosition = false;
	}
	
	public void exportVisible(String fileName) throws FileNotFoundException, IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		out.write(xSeriesName + ",");
		P[][] points = new P[data.size()][];
		int[] indices = new int[data.size()];
		boolean[] finished = new boolean[data.size()];
		int nFinished = 0;
		for (int i=0;i<data.size();i++) { //we use only the enabled series
			if (data.elementAt(i).getEnabled()) {
				finished[i] = false;
			} else {
				finished[i] = true;
				nFinished++;
			}
		}
		for (int i=0;i<data.size();i++) {
			if (finished[i]) continue;
			out.write(data.elementAt(i).getName() + ",");
			points[i] = data.elementAt(i).getData();
			indices[i] = 0;
			finished[i] = false;
		}
		out.newLine();
		//at every cycle, output the minimum x value, and then output all values for which this min x is their x value. the others output an empty space
		//(they will be skipped by my csv parser, and thus we will obtain the exact same graph as the one displayed)
		DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
		while (nFinished < finished.length) {
			double minX = Double.NaN;
			for (int i=0;i<points.length;i++) {
				if (!finished[i] && (Double.isNaN(minX) || minX > points[i][indices[i]].x)) {
					minX = points[i][indices[i]].x;
				}
			}
			out.write("" + formatter.format(minX) + ",");
			for (int i=0;i<points.length;i++) {
				if (!finished[i] && points[i][indices[i]].x == minX) {
					out.write("" + formatter.format(points[i][indices[i]].y) + ",");
					indices[i]++; //this datum has been used, so we can go to the next
					if (indices[i] == points[i].length) {
						finished[i] = true;
						nFinished++;
					}
				} else {
					if (data.elementAt(i).getEnabled()) out.write(" ,");
				}
			}
			out.newLine();
		}
		out.close();
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			Graph g = new Graph();
			JFrame fin = new JFrame("Graph");
			fin.getContentPane().add(g);
			fin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			fin.setBounds(250,50,800,800);
			fin.setVisible(true);
			g.parseCSV(DEFAULT_CSV_FILE);
			g.repaint();
		} else {
			Graph.plotGraph(new File(args[0]));
			exitOnClose();
		}
	}

	
	public static void plotGraph(File csvFile) {
		Graph g = new Graph();
		g.reset();
		try {
			g.parseCSV(csvFile.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(CSV_IO_PROBLEM);
			e.printStackTrace();
		}
		JFrame fin = new JFrame(AUTOGRAPH_WINDOW_TITLE);
		Frame[] listaFinestre = JFrame.getFrames();
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				fin = (JFrame)listaFinestre[i];
				fin.getContentPane().removeAll();
				break;
			}
		}
		fin.getContentPane().add(g);
		java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		fin.setBounds((int)(0.15 * dim.width), (int)(0.15 * dim.height), (int)(0.7 * dim.width), (int)(0.7 * dim.height));
		fin.setVisible(true);
	}
	
	//you can use this if you want to use the existing window and add a new .csv files to the already shown ones (the plotGraph method substitutes the window)
	public static void addGraph(File csvFile) {
		JFrame fin = new JFrame(AUTOGRAPH_WINDOW_TITLE);
		Frame[] listaFinestre = JFrame.getFrames();
		Graph g = null;
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				fin = (JFrame)listaFinestre[i];
				Component[] components = fin.getContentPane().getComponents();
				for (int j=0;j<components.length;j++) {
					if (components[j] instanceof Graph) {
						g = (Graph)components[j];
						break;
					}
				}
				break;
			}
		}
		if (g == null) {
			g = new Graph();
		}
		try {
			g.parseCSV(csvFile.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(CSV_IO_PROBLEM);
			e.printStackTrace();
		}
		java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		fin.setBounds((int)(0.15 * dim.width), (int)(0.15 * dim.height), (int)(0.7 * dim.width), (int)(0.7 * dim.height));
		fin.setVisible(true);
	}

	public static void exitOnClose() {
		Frame[] listaFinestre = JFrame.getFrames();
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				JFrame fin = (JFrame)listaFinestre[i];
				fin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				break;
			}
		}
	}


	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			if (!legendBounds.contains(e.getX(), e.getY()) || !showLegend) {
				showLegend = !showLegend;
				if (showLegend) {
					legendBounds = null;
					customLegendPosition = false;
				}
			} else if (showLegend) { //you can disable/enable series only if the legend is visible
				int xPresumed = e.getX() - legendBounds.x,
					yPresumed = e.getY() - legendBounds.y;
				int seriesIdx = findSeriesInLegend(xPresumed, yPresumed);
				if (seriesIdx != -1) {
					if (xPresumed > 30) {
						this.changeEnabledSeries(seriesIdx);
					} else {
						this.changeSeriesColor(seriesIdx);
					}
				}
			}
			this.repaint();
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if (legendBounds != null && legendBounds.contains(e.getX(), e.getY())) {
				legendBounds = null;
				customLegendPosition = false;
				this.repaint();
			} else {
				popupMenu.show(this, e.getX(), e.getY());
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1 && legendBounds != null && legendBounds.contains(e.getX(), e.getY())) {
			customLegendPosition = true;
			oldLegendX = legendBounds.x - e.getX();
			oldLegendY = legendBounds.y - e.getY();
			movingLegend = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (movingLegend) {
			legendBounds.x = oldLegendX + e.getX();
			legendBounds.y = oldLegendY + e.getY();
			movingLegend = false;
			this.repaint();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (movingLegend) {
			legendBounds.x = oldLegendX + e.getX();
			legendBounds.y = oldLegendY + e.getY();
			oldLegendX = legendBounds.x - e.getX();
			oldLegendY = legendBounds.y - e.getY();
			this.repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
		if (legendBounds != null && showLegend && legendBounds.contains(e.getX(), e.getY())) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JMenuItem) {
			JMenuItem menu = (JMenuItem)source;
			if (menu.getText().equals(OPEN_LABEL)) {
				String fileName = FileUtils.open(CSV_FILE_EXTENSION, CSV_FILE_DESCRIPTION, this);
				if (fileName != null) {
					try {
						this.parseCSV(fileName);
						this.repaint();
					} catch (Exception ex) {
						System.err.println(GENERIC_ERROR_S + ex);
						ex.printStackTrace();
					}
				}
			} else if (menu.getText().equals(SAVE_LABEL)) {
				FileUtils.saveToPNG(this);
			} else if (menu.getText().equals(EXPORT_VISIBLE_LABEL)) {
				String fileName = FileUtils.save(CSV_FILE_EXTENSION, CSV_FILE_DESCRIPTION, this);
				if (fileName != null) {
					try {
						this.exportVisible(fileName);
					} catch (Exception ex) {
						System.err.println(GENERIC_ERROR_S + ex);
						ex.printStackTrace();
					}
				}
			} else if (menu.getText().equals(CLEAR_LABEL)) {
				this.reset();
				this.repaint();
			} else if (menu.getText().equals(INTERVAL_LABEL)) {
				String val = JOptionPane.showInputDialog(this, "Give the value of minimum X", scale.getMinX());
				if (val != null) scale.setMinX(new Double(val));
				val = JOptionPane.showInputDialog(this, "Give the value of maximum X", scale.getMaxX());
				if (val != null) scale.setMaxX(new Double(val));
				val = JOptionPane.showInputDialog(this, "Give the value of minimum Y", scale.getMinY());
				if (val != null) scale.setMinY(new Double(val));
				val = JOptionPane.showInputDialog(this, "Give the value of maximum Y", scale.getMaxY());
				if (val != null) scale.setMaxY(new Double(val));
				this.repaint();
			} else if (menu.getText().equals(CLOSE_LABEL)) {
				//findJFrame(this).setVisible(false);
				findJFrame(this).dispose();
			}
		}
	}
	
	private JFrame findJFrame(Component c) {
		if (c instanceof JFrame) {
			return (JFrame)c;
		} else {
			return findJFrame(c.getParent());
		}
	}

	private int oldWidth = -1, oldHeight = -1;
	public void componentResized(ComponentEvent e) {
		if (!showLegend || !customLegendPosition) {
			oldWidth = this.getWidth();
			oldHeight = this.getHeight();
			return;
		}
		if (oldWidth != -1 && oldHeight != -1) {
			legendBounds.x = (int)((double)legendBounds.x / oldWidth * this.getWidth());
			legendBounds.y = (int)((double)legendBounds.y / oldHeight * this.getHeight());
			repaint();
		}
		oldWidth = this.getWidth();
		oldHeight = this.getHeight();
	}
	
	public void componentHidden(ComponentEvent e) {
		//Auto-generated method stub
	}

	public void componentMoved(ComponentEvent e) {
		//Auto-generated method stub
	}

	public void componentShown(ComponentEvent e) {
		//Auto-generated method stub
	}
	
}
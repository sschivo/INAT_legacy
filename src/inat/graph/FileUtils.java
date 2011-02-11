package inat.graph;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


public class FileUtils {
	private static File currentDirectory = null;
	
	public static String open(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File pathName) {
				if (pathName.getAbsolutePath().endsWith(fileType)) {
					return true;
				}
				return false;
			}

			public String getDescription() {
				return description;
			}
		});
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showOpenDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	
	public static String save(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File pathName) {
				if (pathName.getAbsolutePath().endsWith(fileType)) {
					return true;
				}
				return false;
			}

			public String getDescription() {
				return description;
			}
		});
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showSaveDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			String fileName = chooser.getSelectedFile().getAbsolutePath();
			if (!fileName.endsWith(fileType)) {
				fileName += fileType;
			}
			return fileName;
		}
		return null;
	}
	
	public static void saveToPNG(Component c, String fileName) {
		try {
			BufferedImage image = new BufferedImage(c.getSize().width, c.getSize().height, BufferedImage.TYPE_INT_RGB);
			Graphics imgGraphics = image.createGraphics();
			c.paint(imgGraphics );
			File f = new File(fileName);
			f.delete();
			ImageIO.write(image, "png", f);
			imgGraphics =null;
			image=null;
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
	
	public static void saveToPNG(Component c) {
		String fileName = save(".png", "PNG image", c);
		if (fileName != null) {
			saveToPNG(c, fileName);
		}
	}
}

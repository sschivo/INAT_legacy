package inat.graph;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * The class used for file input/output
 */
public class FileUtils {
	private static File currentDirectory = null;
	
	/**
	 * Show the File Chooser dialog and return the name of the chosen file.
	 * You can choose the file type (ex. ".csv"), the description (ex. "Commma
	 * separated files"), and the parent Component (null is ok: it is just to
	 * tell the O.S. to which window the dialog will "belong") 
	 * @param fileType The file type (ex. ".png")
	 * @param description The file type description (ex. "Image file")
	 * @param parent The parent Component (typically a window. null is ok)
	 * @return The complete (absoluite) path of the file selected by the user, or
	 * null if the user has selected no file/closed the dialog
	 */
	public static String open(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		if (fileType != null) {
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File pathName) {
					if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
						return true;
					}
					return false;
				}
	
				public String getDescription() {
					return description;
				}
			});
		} else {
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File pathName) {
					return true;
				}
	
				public String getDescription() {
					return description;
				}
			});
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showOpenDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	
	/**
	 * Show the save dialog. The workings are the same as the with the open function
	 * @param fileType The file type (ex. ".png")
	 * @param description The file type description (ex. "Image file")
	 * @param parent The parent Component (typically a window. null is ok)
	 * @return The complete (absoluite) path of the file selected by the user, or
	 * null if the user has selected no file/closed the dialog
	 */
	public static String save(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File pathName) {
				if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
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
	
	/**
	 * Save what is currently shown on the given Component to a given .png file
	 * @param c The component whose "photograph" is to be saved
	 * @param fileName The name of the file in which to save the image
	 */
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
	
	/**
	 * Save what is currently shown on the given Component to a
	 * file that the user will choose via the open dialog.
	 * @param c The component to be saved
	 */
	public static void saveToPNG(Component c) {
		String fileName = save(".png", "PNG image", c);
		if (fileName != null) {
			saveToPNG(c, fileName);
		}
	}
}

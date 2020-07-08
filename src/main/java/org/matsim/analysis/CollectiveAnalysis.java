/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;

/**
 * @author Aravind
 *
 */
public class CollectiveAnalysis {
	
	public static void main(String[] args) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("choose root directory");
		chooser.showOpenDialog(null);
		File rootPath = chooser.getSelectedFile();
		File[] directories = new File(rootPath.toString()).listFiles(File::isDirectory);

		try {
			FileWriter fwriter = new FileWriter(new File(rootPath + "\\runOverview.csv"), false);
			BufferedWriter bww = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bww);

			for (int i = 1; i < directories.length; i++) {
				File directory = directories[i];
				File[] files = directory.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.endsWith(".txt") || filename.endsWith(".csv");
					}
				});
				if (i == 1) {
					writer.println("RunID");
				}
				String[] splitDirectory = directory.toString().split("-");
				String runId = splitDirectory[1];
				writer.println();
				writer.print(runId);
				writer.print(",");
				String fileName = null;
				for (int j = 0; j < files.length; j++) {
					File file = files[j];
					fileName = file.getName();
					if (fileName.endsWith(".drt_customer_stats_drt.csv")
							|| fileName.endsWith(".drt_vehicle_stats_drt.csv") || fileName.endsWith(".modestats.txt")
							|| fileName.endsWith(".scorestats.txt") || fileName.endsWith(".pkm_modestats.txt")) {

						String filePath = directories[i] + "\\" + fileName;
						String[] lastLine = readDataFile(filePath);

						for (int k = 0; k < lastLine.length; k++) {
							String eachValue = lastLine[k];
							if (k == 0) {
								writer.print(",");
								writer.print("<=NewFile=>");
								writer.print(",");
								writer.print(fileName);
								writer.print(",");
							}
							writer.print(eachValue);

							if (k != lastLine.length - 1)
								writer.print(",");
						}
					}
				}
			}
			writer.flush();
			writer.close();
			System.out.println(rootPath + "\\runOverview.csv" + " file written succesfully");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String[] readDataFile(String filePath) {

		BufferedReader br;
		String line = null;
		String[] lastLineValues = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			while ((line = br.readLine()) != null) {
				if (filePath.endsWith(".txt")) {
					lastLineValues = line.split("\\t");
				} else if (filePath.endsWith(".csv")) {
					lastLineValues = line.split(";");
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return lastLineValues;
	}

}

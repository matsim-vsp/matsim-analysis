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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;

/**
 * @author Aravind
 *
 */
public class CollectiveAnalysis {

	public static void main(String[] args) {
		// no: of columns in each file
		int[] attributes = new int[5];
		attributes[0] = 15; // .drt_customer_stats_drt.csv
		attributes[1] = 13; // .drt_vehicle_stats_drt.csv
		attributes[2] = 11; // .modestats.txt
		attributes[3] = 9; // .pkm_modestats.txt
		attributes[4] = 6; // .scorestats.txt
		ArrayList<String> fileList = new ArrayList<String>();
		fileList.add("drt_customer_stats_drt.csv");
		fileList.add("drt_vehicle_stats_drt.csv");
		fileList.add("modestats.txt");
		fileList.add("pkm_modestats.txt");
		fileList.add("scorestats.txt");

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("choose root directory");
		chooser.showOpenDialog(null);
		File rootPath = chooser.getSelectedFile();
		File[] directories = new File(rootPath.toString()).listFiles(File::isDirectory);
		File[] directories1 = directories;
		for (int i = 0; i < directories1.length; i++) {
			File dir = directories1[i];
			if (dir.isHidden()) {
				List<File> list = new ArrayList<File>(Arrays.asList(directories));
				list.remove(dir);
				directories = list.toArray(new File[0]);
			}
		}
		try {
			FileWriter fwriter = new FileWriter(new File(rootPath + "/runOverview.csv"), false);
			BufferedWriter bww = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bww);

			for (int i = 0; i < directories.length; i++) {
				File directory = directories[i];
				File[] files = directory.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.endsWith(".txt") || filename.endsWith(".csv");
					}
				});
				if (i == 1) {
					writer.println("RunID");
				}

				String fileName = null;
				ArrayList<String> paths = new ArrayList<String>();
				for (int j = 0; j < files.length; j++) {
					File file = files[j];
					fileName = file.getName();
					if (fileName.endsWith("drt_customer_stats_drt.csv")
							|| fileName.endsWith("drt_vehicle_stats_drt.csv") || fileName.endsWith("modestats.txt")
							|| fileName.endsWith("scorestats.txt") || fileName.endsWith("pkm_modestats.txt")) {

						paths.add(fileName);

					}
				}
				Collections.sort(paths, new Comparator<String>() {
					@Override
					public int compare(String s1, String s2) {
						return s1.compareToIgnoreCase(s2);
					}
				});

				int count = 0;
				int count1 = 0;
				Iterator<String> pathItr = paths.iterator();
				String runId = null;
				while (pathItr.hasNext()) {
					String name = pathItr.next();
					if (count == 0) {
						String[] splitDirectory = name.split("[.]");
						runId = splitDirectory[0];
						writer.println();
						writer.print(runId);

					}
					String modName = name.replace(runId + ".", "");
					int space = 0;
					int index = fileList.indexOf(modName);
					if (index != count1) {

						for (int a = count1; a < index; a++) {
							space = space + attributes[a];
						}
						count1 = index;
						if (count == 0) {
							writer.print(",");
							writer.print("	");
							writer.print(",");
							writer.print("	");
							writer.print(",");
						}
						for (int b = 0; b < space; b++) {
							writer.print(" ");
							writer.print(",");
						}
					}

					String filePath = directories[i] + "/" + name;
					String[] lastLine = readDataFile(filePath);

					for (int k = 0; k < lastLine.length; k++) {
						String eachValue = lastLine[k];
						if (k == 0) {
							if (space == 0) {
								writer.print(",");
							}

							writer.print("	");
							writer.print(",");
							writer.print("	");
							writer.print(",");
							writer.print(name);
							writer.print(",");
						}
						if (eachValue.contains(",")) {
							eachValue = String.format("\"%s\"", eachValue);
						}
						if (!eachValue.matches("^[a-zA-Z0-9.,_\"\"]+$")) {
							eachValue = "NA";
						}
						writer.print(eachValue);

						if (k != lastLine.length - 1)
							writer.print(",");
					}
					count1++;
					count++;

				}

			}

			writer.flush();
			writer.close();
			System.out.println(rootPath + "/runOverview.csv" + " file written succesfully");
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
					lastLineValues = line.split("\\s");
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

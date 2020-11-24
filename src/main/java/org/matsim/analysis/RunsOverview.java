package org.matsim.analysis;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

public class RunsOverview {

	ArrayList<String> fileList;
	String separator = ",";
	String directoryToScanForRuns;
	String[] filesList;
	int drt_customer_stats_drt;
	int drt_vehicle_stats_drt;
	int modestats;
	int pkm_modestats;
	int scorestats;
	ArrayList<String> columNames;

	public RunsOverview(ArrayList<String> fileList) {
		this.fileList = fileList;
	}

	public static void main(String[] args) {

		RunsOverview analysis = new RunsOverview(null);
		String filesToCopy = null;
		if (args.length >= 2) {
			analysis.setDirectoryToScanForRuns(args[0]);
			analysis.setSeparator(args[1]);
		}
		if (args.length == 3) {
			filesToCopy = args[2];
			String[] filesToCopyList = null;
			if (filesToCopy != null && !filesToCopy.equals("null")) {
				filesToCopyList = filesToCopy.split(",");
				analysis.addFilesinList(filesToCopyList);
			}
		}

		if (args.length < 2) {

			JFrame frame = new JFrame();
			frame.setTitle("Choose separator");
			frame.setSize(500, 250);
			frame.setLocationRelativeTo(null);
			frame.getContentPane().setLayout(new BorderLayout());
			JPanel panel = new JPanel();
			JLabel label = new JLabel("Choose the separator");
			panel.add(label);

			String comboboxList[] = { "Comma ,", "Semicolon ;" };
			JComboBox<String> comboBox = new JComboBox<String>(comboboxList);
			panel.add(comboBox);

			JButton okButton = new JButton("ok"); // added for ok button

			panel.add(okButton);
			panel.setVisible(true);
			panel.setSize(500, 250);

			frame.add(panel);
			frame.setVisible(true);

			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String separator = null;
					// frame.setVisible(false);
					frame.dispose();
					Object item = comboBox.getSelectedItem();
					System.out.println("choosen value " + item.toString());
					String userOption = item.toString();

					switch (userOption) {

					case "Comma ,":
						separator = ",";
						break;

					case "Semicolon ;":
						separator = ";";
						break;
					}
					analysis.setSeparator(separator);
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setDialogTitle("choose root directory");
					chooser.showOpenDialog(null);
					File rootPath = chooser.getSelectedFile();
					analysis.setDirectoryToScanForRuns(rootPath.toString());
					analysis.run(analysis.getSeparator(), analysis.getDirectoryToScanForRuns());
				}
			});
		} else {
			analysis.run(analysis.getSeparator(), analysis.getDirectoryToScanForRuns());
		}
	}

	public void addFilesinList(String[] filesArray) {
		fileList = new ArrayList<String>();
		for (int k = 0; k < filesArray.length; k++) {
			fileList.add(filesArray[k]);
		}
		Collections.sort(fileList, (s1, s2) -> s1.compareToIgnoreCase(s2));
	}

	public void run(String separator, String directoryToScanForRuns) {

		ArrayList<File> directoryPaths = new ArrayList<File>();
		File[] directories = new File(directoryToScanForRuns).listFiles(File::isDirectory);
		for (int i = 0; i < directories.length; i++) {
			File dir = directories[i];
			if (!dir.isHidden() && dir.getName().contains("output-")) {
				directoryPaths.add(dir);
			}
		}
		listAllFiles(directoryPaths);
	}

	public void listAllFiles(ArrayList<File> directoryPaths) {
		Map<String, ArrayList<String>> runIdWithPath = new HashMap<String, ArrayList<String>>();
		Iterator<File> directoryPathsItr = directoryPaths.iterator();
		while (directoryPathsItr.hasNext()) {
			ArrayList<String> pathList = new ArrayList<String>();
			File directory = directoryPathsItr.next();
			String dirName = directory.getName();
			String[] nameSplit = dirName.split("-");
			Iterator<String> filesItr;
			if (fileList == null) {
				initiateFileList();
			}
			filesItr = fileList.iterator();
			while (filesItr.hasNext()) {
				String path = directory + "/" + nameSplit[1] + "." + filesItr.next();
				pathList.add(path);
			}
			runIdWithPath.put(nameSplit[1], pathList);
		}
		// sorting runids
		LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted = new LinkedHashMap<>();
		runIdWithPath.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> runIdWithPathSorted.put(x.getKey(), x.getValue()));
		String[][] matrix = readFiles(runIdWithPathSorted);
		String[][] matrixToPrint = addColumnTitles(matrix, columNames);
		writeData(matrixToPrint, getDirectoryToScanForRuns(), separator);
	}

	public String[][] readFiles(LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
		String line = null;
		Set<String> keySet = runIdWithPathSorted.keySet();
		Iterator<String> keyItr = keySet.iterator();
		LinkedHashMap<String, ArrayList<String>> mappedFilesAndColumns = mapFilesAndColumns(runIdWithPathSorted);
		String[][] matrix = new String[keySet.size() + 1][100];
		int rowCount = 1;
		int columnCount = 0;
		while (keyItr.hasNext()) {
			String[] values = null;
			String[] columns = null;
			columnCount = 3;
			String key = keyItr.next();
			matrix[rowCount][0] = key;
			ArrayList<String> filePaths = runIdWithPathSorted.get(key);
			BufferedReader br;
			Iterator<String> filePathsItr = filePaths.iterator();
			while (filePathsItr.hasNext()) {
				ArrayList<String> val = new ArrayList<String>();
				ArrayList<String> col = new ArrayList<String>();
				String filePath = filePathsItr.next();
				try {
					br = new BufferedReader(new FileReader(filePath));
					String lastLineToRead = null;
					String firstLineToRead = null;
					int count = 0;
					while ((line = br.readLine()) != null) {
						if (count == 0) {
							firstLineToRead = line;
						}
						lastLineToRead = line;
						count++;
					}
					if (filePath.endsWith(".txt")) {
						values = lastLineToRead.split("\t");
						columns = firstLineToRead.split("\t");
					} else if (filePath.endsWith(".csv")) {
						values = lastLineToRead.split(";");
						columns = firstLineToRead.split(";");
					}
					for (int i = 0; i < values.length; i++) {
						val.add(values[i]);
						col.add(columns[i]);
					}
					int loopLimit = values.length;
					for (int i = 0; i < loopLimit; i++) {
						File file = new File(filePath);
						String fileName = file.getName();
						String[] fileNameSplit = fileName.split("[.]");
						ArrayList<String> listToCompare = mappedFilesAndColumns.get(fileNameSplit[1]);
						if (!col.get(i).contentEquals(listToCompare.get(i))) {
							val.add(i, "NA");
							col.add(i, listToCompare.get(i));
							loopLimit++;
						}
						if (i == 0) {
							matrix[rowCount][columnCount] = fileName;
							columnCount++;
						}
						String eachValue = val.get(i);
						String[] split = null;
						if (eachValue.contains(",")) {
							split = eachValue.split(",");
							eachValue = "";
							for (int ii = 0; ii < split.length; ii++) {
								eachValue += split[ii];
							}
						}
						if (StringUtils.isNumeric(eachValue)) {
							NumberFormat nf = NumberFormat.getInstance(Locale.UK);
							nf.parse(eachValue).doubleValue();
						}
						if (!eachValue.matches("^[a-zA-Z0-9.,_\"\"]+$")) {
							eachValue = "NA";
						}
						matrix[rowCount][columnCount] = eachValue;
						columnCount++;
					}
					columnCount += 2;
				} catch (FileNotFoundException e) {
					File file1 = new File(filePath);
					matrix[rowCount][columnCount] = file1.getName();
					columnCount++;
					String[] fileName = filePath.split("[.]");
					String file = fileName[fileName.length - 2];
					int columnNumber = findOutColumnCount(file);
					for (int i = 0; i < columnNumber; i++) {
						matrix[rowCount][columnCount] = "NA";
						columnCount++;
					}
					columnCount += 2;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			rowCount++;
		}
		return matrix;
	}

	// this method is to map files with respective column names
	public LinkedHashMap<String, ArrayList<String>> mapFilesAndColumns(
			LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
		String line = null;
		String[] columnTitles = null;
		int columnSize = 0;
		String[] values;
		Set<String> keySet = runIdWithPathSorted.keySet();
		Iterator<String> keyItr = keySet.iterator();
		int prevColumnSize = 0;
		LinkedHashMap<String, ArrayList<String>> mappedFilesAndColumns = null;

		while (keyItr.hasNext()) {
			columNames = new ArrayList<String>();
			LinkedHashMap<String, ArrayList<String>> filesWithColumnNames = new LinkedHashMap<String, ArrayList<String>>();
			values = null;
			columnSize = 0;
			String key = keyItr.next();
			ArrayList<String> filePaths = runIdWithPathSorted.get(key);
			BufferedReader br;
			Iterator<String> filePathsItr = filePaths.iterator();
			prevColumnSize = columnSize;
			int itr = 1;
			while (filePathsItr.hasNext()) {
				try {
					String path = filePathsItr.next();
					File file = new File(path);
					br = new BufferedReader(new FileReader(path));
//					while ((line = br.readLine()) != null) {
					String lastLineToRead = null;
					String firstLineToRead = null;
					int count = 0;
					while ((line = br.readLine()) != null) {
						if (count == 0) {
							firstLineToRead = line;
						}
						lastLineToRead = line;
						count++;
					}
					if (path.endsWith(".txt")) {
						values = lastLineToRead.split("\t");
						columnTitles = firstLineToRead.split("\t");
					} else if (path.endsWith(".csv")) {
						values = lastLineToRead.split(";");
						columnTitles = firstLineToRead.split(";");
					}
					columnSize += values.length;
					String[] fileName = file.getName().split("[.]");
					decideEachFileColumnsCount(fileName[1], values.length);
					createListOfColumnTitles(columnTitles);
					mappedFilesAndColumns = mapColumnTitlesToFiles(columnTitles, fileName[1], filesWithColumnNames);
//					}
				} catch (FileNotFoundException e) {
					itr=0;
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
				itr++;
			}
			if (itr != 0 && columnSize == prevColumnSize)
				break;

		}
		return mappedFilesAndColumns;
	}

	// ========================================================================================================================================================================

	private void initiateFileList() {
		fileList = new ArrayList<String>();
		fileList.add("drt_customer_stats_drt.csv");
		fileList.add("drt_vehicle_stats_drt.csv");
		fileList.add("modestats.txt");
		fileList.add("pkm_modestats.txt");
		fileList.add("scorestats.txt");
		Collections.sort(fileList, (s1, s2) -> s1.compareToIgnoreCase(s2));
	}

	private void decideEachFileColumnsCount(String fileName, int noOfColumns) {

		switch (fileName) {

		case "drt_customer_stats_drt":
			drt_customer_stats_drt = noOfColumns;
			break;

		case "drt_vehicle_stats_drt":
			drt_vehicle_stats_drt = noOfColumns;
			break;

		case "modestats":
			modestats = noOfColumns;
			break;

		case "pkm_modestats":
			pkm_modestats = noOfColumns;
			break;

		case "scorestats":
			scorestats = noOfColumns;
			break;

		}

	}

	private int findOutColumnCount(String fileName) {
		int returValue = 0;
		switch (fileName) {

		case "drt_customer_stats_drt":
			returValue = drt_customer_stats_drt;
			break;

		case "drt_vehicle_stats_drt":
			returValue = drt_vehicle_stats_drt;
			break;

		case "modestats":
			returValue = modestats;
			break;

		case "pkm_modestats":
			returValue = pkm_modestats;
			break;

		case "scorestats":
			returValue = scorestats;
			break;

		}
		return returValue;

	}

	public String[][] addColumnTitles(String[][] matrix, ArrayList<String> titles) {
		Iterator<String> titlesItr = titles.iterator();
		int count = 0;
		while (titlesItr.hasNext()) {
			if (count < 4) {
				matrix[0][count] = null;
			} else {
				matrix[0][count] = titlesItr.next();
			}
			count++;
		}
		return matrix;
	}

	public void createListOfColumnTitles(String[] columnTitles) {
		for (int i = 0; i < columnTitles.length; i++) {
			columNames.add(columnTitles[i]);
		}
		columNames.add(null);
		columNames.add(null);
		columNames.add(null);
	}

	private LinkedHashMap<String, ArrayList<String>> mapColumnTitlesToFiles(String[] columnTitles, String fileName,
			LinkedHashMap<String, ArrayList<String>> filesWithColumnNames) {
		ArrayList<String> columNames = new ArrayList<String>();
		for (int i = 0; i < columnTitles.length; i++) {
			columNames.add(columnTitles[i]);
		}
		filesWithColumnNames.put(fileName, columNames);
		return filesWithColumnNames;
	}

	// writing data to file
	public static void writeData(String[][] matrix, String rootPath, String separator) {
		int column = matrix[0].length;
		int row = matrix.length;
		try {
			FileWriter fwriter = new FileWriter(new File(rootPath + "/runOverview.csv"), false);
			BufferedWriter bw = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bw);
			for (int i = 0; i < row; i++) {
				writer.println();
				for (int j = 0; j < column; j++) {
					String value = matrix[i][j];
					if (value == null) {
						value = "";
					}
					writer.print(value);
					writer.print(separator);
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

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public String getDirectoryToScanForRuns() {
		return directoryToScanForRuns;
	}

	public void setDirectoryToScanForRuns(String directoryToScanForRuns) {
		this.directoryToScanForRuns = directoryToScanForRuns;
	}

}
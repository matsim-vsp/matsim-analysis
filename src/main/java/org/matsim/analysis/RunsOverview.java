package org.matsim.analysis;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class RunsOverview {

	Set<String> fileList;
	ArrayList<String> columnNames;
	HashMap<String, Integer> fileColumnCount;
	private static final Logger log = Logger.getLogger(RunsOverview.class);

	public RunsOverview(Set<String> fileList) {
		this.fileList = fileList;
	}

	public static void main(String[] args) {

		RunsOverview analysis = new RunsOverview(null);
		String filesToCopy;
		String separator = ",";
		String directoryToScanForRuns = "";
		if (args.length >= 2) {
			directoryToScanForRuns = args[0];
			separator = args[1];
		}
		if (args.length == 3) {
			filesToCopy = args[2];
			String[] filesToCopyList;
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

			String[] comboboxList;
			comboboxList = new String[]{ "Comma ,", "Semicolon ;" };
			JComboBox<String> comboBox = new JComboBox<>(comboboxList);
			panel.add(comboBox);

			JButton okButton = new JButton("ok"); // added for ok button

			panel.add(okButton);
			panel.setVisible(true);
			panel.setSize(500, 250);

			frame.add(panel);
			frame.setVisible(true);

			okButton.addActionListener(e -> {
				String separator1 = null;
				frame.dispose();
				Object item = comboBox.getSelectedItem();
				System.out.println("chosen value " + item.toString());
				String userOption = item.toString();

				switch (userOption) {

				case "Comma ,":
					separator1 = ",";
					break;

				case "Semicolon ;":
					separator1 = ";";
					break;
				}
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setDialogTitle("choose root directory");
				chooser.showOpenDialog(null);
				File rootPath = chooser.getSelectedFile();
				analysis.run(separator1, rootPath.toString());
			});
		} else {
			analysis.run(separator, directoryToScanForRuns);
		}
	}

	public void addFilesinList(String[] filesArray) {
		fileList = Set.of(filesArray);
	}

	public void run(String separator, String directoryToScanForRuns) {

		ArrayList<File> directoryPaths = new ArrayList<>();
		File[] directories = new File(directoryToScanForRuns).listFiles(File::isDirectory);
		for (File dir : directories) {
			if (!dir.isHidden() && dir.getName().contains("output-")) {
				directoryPaths.add(dir);
			}
		}
		listAllFiles(directoryPaths, separator, directoryToScanForRuns);
	}

	private void listAllFiles(ArrayList<File> directoryPaths, String separator, String directoryToScanForRuns) {
		Map<String, ArrayList<String>> runIdWithPath = new HashMap<>();
		for (File directoryPath : directoryPaths) {
			ArrayList<String> pathList = new ArrayList<>();
			String dirName = directoryPath.getName();
			String[] nameSplit = dirName.split("-");
			Iterator<String> filesItr;
			if (fileList == null) {
				initiateDefaultFileList();
			}
			filesItr = fileList.iterator();
			while (filesItr.hasNext()) {
				String path = directoryPath + "/" + nameSplit[1] + "." + filesItr.next();
				pathList.add(path);
			}
			runIdWithPath.put(nameSplit[1], pathList);
		}
		// sorting runids
		LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted = new LinkedHashMap<>();
		runIdWithPath.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> runIdWithPathSorted.put(x.getKey(), x.getValue()));
		String[][] matrix = readFiles(runIdWithPathSorted);
		String[][] matrixToPrint = addColumnTitles(matrix, columnNames);
		writeData(matrixToPrint, directoryToScanForRuns, separator);
	}

	private String[][] readFiles(LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
		String line;
		Set<String> keySet = runIdWithPathSorted.keySet();
		Iterator<String> keyItr = keySet.iterator();
		LinkedHashMap<String, ArrayList<String>> mappedFilesAndColumns = mapFilesAndColumns(runIdWithPathSorted);
		String[][] matrix = new String[keySet.size() + 1][100];
		int rowCount = 1;
		int columnCount;
		while (keyItr.hasNext()) {
			String[] values = null;
			String[] columns = null;
			columnCount = 3;
			String key = keyItr.next();
			matrix[rowCount][0] = key;
			ArrayList<String> filePaths = runIdWithPathSorted.get(key);
			BufferedReader br;
			for (String path : filePaths) {
				ArrayList<String> val = new ArrayList<>();
				ArrayList<String> col = new ArrayList<>();
				try {
					br = new BufferedReader(new FileReader(path));
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
						columns = firstLineToRead.split("\t");
					} else if (path.endsWith(".csv")) {
						// TODO: Find way to identify csv with , separator
						values = lastLineToRead.split(";");
						columns = firstLineToRead.split(";");
					}
					for (int i = 0; i < values.length; i++) {
						val.add(values[i]);
						col.add(columns[i]);
					}
					int loopLimit = values.length;
					for (int i = 0; i < loopLimit; i++) {
						File file = new File(path);
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
						StringBuilder eachValue = new StringBuilder(val.get(i));
						String[] split;
						if (eachValue.toString().contains(",")) {
							split = eachValue.toString().split(",");
							eachValue = new StringBuilder();
							for (String s : split) {
								eachValue.append(s);
							}
						}
						if (StringUtils.isNumeric(eachValue.toString())) {
							NumberFormat nf = NumberFormat.getInstance(Locale.UK);
							nf.parse(eachValue.toString()).doubleValue();
						}
						if (!eachValue.toString().matches("^[a-zA-Z0-9.,_\"\"]+$")) {
							eachValue = new StringBuilder("NA");
						}
						matrix[rowCount][columnCount] = eachValue.toString();
						columnCount++;
					}
					columnCount += 2;
				} catch (FileNotFoundException e) {
					log.debug(e);
					File file1 = new File(path);
					matrix[rowCount][columnCount] = file1.getName();
					columnCount++;
					String[] fileName = path.split("[.]");
					String file = fileName[fileName.length - 2];

					int columnNumber = fileColumnCount.get(file);
					for (int i = 0; i < columnNumber; i++) {
						matrix[rowCount][columnCount] = "NA";
						columnCount++;
					}
					columnCount += 2;
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			rowCount++;
		}
		return matrix;
	}

	// this method is to map files with respective column names
	private LinkedHashMap<String, ArrayList<String>> mapFilesAndColumns(
			LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
		fileColumnCount = new HashMap<>();
		String line;
		String[] columnTitles = null;
		int columnSize;
		String[] values;
		Set<String> keySet = runIdWithPathSorted.keySet();
		Iterator<String> keyItr = keySet.iterator();
		int prevColumnSize;
		LinkedHashMap<String, ArrayList<String>> mappedFilesAndColumns = null;

		while (keyItr.hasNext()) {
			columnNames = new ArrayList<>();
			LinkedHashMap<String, ArrayList<String>> filesWithColumnNames = new LinkedHashMap<>();
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

					if (fileColumnCount.get(fileName[1]) != null && fileColumnCount.get(fileName[1]) != values.length) {
						log.error("Number of columns in file " + file.getName() +
								" differs from number of columns in other runs. Not implemented yet. Exiting.");
						throw new RuntimeException("Number of columns in file " + file.getName() +
								" differs from number of columns in other runs. Not implemented yet. Exiting.");
					} else {
						fileColumnCount.put(fileName[1], values.length);
					}

					createListOfColumnTitles(columnTitles);
					mappedFilesAndColumns = mapColumnTitlesToFiles(columnTitles, fileName[1], filesWithColumnNames);
//					}
				} catch (FileNotFoundException e) {
					itr = 0;
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

	private void initiateDefaultFileList() {
		fileList = new HashSet<>();
		fileList.add("drt_customer_stats_drt.csv");
		fileList.add("drt_vehicle_stats_drt.csv");
		fileList.add("modestats.txt");
		fileList.add("pkm_modestats.txt");
		fileList.add("scorestats.txt");
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
		Collections.addAll(columnNames, columnTitles);
		columnNames.add(null);
		columnNames.add(null);
		columnNames.add(null);
	}

	private LinkedHashMap<String, ArrayList<String>> mapColumnTitlesToFiles(String[] columnTitles, String fileName,
			LinkedHashMap<String, ArrayList<String>> filesWithColumnNames) {
		ArrayList<String> columnNames = new ArrayList<>();
		Collections.addAll(columnNames, columnTitles);
		filesWithColumnNames.put(fileName, columnNames);
		return filesWithColumnNames;
	}

	// writing data to file
	public static void writeData(String[][] matrix, String rootPath, String separator) {
		int column = matrix[0].length;
		try {
			FileWriter fwriter = new FileWriter(rootPath + "/runsOverview.csv", false);
			BufferedWriter bw = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bw);
			for (String[] strings : matrix) {
				for (int j = 0; j < column; j++) {
					String value = strings[j];
					if (value == null) {
						value = "";
					}
					writer.print(value);
					writer.print(separator);
				}
				writer.println();
			}
			writer.flush();
			writer.close();
			System.out.println(rootPath + "/runsOverview.csv" + " file written successfully");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
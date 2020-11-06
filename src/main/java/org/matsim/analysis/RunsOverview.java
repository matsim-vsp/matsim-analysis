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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

public class RunsOverview {

	LinkedHashSet<String> fileList = new LinkedHashSet<String>();
	String directoryToScanForRuns = null;
	String separator = ",";
	Set<String> filesList = null;

	public RunsOverview(String directoryToScanForRuns, String separator, Set<String> filesList) {
		this.directoryToScanForRuns = directoryToScanForRuns;
		this.separator = separator;
		this.filesList = filesList;
	}

	public static void main(String[] args) {
		String directoryToScanForRuns = null;
		String separator = null;
		Set<String> filesList;

		if (args.length < 2) {
			// not sufficient arguments to do anything, start GUI to make the user specify arguments
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("choose root directory");
			chooser.showOpenDialog(null);

			directoryToScanForRuns = chooser.getSelectedFile().getPath();

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

			String finalDirectoryToScanForRuns = directoryToScanForRuns;
			okButton.addActionListener(e -> {
				String separator1 = null;
				// frame.setVisible(false);
				frame.dispose();
				Object item = comboBox.getSelectedItem();
				System.out.println("choosen value " + item.toString());
				String userOption = item.toString();

				switch (userOption) {

					case "Comma ,":
						separator1 = ",";
						break;

					case "Semicolon ;":
						separator1 = ";";
						break;
				}
				(new RunsOverview(finalDirectoryToScanForRuns, separator1, getDefaultDrtRunFileSet())).run();
			});
		} else {
			directoryToScanForRuns = args[0];
			separator = args[1];
			if (args.length == 3) {
				String filesToCopy = args[2];
				String[] filesToCopyList = null;
				if (filesToCopy != null && !filesToCopy.equals("null")) {
					filesToCopyList = filesToCopy.split(",");
					filesList = Set.of(filesToCopyList);
				}
			} else {
				filesList = getDefaultDrtRunFileSet();
			}
			(new RunsOverview(directoryToScanForRuns, separator, getDefaultDrtRunFileSet())).run();
		}
	}

	private LinkedHashMap<String, Map<String, Map<String, String>>> readDataFile(
			LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
		// runID filename column value
		LinkedHashMap<String, Map<String, Map<String, String>>> toPrint = new LinkedHashMap<>();
		BufferedReader br;
		String line = null;
		String[] values = null;
		String[] keys = null;
		Iterator<String> runIdWithPathSortedItr = runIdWithPathSorted.keySet().iterator();
		while (runIdWithPathSortedItr.hasNext()) {
			Map<String, Map<String, String>> lastLineValuesWithFileName = new TreeMap<String, Map<String, String>>();
			String runId = runIdWithPathSortedItr.next();
			ArrayList<String> pathList = runIdWithPathSorted.get(runId);
			ListIterator<String> pathListItr = pathList.listIterator();
			while (pathListItr.hasNext()) {
				String filePath = pathListItr.next();
				// TreeMap sort key by default
				Map<String, String> lastLineValues = new TreeMap<String, String>();
				try {
					br = new BufferedReader(new FileReader(filePath));
					int itr = 0;
					while ((line = br.readLine()) != null) {
						if (filePath.endsWith(".txt")) {
							values = line.split("\\s");
							if (itr == 0) {
								keys = line.split("\\s");
								keys = removeDuplicateKeys(keys);
							}
						} else if (filePath.endsWith(".csv")) {
							values = line.split(";");
							if (itr == 0) {
								keys = line.split(";");
								keys = removeDuplicateKeys(keys);
							}
						}
						itr++;
					}
					for (int i = 0; i < values.length; i++) {
						lastLineValues.put(keys[i], values[i]);
					}
					File fileKey = new File(filePath);
					String[] fileSplit = fileKey.getName().split("[.]");
					lastLineValuesWithFileName.put(fileSplit[1], lastLineValues);

				} catch (FileNotFoundException e) {
					File fileKey = new File(filePath);
					String[] fileSplit = fileKey.getName().split("[.]");
					lastLineValuesWithFileName.put(fileSplit[0], lastLineValues);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			// toPrint ---> (runid --- (filename --> (lastline)))
			toPrint.put(runId, lastLineValuesWithFileName);
		}
		LinkedHashMap<String, Map<String, Map<String, String>>> organisedData = organiseDataTable(toPrint);
		return organisedData;
	}

	// Taking a particular file and its last line values from each runID
	// runId fileName column value
	private LinkedHashMap<String, Map<String, Map<String, String>>> organiseDataTable(
			LinkedHashMap<String, Map<String, Map<String, String>>> toPrint) {
		fileList = getDefaultDrtRunFileSet(); // TODO: This is the initialiseFileList method renamed. This method call here is evil. It does not care which files the user has set up, instead it only works with the default list
		// TreeMap sort key by default
		Map<String, Map<String, String>> lastLine = new TreeMap<String, Map<String, String>>();
		Iterator<String> fileListItr = fileList.iterator();
		Set<String> runIds = toPrint.keySet();
		String fileName = null;
		// getting a particular file from all runIDs
		while (fileListItr.hasNext()) {
			Iterator<String> runIdsItr = runIds.iterator();
			fileName = fileListItr.next();
			String runId = null;
			while (runIdsItr.hasNext()) {
				runId = runIdsItr.next();
				Map<String, String> lastline = toPrint.get(runId).get(fileName);
				// last line of only one file for all runIDs
				lastLine.put(runId, lastline);
			}
			// organized data for last line of only one file for all runIDs
			Map<String, Map<String, String>> orgaisedData = organiseData(lastLine);
			// Now, replace the old values with the new organized data in toPrint
			// overriding the existing (file, last-line) pair with the organized one
			Iterator<String> runIdItr1 = runIds.iterator();
			while (runIdItr1.hasNext()) {
				String runId1 = runIdItr1.next();
				// the files in the directory
				// replace the old values with the new organized data in toPrint
				toPrint.get(runId1).get(fileName).putAll(orgaisedData.get(runId1));
			}
			// toPrint.put(runId, orgaisedData); //wrong
		}

		return toPrint;
	}

	// working with only one file for each runID
	// Here we only check if all the files are having the same columns, if any
	// column is missing add that column with NA as value
	private Map<String, Map<String, String>> organiseData(Map<String, Map<String, String>> lastLine) {
		LinkedHashSet<String> columnName = new LinkedHashSet<String>();

		Set<String> runIdKey = lastLine.keySet();
		Iterator<String> runIdKeyItr = runIdKey.iterator();
		while (runIdKeyItr.hasNext()) {
			String runId = runIdKeyItr.next();
			Map<String, String> lastline = lastLine.get(runId);
			Set<String> lastlineKeySet = lastline.keySet();
			Iterator<String> lastlineKeySetItr = lastlineKeySet.iterator();
			while (lastlineKeySetItr.hasNext()) {
				String lastlineKey = lastlineKeySetItr.next();
				columnName.add(lastlineKey);
			}
			Set<String> runIdKey1 = lastLine.keySet();
			Iterator<String> runIdKeyItr1 = runIdKey1.iterator();
			while (runIdKeyItr1.hasNext()) {
				String runId1 = runIdKeyItr1.next();
				Map<String, String> lastline1 = lastLine.get(runId1);
				Iterator<String> columnNameItr = columnName.iterator();
				while (columnNameItr.hasNext()) {
					String column = columnNameItr.next();
					if (!lastline1.containsKey(column)) {
						lastline1.put(column, "NA");
					}
				}
				lastLine.put(runId1, lastline1);
			}
		}
		return lastLine;
	}

	// writing data to csv file
	public void writeData(LinkedHashMap<String, Map<String, Map<String, String>>> dataToWrite, File rootPath,
			String separator) {

		FileWriter fwriter;
		try {
			fwriter = new FileWriter(new File(rootPath + "/runOverview.csv"), false);
			BufferedWriter bww = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bww);

			Iterator<String> columnRuIds = dataToWrite.keySet().iterator();
			int i = 1;
			while (columnRuIds.hasNext()) {
				String columnRunId1 = columnRuIds.next();
				Map<String, Map<String, String>> dataForRunId1 = dataToWrite.get(columnRunId1);
				Iterator<String> fileKeyItr1 = dataForRunId1.keySet().iterator();
				while (fileKeyItr1.hasNext()) {
					if (i == 1) {
						writer.print("RunID");
						writer.print(" ");
						writer.print(separator);
						writer.print(" ");
						writer.print(separator);
					}
					writer.print(" ");
					writer.print(separator);
					writer.print(" ");
					writer.print(separator);
					String fileKey1 = fileKeyItr1.next();
					Map<String, String> columnData1 = dataForRunId1.get(fileKey1);
					Iterator<String> columnNnameKeyset = columnData1.keySet().iterator();
					while (columnNnameKeyset.hasNext()) {
						String columnName = columnNnameKeyset.next();
						writer.print(fileKey1 + "_" + columnName);
						writer.print(separator);
					}
					i++;
				}
				break;
			}

			Iterator<String> ruIds = dataToWrite.keySet().iterator();
			while (ruIds.hasNext()) {
				String runId = ruIds.next();
				writer.println();
				writer.print(runId);
				writer.print(separator);
				writer.print(" ");
				writer.print(separator);

				Map<String, Map<String, String>> dataForRunId = dataToWrite.get(runId);
				Iterator<String> fileKeyItr = dataForRunId.keySet().iterator();
				while (fileKeyItr.hasNext()) {
					String fileKey = fileKeyItr.next();
					writer.print(" ");
					writer.print(separator);
					writer.print(fileKey);
					writer.print(separator);
					Map<String, String> columnData = dataForRunId.get(fileKey);
					Iterator<String> columnKeyItr = columnData.keySet().iterator();
					while (columnKeyItr.hasNext()) {
						String columnkey = columnKeyItr.next();
						String eachValue = columnData.get(columnkey);
						String[] split = null;
						if (eachValue.contains(",")) {
							split = eachValue.split(",");
							eachValue = "";
							for (int ii = 0; ii < split.length; ii++) {
								eachValue += split[ii];
							}
						}
						if (StringUtils.isNumeric(eachValue)) {
							// eachValue = String.format("\"%s\"", eachValue);
							NumberFormat nf = NumberFormat.getInstance(Locale.UK);
							nf.parse(eachValue).doubleValue();
						}
						if (!eachValue.matches("^[a-zA-Z0-9.,_\"\"]+$")) {
							eachValue = "NA";
						}
						writer.print(eachValue);
						writer.print(separator);
					}
				}
			}
			writer.flush();
			writer.close();
			System.out.println(rootPath + "/runOverview.csv" + " file written succesfully");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// duplicate keys will be created if the column name contains space(column name
	// with space will be read as two columns, Eg. columns in scorestats.txt are
	// avg. EXECUTED avg. WORST avg. AVG avg. BEST),
	// here avg. BEST will be read as two columns column 1:avg. column 2: BEST
	// This method will identify such column names and rebuild the actual column
	// name
	private String[] removeDuplicateKeys(String[] keys) {

		ArrayList<String> uniqueKeys = new ArrayList<String>();
		ArrayList<String> duplicateKeys = new ArrayList<String>();
		ArrayList<String> finalKeys = new ArrayList<String>();
		for (String key : keys) {
			if (!uniqueKeys.contains(key)) {
				uniqueKeys.add(key);
			} else if (!duplicateKeys.contains(key)) {
				duplicateKeys.add(key);
			}
		}
		if (duplicateKeys.size() > 0) {

			Iterator<String> duplicateKeysItr = duplicateKeys.iterator();
			while (duplicateKeysItr.hasNext()) {
				String duplicateKey = duplicateKeysItr.next();
				uniqueKeys.remove(duplicateKey);
			}
			Iterator<String> duplicateKeysItr1 = duplicateKeys.iterator();
			while (duplicateKeysItr1.hasNext()) {
				String duplicateKey = duplicateKeysItr1.next();
				Iterator<String> uniqueKeysItr = null;
				if (finalKeys.size() > 0) {
					uniqueKeysItr = finalKeys.iterator();
					finalKeys = new ArrayList<String>();
				} else {
					uniqueKeysItr = uniqueKeys.iterator();
				}
				while (uniqueKeysItr.hasNext()) {
					String uniqueKey = uniqueKeysItr.next();
					uniqueKey = duplicateKey + uniqueKey;
					finalKeys.add(uniqueKey);
				}
			}
			String[] finalKeyArray = new String[finalKeys.size()];
			Object[] finalKeyArrayObject = finalKeys.toArray();
			for (int i = 0; i < finalKeys.size(); i++) {
				String key = finalKeyArrayObject[i].toString();
				finalKeyArray[i] = key;
			}

			return finalKeyArray;
		}
		return keys;
	}

	public void run() {

		Map<String, ArrayList<String>> runIdWithPath = new HashMap<String, ArrayList<String>>();
		File rootPath = null;
		if (directoryToScanForRuns == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("choose root directory");
			chooser.showOpenDialog(null);

			rootPath = chooser.getSelectedFile();
		} else {
			rootPath = new File(directoryToScanForRuns);
		}

		File[] directories = new File(rootPath.toString()).listFiles(File::isDirectory);

		File[] directories1 = directories;
		for (int i = 0; i < directories1.length; i++) {
			File dir = directories1[i];
			if (dir.isHidden() || !dir.getName().contains("output-")) {
				List<File> list = new ArrayList<File>(Arrays.asList(directories));
				list.remove(dir);
				directories = list.toArray(new File[0]);
			}
		}
		ArrayList<String> paths = new ArrayList<String>();
		for (int i = 0; i < directories.length; i++) {
			paths = new ArrayList<String>();
			File directory = directories[i];
			File[] files = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".txt") || filename.endsWith(".csv");
				}
			});
			String fileName = null;
			for (int j = 0; j < files.length; j++) {
				File file = files[j];
				fileName = file.getName();
				String[] fileNames = fileName.split("[.]");
				String fileNameToCompare = fileNames[1];
				if(filesList.contains(fileNameToCompare)) {
					paths.add(directories[i] + "/" + fileName);
				}
			}
			Collections.sort(paths, (s1, s2) -> s1.compareToIgnoreCase(s2));
			Iterator<String> pathItr = paths.iterator();
			String runId = null;
			while (pathItr.hasNext()) {
				String path = pathItr.next();
				File filePath = new File(path);
				String name = filePath.getName();
				String[] splitDirectory = name.split("[.]");
				runId = splitDirectory[0];
				break;
			}
			runIdWithPath.put(runId, paths);
		}
		// sorting runids
		LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted = new LinkedHashMap<>();
		runIdWithPath.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> runIdWithPathSorted.put(x.getKey(), x.getValue()));
		Iterator<String> keyItr = runIdWithPathSorted.keySet().iterator();
		while (keyItr.hasNext()) {
			fileList = getDefaultDrtRunFileSet(); // TODO: This is the initialiseFileList method renamed. This method call here is evil. It does not care which files the user has set up, instead it only works with the default list
			String key = keyItr.next();
			ArrayList<String> value = runIdWithPathSorted.get(key);
			Iterator<String> valItr = value.iterator();
			ArrayList<String> fileNames = new ArrayList<String>();
			File filePath = null;
			while (valItr.hasNext()) {
				String eachvalue = valItr.next();
				filePath = new File(eachvalue);
				String name = filePath.getName();
				String[] path = name.split("[.]");
				String filename = path[path.length - (path.length - 1)];
				fileNames.add(filename);
			}
			fileList.removeAll(fileNames);

			Iterator<String> fileListItr = fileList.iterator();
			while (fileListItr.hasNext()) {
				String eachvalue = fileListItr.next();
				eachvalue = filePath.getParent() + "/" + eachvalue + ".txt";
				value.add(eachvalue);
			}

			// value.addAll(fileList);

			runIdWithPathSorted.put(key, value);
		}
		LinkedHashMap<String, Map<String, Map<String, String>>> dataToWrite = readDataFile(runIdWithPathSorted);
		writeData(dataToWrite, rootPath, separator);

	}

	public static LinkedHashSet<String> getDefaultDrtRunFileSet() {
		LinkedHashSet<String> fileList = new LinkedHashSet<String>();
		fileList.add("drt_customer_stats_drt");
		fileList.add("drt_vehicle_stats_drt");
		fileList.add("modestats");
		fileList.add("pkm_modestats");
		fileList.add("scorestats");
		return fileList;
	}

}
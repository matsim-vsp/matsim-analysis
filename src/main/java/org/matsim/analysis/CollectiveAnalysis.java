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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JFileChooser;

public class CollectiveAnalysis {

	static LinkedHashSet<String> fileList = new LinkedHashSet<String>();

	public static void main(String[] args) {

		Map<String, ArrayList<String>> runIdWithPath = new HashMap<String, ArrayList<String>>();

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("choose root directory");
		chooser.showOpenDialog(null);
		File rootPath = chooser.getSelectedFile();
		File[] directories = new File(rootPath.toString()).listFiles(File::isDirectory);
		File[] directories1 = directories;
		for (int i = 0; i < directories1.length; i++) {
			File dir = directories1[i];
			if (dir.isHidden() || !dir.getName().contains("output")) {
				List<File> list = new ArrayList<File>(Arrays.asList(directories));
				list.remove(dir);
				directories = list.toArray(new File[0]);
			}
		}
//		try {
//			FileWriter fwriter = new FileWriter(new File(rootPath + "/runOverview.csv"), false);
//			BufferedWriter bww = new BufferedWriter(fwriter);
//			PrintWriter writer = new PrintWriter(bww);
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
				if (fileName.endsWith("drt_customer_stats_drt.csv") || fileName.endsWith("drt_vehicle_stats_drt.csv")
						|| fileName.endsWith("modestats.txt") || fileName.endsWith("scorestats.txt")
						|| fileName.endsWith("pkm_modestats.txt")) {

					paths.add(directories[i] + "/" + fileName);

				}
			}
			Collections.sort(paths, new Comparator<String>() {
				@Override
				public int compare(String s1, String s2) {
					return s1.compareToIgnoreCase(s2);
				}
			});
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
		//sorting runids
		LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted = new LinkedHashMap<>();
		runIdWithPath.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> runIdWithPathSorted.put(x.getKey(), x.getValue()));
		Iterator<String> keyItr = runIdWithPathSorted.keySet().iterator();
		while (keyItr.hasNext()) {
			initiateFileList();
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
			while(fileListItr.hasNext()) {
				String eachvalue = fileListItr.next();
				eachvalue = filePath.getParent()+"\\"+eachvalue+".txt";
				value.add(eachvalue);
			}
			
			//value.addAll(fileList);
			
			runIdWithPathSorted.put(key, value);
		}
		LinkedHashMap<String, Map<String, Map<String, String>>> dataToWrite = readDataFile(runIdWithPathSorted);
		writeData(dataToWrite, rootPath);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private static LinkedHashMap<String, Map<String, Map<String, String>>> readDataFile(
			LinkedHashMap<String, ArrayList<String>> runIdWithPathSorted) {
			//		  runID		  filename	  column	value
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
				//TreeMap sort key by default
				Map<String, String> lastLineValues = new TreeMap<String, String>();
				try {
					br = new BufferedReader(new FileReader(filePath));
					int itr = 0;
					while ((line = br.readLine()) != null) {
						if (filePath.endsWith(".txt")) {
							values = line.split("\\s");
							if (itr == 0) {
								keys = line.split("\\s");
							}
						} else if (filePath.endsWith(".csv")) {
							values = line.split(";");
							if (itr == 0) {
								keys = line.split(";");
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
			//toPrint ---> (runid --- (filename --> (lastline)))
			toPrint.put(runId, lastLineValuesWithFileName);
		}
		LinkedHashMap<String, Map<String, Map<String, String>>> organisedData = organiseDataTable(toPrint);
		return organisedData;
	}
	//Taking a particular file and its last line values from each runID
	//							  runId		fileName	 column   value
	private static LinkedHashMap<String, Map<String, Map<String, String>>> organiseDataTable(
			LinkedHashMap<String, Map<String, Map<String, String>>> toPrint) {
		initiateFileList();
		//TreeMap sort key by default
		Map<String, Map<String, String>> lastLine = new TreeMap<String, Map<String, String>>();
		Iterator<String> fileListItr = fileList.iterator();
		Set<String> runIds = toPrint.keySet();
		String fileName = null;
		//getting a particular file from all runIDs
		while (fileListItr.hasNext()) {
			Iterator<String> runIdsItr = runIds.iterator();
			fileName = fileListItr.next();
			String runId = null;
			while (runIdsItr.hasNext()) {
				runId = runIdsItr.next();
				Map<String, String> lastline = toPrint.get(runId).get(fileName);
				//last line of only one file for all runIDs 
				lastLine.put(runId, lastline);
			}
			//organized data for last line of only one file for all runIDs
			Map<String, Map<String, String>> orgaisedData = organiseData(lastLine);
			//Now, replace the old values with the new organized data in toPrint
			//overriding the existing (file, last-line) pair with the organized one
			Iterator<String> runIdItr1 = runIds.iterator();
			while(runIdItr1.hasNext()) {
				String runId1 = runIdItr1.next();
				    //file      column   value
				//Map<String, Map<String, String>> files = toPrint.get(runId1);//contains all the files in the directory
				//replace the old values with the new organized data in toPrint
				toPrint.get(runId1).get(fileName).putAll(orgaisedData.get(runId1));
			}
			//toPrint.put(runId, orgaisedData); //wrong
		}

		return toPrint;
	}

	//working with only one file for each runID
	//Here we only check if all the files are having the same columns, if any column is missing add that column with NA as value
	private static Map<String, Map<String, String>> organiseData(Map<String, Map<String, String>> lastLine) {
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
				while(columnNameItr.hasNext()) {
					String column = columnNameItr.next();
					if(!lastline1.containsKey(column)) {
						lastline1.put(column, "NA");
					}
				}
				lastLine.put(runId1, lastline1);
			}
		}
		return lastLine;
	}
	
	//writing data to csv file
	public static void writeData(LinkedHashMap<String, Map<String, Map<String, String>>> dataToWrite, File rootPath) {
		
		FileWriter fwriter;
		try {
			fwriter = new FileWriter(new File(rootPath + "/runOverview.csv"), false);
			BufferedWriter bww = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bww);
			
			Iterator<String> columnRuIds = dataToWrite.keySet().iterator();
			int i = 1;
			while(columnRuIds.hasNext()) {
				String columnRunId1 = columnRuIds.next();
				Map<String, Map<String, String>> dataForRunId1 = dataToWrite.get(columnRunId1);
				Iterator<String> fileKeyItr1 = dataForRunId1.keySet().iterator();
				while(fileKeyItr1.hasNext()) {
					if(i == 1) {
						writer.print("RunID");
						writer.print(" ");
						writer.print(",");
						writer.print(" ");
						writer.print(",");
					}
					writer.print(" ");
					writer.print(",");
					writer.print(" ");
					writer.print(",");
					String fileKey1 = fileKeyItr1.next();
					Map<String, String> columnData1 = dataForRunId1.get(fileKey1);
					Iterator<String> columnNnameKeyset = columnData1.keySet().iterator();
					while(columnNnameKeyset.hasNext()) {
						String columnName = columnNnameKeyset.next();
						writer.print(columnName);
						writer.print(",");
					}
						i++;		
				}
				break;
			}
			
			Iterator<String> ruIds = dataToWrite.keySet().iterator();
			while(ruIds.hasNext()) {
				String runId = ruIds.next();
					writer.println();
					writer.print(runId);
					writer.print(",");
					writer.print(" ");
					writer.print(",");
				
				Map<String, Map<String, String>> dataForRunId = dataToWrite.get(runId);
				Iterator<String> fileKeyItr = dataForRunId.keySet().iterator();
				while(fileKeyItr.hasNext()) {
					String fileKey = fileKeyItr.next();
					writer.print(" ");
					writer.print(",");
					writer.print(fileKey);
					writer.print(",");
					Map<String, String> columnData = dataForRunId.get(fileKey);
					Iterator<String> columnKeyItr = columnData.keySet().iterator();
					while(columnKeyItr.hasNext()) {
						String columnkey = columnKeyItr.next();
						String eachValue = columnData.get(columnkey);
						
						if (eachValue.contains(",")) {
							eachValue = String.format("\"%s\"", eachValue);
						}
						if (!eachValue.matches("^[a-zA-Z0-9.,_\"\"]+$")) {
							eachValue = "NA";
						}
						writer.print(eachValue);
						writer.print(",");
					}
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
	
	private static void initiateFileList() {
		fileList = new LinkedHashSet<String>();
		fileList.add("drt_customer_stats_drt");
		fileList.add("drt_vehicle_stats_drt");
		fileList.add("modestats");
		fileList.add("pkm_modestats");
		fileList.add("scorestats");
	}

}
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.junit.Test;

public class CollectiveAnalysisTest {

	static String directoryToScanForRuns = null;
	static ArrayList<String> runIds = new ArrayList<String>();
	static LinkedHashSet<String> fileList = new LinkedHashSet<String>();

	@Test
	public void testAnalysisOutputs() {
		CollectiveAnalysisTest.main((new String[]{"C:\\Users\\Aravind\\svn"}));
	}

	public static void main(String[] args) {
		directoryToScanForRuns = args[0];
		File file = new File(directoryToScanForRuns);
		if (directoryToScanForRuns != null && file.exists()) {
			chooseFilePathAndRead(directoryToScanForRuns);
		}
		
	}

	private static void chooseFilePathAndRead(String directoryToScanForRuns2) {

		Map<String, ArrayList<String>> runIdWithPath = new HashMap<String, ArrayList<String>>();
		File rootPath = null;
		rootPath = new File(directoryToScanForRuns2);

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
			ArrayList<String> fileListToCompare = new ArrayList<String>();

			fileListToCompare.add("drt_customer_stats_drt");
			fileListToCompare.add("drt_vehicle_stats_drt");
			fileListToCompare.add("modestats");
			fileListToCompare.add("scorestats");
			fileListToCompare.add("pkm_modestats");

			String fileName = null;
			for (int j = 0; j < files.length; j++) {
				File file = files[j];
				fileName = file.getName();
				String[] fileNames = fileName.split("[.]");
				String fileNameToCompare = fileNames[1];

				if (fileListToCompare.contains(fileNameToCompare)) {
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
		Iterator<String> runIdItr = runIdWithPath.keySet().iterator();
		requiredRunIds();
		ArrayList<String> runIdsToRremove = new ArrayList<String>();
		while (runIdItr.hasNext()) {
			String runId = runIdItr.next();
			if (!runIds.contains(runId)) {
				runIdsToRremove.add(runId);
			}
		}
		Iterator<String> runIdsToRremoveItr = runIdsToRremove.iterator();
		while (runIdsToRremoveItr.hasNext()) {
			runIdWithPath.remove(runIdsToRremoveItr.next());
		}
		Iterator<String> runIdWithPathItr = runIdWithPath.keySet().iterator();
		ArrayList<String> filesToCompare = new ArrayList<String>();
		initiateFileList();
		while (runIdWithPathItr.hasNext()) {
			String runId = runIdWithPathItr.next();
			ArrayList<String> pathList = runIdWithPath.get(runId);
			Iterator<String> pathListItr = pathList.iterator();
			while (pathListItr.hasNext()) {
				String path = pathListItr.next();
				File filePath = new File(path);
				String name = filePath.getName();
				String[] splitDirectory = name.split("[.]");
				filesToCompare.add(splitDirectory[1]);
			}
			Iterator<String> fileListItr = fileList.iterator();
			while (fileListItr.hasNext()) {
				String fileName = fileListItr.next();
				if (!filesToCompare.contains(fileName)) {
					System.out.println("The RunId " + runId + " does not contain the file " + fileName);
				}
			}
		}
		readAndWriteDataFile(runIdWithPath);

	}

	private static void readAndWriteDataFile(Map<String, ArrayList<String>> runIdWithPath) {

		BufferedReader br;
		String line = null;
		Iterator<String> runIdWithPathItr = runIdWithPath.keySet().iterator();
		while (runIdWithPathItr.hasNext()) {
			String runId = runIdWithPathItr.next();
			ArrayList<String> pathList = runIdWithPath.get(runId);
			ArrayList<String> linesToPrint = new ArrayList<String>();
			ListIterator<String> pathListItr = pathList.listIterator();
			while (pathListItr.hasNext()) {
				String filePath = pathListItr.next();

				try {
					br = new BufferedReader(new FileReader(filePath));
					int itr = 0;
					while ((line = br.readLine()) != null) {
						if (itr == 0 || itr == 2 || itr == 3 || itr == 4 || itr == 500 || itr == 501) {
							linesToPrint.add(line);
						}
						itr++;
					}
					String userDirectory = System.getProperty("user.dir");
					FileWriter fwriter;
					File file = new File(filePath);
					// File directoryToCopy = new File(userDirectory +
					// "/test/input/org/matsim/analysis/");
					File filePathToWrite = new File(
							userDirectory + "/test/input/org/matsim/analysis/run-overview/" + file.getName());

					try {
						fwriter = new FileWriter(filePathToWrite, false);
						BufferedWriter bww = new BufferedWriter(fwriter);
						PrintWriter writer = new PrintWriter(bww);
						Iterator<String> linesToPrintItr = linesToPrint.iterator();

						while (linesToPrintItr.hasNext()) {
							writer.write(linesToPrintItr.next());
							writer.println();
						}
						writer.flush();
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} catch (FileNotFoundException e) {
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	private static void requiredRunIds() {
		runIds = new ArrayList<String>();
		runIds.add("snzDrt340");
		runIds.add("snzDrt341");
		runIds.add("snzDrt342");
		runIds.add("snzDrt343");
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

package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Test;

public class RunsOverviewTest {

	static ArrayList<String> runIds = new ArrayList<String>();
	static LinkedHashSet<String> fileList = new LinkedHashSet<String>();

	@SuppressWarnings("static-access")
	@Test
	public void testAnalysisOutputs() {

		RunsOverview runOverview = new RunsOverview();
		runOverview.chooseFilePathAndRead(",");
		BufferedReader brOne;
		BufferedReader brTwo;

		try {
			brOne = new BufferedReader(
					new FileReader("../matsim-analysis/test/input/org/matsim/analysis/run-overview/runOverview.csv"));
			CSVParser csvParserRunOverview = new CSVParser(brOne, CSVFormat.DEFAULT);
			brTwo = new BufferedReader(new FileReader(
					"../matsim-analysis/test/input/org/matsim/analysis/run-overview/runOverviewToCompare.csv"));
			CSVParser csvParserRunOverviewToCompare = new CSVParser(brTwo, CSVFormat.DEFAULT);
			for (CSVRecord csvRecord : csvParserRunOverview) {
				for (CSVRecord csvRecordToCompare : csvParserRunOverviewToCompare) {

//						String val1 = "0.22";
//						String val2 = "1.22";
//						Assert.assertArrayEquals("failure" + " fails", val1.toCharArray(),
//								val2.toCharArray());
					String fileNameOne = (!(csvRecord.get(3).length() > 1) ? "Row" : csvRecord.get(3));
					String runID = csvRecord.get(0);

					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(4).toCharArray(), csvRecord.get(4).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(5).toCharArray(), csvRecord.get(5).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(6).toCharArray(), csvRecord.get(6).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(7).toCharArray(), csvRecord.get(7).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(8).toCharArray(), csvRecord.get(8).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(9).toCharArray(), csvRecord.get(9).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(10).toCharArray(), csvRecord.get(10).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(11).toCharArray(), csvRecord.get(11).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(12).toCharArray(), csvRecord.get(12).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(13).toCharArray(), csvRecord.get(13).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(14).toCharArray(), csvRecord.get(14).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(15).toCharArray(), csvRecord.get(15).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(16).toCharArray(), csvRecord.get(16).toCharArray());
					Assert.assertArrayEquals(fileNameOne + " of " + runID + " does not match",
							csvRecordToCompare.get(17).toCharArray(), csvRecord.get(17).toCharArray());

					String fileNameTwo = (!(csvRecord.get(19).length() > 1) ? "Row" : csvRecord.get(19));

					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(20).toCharArray(), csvRecord.get(20).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(21).toCharArray(), csvRecord.get(21).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(22).toCharArray(), csvRecord.get(22).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(23).toCharArray(), csvRecord.get(23).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(24).toCharArray(), csvRecord.get(24).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(25).toCharArray(), csvRecord.get(25).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(26).toCharArray(), csvRecord.get(26).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(27).toCharArray(), csvRecord.get(27).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(28).toCharArray(), csvRecord.get(28).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(29).toCharArray(), csvRecord.get(29).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(30).toCharArray(), csvRecord.get(30).toCharArray());
					Assert.assertArrayEquals(fileNameTwo + " of " + runID + " does not match",
							csvRecordToCompare.get(31).toCharArray(), csvRecord.get(31).toCharArray());

					String fileNameThree = (!(csvRecord.get(33).length() > 1) ? "Row" : csvRecord.get(33));

					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(34).toCharArray(), csvRecord.get(34).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(35).toCharArray(), csvRecord.get(35).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(36).toCharArray(), csvRecord.get(36).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(37).toCharArray(), csvRecord.get(37).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(38).toCharArray(), csvRecord.get(38).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(39).toCharArray(), csvRecord.get(39).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(40).toCharArray(), csvRecord.get(40).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(41).toCharArray(), csvRecord.get(41).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(42).toCharArray(), csvRecord.get(42).toCharArray());
					Assert.assertArrayEquals(fileNameThree + " of " + runID + " does not match",
							csvRecordToCompare.get(43).toCharArray(), csvRecord.get(43).toCharArray());

					String fileNameFour = (!(csvRecord.get(45).length() > 1) ? "Row" : csvRecord.get(45));

					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(46).toCharArray(), csvRecord.get(46).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(47).toCharArray(), csvRecord.get(47).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(48).toCharArray(), csvRecord.get(48).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(49).toCharArray(), csvRecord.get(49).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(50).toCharArray(), csvRecord.get(50).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(51).toCharArray(), csvRecord.get(51).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(52).toCharArray(), csvRecord.get(52).toCharArray());
					Assert.assertArrayEquals(fileNameFour + " of " + runID + " does not match",
							csvRecordToCompare.get(53).toCharArray(), csvRecord.get(53).toCharArray());

					String fileNameFive = (!(csvRecord.get(55).length() > 1) ? "Row" : csvRecord.get(55));

					Assert.assertArrayEquals(fileNameFive + " of " + runID + " does not match",
							csvRecordToCompare.get(56).toCharArray(), csvRecord.get(56).toCharArray());
					Assert.assertArrayEquals(fileNameFive + " of " + runID + " does not match",
							csvRecordToCompare.get(57).toCharArray(), csvRecord.get(57).toCharArray());
					Assert.assertArrayEquals(fileNameFive + " of " + runID + " does not match",
							csvRecordToCompare.get(58).toCharArray(), csvRecord.get(58).toCharArray());
					Assert.assertArrayEquals(fileNameFive + " of " + runID + " does not match",
							csvRecordToCompare.get(59).toCharArray(), csvRecord.get(59).toCharArray());
					Assert.assertArrayEquals(fileNameFive + " of " + runID + " does not match",
							csvRecordToCompare.get(60).toCharArray(), csvRecord.get(60).toCharArray());

					break;
				}

			}
			brOne.close();
			brTwo.close();
			csvParserRunOverview.close();
			csvParserRunOverviewToCompare.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

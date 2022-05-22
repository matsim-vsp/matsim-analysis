/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.pngSequence2Video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.jcodec.api.awt.AWTSequenceEncoder;

/**
* @author ikaddoura
*/

public class MATSimVideoUtils {
	private static final Logger log = Logger.getLogger(MATSimVideoUtils.class);

	public static void createLegHistogramVideo(String runDirectory, String runId, String outputDirectory) throws IOException {
		createVideo(runDirectory, runId, outputDirectory, 1, "legHistogram_all");
	}
	
	public static void createLegHistogramVideo(String runDirectory) throws IOException {
		createVideo(runDirectory, null, runDirectory, 1, "legHistogram_all");
	}

	public static void createVideo(String runDirectory, String runId, String outputDirectory, int interval, String pngFileName) throws IOException {
		createVideoX(runDirectory, runId, outputDirectory, interval, pngFileName);
	}
	
	public static void createVideo(String runDirectory, int interval, String pngFileName) throws IOException {
		createVideoX(runDirectory, null, runDirectory, interval, pngFileName);
	}
	
	private static void createVideoX(String runDirectory, String runId, String outputDirectory, int interval, String pngFileName) throws IOException {

		int firstIteration = 0;
		int lastIteration = 1000;
		
		log.info("Generating a video using a png sequence... (file name: " + pngFileName + ", iteration interval: " + interval + ")");
		
		if (!runDirectory.endsWith("/")) {
			runDirectory = runDirectory + "/";
		}
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		String outputDirectoryWithRunId;
		if (runId != null) {
			outputDirectoryWithRunId = outputDirectory + runId + ".";
		} else {
			outputDirectoryWithRunId = outputDirectory;
		}
		
		String outputFile = outputDirectoryWithRunId + pngFileName + ".mp4";
		AWTSequenceEncoder enc = AWTSequenceEncoder.create25Fps(new File(outputFile));

		int counter = 0;
		int counterNoImage = 0;
		for (int i = firstIteration; i<= lastIteration; i++) {
			
			if (counter % interval == 0) {
				
				if (counter % 10 == 0) log.info("Creating frame for iteration " + counter);
				
				String pngFile = null;
				BufferedImage image = null;
				
				try {
					if (runId == null) {
						pngFile = runDirectory + "ITERS/it." + i + "/" + i + "." + pngFileName + ".png";
					} else {
						pngFile = runDirectory + "ITERS/it." + i + "/" + runId + "." + i + "." + pngFileName + ".png";
					}
					image = ImageIO.read(new File(pngFile));

				} catch (IOException e) {

					try {
						pngFile = runDirectory + "ITERS/it." + i + "/" + i + "." + pngFileName + ".png";
						image = ImageIO.read(new File(pngFile));

					} catch (IOException e2){
						if (counterNoImage <= 5) log.warn("Couldn't find png for iteration " + i + "." );
						if (counterNoImage == 5) log.warn("Further warnings of this type will not be printed out.");
						counterNoImage++;
					}
				}
								
				if (image != null) {
					enc.encodeImage(image);
				} else {
//					log.warn("Skipping image...");
				}
			}
			counter++;
		}
		
		try { 
			
			if (counterNoImage < counter) {
				enc.finish();
				log.info("Generating a video using a png sequence... Done. Video written to " + outputFile);
			} else {
				log.warn("Couldn't create a video." );
			}

		} catch (IOException e) {
			log.warn("Couldn't create a video." );
		}
	}

}


[![Build Status](https://travis-ci.org/github/matsim-vsp/matsim-analysis.svg?branch=master)](https://travis-ci.org/github/matsim-vsp/matsim-analysis)

# matsim-analysis

This project may be used to analyze and compare MATSim (Multi-Agent Transport Simulation, www.matsim.org) simulation runs.
Writes out several files (shp, csv) which may be used for visualizations in GIS software (e.g. www.qgis.org) or further processing in spreadsheets software (e.g. Excel).
  
### Some of the analysis functionality:

* person-based analysis (writes person information, e.g. number of trips per day, daily score, used modes, etc. into a csv file, ...)
* trip-based analysis (writes trip information, e.g. travel times, into a csv file, ...)
* aggregated analysis (e.g. number of trips per OD relation, total travel time per mode, total user benefits, ...)
* spatial analysis (e.g. traffic volume per road segment, ...)
* temporal analysis (e.g. number of trips, toll payments etc. per time of day, ...)
* scenario comparison (e.g. person-specific change in score, change in mode, ...)

### Note

The analysis results should be correct for the tested simulation setups and MATSim versions. There is no guarantee that the code does something useful in all possible cases MATSim may be used or extended!

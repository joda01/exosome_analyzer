javac -source 1.7 -target 1.7 -cp "libs/fijii/jars/ij-1.53c.jar":"libs/poi-4.1.2/poi-4.1.2.jar":"libs/poi-4.1.2/poi-excelant-4.1.2.jar":"libs/poi-4.1.2/poi-ooxml-4.1.2.jar":"libs/poi-4.1.2/poi-ooxml-schemas-4.1.2.jar":"libs/commons-lang3-3.11/commons-lang3-3.11.jar":"libs/opencsv/opencsv-4.1.jar" org/danmayr/imagej/*.java org/danmayr/imagej/gui/*.java org/danmayr/imagej/algorithm/*.java org/danmayr/imagej/excel/*.java

jar cmvf META-INF/MANIFEST.MF EvColoc.jar plugins.config org/*

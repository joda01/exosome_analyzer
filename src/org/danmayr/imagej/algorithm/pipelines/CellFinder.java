package org.danmayr.imagej.algorithm.pipelines;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.util.*;
import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.algorithm.filters.Filter;

import org.danmayr.imagej.algorithm.AnalyseSettings;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.shape_detector.*;


public class CellFinder extends Pipeline {

    static int MAX_THERSHOLD = 255;

    public CellFinder(AnalyseSettings settings, ChannelType ch0, ChannelType ch1) {
        super(settings, ch0, ch1);
    }

    @Override
    protected TreeMap<Integer, Channel> startPipeline(File img) {

        TreeMap<Integer, Channel> channels = new TreeMap<Integer, Channel>();
        RoiManager rm = new RoiManager();


        ImagePlus img0 = getImageCh0();
        ImagePlus img1 = getImageCh1();


        double[] in0 = new double[2];
        /*ImagePlus img0BeforeTh = preFilterSetColoc(img0, mSettings.ch0.enhanceContrast, mSettings.ch0.mThersholdMethod,
                mSettings.ch0.minThershold, mSettings.ch0.maxThershold, in0);*/

        /*Filter.AnalyzeParticles(img0,rm);
        Channel measCh0 = Filter.MeasureImage(0, "ch0", mSettings, img0BeforeTh, img0, rm);
        measCh0.setThershold(in0[0], in0[1]);
        channels.put(0, measCh0);*/


        Cell_Magic_Wand_Tool wandTool = new Cell_Magic_Wand_Tool();

        Vector<Pair<Integer,Integer>> result = Filter.FindMaxima(img0);

        //IJ.run(img0, "Find Maxima...", "prominence=200 exclude light output=List");
        //IJ.saveAs("Results", "/home/joachim/pCloudDrive/documents/uni salzburg/transfection efficiency/MS22 CD81 GFP/4h after transfection/Results.csv");


        for(int n = 0;n<result.size();n++){
            wandTool.addNewShape(img0,rm, result.get(n).getFirst(), result.get(n).getSecond());
            IJ.log(String.valueOf(result.get(n).getFirst()) + " | " + String.valueOf(result.get(n).getSecond()));
        }

        return channels;
    }

}

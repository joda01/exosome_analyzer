
package org.danmayr.imagej.algorithm.filters;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.plugin.Colors;
import ij.plugin.OverlayLabels;
import ij.plugin.FolderOpener;
import ij.util.*;
import ij.macro.*;
import ij.measure.*;
import ij.plugin.OverlayCommands;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JWindow;

import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.ParticleAnalyzer;

import org.danmayr.imagej.algorithm.structs.*;
import org.danmayr.imagej.performance_analyzer.PerformanceAnalyzer;
import org.danmayr.imagej.algorithm.statistics.*;
import org.danmayr.imagej.algorithm.AnalyseSettings;

public class Filter {
    static int RESULT_FILE_ROI_IDX = 0;
    static int RESULT_FILE_IDX_AREA_SIZE = 1;
    static int RESULT_FILE_IDX_MEAN_GRAYSCALE = 2;
    static int RESULT_FILE_IDX_CIRCULARITY = 5;

    public Filter() {

    }

    public static ImagePlus AddImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("Max create", ch0, ch1);
        return sumImage;
    }

    public static ImagePlus ANDImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("AND create", ch0, ch1);
        return sumImage;
    }

    public static ImagePlus XORImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("XOR create", ch0, ch1);
        return sumImage;
    }

    public static ImagePlus SubtractImages(ImagePlus ch0, ImagePlus ch1) {
        ImageCalculator ic = new ImageCalculator();
        ImagePlus sumImage = ic.run("Subtract create", ch0, ch1);
        return sumImage;
    }

    public static void Median(ImagePlus img) {
        IJ.run(img, "Median...", "radius=3");
    }

    public static void Watershed(ImagePlus img) {
        IJ.run(img, "Watershed", "");
    }

    public static void FillHoles(ImagePlus img) {
        IJ.run(img, "Fill Holes", "");
    }

    public static void Voronoi(ImagePlus img) {
        IJ.run(img, "Voronoi", "");
    }

    public static ImagePlus duplicateImage(ImagePlus img) {
        return img.duplicate();
    }

    public static void FindEdges(ImagePlus img) {
        IJ.run(img, "Find Edges", "");
    }

    public static void Smooth(ImagePlus img) {
        IJ.run(img, "Smooth", "");
    }

    public static void EnhanceContrast(ImagePlus img) {
        IJ.run(img, "Enhance Contrast...", "saturated=0.3 normalize");
    }

    public static void SubtractBackground(ImagePlus img) {
        IJ.run(img, "Subtract Background...", "rolling=4 sliding");
    }

    public static void ApplyGaus(ImagePlus img) {
        IJ.run(img, "Convolve...", "text1=[1 4 6 4 1\n4 16 24 16 4\n6 24 36 24 6\n4 16 24 16 4\n1 4 6 4 1] normalize");
    }

    public static void AddThersholdToROI(ImagePlus img, RoiManager rm) {
        ClearRois(img, rm);
        IJ.run(img, "Create Selection", "");
        rm.addRoi(img.getRoi());
    }

    public static void ApplyThershold(ImagePlus img, AutoThresholder.Method thersholdMethod) {
        ApplyThershold(img, thersholdMethod, -1, -1, null, true);
    }

    public static void ApplyThershold(ImagePlus img, AutoThresholder.Method thersholdMethod, double lowerThershold,
            double upperThershold, double[] thRet, boolean convertToMask) {

        ImageProcessor ip = img.getProcessor();

        ip.setRoi(img.getRoi());

        boolean darkBackground = true;
        int measurements = Analyzer.getMeasurements();
        Analyzer.setMeasurements(Measurements.AREA + Measurements.MIN_MAX);
        ImageStatistics stats = new StackStatistics(img);
        Analyzer.setMeasurements(measurements);
        AutoThresholder thresholder = new AutoThresholder();
        double min = 0.0, max = 255.0;
        if (img.getBitDepth() != 8) {
            min = stats.min;
            max = stats.max;
        }
        int threshold = thresholder.getThreshold(thersholdMethod, stats.histogram);

        double lower, upper;

        //
        // Take auto thershold
        //
        if (darkBackground) {
            if (ip.isInvertedLut()) {
                lower = 0.0;
                upper = threshold;
            } else {
                lower = threshold + 1;
                upper = 255.0;
            }
        } else {
            if (ip.isInvertedLut()) {
                lower = threshold + 1;
                upper = 255.0;
            } else {
                lower = 0.0;
                upper = threshold;
            }
        }
        if (lower > 255) {
            lower = 255;
        }
        if (max > min) {
            lower = min + (lower / 255.0) * (max - min);
            upper = min + (upper / 255.0) * (max - min);
        } else {
            lower = upper = min;
        }
        //
        // Take manual thershold
        //
        if (lowerThershold >= 0 && upperThershold >= 0) {
            lower = lowerThershold;
            upper = upperThershold;
        }

        if (thRet != null) {
            thRet[0] = lower;
            thRet[1] = upper;
        }
        ip.setMinAndMax(min, max);
        ip.setThreshold(lower, upper, ImageProcessor.RED_LUT);
        img.updateAndDraw();

        if (true == convertToMask) {
            ByteProcessor mask = img.createThresholdMask();
            mask = ip.createMask();
            img.setImage(new ImagePlus(img.getTitle(), mask));
        }

    }

    // public static void ApplyThershold(ImagePlus img, String thersholdMethod) {
    // IJ.setAutoThreshold(img, thersholdMethod + " dark");
    // Prefs.blackBackground = true;
    // IJ.run(img, "Convert to Mask", "");
    // }

    public static double[] getAutoThreshold(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        double max = ip.getMaxThreshold();
        double min = ip.getMinThreshold();
        double[] ret = { min, max };
        return ret;
    }

    //
    // Merge channels with clors
    // Each index coresponds to one color
    // {"red", "green", "blue", "gray", "cyan", "magenta", "yellow"};
    //
    public static ImagePlus MergeChannels(ImagePlus[] ary) {
        // RGBStackMerge rgb = new RGBStackMerge();
        ImagePlus mrg = RGBStackMerge.mergeChannels(ary, true);
        return mrg;
    }

    public static void showNoRoi(ImagePlus image, RoiManager rm) {
        rm.runCommand(image, "Show All without labels");
        rm.runCommand(image, "Show None");
        IJ.run(image, "Select None", "");
    }

    public static void SaveImage(ImagePlus image, String imageName, RoiManager rm) {
        IJ.saveAs(image, "Jpeg", imageName);
    }

    public static void SaveImageWithOverlay(ImagePlus image, RoiManager rm, String imageName) {
        rm.runCommand(image, "Show All without labels");
        // rm.runCommand("Set Color", "red");
        // aintRoiLabels(image, rm);
        // IJ.run(image,rescource, "font=SanSerif label=red label_0=14 additional=none
        // label_1=right");
        ImagePlus overlayimage = image.flatten();
        IJ.saveAs(overlayimage, "Jpeg", imageName);
        rm.runCommand(image, "Show None");
    }

    private static void PaintRoiLabels(ImagePlus image, RoiManager rm) {

        Overlay ov = new Overlay();

        int fontSize = 12;

        Font font = new Font("SansSerif", Font.PLAIN, fontSize);

        Roi[] rois = rm.getRoisAsArray();
        for (int n = 0; n < rois.length; n++) {
            Rectangle rec = rois[n].getBounds();

            double p;
            if (fontSize < 16) {
                p = 10;
            } else if (fontSize < 24) {
                p = 12;
            } else {
                p = 20;
            }

            double x1 = rec.getX() + rec.getWidth() + 5;
            double y1 = rec.getY() + 0.5 * rec.getHeight() + p;

            TextRoi lbl = new TextRoi(x1, y1, Integer.toString(n + 1), font);
            lbl.setStrokeColor(Color.red);
            lbl.setFillColor(Color.black);
            ov.add(lbl);
        }

        image.setOverlay(ov);

    }

    public static void InvertImage(ImagePlus image) {
        IJ.run(image, "Invert", "");
    }

    public static void ClearRois(ImagePlus image, RoiManager rm) {
        rm.runCommand(image, "Show None");
        image.setRoi(new OvalRoi(1, 1, 1, 1));
        rm.addRoi(image.getRoi());
        rm.runCommand(image, "Delete");
        IJ.run(image, "Select None", "");
    }

    public static void SetRoiInImage(ImagePlus image, RoiManager rm, int idx) {
        if (idx < rm.getCount()) {
            image.setRoi(rm.getRoi(idx));
            image.getProcessor().setRoi(rm.getRoi(idx));
        }
    }

    public static void ClearRoiInImage(ImagePlus image) {
        image.deleteRoi();
        image.getProcessor().setRoi((Roi) null);
    }

    public static ImagePlus AnalyzeParticles(ImagePlus image, RoiManager rm, double minSize, double maxSize,
            double minCircularity) {
        return AnalyzeParticles(image, rm, minSize, maxSize, minCircularity, true, null);
    }

    public static ImagePlus AnalyzeParticlesDoNotAdd(ImagePlus image, RoiManager rm, double minSize, double maxSize,
            double minCircularity, ResultsTable rt) {
        return AnalyzeParticles(image, rm, minSize, maxSize, minCircularity, false, rt);
    }

    public static ImagePlus AnalyzeParticles(ImagePlus image, RoiManager rm, double minSize, double maxSize,
            double minCircularity, boolean addToRoi) {
        return AnalyzeParticles(image, rm, minSize, maxSize, minCircularity, addToRoi, null);
    }

    public static ImagePlus AnalyzeParticles(ImagePlus image, RoiManager rm, double minSize, double maxSize,
            double minCircularity, boolean addToRoi, ResultsTable rt) {

        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/ParticleAnalyzer.html
        // ParticleAnalyzer analyzer
        // Analyzer
        // int options, int measurements, ResultsTable rt, double minSize, double
        // maxSize, double minCirc, double maxCirc
        int option = ParticleAnalyzer.SHOW_MASKS;
        if (true == addToRoi) {
            Filter.ClearRois(image, rm);
            option |= ParticleAnalyzer.ADD_TO_MANAGER;
        } else {
            option &= ~ParticleAnalyzer.ADD_TO_MANAGER;
        }
        if (maxSize < 0) {
            maxSize = 999999;
        }

        /*
         * public static final int AREA=1,MEAN=2,STD_DEV=4,MODE=8,MIN_MAX=16,
         * CENTROID=32,CENTER_OF_MASS=64,PERIMETER=128, LIMIT=256, RECT=512,
         * LABELS=1024,ELLIPSE=2048,INVERT_Y=4096,CIRCULARITY=8192,
         * SHAPE_DESCRIPTORS=8192,FERET=16384,INTEGRATED_DENSITY=0x8000, MEDIAN=0x10000,
         * SKEWNESS=0x20000, KURTOSIS=0x40000, AREA_FRACTION=0x80000, SLICE=0x100000,
         * STACK_POSITION=0x100000, SCIENTIFIC_NOTATION=0x200000,
         * ADD_TO_OVERLAY=0x400000, NaN_EMPTY_CELLS=0x800000;
         * 
         * public static final int ALL_STATS = AREA+MEAN+STD_DEV+MODE+MIN_MAX+
         * CENTROID+CENTER_OF_MASS+PERIMETER+RECT+
         * ELLIPSE+SHAPE_DESCRIPTORS+FERET+INTEGRATED_DENSITY+
         * MEDIAN+SKEWNESS+KURTOSIS+AREA_FRACTION;
         */

        int measurements = Measurements.AREA | Measurements.MEAN | Measurements.MIN_MAX
                | Measurements.SHAPE_DESCRIPTORS;
        ParticleAnalyzer analyzer = new ParticleAnalyzer(option, measurements, rt, minSize, maxSize, minCircularity,
                1.0);
        if (true == addToRoi) {
            ParticleAnalyzer.setRoiManager(rm);
        } else {
            ParticleAnalyzer.setRoiManager(null);
        }
        analyzer.setHideOutputImage(true);
        analyzer.analyze(image, image.getProcessor());
        ImagePlus mask = analyzer.getOutputImage();
        Filter.InvertImage(mask);
        Filter.ApplyThershold(mask, AutoThresholder.Method.Default);
        return mask;

    }

    public static void RoiSave(ImagePlus image, RoiManager rm) {
        File roizip = new File("roiset.zip");
        roizip.delete();
        rm.runCommand("save", "roiset.zip");
    }

    public static void RoiOpen(ImagePlus image, RoiManager rm) {
        ClearRois(image, rm);
        rm.runCommand("open", "roiset.zip");
    }

    ///
    /// Execute analyze particles before
    ///
    public static Channel MeasureImage(String channelName, AnalyseSettings settings, ImagePlus imageOrigial,
            ImagePlus imageThershold, RoiManager rm) {
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/frame/RoiManager.html
        // multiMeasure(ImagePlus imp)
        // import ij.plugin.frame.RoiManager
        // https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
        // ij.measure.ResultsTable

        ResultsTable r1 = measure(imageOrigial, rm);
        ResultsTable r2 = measure(imageThershold, rm);
        Channel ch = createChannelFromMeasurement(channelName, settings, r1, r2);
        return ch;
    }

    private static ResultsTable measure(ImagePlus image, RoiManager rm) {
        ResultsTable rt = new ResultsTable();
        int measurements = Measurements.AREA | Measurements.MEAN | Measurements.MIN_MAX
                | Measurements.SHAPE_DESCRIPTORS;
        Analyzer analyzer = new Analyzer(image, measurements, rt);

        for (int n = 0; n < rm.getCount(); n++) {
            SetRoiInImage(image, rm, n);
            analyzer.measure();
        }
        return rt;
    }

    public static Channel createChannelFromMeasurement(String channelName, AnalyseSettings settings,
            ResultsTable imgOriginal, ResultsTable imgThershold) {

        int area = imgThershold.getColumnIndex("Area");
        int mean = imgThershold.getColumnIndex("Mean");
        int circ = imgThershold.getColumnIndex("Circ.");

        Channel ch = new Channel(channelName, new Statistics());

        // First line is header therefore start with 1
        for (int i = 0; i < imgOriginal.size(); i++) {

            double areaSize = imgOriginal.getValueAsDouble(area, i);
            double grayScale = imgOriginal.getValueAsDouble(mean, i);
            double thersholdScale = imgThershold.getValueAsDouble(mean, i);
            double circularity = imgOriginal.getValueAsDouble(circ, i);
            int roiNr = i;

            ParticleInfo exosom = new ParticleInfo(roiNr, areaSize, grayScale, thersholdScale, circularity);
            if (null != settings) {
                exosom.validatearticle(settings.mMinParticleSize, settings.mMaxParticleSize, settings.mMinCircularity,
                        settings.minIntensity);
            }
            ch.addRoi(exosom);
        }

        ch.calcStatistics();

        return ch;
    }

}

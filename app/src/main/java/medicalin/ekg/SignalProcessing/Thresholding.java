package medicalin.ekg.SignalProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Thresholding {
    List<Double> mwT = new ArrayList<Double>();
    double max_h;
    double avr;
    double thr;
    public Thresholding(List<Double> mwVal){
        //Calculate the Threshold, Thr = Moving Window Integration (mwi) / absolute mean of mwi
        List<Double> mwAbs = new ArrayList<Double>();
        for(int i = 0; i<mwVal.size(); i++) mwAbs.add(Math.abs(mwVal.get(i)));
        double maxMW = getMax(mwAbs);
        for(int i = 0; i<mwAbs.size(); i++) mwT.add((double)mwVal.get(i)/(double)maxMW);

        max_h = Collections.max(mwT);
        avr = calculateAverage(mwT);
        thr = max_h*avr;

        System.out.println("MaxH: "+max_h+" AVR: "+avr+" Thr: "+thr);
    }

    //Method to calculate the average or mean of absolute Moving Window Integration output
    private static double calculateAverage(List<Double> marks) {
        double sum = 0;
        if(!marks.isEmpty()) {
            for (Double mark : marks) sum += mark;
            return sum / marks.size();
        }
        return sum;
    }

    // Method for getting the maximum value
    public static double getMax(List<Double> inputArray){
        double maxValue = inputArray.get(0);
        for(int i=1;i < inputArray.size();i++){
            if(inputArray.get(i) > maxValue){
                maxValue = inputArray.get(i);
            }
        }
        return maxValue;
    }

    public List<Double> getThresholdMW(){
        return mwT;
    }

    public double getThreshold(){
        return thr;
    }
}

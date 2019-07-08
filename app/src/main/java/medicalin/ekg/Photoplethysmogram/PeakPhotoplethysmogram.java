package medicalin.ekg.Photoplethysmogram;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import medicalin.ekg.SignalProcessing.Detrend;

public class PeakPhotoplethysmogram {
    private MovingAverage ma;
    private List<Double> mwVal;
    private Detrend dt;
    private List<Double> dPPG;
    private RemoveConsecutiveR removeConsecutiveR;
    private List<Integer> ann;
    private double rrAvr,hr;
    private double lastRR, lastHR;

    public PeakPhotoplethysmogram (List<Integer> ppg, List<Double> time) {
        ann = new ArrayList<Integer>();

        for(int i = 0; i < ppg.size();i++){
            ann.add(0);
        }

        dt = new Detrend(ppg);
        dPPG = new ArrayList<Double>();
        dPPG = dt.getDetrend();

        ma = new MovingAverage();
        mwVal = new ArrayList<Double>();

        double mwip;

        for(int i = 0; i < dPPG.size();i++){
            mwip = ma.calculate(dPPG.get(i));
            mwVal.add(mwip);
        }

        cancelDelay(mwVal,15);

        double thr = 0.3*getMaxValue(mwVal);

        List<Integer> ir = new ArrayList<Integer>();
        for(int i =0; i<mwVal.size();i++){
            if(mwVal.get(i) > thr) ir.add(i);
        }

        removeConsecutiveR = new RemoveConsecutiveR(ppg,ir);

        ir = removeConsecutiveR.getIrs();

        for(int i = 0; i < ir.size(); i++){
            ann.set(ir.get(i),1);
            Log.d("Peak PPG ",String.valueOf(ir.get(i)));
        }

        double rrSum = 0.000;
        int rrDiv = 0;
        for(int i = 1;i<ir.size()-1;i++){
            double rrInt = time.get(ir.get(i)) - time.get(ir.get(i-1));
            if(rrInt > 0.400 && rrInt < 2.000){
                rrSum += rrInt;
                rrDiv += 1;
            }
        }

        if(rrDiv > 0){
            rrAvr = rrSum/rrDiv;
            hr = 60.000/rrAvr;
            Log.d("HR PPG ",String.valueOf(hr));
            lastRR = rrAvr;
            lastHR = hr;
        }else{
            rrAvr = lastRR;
            hr = lastHR;
        }

    }

    private static void cancelDelay(List<Double> arrayList, int numberDelay){
        for(int i = 0; i<arrayList.size()-numberDelay;i++){
            arrayList.set(i, arrayList.get(i + numberDelay));
        }
    }

    private static double getMaxValue(List<Double> numbers){
        double maxValue = numbers.get(0);
        for(int i=1;i < numbers.size();i++){
            if(numbers.get(i) > maxValue){
                maxValue = numbers.get(i);
            }
        }
        return maxValue;
    }

    public List<Integer> getAnnotation(){
        return ann;
    }
    public double getRrAvr(){
        return rrAvr;
    }

    public double getHr(){
        return hr;
    }
}

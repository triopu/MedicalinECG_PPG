package medicalin.ekg;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import medicalin.ekg.SignalProcessing.*;

public class TEnd {

    List<Integer> mECG      = new ArrayList<Integer>();
    List<Integer> dataECG   = new ArrayList<Integer>();
    List<Double> dataTime   = new ArrayList<Double>();

    private LowPassFilter lp;
    private Derivative dr;
    private Squaring sq;
    private MovingWindowIntegrationT mw;
    private Detrend dt;

    List<Integer> leftBoundT = new ArrayList<Integer>();
    List<Integer> rightBoundT = new ArrayList<Integer>();
    List<Integer> posRegT = new ArrayList<Integer>();
    List<Integer> endT = new ArrayList<Integer>();

    ArrayList<Integer> processedECGData = new ArrayList<Integer>();
    ArrayList<Double> processedECGTime = new ArrayList<Double>();

    ArrayList<Integer> processedData = new ArrayList<Integer>();
    ArrayList<Double> processedTime = new ArrayList<Double>();

    public TEnd(List<Integer> data, List<Double> time, List<Integer> listOfPeak,  List<Integer> rightBound){
        Log.d("List of Peak is",String.valueOf(listOfPeak));

        for(int i =0; i<data.size();i++){
            mECG.add(data.get(i));
            dataECG.add(data.get(i));
            dataTime.add(time.get(i));
        }

        //Removing P and QRS
        int midWave = 0;
        for(int i = 0; i < listOfPeak.size()-1;i++){
            midWave = (listOfPeak.get(i+1)+listOfPeak.get(i))/2;
            int leftRem = listOfPeak.get(i+1)-midWave;
            leftRem = listOfPeak.get(i) - leftRem;
            if(leftRem < 0) leftRem = 0;
            if(midWave > data.size()) midWave = data.size()-1;
            while (true){
                mECG.set(leftRem,data.get(midWave));
                leftRem += 1;
                if(leftRem>rightBound.get(i)) break;
            }
        }

        if (midWave == 0){
            return;
        }

        Log.d("Length of Midwave is ",String.valueOf(midWave));

        //Used Data is last peak - 1
        List<Integer> ecgDataR = new ArrayList<Integer>();
        processedData = new ArrayList<Integer>();
        processedTime = new ArrayList<Double>();
        for(int i = 0; i < midWave; i++){
            ecgDataR.add(mECG.get(i));
            processedData.add(data.get(i));
            processedTime.add(time.get(i));
        }

        /*
        //Save unused Data, the leftover of previous process
        int unused = midWave;
        processedECGData = new ArrayList<Integer>();
        processedECGTime = new ArrayList<Double>();
        boolean unn = true;
        while (unn){
            if(unused > dataECG.size()-2) unn = false;
            processedECGData.add(dataECG.get(unused));
            processedECGTime.add(dataTime.get(unused));
            unused += 1;
        }
        */


        dt = new Detrend(data);
        lp = new LowPassFilter(300);
        dr = new Derivative();
        sq = new Squaring();
        mw = new MovingWindowIntegrationT();

        List<Integer> lpValT = new ArrayList<Integer>();
        List<Integer> drValT = new ArrayList<Integer>();
        List<Integer> sqValT = new ArrayList<Integer>();
        List<Double> mwValT  = new ArrayList<Double>();

        //ECG data is passed through LPF, Derivative, Squaring, and mwi
        int lpf, drv, sqr;
        double mwi;
        for (int aData : ecgDataR) {
            lpf = (int) lp.filter(aData);       lpValT.add(lpf);
            drv = dr.derive(lpf);               drValT.add(drv);
            sqr = sq.square(drv);               sqValT.add(sqr);
            mwi = mw.calculate(sqr);            mwValT.add(mwi);
        }

        cancelDelay(lpValT,6);
        cancelDelay(drValT, 2);
        cancelDelayMW(mwValT);

        //Find the Threshold for T-end detection
        Thresholding thresholding = new Thresholding(mwValT);
        List<Double> mwTT = thresholding.getThresholdMW();
        double thrT = thresholding.getThreshold();
        double initThr = thrT;

        double alpha = 1;

        //T-end is detected between 2 consecutive R-peak
        //If T-end is not detected, multiply the Threshold by alpha, until alpha = 0.1
        //Give maximum iteration by 10
        for(int i = 0; i < listOfPeak.size()-1;i++){
            boolean k = true;
            int iter = 0;
            int indT = 0;
            while (k){
                posRegT = new ArrayList<Integer>();

                //Start od searching is 1st R-peak, End of searching is 2nd peak
                //If the 2nd peak is last peak + 1, End of searching is midWave, look at used data
                int searchEnd;
                if(i == (listOfPeak.size()-2)){
                    searchEnd = midWave;
                }
                else{
                    searchEnd = listOfPeak.get(i+1);
                }

                //Converting to 0 and 1 to find the right bound
                for (int m = listOfPeak.get(i); m < searchEnd-1; m++) {
                    if (mwTT.get(m) > thrT) posRegT.add(1);
                    else posRegT.add(0);
                }

                //Find the right bound
                leftBoundT = new ArrayList<Integer>();
                rightBoundT = new ArrayList<Integer>();
                int n;
                for (n = 0; n < posRegT.size() - 1; n++) {
                    if (posRegT.get(n + 1) - posRegT.get(n) == 1) leftBoundT.add(n);
                    else if (posRegT.get(n + 1) - posRegT.get(n) == -1) rightBoundT.add(n);
                    if (rightBoundT.size() > 0 && leftBoundT.size() > 0) {
                        //If the first data is found to be right bound, we have to remove it
                        if (rightBoundT.get(0) < leftBoundT.get(0)) {
                            rightBoundT.remove(0);
                        }
                    }
                }

                //Iteration is add by 1
                iter += 1;

                //If one right bound is found, return the index of T as the rightBoundT.get(0)
                //Stop the iteration by switch k to false
                if(rightBoundT.size() == 1){
                    indT = rightBoundT.get(0);
                    k = false;
                }

                //If the right bound is not found, reduce the Threshold
                if(rightBoundT.size()<1){
                    alpha = alpha - 0.01;
                    thrT = initThr * (alpha);
                }

                //If more than one right bound are found,
                //find the most right position
                if(rightBoundT.size()>1){
                    int peakRef = 125;
                    int minVal = Math.abs(rightBoundT.get(0)- peakRef); // Keeps a running count of the smallest value so far
                    int minIdx = 0; // Will store the index of minVal
                    for(int idx=1; idx < rightBoundT.size(); idx++) {
                        if(Math.abs(rightBoundT.get(idx)- peakRef) < minVal) {
                            minVal = Math.abs(rightBoundT.get(idx)- peakRef);
                            minIdx = idx;
                        }
                    }
                    indT = rightBoundT.get(minIdx);
                    k = false;
                }

                //If iteration is more than 10 times
                //If the second bound is Start of Search, T-end is midWave
                //Else it's the avearage of two consecutive R-Peak
                if(iter > 50){
                    k = false;
                    if(i == listOfPeak.size()) indT = midWave;
                    else indT = ((listOfPeak.get(i+1)+listOfPeak.get(i))/2)-listOfPeak.get(i);
                }
            }
            //Collect the T-end
            indT = indT + listOfPeak.get(i);
            endT.add(indT);
        }
    }

    private void cancelDelay(List<Integer> arrayList, int numberDelay){
        for(int i = 0; i<arrayList.size()-numberDelay;i++){
            arrayList.set(i, arrayList.get(i + numberDelay));
        }
    }

    private void cancelDelayMW(List<Double> arrayList){
        for(int i = 0; i<arrayList.size()- 20; i++){
            arrayList.set(i, arrayList.get(i + 20));
        }
    }

    public List<Integer> getTEnd(){
        return endT;
    }
}

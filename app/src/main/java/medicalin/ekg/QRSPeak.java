package medicalin.ekg;

import java.util.ArrayList;
import java.util.List;

import medicalin.ekg.SignalProcessing.Derivative;
import medicalin.ekg.SignalProcessing.Detrend;
import medicalin.ekg.SignalProcessing.HighPassFilter;
import medicalin.ekg.SignalProcessing.LowPassFilter;
import medicalin.ekg.SignalProcessing.MovingWindowIntegration;
import medicalin.ekg.SignalProcessing.PeakDetection;
import medicalin.ekg.SignalProcessing.Squaring;
import medicalin.ekg.SignalProcessing.Thresholding;

public class QRSPeak {

    private LowPassFilter lp;
    private HighPassFilter hp;
    private Derivative dr;
    private Squaring sq;
    private MovingWindowIntegration mw;
    private Detrend dt;

    private List<Integer> listOfPeak    = new ArrayList<Integer>();
    private List<Integer> leftBound     = new ArrayList<Integer>();
    private List<Integer> rightBound    = new ArrayList<Integer>();


    List<Integer> lpVal;
    List<Integer> hpVal;
    List<Integer> drVal;
    List<Integer> sqVal;
    List<Double> mwVal;
    List<Integer> dataECG;
    List<Double> dataTime;
    List<Integer> annECG;
    List<Double> dECG;

    public QRSPeak(List<Double> time, List<Integer> data, int dataCount) {
        //Pan-Tompkins Section
        int lpf, hpf, drv, sqr;
        double mwi;

        dt = new Detrend(data);
        lp = new LowPassFilter(300);
        hp = new HighPassFilter();
        dr = new Derivative();
        sq = new Squaring();
        mw = new MovingWindowIntegration();

        lpVal = new ArrayList<Integer>();
        hpVal = new ArrayList<Integer>();
        drVal = new ArrayList<Integer>();
        sqVal = new ArrayList<Integer>();
        mwVal = new ArrayList<Double>();
        dataECG = new ArrayList<Integer>();
        dataTime = new ArrayList<Double>();
        annECG = new ArrayList<Integer>();
        dECG = new ArrayList<Double>();
        dECG = dt.getDetrend();

        for (int i = 0; i < dataCount; i++) {
            annECG.add(0);
            dataECG.add(data.get(i));
            dataTime.add(time.get(i));
            lpf = (int) lp.filter(dECG.get(i).intValue()); lpVal.add(lpf);
            hpf = (int) hp.filter(lpf); hpVal.add(hpf);
            drv = dr.derive(hpf); drVal.add(drv);
            sqr = sq.square(drv); sqVal.add(sqr);
            mwi = mw.calculate(sqr); mwVal.add(mwi);
        }

        // Removing the delay of Pan-Tompkins
        cancelDelay(lpVal,6);
        cancelDelay(hpVal,16);
        cancelDelay(drVal, 2);
        cancelDelayMW(mwVal);

        Thresholding thresholding = new Thresholding(mwVal);
        List<Double> mwT = thresholding.getThresholdMW();
        double thr = thresholding.getThreshold();

        PeakDetection peakDetection = new PeakDetection(mwT,thr,dataECG, dataTime);
        listOfPeak = peakDetection.getPeakIndex();
        leftBound  = peakDetection.getLeftBound();
        rightBound  = peakDetection.getRightBound();
    }

    //Method to cancel delay
    private void cancelDelay(List<Integer> arrayList, int numberDelay){
        for(int i = 0; i<arrayList.size()-numberDelay;i++){
            arrayList.set(i, arrayList.get(i + numberDelay));
        }
    }

    private void cancelDelayMW(List<Double> arrayList){
        for(int i = 0; i<arrayList.size()- 30; i++){
            arrayList.set(i, arrayList.get(i + 30));
        }
    }

    public List<Integer> getPeak(){
        return listOfPeak;
    }

    public List<Integer> getLP(){
        return lpVal;
    }

    public List<Integer> getHP(){
        return hpVal;
    }

    public List<Integer> getDR(){
        return drVal;
    }

    public List<Integer> getSQ(){
        return sqVal;
    }

    public List<Double> getMW(){
        return mwVal;
    }

    public List<Integer> getLeftBound(){
        return leftBound;
    }

    public List<Integer> getRightBound(){
        return rightBound;
    }

}

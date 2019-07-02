package medicalin.ekg;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CalculateQT {
    private double qtAvr;
    private double rrAvr,hr;
    private double lastRR, lastHR, lastQT;
    List<Integer> annECG = new ArrayList<Integer>();
    List<Integer> processedAnn = new ArrayList<Integer>();

    ArrayList<Integer> processedECGData = new ArrayList<Integer>();
    ArrayList<Double> processedECGTime = new ArrayList<Double>();

    ArrayList<Integer> processedData = new ArrayList<Integer>();
    ArrayList<Double> processedTime = new ArrayList<Double>();

    public CalculateQT(List<Integer> data, List<Double> time, List<Integer> peakIndex, List<Integer> qrsOnset, List<Integer> endT){
        //Calculate RR and HR

        if(endT.size() > 1){

            annECG = new ArrayList<Integer>();
            Log.d("Annotation ","Q: "+String.valueOf(qrsOnset)+" Length: "+String.valueOf(qrsOnset.size()));
            Log.d("Annotation ","R: "+String.valueOf(peakIndex)+" Length: "+String.valueOf(peakIndex.size()));
            Log.d("Annotation ","T: "+String.valueOf(endT)+" Length: "+String.valueOf(endT.size()));


            for(int i = 0; i<data.size();i++){
                annECG.add(0);
            }
            double rrSum = 0.000;
            int rrDiv = 0;
            for(int i = 1;i<peakIndex.size()-1;i++){
                double rrInt = time.get(peakIndex.get(i)) - time.get(peakIndex.get(i-1));
                if(rrInt > 0.400 && rrInt < 2.000){
                    rrSum += rrInt;
                    rrDiv += 1;
                }
            }

            for (int i = 0; i<endT.size();i++){
                annECG.set(endT.get(i),3);
            }

            for (int i = 0; i<peakIndex.size(); i++){
                annECG.set(peakIndex.get(i),2);
            }

            for (int i = 0; i<qrsOnset.size(); i++){
                if(qrsOnset.get(i) > 4) annECG.set(qrsOnset.get(i)-4,1);
                else annECG.set(qrsOnset.get(i),1);
            }

            if(rrDiv > 0){
                rrAvr = rrSum/rrDiv;
                hr = 60.000/rrAvr;
                lastRR = rrAvr;
                lastHR = hr;
            }else{
                rrAvr = lastRR;
                hr = lastHR;
            }

            //Calculate QT
            double qtSum = 0.000;
            int qtDiv = 0;
            for(int i = 0;i<endT.size();i++){
                double qtInt = time.get(endT.get(i)) - time.get(qrsOnset.get(i));
                if(qtInt > 0.100 && qtInt < 2.000){
                    qtSum += qtInt;
                    qtDiv += 1;
                }
            }

            if(qtDiv > 0){
                qtAvr = qtSum/qtDiv;
                lastQT = qtAvr;
            }else{
                qtAvr = lastQT;
            }

            int limit = endT.get(endT.size()-1) + 10;


            //Used Data is last peak - 1
            processedData = new ArrayList<Integer>();
            processedTime = new ArrayList<Double>();
            processedAnn = new ArrayList<Integer>();
            for(int i = 0; i < limit; i++){
                processedData.add(data.get(i));
                processedTime.add(time.get(i));
                processedAnn.add(annECG.get(i));
            }

            //Save unused Data, the leftover of previous process
            processedECGData = new ArrayList<Integer>();
            processedECGTime = new ArrayList<Double>();

            for(int i = limit; i < data.size(); i++){
                processedECGData.add(data.get(i));
                processedECGTime.add(time.get(i));
            }

            Log.d("Check Data",String.valueOf(data.size())+" U: "+String.valueOf(processedECGData.size())+" L: "+String.valueOf(limit));
        }
    }

    public double getRrAvr(){
        return rrAvr;
    }

    public double getHr(){
        return hr;
    }

    public double getQtAvr(){
        return qtAvr;
    }

    public List<Integer> getAnnECG(){
        return processedAnn;
    }

    public ArrayList<Integer> getUnusedData(){
        return processedECGData;
    }

    public ArrayList<Double> getUnusedTime(){
        return processedECGTime;
    }

    public ArrayList<Integer> getUsedData(){
        return processedData;
    }

    public ArrayList<Double> getUsedTime(){
        return processedTime;
    }
}

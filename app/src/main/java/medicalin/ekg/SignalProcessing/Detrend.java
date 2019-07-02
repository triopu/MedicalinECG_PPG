package medicalin.ekg.SignalProcessing;

import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import medicalin.ekg.SignalProcessing.Polynomial.Polyfit;
import medicalin.ekg.SignalProcessing.Polynomial.Polyval;

public class Detrend {
    static Polyfit polyfit = null;
    static Polyval polyval;
    static double[] sECG;
    private List<Double> dECG;
    private List<Double> detECG;

    public Detrend(List<Integer> d) {
        double[] dd = new double[d.size()];
        for (int i = 0; i < dd.length-1; i++) {
            if(i>dd.length-1) break;
            dd[i] = d.get(i);
        }

        double[] x = new double[dd.length];
        int c = 0;
        for (int i = 0; i < x.length -1; i++) {
            x[i] = c;
            c = c + 1;
        }

        try {
            polyfit = new Polyfit(x, dd, 6);
            polyval = new Polyval(x, polyfit);
        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] xx = new double[polyval.getYout().length];

        for (int i = 0; i <= polyval.getYout().length - 1; i++) {
            BigDecimal bd = new BigDecimal(polyval.getYout()[i]).setScale(2, BigDecimal.ROUND_HALF_UP);
            xx[i] = bd.doubleValue();
        }

        sECG = subtract(dd, xx);

        double minDECG = getMin(sECG);
        double maxDECG = getMax(sECG);

        double minData = getMin(dd);
        double maxData = getMax(dd);

        Log.d("MinMax Value ",String.valueOf(minDECG)+"/"+String.valueOf(maxDECG)+"/"+ String.valueOf(minData)+"/"+String.valueOf(maxData));


        detECG = new ArrayList<Double>();
        for (double v : sECG) {
            detECG.add(map(v, minDECG, maxDECG, minData, maxData));
        }
    }

    private double[] subtract(double[] first, double[] second) {
        int length = first.length < second.length ? first.length : second.length;
        double[] result = new double[length];
        for (int i = 0; i < length-1; i++) {
            result[i] = first[i] - second[i];
        }
        return result;
    }

    public List<Double> getDetrend(){
        return detECG;
    }

    private double map(double x, double in_min, double in_max, double out_min, double out_max) {
        //Log.d("Mapping Value",String.valueOf((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min));
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    // Method for getting the maximum value
    public static double getMax(double[] inputArray){
        double maxValue = inputArray[0];
        for(int i=0;i < inputArray.length;i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    // Method for getting the minimum value
    public static double getMin(double[] inputArray){
        double minValue = inputArray[0];
        for(int i=0;i<inputArray.length;i++){
            if(inputArray[i] < minValue){
                minValue = inputArray[i];
            }
        }
        return minValue;
    }
}

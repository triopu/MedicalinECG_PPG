package medicalin.ekg.SignalProcessing;

import java.util.ArrayList;
import java.util.List;

public class PeakDetection {

    List<Integer> leftBound = new ArrayList<Integer>();
    List<Integer> rightBound = new ArrayList<Integer>();
    List<Integer> peakIndex = new ArrayList<Integer>();

    public PeakDetection (List<Double> mwT, double thr, List<Integer> dt, List<Double> dataTime){
        //Convert the data into 1 and 0 to find the left and right bound
        List<Integer> posReg = new ArrayList<Integer>();
        for (int i = 0; i<mwT.size();i++)if(mwT.get(i)>thr)posReg.add(1);else posReg.add(0);
        int peakB = 0;
        //Find the right and left bound
        for (int i = 0; i<posReg.size()-1;i++) {
            if (posReg.get(i + 1) - posReg.get(i) == 1) leftBound.add(i);
            else if (posReg.get(i + 1) - posReg.get(i) == -1) rightBound.add(i);

            //If the first data is found to be right bound, we have to remove it
            if (rightBound.size() > 0 && leftBound.size() > 0) {
                //System.out.println("Condition 1");
                if (rightBound.get(0) < leftBound.get(0)) rightBound.remove(0);
            }

            //Remove the first right bound if two right bound's distance is less than 100 point data
            if (rightBound.size() > 1) {
                if (rightBound.get(rightBound.size() - 1) - rightBound.get(rightBound.size() - 2) < 100) {
                    rightBound.remove(rightBound.size() - 2);
                }
            }

            //Remove the first left bound if two left bound's distance is less than 100 point data
            if (leftBound.size() > 1) {
                if (leftBound.get(leftBound.size() - 1) - leftBound.get(leftBound.size() - 2) < 100) {
                    leftBound.remove(leftBound.size() - 2);
                }
            }

            //Left(i+1) is always more than right(i)
            if (leftBound.size() > 1) {
                if (dataTime.get(leftBound.size()-1) < dataTime.get(leftBound.size()-2)) {
                    leftBound.remove(leftBound.size() - 1);
                }
            }
        }

        if(leftBound.size() > 0){
            int boundSize = 0;
            if(rightBound.size() > leftBound.size()) boundSize = leftBound.size();
            else boundSize = rightBound.size();
            for(int i=0; i<boundSize;i++) {
                if( i <= (leftBound.size() -1) && i <= (rightBound.size() - 1)) {
                    int peak = findPeak(dt, leftBound.get(i), rightBound.get(i));
                    //If index of peak is 0, its mean the right and left bound is too close
                    if (peak == 0) {
                        System.out.println("PD QRSOnset Zero Peak " + String.valueOf(leftBound.get(leftBound.size() - 1)) + " " + String.valueOf(rightBound.get(rightBound.size() - 1)));
                        if (leftBound.get(leftBound.size() - 1) > rightBound.get(rightBound.size() - 1)) {
                            rightBound.remove(rightBound.size() - 1);
                        } else {
                            leftBound.remove(leftBound.size() - 1);
                            rightBound.remove(rightBound.size() - 1);
                        }
                    } else {
                        if (peak != peakB) {
                            peakIndex.add(peak);
                            peakB = peak;
                        }
                    }
                }
            }
        }
    }

    //Method to find R-peak
    private int findPeak(List<Integer> theData, int minBound, int maxBound){
        double peak = 0;
        int k = 0;
        for(int i = minBound;i < maxBound; i++){
            if(theData.get(i) >= peak){
                peak = theData.get(i);
                k = i+1;
            }
        }
        if(k != 0) return k-1;
        else return k;
    }

    public List<Integer> getLeftBound(){
        return leftBound;
    }

    public List<Integer> getRightBound(){
        return rightBound;
    }

    public List<Integer> getPeakIndex(){
        return peakIndex;
    }
}

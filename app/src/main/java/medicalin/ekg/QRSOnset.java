package medicalin.ekg;

import java.util.ArrayList;
import java.util.List;

public class QRSOnset {
    List<Integer> qrsOnset = new ArrayList<Integer>();
    public QRSOnset(List<Integer> hpVal, List<Integer> leftBound, List<Integer> peakIndex){
        for(int i = 0; i<peakIndex.size();i++){
            int qrsOn = findBase(hpVal,leftBound.get(i), peakIndex.get(i));
            qrsOnset.add(qrsOn);
        }
    }

    private int findBase(List<Integer> theData, int minBound, int maxBound) {
        int base = 1000;
        int k = 0;
        for (int i = minBound; i < maxBound; i++) {
            if (theData.get(i) < base) {
                base = theData.get(i);
                k = i + 1;
            }
        }
        if (k == 0) return minBound;
        return k;
    }

    public List<Integer> getQRSOnset(){
        return qrsOnset;
    }
}

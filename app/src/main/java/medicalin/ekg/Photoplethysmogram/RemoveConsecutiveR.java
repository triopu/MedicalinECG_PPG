package medicalin.ekg.Photoplethysmogram;

import java.util.ArrayList;
import java.util.List;

public class RemoveConsecutiveR {
    List<Integer> irs;
    public RemoveConsecutiveR(List<Integer> ecg, List<Integer> ir){
        int irLength = ir.size();
        irs = new ArrayList<Integer>();
        if (irLength < 2){
            irs = ir;
        }else{
            int i = 1;
            while(true){
                if(i>irLength - 1) break;
                if(ir.get(i) - ir.get(i-1) < 100){
                    irLength = irLength-1;
                    if(ecg.get(ir.get(i))>ecg.get(ir.get(i-1))){
                        ir.remove(i-1);
                    }else{
                        ir.remove(i);
                    }
                }else{
                    i = i+1;
                }
            }
            irs = ir;
        }
    }

    public List<Integer> getIrs(){
        return irs;
    }
}

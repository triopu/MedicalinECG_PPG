package medicalin.ekg.SignalProcessing;

import java.util.List;

public class MovingWindowIntegrationT {
    double x[] = new double[20];
    int n;
    long sum;
    double ly;
    double y;
    List<Double> Y;

    public double calculate(double k){
        if((++n)==20){
            n = 0;
        }
        sum -= x[n];
        sum += k;
        x[n] = k;
        ly = (double) sum /20.00;
        return (ly);
    }
}

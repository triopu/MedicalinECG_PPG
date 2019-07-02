package medicalin.ekg.SignalProcessing;

import java.util.List;

public class MovingWindowIntegration {
    double x[] = new double[32];
    int n;
    long sum;
    double ly;
    double y;
    List<Double> Y;

    public double calculate(double k){
        if((++n)==32){
            n = 0;
        }
        sum -= x[n];
        sum += k;
        x[n] = k;
        ly = (double) sum /32.00;
        return (ly);
    }
}

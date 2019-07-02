package medicalin.ekg.Photoplethysmogram;

import java.util.List;

public class MovingAverage {
    double x[] = new double[50];
    int n;
    long sum;
    double ly;
    double y;
    List<Double> Y;

    public double calculate(double k){
        if((++n)==50){
            n = 0;
        }
        sum -= x[n];
        sum += k;
        x[n] = k;
        ly = (double) sum /50.00;
        return (ly);
    }
}

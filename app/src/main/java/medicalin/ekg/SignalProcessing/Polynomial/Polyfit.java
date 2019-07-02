package medicalin.ekg.SignalProcessing.Polynomial;

import Jama.*;

public class Polyfit {
    private Matrix R;
    private double[] polynomialCoefficients;
    private int degreeOfFreedom;
    private double norm;
    private double yIntercept;
    private Matrix polyCoeffMatrix;

    public Polyfit(double[] _x, double[] _y, int order) throws Exception {
        int xLength = _x.length,
                yLength = _y.length;
        double tempValue = 0.0;
        Matrix y2D = new Matrix(JElmat.convertTo2D(_y));
        Matrix yTranspose = y2D.transpose();

        if (xLength != yLength) {
            throw new Exception(" Polyfit :- The lengths of the 2-input array parameters must be equal.");
        }
        if (xLength < 2) {
            throw new Exception(" Polyfit :- There must be at least 2 data points for polynomial fitting.");
        }
        if (order < 0) {
            throw new Exception(" Polyfit :- The polynomial fitting order cannot be less than zero.");
        }
        if (order >= xLength) {
            throw new Exception(" Polyfit :- The polynomial order = " + order + " , must be less than the number of data points = " + xLength);
        }
        Matrix tempMatrix = null;
        try {
            tempMatrix = JElmat.vander(_x, order + 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        QRDecomposition qr = new QRDecomposition(tempMatrix);
        Matrix Q = qr.getQ();
        R = qr.getR();
        Matrix result = R.inverse().times(Q.transpose().times(yTranspose));
        double[][] arrayResult = result.transpose().getArray();
        polynomialCoefficients = arrayResult[0];
        degreeOfFreedom = yLength - (order + 1);
        Matrix r = yTranspose.minus(tempMatrix.times(result));
        norm = r.norm2();
        polyCoeffMatrix = new Matrix(JElmat.convertTo2D(polynomialCoefficients));
        yIntercept = polynomialCoefficients[polynomialCoefficients.length - 1];

    }//end constructor


    public double[] getPolynomialCoefficients() {
        return polynomialCoefficients;
    }

    public Matrix getR() {
        return R;
    }

    public int getDegreeOfFreedom() {
        return degreeOfFreedom;
    }

    public double getNorm() {
        return norm;
    }

    public double getYIntercept() {
        return yIntercept;
    }

    public Matrix getPolyCoeffMatrix() {
        return polyCoeffMatrix;
    }

}
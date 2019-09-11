package com.jstarcraft.rns.utility;

/**
 * Digamma
 * 
 * <pre>
 * http://www.psc.edu/~burkardt/src/dirichlet/dirichlet.f
 * </pre>
 * 
 * @author Birdy
 *
 */
class Digamma {

    static float large = 9.5F;
    static float d1 = -0.5772156649015328606065121F; // digamma(1)
    static float d2 = (float) (Math.pow(Math.PI, 2.0) / 6.0F);
    static float small = 1E-6F;
    static float s3 = 1F / 12F;
    static float s4 = 1F / 120F;
    static float s5 = 1F / 252F;
    static float s6 = 1F / 240F;
    static float s7 = 1F / 132F;
    static float s8 = 691F / 32760F;
    static float s9 = 1F / 12F;
    static float s10 = 3617F / 8160F;

    public static float calculate(float x) {
        float y = 0F;
        float r = 0F;

        if (Double.isInfinite(x) || Double.isNaN(x)) {
            return 0F / 0F;
        }

        if (x == 0F) {
            return -1F / 0F;
        }

        if (x < 0F) {
            // Use the reflection formula (Jeffrey 11.1.6):
            // digamma(-x) = digamma(x+1) + pi*cot(pi*x)
            y = (float) (Digamma.calculate(-x + 1) + Math.PI * (1F / Math.tan(-Math.PI * x)));
            return y;
            // This is related to the identity
            // digamma(-x) = digamma(x+1) - digamma(z) + digamma(1-z)
            // where z is the fractional part of x
            // For example:
            // digamma(-3.1) = 1/3.1 + 1/2.1 + 1/1.1 + 1/0.1 + digamma(1-0.1)
            // = digamma(4.1) - digamma(0.1) + digamma(1-0.1)
            // Then we use
            // digamma(1-z) - digamma(z) = pi*cot(pi*z)
        }

        // Use approximation if argument <= small.
        if (x <= small) {
            y = y + d1 - 1F / x + d2 * x;
            return y;
        }

        // Reduce to digamma(X + N) where (X + N) >= large.
        while (true) {
            if (x > small && x < large) {
                y = y - 1F / x;
                x = x + 1F;
            } else {
                break;
            }
        }

        // Use de Moivre's expansion if argument >= large.
        // In maple: asympt(Psi(x), x);
        if (x >= large) {
            r = 1F / x;
            y = (float) (y + Math.log(x) - 0.5F * r);
            r = r * r;
            y = y - r * (s3 - r * (s4 - r * (s5 - r * (s6 - r * s7))));
        }

        return y;
    }

    // return the inverse function of digamma
    // i.e., returns x such that digamma(x) = y
    // adapted from Tony Minka fastfit Matlab code
    public static float inverse(float y, int n) {
        // Newton iteration to solve digamma(x)-y = 0
        float x = (float) (Math.exp(y) + 0.5F);
        if (y <= -2.22F) {
            x = -1F / (y - calculate(1));
        }

        for (int iter = 0; iter < n; iter++) {
            x = x - (calculate(x) - y) / Trigamma.calculate(x);
        }
        return x;
    }

}
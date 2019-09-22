package com.jstarcraft.rns.utility;

/**
 * Trigamma
 * 
 * <pre>
 * http://www.psc.edu/~burkardt/src/dirichlet/dirichlet.f
 * </pre>
 * 
 * @author Birdy
 *
 */
class Trigamma {

    static float small = 1E-4F;
    static float large = 8F;
    static float c = (float) (Math.pow(Math.PI, 2F) / 6F);
    static float c1 = -2.404113806319188570799476F;
    static float b2 = 1F / 6F;
    static float b4 = -1F / 30F;
    static float b6 = 1F / 42F;
    static float b8 = -1F / 30F;
    static float b10 = 5F / 66F;

    static float calculate(float x) {
        float y = 0F;
        float z = 0F;

        if (Float.isInfinite(x) || Float.isNaN(x)) {
            return 0F / 0F;
        }

        // zero or negative integer
        if (x <= 0F && Math.floor(x) == x) {
            return 1F / 0F;
        }

        // Negative non-integer
        if (x < 0 && Math.floor(x) != x) {
            // Use the derivative of the digamma reflection formula:
            // -trigamma(-x) = trigamma(x+1) - (pi*csc(pi*x))^2
            y = (float) (-Trigamma.calculate(-x + 1F) + Math.pow(Math.PI * (1F / Math.sin(-Math.PI * x)), 2F));
            return y;
        }

        // Small value approximation
        if (x <= small) {
            y = 1F / (x * x) + c + c1 * x;
            return y;
        }

        // Reduce to trigamma(x+n) where ( X + N ) >= large.
        while (true) {
            if (x > small && x < large) {
                y = y + 1F / (x * x);
                x = x + 1F;
            } else {
                break;
            }
        }

        if (x >= large) {
            z = 1F / (x * x);
            y = y + 0.5F * z + (1F + z * (b2 + z * (b4 + z * (b6 + z * (b8 + z * b10))))) / x;
        }

        return y;
    }

}
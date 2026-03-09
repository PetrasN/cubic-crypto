package com.petrasnefas.cubic.tags;

import java.math.BigInteger;

public final class Jacobi {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE  = BigInteger.ONE;

    private Jacobi() {}

    /** Pilnas variantas: pats sumažina a mod n. */
    public static int jacobi(BigInteger a, BigInteger n) {
        if (n.signum() <= 0 || !n.testBit(0)) {
            throw new IllegalArgumentException("n must be positive odd");
        }
        a = a.mod(n);
        return jacobiReducedFast(a, n);
    }

    /**
     * Greitas variantas: tikisi, kad 0 <= a < n ir n yra teigiamas nelyginis.
     * (Čia specialiai nėra range-check'ų – kad būtų maksimaliai greita.)
     */
    public static int jacobiReducedFast(BigInteger a, BigInteger n) {
        if (a.equals(ZERO)) return 0;
        if (a.equals(ONE))  return 1;

        int result = 1;

        while (!a.equals(ZERO)) {
            // Remove factors of 2 from a in one shot
            int s = a.getLowestSetBit();
            if (s > 0) {
                a = a.shiftRight(s);

                // If s is odd, flip sign depending on n mod 8
                if ((s & 1) == 1) {
                    int nMod8 = n.intValue() & 7;
                    if (nMod8 == 3 || nMod8 == 5) result = -result;
                }
            }

            if (a.equals(ONE)) return result;

            // Quadratic reciprocity: if a≡3(mod4) and n≡3(mod4), flip sign
            if ( ((a.intValue() & 3) == 3) && ((n.intValue() & 3) == 3) ) {
                result = -result;
            }

            // Instead of swap then a = a mod n, do it in one step:
            // (a, n) <- (n mod a, a)
            BigInteger nModA = n.mod(a);
            n = a;
            a = nModA;
        }

        return n.equals(ONE) ? result : 0;
    }
}
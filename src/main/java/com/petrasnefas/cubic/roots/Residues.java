package com.petrasnefas.cubic.roots;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class Residues {
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    private Residues(){}

    public static BigInteger findNonCubicResidue(BigInteger p) {
        BigInteger exp = p.subtract(ONE).divide(THREE);
        SecureRandom rnd = new SecureRandom();

        // Random paieška (stabili ir greita dideliems p)
        for (int tries = 0; tries < 1000; tries++) {
            BigInteger z = new BigInteger(p.bitLength(), rnd).mod(p);
            if (z.compareTo(BigInteger.TWO) < 0) continue;
            if (!z.modPow(exp, p).equals(ONE)) return z;
        }

        // fallback (labai retai prireiks)
        for (BigInteger z = BigInteger.TWO; z.compareTo(p) < 0; z = z.add(ONE)) {
            if (!z.modPow(exp, p).equals(ONE)) return z;
        }

        throw new IllegalStateException("Non-cubic residue not found (unexpected).");
    }
}
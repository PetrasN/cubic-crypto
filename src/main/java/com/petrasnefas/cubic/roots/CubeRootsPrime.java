package com.petrasnefas.cubic.roots;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Cube roots modulo an odd prime p with p ≡ 1 (mod 3),
 * using the Adleman–Manders–Miller (AMM) cube root algorithm (r = 3).
 *
 * Based on the algorithm outline in Table 1 (AMM cube root algorithm).  :contentReference[oaicite:1]{index=1}
 */
public final class CubeRootsPrime {
    private static final BigInteger ZERO  = BigInteger.ZERO;
    private static final BigInteger ONE   = BigInteger.ONE;
    private static final BigInteger TWO   = BigInteger.TWO;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    private CubeRootsPrime() {}

    /**
     * Returns 0 or 3 roots x such that x^3 ≡ a (mod p),
     * assuming p is prime and p ≡ 1 (mod 3).
     */
    public static List<BigInteger> cubeRootsModPrime(BigInteger a, BigInteger p) {
        a = a.mod(p);
        if (a.equals(ZERO)) return List.of(ZERO, ZERO, ZERO);

        // Check cubic residuosity: a^((p-1)/3) must be 1 for a cubic residue.
        BigInteger expCheck = p.subtract(ONE).divide(THREE);
        if (!a.modPow(expCheck, p).equals(ONE)) return List.of();

        // Step 1: p - 1 = 3^s * t, with t = 3l ± 1 (i.e., t mod 3 is 1 or 2)
        BigInteger t = p.subtract(ONE);
        int s = 0;
        while (t.mod(THREE).equals(ZERO)) {
            t = t.divide(THREE);
            s++;
        }
        // Now gcd(t, 3) = 1, and s = ν3(p-1) >= 1

        // Compute l from t = 3l ± 1
        // If t ≡ 1 (mod 3): t = 3l + 1 => l = (t - 1)/3, and final inverse is taken.
        // If t ≡ 2 (mod 3): t = 3l - 1 => l = (t + 1)/3, and no final inverse.
        BigInteger tMod3 = t.mod(THREE);
        if (!tMod3.equals(ONE) && !tMod3.equals(TWO)) {
            throw new IllegalStateException("Invariant broken: t must be 1 or 2 mod 3");
        }
        boolean plusCase = tMod3.equals(ONE); // t = 3l + 1
        BigInteger l = plusCase ? t.subtract(ONE).divide(THREE) : t.add(ONE).divide(THREE);

        // Step 2: select a cubic non-residue b
        BigInteger b = Residues.findNonCubicResidue(p);

        // c ← b^t (mod p)
        BigInteger c = b.modPow(t, p);

        // c' ← c^(3^(s-1)) (via repeated cubing)
        // Note: c' = b^((p-1)/3) is a primitive cube root of unity.
        BigInteger cPrime = c;
        for (int i = 0; i < s - 1; i++) {
            cPrime = cPrime.modPow(THREE, p); // cube
        }

        // Step 3: compute cube root using the AMM loop
        BigInteger h = ONE;
        BigInteger r = a.modPow(t, p); // r ← a^t

        // for i = 1 to s-1
        for (int i = 1; i <= s - 1; i++) {
            // d ← r^(3^(s-i-1))
            BigInteger d = r;
            int cubes = (s - i - 1);
            for (int j = 0; j < cubes; j++) {
                d = d.modPow(THREE, p);
            }

            int k;
            if (d.equals(ONE)) {
                k = 0;
            } else if (d.equals(cPrime)) {
                k = 2;
            } else {
                k = 1;
            }

            // h ← h * c^k
            if (k != 0) {
                h = h.multiply(c.modPow(BigInteger.valueOf(k), p)).mod(p);
            }

            // r ← r * (c^3)^k
            BigInteger c3 = c.modPow(THREE, p);
            if (k != 0) {
                r = r.multiply(c3.modPow(BigInteger.valueOf(k), p)).mod(p);
            }

            // c ← c^3
            c = c3;
        }

        // Step 4: r ← a^l * h
        BigInteger root = a.modPow(l, p).multiply(h).mod(p);

        // if t = 3l + 1, then r ← r^{-1}
        if (plusCase) {
            root = root.modInverse(p);
        }

        // Other two roots: multiply by ω and ω^2, where ω = c' is primitive cube root of unity.
        BigInteger omega = cPrime;                 // ω
        BigInteger omega2 = omega.modPow(TWO, p);  // ω^2

        BigInteger r2 = root.multiply(omega).mod(p);
        BigInteger r3 = root.multiply(omega2).mod(p);

        List<BigInteger> roots = new ArrayList<>(3);
        roots.add(root);
        roots.add(r2);
        roots.add(r3);
        return roots;
    }
}
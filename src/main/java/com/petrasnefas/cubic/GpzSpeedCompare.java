package com.petrasnefas.cubic;

import com.petrasnefas.cubic.roots.CubeRootsComposite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Arrays;

public class GpzSpeedCompare {

    private static final SecureRandom RNG = new SecureRandom();
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.TWO;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    public static void main(String[] args) {
        int nBits = 2048;
        int warmup = 200;
        int iters  = 2000;
        int k = 16; // pvz. 16 arba 18

        BigInteger p = randomPrimeCongruent1Mod3(nBits / 2);
        BigInteger q = randomPrimeCongruent1Mod3(nBits / 2);
        BigInteger n = p.multiply(q);
        int ell = n.bitLength();

        // vienas "tipinis" m (galite ir keisti kiekvieną iteraciją)
        BigInteger m = randomUniformBelow(n.subtract(TWO));
        BigInteger c  = modCube(m, n);
        BigInteger c1 = modCube(m.add(ONE), n);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            oldWay(p, q, n, c, c1);
            newWay(p, q, n, ell, c, c1, k);
        }

        long[] tOld = new long[iters];
        long[] tNew = new long[iters];

        for (int i = 0; i < iters; i++) {
            long s1 = System.nanoTime();
            oldWay(p, q, n, c, c1);
            tOld[i] = System.nanoTime() - s1;

            long s2 = System.nanoTime();
            newWay(p, q, n, ell, c, c1, k);
            tNew[i] = System.nanoTime() - s2;

            // jei norite "realistiškiau", kaskart keiskite m:
            // m = randomUniformBelow(n.subtract(TWO));
            // c  = modCube(m, n);
            // c1 = modCube(m.add(ONE), n);
        }

        Arrays.sort(tOld);
        Arrays.sort(tNew);

        long medOld = tOld[iters / 2];
        long medNew = tNew[iters / 2];

        System.out.printf("Median OLD (2 decrypt): %d ns%n", medOld);
        System.out.printf("Median NEW (1 decrypt + 9 cubes): %d ns%n", medNew);
        System.out.printf("Speedup: %.3f x%n", (double) medOld / (double) medNew);
    }

    // SENAS: 2 kartus cubeRootsModN (c ir c1)
    private static void oldWay(BigInteger p, BigInteger q, BigInteger n, BigInteger c, BigInteger c1) {
        List<BigInteger> r1 = CubeRootsComposite.cubeRootsModN(c,  p, q);
        List<BigInteger> r2 = CubeRootsComposite.cubeRootsModN(c1, p, q);
        // kad JIT neišmestų:
        if (r1.size() != 9 || r2.size() != 9) throw new RuntimeException("roots != 9");
    }

    // NAUJAS: 1 kartą cubeRootsModN(c) + 9 kubai + tag match
    private static void newWay(BigInteger p, BigInteger q, BigInteger n, int ell, BigInteger c, BigInteger c1, int k) {
        List<BigInteger> cand = CubeRootsComposite.cubeRootsModN(c, p, q);
        if (cand.size() != 9) throw new RuntimeException("roots != 9");

        // LSB tag (galite pakeisti į MSB)
        BigInteger mask = TWO.pow(k).subtract(ONE);
        BigInteger tag = c1.and(mask);

        int matches = 0;
        for (int i = 0; i < 9; i++) {
            BigInteger ai = modCube(cand.get(i).add(ONE), n);
            if (ai.and(mask).equals(tag)) matches++;
        }
        if (matches < 1) throw new RuntimeException("no match?");
    }

    private static BigInteger modCube(BigInteger x, BigInteger n) {
        BigInteger x2 = x.multiply(x).mod(n);
        return x2.multiply(x).mod(n);
    }

    private static BigInteger randomPrimeCongruent1Mod3(int bits) {
        while (true) {
            BigInteger p = BigInteger.probablePrime(bits, RNG);
            if (p.mod(THREE).equals(ONE)) return p;
        }
    }

    private static BigInteger randomUniformBelow(BigInteger bound) {
        int bits = bound.bitLength();
        while (true) {
            BigInteger r = new BigInteger(bits, RNG);
            if (r.compareTo(bound) < 0) return r;
        }
    }
}
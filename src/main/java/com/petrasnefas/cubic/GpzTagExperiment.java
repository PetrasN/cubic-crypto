package com.petrasnefas.cubic;


import com.petrasnefas.cubic.roots.CubeRootsComposite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class GpzTagExperiment {

    private static final SecureRandom RNG = new SecureRandom();

    private static final BigInteger ZERO  = BigInteger.ZERO;
    private static final BigInteger ONE   = BigInteger.ONE;
    private static final BigInteger TWO   = BigInteger.TWO;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    public static void main(String[] args) {
        // ===== Konfigūracija =====
        int nBits = 2048;           // n bitų ilgis
        int trialsTotal = 10_000;   // kiek bandymų su skirtingais m
        int trialsPerKey = 200;     // kiek m vienam raktui (greičiau nei generuoti 10k raktų)
        int kMax = 32;              // iki kiek bitų ieškom minimalų k
        int kMinStart = 1;

        int[] kCheck = {12, 14, 16, 18, 20};

        long[] histLsb = new long[kMax + 1]; // k=0 reiškia "neužteko iki kMax"
        long[] histMsb = new long[kMax + 1];

        long[] succLsbAtK = new long[kCheck.length];
        long[] succMsbAtK = new long[kCheck.length];

        long done = 0;
        int keysNeeded = (int) Math.ceil(trialsTotal / (double) trialsPerKey);

        for (int keyIdx = 0; keyIdx < keysNeeded && done < trialsTotal; keyIdx++) {
            // 1) Raktas: p,q ≡ 1 (mod 3)
            BigInteger p = randomPrimeCongruent1Mod3(nBits / 2);
            BigInteger q = randomPrimeCongruent1Mod3(nBits / 2);
            BigInteger n = p.multiply(q);
            int ell = n.bitLength(); // MSB apibrėžimui

            int reps = Math.min(trialsPerKey, trialsTotal - (int) done);
            for (int t = 0; t < reps; t++) {
                done++;

                // 2) Atsitiktinis m ∈ [0, n-2]
                BigInteger m = randomUniformBelow(n.subtract(ONE));
                if (m.compareTo(n.subtract(TWO)) > 0) m = n.subtract(TWO);

                // 3) c = m^3 mod n, c1 = (m+1)^3 mod n
                BigInteger c  = modCube(m, n);
                BigInteger c1 = modCube(m.add(ONE), n);

                // 4) 9 kandidatai m_i: m_i^3 ≡ c (mod n)
                List<BigInteger> candList = CubeRootsComposite.cubeRootsModN(c, p, q);
                if (candList.size() != 9) {
                    // jei taip nutiko, reiškia c nebuvo kubinė liekana mod p ar q (neturėtų, nes c=m^3),
                    // arba kažkas edge-case; galit praleisti tokius atvejus.
                    // Čia darom griežtai:
                    throw new IllegalStateException("Tikėtasi 9 kandidatų, gauta: " + candList.size());
                }

                // 5) Iš anksto paskaičiuojam a_i = (m_i+1)^3 mod n (9 kartus)
                BigInteger[] a = new BigInteger[9];
                for (int i = 0; i < 9; i++) {
                    a[i] = modCube(candList.get(i).add(ONE), n);
                }

                // 6) Minimalus k, kada vienareikšmis sutapimas
                int kStarLsb = findMinimalK_LSB(c1, a, kMinStart, kMax);
                int kStarMsb = findMinimalK_MSB(c1, a, ell, kMinStart, kMax);

                histLsb[kStarLsb]++;
                histMsb[kStarMsb]++;

                // 7) Sėkmė konkretiems k
                for (int i = 0; i < kCheck.length; i++) {
                    int k = kCheck[i];
                    if (isUniqueMatch_LSB(c1, a, k)) succLsbAtK[i]++;
                    if (isUniqueMatch_MSB(c1, a, ell, k)) succMsbAtK[i]++;
                }

                if (done % 500 == 0) {
                    System.out.printf("Progress: %d/%d (keys=%d)%n", done, trialsTotal, keyIdx + 1);
                }
            }
        }

        // ===== Rezultatų ataskaita =====
        System.out.println("\n=== Minimalus k (histogramos) ===");
        printHistogram("LSB", histLsb);
        printHistogram("MSB", histMsb);

        System.out.println("\n=== Vienareikšmiškumo tikimybė fiksuotiems k ===");
        for (int i = 0; i < kCheck.length; i++) {
            double pL = succLsbAtK[i] / (double) trialsTotal;
            double pM = succMsbAtK[i] / (double) trialsTotal;
            System.out.printf("k=%d:  LSB=%.6f   MSB=%.6f%n", kCheck[i], pL, pM);
        }
    }

    // ===== Pagalbinės funkcijos =====

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

    private static int findMinimalK_LSB(BigInteger c1, BigInteger[] a, int kMin, int kMax) {
        for (int k = kMin; k <= kMax; k++) {
            if (isUniqueMatch_LSB(c1, a, k)) return k;
        }
        return 0;
    }

    private static boolean isUniqueMatch_LSB(BigInteger c1, BigInteger[] a, int k) {
        BigInteger mask = TWO.pow(k).subtract(ONE);     // (2^k - 1)
        BigInteger tag = c1.and(mask);                  // LSB_k(c1)

        int matches = 0;
        for (BigInteger ai : a) {
            if (ai.and(mask).equals(tag)) matches++;
            if (matches > 1) return false;
        }
        return matches == 1;
    }

    private static int findMinimalK_MSB(BigInteger c1, BigInteger[] a, int ell, int kMin, int kMax) {
        for (int k = kMin; k <= kMax; k++) {
            if (isUniqueMatch_MSB(c1, a, ell, k)) return k;
        }
        return 0;
    }

    private static boolean isUniqueMatch_MSB(BigInteger c1, BigInteger[] a, int ell, int k) {
        int shift = ell - k;
        if (shift < 0) shift = 0;
        BigInteger tag = c1.shiftRight(shift);          // MSB_k(c1) pagal ell-bit reprezentaciją

        int matches = 0;
        for (BigInteger ai : a) {
            if (ai.shiftRight(shift).equals(tag)) matches++;
            if (matches > 1) return false;
        }
        return matches == 1;
    }

    private static void printHistogram(String name, long[] hist) {
        long total = Arrays.stream(hist).sum();
        System.out.printf("%s: total=%d%n", name, total);
        for (int k = 0; k < hist.length; k++) {
            if (hist[k] == 0) continue;
            if (k == 0) System.out.printf("  k=0 (neužteko iki kMax): %d%n", hist[k]);
            else System.out.printf("  k=%d: %d%n", k, hist[k]);
        }
    }
}
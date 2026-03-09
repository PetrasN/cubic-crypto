package com.petrasnefas.cubic.tests;

import com.petrasnefas.cubic.roots.CubeRootsComposite;
import com.petrasnefas.cubic.tags.Jacobi;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class AdaptiveJacobiStats {

    private static final SecureRandom RNG = new SecureRandom();

    private static final BigInteger ONE   = BigInteger.ONE;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    // === Parametrai ===
    private static final int NBITS_N = 2048;
    private static final int TRIALS = 10_000;
    private static final int TMAX   = 18;     // tikrinam (m+1)...(m+TMAX)
    private static final boolean SAVE_CSV = true;

    public static void main(String[] args) throws IOException {
        long t0 = System.nanoTime();

        int errors = 0;
        int uniqueByTmax = 0;

        // hist[1..TMAX], hist[TMAX+1] = "neunikalu iki Tmax" (minT=-1 CSV'e)
        int[] hist = new int[TMAX + 2];

        long sumT = 0;

        try (FileWriter fw = SAVE_CSV ? new FileWriter("adaptive_minT.csv") : null) {
            if (SAVE_CSV) fw.write("trial,minT,candidatesLeft\n");

            for (int trial = 1; trial <= TRIALS; trial++) {

                // 1) p,q: 1024-bitų pirminiai, ≡1 (mod 3)
                BigInteger p = randomPrimeMod1(NBITS_N / 2, 3);
                BigInteger q;
                do { q = randomPrimeMod1(NBITS_N / 2, 3); } while (q.equals(p));
                BigInteger n = p.multiply(q);

                // 2) m: atsitiktinis, gcd(m,n)=1
                // (Kad m+t būtų < n, čia parenkam m < n-(TMAX+2). Nebūtina, bet tvarkingiau.)
                BigInteger bound = n.subtract(BigInteger.valueOf(TMAX + 2));
                BigInteger m = randomCoprimeBelow(bound, n);

                // 3) tikslinis Jacobi vektorius: J[t] = (m+t / n), t=1..TMAX
                int[] J = new int[TMAX + 1];
                for (int t = 1; t <= TMAX; t++) {
                    J[t] = Jacobi.jacobi(m.add(BigInteger.valueOf(t)), n);
                }

                // 4) c = m^3 mod n
                BigInteger c = m.modPow(THREE, n);

                // 5) 9 kandidatai (kubinės šaknys)
                List<BigInteger> roots = CubeRootsComposite.cubeRootsModN(c, p, q); // :contentReference[oaicite:1]{index=1}
                if (roots.size() != 9) {
                    // jei taip nutinka, tikėtina: Rp/Rq tušti arba ne 3 šaknys (patikrinkite p,q sąlygas / implementaciją)
                    errors++;
                    hist[TMAX + 1]++;
                    if (SAVE_CSV) fw.write(trial + ",-1," + roots.size() + "\n");
                    continue;
                }

                // 6) Adaptacinis filtravimas
                List<BigInteger> cand = new ArrayList<>(roots);

                int minT = TMAX + 1; // default: neunikalu iki Tmax
                for (int t = 1; t <= TMAX; t++) {
                    int target = J[t];
                    BigInteger add = BigInteger.valueOf(t);

                    cand.removeIf(x -> Jacobi.jacobi(x.add(add), n) != target);

                    if (cand.size() == 1) {
                        minT = t;
                        break;
                    }
                    if (cand.isEmpty()) {
                        // neturėtų įvykti jei roots tikrai atitinka c
                        minT = TMAX + 1;
                        break;
                    }
                }

                int candidatesLeft = cand.size();

                // 7) Patikra: jei unikalus – turi būti m
                if (minT <= TMAX) {
                    uniqueByTmax++;
                    sumT += minT;

                    BigInteger recovered = cand.get(0).mod(n);
                    if (!recovered.equals(m.mod(n))) {
                        errors++;
                    }
                    hist[minT]++;
                } else {
                    hist[TMAX + 1]++;
                }

                if (SAVE_CSV) {
                    fw.write(trial + "," + (minT <= TMAX ? minT : -1) + "," + candidatesLeft + "\n");
                }

                if (trial % 1000 == 0) {
                    System.out.printf("Progress: %d/%d | Errors: %d | Unique rate so far: %.4f%n",
                            trial, TRIALS, errors, uniqueByTmax * 1.0 / trial);
                }
            }
        }

        long t1 = System.nanoTime();
        double seconds = (t1 - t0) / 1e9;

        int noUnique = hist[TMAX + 1];
        double uniqueRate = uniqueByTmax * 1.0 / TRIALS;
        double meanT = uniqueByTmax == 0 ? Double.NaN : (sumT * 1.0 / uniqueByTmax);
        int medianT = medianFromHistogram(hist, TMAX, uniqueByTmax);
        int p95T = percentileFromHistogram(hist, TMAX, uniqueByTmax, 0.95);

        System.out.println("\n===== Adaptive Jacobi summary =====");
        System.out.printf("Trials: %d, Errors: %d%n", TRIALS, errors);
        System.out.printf("Unique found by Tmax=%d: %d (rate=%.4f)%n", TMAX, uniqueByTmax, uniqueRate);
        System.out.printf("No-unique up to Tmax: %d%n", noUnique);
        System.out.printf("mean(minT)=%.2f, median(minT)=%d, p95(minT)=%d%n", meanT, medianT, p95T);
        System.out.printf("Time: %.1fs, %.6fs/trial%n", seconds, seconds / TRIALS);

        System.out.println("\nHistogram (minT -> count):");
        for (int t = 1; t <= TMAX; t++) {
            System.out.printf("%2d -> %d%n", t, hist[t]);
        }
        System.out.printf(">%2d -> %d%n", TMAX, hist[TMAX + 1]);

        if (SAVE_CSV) {
            System.out.println("\nSaved: adaptive_minT.csv (minT=-1 reiškia: neunikalu iki Tmax)");
        }
    }

    // === Pagalbiniai metodai ===

    private static BigInteger randomPrimeMod1(int bitLen, int mod) {
        BigInteger M = BigInteger.valueOf(mod);
        while (true) {
            BigInteger p = BigInteger.probablePrime(bitLen, RNG);
            if (p.mod(M).equals(ONE)) return p;
        }
    }

    private static BigInteger randomCoprimeBelow(BigInteger boundExclusive, BigInteger n) {
        while (true) {
            BigInteger m = new BigInteger(boundExclusive.bitLength(), RNG);
            if (m.signum() <= 0) continue;
            if (m.compareTo(boundExclusive) >= 0) continue;
            if (m.gcd(n).equals(ONE)) return m;
        }
    }

    private static int medianFromHistogram(int[] hist, int tmax, int totalUnique) {
        if (totalUnique <= 0) return -1;
        int target = (totalUnique + 1) / 2;
        int cum = 0;
        for (int t = 1; t <= tmax; t++) {
            cum += hist[t];
            if (cum >= target) return t;
        }
        return tmax;
    }

    private static int percentileFromHistogram(int[] hist, int tmax, int totalUnique, double p) {
        if (totalUnique <= 0) return -1;
        int target = (int) Math.ceil(totalUnique * p);
        int cum = 0;
        for (int t = 1; t <= tmax; t++) {
            cum += hist[t];
            if (cum >= target) return t;
        }
        return tmax;
    }
}
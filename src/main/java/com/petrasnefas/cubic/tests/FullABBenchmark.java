package com.petrasnefas.cubic.tests;

import com.petrasnefas.cubic.roots.CubeRootsComposite;
import com.petrasnefas.cubic.tags.Jacobi;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class FullABBenchmark {

    private static final SecureRandom RNG = new SecureRandom();

    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    // Konfigūracija
    private static final int[] KEY_BITS = {1024, 1536, 2048};
    private static final int TRIALS = 1000;

    // Algoritmas 1
    private static final int ALG1_A_TMAX = 16;      // A pusė paruošia 16 simbolių
    private static final int ALG1_B_START = 4;      // B pusė pradeda nuo 4

    // Algoritmas 3
    private static final int ALG3_BITS = 16;        // perduodami 16 bitų

    public static void main(String[] args) {
        for (int nBits : KEY_BITS) {
            benchmarkForKeySize(nBits);
            System.out.println();
        }
    }

    private static void benchmarkForKeySize(int nBits) {
        System.out.println("==================================================");
        System.out.printf("Full A/B Benchmark | nBits=%d%n", nBits);
        System.out.println("==================================================");

        long a1Total = 0, b1Total = 0;
        long a2Total = 0, b2Total = 0;
        long a3Total = 0, b3Total = 0;

        int err1 = 0, err2 = 0, err3 = 0;

        long globalStart = System.nanoTime();

        for (int trial = 1; trial <= TRIALS; trial++) {

            // 1) Generuojame raktus
            BigInteger p = randomPrimeMod1(nBits / 2, 3);
            BigInteger q;
            do {
                q = randomPrimeMod1(nBits / 2, 3);
            } while (q.equals(p));

            BigInteger n = p.multiply(q);

            // 2) Atsitiktinis m: 1 <= m < n-1, gcd(m,n)=1
            BigInteger m = randomCoprimeBelow(n.subtract(ONE), n);

            // ======================================================
            // ALGORTIMAS 1
            // A pusė: c = m^3 mod n + Jacobi(m+1..m+16)
            // B pusė: 1 šaknis + adaptacinis filtravimas nuo 4 simbolių
            // ======================================================

            long a1s = System.nanoTime();

            BigInteger c1 = m.modPow(THREE, n);
            int[] jacSeq = new int[ALG1_A_TMAX + 1]; // nuo 1 iki 16
            for (int t = 1; t <= ALG1_A_TMAX; t++) {
                jacSeq[t] = Jacobi.jacobi(m.add(BigInteger.valueOf(t)), n);
            }

            long a1e = System.nanoTime();
            a1Total += (a1e - a1s);

            long b1s = System.nanoTime();

            List<BigInteger> roots1 = CubeRootsComposite.cubeRootsModN(c1, p, q);
            BigInteger rec1 = recoverAlg1Adaptive(roots1, jacSeq, n, ALG1_B_START, ALG1_A_TMAX);

            long b1e = System.nanoTime();
            b1Total += (b1e - b1s);

            if (rec1 == null || !rec1.equals(m.mod(n))) {
                err1++;
            }

            // ======================================================
            // ALGORTIMAS 2
            // A pusė: c = m^3 mod n, c2 = (m+1)^3 mod n
            // B pusė: 2 šaknų skaičiavimai + poros paieška
            // ======================================================

            long a2s = System.nanoTime();

            BigInteger c2a = m.modPow(THREE, n);
            BigInteger c2b = m.add(ONE).modPow(THREE, n);

            long a2e = System.nanoTime();
            a2Total += (a2e - a2s);

            long b2s = System.nanoTime();

            List<BigInteger> roots2a = CubeRootsComposite.cubeRootsModN(c2a, p, q);
            List<BigInteger> roots2b = CubeRootsComposite.cubeRootsModN(c2b, p, q);
            BigInteger rec2 = recoverAlg2AdjacentPair(roots2a, roots2b, n);

            long b2e = System.nanoTime();
            b2Total += (b2e - b2s);

            if (rec2 == null || !rec2.equals(m.mod(n))) {
                err2++;
            }

            // ======================================================
            // ALGORTIMAS 3
            // A pusė: c = m^3 mod n, c2 = (m+1)^3 mod n, siunčiami 16 LSB(c2)
            // B pusė: 1 šaknis + 9 kubai + bitų tikrinimas
            // ======================================================

            long a3s = System.nanoTime();

            BigInteger c3 = m.modPow(THREE, n);
            BigInteger c3next = m.add(ONE).modPow(THREE, n);
            BigInteger mask = ONE.shiftLeft(ALG3_BITS).subtract(ONE);
            BigInteger c3bits = c3next.and(mask);

            long a3e = System.nanoTime();
            a3Total += (a3e - a3s);

            long b3s = System.nanoTime();

            List<BigInteger> roots3 = CubeRootsComposite.cubeRootsModN(c3, p, q);
            BigInteger rec3 = recoverAlg3ByBits(roots3, c3bits, mask, n);

            long b3e = System.nanoTime();
            b3Total += (b3e - b3s);

            if (rec3 == null || !rec3.equals(m.mod(n))) {
                err3++;
            }

            if (trial % 200 == 0 || trial == TRIALS) {
                double elapsed = (System.nanoTime() - globalStart) / 1e9;
                System.out.printf("Progress: %d/%d | Elapsed: %.1f s | Errors: A1=%d A2=%d A3=%d%n",
                        trial, TRIALS, elapsed, err1, err2, err3);
            }
        }

        System.out.println("\n===== Summary =====");
        System.out.printf("Trials: %d%n", TRIALS);

        System.out.printf("Algoritmas 1 | errors=%d | A avg = %.0f ns | B avg = %.0f ns%n",
                err1, a1Total * 1.0 / TRIALS, b1Total * 1.0 / TRIALS);

        System.out.printf("Algoritmas 2 | errors=%d | A avg = %.0f ns | B avg = %.0f ns%n",
                err2, a2Total * 1.0 / TRIALS, b2Total * 1.0 / TRIALS);

        System.out.printf("Algoritmas 3 | errors=%d | A avg = %.0f ns | B avg = %.0f ns%n",
                err3, a3Total * 1.0 / TRIALS, b3Total * 1.0 / TRIALS);

        System.out.println("\nms formatu:");
        System.out.printf("Algoritmas 1 | A = %.4f ms | B = %.4f ms%n",
                a1Total / 1e6 / TRIALS, b1Total / 1e6 / TRIALS);

        System.out.printf("Algoritmas 2 | A = %.4f ms | B = %.4f ms%n",
                a2Total / 1e6 / TRIALS, b2Total / 1e6 / TRIALS);

        System.out.printf("Algoritmas 3 | A = %.4f ms | B = %.4f ms%n",
                a3Total / 1e6 / TRIALS, b3Total / 1e6 / TRIALS);
    }

    // ==========================================================
    // Algoritmas 1: adaptacinis Jacobi filtravimas
    // ==========================================================
    private static BigInteger recoverAlg1Adaptive(List<BigInteger> roots,
                                                  int[] jacSeq,
                                                  BigInteger n,
                                                  int startT,
                                                  int maxT) {
        if (roots == null || roots.isEmpty()) return null;

        List<BigInteger> cand = new ArrayList<>(roots);

        for (int t = 1; t <= maxT; t++) {
            if (t < startT) {
                // vis tiek atliekame filtravimą, nes kitaip pradėti nuo 4 nėra prasmės
                // "startT" čia interpretuojame kaip minimalų adaptacijos lygį,
                // bet kandidatų mažinimą pradedame nuo 1.
            }

            final int tt = t;
            final int target = jacSeq[t];
            final BigInteger add = BigInteger.valueOf(tt);

            cand.removeIf(x -> Jacobi.jacobi(x.add(add), n) != target);

            if (cand.size() == 1) {
                return cand.get(0).mod(n);
            }
            if (cand.isEmpty()) {
                return null;
            }
        }

        return cand.size() == 1 ? cand.get(0).mod(n) : null;
    }

    // ==========================================================
    // Algoritmas 2: poros (m, m+1) paieška
    // ==========================================================
    private static BigInteger recoverAlg2AdjacentPair(List<BigInteger> R,
                                                      List<BigInteger> S,
                                                      BigInteger n) {
        if (R == null || S == null || R.size() != 9 || S.size() != 9) return null;

        for (BigInteger x : R) {
            BigInteger y = x.add(ONE);
            if (y.compareTo(n) >= 0) y = y.subtract(n);

            for (BigInteger s : S) {
                if (s.equals(y)) {
                    return x.mod(n);
                }
            }
        }
        return null;
    }

    // ==========================================================
    // Algoritmas 3: 16 bitų tikrinimas
    // ==========================================================
    private static BigInteger recoverAlg3ByBits(List<BigInteger> roots,
                                                BigInteger bits,
                                                BigInteger mask,
                                                BigInteger n) {
        if (roots == null || roots.isEmpty()) return null;

        BigInteger found = null;
        int matches = 0;

        for (BigInteger r : roots) {
            BigInteger test = r.add(ONE).modPow(THREE, n);
            if (test.and(mask).equals(bits)) {
                found = r.mod(n);
                matches++;
            }
        }

        return matches == 1 ? found : null;
    }

    // ==========================================================
    // Pagalbiniai metodai
    // ==========================================================
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
}
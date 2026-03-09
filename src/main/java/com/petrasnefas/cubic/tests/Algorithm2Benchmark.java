package com.petrasnefas.cubic.tests;

import com.petrasnefas.cubic.roots.CubeRootsComposite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Algorithm2Benchmark {

    private static final SecureRandom RNG = new SecureRandom();

    private static final BigInteger ZERO  = BigInteger.ZERO;
    private static final BigInteger ONE   = BigInteger.ONE;
    private static final BigInteger THREE = BigInteger.valueOf(3);

    // ==== Konfigūracija ====
    // Rakto ilgiai (n bitais) – galite papildyti/keisti
    private static final int[] KEY_BITS = {1024, 1536, 2048};

    // Kiek raktų generuoti kiekvienam ilgiui (keygen brangus)
    private static final int KEYS_PER_SIZE = 5;

    // Kiek žinučių testuoti su vienu raktu (čia renkame greitoveikos statistiką)
    private static final int MSGS_PER_KEY = 200;

    // Progress kas kiek žinučių (ne per dažnai)
    private static final int PROGRESS_EVERY = 200;

    public static void main(String[] args) {
        for (int nBits : KEY_BITS) {
            runForKeySize(nBits);
            System.out.println();
        }
    }

    private static void runForKeySize(int nBits) {
        System.out.println("==================================================");
        System.out.printf("Algoritmas 2 (m ir m+1): benchmark | nBits=%d%n", nBits);
        System.out.println("==================================================");

        int totalTrials = KEYS_PER_SIZE * MSGS_PER_KEY;

        // Laikų masyvai (ns) – po vieną įrašą kiekvienam bandymui
        long[] tEncrypt = new long[totalTrials];     // c ir c1 skaičiavimas
        long[] tRoots1  = new long[totalTrials];     // šaknys iš c
        long[] tRoots2  = new long[totalTrials];     // šaknys iš c1
        long[] tMatch   = new long[totalTrials];     // poros paieška
        long[] tTotal   = new long[totalTrials];     // bendras A2 laikas

        int idx = 0;
        int errors = 0;
        int noPair = 0;

        long globalStart = System.nanoTime();

        for (int k = 1; k <= KEYS_PER_SIZE; k++) {

            // 1) Raktas: p,q po ~nBits/2, p≡q≡1 (mod 3)
            BigInteger p = randomPrimeMod1(nBits / 2, 3);
            BigInteger q;
            do { q = randomPrimeMod1(nBits / 2, 3); } while (q.equals(p));
            BigInteger n = p.multiply(q);

            for (int mIdx = 1; mIdx <= MSGS_PER_KEY; mIdx++, idx++) {

                // 2) Parenkam m: 1 <= m < n-1, gcd(m,n)=1 (kad m+1 būtų < n)
                BigInteger m = randomCoprimeBelow(n.subtract(ONE), n);

                // 3) A pusė: šifravimas (du kubai)
                long t0 = System.nanoTime();
                BigInteger c  = m.modPow(THREE, n);
                BigInteger c1 = m.add(ONE).modPow(THREE, n);
                long t1 = System.nanoTime();
                tEncrypt[idx] = t1 - t0;

                // 4) B pusė: šaknys iš c
                long r0 = System.nanoTime();
                List<BigInteger> R = CubeRootsComposite.cubeRootsModN(c, p, q);
                long r1 = System.nanoTime();
                tRoots1[idx] = r1 - r0;

                // 5) B pusė: šaknys iš c1
                long s0 = System.nanoTime();
                List<BigInteger> S = CubeRootsComposite.cubeRootsModN(c1, p, q);
                long s1 = System.nanoTime();
                tRoots2[idx] = s1 - s0;

                // 6) Poros (x, y=x+1) paieška
                long m0 = System.nanoTime();
                BigInteger recovered = recoverByAdjacentPair(R, S, n);
                long m1t = System.nanoTime();
                tMatch[idx] = m1t - m0;

                long end = System.nanoTime();
                tTotal[idx] = end - t0;

                if (recovered == null) {
                    noPair++;
                    errors++;
                } else if (!recovered.equals(m)) {
                    errors++;
                }

                int done = idx + 1;
                if (done % PROGRESS_EVERY == 0) {
                    double elapsed = (System.nanoTime() - globalStart) / 1e9;
                    System.out.printf("Progress: %d/%d | Errors: %d | Elapsed: %.1f s%n",
                            done, totalTrials, errors, elapsed);
                }
            }
        }

        // 7) Santrauka
        double acc = (totalTrials - errors) * 1.0 / totalTrials;

        System.out.println("\n===== Summary =====");
        System.out.printf("Trials: %d%n", totalTrials);
        System.out.printf("Errors: %d (noPair=%d)%n", errors, noPair);
        System.out.printf("Accuracy: %.6f%n", acc);

        printStats("Encrypt (2 cubes)", tEncrypt);
        printStats("Roots #1 (from c)", tRoots1);
        printStats("Roots #2 (from c1)", tRoots2);
        printStats("Match (pair search)", tMatch);
        printStats("Total (Alg2)", tTotal);

        // Papildoma interpretacija
        System.out.println("\nInterpretacija:");
        System.out.println("- Algoritmas 2 yra deterministinis: tikslumas turėtų būti 1.000000, jei kubinių šaknų funkcija korektiška.");
        System.out.println("- Greitoveiką dominuoja du kubinių šaknų skaičiavimai (Roots #1 ir Roots #2).");
        System.out.println("- Poros paieškos laikas yra nereikšmingas lyginant su šaknų radimu.");
    }

    /**
     * Algoritmas 2:
     * randa m, ieškodamas poros (x in R, y in S), kur y == x+1 (mod n).
     * Grąžina x (t. y. m) arba null, jei nerado.
     */
    private static BigInteger recoverByAdjacentPair(List<BigInteger> R, List<BigInteger> S, BigInteger n) {
        if (R == null || S == null) return null;
        if (R.size() != 9 || S.size() != 9) return null;

        // Kad paieška būtų greita – konvertuojam S į rinkinį per List.contains (9 elem. – pakanka)
        for (BigInteger x : R) {
            BigInteger y = x.add(ONE);
            if (y.compareTo(n) >= 0) y = y.subtract(n); // mod n
            // y turi būti tarp S
            for (BigInteger s : S) {
                if (s.equals(y)) return x.mod(n);
            }
        }
        return null;
    }

    // === Pagalbinės funkcijos ===

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

    private static void printStats(String label, long[] nanos) {
        List<Long> list = new ArrayList<>(nanos.length);
        for (long x : nanos) list.add(x);
        Collections.sort(list);

        long min = list.get(0);
        long max = list.get(list.size() - 1);
        long median = list.get(list.size() / 2);
        long p95 = list.get((int)Math.ceil(0.95 * (list.size() - 1)));
        double mean = list.stream().mapToDouble(Long::doubleValue).average().orElse(Double.NaN);

        System.out.printf("%n[%s]%n", label);
        System.out.printf("min:    %d ns%n", min);
        System.out.printf("mean:   %.0f ns%n", mean);
        System.out.printf("median: %d ns%n", median);
        System.out.printf("p95:    %d ns%n", p95);
        System.out.printf("max:    %d ns%n", max);
    }
}

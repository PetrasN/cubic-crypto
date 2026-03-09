package com.petrasnefas.cubic.bench;

import com.petrasnefas.cubic.core.*;
import com.petrasnefas.cubic.roots.CubeRootsComposite;
import com.petrasnefas.cubic.tags.Jacobi;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public final class ShiftSearchRunner {

    public static void runGreedySearch(int nBits, int M, int maxShift, int maxChosen) {
        SecureRandom rnd = new SecureRandom();

        System.out.println("KeyGen nBits=" + nBits);
        KeyPair kp = CubicKeyGen.generateKeyPair(nBits, rnd);
        PublicKey pk = kp.pk();
        PrivateKey sk = kp.sk();
        BigInteger n = pk.n();

        System.out.println("Generating dataset M=" + M + ", maxShift=" + maxShift);

        // Kandidatai (9) kiekvienam pranešimui
        List<BigInteger[]> candidates = new ArrayList<>(M);
        BigInteger[] messages = new BigInteger[M];

        // Bitai:
        // tagBits[msg][s] = Jacobi(m + (s+1), n)  (s=0..maxShift-1)
        // candBits[msg][s][k] = Jacobi(M_k + (s+1), n)
        byte[][] tagBits = new byte[M][maxShift];
        byte[][][] candBits = new byte[M][maxShift][9];

        for (int i = 0; i < M; i++) {
            BigInteger m = randomCoprime(n, rnd);
            messages[i] = m;

            // c = m^3 mod n
            BigInteger c = m.modPow(BigInteger.valueOf(3), n);

            List<BigInteger> cand = CubeRootsComposite.cubeRootsModN(c, sk.p(), sk.q());
            if (cand.size() != 9) {
                throw new IllegalStateException("Expected 9 candidates, got " + cand.size());
            }
            BigInteger[] arr = cand.toArray(new BigInteger[0]);
            candidates.add(arr);

            // Precompute all bits for shifts 1..maxShift
            for (int s = 1; s <= maxShift; s++) {
                BigInteger xm = addModSmall(m, s, n);
                tagBits[i][s - 1] = (byte) Jacobi.jacobiReducedFast(xm, n);

                for (int k = 0; k < 9; k++) {
                    BigInteger xk = addModSmall(arr[k], s, n);
                    candBits[i][s - 1][k] = (byte) Jacobi.jacobiReducedFast(xk, n);
                }
            }

            if ((i + 1) % 25 == 0) System.out.println("Prepared " + (i + 1) + "/" + M);
        }

        // Alive mask per message: 9 bits set = all candidates alive
        int[] aliveMask = new int[M];
        Arrays.fill(aliveMask, 0x1FF); // 9 bits = 511

        boolean[] usedShift = new boolean[maxShift];
        List<Integer> chosen = new ArrayList<>();

        System.out.println("\nGreedy search...");
        for (int step = 0; step < maxChosen; step++) {
            int bestShift = -1;
            int bestWorst = Integer.MAX_VALUE;
            double bestAvg = Double.POSITIVE_INFINITY;

            for (int sIdx = 0; sIdx < maxShift; sIdx++) {
                if (usedShift[sIdx]) continue;

                int worst = 0;
                long sum = 0;

                for (int i = 0; i < M; i++) {
                    int newMask = filterMask(aliveMask[i], candBits[i][sIdx], tagBits[i][sIdx]);
                    int cnt = Integer.bitCount(newMask);
                    if (cnt > worst) worst = cnt;
                    sum += cnt;

                    // mažas pruning: jei jau blogiau nei geriausias, nebeverta
                    if (worst > bestWorst) break;
                }

                double avg = sum / (double) M;

                if (worst < bestWorst || (worst == bestWorst && avg < bestAvg)) {
                    bestWorst = worst;
                    bestAvg = avg;
                    bestShift = sIdx;
                }
            }

            if (bestShift < 0) break;

            // Apply bestShift to all messages
            for (int i = 0; i < M; i++) {
                aliveMask[i] = filterMask(aliveMask[i], candBits[i][bestShift], tagBits[i][bestShift]);
            }

            usedShift[bestShift] = true;
            chosen.add(bestShift + 1); // shift is 1..maxShift

            // Report current quality
            int worstNow = 0;
            long sumNow = 0;
            int solved = 0;
            for (int i = 0; i < M; i++) {
                int cnt = Integer.bitCount(aliveMask[i]);
                if (cnt > worstNow) worstNow = cnt;
                sumNow += cnt;
                if (cnt == 1) solved++;
            }
            double avgNow = sumNow / (double) M;

            System.out.printf("Step %d: chose shift=%d  worst=%d  avg=%.3f  solved=%d/%d%n",
                    step + 1, bestShift + 1, worstNow, avgNow, solved, M);

            if (worstNow == 1) {
                System.out.println("Reached worst-case = 1 (unique for all sampled messages).");
                break;
            }
        }

        System.out.println("\nChosen shifts S = " + chosen);
        printHistogram(aliveMask);
        System.out.println("NOTE: This guarantee is empirical over the sample M=" + M +
                ". Increase M and/or test multiple keys for stronger confidence.");
    }

    // filters 9-bit mask by comparing candBits[k] to tagBit; returns new mask
    private static int filterMask(int mask, byte[] candBitsShift, byte tagBitShift) {
        int newMask = 0;
        for (int k = 0; k < 9; k++) {
            if (((mask >>> k) & 1) == 0) continue;
            if (candBitsShift[k] == tagBitShift) newMask |= (1 << k);
        }
        return newMask;
    }

    // (a + shift) mod n, where shift is small (<= maxShift). One subtract is enough.
    private static BigInteger addModSmall(BigInteger a, int shift, BigInteger n) {
        BigInteger x = a.add(BigInteger.valueOf(shift));
        if (x.compareTo(n) >= 0) x = x.subtract(n);
        return x;
    }

    private static BigInteger randomCoprime(BigInteger n, SecureRandom rnd) {
        BigInteger m;
        do {
            m = new BigInteger(n.bitLength(), rnd).mod(n);
        } while (m.signum() == 0 || !m.gcd(n).equals(BigInteger.ONE));
        return m;
    }

    private static void printHistogram(int[] aliveMask) {
        Map<Integer, Integer> hist = new TreeMap<>();
        for (int mask : aliveMask) {
            int cnt = Integer.bitCount(mask);
            hist.merge(cnt, 1, Integer::sum);
        }
        System.out.println("\nHistogram of remaining candidates after chosen shifts:");
        for (var e : hist.entrySet()) {
            System.out.printf("  %d candidates: %d%n", e.getKey(), e.getValue());
        }
    }
}
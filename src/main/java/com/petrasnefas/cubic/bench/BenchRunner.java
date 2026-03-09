package com.petrasnefas.cubic.bench;

import com.petrasnefas.cubic.core.*;
import com.petrasnefas.cubic.tags.CubicDecryptor;
import com.petrasnefas.cubic.tags.GPZ;
import com.petrasnefas.cubic.tags.JZ;
import com.petrasnefas.cubic.tags.JZ18Tag;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public final class BenchRunner {

    public static void run(int nBits, int warmup, int iters) {
        SecureRandom rnd = new SecureRandom();

        System.out.println("KeyGen nBits=" + nBits);
        KeyPair kp = CubicKeyGen.generateKeyPair(nBits, rnd);
        PublicKey pk = kp.pk();
        PrivateKey sk = kp.sk();
        BigInteger n = pk.n();

        // Warmup
        for (int i = 0; i < warmup; i++) {
            BigInteger m = randomCoprime(n, rnd);

            var ct1 = GPZ.encrypt(m, pk);
            CubicDecryptor.decryptGPZ(sk, ct1);

            var ct2 = JZ.encrypt(m, pk);
            CubicDecryptor.decryptJZ(sk, ct2);
        }

        // Arrays for times (ns)
        long[] gpzEnc = new long[iters];
        long[] gpzDecAll = new long[iters];
        long[] gpzDis = new long[iters];
        long[] gpzTotal = new long[iters];

        long[] jzEnc = new long[iters];
        long[] jzDecAll = new long[iters];
        long[] jzDis = new long[iters];
        long[] jzTotal = new long[iters];

        int okG = 0, okJ = 0;

        for (int i = 0; i < iters; i++) {
            BigInteger m = randomCoprime(n, rnd);

            // --- GPZ ---
            long t0 = System.nanoTime();
            Ciphertext<BigInteger> ctG = GPZ.encrypt(m, pk);
            long t1 = System.nanoTime();
            DecryptResult drG = CubicDecryptor.decryptGPZ(sk, ctG);
            long t2 = System.nanoTime();

            gpzEnc[i] = t1 - t0;
            gpzDecAll[i] = drG.tDecryptAllNs();
            gpzDis[i] = drG.tDisambiguateNs();
            gpzTotal[i] = t2 - t0;
            if (m.equals(drG.m())) okG++;

            // --- JZ-18 ---
            long u0 = System.nanoTime();
            Ciphertext<JZ18Tag> ctJ = JZ.encrypt(m, pk);
            long u1 = System.nanoTime();
            DecryptResult drJ = CubicDecryptor.decryptJZ(sk, ctJ);
            long u2 = System.nanoTime();

            jzEnc[i] = u1 - u0;
            jzDecAll[i] = drJ.tDecryptAllNs();
            jzDis[i] = drJ.tDisambiguateNs();
            jzTotal[i] = u2 - u0;
            if (m.equals(drJ.m())) okJ++;

            if ((i + 1) % 50 == 0) {
                System.out.println("Progress " + (i + 1) + "/" + iters);
            }
        }

        System.out.println("\n=== RESULTS nBits=" + nBits + " iters=" + iters + " warmup=" + warmup + " ===");
        printStats("GPZ enc", gpzEnc);
        printStats("GPZ decAll", gpzDecAll);
        printStats("GPZ dis", gpzDis);
        printStats("GPZ total", gpzTotal);
        System.out.println("GPZ success: " + okG + "/" + iters);

        System.out.println();
        printStats("JZ-18 enc", jzEnc);
        printStats("JZ-18 decAll", jzDecAll);
        printStats("JZ-18 dis", jzDis);
        printStats("JZ-18 total", jzTotal);
        System.out.println("JZ-18 success: " + okJ + "/" + iters);
    }

    private static void printStats(String name, long[] a) {
        long med = percentile(a, 50);
        long p95 = percentile(a, 95);
        long mean = mean(a);
        System.out.printf("%-12s mean=%d ns  median=%d ns  p95=%d ns%n", name, mean, med, p95);
    }

    private static long mean(long[] a) {
        long s = 0;
        for (long v : a) s += v;
        return s / a.length;
    }

    private static long percentile(long[] a, int p) {
        long[] b = Arrays.copyOf(a, a.length);
        Arrays.sort(b);
        int idx = (int) Math.ceil((p / 100.0) * b.length) - 1;
        if (idx < 0) idx = 0;
        if (idx >= b.length) idx = b.length - 1;
        return b[idx];
    }

    private static BigInteger randomCoprime(BigInteger n, SecureRandom rnd) {
        BigInteger m;
        do {
            m = new BigInteger(n.bitLength(), rnd).mod(n);
        } while (m.signum() == 0 || !m.gcd(n).equals(BigInteger.ONE));
        return m;
    }
}
package com.petrasnefas.cubic.bench;



import com.petrasnefas.cubic.core.CubicKeyGen;
import com.petrasnefas.cubic.core.KeyPair;
import com.petrasnefas.cubic.core.PublicKey;
import com.petrasnefas.cubic.tags.Jacobi;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public final class JacobiMicrobench {

    public static void main(String[] args) {
        run(2048, 2_000, 1_000); // warmup=2000, iters=1000
    }

    public static void run(int nBits, int warmupIters, int iters) {
        SecureRandom rnd = new SecureRandom();

        System.out.println("KeyGen nBits=" + nBits);
        KeyPair kp = CubicKeyGen.generateKeyPair(nBits, rnd);
        PublicKey pk = kp.pk();
        BigInteger n = pk.n();

        // Warmup (kad JIT susišildytų)
        int dummy = 0;
        for (int i = 0; i < warmupIters; i++) {
            BigInteger a = new BigInteger(n.bitLength(), rnd).mod(n);
            dummy ^= Jacobi.jacobiReducedFast(a, n);
        }

        long[] times = new long[iters];

        for (int i = 0; i < iters; i++) {
            BigInteger a = new BigInteger(n.bitLength(), rnd).mod(n);

            long t0 = System.nanoTime();
            dummy ^= Jacobi.jacobiReducedFast(a, n);
            long t1 = System.nanoTime();

            times[i] = t1 - t0;
        }

        System.out.println("dummy=" + dummy); // kad JVM neišmestų ciklo

        printStats("JacobiReducedFast", times);
    }

    private static void printStats(String name, long[] a) {
        long mean = mean(a);
        long med = percentile(a, 50);
        long p95 = percentile(a, 95);
        System.out.printf("%s: mean=%d ns  median=%d ns  p95=%d ns%n", name, mean, med, p95);
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
}
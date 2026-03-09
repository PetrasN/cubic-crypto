package com.petrasnefas.cubic;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class RsaSpeedCompare {

    private static final SecureRandom RNG = new SecureRandom();
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.TWO;

    public static void main(String[] args) {
        int nBits = 2048;
        int warmup = 300;
        int iters = 3000;

        // Standartinis RSA e
        BigInteger e = BigInteger.valueOf(65537);

        // 1) Generuojam raktą
        BigInteger p, q, n, phi, d;
        while (true) {
            p = BigInteger.probablePrime(nBits / 2, RNG);
            q = BigInteger.probablePrime(nBits / 2, RNG);
            n = p.multiply(q);
            phi = p.subtract(ONE).multiply(q.subtract(ONE));
            if (phi.gcd(e).equals(ONE)) {
                d = e.modInverse(phi);
                break;
            }
        }

        // RSA-CRT parametrai
        BigInteger dp = d.mod(p.subtract(ONE));
        BigInteger dq = d.mod(q.subtract(ONE));
        BigInteger qInv = q.modInverse(p); // q^{-1} mod p

        // Atsitiktinis m < n
        BigInteger m = randomUniformBelow(n.subtract(ONE));
        BigInteger c = m.modPow(e, n);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            BigInteger cc = m.modPow(e, n);
            BigInteger mm1 = cc.modPow(d, n);
            BigInteger mm2 = rsaDecryptCRT(cc, p, q, dp, dq, qInv);
            if (!mm1.equals(m) || !mm2.equals(m)) throw new RuntimeException("Mismatch");
        }

        long[] tEnc = new long[iters];
        long[] tDec = new long[iters];
        long[] tDecCrt = new long[iters];

        for (int i = 0; i < iters; i++) {
            long s1 = System.nanoTime();
            BigInteger cc = m.modPow(e, n);
            tEnc[i] = System.nanoTime() - s1;

            long s2 = System.nanoTime();
            BigInteger mm1 = c.modPow(d, n);
            tDec[i] = System.nanoTime() - s2;

            long s3 = System.nanoTime();
            BigInteger mm2 = rsaDecryptCRT(c, p, q, dp, dq, qInv);
            tDecCrt[i] = System.nanoTime() - s3;

            if (!mm1.equals(m) || !mm2.equals(m)) throw new RuntimeException("Mismatch");
        }

        Arrays.sort(tEnc);
        Arrays.sort(tDec);
        Arrays.sort(tDecCrt);

        long medEnc = tEnc[iters / 2];
        long medDec = tDec[iters / 2];
        long medDecCrt = tDecCrt[iters / 2];

        System.out.printf("RSA encrypt (e=65537) median: %d ns (%.3f ms)%n", medEnc, medEnc / 1e6);
        System.out.printf("RSA decrypt (plain)  median: %d ns (%.3f ms)%n", medDec, medDec / 1e6);
        System.out.printf("RSA decrypt (CRT)    median: %d ns (%.3f ms)%n", medDecCrt, medDecCrt / 1e6);
    }

    private static BigInteger rsaDecryptCRT(BigInteger c, BigInteger p, BigInteger q,
                                            BigInteger dp, BigInteger dq, BigInteger qInv) {
        // m1 = c^dp mod p
        BigInteger m1 = c.modPow(dp, p);
        // m2 = c^dq mod q
        BigInteger m2 = c.modPow(dq, q);

        // h = (m1 - m2) * qInv mod p
        BigInteger h = m1.subtract(m2).mod(p).multiply(qInv).mod(p);
        // m = m2 + q*h
        return m2.add(q.multiply(h));
    }

    private static BigInteger randomUniformBelow(BigInteger bound) {
        int bits = bound.bitLength();
        while (true) {
            BigInteger r = new BigInteger(bits, RNG);
            if (r.compareTo(bound) < 0) return r;
        }
    }
}

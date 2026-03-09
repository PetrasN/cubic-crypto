package com.petrasnefas.cubic.core;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class CubicKeyGen {
    private static final BigInteger THREE = BigInteger.valueOf(3);

    private CubicKeyGen() {}

    public static KeyPair generateKeyPair(int nBits, SecureRandom rnd) {
        int pBits = nBits / 2;
        int qBits = nBits - pBits;

        BigInteger p = randomPrimeMod1(pBits, rnd);

        // SVARBU: pirma sugeneruojam q, tik tada lyginam su p
        BigInteger q;
        do {
            q = randomPrimeMod1(qBits, rnd);
        } while (q.equals(p));

        return new KeyPair(new PublicKey(p.multiply(q)), new PrivateKey(p, q));
    }

    // Sugeneruoja pirminį p, kuriam p ≡ 1 (mod 3)
    private static BigInteger randomPrimeMod1(int bits, SecureRandom rnd) {
        while (true) {
            BigInteger p = BigInteger.probablePrime(bits, rnd);
            if (p.mod(THREE).equals(BigInteger.ONE)) {
                return p;
            }
        }
    }
}
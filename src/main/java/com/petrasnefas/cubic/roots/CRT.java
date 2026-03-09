package com.petrasnefas.cubic.roots;

import java.math.BigInteger;

public final class CRT {
    private CRT() {
    }
    public static BigInteger crt(BigInteger a, BigInteger p, BigInteger b, BigInteger q) {
        BigInteger n = p.multiply(q);
        BigInteger qInv = q.modInverse(p);
        BigInteger pInv = p.modInverse(q);

        BigInteger term1 = a.multiply(q).multiply(qInv).mod(n);
        BigInteger term2 = b.multiply(p).multiply(pInv).mod(n);
        return term1.add(term2).mod(n);
    }
}

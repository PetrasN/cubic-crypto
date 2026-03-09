package com.petrasnefas.cubic.core;

import java.math.BigInteger;

public class CubicEncryptor {
    private static final BigInteger THREE = BigInteger.valueOf(3);

    public static BigInteger encrypt(BigInteger m, PublicKey pk) {
        BigInteger n = pk.n();
        // m in [0, n-1]
        m = m.mod(n);
        return m.modPow(THREE, n);
    }
}

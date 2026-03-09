package com.petrasnefas.cubic.tags;

import com.petrasnefas.cubic.core.Ciphertext;
import com.petrasnefas.cubic.core.CubicEncryptor;
import com.petrasnefas.cubic.core.PublicKey;

import java.math.BigInteger;
import java.util.List;

public class GPZ {
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private GPZ(){}

    // tag = papildoma šifrograma cPlus = E(m+1)
    public static Ciphertext<BigInteger> encrypt(BigInteger m, PublicKey pk) {
        BigInteger n = pk.n();
        BigInteger c = CubicEncryptor.encrypt(m, pk);
        BigInteger cPlus = m.add(BigInteger.ONE).mod(n).modPow(THREE, n); // E(m+1)
        return new Ciphertext<>(c, cPlus);
    }

    // Atrenka tą kandidatą mi, kuriam E(mi+1) == cPlus
    public static BigInteger selectCandidate(List<BigInteger> candidates, BigInteger cPlus, BigInteger n) {
        for (BigInteger mi : candidates) {
            BigInteger check = mi.add(BigInteger.ONE).mod(n).modPow(THREE, n);
            if (check.equals(cPlus)) return mi;
        }
        return null;
    }
}

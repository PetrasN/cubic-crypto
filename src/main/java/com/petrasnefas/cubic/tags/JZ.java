package com.petrasnefas.cubic.tags;

import com.petrasnefas.cubic.core.Ciphertext;
import com.petrasnefas.cubic.core.CubicEncryptor;
import com.petrasnefas.cubic.core.PublicKey;

import java.math.BigInteger;
import java.util.List;



import com.petrasnefas.cubic.core.Ciphertext;
import com.petrasnefas.cubic.core.CubicEncryptor;
import com.petrasnefas.cubic.core.PublicKey;

import java.math.BigInteger;

public final class JZ {
    private JZ(){}

    // JŽ-18: tag = (Jacobi(m+1,n), ..., Jacobi(m+18,n))
    public static Ciphertext<JZ18Tag> encrypt(BigInteger m, PublicKey pk) {
        BigInteger n = pk.n();
        BigInteger c = CubicEncryptor.encrypt(m, pk);

        byte[] seq = new byte[18];

        BigInteger x = m.add(BigInteger.ONE);
        if (x.compareTo(n) >= 0) x = x.subtract(n); // (m+1) mod n

        for (int i = 0; i < 18; i++) {
            int j = Jacobi.jacobiReducedFast(x, n);
            seq[i] = (byte) j;

            // x = (x + 1) mod n be mod()
            x = x.add(BigInteger.ONE);
            if (x.compareTo(n) >= 0) x = x.subtract(n);
        }

        return new Ciphertext<>(c, new JZ18Tag(seq));
    }

    // Atranka pagal 18 simbolių
    public static BigInteger selectCandidate(List<BigInteger> candidates, JZ18Tag tag, BigInteger n) {
        byte[] seq = tag.seq();
        int k = candidates.size();
        boolean[] alive = new boolean[k];
        int aliveCount = k;

        for (int i = 0; i < k; i++) alive[i] = true;

        // iš anksto pasiruošiam shift BigInteger (1..18)
        BigInteger[] shifts = new BigInteger[18];
        for (int i = 0; i < 18; i++) shifts[i] = BigInteger.valueOf(i + 1);

        for (int i = 0; i < 18 && aliveCount > 1; i++) {
            byte want = seq[i];
            BigInteger add = shifts[i];

            for (int j = 0; j < k; j++) {
                if (!alive[j]) continue;

                BigInteger mi = candidates.get(j);
                BigInteger x = mi.add(add);
                if (x.compareTo(n) >= 0) x = x.subtract(n);
                int jac = Jacobi.jacobiReducedFast(x, n);
                if ((byte) jac != want) {
                    alive[j] = false;
                    aliveCount--;
                    if (aliveCount == 1) break;
                }
            }
        }

        if (aliveCount != 1) return null;
        for (int j = 0; j < k; j++) if (alive[j]) return candidates.get(j);
        return null;
    }
}

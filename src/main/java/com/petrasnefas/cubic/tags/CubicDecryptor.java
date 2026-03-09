package com.petrasnefas.cubic.tags;

import com.petrasnefas.cubic.core.Ciphertext;
import com.petrasnefas.cubic.core.DecryptResult;
import com.petrasnefas.cubic.core.PrivateKey;
import com.petrasnefas.cubic.roots.CubeRootsComposite;
import java.math.BigInteger;
import java.util.List;

public final class CubicDecryptor {
    private  CubicDecryptor() {
    }
    public static DecryptResult decryptGPZ(PrivateKey sk, Ciphertext<BigInteger> ct) {
        BigInteger p = sk.p();
        BigInteger q = sk.q();
        BigInteger n = sk.n();

        long t0 = System.nanoTime();
        List<BigInteger> candidates = CubeRootsComposite.cubeRootsModN(ct.c(), p, q);
        long t1 = System.nanoTime();

        long t2 = System.nanoTime();
        BigInteger m = GPZ.selectCandidate(candidates, ct.tag(), n);
        long t3 = System.nanoTime();

        return new DecryptResult(m, candidates, (t1 - t0), (t3 - t2));
    }

    public static DecryptResult decryptJZ(PrivateKey sk, Ciphertext<JZ18Tag> ct) {
        BigInteger p = sk.p();
        BigInteger q = sk.q();
        BigInteger n = sk.n();

        long t0 = System.nanoTime();
        List<BigInteger> candidates = CubeRootsComposite.cubeRootsModN(ct.c(), p, q);
        long t1 = System.nanoTime();

        long t2 = System.nanoTime();
        BigInteger m = JZ.selectCandidate(candidates, ct.tag(), n);
        long t3 = System.nanoTime();

        return new DecryptResult(m, candidates, (t1 - t0), (t3 - t2));
    }
}

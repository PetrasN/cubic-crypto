package com.petrasnefas.cubic.roots;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class CubeRootsComposite {
    private CubeRootsComposite(){}
    // 9 šaknys mod n = p*q
    public static List<BigInteger> cubeRootsModN(BigInteger c, BigInteger p, BigInteger q) {
        var Rp = CubeRootsPrime.cubeRootsModPrime(c, p);
        var Rq = CubeRootsPrime.cubeRootsModPrime(c, q);
        if (Rp.isEmpty() || Rq.isEmpty()) return List.of();

        List<BigInteger> out = new ArrayList<>(9);
        for (BigInteger a : Rp) {
            for (BigInteger b : Rq) {
                out.add(CRT.crt(a, p, b, q));
            }
        }
        return out;
    }
}

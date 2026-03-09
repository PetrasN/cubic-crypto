package com.petrasnefas.cubic.core;

import java.math.BigInteger;

public record PrivateKey(BigInteger p, BigInteger q) {
    public BigInteger n() {
        return p.multiply(q);
    }
}

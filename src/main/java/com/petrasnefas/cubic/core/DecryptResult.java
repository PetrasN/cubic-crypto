package com.petrasnefas.cubic.core;

import java.math.BigInteger;
import java.util.List;

public record DecryptResult(BigInteger m, List<BigInteger> candidates,long tDecryptAllNs, long tDisambiguateNs ) {
}

package com.petrasnefas.cubic.core;

import java.math.BigInteger;

public record Ciphertext<T>(BigInteger c,T tag) {
}

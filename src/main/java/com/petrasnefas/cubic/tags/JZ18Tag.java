package com.petrasnefas.cubic.tags;


public record JZ18Tag(byte[] seq) {
    public JZ18Tag {
        if (seq == null || seq.length != 18) {
            throw new IllegalArgumentException("JZ18Tag seq must have length 18");
        }
    }
}

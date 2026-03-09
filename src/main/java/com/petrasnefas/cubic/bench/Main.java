package com.petrasnefas.cubic.bench;

public final class Main {
    public static void main(String[] args) {
        // Parametrai:
        // nBits=2048 realiam raktui
        // M=400..2000 (pradžiai 400, paskui didinkite)
        // maxShift=64 (paieška poslinkių 1..64)
        // maxChosen=10 (kiek daugiausiai poslinkių leisti greedy pasirinkti)
        ShiftSearchRunner.runGreedySearch(2048, 1000, 256, 10);
    }
}
package com.petrasnefas.cubic.tests;

import com.petrasnefas.cubic.tags.Jacobi;
import com.petrasnefas.cubic.roots.CubeRootsComposite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

public class AlgorithmsBenchmark {

    static SecureRandom rnd = new SecureRandom();

    static final BigInteger ONE = BigInteger.ONE;
    static final BigInteger THREE = BigInteger.valueOf(3);

    static final int TRIALS = 1000;
    static final int MAX_JACOBI = 16;
    static final int BITS3 = 16;

    public static void main(String[] args) {

        benchmark(1024);
        benchmark(1536);
        benchmark(2048);

    }

    static void benchmark(int nBits) {

        System.out.println("\n=======================================");
        System.out.println("Benchmark | nBits = " + nBits);
        System.out.println("=======================================");

        long t1=0,t2=0,t3=0;

        for(int trial=0; trial<TRIALS; trial++){

            BigInteger p = randomPrimeMod1(nBits/2);
            BigInteger q = randomPrimeMod1(nBits/2);

            BigInteger n = p.multiply(q);

            BigInteger m;

            do{
                m = new BigInteger(nBits-2,rnd);
            } while(!m.gcd(n).equals(ONE));

            BigInteger c = m.modPow(THREE,n);
            BigInteger c2 = m.add(ONE).modPow(THREE,n);

            List<BigInteger> roots =
                    CubeRootsComposite.cubeRootsModN(c,p,q);

            // ---------- Algoritmas 1 ----------
            long s1 = System.nanoTime();

            int[] jac = new int[MAX_JACOBI];
            for(int i=0;i<MAX_JACOBI;i++)
                jac[i] = Jacobi.jacobiReducedFast(m.add(BigInteger.valueOf(i+1)),n);

            List<BigInteger> candidates = roots;

            for(int t=0; t<MAX_JACOBI && candidates.size()>1; t++){

                int target = jac[t];

                int finalT = t;
                candidates.removeIf(r ->
                        Jacobi.jacobiReducedFast(r.add(BigInteger.valueOf(finalT +1)),n)!=target
                );

            }

            long e1 = System.nanoTime();
            t1 += (e1-s1);

            // ---------- Algoritmas 2 ----------

            long s2 = System.nanoTime();

            List<BigInteger> roots2 =
                    CubeRootsComposite.cubeRootsModN(c2,p,q);

            outer:
            for(BigInteger r:roots){
                for(BigInteger r2:roots2){

                    if(r.add(ONE).mod(n).equals(r2)){
                        break outer;
                    }

                }
            }

            long e2 = System.nanoTime();
            t2 += (e2-s2);

            // ---------- Algoritmas 3 ----------

            long s3 = System.nanoTime();

            BigInteger mask =
                    BigInteger.ONE.shiftLeft(BITS3).subtract(ONE);

            BigInteger bits = c2.and(mask);

            for(BigInteger r:roots){

                BigInteger test =
                        r.add(ONE).modPow(THREE,n);

                if(test.and(mask).equals(bits)){
                    break;
                }

            }

            long e3 = System.nanoTime();
            t3 += (e3-s3);

        }

        System.out.println("Algoritmas 1 avg ns: "+t1/TRIALS);
        System.out.println("Algoritmas 2 avg ns: "+t2/TRIALS);
        System.out.println("Algoritmas 3 avg ns: "+t3/TRIALS);

    }

    static BigInteger randomPrimeMod1(int bits){

        BigInteger p;

        do{
            p = BigInteger.probablePrime(bits,rnd);
        }
        while(!p.mod(BigInteger.valueOf(3)).equals(ONE));

        return p;

    }

}
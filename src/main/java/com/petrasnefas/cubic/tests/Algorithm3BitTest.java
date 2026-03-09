package com.petrasnefas.cubic.tests;

import com.petrasnefas.cubic.roots.CubeRootsComposite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

public class Algorithm3BitTest {

    static SecureRandom rnd = new SecureRandom();

    static final BigInteger ONE = BigInteger.ONE;
    static final BigInteger THREE = BigInteger.valueOf(3);

    static final int NBITS = 2048;
    static final int TRIALS = 10000;

    static final int[] BIT_TEST = {4,6,8,10,12,14,16,18,20};

    public static void main(String[] args) {

        for(int k : BIT_TEST){

            int unique = 0;
            int ambiguous = 0;

            for(int trial=0; trial<TRIALS; trial++){

                BigInteger p = randomPrimeMod1(NBITS/2);
                BigInteger q = randomPrimeMod1(NBITS/2);

                BigInteger n = p.multiply(q);

                BigInteger m;

                do{
                    m = new BigInteger(NBITS-2,rnd);
                } while(!m.gcd(n).equals(ONE));

                BigInteger c = m.modPow(THREE,n);

                BigInteger c2 = m.add(ONE).modPow(THREE,n);

                BigInteger mask = BigInteger.ONE.shiftLeft(k).subtract(ONE);

                BigInteger bits = c2.and(mask);

                List<BigInteger> roots =
                        CubeRootsComposite.cubeRootsModN(c,p,q);

                int matches = 0;

                for(BigInteger r : roots){

                    BigInteger test =
                            r.add(ONE).modPow(THREE,n);

                    if(test.and(mask).equals(bits)){
                        matches++;
                    }
                }

                if(matches==1) unique++;
                else ambiguous++;
            }

            System.out.println("bits="+k+
                    " unique="+unique+
                    " ambiguous="+ambiguous+
                    " rate="+(double)unique/TRIALS);
        }
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
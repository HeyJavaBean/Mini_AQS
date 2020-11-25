package com.imlehr.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Lehr
 * @create: 2020-09-19
 */
public class CvTest {

    public static void main(String[] args) throws InterruptedException {

        boolean a = isHeldExclusively();
        System.out.println(a);


    }

    protected static boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }


}

package com.imlehr.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Lehr
 * @create: 2020-09-19
 */
public class test {

    public static void main(String[] args) throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();

        //LehrReentrantLock lock = new LehrReentrantLock();

        Runnable r1 = ()->
        {

            int time = 1;
            try{

                System.err.println(Thread.currentThread().getName()+":Lock Ready!");
                lock.lock();
                System.err.println(Thread.currentThread().getName()+":Locked!");

                TimeUnit.SECONDS.sleep(time);

                System.err.println(Thread.currentThread().getName()+":Locked Twice Ready!");
                lock.lock();
                System.err.println(Thread.currentThread().getName()+":Locked Twice Done!");

                TimeUnit.SECONDS.sleep(time);

                System.err.println(Thread.currentThread().getName()+":Release 1 ready");
                lock.unlock();
                System.err.println(Thread.currentThread().getName()+":Release 1 done");

                TimeUnit.SECONDS.sleep(time);

                System.err.println(Thread.currentThread().getName()+":Release 2 ready");
                lock.unlock();
                System.err.println(Thread.currentThread().getName()+":Release 2 done");

            }catch (Exception e)
            {

            }

        };
        Thread t1 = new Thread(r1,"No.1");
        Thread t2 = new Thread(r1,"No.2");
        Thread t3 = new Thread(r1,"No.3");
        Thread t4 = new Thread(r1,"No.4");
        Thread t5 = new Thread(r1,"No.5");
        Thread t6 = new Thread(r1,"No.6");
        Thread t7 = new Thread(r1,"No.7");




        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        t6.join();
        t7.join();

        System.out.println("dooooneeeee!");







    }


}

package com.imlehr.test;

import com.imlehr.aqs.ReentrantLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;

/**
 * @author Lehr
 * @create: 2020-09-19
 */
public class MonitorTest {

    private ReentrantLock lock = new ReentrantLock();
    private List listQueue = new ArrayList();//存储消息的集合
    private Condition notNull = lock.newCondition();//队列不为空
    private Condition notFull = lock.newCondition();//队列不为满


    public void add(String message) {
        lock.lock();//操作队列先加锁
        try {
            //队列满了，通知消费者线程，生产线程阻塞
            while (listQueue.size() >= 10) {
                notNull.signal();
                System.out.println("队列已满" + Thread.currentThread().getName() + "等待");
                notFull.await();
            }

            //往队列添加一条消息，同时通知消费者有新消息了
            listQueue.add(message);
            System.out.println(Thread.currentThread().getName() + "生产一条消息");
            notNull.signal();//通知消费者线程
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();//释放锁
        }
    }


    public void remove() {
        lock.lock();//操作队列先加锁
        try {
            //队列空了，通知生产线程，消费线程阻塞
            while (listQueue.size() == 0) {
                System.out.println("队列已空" + Thread.currentThread().getName() + "等待");
                notNull.await();
            }
            //队列删除一条消息，同时通知生产者队列有位置了
            listQueue.get(0);
            listQueue.remove(0);
            System.out.println(Thread.currentThread().getName() + "消费一条消息");
            notFull.signal();//同时通知生产者队列

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


    public static void main(String[] args) throws InterruptedException {
        MonitorTest myQueue = new MonitorTest();

        for (int i = 1; i <= 10; i++) {
            Thread provider = new Thread(() ->
            {
                while (true) {
                    myQueue.add("消息");
                }
            }, "生产线程" + i);


            Thread consumer = new Thread(() -> {
                while (true) {
                    myQueue.remove();
                }
            }, "消费线程" + i);
            provider.start();
            consumer.start();
        }

    }


}




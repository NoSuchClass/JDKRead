package com.bitc.JDK.lang.test;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadIdTest {
    // 用来存储下一个线程ID的原子整数
    private static final AtomicInteger nextId = new AtomicInteger(0);

    // 包含每个线程ID的线程局部变量
    private static final ThreadLocal<Integer> threadId =
            new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return nextId.getAndIncrement();
                }
            };

    // 在需要时分配并返回当前线程的唯一ID
    public static int get() {
        return threadId.get();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Integer name = ThreadIdTest.get();
                    Thread.currentThread().setName("Thread-" + name + " this is now");
                    System.out.println(Thread.currentThread().getName());
                }
            }).start();
        }
    }
}

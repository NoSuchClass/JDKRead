package com.bitc.JDK.lang.test;

public class ThreadLocalNoOOM {
    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    TestClass t = new TestClass(i);
                    t.printId();
                    // remove方法会将referent和value都被设置为null
                    t.threadLocal.remove();
                }
            }
        });
        thread.start();
    }

    static class TestClass {
        private int id;
        private int[] arr;
        private ThreadLocal<TestClass> threadLocal;

        TestClass(int id) {
            this.id = id;
            arr = new int[1000000];
            threadLocal = new ThreadLocal<>();
            threadLocal.set(this);
        }

        public void printId() {
            System.out.println(threadLocal.get().id);
        }
    }
}

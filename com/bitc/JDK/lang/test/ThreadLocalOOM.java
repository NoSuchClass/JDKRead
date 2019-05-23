package com.bitc.JDK.lang.test;

public class ThreadLocalOOM {

    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100000; i++) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    TestClass t = new TestClass(i);
                    t.printId();
                    // 这儿虽然设置为了null，但仅仅只是让t这个引用不能够到达这个线程中TestClass
                    // 的实例对象地址（垃圾回收的算法：可达性分析），但实际上还有一条能够通往这个
                    // 内存地址的路径：Thread对象->对象持有的threadLocals->ThreadLocalMap中的
                    // Entry[] table中Entry实例存储的key value->value中的this指向TestClass实例对象地址
                    t = null;
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

package com.bitc.JDK.util.test;

import com.bitc.JDK.util.function.Supplier;

import java.util.Arrays;
import java.util.List;

public class SupplierTest {
    public void main(String[] args) {
/*        final Car car = Car.create(Car::new);
        final List<Car> cars = Arrays.asList(car);
        cars.forEach(Car::collide);
        cars.forEach(Car::repair);
        final Car police = Car.create(Car::new);
        cars.forEach(police::follow);*/
        System.out.println();
    }

    static class Car {
        public Car() {

        }

        //Supplier是jdk1.8的接口，这里和lamda一起使用了
        public static Car create(final Supplier<Car> supplier) {
            return supplier.get();
        }

        public static void collide(final Car car) {
            System.out.println("Collided " + car.toString());
        }

        public void follow(final Car another) {
            System.out.println("Following the " + another.toString());
        }

        public void repair() {
            System.out.println("Repaired " + this.toString());
        }
    }
}

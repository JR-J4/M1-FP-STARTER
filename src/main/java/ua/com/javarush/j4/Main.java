package ua.com.javarush.j4;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point. Builds the object graph via the composition root and hands it to
 * the picocli front end, which parses arguments and runs the requested command.
 * Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException {

        ArrayList stringList = new ArrayList();
        stringList.add("abc"); //додаємо рядок до списку
        stringList.add("abc"); //додаємо рядок до списку
        stringList.add(1); //додаємо число до списку

        Object o1 = stringList.get(1);


        for (Object o : stringList) {

        }

//
//        ArrayList<String> stringList1 = new ArrayList();
//
//        String s = stringList1.get(1);


        Zoo<Cat> catZoo = new Zoo(Cat.class);
        Zoo<Dog> dogZoo = new Zoo(Dog.class);

        Cat newAnimal = catZoo.createNewAnimal();


        AnimalByName<Cat> animalByName = new AnimalByName<>();

        animalByName.put("Bob", new Cat());

        Cat bob = animalByName.get("Bob");

        Dog newAnimal1 = dogZoo.createNewAnimal();

        System.out.println(newAnimal);

        List<Animal> animals = List.of(new Animal());


        List<Dog> dog = List.of(new Dog());

        List<Cat> cats = List.of(new Cat());

        test(cats);

        new Composition().cli().run(args);
    }



    public static void test(List<? super Cat> animals){


    }
}

package ua.com.javarush.j4;

import java.util.ArrayList;

class Zoo<T> {
  Class<T> clazz;
  ArrayList<T> animals = new ArrayList<T>();

  Zoo(Class<T> clazz){
    this.clazz = clazz;
  }
  public T createNewAnimal() throws InstantiationException, IllegalAccessException {
    T animal = clazz.newInstance();
    animals.add(animal);
    return animal;
  }
}

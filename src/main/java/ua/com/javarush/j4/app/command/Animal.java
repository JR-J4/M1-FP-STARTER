package ua.com.javarush.j4.app.command;

import java.util.Objects;

public class Animal implements Cloneable {
  private long id;
  private String name;
  private int age;
  private Animal bestFriend;

  public Animal(long id, String name, int age, Animal bestFriend) {
    this.id = id;
    this.name = name;
    this.age = age;
    this.bestFriend = bestFriend;

  }

  @Override
  public Animal clone() throws CloneNotSupportedException {

    Animal friend = new Animal(bestFriend.id, bestFriend.name, bestFriend.age, bestFriend.clone());

    return new Animal(id, name, age, friend);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;

    Animal animal = (Animal) o;
    return id == animal.id && age == animal.age && Objects.equals(name, animal.name) && Objects.equals(bestFriend, animal.bestFriend);
  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(id);
    result = 31 * result + Objects.hashCode(name);
    result = 31 * result + age;
    result = 31 * result + Objects.hashCode(bestFriend);
    return result;
  }
}

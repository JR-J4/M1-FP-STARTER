package ua.com.javarush.j4;

import java.util.AbstractMap;
import java.util.Set;

public class AnimalByName<A extends Animal> extends AbstractMap<String, A> {

  @Override
  public Set<Entry<String, A>> entrySet() {
    return Set.of();
  }
}

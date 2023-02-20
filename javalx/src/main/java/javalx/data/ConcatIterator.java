package javalx.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A generic {@link Iterator} over more {@link Iterable}s...
 *
 * @author Gero
 * @param <T> The type of the iterated value
 */
public class ConcatIterator<T> implements Iterator<T> {
  private final List<Iterator<T>> arr;
  private final Iterator<Iterator<T>> arrIterator;
  private Iterator<T> currIterator = null;


  public <TX extends Iterable<T>> ConcatIterator (Collection<TX> iterables) {
    arr = new ArrayList<Iterator<T>>(iterables.size());
    for (Iterable<T> iterable : iterables) {
      arr.add(iterable.iterator());
    }
    arrIterator = arr.iterator();
  }

  @Override public boolean hasNext () {
    if (currIterator != null && currIterator.hasNext()) {
      return true;
    }
    while (arrIterator.hasNext()) {
      currIterator = arrIterator.next();
      if (currIterator.hasNext()) {
        return true;
      }
    }
    return false;
  }

  @Override public T next () {
    if (currIterator != null && currIterator.hasNext()) {
      return currIterator.next();
    }
    while (arrIterator.hasNext()) {
      currIterator = arrIterator.next();
      if (currIterator.hasNext()) {
        return currIterator.next();
      }
    }
    return null;
  }

  @Override public void remove () {
    if (currIterator != null && currIterator.hasNext()) {
      currIterator.remove();
    }
    while (arrIterator.hasNext()) {
      currIterator = arrIterator.next();
      if (currIterator.hasNext()) {
        currIterator.remove();
        return;
      }
    }
  }
}
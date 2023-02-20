package javalx.persistentcollections;

import javalx.data.Option;
import javalx.data.products.P2;

/**
 * An immutable ordered map.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 * @param <M> The self type of the map
 */
public interface OrderedMap<K, V, M extends OrderedMap<K, V, M>> extends FiniteMap<K, V, M> {

  public M removeMin ();

  public M removeMax ();

  public Option<P2<K, V>> getMin ();

  public Option<P2<K, V>> getMax ();


}

package javalx.benchmark.concrete;

import javalx.benchmark.BenchmarkInsertAndRemove;
import javalx.persistentcollections.IntMap;


public class BenchmarkIntMapInsertAndRemove {
  public static void main (String[] args) {
    BenchmarkInsertAndRemove.run("IntMap", IntMap.<Integer>empty());
  }
}

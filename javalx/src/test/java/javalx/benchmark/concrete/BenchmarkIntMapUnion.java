package javalx.benchmark.concrete;

import javalx.benchmark.BenchmarkUnion;
import javalx.persistentcollections.IntMap;

public class BenchmarkIntMapUnion {
  public static void main (String[] args) {
    BenchmarkUnion.run("IntMap", IntMap.<Integer>empty());
  }
}

package javalx.benchmark.concrete;

import javalx.benchmark.BenchmarkInsertAndRemove;
import javalx.persistentcollections.AVLMap;

public class BenchmarkAVLMapInsertAndRemove {
  public static void main (String[] args) {
    BenchmarkInsertAndRemove.run("AVLMap", AVLMap.<Integer, Integer>empty());
  }
}

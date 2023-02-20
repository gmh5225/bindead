package javalx.benchmark.concrete;

import javalx.benchmark.BenchmarkUnion;
import javalx.persistentcollections.AVLMap;

public class BenchmarkAVLMapUnion {
  public static void main (String[] args) {
    BenchmarkUnion.run("AVLMap", AVLMap.<Integer, Integer>empty());
  }
}

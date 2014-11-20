package cn.dreampie;

import java.io.File;

import static org.junit.Assert.*;

public class CoffeeCompilerTest {

  @org.junit.Test
  public void testCompile() throws Exception {
    File input = new File(getClass().getResource("/test.coffee").getPath());
    CoffeeSource coffeeSource = new CoffeeSource(input);
    CoffeeCompiler coffeeCompiler = new CoffeeCompiler();
    coffeeCompiler.setCompress(true);

    File output = new File(input.getAbsolutePath().replace(".coffee", ".js"));
    coffeeCompiler.compile(coffeeSource, output);
  }
}
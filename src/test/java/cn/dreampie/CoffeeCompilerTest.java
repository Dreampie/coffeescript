package cn.dreampie;

import cn.dreampie.resource.CoffeeSource;

import java.io.File;

public class CoffeeCompilerTest {

  @org.junit.Test
  public void testCompile() throws Exception {
    File input = new File(getClass().getResource("/test.coffee").getPath());
    CoffeeSource coffeeSource = new CoffeeSource(input);
    CoffeeCompiler coffeeCompiler = new CoffeeCompiler();
    //only  support  jdk1.7
    coffeeCompiler.setCompress(true);

    File output = new File(input.getAbsolutePath().replace(".coffee", ".js"));
    coffeeCompiler.compile(coffeeSource, output);
  }
}
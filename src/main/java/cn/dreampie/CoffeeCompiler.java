/*
 * Copyright 2010 David Yeung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.dreampie;

import cn.dreampie.logging.CoffeeLogger;
import cn.dreampie.logging.CoffeeLoggerFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wangrenhui on 2014/7/11.
 */
public class CoffeeCompiler {
  private static CoffeeLogger logger = CoffeeLoggerFactory.getLogger(CoffeeSource.class);

  private URL coffeeJs = CoffeeCompiler.class.getClassLoader().getResource("/lib/coffee-script-1.7.1.min.js");
  private List<Option> optionArgs = Collections.emptyList();
  private String encoding = null;
  private Boolean compress = null;

  private Scriptable globalScope;
  private Options options;

  private static final int BUFFER_SIZE = 262144;
  private static final int BUFFER_OFFSET = 0;
  private CompilationLevel compilationLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;


  public CoffeeCompiler() {
    this(Collections.<Option>emptyList());
  }

  /**
   * Returns the COFFEE JavaScript file used by the compiler.
   * COMPILE_STRING
   *
   * @return The COFFEE JavaScript file used by the compiler.
   */
  public URL getCoffeeJs() {
    return coffeeJs;
  }

  /**
   * Sets the COFFEE JavaScript file used by the compiler.
   * Must be set before {@link #compile(java.io.File)}  } is called.
   *
   * @param coffeeJs COFFEE JavaScript file used by the compiler.
   */
  public synchronized void setCoffeeJs(URL coffeeJs) {
    this.coffeeJs = coffeeJs;
  }

  /**
   * Returns whether the compiler will compress the CSS.
   *
   * @return Whether the compiler will compress the CSS.
   */
  public boolean isCompress() {
    return (compress != null && compress.booleanValue()) ||
        optionArgs.contains(Option.COMPRESS);
  }

  /**
   * Sets the compiler to compress the CSS.
   * Must be set before {@link #init()} is called.
   *
   * @param compress If <code>true</code>, sets the compiler to compress the CSS.
   */
  public synchronized void setCompress(boolean compress) {
    this.compress = compress;
  }

  /**
   * Returns the character encoding used by the compiler when writing the output <code>File</code>.
   *
   * @return The character encoding used by the compiler when writing the output <code>File</code>.
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * Sets the character encoding used by the compiler when writing the output <code>File</code>.
   * If not set the platform default will be used.
   * Must be set before {@link #compile(java.io.File)} ()} is called.
   *
   * @param encoding character encoding used by the compiler when writing the output <code>File</code>.
   */
  public synchronized void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public CoffeeCompiler(List<Option> options) {
    this.optionArgs = options;
  }

  private void init() throws IOException {
    InputStream inputStream = null;
    if (coffeeJs == null) {
      inputStream = this.getClass().getResourceAsStream("/lib/coffee-script-1.7.1.min.js");
    } else
      inputStream = coffeeJs.openConnection().getInputStream();
    try {
      try {
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        try {
          Context context = Context.enter();
          context.setOptimizationLevel(-1); // Without this, Rhino hits a 64K bytecode limit and fails
          try {
            globalScope = context.initStandardObjects();
            globalScope.put("logger", globalScope, Context.toObject(logger, globalScope));
            context.evaluateReader(globalScope, reader, "coffee-script.js", 0, null);
          } finally {
            Context.exit();
          }
        } finally {
          reader.close();
        }
      } catch (UnsupportedEncodingException e) {
        logger.error("Failed to initialize Coffee compiler.", e);
        throw new Error(e); // This should never happen
      } finally {
        inputStream.close();
      }
    } catch (IOException e) {
      logger.error("Failed to initialize Coffee compiler.", e);
      throw new Error(e); // This should never happen
    }

  }

  public String compile(String coffeeScriptSource) throws CoffeeException, IOException {
    return compile(coffeeScriptSource, "<inline>");
  }

  public String compile(String coffeeScriptSource, String name) throws CoffeeException, IOException {
    if (globalScope == null) {
      init();
    }
    options = new Options(optionArgs);

    Context context = Context.enter();
    try {
      Scriptable compileScope = context.newObject(globalScope);
      compileScope.setParentScope(globalScope);
      compileScope.put("coffeeScriptSource", compileScope, coffeeScriptSource);
      try {

        return (String) context.evaluateString(compileScope, String.format("CoffeeScript.compile(coffeeScriptSource, %s);", options.toJavaScript()),
            name, 0, null);
      } catch (JavaScriptException e) {
        throw new CoffeeException(e);
      }
    } finally {
      Context.exit();
    }
  }


  /**
   * Compiles the COFFEE input <code>File</code> to CSS.
   *
   * @param input The COFFEE input <code>File</code> to compile.
   * @return The CSS.
   * @throws java.io.IOException If the COFFEE file cannot be read.
   */
  public String compile(File input) throws IOException, CoffeeException {
    return compile(input, input.getName());
  }

  /**
   * Compiles the COFFEE input <code>File</code> to CSS and writes it to the specified output <code>File</code>.
   *
   * @param input  The COFFEE input <code>File</code> to compile.
   * @param output The output <code>File</code> to write the CSS to.
   * @throws java.io.IOException If the COFFEE file cannot be read or the output file cannot be written.
   */
  public void compile(File input, File output) throws IOException, CoffeeException {
    this.compile(input, output, true);
  }

  /**
   * Compiles the COFFEE input <code>File</code> to CSS and writes it to the specified output <code>File</code>.
   *
   * @param input  The COFFEE input <code>File</code> to compile.
   * @param output The output <code>File</code> to write the CSS to.
   * @param force  'false' to only compile the COFFEE input file in case the COFFEE source has been modified (including imports) or the output file does not exists.
   * @throws java.io.IOException If the COFFEE file cannot be read or the output file cannot be written.
   */
  public void compile(File input, File output, boolean force) throws IOException, CoffeeException {
    if (force || !output.exists() || output.lastModified() < input.lastModified()) {
      String data = compile(input);
      writeToFile(output, data);
    }
  }

  public String compile(CoffeeSource input) throws CoffeeException, IOException {
    return compile(input.getNormalizedContent(), input.getName());
  }

  /**
   * Compiles the input <code>CoffeeSource</code> to CSS and writes it to the specified output <code>File</code>.
   *
   * @param input  The input <code>CoffeeSource</code> to compile.
   * @param output The output <code>File</code> to write the CSS to.
   * @throws java.io.IOException If the COFFEE file cannot be read or the output file cannot be written.
   */
  public void compile(CoffeeSource input, File output) throws IOException, CoffeeException {
    compile(input, output, true);
  }

  /**
   * Compiles the input <code>CoffeeSource</code> to CSS and writes it to the specified output <code>File</code>.
   *
   * @param input  The input <code>CoffeeSource</code> to compile.
   * @param output The output <code>File</code> to write the CSS to.
   * @param force  'false' to only compile the input <code>CoffeeSource</code> in case the COFFEE source has been modified (including imports) or the output file does not exists.
   * @throws java.io.IOException If the COFFEE file cannot be read or the output file cannot be written.
   */
  public void compile(CoffeeSource input, File output, boolean force) throws IOException, CoffeeException {
    if (force || (!output.exists() && output.createNewFile()) || output.lastModified() < input.getLastModified()) {
      String data = compile(input);
      writeToFile(output, data);
    }
  }

  public String compile(File input, String name) throws IOException, CoffeeException {
    String data = new CoffeeCompiler(optionArgs).compile(readSourceFrom(new FileInputStream(input)), name);
    return data;
  }


  public void compile(File input, File output, String name) throws IOException, CoffeeException {

    String data = new CoffeeCompiler(optionArgs).compile(readSourceFrom(new FileInputStream(input)), name);

    writeToFile(output, data);

  }

  private String readSourceFrom(InputStream inputStream) {
    final InputStreamReader streamReader = new InputStreamReader(inputStream);
    try {
      try {
        StringBuilder builder = new StringBuilder(BUFFER_SIZE);
        char[] buffer = new char[BUFFER_SIZE];
        int numCharsRead = streamReader.read(buffer, BUFFER_OFFSET, BUFFER_SIZE);
        while (numCharsRead >= 0) {
          builder.append(buffer, BUFFER_OFFSET, numCharsRead);
          numCharsRead = streamReader.read(buffer, BUFFER_OFFSET, BUFFER_SIZE);
        }
        return builder.toString();
      } finally {
        streamReader.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Option> readOptionsFrom(String... args) {
    optionArgs = new LinkedList<Option>();
    if (args != null) {
      if (args.length == 1 && args[0].equals("--bare")) {
        optionArgs.add(Option.BARE);
      }
    }
    return optionArgs;
  }


  private void writeToFile(File output, String data) throws IOException {
    String source = data;
    if (compress) {
      Compiler compiler = new Compiler();
      Result result = compiler.compile(getExterns(), Lists.newArrayList(SourceFile.fromCode(output.getName(), data)), getCompilerOptions());
      source = compiler.toSource();
      logger.debug(result.debugLog);
      for (JSError error : result.errors) {
        logger.error("Closure Minifier Error:  " + error.sourceName + "  Description:  " + error.description);
      }
      for (JSError warning : result.warnings) {
        logger.info("Closure Minifier Warning:  " + warning.sourceName + "  Description:  " + warning.description);
      }

      if (result.success) {
        try {
          FileUtils.writeStringToFile(output, source, encoding);
        } catch (IOException e) {
          throw new CoffeeException("Failed to write minified file to " + output, e);
        }
      } else {
        throw new CoffeeException("Closure Compiler Failed - See error messages on System.err");
      }
    } else {
      try {
        FileUtils.writeStringToFile(output, source, encoding);
      } catch (IOException e) {
        throw new CoffeeException("Failed to write file to " + output, e);
      }
    }
  }

  /**
   * Prepare options for the Compiler.
   */
  private CompilerOptions getCompilerOptions() {
    CompilationLevel level = null;
    try {
      level = this.compilationLevel;
    } catch (IllegalArgumentException e) {
      throw new CoffeeException("Compilation level is invalid", e);
    }

    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);

    return options;
  }

  /**
   * Externs are defined in the Closure documentations as:
   * External variables are declared in 'externs' files. For instance, the file may include
   * definitions for global javascript/browser objects such as window, document.
   * <p/>
   * This method sneaks into the CommandLineRunner class of the Closure command line tool
   * and pulls the default Externs there.  This class could be modified to instead look
   * somewhere more relevant to the project.
   */
  private List<SourceFile> getExterns() {
    try {
      return CommandLineRunner.getDefaultExterns();
    } catch (IOException e) {
      throw new CoffeeException("Unable to load default External variables Files. The files include definitions for global javascript/browser objects such as window, document.", e);
    }
  }

  public List<Option> getOptionArgs() {
    return optionArgs;
  }

  public void setOptionArgs(String... args) {
    this.optionArgs = readOptionsFrom(args);
  }
}

/*
 * Handlebars.java: https://github.com/jknack/handlebars.java
 * Apache License Version 2.0 http://www.apache.org/licenses/LICENSE-2.0
 * Copyright (c) 2012 Edgar Espina
 */
package com.github.jknack.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.yaml.snakeyaml.Yaml;

public class AbstractTest {

  public interface Task {
    void run() throws IOException;
  }

  @SuppressWarnings("serial")
  public static class Hash extends LinkedHashMap<String, Object> {

    public Hash $(final String name, final Object value) {
      put(name, value);
      return this;
    }
  }

  public void shouldCompileTo(final String template, final String data, final String expected)
      throws IOException {
    shouldCompileTo(template, data, expected, "");
  }

  /**
   * Normalizes spaces in the result and compares it with the expected value.
   *
   * @param template The template to compile.
   * @param data The context to apply.
   * @param expected The expected result.
   * @throws IOException If an I/O error occurs.
   */
  public void shouldCompileToNormalized(final String template, final Object data, final String expected)
      throws IOException {
    shouldCompileToNormalized(template, data, new Hash(), new Hash(), expected, "");
  }

  public void shouldCompileTo(final String template, final Object data, final String expected)
      throws IOException {
    shouldCompileTo(template, data, expected, "");
  }

  public void shouldCompileTo(
      final String template, final String context, final String expected, final String message)
      throws IOException {
    Object deserializedContext = context;
    if (deserializedContext != null) {
      deserializedContext = parseYaml(context);
    }
    shouldCompileTo(template, deserializedContext, expected, message);
  }

  public void shouldCompileTo(
      final String template, final Object context, final String expected, final String message)
      throws IOException {
    shouldCompileTo(template, context, new Hash(), expected, message);
  }

  public void shouldCompileTo(
      final String template, final Object context, final Hash helpers, final String expected)
      throws IOException {
    shouldCompileTo(template, context, helpers, expected, "");
  }

  public void shouldCompileTo(
      final String template, final String context, final Hash helpers, final String expected)
      throws IOException {
    shouldCompileTo(template, parseYaml(context), helpers, expected, "");
  }

  private Object parseYaml(String context) {
    return new Yaml().load(context);
  }

  public void shouldCompileTo(
      final String template,
      final String context,
      final Hash helpers,
      final String expected,
      final String message)
      throws IOException {
    shouldCompileTo(template, parseYaml(context), helpers, expected, message);
  }

  public void shouldCompileTo(
      final String template,
      final Object context,
      final Hash helpers,
      final String expected,
      final String message)
      throws IOException {
    shouldCompileTo(template, context, helpers, new Hash(), expected, message);
  }

  public void shouldCompileToWithPartials(
      final String template, final Object context, final Hash partials, final String expected)
      throws IOException {
    shouldCompileTo(template, context, new Hash(), partials, expected, "");
  }

  public void shouldCompileToWithPartials(
      final String template,
      final Object context,
      final Hash partials,
      final String expected,
      final String message)
      throws IOException {
    shouldCompileTo(template, context, new Hash(), partials, expected, message);
  }

  public void shouldCompileTo(
      final String template,
      final Object context,
      final Hash helpers,
      final Hash partials,
      final String expected,
      final String message)
      throws IOException {
    Template t = compile(template, helpers, partials);
    String result = t.apply(configureContext(context));
    assertEquals(expected, result, "'" + expected + "' should === '" + result + "': " + message);
  }

  /*
   * Normalizes spaces in the result and compares it with the expected value.
   *
   * @param template The template to compile.
   * @param context The context to apply.
   * @param helpers The helpers to use.
   * @param partials The partials to use.
   * @param expected The expected result.
   * @param message The message to display on failure.
   * @throws IOException If an I/O error occurs.
   */
  public void shouldCompileToNormalized(
      final String template,
      final Object context,
      final Hash helpers,
      final Hash partials,
      final String expected,
      final String message)
      throws IOException {
    Template t = compile(template, helpers, partials);
    String result = t.apply(configureContext(context));
    assertEquals(expected, normalizeSpaces(result), "'" + expected + "' should === '" + result + "': " + message);
  }

  /**
   * Helper to normalize narrow no-break space (U+202F) and regular non-breaking space (U+00A0)
   * characters to regular space (U+0020).
   */
  public String normalizeSpaces(String input) {
    return input.replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")  // optional: also collapses multiple spaces
                .trim();
  }

  protected Object configureContext(final Object context) {
    return context;
  }

  public Template compile(final String template) throws IOException {
    return compile(template, new Hash());
  }

  public Template compile(final String template, final Hash helpers) throws IOException {
    return compile(template, helpers, new Hash(), false);
  }

  public Template compile(final String template, final Hash helpers, final boolean stringParams)
      throws IOException {
    return compile(template, helpers, new Hash(), stringParams);
  }

  public Template compile(final String template, final Hash helpers, final Hash partials)
      throws IOException {
    return compile(template, helpers, partials, false);
  }

  public Template compile(
      final String template, final Hash helpers, final Hash partials, final boolean stringParams)
      throws IOException {
    MapTemplateLoader loader = new MapTemplateLoader();
    for (Entry<String, Object> entry : partials.entrySet()) {
      loader.define(entry.getKey(), (String) entry.getValue());
    }
    Handlebars handlebars = newHandlebars().with(loader);
    configure(handlebars);
    handlebars.setStringParams(stringParams);

    for (Entry<String, Object> entry : helpers.entrySet()) {
      final Object value = entry.getValue();
      final Helper<?> helper;
      if (!(value instanceof Helper)) {
        helper =
            new Helper<Object>() {
              @Override
              public Object apply(final Object context, final Options options) throws IOException {
                return value.toString();
              }
            };
      } else {
        helper = (Helper<?>) value;
      }
      handlebars.registerHelper(entry.getKey(), helper);
    }
    Template t = handlebars.compileInline(template);
    return t;
  }

  protected void configure(final Handlebars handlebars) {}

  protected Handlebars newHandlebars() {
    return new Handlebars();
  }

  public static final Object $ = new Object();

  public static Hash $(final Object... attributes) {
    Hash model = new Hash();
    for (int i = 0; i < attributes.length; i += 2) {
      model.$((String) attributes[i], attributes[i + 1]);
    }
    return model;
  }

  public void withJava(Predicate<Integer> predicate, Task task) throws IOException {
    if (predicate.test(Handlebars.Utils.javaVersion)) {
      task.run();
    }
  }
}

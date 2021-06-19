package sk.soliont.text.format;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import sk.soliont.text.format.NamedArgsMessageFormat;

class NamedArgsMessageFormatTest {
  @Test
  void namedArgumentsShouldBeResolved() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("Hello {name}. I am glad you enjoy {object}!");
    final Map<String, String> arguments = new HashMap<>();
    arguments.put("name", "user");
    arguments.put("object", "code");
    final String message = namedArgsMessageFormat.format(arguments);
    Assertions.assertEquals("Hello user. I am glad you enjoy code!", message);
  }
}

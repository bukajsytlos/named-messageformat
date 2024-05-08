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
  @Test
  void lotOfNamedArgumentsShouldBeResolved() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("{name1},{name2},{name3},{name4},{name5},{name6},{name7},{name8},{name9},{name10},{name11},{name12},{name13}!");
    final Map<String, String> arguments = new HashMap<>();
    arguments.put("name1", "user1");
    arguments.put("name2", "user2");
    arguments.put("name3", "user3");
    arguments.put("name4", "user4");
    arguments.put("name5", "user5");
    arguments.put("name6", "user6");
    arguments.put("name7", "user7");
    arguments.put("name8", "user8");
    arguments.put("name9", "user9");
    arguments.put("name10", "user10");
    arguments.put("name11", "user11");
    arguments.put("name12", "user12");
    arguments.put("name13", "user13");
    final String message = namedArgsMessageFormat.format(arguments);
    Assertions.assertEquals("user1,user2,user3,user4,user5,user6,user7,user8,user9,user10,user11,user12,user13!", message);
  }

  @Test
  void singleQuoteShouldNotBeHandled() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("I'm fine!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("I'm fine!", message);
  }

  @Test
  void doubleQuoteShouldNotBeHandled() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("I''m fine!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("I''m fine!", message);
  }

  @Test
  void singleQuoteFollowedByBraceShouldHandledAsRaw() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("Hello '{name}'!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("Hello {name}!", message);
  }

  @Test
  void singleQuoteFollowedByBraceShouldHandledAsRaw2() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("Hello '{'name}'!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("Hello {name}'!", message);
  }

  @Test
  void singleQuoteFollowedByBraceWithDoubleQuoteShouldHandledAsRawWithQuote() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("Hello '{name'''!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("Hello {name'!", message);
  }

  @Test
  void singleQuoteFollowedByBraceWithDoubleQuoteShouldHandledAsRawWithQuote3() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("Hello '{{name'''!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("Hello {{name'!", message);
  }

  @Test
  void singleQuoteFollowedByBraceWithDoubleQuoteShouldHandledAsRawWithQuote4() {
    final NamedArgsMessageFormat namedArgsMessageFormat = new NamedArgsMessageFormat("'{''''{name}!");
    final String message = namedArgsMessageFormat.format(null);
    Assertions.assertEquals("{''{name}!", message);
  }
}

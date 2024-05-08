package sk.soliont.text.format;

/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.AttributedCharacterIterator;
import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class NamedArgsMessageFormat extends Format {

  private static final long serialVersionUID = 5318008L;

  /**
   * Constructs a NamedArgsMessageFormat for the default
   * {@link Locale.Category#FORMAT FORMAT} locale and the
   * specified pattern.
   * The constructor first sets the locale, then parses the pattern and
   * creates a list of subformats for the format elements contained in it.
   * Patterns and their interpretation are specified in the
   * <a href="#patterns">class description</a>.
   *
   * @param pattern the pattern for this message format
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws NullPointerException     if {@code pattern} is
   *                                  {@code null}
   */
  public NamedArgsMessageFormat(String pattern) {
    this.locale = Locale.getDefault(Locale.Category.FORMAT);
    applyPattern(pattern);
  }

  /**
   * Constructs a NamedArgsMessageFormat for the specified locale and
   * pattern.
   * The constructor first sets the locale, then parses the pattern and
   * creates a list of subformats for the format elements contained in it.
   * Patterns and their interpretation are specified in the
   * <a href="#patterns">class description</a>.
   *
   * @param pattern the pattern for this message format
   * @param locale  the locale for this message format
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws NullPointerException     if {@code pattern} is
   *                                  {@code null}
   * @since 1.4
   */
  public NamedArgsMessageFormat(String pattern, Locale locale) {
    this.locale = locale;
    applyPattern(pattern);
  }

  /**
   * Sets the locale to be used when creating or comparing subformats.
   * This affects subsequent calls
   * <ul>
   * <li>to the {@link #applyPattern applyPattern}
   *     method if format elements specify
   *     a format type and therefore have the subformats created in the
   *     <code>applyPattern</code> method, as well as
   * <li>to the <code>format</code> and
   *     {@link #formatToCharacterIterator formatToCharacterIterator} methods
   *     if format elements do not specify a format type and therefore have
   *     the subformats created in the formatting methods.
   * </ul>
   * Subformats that have already been created are not affected.
   *
   * @param locale the locale to be used when creating or comparing subformats
   */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /**
   * Gets the locale that's used when creating or comparing subformats.
   *
   * @return the locale used when creating or comparing subformats
   */
  public Locale getLocale() {
    return locale;
  }


  /**
   * Sets the pattern used by this message format.
   * The method parses the pattern and creates a list of subformats
   * for the format elements contained in it.
   * Patterns and their interpretation are specified in the
   * <a href="#patterns">class description</a>.
   *
   * @param pattern the pattern for this message format
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws NullPointerException     if {@code pattern} is
   *                                  {@code null}
   */
  @SuppressWarnings("fallthrough") // fallthrough in switch is expected, suppress it
  public void applyPattern(String pattern) {
    StringBuilder[] segments = new StringBuilder[4];
    // Allocate only segments[SEG_RAW] here. The rest are
    // allocated on demand.
    segments[SEG_RAW] = new StringBuilder();

    int part = SEG_RAW;
    int formatNumber = 0;
    boolean inQuote = false;
    int braceStack = 0;
    maxOffset = -1;
    for (int i = 0; i < pattern.length(); ++i) {
      char ch = pattern.charAt(i);
      if (part == SEG_RAW) {
        if (ch == '\'') {
            if (i + 1 < pattern.length()) {
            if (inQuote) {
              if (pattern.charAt(i + 1) == '\'') {
                segments[part].append(ch);
                ++i;
              } else {
                inQuote = !inQuote;
              }
            } else {
              if (pattern.charAt(i + 1) == '{') {
                inQuote = !inQuote;
              } else {
                segments[part].append(ch);
              }
            }
          } else {
            if (!inQuote) {
              segments[part].append(ch);
            }
          }
        } else if (ch == '{' && !inQuote) {
          part = SEG_INDEX;
          if (segments[SEG_INDEX] == null) {
            segments[SEG_INDEX] = new StringBuilder();
          }
        } else {
          segments[part].append(ch);
        }
      } else {
        if (inQuote) {              // just copy quotes in parts
          segments[part].append(ch);
        } else {
          switch (ch) {
            case ',':
              if (part < SEG_MODIFIER) {
                if (segments[++part] == null) {
                  segments[part] = new StringBuilder();
                }
              } else {
                segments[part].append(ch);
              }
              break;
            case '{':
              ++braceStack;
              segments[part].append(ch);
              break;
            case '}':
              if (braceStack == 0) {
                part = SEG_RAW;
                makeFormat(formatNumber, segments);
                formatNumber++;
                // throw away other segments
                segments[SEG_INDEX] = null;
                segments[SEG_TYPE] = null;
                segments[SEG_MODIFIER] = null;
              } else {
                --braceStack;
                segments[part].append(ch);
              }
              break;
            case ' ':
              // Skip any leading space chars for SEG_TYPE.
              if (part != SEG_TYPE || segments[SEG_TYPE].length() > 0) {
                segments[part].append(ch);
              }
              break;
            case '\'':
              inQuote = true;
              // fall through, so we keep quotes in other parts
            default:
              segments[part].append(ch);
              break;
          }
        }
      }
    }
    if (braceStack == 0 && part != 0) {
      maxOffset = -1;
      throw new IllegalArgumentException("Unmatched braces in the pattern.");
    }
    this.pattern = segments[0].toString();
  }


  /**
   * Returns a pattern representing the current state of the message format.
   * The string is constructed from internal information and therefore
   * does not necessarily equal the previously applied pattern.
   *
   * @return a pattern representing the current state of the message format
   */
  public String toPattern() {
    // later, make this more extensible
    int lastOffset = 0;
    StringBuilder result = new StringBuilder();
    for (int i = 0; i <= maxOffset; ++i) {
      copyAndFixQuotes(pattern, lastOffset, offsets[i], result);
      lastOffset = offsets[i];
      result.append('{').append(argumentNames[i]);
      Format fmt = formats.get(argumentNames[i]);
      if (fmt == null) {
        // do nothing, string format
      } else if (fmt instanceof NumberFormat) {
        if (fmt.equals(NumberFormat.getInstance(locale))) {
          result.append(",number");
        } else if (fmt.equals(NumberFormat.getCurrencyInstance(locale))) {
          result.append(",number,currency");
        } else if (fmt.equals(NumberFormat.getPercentInstance(locale))) {
          result.append(",number,percent");
        } else if (fmt.equals(NumberFormat.getIntegerInstance(locale))) {
          result.append(",number,integer");
        } else {
          if (fmt instanceof DecimalFormat) {
            result.append(",number,").append(((DecimalFormat) fmt).toPattern());
          } else if (fmt instanceof ChoiceFormat) {
            result.append(",choice,").append(((ChoiceFormat) fmt).toPattern());
          }
        }
      } else if (fmt instanceof DateFormat) {
        int index;
        for (index = MODIFIER_DEFAULT; index < DATE_TIME_MODIFIERS.length; index++) {
          DateFormat df = DateFormat.getDateInstance(DATE_TIME_MODIFIERS[index],
              locale);
          if (fmt.equals(df)) {
            result.append(",date");
            break;
          }
          df = DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[index],
              locale);
          if (fmt.equals(df)) {
            result.append(",time");
            break;
          }
        }
        if (index >= DATE_TIME_MODIFIERS.length) {
          if (fmt instanceof SimpleDateFormat) {
            result.append(",date,").append(((SimpleDateFormat) fmt).toPattern());
          } else {
            // UNKNOWN
          }
        } else if (index != MODIFIER_DEFAULT) {
          result.append(',').append(DATE_TIME_MODIFIER_KEYWORDS[index]);
        }
      }
      result.append('}');
    }
    copyAndFixQuotes(pattern, lastOffset, pattern.length(), result);
    return result.toString();
  }


  /**
   * Sets the formats to use for the format elements in the
   * previously set pattern string.
   * The order of formats in <code>newFormats</code> corresponds to
   * the order of format elements in the pattern string.
   * <p>
   * If more formats are provided than needed by the pattern string,
   * the remaining ones are ignored. If fewer formats are provided
   * than needed, then only the first <code>newFormats.length</code>
   * formats are replaced.
   * <p>
   *
   * @param newFormats the new formats to use
   * @throws NullPointerException if <code>newFormats</code> is null
   */
  public void setFormats(Map<String, Format> newFormats) {
    formats = new LinkedHashMap<>(newFormats);
  }

  /**
   * Sets the format to use for the format elements within the
   * previously set pattern string that use the given argument
   * index.
   * The argument index is part of the format element definition and
   * represents an index into the <code>arguments</code> array passed
   * to the <code>format</code> methods or the result array returned
   * by the <code>parse</code> methods.
   * <p>
   * If the argument index is used for more than one format element
   * in the pattern string, then the new format is used for all such
   * format elements. If the argument index is not used for any format
   * element in the pattern string, then the new format is ignored.
   *
   * @param argumentName the argument index for which to use the new format
   * @param newFormat     the new format to use
   * @since 1.4
   */
  public void setFormatByArgumentName(String argumentName, Format newFormat) {
    formats.put(argumentName, newFormat);
  }

  /**
   * Gets the formats used for the values passed into
   * <code>format</code> methods or returned from <code>parse</code>
   * methods. The names of elements in the returned map
   * correspond to the argument names used in the previously set
   * pattern string.
   * <p>
   * If an argument name is used for more than one format element
   * in the pattern string, then the format used for the last such
   * format element is returned from the map. If an argument name
   * is not used for any format element in the pattern string, then
   * null is returned from the map.
   *
   * @return the formats used for the arguments within the pattern by name
   */
  public Map<String, Format> getFormatsByArgumentName() {
    return new LinkedHashMap<>(formats);
  }

  /**
   * Formats an array of objects and appends the <code>NamedArgsMessageFormat</code>'s
   * pattern, with format elements replaced by the formatted objects, to the
   * provided <code>StringBuffer</code>.
   * <p>
   * The text substituted for the individual format elements is derived from
   * the current subformat of the format element and the
   * <code>arguments</code> element at the format element's argument index
   * as indicated by the first matching line of the following table. An
   * argument is <i>unavailable</i> if <code>arguments</code> is
   * <code>null</code> or has fewer than argumentIndex+1 elements.
   *
   * <table class="plain">
   * <caption style="display:none">Examples of subformat,argument,and formatted text</caption>
   * <thead>
   *    <tr>
   *       <th scope="col">Subformat
   *       <th scope="col">Argument
   *       <th scope="col">Formatted Text
   * </thead>
   * <tbody>
   *    <tr>
   *       <th scope="row" style="text-weight-normal" rowspan=2><i>any</i>
   *       <th scope="row" style="text-weight-normal"><i>unavailable</i>
   *       <td><code>"{" + argumentIndex + "}"</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><code>null</code>
   *       <td><code>"null"</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><code>instanceof ChoiceFormat</code>
   *       <th scope="row" style="text-weight-normal"><i>any</i>
   *       <td><code>subformat.format(argument).indexOf('{') &gt;= 0 ?<br>
   *           (new NamedArgsMessageFormat(subformat.format(argument), getLocale())).format(argument) :
   *           subformat.format(argument)</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><code>!= null</code>
   *       <th scope="row" style="text-weight-normal"><i>any</i>
   *       <td><code>subformat.format(argument)</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal" rowspan=4><code>null</code>
   *       <th scope="row" style="text-weight-normal"><code>instanceof Number</code>
   *       <td><code>NumberFormat.getInstance(getLocale()).format(argument)</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><code>instanceof Date</code>
   *       <td><code>DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale()).format(argument)</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><code>instanceof String</code>
   *       <td><code>argument</code>
   *    <tr>
   *       <th scope="row" style="text-weight-normal"><i>any</i>
   *       <td><code>argument.toString()</code>
   * </tbody>
   * </table>
   * <p>
   * If <code>pos</code> is non-null, and refers to
   * <code>Field.ARGUMENT</code>, the location of the first formatted
   * string will be returned.
   *
   * @param arguments an array of objects to be formatted and substituted.
   * @param result    where text is appended.
   * @param pos       keeps track on the position of the first replaced argument
   *                  in the output string.
   * @return the string buffer passed in as {@code result}, with formatted
   * text appended
   * @throws IllegalArgumentException if an argument in the
   *                                  <code>arguments</code> array is not of the type
   *                                  expected by the format element(s) that use it.
   * @throws NullPointerException     if {@code result} is {@code null}
   */
  public final StringBuffer format(
      Map<String, Object> arguments, StringBuffer result,
      FieldPosition pos
  ) {
    return subformat(arguments, result, pos, null);
  }

  /**
   * Creates a NamedArgsMessageFormat with the given pattern and uses it
   * to format the given arguments. This is equivalent to
   * <blockquote>
   * <code>(new {@link #NamedArgsMessageFormat(String) NamedArgsMessageFormat}(pattern)).{@link #format(Map, StringBuffer, FieldPosition)} format}(arguments, new StringBuffer(), null).toString()</code>
   * </blockquote>
   *
   * @param pattern   the pattern string
   * @param arguments object(s) to format
   * @return the formatted string
   * @throws IllegalArgumentException if the pattern is invalid,
   *                                  or if an argument in the <code>arguments</code> array
   *                                  is not of the type expected by the format element(s)
   *                                  that use it.
   * @throws NullPointerException     if {@code pattern} is {@code null}
   */
  public static String format(String pattern, Map<String, Object> arguments) {
    NamedArgsMessageFormat temp = new NamedArgsMessageFormat(pattern);
    return temp.format(arguments);
  }


  // Overrides
  /**
   * Formats an array of objects and appends the <code>MessageFormat</code>'s
   * pattern, with format elements replaced by the formatted objects, to the
   * provided <code>StringBuffer</code>.
   * This is equivalent to
   * <blockquote>
   *     <code>{@link #format(Map, StringBuffer, FieldPosition)} ((Map) arguments, result, pos)</code>
   * </blockquote>
   *
   * @param arguments an array of objects to be formatted and substituted.
   * @param result where text is appended.
   * @param pos keeps track on the position of the first replaced argument
   *            in the output string.
   * @exception IllegalArgumentException if an argument in the
   *            <code>arguments</code> array is not of the type
   *            expected by the format element(s) that use it.
   * @exception NullPointerException if {@code result} is {@code null}
   */
  public final StringBuffer format(Object arguments, StringBuffer result,
                                   FieldPosition pos)
  {
    return subformat((Map<String, Object>) arguments, result, pos, null);
  }

  /**
   * Parses the string.
   *
   * <p>Caveats: The parse may fail in a number of circumstances.
   * For example:
   * <ul>
   * <li>If one of the arguments does not occur in the pattern.
   * <li>If the format of an argument loses information, such as
   *     with a choice format where a large number formats to "many".
   * <li>Does not yet handle recursion (where
   *     the substituted strings contain {n} references.)
   * <li>Will not always find a match (or the correct match)
   *     if some part of the parse is ambiguous.
   *     For example, if the pattern "{1},{2}" is used with the
   *     string arguments {"a,b", "c"}, it will format as "a,b,c".
   *     When the result is parsed, it will return {"a", "b,c"}.
   * <li>If a single argument is parsed more than once in the string,
   *     then the later parse wins.
   * </ul>
   * When the parse fails, use ParsePosition.getErrorIndex() to find out
   * where in the string the parsing failed.  The returned error
   * index is the starting offset of the sub-patterns that the string
   * is comparing with.  For example, if the parsing string "AAA {0} BBB"
   * is comparing against the pattern "AAD {0} BBB", the error index is
   * 0. When an error occurs, the call to this method will return null.
   * If the source is null, return an empty array.
   *
   * @param source the string to parse
   * @param pos    the parse position
   * @return an array of parsed objects
   * @throws NullPointerException if {@code pos} is {@code null}
   *                              for a non-null {@code source} string.
   */
  public Map<String, Object> parse(String source, ParsePosition pos) {
    if (source == null) {
      return Collections.emptyMap();
    }

    Map<String, Object> resultMap = new LinkedHashMap<>();

    int patternOffset = 0;
    int sourceOffset = pos.getIndex();
    ParsePosition tempStatus = new ParsePosition(0);
    String argumentName = "";
    for (int i = 0; i <= maxOffset; ++i) {
      // match up to format
      int len = offsets[i] - patternOffset;
      if (len == 0 || pattern.regionMatches(patternOffset,
          source, sourceOffset, len)) {
        sourceOffset += len;
        patternOffset += len;
      } else {
        pos.setErrorIndex(sourceOffset);
        return null; // leave index as is to signal error
      }

      // now use format
      if (!formats.containsKey(argumentName)) {   // string format
        // if at end, use longest possible match
        // otherwise uses first match to intervening string
        // does NOT recursively try all possibilities
        int tempLength = (i != maxOffset) ? offsets[i + 1] : pattern.length();

        int next;
        if (patternOffset >= tempLength) {
          next = source.length();
        } else {
          next = source.indexOf(pattern.substring(patternOffset, tempLength),
              sourceOffset);
        }

        if (next < 0) {
          pos.setErrorIndex(sourceOffset);
          return null; // leave index as is to signal error
        } else {
          String strValue = source.substring(sourceOffset, next);
          if (!strValue.equals("{" + argumentNames[i] + "}"))
            resultMap.put(argumentNames[i], source.substring(sourceOffset, next));
          sourceOffset = next;
        }
      } else {
        tempStatus.setIndex(sourceOffset);
        resultMap.put(argumentNames[i], formats.get(argumentName).parseObject(source, new ParsePosition(tempStatus.getIndex())));
        if (tempStatus.getIndex() == sourceOffset) {
          pos.setErrorIndex(sourceOffset);
          return null; // leave index as is to signal error
        }
        sourceOffset = tempStatus.getIndex(); // update
      }
    }
    int len = pattern.length() - patternOffset;
    if (len == 0 || pattern.regionMatches(patternOffset,
        source, sourceOffset, len)) {
      pos.setIndex(sourceOffset + len);
    } else {
      pos.setErrorIndex(sourceOffset);
      return null; // leave index as is to signal error
    }
    return resultMap;
  }

  /**
   * Parses text from the beginning of the given string to produce an object
   * array.
   * The method may not use the entire text of the given string.
   * <p>
   * See the {@link #parse(String, ParsePosition)} method for more information
   * on message parsing.
   *
   * @param source A <code>String</code> whose beginning should be parsed.
   * @return An <code>Object</code> array parsed from the string.
   * @throws ParseException if the beginning of the specified string
   *                        cannot be parsed.
   */
  public Map<String, Object> parse(String source) throws ParseException {
    ParsePosition pos = new ParsePosition(0);
    Map<String, Object> result = parse(source, pos);
    if (pos.getIndex() == 0)  // unchanged, returned object is null
      throw new ParseException("NamedArgsMessageFormat parse error!", pos.getErrorIndex());

    return result;
  }

  /**
   * Parses text from a string to produce an object array.
   * <p>
   * The method attempts to parse text starting at the index given by
   * <code>pos</code>.
   * If parsing succeeds, then the index of <code>pos</code> is updated
   * to the index after the last character used (parsing does not necessarily
   * use all characters up to the end of the string), and the parsed
   * object array is returned. The updated <code>pos</code> can be used to
   * indicate the starting point for the next call to this method.
   * If an error occurs, then the index of <code>pos</code> is not
   * changed, the error index of <code>pos</code> is set to the index of
   * the character where the error occurred, and null is returned.
   * <p>
   * See the {@link #parse(String, ParsePosition)} method for more information
   * on message parsing.
   *
   * @param source A <code>String</code>, part of which should be parsed.
   * @param pos    A <code>ParsePosition</code> object with index and error
   *               index information as described above.
   * @return An <code>Object</code> array parsed from the string. In case of
   * error, returns null.
   * @throws NullPointerException if {@code pos} is null.
   */
  public Object parseObject(String source, ParsePosition pos) {
    return parse(source, new ParsePosition(pos.getIndex()));
  }

  /**
   * Creates and returns a copy of this object.
   *
   * @return a clone of this instance.
   */
  public Object clone() {
    NamedArgsMessageFormat other = (NamedArgsMessageFormat) super.clone();

    // clone arrays. Can't do with utility because of bug in Cloneable
    other.formats = (LinkedHashMap<String, Format>) formats.clone(); // shallow clone
    for (Map.Entry<String, Format> stringFormatEntry : formats.entrySet()) {
      other.formats.put(stringFormatEntry.getKey(), (Format) stringFormatEntry.getValue().clone());
    }
    // for primitives or immutables, shallow clone is enough
    other.offsets = offsets.clone();
    other.argumentNames = argumentNames.clone();

    return other;
  }

  /**
   * Equality comparison between two message format objects
   */
  public boolean equals(Object obj) {
    if (this == obj)                      // quick check
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    NamedArgsMessageFormat other = (NamedArgsMessageFormat) obj;
    return (maxOffset == other.maxOffset
        && pattern.equals(other.pattern)
        && ((locale != null && locale.equals(other.locale))
        || (locale == null && other.locale == null))
        && Arrays.equals(offsets, other.offsets)
        && Arrays.equals(argumentNames, other.argumentNames)
        && Objects.equals(formats, other.formats));
  }

  /**
   * Generates a hash code for the message format object.
   */
  public int hashCode() {
    return pattern.hashCode(); // enough for reasonable distribution
  }


  /**
   * Defines constants that are used as attribute keys in the
   * <code>AttributedCharacterIterator</code> returned
   * from <code>NamedArgsMessageFormat.formatToCharacterIterator</code>.
   *
   * @since 1.4
   */
  public static class Field extends Format.Field {

    // Proclaim serial compatibility with 1.4 FCS
    private static final long serialVersionUID = 7899943957617360810L;

    /**
     * Creates a Field with the specified name.
     *
     * @param name Name of the attribute
     */
    protected Field(String name) {
      super(name);
    }

    /**
     * Resolves instances being deserialized to the predefined constants.
     *
     * @return resolved NamedArgsMessageFormat.Field constant
     * @throws InvalidObjectException if the constant could not be
     *                                resolved.
     */
    protected Object readResolve() throws InvalidObjectException {
      if (this.getClass() != Field.class) {
        throw new InvalidObjectException("subclass didn't correctly implement readResolve");
      }

      return ARGUMENT;
    }

    //
    // The constants
    //

    /**
     * Constant identifying a portion of a message that was generated
     * from an argument passed into <code>formatToCharacterIterator</code>.
     * The value associated with the key will be an <code>Integer</code>
     * indicating the index in the <code>arguments</code> array of the
     * argument from which the text was generated.
     */
    public static final Field ARGUMENT =
        new Field("message argument field");
  }

  // ===========================privates============================

  /**
   * The locale to use for formatting numbers and dates.
   *
   * @serial
   */
  private Locale locale;

  /**
   * The string that the formatted values are to be plugged into.  In other words, this
   * is the pattern supplied on construction with all of the {} expressions taken out.
   *
   * @serial
   */
  private String pattern = "";

  /**
   * The initially expected number of subformats in the format
   */
  private static final int INITIAL_FORMATS = 10;

  /**
   * An array of formatters, which are used to format the arguments.
   *
   * @serial
   */
  private LinkedHashMap<String, Format> formats = new LinkedHashMap<>(INITIAL_FORMATS);

  /**
   * The positions where the results of formatting each argument are to be inserted
   * into the pattern.
   *
   * @serial
   */
  private int[] offsets = new int[INITIAL_FORMATS];

  /**
   * The argument numbers corresponding to each formatter.  (The formatters are stored
   * in the order they occur in the pattern, not in the order in which the arguments
   * are specified.)
   *
   * @serial
   */
  private String[] argumentNames = new String[INITIAL_FORMATS];

  /**
   * One less than the number of entries in <code>offsets</code>.  Can also be thought of
   * as the index of the highest-numbered element in <code>offsets</code> that is being used.
   * All of these arrays should have the same number of elements being used as <code>offsets</code>
   * does, and so this variable suffices to tell us how many entries are in all of them.
   *
   * @serial
   */
  private int maxOffset = -1;

  /**
   * Internal routine used by format. If {@code characterIterators} is
   * {@code non-null}, AttributedCharacterIterator will be created from the
   * subformats as necessary. If {@code characterIterators} is {@code null}
   * and {@code fp} is {@code non-null} and identifies
   * {@code Field.ARGUMENT} as the field attribute, the location of
   * the first replaced argument will be set in it.
   *
   * @throws IllegalArgumentException if an argument in the
   *                                  <code>arguments</code> array is not of the type
   *                                  expected by the format element(s) that use it.
   */
  private StringBuffer subformat(
      Map<String, Object> arguments, StringBuffer result,
      FieldPosition fp, List<AttributedCharacterIterator> characterIterators
  ) {
    // note: this implementation assumes a fast substring & index.
    // if this is not true, would be better to append chars one by one.
    int lastOffset = 0;
    int last;
    for (int i = 0; i <= maxOffset; ++i) {
      result.append(pattern, lastOffset, offsets[i]);
      lastOffset = offsets[i];
      String argumentName = argumentNames[i];
      if (arguments == null || !arguments.containsKey(argumentName)) {
        result.append('{').append(argumentName).append('}');
        continue;
      }
      // int argRecursion = ((recursionProtection >> (argumentNumber*2)) & 0x3);
      if (false) { // if (argRecursion == 3){
        // prevent loop!!!
        result.append('\uFFFD');
      } else {
        Object obj = arguments.get(argumentName);
        String arg = null;
        Format subFormatter = null;
        if (obj == null) {
          arg = "null";
        } else if (formats.get(argumentName) != null) {
          subFormatter = formats.get(argumentName);
          if (subFormatter instanceof ChoiceFormat) {
            arg = subFormatter.format(obj);
            if (arg.indexOf('{') >= 0) {
              subFormatter = new NamedArgsMessageFormat(arg, locale);
              obj = arguments;
              arg = null;
            }
          }
        } else if (obj instanceof Number) {
          // format number if can
          subFormatter = NumberFormat.getInstance(locale);
        } else if (obj instanceof Date) {
          // format a Date if can
          subFormatter = DateFormat.getDateTimeInstance(
              DateFormat.SHORT, DateFormat.SHORT, locale);//fix
        } else if (obj instanceof String) {
          arg = (String) obj;

        } else {
          arg = obj.toString();
          if (arg == null) arg = "null";
        }

        // At this point we are in two states, either subFormatter
        // is non-null indicating we should format obj using it,
        // or arg is non-null and we should use it as the value.

        if (subFormatter != null) {
          arg = subFormatter.format(obj);
        }
        last = result.length();
        result.append(arg);
        if (i == 0 && fp != null && Field.ARGUMENT.equals(
            fp.getFieldAttribute())) {
          fp.setBeginIndex(last);
          fp.setEndIndex(result.length());
        }
      }
    }
    result.append(pattern, lastOffset, pattern.length());
    return result;
  }

  // Indices for segments
  private static final int SEG_RAW = 0;
  private static final int SEG_INDEX = 1;
  private static final int SEG_TYPE = 2;
  private static final int SEG_MODIFIER = 3; // modifier or subformat

  // Indices for type keywords
  private static final int TYPE_NULL = 0;
  private static final int TYPE_NUMBER = 1;
  private static final int TYPE_DATE = 2;
  private static final int TYPE_TIME = 3;
  private static final int TYPE_CHOICE = 4;

  private static final String[] TYPE_KEYWORDS = {
      "",
      "number",
      "date",
      "time",
      "choice"
  };

  // Indices for number modifiers
  private static final int MODIFIER_DEFAULT = 0; // common in number and date-time
  private static final int MODIFIER_CURRENCY = 1;
  private static final int MODIFIER_PERCENT = 2;
  private static final int MODIFIER_INTEGER = 3;

  private static final String[] NUMBER_MODIFIER_KEYWORDS = {
      "",
      "currency",
      "percent",
      "integer"
  };

  // Indices for date-time modifiers
  private static final int MODIFIER_SHORT = 1;
  private static final int MODIFIER_MEDIUM = 2;
  private static final int MODIFIER_LONG = 3;
  private static final int MODIFIER_FULL = 4;

  private static final String[] DATE_TIME_MODIFIER_KEYWORDS = {
      "",
      "short",
      "medium",
      "long",
      "full"
  };

  // Date-time style values corresponding to the date-time modifiers.
  private static final int[] DATE_TIME_MODIFIERS = {
      DateFormat.DEFAULT,
      DateFormat.SHORT,
      DateFormat.MEDIUM,
      DateFormat.LONG,
      DateFormat.FULL,
  };

  private void makeFormat(
      int offsetNumber,
      StringBuilder[] textSegments
  ) {
    String[] segments = new String[textSegments.length];
    for (int i = 0; i < textSegments.length; i++) {
      StringBuilder oneseg = textSegments[i];
      segments[i] = (oneseg != null) ? oneseg.toString() : "";
    }

    // get the argument number
    String argumentName = segments[SEG_INDEX];
    if (argumentName.isEmpty()) {
      throw new IllegalArgumentException("Named argument is empty");
    }
    // resize format information arrays if necessary
    if (offsetNumber >= offsets.length) {
      int newLength = offsets.length * 2;
      Format[] newFormats = new Format[newLength];
      int[] newOffsets = new int[newLength];
      String[] newArgumentNames = new String[newLength];
      System.arraycopy(formats, 0, newFormats, 0, maxOffset + 1);
      System.arraycopy(offsets, 0, newOffsets, 0, maxOffset + 1);
      System.arraycopy(argumentNames, 0, newArgumentNames, 0, maxOffset + 1);
      offsets = newOffsets;
      argumentNames = newArgumentNames;
    }
    int oldMaxOffset = maxOffset;
    maxOffset = offsetNumber;
    offsets[offsetNumber] = segments[SEG_RAW].length();
    argumentNames[offsetNumber] = argumentName;

    // now get the format
    Format newFormat = null;
    if (!segments[SEG_TYPE].isEmpty()) {
      int type = findKeyword(segments[SEG_TYPE], TYPE_KEYWORDS);
      switch (type) {
        case TYPE_NULL:
          // Type "" is allowed. e.g., "{0,}", "{0,,}", and "{0,,#}"
          // are treated as "{0}".
          break;

        case TYPE_NUMBER:
          switch (findKeyword(segments[SEG_MODIFIER], NUMBER_MODIFIER_KEYWORDS)) {
            case MODIFIER_DEFAULT:
              newFormat = NumberFormat.getInstance(locale);
              break;
            case MODIFIER_CURRENCY:
              newFormat = NumberFormat.getCurrencyInstance(locale);
              break;
            case MODIFIER_PERCENT:
              newFormat = NumberFormat.getPercentInstance(locale);
              break;
            case MODIFIER_INTEGER:
              newFormat = NumberFormat.getIntegerInstance(locale);
              break;
            default: // DecimalFormat pattern
              try {
                newFormat = new DecimalFormat(segments[SEG_MODIFIER],
                    DecimalFormatSymbols.getInstance(locale));
              } catch (IllegalArgumentException e) {
                maxOffset = oldMaxOffset;
                throw e;
              }
              break;
          }
          break;

        case TYPE_DATE:
        case TYPE_TIME:
          int mod = findKeyword(segments[SEG_MODIFIER], DATE_TIME_MODIFIER_KEYWORDS);
          if (mod >= 0 && mod < DATE_TIME_MODIFIER_KEYWORDS.length) {
            if (type == TYPE_DATE) {
              newFormat = DateFormat.getDateInstance(DATE_TIME_MODIFIERS[mod],
                  locale);
            } else {
              newFormat = DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[mod],
                  locale);
            }
          } else {
            // SimpleDateFormat pattern
            try {
              newFormat = new SimpleDateFormat(segments[SEG_MODIFIER], locale);
            } catch (IllegalArgumentException e) {
              maxOffset = oldMaxOffset;
              throw e;
            }
          }
          break;

        case TYPE_CHOICE:
          try {
            // ChoiceFormat pattern
            newFormat = new ChoiceFormat(segments[SEG_MODIFIER]);
          } catch (Exception e) {
            maxOffset = oldMaxOffset;
            throw new IllegalArgumentException("Choice Pattern incorrect: "
                + segments[SEG_MODIFIER], e);
          }
          break;

        default:
          maxOffset = oldMaxOffset;
          throw new IllegalArgumentException("unknown format type: " +
              segments[SEG_TYPE]);
      }
    }
    formats.put(argumentName, newFormat);
  }

  private static final int findKeyword(String s, String[] list) {
    for (int i = 0; i < list.length; ++i) {
      if (s.equals(list[i]))
        return i;
    }

    // Try trimmed lowercase.
    String ls = s.trim().toLowerCase(Locale.ROOT);
    if (ls != s) {
      for (int i = 0; i < list.length; ++i) {
        if (ls.equals(list[i]))
          return i;
      }
    }
    return -1;
  }

  private static final void copyAndFixQuotes(
      String source, int start, int end,
      StringBuilder target
  ) {
    boolean quoted = false;

    for (int i = start; i < end; ++i) {
      char ch = source.charAt(i);
      if (ch == '{') {
        if (!quoted) {
          target.append('\'');
          quoted = true;
        }
        target.append(ch);
      } else if (ch == '\'') {
        target.append("''");
      } else {
        if (quoted) {
          target.append('\'');
          quoted = false;
        }
        target.append(ch);
      }
    }
    if (quoted) {
      target.append('\'');
    }
  }

  /**
   * After reading an object from the input stream, do a simple verification
   * to maintain class invariants.
   *
   * @throws InvalidObjectException if the objects read from the stream is invalid.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    boolean isValid = maxOffset >= -1
//        && formats.length > maxOffset
        && offsets.length > maxOffset
        && argumentNames.length > maxOffset;
    if (isValid) {
      int lastOffset = pattern.length() + 1;
      for (int i = maxOffset; i >= 0; --i) {
        if ((offsets[i] < 0) || (offsets[i] > lastOffset)) {
          isValid = false;
          break;
        } else {
          lastOffset = offsets[i];
        }
      }
    }
    if (!isValid) {
      throw new InvalidObjectException("Could not reconstruct NamedArgsMessageFormat from corrupt stream.");
    }
  }
}

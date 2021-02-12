package io.ebeaninternal.server.querydefn;

import io.ebean.FetchConfig;
import io.ebeaninternal.server.util.DSelectColumnsParser;

import java.util.Iterator;
import java.util.Set;

/**
 * Parses the path properties string.
 */
class OrmQueryPropertiesParser {

  private static final Response EMPTY = new Response();

  /**
   * Immutable response of the parsed properties and options.
   */
  static class Response {

    final boolean readOnly;
    final boolean cache;
    final FetchConfig fetchConfig;
    final String properties;
    final Set<String> included;

    Response(boolean readOnly, boolean cache, int queryFetchBatch, int lazyFetchBatch, String properties, Set<String> included) {
      this.readOnly = readOnly;
      this.cache = cache;
      this.properties = properties;
      this.included = included;
      if (lazyFetchBatch > -1 || queryFetchBatch > -1) {
        this.fetchConfig = new FetchConfig().lazy(lazyFetchBatch).query(queryFetchBatch);
      } else {
        this.fetchConfig = OrmQueryProperties.DEFAULT_FETCH;
      }
    }

    Response() {
      this.readOnly = false;
      this.cache = false;
      this.fetchConfig = OrmQueryProperties.DEFAULT_FETCH;
      this.properties = "";
      this.included = null;
    }
  }

  /**
   * Parses the path properties string returning the parsed properties and options.
   * In general it is comma delimited with some special strings like +lazy(20).
   */
  public static Response parse(String rawProperties) {
    return new OrmQueryPropertiesParser(rawProperties).parse();
  }

  private final String inputProperties;
  private boolean allProperties;
  private boolean readOnly;
  private boolean cache;
  private int queryFetchBatch = -1;
  private int lazyFetchBatch = -1;

  private OrmQueryPropertiesParser(String inputProperties) {
    this.inputProperties = inputProperties;
  }

  /**
   * Parse the raw string properties input.
   */
  private Response parse() {
    if (inputProperties == null || inputProperties.isEmpty()) {
      return EMPTY;
    }
    if (inputProperties.equals("*")) {
      // explicit all properties
      allProperties = true;
      return new Response(readOnly, cache, queryFetchBatch, lazyFetchBatch, "*", null);
    }
    boolean hints = false;
    Set<String> fields = splitRawSelect(inputProperties);
    final Iterator<String> iterator = fields.iterator();
    while (iterator.hasNext()) {
      String val = iterator.next();
      if (val.startsWith("+")) {
        hints = true;
        iterator.remove();
        parseHint(val);
      } else if (val.equals("*")) {
        allProperties = true;
      }
    }
    String properties = allProperties ? "*" : hints ? String.join(",", fields) : inputProperties;
    if (fields.isEmpty()) {
      fields = null;
    }
    return new Response(readOnly, cache, queryFetchBatch, lazyFetchBatch, properties, fields);
  }

  private void parseHint(String val) {
    if (val.equals("+readonly")) {
      readOnly = true;
    } else if (val.equals("+cache")) {
      cache = true;
    } else if (val.startsWith("+query")) {
      queryFetchBatch = parseBatch(val);
    } else if (val.startsWith("+lazy")) {
      lazyFetchBatch = parseBatch(val);
    }
  }

  private int parseBatch(String val) {
    if (val.endsWith(")")) {
      int start = val.lastIndexOf('(');
      if (start > 0) {
        return Integer.parseInt(val.substring(start + 1, val.length() - 1));
      }
    }
    return 0;
  }

  /**
   * Split allowing 'dynamic function based properties'.
   */
  private Set<String> splitRawSelect(String inputProperties) {
    return DSelectColumnsParser.parse(inputProperties);
  }

}

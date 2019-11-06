package zipkin2.storage.logzio.client;

import zipkin2.internal.Nullable;
import zipkin2.storage.logzio.ConsumerParams;

import java.util.*;

public final class SearchRequest {

  public static SearchRequest create() {
    return new SearchRequest(null);
  }

  public static SearchRequest create(String type) {
    return new SearchRequest(type);
  }

  /**
   * The maximum results returned in a query. This only affects non-aggregation requests.
   *
   * <p>Not configurable as it implies adjustments to the index template (index.max_result_window)
   *
   * <p> See https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-from-size.html
   */
  private static final int MAX_RESULT_WINDOW = 10000; // the default logz.io allowed limit

  @Nullable
  transient final String type;

  private Integer size = MAX_RESULT_WINDOW;
  private Boolean _source;
  private Object query;
  private Map<String, Aggregation> aggs;

   SearchRequest(@Nullable String type) {

    this.type = type;
  }

  public static class Filters extends ArrayList<Object> {
    public Filters addRange(String field, long from, Long to) {
      add(new Range(field, from, to));
      return this;
    }

    public Filters addTerm(String field, String value) {
      add(new Term(field, value));
      return this;
    }
    public Filters addTerms(String field, List<String> values) {
      add(new Terms(field, values));
      return this;
    }
  }

  public SearchRequest filters(Filters filters) {
     filters.addTerm("type", ConsumerParams.TYPE);
    return query(new BoolQuery("must", filters));
  }

  public SearchRequest term(String field, String value) {
    return query(new Term(field, value));
  }

  public SearchRequest terms(String field, List<String> values) {
    return query(new Terms(field, values));
  }

  public SearchRequest addAggregation(Aggregation agg) {
    size = null; // we return aggs, not source data
    _source = false;
    if (aggs == null) aggs = new LinkedHashMap<>();
    aggs.put(agg.field, agg);
    return this;
  }

  String tag() {
    return aggs != null ? "aggregation" : "search";
  }

  SearchRequest query(Object filter) {
    query = Collections.singletonMap("bool", Collections.singletonMap("filter", filter));
    return this;
  }

  static class Term {
    final Map<String, String> term;

    Term(String field, String value) {
      term = Collections.singletonMap(field, value);
    }
  }

  static class Terms {
    final Map<String, Collection<String>> terms;

    Terms(String field, Collection<String> values) {
      this.terms = Collections.singletonMap(field, values);
    }
  }

  static class Range {
    final Map<String, Bounds> range;

    Range(String field, long from, Long to) {
      range = Collections.singletonMap(field, new Bounds(from, to));
    }

    static class Bounds {
      final long from;
      final Long to;
      final boolean include_lower = true;
      final boolean include_upper = true;

      Bounds(long from, Long to) {
        this.from = from;
        this.to = to;
      }
    }
  }

  static class BoolQuery {
    final Map<String, Object> bool;

    BoolQuery(String op, Object clause) {
      bool = Collections.singletonMap(op, clause);
    }
  }
}

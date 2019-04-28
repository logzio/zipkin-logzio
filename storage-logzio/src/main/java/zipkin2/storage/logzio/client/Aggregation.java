
package zipkin2.storage.logzio.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Aggregation {
  transient final String field;
  AggTerms terms;
  Map<String, String> min;
  Map<String, Aggregation> aggs;

  Aggregation(String field) {
    this.field = field;
  }

  public static Aggregation terms(String field, int size) {
    Aggregation result = new Aggregation(field);
    result.terms = new AggTerms(field, size);
    return result;
  }

  public Aggregation orderBy(String subAgg, String direction) {
    terms.order(subAgg, direction);
    return this;
  }

  public static Aggregation min(String field) {
    Aggregation result = new Aggregation(field);
    result.min = Collections.singletonMap("field", field);
    return result;
  }

  static class AggTerms {
    AggTerms(String field, int size) {
      this.field = field;
      this.size = size;
    }

    final String field;
    int size;
    Map<String, String> order;

    AggTerms order(String agg, String direction) {
      order = Collections.singletonMap(agg, direction);
      return this;
    }
  }

  public Aggregation addSubAggregation(Aggregation agg) {
    if (aggs == null) aggs = new LinkedHashMap<>();
    aggs.put(agg.field, agg);
    return this;
  }
}

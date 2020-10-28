package com.hubspot.jinjava.lib.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.jinjava.BaseJinjavaTest;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class XmlAttrFilterTest extends BaseJinjavaTest {

  @Test
  public void testXmlAttr() {
    Map<String, Object> context = new HashMap<>();
    context.put("variable", 42);

    Document dom = Jsoup.parseBodyFragment(
      jinjava.render(
        "<ul{{ {'class': 'my_list', 'missing': none, 'id': 'list-' ~ variable}|xmlattr }}></ul>",
        context
      )
    );

    assertThat(dom.select("ul").attr("class")).isEqualTo("my_list");
    assertThat(dom.select("ul").attr("id")).isEqualTo("list-42");
    assertThat(dom.select("ul").attr("missing")).isEmpty();
  }
}

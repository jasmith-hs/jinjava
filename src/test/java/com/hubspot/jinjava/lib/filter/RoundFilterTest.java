package com.hubspot.jinjava.lib.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.jinjava.BaseJinjavaTest;
import java.util.HashMap;
import org.junit.Test;

public class RoundFilterTest extends BaseJinjavaTest {

  @Test
  public void roundDefault() {
    assertThat(jinjava.render("{{ 42.55|round }}", new HashMap<String, Object>()))
      .isEqualTo("43");
  }

  @Test
  public void roundFloor() {
    assertThat(
        jinjava.render("{{ 42.55|round(1, 'floor') }}", new HashMap<String, Object>())
      )
      .isEqualTo("42.5");
  }
}

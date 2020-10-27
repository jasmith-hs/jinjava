package com.hubspot.jinjava.lib.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.interpret.TemplateError.ErrorReason;
import org.junit.Before;
import org.junit.Test;

public class DoTagTest {
  public Context context;
  public JinjavaInterpreter interpreter;
  public Jinjava jinjava;

  @Before
  public void setup() {
    jinjava = new Jinjava();
    interpreter = jinjava.newInterpreter();
    context = interpreter.getContext();
  }

  @Test
  public void itResolvesExpressions() {
    String template = "{% set output = [] %}{% do output.append('hey') %}{{ output }}";
    assertThat(jinjava.render(template, Maps.newHashMap())).isEqualTo("[hey]");
  }

  @Test
  public void itAddsTemplateErrorOnEmptyExpression() {
    String template = "{% do %}";
    RenderResult renderResult = jinjava.renderForResult(template, Maps.newHashMap());
    assertThat(renderResult.getErrors()).hasSize(1);
    assertThat(renderResult.getErrors().get(0).getReason())
      .isEqualTo(ErrorReason.SYNTAX_ERROR);
  }
}

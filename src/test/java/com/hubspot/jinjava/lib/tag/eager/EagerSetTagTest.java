package com.hubspot.jinjava.lib.tag.eager;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.SetTagTest;
import com.hubspot.jinjava.tree.parse.TagToken;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EagerSetTagTest extends SetTagTest {
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void setup() {
    super.setup();
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        context,
        JinjavaConfig.newBuilder().withPreserveForFinalPass(true).build()
      );
    tag = new EagerSetTag();
    context.registerTag(tag);
    context.put("deferred", DeferredValue.instance());
    expectedNodeInterpreter =
      new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/settag");
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }

  @Test
  public void itEvaluatesExpression() {
    context.put("bar", 3);
    context.setEagerMode(true);
    expectedNodeInterpreter.assertExpectedOutput("evaluates-expression");
    Optional<EagerToken> maybeEagerToken = context
      .getEagerTokens()
      .stream()
      .filter(e -> ((TagToken) e.getToken()).getTagName().equals(tag.getName()))
      .findAny();
    assertThat(maybeEagerToken).isPresent();
    assertThat(maybeEagerToken.get().getSetDeferredWords()).containsExactly("foo");
  }

  @Test
  public void itPartiallyEvaluatesDeferredExpression() {
    context.put("bar", 3);
    context.setEagerMode(true);
    expectedNodeInterpreter.assertExpectedOutput(
      "partially-evaluates-deferred-expression"
    );
    Optional<EagerToken> maybeEagerToken = context
      .getEagerTokens()
      .stream()
      .filter(e -> ((TagToken) e.getToken()).getTagName().equals(tag.getName()))
      .findAny();
    assertThat(maybeEagerToken).isPresent();
    assertThat(maybeEagerToken.get().getSetDeferredWords())
      .containsExactlyInAnyOrder("foo");
    assertThat(maybeEagerToken.get().getUsedDeferredWords())
      .containsExactlyInAnyOrder("deferred", "range");
  }

  @Test
  public void itHandlesMultipleVars() {
    context.put("bar", 3);
    context.put("baz", 6);
    context.setEagerMode(true);
    expectedNodeInterpreter.assertExpectedOutput("handles-multiple-vars");
    Optional<EagerToken> maybeEagerToken = context
      .getEagerTokens()
      .stream()
      .filter(e -> ((TagToken) e.getToken()).getTagName().equals(tag.getName()))
      .findAny();
    assertThat(maybeEagerToken).isPresent();
    assertThat(maybeEagerToken.get().getSetDeferredWords())
      .containsExactlyInAnyOrder("foo", "foobar");
    assertThat(maybeEagerToken.get().getUsedDeferredWords())
      .containsExactlyInAnyOrder("deferred", "range");
  }

  @Test
  @Override
  @Ignore
  public void itThrowsAndDefersVarWhenValContainsDeferred() {
    // Deferred values are handled differently. Test does not apply.
  }

  @Test
  @Override
  @Ignore
  public void itThrowsAndDefersMultiVarWhenValContainsDeferred() {
    // Deferred values are handled differently. Test does not apply.
  }
}

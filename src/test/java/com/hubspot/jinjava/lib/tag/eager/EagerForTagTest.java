package com.hubspot.jinjava.lib.tag.eager;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.ForTagTest;
import com.hubspot.jinjava.tree.parse.TagToken;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EagerForTagTest extends ForTagTest {
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void eagerSetup() {
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        context,
        JinjavaConfig.newBuilder().withPreserveForFinalPass(true).build()
      );
    tag = new EagerForTag();
    context.registerTag(tag);
    context.put("deferred", DeferredValue.instance());
    expectedNodeInterpreter =
      new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/fortag");
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }

  @Test
  public void itRegistersEagerToken() {
    expectedNodeInterpreter.assertExpectedOutput("registers-eager-token");
    Optional<EagerToken> maybeEagerToken = context
      .getEagerTokens()
      .stream()
      .filter(e -> ((TagToken) e.getToken()).getTagName().equals(tag.getName()))
      .findAny();
    assertThat(maybeEagerToken).isPresent();
    assertThat(maybeEagerToken.get().getSetDeferredWords())
      .containsExactlyInAnyOrder("item");
    assertThat(maybeEagerToken.get().getUsedDeferredWords())
      .containsExactlyInAnyOrder("deferred");
  }

  @Test
  public void itHandlesMultipleLoopVars() {
    expectedNodeInterpreter.assertExpectedOutput("handles-multiple-loop-vars");
    Optional<EagerToken> maybeEagerToken = context
      .getEagerTokens()
      .stream()
      .filter(e -> e.getToken() instanceof TagToken)
      .filter(e -> ((TagToken) e.getToken()).getTagName().equals(tag.getName()))
      .findAny();
    assertThat(maybeEagerToken).isPresent();
    assertThat(maybeEagerToken.get().getSetDeferredWords())
      .containsExactlyInAnyOrder("item", "item2");
    assertThat(maybeEagerToken.get().getUsedDeferredWords())
      .containsExactlyInAnyOrder("deferred");
  }

  @Test
  public void itHandlesNestedDeferredForLoop() {
    context.put("food_types", ImmutableList.of("sandwich", "salad", "smoothie"));
    expectedNodeInterpreter.assertExpectedOutput("handles-nested-deferred-for-loop");
  }
}

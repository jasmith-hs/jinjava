package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.DoTagTest;
import com.hubspot.jinjava.lib.tag.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EagerDoTagTest extends DoTagTest {
  private Tag tag;
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void eagerSetup() {
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        context,
        JinjavaConfig.newBuilder().withPreserveForFinalPass(true).build()
      );

    tag = new EagerDoTag();
    context.registerTag(tag);
    context.put("deferred", DeferredValue.instance());
    expectedNodeInterpreter =
      new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/dotag");
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }

  @Test
  public void itHandlesDeferredDo() {
    context.put("foo", 2);
    expectedNodeInterpreter.assertExpectedOutput("handles-deferred-do");
  }
}

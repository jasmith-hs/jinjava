package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.FromTag;
import com.hubspot.jinjava.lib.tag.FromTagTest;
import com.hubspot.jinjava.lib.tag.Tag;
import org.junit.After;
import org.junit.Before;

public class EagerFromTagTest extends FromTagTest {
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void eagerSetup() {
    context.put("padding", 42);
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        context,
        JinjavaConfig.newBuilder().withEagerExecutionEnabled(true).build()
      );
    Tag tag = EagerTagFactory
      .getEagerTagDecorator(FromTag.class)
      .orElseThrow(RuntimeException::new);
    context.registerTag(tag);
    context.put("deferred", DeferredValue.instance());
    expectedNodeInterpreter =
      new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/from");
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }
}

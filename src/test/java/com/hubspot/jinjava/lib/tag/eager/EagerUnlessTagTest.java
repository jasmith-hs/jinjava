package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.UnlessTagTest;
import org.junit.After;
import org.junit.Before;

public class EagerUnlessTagTest extends UnlessTagTest {
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void eagerSetup() {
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        context,
        JinjavaConfig.newBuilder().withPreserveForFinalPass(true).build()
      );
    tag = new EagerUnlessTag();
    context.registerTag(tag);
    context.put("deferred", DeferredValue.instance());
    expectedNodeInterpreter =
      new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/unlesstag");
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }
}

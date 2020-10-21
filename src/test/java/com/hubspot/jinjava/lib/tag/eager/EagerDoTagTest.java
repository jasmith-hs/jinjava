package com.hubspot.jinjava.lib.tag.eager;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.DoTag;
import com.hubspot.jinjava.lib.tag.DoTagTest;
import com.hubspot.jinjava.lib.tag.SetTagTest;
import com.hubspot.jinjava.lib.tag.Tag;
import com.hubspot.jinjava.tree.parse.TagToken;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EagerDoTagTest extends DoTagTest {
  private Tag tag;
  private ExpectedNodeInterpreter expectedNodeInterpreter;

  @Before
  public void setup() {
    super.setup();
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

package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.ExpectedNodeInterpreter;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.DoTag;
import com.hubspot.jinjava.lib.tag.PrintTag;
import com.hubspot.jinjava.lib.tag.Tag;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EagerGenericTagTest {
  private Jinjava jinjava;
  private Context context;
  private JinjavaInterpreter interpreter;
  private EagerTagFactory eagerTagFactory;

  @Before
  public void setup() {
    jinjava = new Jinjava();
    interpreter =
      new JinjavaInterpreter(
        jinjava,
        jinjava.getGlobalContextCopy(),
        JinjavaConfig.newBuilder().withPreserveForFinalPass(true).build()
      );
    context = interpreter.getContext();
    context.put("deferred", DeferredValue.instance());
    eagerTagFactory = new EagerTagFactory();
    JinjavaInterpreter.pushCurrent(interpreter);
  }

  @After
  public void teardown() {
    JinjavaInterpreter.popCurrent();
  }

  private EagerTagDecorator<? extends Tag> setupClass(Class<? extends Tag> clazz) {
    Optional<? extends EagerTagDecorator<? extends Tag>> maybeTag = eagerTagFactory.getEagerTagDecorator(
      clazz
    );
    maybeTag.ifPresent(eagerTagDecorator -> context.registerTag(eagerTagDecorator));
    return maybeTag.orElseThrow(RuntimeException::new);
  }

  private ExpectedNodeInterpreter makeExpectedNodeInterpreter(Tag tag) {
    return new ExpectedNodeInterpreter(interpreter, tag, "tags/eager/generic");
  }

  @Test
  public void itHandlesDeferredPrint() {
    EagerTagDecorator<? extends Tag> eagerTagDecorator = setupClass(PrintTag.class);
    makeExpectedNodeInterpreter(eagerTagDecorator)
      .assertExpectedOutput("handles-deferred-print");
  }

  @Test
  public void itHandlesDeferredPrintHelpers() {
    EagerTagDecorator<? extends Tag> eagerTagDecorator = setupClass(PrintTag.class);
    makeExpectedNodeInterpreter(eagerTagDecorator)
      .assertExpectedOutput("handles-deferred-print-helpers");
  }
}

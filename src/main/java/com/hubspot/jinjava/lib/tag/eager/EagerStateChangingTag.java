package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.Tag;
import com.hubspot.jinjava.tree.TagNode;

public abstract class EagerStateChangingTag<T extends Tag> extends EagerTagDecorator<T> {

  public EagerStateChangingTag(T tag) {
    super(tag);
  }

  @Override
  public final String interpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    if (interpreter.getContext().isEagerMode()) {
      // Preserve state-changing tags when eagerly executing nodes.
      return eagerInterpret(tagNode, interpreter);
    } else {
      return super.interpret(tagNode, interpreter);
    }
  }
}

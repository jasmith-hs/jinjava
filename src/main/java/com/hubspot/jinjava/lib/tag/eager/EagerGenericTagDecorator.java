package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.EagerValueException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.Tag;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.tree.parse.TagToken;
import org.apache.commons.lang3.StringUtils;

public class EagerGenericTagDecorator<T extends Tag>
  extends EagerTagDecorator<T>
  implements Tag {

  public EagerGenericTagDecorator(T tag) {
    super(tag);
  }

  @Override
  public String eagerInterpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    StringBuilder result = new StringBuilder(
      getEagerImage((TagToken) tagNode.getMaster(), interpreter)
    );

    for (Node child : tagNode.getChildren()) {
      result.append(interpreter.render(child));
    }

    if (StringUtils.isNotBlank(tagNode.getEndName())) {
      result.append("{% ").append(tagNode.getEndName()).append(" %}");
    }

    return result.toString();
  }
}

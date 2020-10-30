package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.lib.tag.ElseIfTag;
import com.hubspot.jinjava.lib.tag.ElseTag;
import com.hubspot.jinjava.lib.tag.IfTag;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.util.LengthLimitingStringBuilder;
import org.apache.commons.lang3.StringUtils;

public class EagerIfTag extends EagerTagDecorator<IfTag> {

  public EagerIfTag() {
    super(new IfTag());
  }

  public EagerIfTag(IfTag ifTag) {
    super(ifTag);
  }

  @Override
  public String eagerInterpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    if (StringUtils.isBlank(tagNode.getHelpers())) {
      throw new TemplateSyntaxException(
        interpreter,
        tagNode.getMaster().getImage(),
        "Tag 'if' expects expression"
      );
    }

    LengthLimitingStringBuilder result = new LengthLimitingStringBuilder(
      interpreter.getConfig().getMaxOutputSize()
    );

    result.append(
      executeInChildContext(
        eagerInterpreter ->
          getEagerImage(tagNode.getMaster(), eagerInterpreter) +
          renderChildren(tagNode, eagerInterpreter),
        interpreter,
        false
      )
    );
    tagNode.getMaster().setRightTrimAfterEnd(false);
    result.append(reconstructEnd(tagNode));

    return result.toString();
  }

  @Override
  public String renderChildren(TagNode tagNode, JinjavaInterpreter eagerInterpreter) {
    StringBuilder sb = new StringBuilder();
    for (Node child : tagNode.getChildren()) {
      if (TagNode.class.isAssignableFrom(child.getClass())) {
        TagNode tag = (TagNode) child;
        if (
          tag.getName().equals(ElseIfTag.TAG_NAME) ||
          tag.getName().equals(ElseTag.TAG_NAME)
        ) {
          sb.append(getEagerImage(tag.getMaster(), eagerInterpreter));
          continue;
        }
      }
      sb.append(renderChild(child, eagerInterpreter));
    }
    return sb.toString();
  }
}

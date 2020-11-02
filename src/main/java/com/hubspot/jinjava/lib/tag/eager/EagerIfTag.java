package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.DeferredValueException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.lib.tag.ElseIfTag;
import com.hubspot.jinjava.lib.tag.ElseTag;
import com.hubspot.jinjava.lib.tag.IfTag;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.util.LengthLimitingStringBuilder;
import com.hubspot.jinjava.util.ObjectTruthValue;
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
    // If the branch is impossible, it should be removed
    boolean definitelyDrop = shouldDropBranch(tagNode, eagerInterpreter);
    // If the branch would definitely get executed, change it to and else
    // and drop subsequent branches.
    // We know this has to be false otherwise IfTag would have chosen the first branch.
    boolean definitelyExecuted = false;
    StringBuilder sb = new StringBuilder();
    for (Node child : tagNode.getChildren()) {
      if (TagNode.class.isAssignableFrom(child.getClass())) {
        TagNode tag = (TagNode) child;
        if (
          tag.getName().equals(ElseIfTag.TAG_NAME) ||
          tag.getName().equals(ElseTag.TAG_NAME)
        ) {
          if (definitelyExecuted) {
            break;
          }
          definitelyDrop =
            tag.getName().equals(ElseIfTag.TAG_NAME) &&
            shouldDropBranch(tag, eagerInterpreter);
          if (!definitelyDrop) {
            definitelyExecuted =
              tag.getName().equals(ElseTag.TAG_NAME) ||
              isDefinitelyExecuted(tag, eagerInterpreter);
            if (definitelyExecuted) {
              sb.append(
                String.format(
                  "%s else %s",
                  tag.getSymbols().getExpressionStartWithTag(),
                  tag.getSymbols().getExpressionEndWithTag()
                )
              );
            } else {
              sb.append(getEagerImage(tag.getMaster(), eagerInterpreter));
            }
          }
          continue;
        }
      }
      if (!definitelyDrop) {
        sb.append(renderChild(child, eagerInterpreter));
      }
    }
    return sb.toString();
  }

  private boolean shouldDropBranch(TagNode tagNode, JinjavaInterpreter eagerInterpreter) {
    try {
      return !ObjectTruthValue.evaluate(
        eagerInterpreter.resolveELExpression(
          tagNode.getHelpers(),
          tagNode.getLineNumber()
        )
      );
    } catch (DeferredValueException e) {
      return false;
    }
  }

  private boolean isDefinitelyExecuted(
    TagNode tagNode,
    JinjavaInterpreter eagerInterpreter
  ) {
    try {
      return ObjectTruthValue.evaluate(
        eagerInterpreter.resolveELExpression(
          tagNode.getHelpers(),
          tagNode.getLineNumber()
        )
      );
    } catch (DeferredValueException e) {
      return false;
    }
  }
}

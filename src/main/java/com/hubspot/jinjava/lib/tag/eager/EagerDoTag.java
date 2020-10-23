package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.DoTag;
import com.hubspot.jinjava.tree.parse.TagToken;
import com.hubspot.jinjava.util.ChunkResolver;
import java.util.StringJoiner;

public class EagerDoTag extends EagerStateChangingTag<DoTag> {

  public EagerDoTag() {
    super(new DoTag());
  }

  public EagerDoTag(DoTag doTag) {
    super(doTag);
  }

  @Override
  public String getEagerTagImage(TagToken tagToken, JinjavaInterpreter interpreter) {
    String expr = tagToken.getHelpers();
    ChunkResolver chunkResolver = new ChunkResolver(expr, tagToken, interpreter)
    .useMiniChunks(true);
    EagerStringResult resolvedExpression = executeInChildContext(
      eagerInterpreter -> chunkResolver.resolveChunks(),
      interpreter
    );
    StringJoiner joiner = new StringJoiner(" ");
    joiner
      .add(tagToken.getSymbols().getExpressionStartWithTag())
      .add(tagToken.getTagName())
      .add(resolvedExpression.getResult());
    interpreter
      .getContext()
      .handleEagerToken(new EagerToken(tagToken, chunkResolver.getDeferredWords()));
    if (chunkResolver.getDeferredWords().isEmpty()) {
      // Possible set tag in front of this one. Omits result
      return resolvedExpression.getPrefixToPreserveState();
    }
    joiner.add(tagToken.getSymbols().getExpressionEndWithTag());
    // Possible set tag in front of this one.
    return resolvedExpression.getPrefixToPreserveState() + joiner.toString();
  }
}

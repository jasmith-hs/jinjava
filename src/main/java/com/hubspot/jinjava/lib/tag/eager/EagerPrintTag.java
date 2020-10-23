package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.PrintTag;
import com.hubspot.jinjava.tree.parse.TagToken;
import com.hubspot.jinjava.util.ChunkResolver;
import java.util.StringJoiner;

public class EagerPrintTag extends EagerStateChangingTag<PrintTag> {

  public EagerPrintTag() {
    super(new PrintTag());
  }

  public EagerPrintTag(PrintTag printTag) {
    super(printTag);
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
      return resolvedExpression.toString();
    }
    joiner.add(tagToken.getSymbols().getExpressionEndWithTag());
    // Possible set tag in front of this one.
    return resolvedExpression.getPrefixToPreserveState() + joiner.toString();
  }
}

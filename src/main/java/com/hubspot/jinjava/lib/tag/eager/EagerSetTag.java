package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.DeferredValueException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.lib.tag.SetTag;
import com.hubspot.jinjava.tree.parse.TagToken;
import com.hubspot.jinjava.util.ChunkResolver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class EagerSetTag extends EagerStateChangingTag<SetTag> {

  public EagerSetTag() {
    super(new SetTag());
  }

  public EagerSetTag(SetTag setTag) {
    super(setTag);
  }

  @Override
  public String getEagerTagImage(TagToken tagToken, JinjavaInterpreter interpreter) {
    if (!tagToken.getHelpers().contains("=")) {
      throw new TemplateSyntaxException(
        interpreter,
        tagToken.getImage(),
        "Tag 'set' expects an assignment expression with '=', but was: " +
        tagToken.getHelpers()
      );
    }
    StringJoiner joiner = new StringJoiner(" ");
    joiner
      .add(tagToken.getSymbols().getExpressionStartWithTag())
      .add(tagToken.getTagName());

    int eqPos = tagToken.getHelpers().indexOf('=');
    String var = tagToken.getHelpers().substring(0, eqPos).trim();

    joiner.add(var);

    joiner.add("=");

    String expr = tagToken.getHelpers().substring(eqPos + 1);
    ChunkResolver chunkResolver = new ChunkResolver(expr, tagToken, interpreter)
    .useMiniChunks(true);
    String resolvedExpression = executeInChildContext(
      eagerInterpreter -> chunkResolver.resolveChunks(),
      interpreter
    );
    joiner.add(resolvedExpression);
    Set<String> deferredVariables = new HashSet<>(chunkResolver.getDeferredVariables());
    String[] varTokens = var.split(",");
    if (!deferredVariables.isEmpty()) {
      deferredVariables.addAll(
        Arrays.stream(varTokens).map(String::trim).collect(Collectors.toSet())
      );
    } else {
      try {
        getTag().executeSet(tagToken, interpreter, varTokens, resolvedExpression);
        return "";
      } catch (DeferredValueException e) {
        deferredVariables.addAll(
          Arrays.stream(varTokens).map(String::trim).collect(Collectors.toSet())
        );
      }
    }

    interpreter
      .getContext()
      .handleEagerToken(new EagerToken(tagToken, deferredVariables));
    joiner.add(tagToken.getSymbols().getExpressionEndWithTag());

    return joiner.toString();
  }
}

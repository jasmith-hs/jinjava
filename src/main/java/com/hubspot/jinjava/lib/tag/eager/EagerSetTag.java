package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.DeferredValue;
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

    int eqPos = tagToken.getHelpers().indexOf('=');
    String variables = tagToken.getHelpers().substring(0, eqPos).trim();

    String expression = tagToken.getHelpers().substring(eqPos + 1);
    ChunkResolver chunkResolver = new ChunkResolver(expression, tagToken, interpreter)
    .useMiniChunks(true);
    EagerStringResult resolvedExpression = executeInChildContext(
      eagerInterpreter -> chunkResolver.resolveChunks(),
      interpreter,
      true
    );
    StringJoiner joiner = new StringJoiner(" ");
    joiner
      .add(tagToken.getSymbols().getExpressionStartWithTag())
      .add(tagToken.getTagName())
      .add(variables)
      .add("=")
      .add(resolvedExpression.getResult())
      .add(tagToken.getSymbols().getExpressionEndWithTag());
    StringBuilder prefixToPreserveState = new StringBuilder(
      interpreter.getContext().isEagerMode()
        ? resolvedExpression.getPrefixToPreserveState()
        : ""
    );
    Set<String> deferredWords = new HashSet<>(chunkResolver.getDeferredWords());
    String[] varTokens = variables.split(",");
    deferredWords.addAll(
      Arrays
        .stream(varTokens)
        .map(prop -> prop.split("\\.", 2)[0])
        .filter(v -> interpreter.getContext().get(v) instanceof DeferredValue)
        .collect(Collectors.toSet())
    );
    if (chunkResolver.getDeferredWords().isEmpty()) {
      try {
        getTag()
          .executeSet(tagToken, interpreter, varTokens, resolvedExpression.getResult());
        // Possible macro/set tag in front of this one.
        return prefixToPreserveState.toString();
      } catch (DeferredValueException ignored) {}
    }
    prefixToPreserveState.append(
      getNewlyDeferredFunctionImages(deferredWords, interpreter)
    );
    // Defer all vars.
    deferredWords.addAll(
      Arrays.stream(varTokens).map(String::trim).collect(Collectors.toSet())
    );

    interpreter.getContext().handleEagerToken(new EagerToken(tagToken, deferredWords));
    // Possible macro/set tag in front of this one.
    return prefixToPreserveState.toString() + joiner.toString();
  }
}

package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.interpret.DeferredValueException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.JinjavaInterpreter.InterpreterScopeClosable;
import com.hubspot.jinjava.lib.tag.Tag;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.tree.parse.ExpressionToken;
import com.hubspot.jinjava.tree.parse.NoteToken;
import com.hubspot.jinjava.tree.parse.TagToken;
import com.hubspot.jinjava.tree.parse.TextToken;
import com.hubspot.jinjava.tree.parse.Token;
import com.hubspot.jinjava.util.ChunkResolver;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

public abstract class EagerTagDecorator<T extends Tag> implements Tag {
  private T tag;

  public EagerTagDecorator(T tag) {
    this.tag = tag;
  }

  public T getTag() {
    return tag;
  }

  public String eagerInterpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    StringBuilder result = new StringBuilder(
      executeInChildContext(
        eagerInterpreter -> getEagerImage(tagNode.getMaster(), eagerInterpreter),
        eagerInterpreter -> renderChildren(tagNode, eagerInterpreter),
        interpreter
      )
    );

    if (StringUtils.isNotBlank(tagNode.getEndName())) {
      result.append(tagNode.reconstructEnd());
    }

    return result.toString();
  }

  public String renderChildren(TagNode tagNode, JinjavaInterpreter eagerInterpreter) {
    StringBuilder sb = new StringBuilder();
    for (Node child : tagNode.getChildren()) {
      sb.append(renderChild(child, eagerInterpreter));
    }
    return sb.toString();
  }

  /**
   * Execute the specified functions within an isolated context.
   * The <code>declareEnabledFunction</code> is run, allowing for new values to get
   *   added to the isolated context, ie declared.
   * The <code>declareDisabledFunction</code> is run and will throw a
   *   <code>DeferredValueException</code> if a new value is attempted to be added
   *   to the isolated context, ie declared.
   * @param declareEnabledFunction Function to run allowing declaration of variables.
   * @param declareDisabledFunction Function to run restricting declaration of variables.
   * @param interpreter JinjavaInterpreter to create a child from.
   * @return The combined string results of <code>declareEnabledFunction</code> and
   *   <code>declareDisabledFunction</code>
   */
  public String executeInChildContext(
    Function<JinjavaInterpreter, String> declareEnabledFunction,
    Function<JinjavaInterpreter, String> declareDisabledFunction,
    JinjavaInterpreter interpreter
  ) {
    StringBuilder result = new StringBuilder();
    try (InterpreterScopeClosable c = interpreter.enterScope()) {
      interpreter.getContext().setEagerMode(false);
      result.append(declareEnabledFunction.apply(interpreter));
      interpreter.getContext().setEagerMode(true);
      result.append(declareDisabledFunction.apply(interpreter));
    }
    return result.toString();
  }

  public final Object renderChild(Node child, JinjavaInterpreter interpreter) {
    try {
      return child.render(interpreter);
    } catch (DeferredValueException e) {
      return getEagerImage(child.getMaster(), interpreter);
    }
  }

  public final String getEagerImage(Token token, JinjavaInterpreter interpreter) {
    String eagerImage;
    if (token instanceof TagToken) {
      eagerImage = getEagerTagImage((TagToken) token, interpreter);
    } else if (token instanceof ExpressionToken) {
      eagerImage = getEagerExpressionImage((ExpressionToken) token, interpreter);
    } else if (token instanceof TextToken) {
      eagerImage = getEagerTextImage((TextToken) token, interpreter);
    } else if (token instanceof NoteToken) {
      eagerImage = getEagerNoteImage((NoteToken) token, interpreter);
    } else {
      throw new DeferredValueException("Unsupported Token type");
    }
    return eagerImage;
  }

  public String getEagerTagImage(TagToken tagToken, JinjavaInterpreter interpreter) {
    StringJoiner joiner = new StringJoiner(" ");
    joiner
      .add(tagToken.getSymbols().getExpressionStartWithTag())
      .add(tagToken.getTagName());

    ChunkResolver chunkResolver = new ChunkResolver(
      tagToken.getHelpers().trim(),
      tagToken,
      interpreter
    )
    .useMiniChunks(true);
    String resolvedChunks = chunkResolver.resolveChunks();
    if (StringUtils.isNotBlank(resolvedChunks)) {
      joiner.add(resolvedChunks);
    }
    interpreter
      .getContext()
      .handleEagerToken(new EagerToken(tagToken, chunkResolver.getDeferredVariables()));

    joiner.add(tagToken.getSymbols().getExpressionEndWithTag());
    return joiner.toString();
  }

  public String getEagerExpressionImage(
    ExpressionToken expressionToken,
    JinjavaInterpreter interpreter
  ) {
    interpreter
      .getContext()
      .handleEagerToken(
        new EagerToken(expressionToken, Collections.singleton(expressionToken.getExpr()))
      );
    return expressionToken.getImage();
  }

  public String getEagerTextImage(TextToken textToken, JinjavaInterpreter interpreter) {
    interpreter
      .getContext()
      .handleEagerToken(
        new EagerToken(textToken, Collections.singleton(textToken.output()))
      );
    return textToken.getImage();
  }

  public String getEagerNoteImage(NoteToken noteToken, JinjavaInterpreter interpreter) {
    // Notes should not throw DeferredValueExceptions, but this will handle it anyway
    return "";
  }

  @Override
  public String interpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    try {
      return tag.interpret(tagNode, interpreter);
    } catch (DeferredValueException e) {
      return eagerInterpret(tagNode, interpreter);
    }
  }

  @Override
  public String getName() {
    return tag.getName();
  }

  @Override
  public String getEndTagName() {
    return tag.getEndTagName();
  }

  @Override
  public boolean isRenderedInValidationMode() {
    return tag.isRenderedInValidationMode();
  }
}

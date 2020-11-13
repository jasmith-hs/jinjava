package com.hubspot.jinjava.tree.eager;

import static com.hubspot.jinjava.lib.tag.eager.EagerTagDecorator.executeInChildContext;
import static com.hubspot.jinjava.lib.tag.eager.EagerTagDecorator.getNewlyDeferredFunctionImages;
import static com.hubspot.jinjava.lib.tag.eager.EagerTagDecorator.wrapInAutoEscapeIfNeeded;
import static com.hubspot.jinjava.lib.tag.eager.EagerTagDecorator.wrapInTag;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.filter.EscapeFilter;
import com.hubspot.jinjava.lib.tag.RawTag;
import com.hubspot.jinjava.lib.tag.eager.EagerStringResult;
import com.hubspot.jinjava.lib.tag.eager.EagerToken;
import com.hubspot.jinjava.tree.ExpressionNode;
import com.hubspot.jinjava.tree.output.OutputNode;
import com.hubspot.jinjava.tree.output.RenderedOutputNode;
import com.hubspot.jinjava.tree.parse.ExpressionToken;
import com.hubspot.jinjava.tree.parse.TagToken;
import com.hubspot.jinjava.util.ChunkResolver;
import com.hubspot.jinjava.util.Logging;
import com.hubspot.jinjava.util.WhitespaceUtils;
import org.apache.commons.lang3.StringUtils;

public class EagerExpressionNode extends ExpressionNode {

  public EagerExpressionNode(ExpressionToken token) {
    super(token);
  }

  @Override
  public OutputNode render(JinjavaInterpreter interpreter) {
    if (interpreter.getConfig().isEagerExecutionEnabled()) {
      EagerStringResult eagerStringResult = eagerResolveExpression(interpreter);
      return new RenderedOutputNode(
        eagerStringResult.getPrefixToPreserveState() + eagerStringResult.getResult()
      );
    } else {
      return super.render(interpreter);
    }
  }

  private EagerStringResult eagerResolveExpression(JinjavaInterpreter interpreter) {
    ChunkResolver chunkResolver = new ChunkResolver(master.getExpr(), master, interpreter)
    .useMiniChunks(true);
    EagerStringResult resolvedExpression = executeInChildContext(
      eagerInterpreter -> chunkResolver.resolveChunks(),
      interpreter,
      true
    );
    StringBuilder prefixToPreserveState = new StringBuilder(
      interpreter.getContext().isProtectedMode()
        ? resolvedExpression.getPrefixToPreserveState()
        : ""
    );
    if (chunkResolver.getDeferredWords().isEmpty()) {
      String result = WhitespaceUtils.unquote(resolvedExpression.getResult());
      if (
        !StringUtils.equals(result, master.getImage()) &&
        (
          StringUtils.contains(result, getSymbols().getExpressionStart()) ||
          StringUtils.contains(result, getSymbols().getExpressionStartWithTag())
        )
      ) {
        if (interpreter.getConfig().isNestedInterpretationEnabled()) {
          try {
            result = interpreter.renderFlat(result);
          } catch (Exception e) {
            Logging.ENGINE_LOG.warn("Error rendering variable node result", e);
          }
        } else {
          // Possible macro/set tag in front of this one. Includes result
          result = wrapInRawOrExpressionIfNeeded(result, interpreter);
        }
      }

      if (interpreter.getContext().isAutoEscape()) {
        result = EscapeFilter.escapeHtmlEntities(result);
      }
      return new EagerStringResult(result, prefixToPreserveState.toString());
    }
    prefixToPreserveState.append(
      getNewlyDeferredFunctionImages(chunkResolver.getDeferredWords(), interpreter)
    );
    String helpers = wrapInExpression(resolvedExpression.getResult(), interpreter);
    interpreter
      .getContext()
      .handleEagerToken(
        new EagerToken(
          new TagToken(
            helpers,
            master.getLineNumber(),
            master.getStartPosition(),
            master.getSymbols()
          ),
          chunkResolver.getDeferredWords()
        )
      );
    // There is no result because it couldn't be entirely evaluated.
    return new EagerStringResult(
      "",
      wrapInAutoEscapeIfNeeded(prefixToPreserveState.toString() + helpers, interpreter)
    );
  }

  private static String wrapInRawOrExpressionIfNeeded(
    String output,
    JinjavaInterpreter interpreter
  ) {
    if (
      interpreter.getConfig().isPreserveForFinalPass() &&
      (
        output.contains(
          interpreter.getConfig().getTokenScannerSymbols().getExpressionStart()
        ) ||
        output.contains(
          interpreter.getConfig().getTokenScannerSymbols().getExpressionStartWithTag()
        )
      )
    ) {
      return wrapInTag(output, RawTag.TAG_NAME, interpreter);
    }
    return output;
  }

  private static String wrapInExpression(String output, JinjavaInterpreter interpreter) {
    return String.format(
      "%s %s %s",
      interpreter.getConfig().getTokenScannerSymbols().getExpressionStart(),
      output,
      interpreter.getConfig().getTokenScannerSymbols().getExpressionEnd()
    );
  }
}

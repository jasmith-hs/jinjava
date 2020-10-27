package com.hubspot.jinjava.lib.tag.eager;

import static com.hubspot.jinjava.interpret.Context.GLOBAL_MACROS_SCOPE_KEY;
import static com.hubspot.jinjava.interpret.Context.IMPORT_RESOURCE_PATH_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.interpret.Context.Library;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.DeferredValueException;
import com.hubspot.jinjava.interpret.DisabledException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.JinjavaInterpreter.InterpreterScopeClosable;
import com.hubspot.jinjava.lib.tag.SetTag;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
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
          interpreter,
          false
        )
        .toString()
    );

    if (StringUtils.isNotBlank(tagNode.getEndName())) {
      result.append(reconstructEnd(tagNode));
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
   * Execute the specified function within an isolated context,
   *   not allowing anything new to be declared.
   * The <code>function</code> is run and will throw a
   *   <code>DeferredValueException</code> if a new value is attempted to be added
   *   to the isolated context, ie declared.
   * @param function Function to run in a child context.
   * @param interpreter JinjavaInterpreter to create a child from.
   * @param takeNewValue If a value is updated (not replaced) either take the new value or
   *                     take the previous value and put it into the
   *                     <code>EagerStringResult.prefixToPreserveState</code>.
   *                     When true, eager mode is off. When false, eager mode is on.
   * @return The combined string results of <code>declareEnabledFunction</code> and
   *   <code>function</code>
   */
  public EagerStringResult executeInChildContext(
    Function<JinjavaInterpreter, String> function,
    JinjavaInterpreter interpreter,
    boolean takeNewValue
  ) {
    return executeInChildContext(
      takeNewValue ? e -> "" : function,
      takeNewValue ? function : e -> "",
      interpreter,
      takeNewValue
    );
  }

  /**
   * Execute the specified functions within an isolated context.
   * Additionally, if the execution causes existing values on the context to become
   *   deferred, then their previous values will wrapped in a <code>set</code>
   *   tag that gets prepended to the returned result.
   * The <code>declareEnabledFunction</code> is run, allowing for new values to get
   *   added to the isolated context, ie declared. (Used in tags that may declare values)
   * The <code>declareDisabledFunction</code> is run and will throw a
   *   <code>DeferredValueException</code> if a new value is attempted to be added
   *   to the isolated context, ie declared.
   * @param declareEnabledFunction Function to run allowing declaration of variables.
   * @param declareDisabledFunction Function to run restricting declaration of variables.
   * @param interpreter JinjavaInterpreter to create a child from.
   * @param takeNewValue If a value is updated (not replaced) either take the new value or
   *                     take the previous value and put it into the
   *                     <code>EagerStringResult.prefixToPreserveState</code>.
   * @return An <code>EagerStringResult</code> where:
   *  <code>result</code> is the combined string results of
   *    <code>declareEnabledFunction</code> and <code>declareDisabledFunction</code>.
   *  <code>prefixToPreserveState</code> is either blank or a <code>set</code> tag
   *    that preserves the state within the output for a second rendering pass.
   */
  public EagerStringResult executeInChildContext(
    Function<JinjavaInterpreter, String> declareEnabledFunction,
    Function<JinjavaInterpreter, String> declareDisabledFunction,
    JinjavaInterpreter interpreter,
    boolean takeNewValue
  ) {
    StringBuilder result = new StringBuilder();
    Map<String, Integer> initiallyResolvedHashes = new HashMap<>();
    Map<String, String> initiallyResolvedAsStrings = new HashMap<>();
    interpreter
      .getContext()
      .entrySet()
      .stream()
      .filter(
        e ->
          !e.getKey().equals(GLOBAL_MACROS_SCOPE_KEY) &&
          !e.getKey().equals(IMPORT_RESOURCE_PATH_KEY)
      )
      .filter(e -> !(e.getValue() instanceof DeferredValue))
      .forEach(
        entry -> {
          initiallyResolvedHashes.put(entry.getKey(), entry.getValue().hashCode());
          try {
            initiallyResolvedAsStrings.put(
              entry.getKey(),
              ChunkResolver.getValueAsJinjavaString(entry.getValue())
            );
          } catch (JsonProcessingException jsonProcessingException) {
            // do nothing
          }
        }
      );

    try (InterpreterScopeClosable c = interpreter.enterScope()) {
      interpreter.getContext().setEagerMode(false);
      result.append(declareEnabledFunction.apply(interpreter));
      interpreter.getContext().setEagerMode(true);
      result.append(declareDisabledFunction.apply(interpreter));
    }
    Map<String, String> deferredValuesToSet = interpreter
      .getContext()
      .entrySet()
      .stream()
      .filter(e -> initiallyResolvedHashes.containsKey(e.getKey()))
      .filter(
        e -> !initiallyResolvedHashes.get(e.getKey()).equals(e.getValue().hashCode())
      )
      .collect(
        Collectors.toMap(
          Entry::getKey,
          e -> {
            try {
              if (e instanceof DeferredValue) {
                return ChunkResolver.getValueAsJinjavaString(
                  ((DeferredValue) e.getValue()).getOriginalValue()
                );
              }
              if (takeNewValue) {
                return ChunkResolver.getValueAsJinjavaString(e.getValue());
              }
              if (initiallyResolvedAsStrings.containsKey(e.getKey())) {
                return initiallyResolvedAsStrings.get(e.getKey());
              }
            } catch (JsonProcessingException ignored) {
              // pass through
            }
            // Previous value could not be mapped to a string
            throw new DeferredValueException(e.getKey());
          }
        )
      );
    if (deferredValuesToSet.size() > 0) {
      return new EagerStringResult(
        result.toString(),
        buildSetTagForDeferredInChildContext(
          deferredValuesToSet,
          interpreter,
          takeNewValue
        )
      );
    }
    return new EagerStringResult(result.toString());
  }

  public String getNewlyDeferredFunctionImages(
    Set<String> deferredWords,
    JinjavaInterpreter interpreter
  ) {
    Set<String> toRemove = new HashSet<>();
    String result = deferredWords
      .stream()
      .filter(w -> !interpreter.getContext().containsKey(w))
      .map(w -> interpreter.getContext().getGlobalMacro(w))
      .filter(Objects::nonNull)
      .peek(macro -> toRemove.add(macro.getName()))
      .filter(macro -> !macro.isDeferred())
      .peek(
        macro -> {
          macro.setDeferred(true);
          interpreter.getContext().addGlobalMacro(macro);
        }
      )
      .map(
        macro ->
          executeInChildContext(
            eagerInterpreter -> {
              try {
                return (String) macro.evaluate(
                  macro
                    .getArguments()
                    .stream()
                    .map(arg -> DeferredValue.instance())
                    .toArray()
                );
              } catch (DeferredValueException e) {
                // TODO eager reconstruct;
                return macro.reconstructImage();
              }
            },
            interpreter,
            false
          )
      )
      .map(EagerStringResult::toString)
      .collect(Collectors.joining());
    deferredWords.removeAll(toRemove);
    return result;
  }

  private String buildSetTagForDeferredInChildContext(
    Map<String, String> deferredValuesToSet,
    JinjavaInterpreter interpreter,
    boolean takeNewValue
  ) {
    if (
      interpreter.getConfig().getDisabled().containsKey(Library.TAG) &&
      interpreter.getConfig().getDisabled().get(Library.TAG).contains(SetTag.TAG_NAME)
    ) {
      throw new DisabledException("set tag disabled");
    }

    StringJoiner vars = new StringJoiner(", ");
    StringJoiner values = new StringJoiner(", ");
    deferredValuesToSet.forEach(
      (key, value) -> {
        // This ensures they are properly aligned to each other.
        vars.add(key);
        values.add(value);
      }
    );
    StringJoiner result = new StringJoiner(" ");
    result
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionStartWithTag())
      .add(SetTag.TAG_NAME)
      .add(vars.toString())
      .add("=")
      .add(values.toString())
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionEndWithTag());
    String image = result.toString();
    // Don't defer if we're sticking with the new value
    if (!takeNewValue) {
      interpreter
        .getContext()
        .handleEagerToken(
          new EagerToken(
            new TagToken(
              image,
              // TODO this line number won't be accurate, currently doesn't matter.
              interpreter.getLineNumber(),
              interpreter.getPosition(),
              interpreter.getConfig().getTokenScannerSymbols()
            ),
            Collections.emptySet(),
            deferredValuesToSet.keySet()
          )
        );
    }
    return image;
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
    joiner.add(tagToken.getSymbols().getExpressionEndWithTag());
    String newlyDeferredFunctionImages = getNewlyDeferredFunctionImages(
      chunkResolver.getDeferredWords(),
      interpreter
    );

    interpreter
      .getContext()
      .handleEagerToken(new EagerToken(tagToken, chunkResolver.getDeferredWords()));

    return (newlyDeferredFunctionImages + joiner.toString());
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
      if (interpreter.getConfig().isPreserveForFinalPass()) {
        return eagerInterpret(tagNode, interpreter);
      } else {
        throw e;
      }
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

  public String reconstructEnd(TagNode tagNode) {
    return String.format(
      "%s %s %s",
      tagNode.getSymbols().getExpressionStartWithTag(),
      tagNode.getEndName(),
      tagNode.getSymbols().getExpressionEndWithTag()
    );
  }
}

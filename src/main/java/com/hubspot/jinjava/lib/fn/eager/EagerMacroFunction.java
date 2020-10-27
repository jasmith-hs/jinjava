package com.hubspot.jinjava.lib.fn.eager;

import static com.hubspot.jinjava.interpret.Context.GLOBAL_MACROS_SCOPE_KEY;
import static com.hubspot.jinjava.interpret.Context.IMPORT_RESOURCE_PATH_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.el.ext.AbstractCallableMethod;
import com.hubspot.jinjava.interpret.DeferredValue;
import com.hubspot.jinjava.interpret.DeferredValueException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.JinjavaInterpreter.InterpreterScopeClosable;
import com.hubspot.jinjava.lib.fn.MacroFunction;
import com.hubspot.jinjava.lib.tag.MacroTag;
import com.hubspot.jinjava.util.ChunkResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class EagerMacroFunction extends AbstractCallableMethod {
  private MacroFunction macroFunction;
  private JinjavaInterpreter interpreter;

  public EagerMacroFunction(MacroFunction macroFunction, JinjavaInterpreter interpreter) {
    super(
      macroFunction.getName(),
      getLinkedHashmap(macroFunction.getArguments(), macroFunction.getDefaults())
    );
    this.macroFunction = macroFunction;
    this.interpreter = interpreter;
  }

  private static LinkedHashMap<String, Object> getLinkedHashmap(
    List<String> args,
    Map<String, Object> defaults
  ) {
    LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
    for (String arg : args) {
      linkedHashMap.put(arg, defaults.get(arg));
    }
    return linkedHashMap;
  }

  public Object doEvaluate(
    Map<String, Object> argMap,
    Map<String, Object> kwargMap,
    List<Object> varArgs
  ) {
    Optional<String> importFile = macroFunction.getImportFile(interpreter);
    try (InterpreterScopeClosable c = interpreter.enterScope()) {
      return macroFunction.getEvaluationResult(argMap, kwargMap, varArgs, interpreter);
    } finally {
      importFile.ifPresent(path -> interpreter.getContext().getCurrentPathStack().pop());
    }
  }

  public String getStartTag(JinjavaInterpreter interpreter) {
    StringJoiner argJoiner = new StringJoiner(", ");
    for (String arg : macroFunction.getArguments()) {
      try {
        if (macroFunction.getDefaults().get(arg) != null) {
          argJoiner.add(
            String.format(
              "%s=%s",
              arg,
              ChunkResolver.getValueAsJinjavaString(macroFunction.getDefaults().get(arg))
            )
          );
          continue;
        }
      } catch (JsonProcessingException ignored) {}
      argJoiner.add(arg);
    }
    return new StringJoiner(" ")
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionStartWithTag())
      .add(MacroTag.TAG_NAME)
      .add(String.format("%s(%s)", macroFunction.getName(), argJoiner.toString()))
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionEndWithTag())
      .toString();
  }

  public String getEndTag(JinjavaInterpreter interpreter) {
    return new StringJoiner(" ")
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionStartWithTag())
      .add(String.format("end%s", MacroTag.TAG_NAME))
      .add(interpreter.getConfig().getTokenScannerSymbols().getExpressionEndWithTag())
      .toString();
  }

  public String reconstructImage() {
    String result;
    try {
      result =
        (String) evaluate(
          macroFunction
            .getArguments()
            .stream()
            .map(arg -> DeferredValue.instance())
            .toArray()
        );
    } catch (DeferredValueException ignored) {
      return macroFunction.reconstructImage();
    }

    return (getStartTag(interpreter) + result + getEndTag(interpreter));
  }
}

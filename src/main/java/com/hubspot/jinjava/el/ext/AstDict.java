package com.hubspot.jinjava.el.ext;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateStateException;
import com.hubspot.jinjava.objects.collections.SizeLimitingPyMap;
import de.odysseus.el.tree.Bindings;
import de.odysseus.el.tree.impl.ast.AstIdentifier;
import de.odysseus.el.tree.impl.ast.AstLiteral;
import de.odysseus.el.tree.impl.ast.AstNode;
import de.odysseus.el.tree.impl.ast.AstString;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.el.ELContext;

public class AstDict extends AstLiteral {
  private final Map<AstNode, AstNode> dict;

  public AstDict(Map<AstNode, AstNode> dict) {
    this.dict = dict;
  }

  @Override
  public Object eval(Bindings bindings, ELContext context) {
    Map<String, Object> resolved = new LinkedHashMap<>();

    for (Map.Entry<AstNode, AstNode> entry : dict.entrySet()) {
      String key;

      if (entry.getKey() instanceof AstString) {
        key = Objects.toString(entry.getKey().eval(bindings, context));
      } else if (entry.getKey() instanceof AstIdentifier) {
        key = ((AstIdentifier) entry.getKey()).getName();
      } else {
        throw new TemplateStateException(
          "Dict key must be a string or identifier, was: " + entry.getKey()
        );
      }

      resolved.put(key, entry.getValue().eval(bindings, context));
    }

    JinjavaInterpreter interpreter = (JinjavaInterpreter) context
      .getELResolver()
      .getValue(context, null, ExtendedParser.INTERPRETER);

    return new SizeLimitingPyMap(resolved, interpreter.getConfig().getMaxMapSize());
  }

  @Override
  public void appendStructure(StringBuilder builder, Bindings bindings) {
    throw new UnsupportedOperationException(
      "appendStructure not implemented in " + getClass().getSimpleName()
    );
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("{");

    for (Map.Entry<AstNode, AstNode> entry : dict.entrySet()) {
      s.append(entry.getKey()).append(":").append(entry.getValue());
    }

    return s.append("}").toString();
  }
}

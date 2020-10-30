package com.hubspot.jinjava.tree.parse;

import com.google.common.base.Strings;

public class EagerExpressionToken extends TagToken {
  private static final TokenScannerSymbols EAGER_EXPRESSION_SYMBOLS = new EagerExpressionTokenScannerSymbols();
  private String rawTagName;
  private String expression;

  public EagerExpressionToken(
    String image,
    int lineNumber,
    int startPosition,
    String expression
  ) {
    super(image, lineNumber, startPosition, EAGER_EXPRESSION_SYMBOLS);
    this.expression = Strings.nullToEmpty(expression);
  }

  @Override
  protected void parse() {
    this.rawTagName = "";
  }

  @Override
  public String getRawTagName() {
    return rawTagName;
  }

  @Override
  public String getTagName() {
    return rawTagName;
  }

  @Override
  public String getHelpers() {
    return expression;
  }

  @Override
  public String toString() {
    return "{{ " + expression + " }}";
  }
}

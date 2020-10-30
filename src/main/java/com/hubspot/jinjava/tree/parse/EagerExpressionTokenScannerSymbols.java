package com.hubspot.jinjava.tree.parse;

public class EagerExpressionTokenScannerSymbols extends DefaultTokenScannerSymbols {
  public static final String EXPRESSION_START_WITH_TAG = "{{";
  public static final String EXPRESSION_END_WITH_TAG = "}}";

  public String getExpressionStartWithTag() {
    return EXPRESSION_START_WITH_TAG;
  }

  public String getExpressionEndWithTag() {
    return EXPRESSION_END_WITH_TAG;
  }
}

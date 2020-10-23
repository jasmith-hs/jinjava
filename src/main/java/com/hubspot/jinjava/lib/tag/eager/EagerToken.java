package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.tree.parse.Token;
import java.util.Set;

public class EagerToken {
  private Token token;
  private Set<String> deferredWords;

  public EagerToken(Token token, Set<String> deferredWords) {
    this.token = token;
    this.deferredWords = deferredWords;
  }

  public Token getToken() {
    return token;
  }

  public Set<String> getDeferredWords() {
    return deferredWords;
  }
}

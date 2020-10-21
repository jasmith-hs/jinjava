package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.lib.tag.DoTag;

public class EagerDoTag extends EagerStateChangingTag<DoTag> {

  public EagerDoTag() {
    super(new DoTag());
  }

  public EagerDoTag(DoTag doTag) {
    super(doTag);
  }
}

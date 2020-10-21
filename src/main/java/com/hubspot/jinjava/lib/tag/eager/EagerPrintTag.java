package com.hubspot.jinjava.lib.tag.eager;

import com.hubspot.jinjava.lib.tag.PrintTag;

public class EagerPrintTag extends EagerStateChangingTag<PrintTag> {

  public EagerPrintTag() {
    super(new PrintTag());
  }

  public EagerPrintTag(PrintTag printTag) {
    super(printTag);
  }
}

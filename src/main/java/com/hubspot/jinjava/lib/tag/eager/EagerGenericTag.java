package com.hubspot.jinjava.lib.tag.eager;

import com.google.common.collect.ImmutableSet;
import com.hubspot.jinjava.lib.tag.DoTag;
import com.hubspot.jinjava.lib.tag.PrintTag;
import com.hubspot.jinjava.lib.tag.Tag;
import java.util.Set;

public class EagerGenericTag<T extends Tag> extends EagerTagDecorator<T> implements Tag {
  public static final Set<Class<? extends Tag>> SUPPORTED_CLASSES = ImmutableSet.of(
    PrintTag.class,
    DoTag.class
  );

  // Print, Do,

  public EagerGenericTag(T tag) {
    super(tag);
  }
}

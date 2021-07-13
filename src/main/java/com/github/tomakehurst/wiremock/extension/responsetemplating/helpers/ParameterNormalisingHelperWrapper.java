package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import java.io.IOException;

public class ParameterNormalisingHelperWrapper implements Helper<Object> {

  private final Helper<Object> target;

  public ParameterNormalisingHelperWrapper(Helper<Object> helper) {
    this.target = helper;
  }

  @Override
  public Object apply(Object context, Options options) throws IOException {
    Object newContext = unwrapValue(context);

    for (int i = 0; i < options.params.length; i++) {
      options.params[i] = unwrapValue(options.param(i));
    }
    options.hash.forEach((key, value) -> options.hash.put(key, unwrapValue(value)));

    return target.apply(newContext, options);
  }

  private Object unwrapValue(Object value) {
    Object newValue = value;
    if (value != null && ListOrSingle.class.isAssignableFrom(value.getClass())) {
      ListOrSingle<?> listOrSingle = (ListOrSingle<?>) value;
      if (listOrSingle.size() == 1) {
        newValue = listOrSingle.getFirst();
      }
    }

    return newValue;
  }
}

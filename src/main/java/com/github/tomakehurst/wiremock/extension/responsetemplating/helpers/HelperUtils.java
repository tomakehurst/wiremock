package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

public class HelperUtils {

  public static Integer coerceToInt(Object value) {
    if (value == null) {
      return null;
    }

    if (Number.class.isAssignableFrom(value.getClass())) {
      return ((Number) value).intValue();
    }

    if (CharSequence.class.isAssignableFrom(value.getClass())) {
      return Integer.parseInt(value.toString());
    }

    return null;
  }

  public static Double coerceToDouble(Object value) {
    if (value == null) {
      return null;
    }

    if (Number.class.isAssignableFrom(value.getClass())) {
      return ((Number) value).doubleValue();
    }

    if (CharSequence.class.isAssignableFrom(value.getClass())) {
      return Double.parseDouble(value.toString());
    }

    return null;
  }
}

/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathsHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    if (options.params.length != 2) {
      return handleError(
          "All maths functions require two operands and an operator as parameters e.g. 3 '+' 2");
    }

    BigDecimal left = coerceToBigDecimal(context);
    String operator = options.params[0].toString();
    BigDecimal right = coerceToBigDecimal(options.params[1]);

    BigDecimal result = null;
    switch (operator) {
      case "+":
        result = left.add(right);
        break;
      case "-":
        result = left.subtract(right);
        break;
      case "*":
      case "x":
        result = left.multiply(right);
        break;
      case "/":
        result = left.divide(right, RoundingMode.HALF_UP);
        break;
      case "%":
        result = left.remainder(right);
        break;
      default:
        return handleError(operator + " is not a valid mathematical operator");
    }

    return reduceToPrimitiveNumber(result);
  }

  private static BigDecimal coerceToBigDecimal(Object value) {
    if (value instanceof Integer) {
      return new BigDecimal((int) value);
    }

    if (value instanceof Long) {
      return new BigDecimal((long) value);
    }

    if (value instanceof Double) {
      return BigDecimal.valueOf((double) value);
    }

    if (value instanceof Float) {
      return BigDecimal.valueOf((float) value);
    }

    if (value instanceof CharSequence) {
      return new BigDecimal(value.toString());
    }

    return new BigDecimal(0);
  }

  private static Object reduceToPrimitiveNumber(BigDecimal value) {
    if (value == null) {
      return null;
    }

    if (value.scale() == 0) {
      if (value.longValue() <= Integer.MAX_VALUE) {
        return value.intValue();
      }

      return value.longValue();
    }

    return value.doubleValue();
  }
}

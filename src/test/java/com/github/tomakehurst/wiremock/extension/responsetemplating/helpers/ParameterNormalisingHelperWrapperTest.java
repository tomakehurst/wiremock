package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import java.io.IOException;
import org.junit.Test;

public class ParameterNormalisingHelperWrapperTest extends HandlebarsHelperTestBase {

  @Test
  public void unwrapsListOrSingleWhenSingle() throws Exception {
    ListOrSingle<Object> value = new ListOrSingle<>("one");
    ObjectTestHelper helper = new ObjectTestHelper();
    ParameterNormalisingHelperWrapper wrapper = new ParameterNormalisingHelperWrapper(helper);

    wrapper.apply(
        value, createOptions(map("hashThings", new ListOrSingle<>(42)), new ListOrSingle<>(3.14)));

    assertThat(helper.context, instanceOf(String.class));
    assertThat(helper.context, is("one"));

    assertThat(helper.options.hash("hashThings"), instanceOf(Integer.class));
    assertThat(helper.options.hash("hashThings"), is(42));

    assertThat(helper.options.param(0), instanceOf(Double.class));
    assertThat(helper.options.param(0), is(3.14));
  }

  @Test
  public void doesNotUnwrapListOrSingleWhenNotSingle() throws Exception {
    ListOrSingle<Object> value = new ListOrSingle<>("one", "two");
    ObjectTestHelper helper = new ObjectTestHelper();
    ParameterNormalisingHelperWrapper wrapper = new ParameterNormalisingHelperWrapper(helper);

    wrapper.apply(
        value,
        createOptions(
            map("hashThings", new ListOrSingle<>(41, 42)), new ListOrSingle<>(0.1, 3.14)));

    assertThat(helper.context, instanceOf(ListOrSingle.class));
    assertThat(helper.options.hash("hashThings"), instanceOf(ListOrSingle.class));
    assertThat(helper.options.param(0), instanceOf(ListOrSingle.class));
  }

  public static class ObjectTestHelper extends HandlebarsHelper<Object> {
    public Object context;
    public Options options;

    @Override
    public Object apply(Object context, Options options) throws IOException {
      this.context = context;
      this.options = options;
      return null;
    }
  }
}

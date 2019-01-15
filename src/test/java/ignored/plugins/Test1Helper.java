package ignored.plugins;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class Test1Helper implements Helper<Object> {

	@Override
	public Object apply(Object arg0, Options arg1) throws IOException {
		// Just test
		return null;
	}

}

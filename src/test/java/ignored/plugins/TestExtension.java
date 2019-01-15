package ignored.plugins;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

public class TestExtension extends ResponseTransformer {

	@Override
	public String getName() {
		return "Test transformer";
	}

	@Override
	public Response transform(Request arg0, Response arg1, FileSource arg2, Parameters arg3) {
		//Do nothing
		return null;
	}

}

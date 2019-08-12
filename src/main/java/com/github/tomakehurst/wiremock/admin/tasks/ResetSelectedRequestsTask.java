package com.github.tomakehurst.wiremock.admin.tasks;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.verification.VerificationResult;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.common.Json.write;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Created by ksaleh on 11/14/17.
 */
public class ResetSelectedRequestsTask implements AdminTask {
    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        RequestPattern requestPattern = Json.read(request.getBodyAsString(), RequestPattern.class);
        admin.resetSelectedRequests(requestPattern);

        return responseDefinition()
                .withStatus(HTTP_OK)
                .build();
    }
}

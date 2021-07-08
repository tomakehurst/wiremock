package com.github.tomakehurst.wiremock.stubbing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.List;

public class ResponseSequence {

    private List<ResponseDefinition> responses;
    private int responseSequenceIndex = 0;
    private boolean loopResponseSequence;

    public ResponseSequence() {
    }

    public ResponseSequence(List<ResponseDefinition> responses, boolean loopResponseSequence) {
        this.responses = responses;
        this.loopResponseSequence = loopResponseSequence;
    }

    @JsonIgnore
    public ResponseDefinition yieldResponse() {
        if (responseSequenceIndex == responses.size()) {
            return ResponseDefinition.ok();
        }

        ResponseDefinition definition = responses.get(responseSequenceIndex++);
        if (loopResponseSequence && responseSequenceIndex  == responses.size()) {
            responseSequenceIndex = 0;
        }
        return definition;
    }

    public boolean isLoopResponseSequence() {
        return loopResponseSequence;
    }

    public void setLoopResponseSequence(boolean loopResponseSequence) {
        this.loopResponseSequence = loopResponseSequence;
    }

    public List<ResponseDefinition> getResponses() {
        return responses;
    }

    public void setResponses(List<ResponseDefinition> responses) {
        this.responses = responses;
    }

}

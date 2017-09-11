import {StubMapping} from './stub-mapping';
import {ResponseDefinition} from './response-definition';
import {Item} from './item';
import {LoggedRequest} from './logged-request';
import {LoggedResponse} from './logged-response';

export class ServeEvent implements Item {
  id: string;
  request: LoggedRequest;
  stubMapping: StubMapping;
  responseDefinition: ResponseDefinition;
  response: LoggedResponse;
  wasMatched: boolean;

  getTitle(): string {
    return this.request.url;
  }

  getSubtitle(): string {
    return this.request.getSubtitle() + ', status=' + this.response.status;
  }

  getId(): string {
    return this.id;
  }

  deserialize(unchecked: ServeEvent): ServeEvent {
    this.id = unchecked.id;
    this.request = new LoggedRequest().deserialize(unchecked.request);
    this.stubMapping = new StubMapping().deserialize(unchecked.stubMapping);
    this.responseDefinition = unchecked.responseDefinition;
    this.response = unchecked.response;
    this.wasMatched = unchecked.wasMatched;

    return this;
  }
}

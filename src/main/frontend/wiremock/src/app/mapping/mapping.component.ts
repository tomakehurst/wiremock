import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {StubMapping} from '../wiremock/model/stub-mapping';
import {DataEntries, Entry} from '../code-entry-list/code-entry-list.component';
import {UtilService} from 'app/services/util.service';

@Component({
  selector: 'wm-mapping',
  templateUrl: './mapping.component.html',
  styleUrls: ['./mapping.component.scss']
})
export class MappingComponent implements OnInit, OnChanges {

  @Input('selectedMapping')
  selectedMapping: StubMapping | null;
  code: string;

  general: DataEntries;
  request: DataEntries;
  responseDefinition: DataEntries;


  constructor() {
  }

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.code = UtilService.toJson(this.selectedMapping);
    this.general = this.getGeneral();
    this.request = this.getRequest();
    this.responseDefinition = this.getResponseDefinition();
  }

  isVisible(): boolean {
    return UtilService.isDefined(this.selectedMapping);
  }

  getGeneral() {
    const dataEntries = new DataEntries();
    if (this.selectedMapping == null || typeof this.selectedMapping === 'undefined') {
      return dataEntries;
    }
    dataEntries.addEntry(new Entry('uuid', this.selectedMapping.uuid, 'plain'));
    dataEntries.addEntry(new Entry('name', this.selectedMapping.name, 'plain'));
    dataEntries.addEntry(new Entry('priority', this.selectedMapping.priority, 'plain'));
    dataEntries.addEntry(new Entry('scenarioName', this.selectedMapping.scenarioName, 'plain'));
    dataEntries.addEntry(new Entry('requiredScenarioState', this.selectedMapping.requiredScenarioState, 'plain'));
    dataEntries.addEntry(new Entry('newScenarioState', this.selectedMapping.newScenarioState, 'plain'));

    return dataEntries;
  }

  getRequest() {
    const dataEntries = new DataEntries();
    if (this.selectedMapping == null || typeof this.selectedMapping === 'undefined') {
      return dataEntries;
    }

    dataEntries.addEntry(new Entry('url', this.selectedMapping.request.url, 'plain'));
    dataEntries.addEntry(new Entry('urlPattern', this.selectedMapping.request.urlPattern, 'plain'));
    dataEntries.addEntry(new Entry('urlPath', this.selectedMapping.request.urlPath, 'plain'));
    dataEntries.addEntry(new Entry('urlPathPattern', this.selectedMapping.request.urlPathPattern, 'plain'));
    dataEntries.addEntry(new Entry('urlQueryParams', UtilService.getParametersOfUrl(this.selectedMapping.request.url), '', 'params'));
    dataEntries.addEntry(new Entry('method', this.selectedMapping.request.method, 'plain'));
    dataEntries.addEntry(new Entry('headers', UtilService.toJson(this.selectedMapping.request.headers), 'json'));
    dataEntries.addEntry(new Entry('queryParameters', this.selectedMapping.request.queryParameters, ''));
    dataEntries.addEntry(new Entry('cookies', UtilService.toJson(this.selectedMapping.request.cookies), 'json'));
    dataEntries.addEntry(new Entry('basicAuth', UtilService.toJson(this.selectedMapping.request.basicAuth), 'json'));
    dataEntries.addEntry(new Entry('bodyPatterns', UtilService.toJson(this.selectedMapping.request.bodyPatterns), 'json'));
    dataEntries.addEntry(new Entry('customMatcher', UtilService.toJson(this.selectedMapping.request.customMatcher), 'json'));

    return dataEntries;
  }

  getResponseDefinition() {
    const dataEntries = new DataEntries();
    if (this.selectedMapping == null || typeof this.selectedMapping === 'undefined') {
      return dataEntries;
    }
    dataEntries.addEntry(new Entry('status', this.selectedMapping.response.status, 'plain'));
    dataEntries.addEntry(new Entry('statusMessage', this.selectedMapping.response.statusMessage, 'plain'));
    dataEntries.addEntry(new Entry('body', this.selectedMapping.response.body, ''));
    dataEntries.addEntry(new Entry('jsonBody', this.selectedMapping.response.jsonBody, ''));
    dataEntries.addEntry(new Entry('base64Body', this.selectedMapping.response.base64Body, 'plain'));
    dataEntries.addEntry(new Entry('bodyFileName', this.selectedMapping.response.bodyFileName, 'plain'));
    dataEntries.addEntry(new Entry('headers', UtilService.toJson(this.selectedMapping.response.headers), 'json'));
    dataEntries.addEntry(new Entry('additionalProxy RequestHeaders',
      UtilService.toJson(this.selectedMapping.response.additionalProxyRequestHeaders), ''));
    dataEntries.addEntry(new Entry('fixedDelayMilliseconds', this.selectedMapping.response.fixedDelayMilliseconds, 'plain'));
    dataEntries.addEntry(new Entry('delayDistribution', this.selectedMapping.response.delayDistribution, ''));
    dataEntries.addEntry(new Entry('proxyBaseUrl', this.selectedMapping.response.proxyBaseUrl, 'plain'));
    dataEntries.addEntry(new Entry('fault', this.selectedMapping.response.fault, ''));
    dataEntries.addEntry(new Entry('transformers', UtilService.toJson(this.selectedMapping.response.transformers), 'json'));
    dataEntries.addEntry(new Entry('transformerParameters',
      UtilService.toJson(this.selectedMapping.response.transformerParameters), 'json'));
    dataEntries.addEntry(new Entry('fromConfiguredStub', this.selectedMapping.response.fromConfiguredStub, 'plain'));
    // dataEntries.addEntry(new Entry('isProxyingEnabled', this.selectedMapping.response.fromConfiguredStub, 'plain'));

    return dataEntries;
  }
}

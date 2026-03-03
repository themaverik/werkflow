/**
 * Flowable Properties Provider Module
 *
 * bpmn-js compatible module definition for the Flowable properties provider.
 * Used as an additionalModule in BpmnModeler to replace the default
 * CamundaPlatformPropertiesProviderModule.
 */
import FlowablePropertiesProvider from './flowable-properties-provider'

const FlowablePropertiesProviderModule = {
  __init__: ['flowablePropertiesProvider'],
  flowablePropertiesProvider: ['type', FlowablePropertiesProvider]
}

export default FlowablePropertiesProviderModule

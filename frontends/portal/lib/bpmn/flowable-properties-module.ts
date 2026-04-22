/**
 * Flowable Properties Provider Module
 *
 * bpmn-js compatible module definition for the Flowable properties provider.
 * Used as an additionalModule in BpmnModeler to replace the default
 * CamundaPlatformPropertiesProviderModule.
 */
import FlowablePropertiesProvider from './flowable-properties-provider'
import ActionBlockRenderer from './action-block-renderer'
import WerkflowPaletteProvider from './werkflow-palette-provider'

const FlowablePropertiesProviderModule = {
  __init__: ['flowablePropertiesProvider', 'actionBlockRenderer', 'werkflowPaletteProvider'],
  flowablePropertiesProvider: ['type', FlowablePropertiesProvider],
  actionBlockRenderer: ['type', ActionBlockRenderer],
  werkflowPaletteProvider: ['type', WerkflowPaletteProvider],
}

export default FlowablePropertiesProviderModule

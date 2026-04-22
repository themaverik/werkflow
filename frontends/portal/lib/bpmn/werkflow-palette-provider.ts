/**
 * WerkflowPaletteProvider
 *
 * Extends the default bpmn-js palette with a Business Rule Task entry.
 * Business Rule Tasks use a DMN decision table (flowable:decisionRef) to
 * route workflow decisions — the recommended pattern for DoA routing.
 *
 * bpmn-js palette providers implement two methods:
 *   getPaletteEntries() — returns an object of palette entry descriptors
 */
export default class WerkflowPaletteProvider {
  static $inject = ['palette', 'create', 'elementFactory', 'spaceTool', 'lassoTool', 'handTool']

  private _create: any
  private _elementFactory: any

  constructor(
    palette: any,
    create: any,
    elementFactory: any,
    _spaceTool: any,
    _lassoTool: any,
    _handTool: any
  ) {
    this._create = create
    this._elementFactory = elementFactory
    palette.registerProvider(this)
  }

  getPaletteEntries(): Record<string, any> {
    const { _create: create, _elementFactory: elementFactory } = this

    return {
      'create.business-rule-task': {
        group: 'activity',
        className: 'bpmn-icon-business-rule-task',
        title: 'Create Business Rule Task (DMN)',
        action: {
          dragstart: createBusinessRuleTask,
          click: createBusinessRuleTask,
        },
      },
    }

    function createBusinessRuleTask(event: any) {
      const shape = elementFactory.createShape({
        type: 'bpmn:BusinessRuleTask',
      })
      create.start(event, shape)
    }
  }
}

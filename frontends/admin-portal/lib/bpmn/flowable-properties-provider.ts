/**
 * Flowable Properties Provider
 *
 * Extends BPMN properties panel with Flowable-specific properties:
 * - Assignee (user assignment)
 * - Candidate Users (potential assignees)
 * - Candidate Groups (group assignments)
 * - Form Key (link to Form.io form)
 * - Priority
 * - Due Date Expression
 */

import { is } from 'bpmn-js/lib/util/ModelUtil'

/**
 * Flowable properties provider for user tasks
 */
export default function FlowablePropertiesProvider(
  propertiesPanel: any,
  injector: any
) {
  this._propertiesPanel = propertiesPanel
  this._injector = injector

  // Register Flowable-specific groups
  propertiesPanel.registerProvider(500, this)
}

FlowablePropertiesProvider.$inject = ['propertiesPanel', 'injector']

/**
 * Get Flowable-specific property groups
 */
FlowablePropertiesProvider.prototype.getGroups = function (element: any) {
  return (groups: any[]) => {
    // Only add Flowable properties for User Tasks
    if (is(element, 'bpmn:UserTask')) {
      // Add Flowable assignment group after the general group
      const generalIdx = groups.findIndex((g: any) => g.id === 'general')

      groups.splice(generalIdx + 1, 0, {
        id: 'flowable-assignment',
        label: 'Assignment',
        entries: [
          {
            id: 'assignee',
            element,
            component: AssigneeEntry,
            isEdited: isTextFieldEntryEdited
          },
          {
            id: 'candidateUsers',
            element,
            component: CandidateUsersEntry,
            isEdited: isTextFieldEntryEdited
          },
          {
            id: 'candidateGroups',
            element,
            component: CandidateGroupsEntry,
            isEdited: isTextFieldEntryEdited
          }
        ]
      })

      // Add Flowable forms group
      groups.splice(generalIdx + 2, 0, {
        id: 'flowable-forms',
        label: 'Forms',
        entries: [
          {
            id: 'formKey',
            element,
            component: FormKeyEntry,
            isEdited: isTextFieldEntryEdited
          }
        ]
      })

      // Add Flowable task configuration group
      groups.splice(generalIdx + 3, 0, {
        id: 'flowable-task',
        label: 'Task Configuration',
        entries: [
          {
            id: 'priority',
            element,
            component: PriorityEntry,
            isEdited: isTextFieldEntryEdited
          },
          {
            id: 'dueDate',
            element,
            component: DueDateEntry,
            isEdited: isTextFieldEntryEdited
          }
        ]
      })
    }

    return groups
  }
}

/**
 * Entry components for Flowable properties
 */

function AssigneeEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.assignee || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      assignee: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Assignee'),
    description: translate('User ID or expression (e.g., ${initiator})'),
    getValue,
    setValue,
    debounce
  }
}

function CandidateUsersEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.candidateUsers || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      candidateUsers: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Candidate Users'),
    description: translate('Comma-separated user IDs (e.g., user1,user2)'),
    getValue,
    setValue,
    debounce
  }
}

function CandidateGroupsEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.candidateGroups || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      candidateGroups: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Candidate Groups'),
    description: translate('Comma-separated group IDs (e.g., HR_ADMIN,MANAGER)'),
    getValue,
    setValue,
    debounce
  }
}

function FormKeyEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.formKey || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      formKey: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Form Key'),
    description: translate('Form.io form key (e.g., leave-request-form)'),
    getValue,
    setValue,
    debounce
  }
}

function PriorityEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.priority || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      priority: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Priority'),
    description: translate('Task priority (0-100 or expression)'),
    getValue,
    setValue,
    debounce
  }
}

function DueDateEntry(props: any) {
  const { element, id } = props
  const modeling = props.modeling || props.injector.get('modeling')
  const translate = props.translate || ((s: string) => s)
  const debounce = props.debounce || ((fn: Function) => fn)

  const getValue = () => {
    return element.businessObject.dueDate || ''
  }

  const setValue = (value: string) => {
    modeling.updateProperties(element, {
      dueDate: value || undefined
    })
  }

  return {
    id,
    element,
    label: translate('Due Date'),
    description: translate('ISO date or expression (e.g., ${now() + duration(\'P7D\')})'),
    getValue,
    setValue,
    debounce
  }
}

/**
 * Helper function to check if a text field entry is edited
 */
function isTextFieldEntryEdited(entry: any) {
  const { getValue } = entry
  return getValue && getValue() !== ''
}

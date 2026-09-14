# Add a Department to Workflow Routing

Follow these steps when a new organisational department needs to participate in workflow routing.

Why no code or BPMN change is needed — the department passthrough layer — is explained in
[Flowable Candidate Group Resolution](../explanation/Flowable-Group-Resolution.md).

---

## Steps

1. **Provision users in Keycloak.** Set the `department` attribute on user accounts (or their Keycloak group) to the department code string (e.g. `Legal`).

2. **Set `custodianDeptCode` on domain records.** When creating `InventoryCategory` or other records that drive department-scoped task routing, set `custodianDeptCode` to the same department code string.

3. **Done.** No code changes, no configuration changes, no BPMN changes are required. The department code flows through `FlowableGroupResolver` automatically and matches the process variable set at process start.

---

## Keeping department codes consistent

The only ongoing requirement is that the department code string matches between Keycloak user
attributes and domain record fields. These values should originate from the same source (e.g. an
HR system or a department reference table) to prevent drift.

# Add a Role with Approval Authority

Follow these steps when a new Keycloak role needs to carry workflow approval authority.

Why the model works this way — the seven resolution layers, the DOA threshold table and the
department passthrough — is explained in
[Flowable Candidate Group Resolution](../explanation/Flowable-Group-Resolution.md).

---

## Steps

1. **Define the role in Keycloak.** Add the role to `werkflow-realm.json` with a description that records its intended authority level and approval scope.

2. **Determine the DOA level.** Based on the approval limits the role should carry, identify which `DOA_LN` level it corresponds to. Consult the `doa_threshold` table for the relevant tenant to understand what amounts each level covers.

3. **Add the mapping to application.yml.** Under `app.flowable.role-mappings`, add an entry for the new role name. Include all DOA levels up to and including the level the role carries, preserving the inheritance pattern.

   Example for a new `doa_approver_level2_alt` role that maps to L2 authority:
   ```yaml
   doa_approver_level2_alt: [DOA_L1, DOA_L2]
   ```

4. **Assign the role and `doa_level` attribute to users in Keycloak.** The `doa_level` attribute is used for display and reporting; the role is what drives group resolution.

5. **Configure amount thresholds for the tenant.** If the new role introduces a new DOA level, insert the corresponding threshold row(s) via the admin UI. If it maps to an existing level, no threshold change is needed.

6. **No BPMN changes required.** Any task that specifies `candidateGroups="DOA_L2"` will automatically become visible to users carrying this new role. Routing is driven by group membership, not by role name.

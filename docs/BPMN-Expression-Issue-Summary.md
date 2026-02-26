# BPMN Expression Issue - Executive Summary

**Date**: 2025-12-17
**Issue**: Engine service failing to start due to invalid Flowable EL syntax
**Impact**: Critical - All BPMN workflows using REST calls are broken
**Status**: Solution provided, awaiting implementation

---

## The Problem in 30 Seconds

The BPMN file `capex-approval-process-v2.bpmn20.xml` (currently disabled) attempts to construct JSON payloads using string concatenation:

```xml
<!-- THIS IS BROKEN -->
<flowable:expression>${'{"title":"' + title + '","description":"' + description + '"}'}</flowable:expression>
```

**Why it fails:**
1. No JSON escaping - quotes in values break the string
2. Type issues - numbers and booleans become strings
3. Null handling - null variables become string "null"
4. Unmaintainable - 13 fields in one expression
5. Missing beans - `restServiceDelegate` doesn't exist

**Current state:**
- `capex-approval-process-v2.bpmn20.xml.disabled` - Broken, disabled to allow engine to start
- `capex-approval-process.bpmn20.xml` - Working, uses method call pattern (good reference)

---

## The Solution in 30 Seconds

**Use custom Java delegates instead of inline expressions.**

```java
// Java Delegate (services/engine/src/main/java/.../CapExServiceDelegate.java)
@Component("capexServiceDelegate")
public class CapExServiceDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // Extract variables
        Map<String, Object> data = Map.of(
            "title", execution.getVariable("title"),
            "amount", execution.getVariable("requestAmount")
        );

        // Make REST call (Jackson auto-serializes to JSON)
        restTemplate.postForEntity(url, data, Map.class);
    }
}
```

```xml
<!-- BPMN File - Clean and simple -->
<serviceTask id="createRequest"
             flowable:delegateExpression="${capexServiceDelegate}">
  <extensionElements>
    <flowable:executionListener event="start"
      expression="${execution.setVariable('operation', 'CREATE_REQUEST')}"/>
  </extensionElements>
</serviceTask>
```

---

## Why This Solution is Better

| Aspect | String Concatenation (BAD) | Java Delegate (GOOD) |
|--------|---------------------------|----------------------|
| **JSON Escaping** | Manual, error-prone | Automatic via Jackson |
| **Type Safety** | None - everything is string | Compile-time checks |
| **Null Handling** | Breaks with "null" string | Proper JSON null |
| **Maintainability** | 200-char expression line | Clean Java code |
| **Testing** | Cannot unit test | Easy mocking |
| **Error Handling** | No control | Full try-catch |
| **Logging** | None | Full SLF4J logging |
| **Debugging** | Impossible | Stack traces |

---

## Implementation Checklist

**Priority: P0 - Critical**

### Step 1: Create Delegates (2 hours)
- [ ] Create `CapExServiceDelegate.java`
- [ ] Create `ProcurementServiceDelegate.java`
- [ ] Create `InventoryServiceDelegate.java`
- [ ] Create `NotificationServiceDelegate.java`

### Step 2: Update BPMN Files (1 hour)
- [ ] Fix `capex-approval-process-v2.bpmn20.xml`
- [ ] Remove `.disabled` suffix after testing
- [ ] Update `procurement-approval-process.bpmn20.xml`
- [ ] Update `asset-transfer-approval-process.bpmn20.xml`

### Step 3: Test (2 hours)
- [ ] Unit test each delegate
- [ ] Integration test with Finance service
- [ ] End-to-end workflow test

### Step 4: Deploy (1 hour)
- [ ] Build Docker images
- [ ] Deploy to Docker Compose environment
- [ ] Verify engine starts without errors
- [ ] Run smoke test

**Total Time: 6 hours (1 day)**

---

## Quick Links

- **Full architectural analysis**: `/docs/BPMN-JSON-Expression-Architecture-Solution.md`
- **Implementation guide**: `/docs/BPMN-Delegate-Implementation-Quick-Start.md`
- **Broken BPMN file**: `/services/engine/src/main/resources/processes/capex-approval-process-v2.bpmn20.xml.disabled`
- **Working reference**: `/services/engine/src/main/resources/processes/capex-approval-process.bpmn20.xml`
- **Example delegate**: `/services/engine/src/main/java/com/werkflow/engine/delegate/TaskAssignmentDelegate.java`

---

## Files to Create

```
services/engine/src/main/java/com/werkflow/engine/delegate/
├── CapExServiceDelegate.java          (NEW - 200 lines)
├── ProcurementServiceDelegate.java    (NEW - 180 lines)
├── InventoryServiceDelegate.java      (NEW - 150 lines)
└── NotificationServiceDelegate.java   (NEW - 100 lines)

services/engine/src/main/java/com/werkflow/engine/config/
└── RestTemplateConfig.java            (NEW - 20 lines, if not exists)
```

---

## Files to Update

```
services/engine/src/main/resources/processes/
├── capex-approval-process-v2.bpmn20.xml.disabled → .bpmn20.xml (after fixes)
├── procurement-approval-process.bpmn20.xml        (update service tasks)
└── asset-transfer-approval-process.bpmn20.xml     (update service tasks)
```

---

## Alternative Considered (Not Recommended)

**Option**: Use Flowable's built-in HTTP task with Groovy scripts

**Why rejected:**
- Still requires scripting knowledge
- Groovy adds complexity and runtime overhead
- Harder to test and debug
- Not type-safe
- BPMN files become cluttered

---

## Long-Term Vision

1. **Phase 1 (Now)**: Implement service-specific delegates
2. **Phase 2 (Week 2)**: Add resilience patterns (retry, circuit breaker)
3. **Phase 3 (Month 1)**: Consider generic delegate if patterns emerge
4. **Phase 4 (Month 2)**: Move to event-driven architecture (async messaging)

---

## Key Takeaways

1. **Never construct JSON in BPMN expressions** - Use Java delegates
2. **Keep BPMN files simple** - Business logic belongs in code
3. **Leverage Spring Boot** - RestTemplate, Jackson, etc. handle complexity
4. **Test delegates thoroughly** - Unit tests prevent runtime failures
5. **Use existing patterns** - `capex-approval-process.bpmn20.xml` (v1) works correctly

---

## Questions & Answers

**Q: Why not just escape the quotes properly?**
A: Even with escaping, you still have type issues, null handling, and unmaintainable 200-character expressions.

**Q: Can we use the original `#{{}}` syntax?**
A: No, that syntax doesn't exist in Flowable's Expression Language. It was never valid.

**Q: What about Flowable's HTTP task?**
A: It's built-in but still requires manual JSON construction. Custom delegates are cleaner.

**Q: How do we test BPMN files before deployment?**
A: Unit test the delegates, then integration test the full workflow with mocked services.

**Q: Can we reuse delegates across workflows?**
A: Yes! One `CapExServiceDelegate` can support multiple operations via the `operation` variable.

**Q: What if Finance service is down?**
A: Delegate throws exception, Flowable marks workflow as failed, admin can retry or rollback.

---

## Success Criteria

After implementation, you should see:

✅ Engine starts without errors
✅ No `.disabled` BPMN files
✅ All service tasks use Java delegates
✅ Unit tests pass for all delegates
✅ Integration tests pass for key workflows
✅ Logs show successful REST calls
✅ Workflow instances complete successfully

---

**Next Steps**: Choose one option:

1. **DIY**: Use the quick-start guide to implement delegates yourself
2. **Review First**: Read the full architecture document for deeper understanding
3. **Request Help**: Ask for pair programming session to implement first delegate together

---

**Document Status**: Ready for Implementation
**Estimated Effort**: 1 day (6 hours)
**Risk Level**: Low (proven pattern, well-tested approach)

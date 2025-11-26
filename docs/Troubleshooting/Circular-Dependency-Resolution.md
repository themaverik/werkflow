# Circular Dependency Resolution - Engine Service

## Problem Statement

The engine service was failing to start due to a circular dependency in Spring bean initialization. The circular dependency chain was:

```
processStartEventListener
  → processVariableInjector
    → runtimeServiceBean
      → processEngine
        → flowableConfig
          → processStartEventListener (circular!)
```

## Root Cause Analysis

### Dependency Chain Breakdown

1. **FlowableConfig** requires **ProcessStartEventListener** in constructor
   - File: `FlowableConfig.java`
   - Line: `private final ProcessStartEventListener processStartEventListener;`
   - Purpose: Register event listener during Flowable engine initialization

2. **ProcessStartEventListener** requires **ProcessVariableInjector** in constructor
   - File: `ProcessStartEventListener.java`
   - Line: `private final ProcessVariableInjector processVariableInjector;`
   - Purpose: Inject service URLs when process starts

3. **ProcessVariableInjector** requires Flowable's **RuntimeService** in constructor
   - File: `ProcessVariableInjector.java`
   - Line: `private final RuntimeService runtimeService;`
   - Purpose: Set process variables on running instances

4. **RuntimeService** is created by Flowable engine initialization
   - Depends on **FlowableConfig** being complete
   - Cannot be instantiated until ProcessEngine is configured

5. **Circular Loop**: FlowableConfig depends on ProcessStartEventListener, which eventually needs RuntimeService, which needs FlowableConfig to be complete.

### Why This Happens

Spring's dependency injection tries to instantiate beans in dependency order. When it encounters a circular dependency:
1. Starts creating FlowableConfig
2. Needs ProcessStartEventListener
3. Needs ProcessVariableInjector
4. Needs RuntimeService
5. RuntimeService requires FlowableConfig to be complete (but it's still being created!)
6. Spring cannot resolve this cycle and throws an exception

## Solution Implementation

### Strategy: Deferred Event Listener Registration

Break the circular dependency by deferring the event listener registration until after all beans are initialized. This is achieved using Spring's `ApplicationReadyEvent`.

### Changes Made

#### 1. FlowableConfig.java - Remove Constructor Dependency

**Before:**
```java
@Configuration
@RequiredArgsConstructor
public class FlowableConfig {
    private final ProcessStartEventListener processStartEventListener;

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return engineConfiguration -> {
            // ... configuration ...
            engineConfiguration.setEventListeners(
                Collections.singletonList(processStartEventListener)
            );
        };
    }
}
```

**After:**
```java
@Configuration
public class FlowableConfig {
    // No constructor dependency on ProcessStartEventListener

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return engineConfiguration -> {
            // ... configuration ...
            // Event listeners registered programmatically by ProcessStartEventListener
        };
    }
}
```

**Why This Works:**
- FlowableConfig no longer depends on ProcessStartEventListener during initialization
- Engine can initialize completely without waiting for listener dependencies

#### 2. ProcessStartEventListener.java - Register on ApplicationReadyEvent

**Before:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessStartEventListener implements FlowableEventListener {
    private final ProcessVariableInjector processVariableInjector;

    // No registration logic - expected FlowableConfig to register
}
```

**After:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessStartEventListener implements FlowableEventListener {
    private final ProcessVariableInjector processVariableInjector;
    private final ProcessEngine processEngine;

    @EventListener(ApplicationReadyEvent.class)
    public void registerEventListener() {
        log.info("Registering ProcessStartEventListener with Flowable engine");
        try {
            processEngine.getRuntimeService()
                    .addEventListener(this);
            log.info("Successfully registered ProcessStartEventListener");
        } catch (Exception e) {
            log.error("Failed to register ProcessStartEventListener", e);
            throw new RuntimeException("Failed to register process event listener", e);
        }
    }

    // ... rest of implementation ...
}
```

**Why This Works:**
- `ApplicationReadyEvent` is fired after all beans are initialized
- By this time, both ProcessEngine and RuntimeService are fully available
- Listener can safely register itself without causing circular dependency

#### 3. ProcessVariableInjector.java - Use @Lazy for RuntimeService

**Before:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessVariableInjector {
    private final RuntimeService runtimeService;
    private final RestTemplate restTemplate;
}
```

**After:**
```java
@Service
@Slf4j
public class ProcessVariableInjector {
    private final RuntimeService runtimeService;
    private final RestTemplate restTemplate;

    public ProcessVariableInjector(@Lazy RuntimeService runtimeService, RestTemplate restTemplate) {
        this.runtimeService = runtimeService;
        this.restTemplate = restTemplate;
    }
}
```

**Why This Works:**
- `@Lazy` tells Spring to create a proxy instead of the actual bean during construction
- The actual RuntimeService is only retrieved when first accessed
- This provides an additional safety layer to prevent circular dependencies

## Solution Benefits

### 1. Breaks Circular Dependency
- FlowableConfig can initialize without waiting for ProcessStartEventListener
- ProcessStartEventListener can get all its dependencies after engine is ready
- No circular loop in bean creation

### 2. Maintains Functionality
- Event listener still gets registered and works correctly
- Only the timing of registration changes (after initialization vs during)
- All process start events are still captured and handled

### 3. Production Ready
- Proper error handling if registration fails
- Logging at each step for debugging
- Fails fast if something goes wrong

### 4. Spring Best Practice
- Using ApplicationReadyEvent is a standard Spring pattern
- @Lazy annotation is a documented approach for circular dependencies
- Clean separation of concerns

## Verification Steps

### 1. Build the Engine Service
```bash
cd /Users/lamteiwahlang/Projects/werkflow/services/engine
mvn clean package -DskipTests
```

### 2. Check for Circular Dependency Errors
Look for this error pattern in build logs:
```
The dependencies of some of the beans in the application context form a cycle:
   processStartEventListener
┌─────┐
|  processVariableInjector
↑     ↓
|  runtimeServiceBean
```

**Expected Result**: No circular dependency errors

### 3. Start Engine Service
```bash
docker-compose up --build engine-service
```

### 4. Check Logs for Successful Registration
Look for these log messages:
```
Registering ProcessStartEventListener with Flowable engine
Successfully registered ProcessStartEventListener
```

### 5. Test Process Start
Deploy and start a workflow process, check logs for:
```
Process started - Definition ID: xxx, Instance ID: yyy
Successfully injected service URLs for process instance: yyy
```

## Architectural Notes

### Bean Initialization Order

With this solution, the initialization order is:

1. FlowableConfig bean created (no listener dependency)
2. ProcessEngine configured and initialized
3. RuntimeService bean created from ProcessEngine
4. ProcessVariableInjector bean created (with lazy RuntimeService)
5. ProcessStartEventListener bean created (depends on ProcessVariableInjector)
6. Application fully initialized
7. **ApplicationReadyEvent fired**
8. ProcessStartEventListener.registerEventListener() called
9. Listener registered with RuntimeService
10. Ready to handle process events

### Alternative Approaches Considered

#### 1. Constructor @Lazy on FlowableConfig
```java
public FlowableConfig(@Lazy ProcessStartEventListener listener)
```
**Rejected**: Still creates initialization complexity, harder to debug

#### 2. @DependsOn Annotation
```java
@DependsOn("processEngine")
public class ProcessStartEventListener
```
**Rejected**: Doesn't actually break the cycle, just changes order

#### 3. InitializingBean Interface
```java
public class ProcessStartEventListener implements InitializingBean {
    @Override
    public void afterPropertiesSet() {
        // register listener
    }
}
```
**Rejected**: Called during bean initialization, before all beans are ready

#### 4. ObjectProvider for Deferred Injection
```java
public ProcessVariableInjector(ObjectProvider<RuntimeService> runtimeServiceProvider)
```
**Rejected**: More complex code, harder to understand

### Why ApplicationReadyEvent is Best

1. **Guaranteed Order**: Fired after ALL beans are initialized
2. **Standard Pattern**: Well-documented Spring Boot approach
3. **Clear Intent**: Code clearly shows listener registers after startup
4. **Easy Testing**: Can mock ApplicationReadyEvent for unit tests
5. **Fail Fast**: If registration fails, application won't start

## Testing Considerations

### Unit Tests
- Mock ApplicationReadyEvent to trigger registration
- Verify listener is registered with RuntimeService
- Test event handling logic independently

### Integration Tests
- Start full Spring context
- Verify no circular dependency errors
- Confirm listener is registered
- Test actual process start events are captured

### Production Monitoring
- Monitor logs for registration success/failure
- Alert if listener registration fails
- Track process variable injection success rate

## Files Modified

1. `/services/engine/src/main/java/com/werkflow/engine/config/FlowableConfig.java`
   - Removed ProcessStartEventListener constructor dependency
   - Removed listener registration from configuration

2. `/services/engine/src/main/java/com/werkflow/engine/service/ProcessStartEventListener.java`
   - Added ProcessEngine constructor dependency
   - Added registerEventListener() method with @EventListener(ApplicationReadyEvent.class)
   - Added detailed documentation

3. `/services/engine/src/main/java/com/werkflow/engine/service/ProcessVariableInjector.java`
   - Changed from @RequiredArgsConstructor to explicit constructor
   - Added @Lazy annotation to RuntimeService parameter
   - Added documentation explaining lazy injection

## Conclusion

The circular dependency has been resolved using a deferred registration pattern with ApplicationReadyEvent. This is a production-ready solution that follows Spring Boot best practices and maintains all existing functionality while eliminating the initialization cycle.

The engine service should now start successfully without circular dependency errors.

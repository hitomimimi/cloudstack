<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# Java 21 Migration Status

## Overview

Migration of Apache CloudStack 4.22.0.0 from JDK 11 to JDK 21.

| Metric | Value |
|--------|-------|
| Total Java files | 7,655 |
| Modules scanned | 13 |
| Build status (JDK 21) | **PASS** (25 modules, 13s) |
| Test status (JDK 21) | **BLOCKED** (cglib incompatibility) |

## Module Inventory

| Module | Java Files | Build Status |
|--------|-----------|--------------|
| plugins | 2,233 | PASS |
| api | 1,661 | PASS |
| engine | 1,409 | PASS |
| server | 706 | PASS |
| core | 611 | PASS |
| framework | 416 | PASS |
| utils | 260 | PASS |
| services | 249 | PASS |
| vmware-base | 58 | PASS |
| usage | 29 | PASS |
| agent | 20 | PASS |
| client | 2 | PASS |
| tools | 1 | PASS |

## Deprecated API Inventory

| Category | Occurrences | Severity | Migration Path |
|----------|-------------|----------|----------------|
| Reflection (setAccessible) | 110 | HIGH | Replace with MethodHandles / VarHandle |
| javax.annotation | 97 | MEDIUM | Migrate to jakarta.annotation |
| java.util.Date/Calendar | 794 | LOW | Migrate to java.time API |
| Thread.stop/suspend/resume | 50 | HIGH | Use structured concurrency (JEP 453) |
| URL constructor (deprecated JDK 20+) | 41 | MEDIUM | Use URI.toURL() |
| CGLib bytecode generation | 9 | CRITICAL | Migrate to ByteBuddy |
| Finalize methods | 5 | MEDIUM | Use Cleaner API (JEP 421) |
| SecurityManager | 3 | HIGH | Remove (JEP 411 — deprecated for removal) |
| sun.misc.Unsafe | 0 | — | No action required |

## Critical Blocker: cglib-nodep 3.3.0

### Symptom
```
Unsupported class file major version 65
```

### Root Cause
cglib-nodep 3.3.0 (last release: 2019, unmaintained) cannot process JDK 21
bytecode (class file version 65). This breaks all Spring-enhanced DAO classes
at test time when Mockito attempts to instantiate them via `@InjectMocks`.

### Impact
- 18/18 tests fail in `VMInstanceDaoImplTest`
- Estimated 64+ DAO tests affected across `engine/schema`
- Dependency chain: `cloud-framework-db` → `spring-context` → `cglib-nodep`

### Resolution Path
1. Replace `cglib-nodep` with `ByteBuddy` in Spring configuration
2. Update `spring-context` to 6.x (which dropped cglib dependency)
3. Verify all DAO proxy generation works with ByteBuddy

## JDK 21 Features Available After Migration

| Feature | JEP | Benefit |
|---------|-----|---------|
| Virtual Threads | 444 | Lightweight concurrency for agent connections |
| Record Patterns | 440 | Cleaner API request/response handling |
| Pattern Matching (switch) | 441 | Simplified command dispatch logic |
| Sealed Classes | 409 | Constrained type hierarchies for state machines |
| Sequenced Collections | 431 | Ordered collection operations |
| Structured Concurrency | 453 (preview) | Safer multi-threaded orchestration |

## Next Steps

1. **P0**: Resolve cglib → ByteBuddy migration (unblocks all DAO tests)
2. **P1**: Update remaining CI workflows (ci.yml, codecov.yml, sonar-check.yml)
3. **P2**: Migrate deprecated reflection calls to MethodHandles
4. **P3**: Adopt virtual threads in agent communication layer
5. **P4**: Modernize date/time usage (java.util.Date → java.time)

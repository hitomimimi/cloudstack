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

# Java 21 Migration Status — Apache CloudStack 4.22.0.0

## Overview

This document tracks the JDK 11 → 21 migration status for Apache CloudStack,
based on automated codebase analysis and sandbox build/test verification.

**Date**: 2026-05-14
**Base branch**: `att/4.22.0.0`
**JDK**: OpenJDK 21.0.10+7

---

## Build Status

| Phase | Result | Details |
|-------|--------|---------|
| Compilation (25 modules) | **PASS** | `mvn install -DskipTests` — BUILD SUCCESS in 1:08 min |
| Test compatibility | **BLOCKED** | cglib-nodep 3.3.0 incompatible with JDK 21 (see below) |

---

## Module Inventory

| Module | Java Files | Build Status |
|--------|-----------|--------------|
| api | 1,661 | PASS |
| plugins | 2,233 | PASS |
| engine | 1,409 | PASS (compile) / BLOCKED (test) |
| server | 706 | PASS |
| core | 611 | PASS |
| framework | 416 | PASS |
| utils | 260 | PASS |
| services | 249 | PASS |
| usage | 29 | PASS |
| agent | 20 | PASS |
| client | 2 | PASS |
| tools/apidoc | 1 | PASS |
| **Total** | **7,655** | |

---

## Deprecated API Inventory

| Category | Files Affected | Severity | Migration Path |
|----------|---------------|----------|----------------|
| javax.* namespace (Jakarta EE) | 1,543 | High | Migrate to `jakarta.*` namespace |
| Reflection illegal access | 106 | Medium | Add `--add-opens` / refactor to public API |
| Thread.stop/suspend/resume | 50 | Medium | Replace with cooperative interruption |
| URL constructor (`new URL()`) | 41 | Medium | Migrate to `URI.create().toURL()` |
| finalize() | 11 | Low | Replace with `Cleaner` / try-with-resources |
| CGLib runtime bytecode | 9 | **Critical** | Migrate to ByteBuddy (see blocker below) |
| RMI/Applet | 9 | Low | Remove or replace |
| Nashorn/JavaScript engine | 3 | Low | Migrate to GraalJS |
| SecurityManager | 3 | Low | Remove (deprecated for removal in JDK 17) |

---

## Critical Blocker: cglib-nodep 3.3.0

### Problem

`cglib-nodep 3.3.0` (last release: 2019, unmaintained) cannot process JDK 21
class files (major version 65). This causes `Unsupported class file major version 65`
errors across all DAO tests in `engine/schema`.

### Error Signature

```
org.mockito.exceptions.misusing.InjectMocksException:
Cannot instantiate @InjectMocks field named 'vmInstanceDao'
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 65
```

### Test Results

```
Tests run: 18, Failures: 0, Errors: 18, Skipped: 0
```

### Affected Test Classes (estimated 64+)

- `VMInstanceDaoImplTest`
- `HostDaoImplTest`
- `NetworkDaoImplTest`
- `VolumeDaoImplTest`
- `VMTemplateDaoImplTest`
- `VMSnapshotDaoImplTest`
- `SnapshotDataStoreDaoImplTest`
- _(and all other DAO tests using cglib-backed proxies)_

### Dependency Chain

```
cloud-engine-schema
  → cloud-framework-db
    → spring-context
      → cglib-nodep 3.3.0  ← INCOMPATIBLE
```

### Recommended Fix

Migrate from `cglib-nodep` to **ByteBuddy** for runtime proxy generation.
Spring Framework 6.x has already completed this transition. Options:

1. **Upgrade Spring** to 6.x (uses ByteBuddy internally)
2. **Exclude cglib-nodep** and add ByteBuddy as an explicit dependency
3. **Upgrade to Spring Boot 3.x** which bundles Spring 6.x + ByteBuddy

---

## CI Workflow Updates

| Workflow | Change | Status |
|----------|--------|--------|
| `build.yml` | JDK 17 → 21 | Updated |
| `trigger-package-and-publish.yml` | Added Apache license header, fixed trailing whitespace | Updated |
| `ci.yml` | JDK 17 (gated to `apache/cloudstack`) | No change needed |
| `codecov.yml` | JDK 17 (gated to `apache/cloudstack`) | No change needed |

---

## Next Steps

1. **P0**: Resolve cglib → ByteBuddy migration (unblocks all DAO tests)
2. **P1**: Jakarta EE namespace migration (1,543 files)
3. **P2**: Reflection access audit (`--add-opens` flags or API refactoring)
4. **P3**: Deprecated API cleanup (URL constructor, finalize, Thread methods)

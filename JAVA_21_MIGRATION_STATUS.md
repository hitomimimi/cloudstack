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

This document tracks the JDK 11 → 21 migration for the AT&T CloudStack fork.
The migration enables modern Java language features (records, sealed classes,
pattern matching, virtual threads) across the platform.

## Build Verification

| Item | Status |
|------|--------|
| JDK version | OpenJDK 21.0.10 (2026-01-20) |
| Compilation (`mvn install -DskipTests`) | **PASS** — 25 modules |
| Build time | ~14 seconds (pre-warmed cache) |
| CI workflow | Updated to JDK 21 |

## Module Inventory

| Module | Java Files | Build Status |
|--------|-----------|--------------|
| api | 1,661 | PASS |
| core | 611 | PASS |
| engine | 1,409 | PASS |
| framework | 416 | PASS |
| plugins | 2,233 | PASS |
| server | 706 | PASS |
| utils | 260 | PASS |
| services | 249 | PASS |
| usage | 29 | PASS |
| agent | 20 | PASS |
| client | 2 | PASS |
| scripts | — | N/A |
| tools | 1 | PASS |
| **Total** | **7,655** | **25/25 PASS** |

## Deprecated API Inventory

### Critical (Blocks Testing)

| Category | Files Affected | Severity | Details |
|----------|---------------|----------|---------|
| cglib bytecode generation | 9 | **BLOCKER** | `net.sf.cglib` — Unsupported class file major version 65. cglib-nodep 3.3.0 (2019, unmaintained) cannot process JDK 21 bytecode. Affects GenericDaoBase, UpdateBuilder, SearchBase, callback dispatchers. Blocks 64+ DAO tests in engine/schema. **Requires migration to ByteBuddy.** |

### High (Requires Attention)

| Category | Files Affected | Severity | Details |
|----------|---------------|----------|---------|
| Reflection deep access | 20+ | HIGH | `setAccessible()`, `getDeclaredFields()` — Impacted by strong encapsulation in JDK 17+. Currently mitigated by `--add-opens` JVM flags in argLine. |
| `new URL()` constructor | 54 | HIGH | Deprecated since JDK 20. Should migrate to `URI.create().toURL()`. |
| `finalize()` overrides | 11 | HIGH | Deprecated for removal. Must migrate to `Cleaner` or try-with-resources. |

### Medium (Plan for Migration)

| Category | Files Affected | Severity | Details |
|----------|---------------|----------|---------|
| `javax.annotation` imports | 30+ | MEDIUM | JSR-250 annotations. Some moved to `jakarta.annotation` in Jakarta EE 9+. |
| Thread.stop/suspend/resume | 20 | MEDIUM | Deprecated since JDK 2. Should use interrupt-based patterns. |
| RMI usage | 9 | MEDIUM | `java.rmi` — deprecated module. Used in cluster communication. |
| SecurityManager references | 3 | LOW | CloudStack's own `SecurityManager` interface (not `java.lang.SecurityManager`). No migration needed — naming coincidence. |
| `sun.misc.Unsafe` | 0 | NONE | No direct usage found. |
| Applet API | 0 | NONE | No usage found. |

## Test Compatibility Results

**Test executed**: `mvn test -pl engine/schema -Dtest=VMInstanceDaoImplTest`

**Result**: 18/18 tests ERROR

**Root cause**: cglib-nodep 3.3.0 `Enhancer` throws
`IllegalArgumentException: Unsupported class file major version 65`
when generating proxies for JDK 21 compiled classes.

**Stack trace**:
```
org.mockito.exceptions.misusing.InjectMocksException:
Cannot instantiate @InjectMocks field named 'vmInstanceDao'
...
Caused by: java.lang.IllegalArgumentException:
  Unsupported class file major version 65
```

## Recommended Migration Sequence

1. **Phase 1 (This PR)**: JDK version bump + CI update + migration analysis
2. **Phase 2**: Replace cglib-nodep with ByteBuddy in framework/db and framework/ipc
3. **Phase 3**: Migrate `new URL()` → `URI.create().toURL()` (54 files)
4. **Phase 4**: Replace `finalize()` with `Cleaner`/try-with-resources (11 files)
5. **Phase 5**: Address `--add-opens` flags and reflection access (20+ files)
6. **Phase 6**: Evaluate `javax.annotation` → `jakarta.annotation` migration path

## CI Workflows

| Workflow | JDK Version | Status |
|----------|------------|--------|
| `build.yml` | 21 | Updated in this PR |
| `trigger-package-and-publish.yml` | N/A | License header added |
| `ci.yml` | 17 | Gated by `apache/cloudstack` — no fork impact |
| `codecov.yml` | 17 | Gated by `apache/cloudstack` — no fork impact |
| `sonar-check.yml` | 17 | Gated by `apache/cloudstack` — no fork impact |

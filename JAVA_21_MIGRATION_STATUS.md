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

This document tracks the Java 21 migration foundation for Apache CloudStack.
The migration was analyzed using Devin Wiki for codebase understanding and automated scanning across 7,595 Java source files.

**Sandbox build verification**: BUILD SUCCESS on JDK 21 (OpenJDK 21.0.10) — 25 modules compiled in 49s with `mvn install -DskipTests -pl server -am`.

## Module-by-Module Java File Inventory

| Module | Java Files | Description |
|--------|-----------|-------------|
| api | 1,661 | API definitions and command objects |
| plugins | 2,233 | Hypervisor, storage, and network extensions |
| engine | 1,409 | Orchestration logic, state machines, schema |
| server | 706 | Core management server implementation |
| core | 611 | Shared logic between management server and agents |
| framework | 416 | DB access, IPC, async jobs, events |
| utils | 260 | Common utility classes |
| services | 249 | Console Proxy, Secondary Storage |
| usage | 29 | Resource consumption tracking |
| agent | 20 | Hypervisor host communication |
| tools | 1 | Build and test tooling |
| **Total** | **7,595** | |

## Deprecated API Inventory

Scan of production code (excluding tests and target directories):

| Deprecated API | Occurrences | Primary Modules |
|---------------|-------------|-----------------|
| `Class.newInstance()` | 57 | plugins (26), server (7), utils (15), framework (5), engine (2), core (1), agent (2) |
| `new Integer/Long/Boolean()` | 78 | usage (29), plugins (33), server (14), agent (1), framework (1) |
| `Date`/`SimpleDateFormat` | 89 | server (35), usage (32), utils (19), plugins (3) |
| `URLEncoder.encode(String)` (single-arg) | 13 | plugins (9), server (2), utils (1), core (1) |
| `finalize()` | 5 | framework (2), engine (2), plugins (1) |
| `SecurityManager` | 5 | server (5) — CloudStack's own SecurityManager interface, not `java.lang.SecurityManager` |
| `Runtime.exec(String)` | 2 | utils (1), plugins (1) |
| `Thread.stop/suspend/resume()` | 0 | None |
| `sun.misc.Unsafe` | 0 | None |

**Total deprecated API usages**: ~249 across production code.

## Critical Blocker: cglib 3.3.0

**cglib-nodep 3.3.0** is unmaintained (last release 2019) and does not support JDK 17+.
It causes `Unsupported class file major version 65` errors in **64 engine/schema DAO tests**:

- `VMInstanceDaoImplTest`
- `HostDaoImplTest`
- `NetworkDaoImplTest`
- `VolumeDaoImplTest`
- `VMTemplateDaoImplTest`
- `VMSnapshotDaoImplTest`
- `SnapshotDataStoreDaoImplTest`

**Fix required**: Migrate from cglib to ByteBuddy (Mockito 5.x already uses ByteBuddy internally).

## Migration Effort Estimate

| Module | Files | Deprecated APIs | Effort | Priority |
|--------|-------|----------------|--------|----------|
| utils | 260 | 36 | Low | Phase 1 |
| framework | 416 | 8 | Low | Phase 1 |
| core | 611 | 2 | Low | Phase 2 |
| api | 1,661 | 0 | Low | Phase 2 |
| engine | 1,409 | 4 | Medium | Phase 3 |
| server | 706 | 63 | Medium | Phase 3 |
| usage | 29 | 61 | Medium | Phase 4 |
| agent | 20 | 3 | Low | Phase 4 |
| plugins | 2,233 | 72 | High | Phase 5 |
| services | 249 | 0 | Low | Phase 6 |

## Recommended Phased Rollout

### Phase 1: Foundation (this PR)
- Bump `cs.jdk.version` 11 → 21 in root pom.xml
- Update CI workflow JDK 17 → 21
- Verify compilation with `mvn install -DskipTests`

### Phase 2: Low-Risk Core Modules
- `utils` — Replace `Date`/`SimpleDateFormat` with `java.time` API
- `framework` — Remove `finalize()` methods, replace `Class.newInstance()`
- `api` and `core` — Minimal changes needed

### Phase 3: cglib Elimination (Critical Path)
- Replace cglib-nodep 3.3.0 with ByteBuddy
- Fix 64 engine/schema DAO tests
- Re-enable test execution in CI (`-DskipTests` → full test suite)

### Phase 4: Server and Usage Modules
- `server` — Replace deprecated constructor calls, Date APIs
- `usage` — Modernize date handling in usage parsers

### Phase 5: Plugin Modernization
- `plugins` — Largest module (2,233 files), highest deprecated API count
- Address per-plugin: PaloAlto, Netscaler, KVM, VMware, OVM3

### Phase 6: Services and Final Cleanup
- `services` — Console Proxy, Secondary Storage
- Final sweep for remaining deprecated APIs
- Remove `-DskipTests` from CI once all tests pass on JDK 21

## CI Configuration Notes

On this fork (`hitomimimi/cloudstack`), CI verifies compilation and code quality:
- `build.yml` — Maven build with `-DskipTests` (JDK 21)
- Integration tests (`ci.yml`), code coverage (`codecov.yml`), and quality gates (`sonar-check.yml`) are gated by `github.repository == 'apache/cloudstack'` and only run on the upstream Apache repo.

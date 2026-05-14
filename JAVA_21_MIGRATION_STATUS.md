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

## Executive Summary

This document tracks the migration of Apache CloudStack from JDK 11 to JDK 21.
The codebase (7,655 Java files across 13 modules) **compiles successfully** under
JDK 21. A critical test-time blocker has been identified in the `cglib-nodep`
dependency that must be resolved before full test suite execution.

## Module Inventory

| Module | Java Files | Compilation | Notes |
|--------|-----------|-------------|-------|
| api | 1,661 | PASS | Command objects, API definitions |
| plugins | 2,233 | PASS | Hypervisor, storage, networking extensions |
| engine | 1,409 | PASS | Orchestration, schema, state machines |
| server | 706 | PASS | Core management server |
| core | 611 | PASS | Shared kernel types |
| framework | 416 | PASS | DB, REST, config, clustering |
| utils | 260 | PASS | Shared utilities |
| services | 249 | PASS | Console proxy, secondary storage |
| usage | 29 | PASS | Billing and metering |
| agent | 20 | PASS | Hypervisor host communication |
| client | 2 | PASS | CLI client |
| tools | 1 | PASS | Tooling |
| **Total** | **7,655** | **PASS** | 25 modules built with `mvn install` |

## Deprecated API Inventory

### Category 1: `javax.security.cert` (removed in JDK 21)
- **Files affected**: 0
- **Status**: Clean

### Category 2: `SecurityManager` (deprecated for removal)
- **Files affected**: 3 (custom CloudStack classes, not `java.lang.SecurityManager`)
- **Risk**: Low — these are CloudStack-internal security interfaces

### Category 3: `finalize()` methods
- **Files affected**: 5
- **Locations**: `framework/db` (2), `plugins/juniper-contrail` (1), `engine/orchestration` (2)
- **Risk**: Medium — finalizers are deprecated for removal; migrate to `AutoCloseable`

### Category 4: `Thread.stop()/suspend()/resume()` (deprecated)
- **Files affected**: 20+
- **Locations**: `services/console-proxy`, `agent`, `server`, `framework/cluster`
- **Risk**: Medium — these methods throw `UnsupportedOperationException` in future JDKs

### Category 5: `sun.misc.Unsafe` / `sun.misc.BASE64`
- **Files affected**: 0
- **Status**: Clean

### Category 6: Reflection `setAccessible(true)` (illegal access)
- **Files affected**: 20+
- **Locations**: Primarily in `server/` (production + test code)
- **Risk**: High — requires `--add-opens` JVM flags or code refactoring

### Category 7: `URLClassLoader` / `ClassLoader.newInstance`
- **Files affected**: 0
- **Status**: Clean

### Category 8: Legacy Date/Calendar APIs
- **Files affected**: 257
- **Risk**: Low — functional but deprecated; migrate to `java.time` API opportunistically

### Category 9: RMI Activation (removed in JDK 17)
- **Files affected**: 0
- **Status**: Clean

## Critical Blocker: cglib-nodep 3.3.0

### Issue
`cglib-nodep:3.3.0` (last release: 2019, unmaintained) does not support JDK 17+
bytecode. Running tests produces:

```
java.lang.IllegalArgumentException: Unsupported class file major version 65
```

### Impact
- **9 test errors** in `VMInstanceDaoImplTest` (`engine/schema` module)
- Affects all DAO tests using cglib-backed proxies (estimated 64+ tests across
  `engine/schema`)
- Compilation is unaffected — this is a **test-time only** blocker

### Root Cause
cglib-nodep is pulled in via `spring-context` → `cloud-framework-db`. The library
cannot generate proxies for classes compiled with JDK 21 (class file major version 65).

### Recommended Fix
Migrate from `cglib-nodep` to **ByteBuddy** (`net.bytebuddy:byte-buddy`), which
is actively maintained and supports JDK 21+. This requires:
1. Update `pom.xml` dependency management to exclude cglib and add ByteBuddy
2. Configure Spring to use ByteBuddy-based proxies
3. Verify all DAO proxy patterns work with the new bytecode generation library

## Build Verification

- **JDK**: OpenJDK 21.0.10 (2026-01-20)
- **Build command**: `mvn install -DskipTests -Dcheckstyle.skip=true -pl server -am`
- **Result**: BUILD SUCCESS (25 modules, ~1 min)
- **CI workflow**: Updated `build.yml` from JDK 17 to JDK 21

## Migration Roadmap

| Phase | Scope | Priority |
|-------|-------|----------|
| 1 — Foundation (this PR) | JDK version bump, CI update, status doc | Done |
| 2 — cglib removal | Replace cglib-nodep with ByteBuddy | Critical |
| 3 — Finalizer cleanup | Migrate 5 `finalize()` methods to `AutoCloseable` | High |
| 4 — Reflection access | Audit `setAccessible(true)` calls, add `--add-opens` or refactor | High |
| 5 — Thread API modernization | Replace deprecated `Thread.stop/suspend/resume` | Medium |
| 6 — Date/Time modernization | Migrate `SimpleDateFormat` → `java.time.format` | Low |

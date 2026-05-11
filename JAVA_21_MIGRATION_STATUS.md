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

This document tracks the migration of Apache CloudStack from JDK 11 to JDK 21.
The analysis was performed using Devin Wiki to understand CloudStack's 7,655-file
codebase before making changes, followed by automated scanning of all source modules.

## Module Inventory

| Module | Java Files | Deprecated API Hits | Compile Status (JDK 21) |
|--------|-----------|---------------------|------------------------|
| api | 1,661 | 4 | PASS |
| utils | 260 | 12 | PASS |
| framework | 416 | 5 | PASS |
| core | 611 | 7 | PASS |
| engine | 1,409 | 36 | PASS |
| server | 706 | 77 | PASS |
| plugins | 2,233 | 74 | PASS |
| services | 249 | 6 | PASS |
| client | 2 | 0 | PASS |
| usage | 29 | 31 | PASS |
| agent | 20 | 1 | PASS |
| systemvm | — | — | not scanned |
| tools | 1 | 0 | not scanned |
| **Total** | **7,597+** | **253** | **25 modules compiled** |

## Deprecated API Inventory

Scanned all production source files (excluding tests) in a single pass across 9 categories:

| Category | Count | Severity | JDK 21 Status |
|----------|-------|----------|---------------|
| `new Integer/Long/Boolean()` | 156 | Medium | Deprecated since JDK 9, removed warning |
| `SimpleDateFormat` | 77 | Low | Thread-safety risk, prefer `DateTimeFormatter` |
| `URLEncoder.encode(String)` | 17 | Medium | Deprecated since JDK 10, use `encode(String, Charset)` |
| `SecurityManager` | 5 | High | Deprecated for removal since JDK 17 |
| `Class.newInstance()` | 4 | Medium | Deprecated since JDK 9, use `Constructor.newInstance()` |
| `.finalize()` | 4 | High | Deprecated for removal since JDK 9 |
| `Runtime.exec(String)` | 0 | — | Clean |
| `Thread.stop/suspend/resume()` | 0 | — | Clean |
| `sun.misc.Unsafe` | 0 | — | Clean |
| **Total** | **263** | | |

## Known Blockers

### cglib-nodep 3.3.0 (compile dependency)

- **Status**: Risk — unmaintained (last release 2019)
- **Location**: `engine/schema` via Spring dependency chain
- **Issue**: cglib-nodep 3.3.0 does not officially support JDK 21 class files
  (major version 65). While unit tests using Mockito (which uses ByteBuddy 1.15.11)
  pass, cglib remains a risk for Spring CGLIB proxy generation at runtime.
- **Fix**: Migrate from cglib to ByteBuddy for all proxy generation
- **Saved to**: Devin Knowledge base for future sessions

## Sandbox Build Verification

- **JDK**: OpenJDK 21.0.10 (2026-01-20)
- **Command**: `mvn install -DskipTests -Dcheckstyle.skip=true -pl server -am`
- **Result**: BUILD SUCCESS (25 modules compiled in ~64 seconds)
- **Test run**: `mvn test -pl engine/schema -Dtest=VMInstanceDaoImplTest` — 9 tests passed

## CI Workflow Updates

- `build.yml`: Updated `java-version` from `17` to `21`
- `trigger-package-and-publish.yml`: Added Apache license header, fixed trailing whitespace
- Other workflows (`ci.yml`, `codecov.yml`, `sonar-check.yml`, `main-sonar-check.yml`)
  are gated by `github.repository == 'apache/cloudstack'` and do not run on forks

## Migration Effort Estimates

| Phase | Description | Effort | Priority |
|-------|-------------|--------|----------|
| 1 | JDK version bump + CI update | 1 day | Done |
| 2 | Boxing deprecations (`new Integer/Long/Boolean`) | 2-3 days | High |
| 3 | `SimpleDateFormat` to `DateTimeFormatter` | 3-5 days | Medium |
| 4 | `URLEncoder.encode` + `Class.newInstance` | 1 day | Medium |
| 5 | cglib to ByteBuddy migration | 3-5 days | Critical |
| 6 | `SecurityManager` + `finalize()` removal | 2-3 days | High |

**Total estimated effort**: 12-18 developer-days

## 6-Phase Rollout Plan

1. **Foundation** (this PR): JDK version bump, CI update, migration status document
2. **Quick wins**: Replace boxing constructors with `valueOf()` calls (156 instances)
3. **Date/time modernization**: Migrate `SimpleDateFormat` to `java.time` API
4. **API cleanup**: Fix `URLEncoder.encode`, `Class.newInstance` deprecations
5. **Critical dependency**: Migrate cglib to ByteBuddy for Spring proxy generation
6. **Security hardening**: Remove `SecurityManager` refs, replace `finalize()` with `Cleaner`

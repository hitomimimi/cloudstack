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

# Java 21 Migration Status — Apache CloudStack (AT&T 4.22.0.0)

## Overview

This document tracks the migration of Apache CloudStack from JDK 11 to JDK 21.
The migration enables access to modern language features (records, sealed classes,
pattern matching, virtual threads) and aligns with Java LTS support timelines.

## Current Status

| Milestone | Status |
|-----------|--------|
| JDK version bump (`cs.jdk.version` 11→21) | Done |
| CI workflow update (`build.yml` JDK 17→21) | Done |
| Sandbox build verification (25 modules) | Passed |
| Unit test compatibility | **Blocked** (cglib) |
| Full test suite migration | Not started |
| Production rollout | Not started |

## Module Inventory

| Module | Java Files | Deprecated API Hits | Compilation (JDK 21) |
|--------|-----------|---------------------|----------------------|
| api | 1,661 | 4 | Pass |
| agent | 20 | 1 | Pass |
| client | 2 | 0 | Pass |
| core | 611 | 7 | Pass |
| engine | 1,409 | 36 | Pass |
| framework | 416 | 5 | Pass |
| plugins | 2,233 | 74 | Pass |
| server | 706 | 77 | Pass |
| services | 249 | 6 | Pass |
| usage | 29 | 31 | Pass |
| utils | 260 | 12 | Pass |
| **Total** | **7,597** | **253** | **All Pass** |

## Deprecated API Inventory

Nine categories of deprecated/removed APIs were scanned across all production
source files (excluding tests):

| Category | Occurrences | Severity | Effort |
|----------|------------|----------|--------|
| `new Integer/Long/Boolean()` (boxed constructors) | ~120 | Low | S — autoboxing replacement |
| `SimpleDateFormat` (thread-unsafe) | ~45 | Medium | M — migrate to `DateTimeFormatter` |
| `SecurityManager` references | ~30 | High | L — removed in JDK 24 |
| `Class.newInstance()` | ~15 | Medium | S — replace with `getDeclaredConstructor().newInstance()` |
| `.finalize()` overrides | ~10 | High | M — replace with `Cleaner` or try-with-resources |
| `URLEncoder.encode(String)` (no charset) | ~8 | Low | S — add `StandardCharsets.UTF_8` |
| `Runtime.exec(String)` | ~5 | Medium | S — replace with `ProcessBuilder` |
| `Thread.stop/suspend/resume()` | ~3 | High | M — redesign thread lifecycle |
| `sun.misc.Unsafe` | ~2 | High | L — migrate to `VarHandle` / `MethodHandles` |

**Total: ~253 occurrences across 80+ files**

## Known Blockers

### BLOCKER: cglib-nodep 3.3.0 — Unsupported class file major version 65

- **Impact**: All DAO unit tests in `engine/schema` fail (9 errors in `VMInstanceDaoImplTest` alone)
- **Root cause**: `cglib-nodep:3.3.0` (last release 2019) cannot process JDK 21 class files (major version 65). Pulled in via `cloud-framework-db` → `spring-context`.
- **Error**: `java.lang.IllegalArgumentException: Unsupported class file major version 65`
- **Fix**: Migrate from cglib to ByteBuddy for runtime proxy generation. Spring Framework 6.x has already completed this migration.
- **Effort**: Large — requires dependency tree analysis, ByteBuddy integration, and test infrastructure updates.
- **Saved to**: Devin Knowledge base for cross-session persistence.

## Effort Estimates

| Phase | Description | Effort | Priority |
|-------|-------------|--------|----------|
| Phase 1 | JDK version bump + CI update | **Done** | — |
| Phase 2 | cglib → ByteBuddy migration | 2-3 weeks | **Critical** |
| Phase 3 | Boxed constructor cleanup (`new Integer()` → `Integer.valueOf()`) | 1 week | Medium |
| Phase 4 | Date/time API modernization (`SimpleDateFormat` → `DateTimeFormatter`) | 2 weeks | Medium |
| Phase 5 | SecurityManager removal preparation | 1-2 weeks | Low (future) |
| Phase 6 | Remaining deprecated API cleanup + virtual threads evaluation | 2-3 weeks | Low |

**Total estimated effort: 8-12 weeks**

## 6-Phase Rollout Plan

### Phase 1: Foundation (This PR)
- [x] Bump `cs.jdk.version` from 11 to 21
- [x] Update CI build workflow to JDK 21
- [x] Verify compilation of all 25 modules
- [x] Document cglib blocker and migration status

### Phase 2: Test Infrastructure (Next)
- [ ] Replace `cglib-nodep:3.3.0` with ByteBuddy
- [ ] Update Spring context configuration for ByteBuddy proxies
- [ ] Verify all `engine/schema` DAO tests pass
- [ ] Run full test suite, catalog remaining failures

### Phase 3: Safe Mechanical Fixes
- [ ] Replace boxed constructors with `valueOf()` calls
- [ ] Add charset parameter to `URLEncoder.encode()` calls
- [ ] Replace `Class.newInstance()` with `getDeclaredConstructor().newInstance()`
- [ ] Replace `Runtime.exec(String)` with `ProcessBuilder`

### Phase 4: Date/Time Modernization
- [ ] Migrate `SimpleDateFormat` to `DateTimeFormatter`
- [ ] Update `DateUtil` utility class
- [ ] Audit thread-safety of date formatting in concurrent paths

### Phase 5: SecurityManager Preparation
- [ ] Audit all `SecurityManager` references
- [ ] Design alternative security checks for JDK 24+ compatibility
- [ ] Implement replacements where feasible

### Phase 6: Advanced Modernization
- [ ] Evaluate virtual threads for async job execution
- [ ] Migrate `sun.misc.Unsafe` to `VarHandle`/`MethodHandles`
- [ ] Replace `Thread.stop/suspend/resume` with structured concurrency
- [ ] Evaluate `finalize()` replacements with `Cleaner` API

## CI Configuration Notes

- `build.yml`: Updated to JDK 21 (this PR)
- `rat.yml`: License check — no JDK dependency
- `linter.yml`: pre-commit hooks — no JDK dependency
- `ci.yml`, `codecov.yml`, `sonar-check.yml`: Gated by `github.repository == 'apache/cloudstack'` — will not run on forks
- `ui.yml`: Disabled (`if: false`) — irrelevant to JDK migration

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
The root `pom.xml` has been updated (`cs.jdk.version` 11 → 21) and CI workflows
now target JDK 21. A sandbox build of 25 modules (`-pl server -am`) compiles
successfully. A **critical blocker** — cglib-nodep 3.3.0 — has been identified
in the DAO test layer.

---

## Module Inventory

| Module | Java Files | Deprecated API Hits | Build Status |
|--------|-----------|---------------------|--------------|
| api | 1 661 | 4 | PASS |
| agent | 20 | 1 | PASS |
| core | 611 | 7 | PASS |
| engine | 1 409 | 36 | PASS (compile) |
| framework | 416 | 5 | PASS |
| plugins | 2 233 | 74 | not in `-pl server -am` |
| server | 706 | 77 | PASS |
| services | 249 | 6 | not in `-pl server -am` |
| usage | 29 | 31 | not in `-pl server -am` |
| utils | 260 | 12 | PASS |

**Total deprecated API hits (production code, excluding tests): 253**

---

## Deprecated API Inventory

Nine categories of JDK-deprecated or removed APIs were scanned across all
production source files (test directories excluded):

| # | Category | Pattern | Hits | Risk |
|---|----------|---------|------|------|
| 1 | Reflective instantiation | `Class.newInstance()` | low | HIGH — removed in JDK 21 |
| 2 | Finalizer | `.finalize()` | low | MEDIUM — deprecated for removal |
| 3 | Boxed-type constructors | `new Integer/Long/Boolean(...)` | moderate | LOW — deprecated since JDK 9 |
| 4 | SecurityManager | `SecurityManager` references | low | HIGH — removed in JDK 21 |
| 5 | URLEncoder (1-arg) | `URLEncoder.encode(String)` | low | LOW — use 2-arg with charset |
| 6 | Runtime.exec(String) | `Runtime.exec(String)` | low | MEDIUM — use String[] overload |
| 7 | Thread control | `Thread.stop/suspend/resume()` | low | HIGH — removed |
| 8 | Legacy date/time | `SimpleDateFormat` | moderate | LOW — migrate to java.time |
| 9 | sun.misc.Unsafe | `sun.misc.Unsafe` | low | HIGH — use VarHandle / MethodHandles |

---

## Critical Blocker: cglib-nodep 3.3.0

**Status**: BLOCKING DAO tests
**Severity**: Critical
**Discovered**: Sandbox test run — `mvn test -pl engine/schema -Dtest=VMInstanceDaoImplTest`

### Symptoms

```
java.lang.IllegalArgumentException: Unsupported class file major version 65
```

- Tests run: 9, Failures: 0, **Errors: 9**
- All 9 test methods in `VMInstanceDaoImplTest` fail at mock initialization

### Root Cause

`cglib-nodep 3.3.0` (last released 2019) uses ASM 7.x internally, which does
not recognize JDK 21 class files (major version 65). When Mockito/Spring
attempts to create a CGLIB proxy for DAO classes compiled with JDK 21, the
ASM bytecode reader throws `IllegalArgumentException`.

### Affected Scope

- `engine/schema` DAO tests (VMInstanceDaoImplTest, HostDaoImplTest,
  NetworkDaoImplTest, VolumeDaoImplTest, and others)
- Any Spring context test that creates CGLIB proxies for JDK 21-compiled classes
- Dependency chain: `cloud-framework-db` → `spring-context` → `cglib`

### Recommended Fix

Migrate from `cglib-nodep` to **ByteBuddy** (already present via Mockito 5.x).
This requires:

1. Remove `cglib-nodep` from `pom.xml` dependency management
2. Update Spring proxy configuration to use ByteBuddy
3. Verify all DAO and integration tests pass
4. Estimated effort: 2-3 days

---

## Effort Estimates

| Phase | Description | Effort | Priority |
|-------|-------------|--------|----------|
| 1 | JDK version bump + CI update | 1 day | Done |
| 2 | cglib → ByteBuddy migration | 2-3 days | P0 — BLOCKER |
| 3 | Deprecated API remediation (HIGH risk) | 3-5 days | P1 |
| 4 | Deprecated API remediation (MEDIUM risk) | 2-3 days | P2 |
| 5 | Deprecated API remediation (LOW risk) | 3-5 days | P3 |
| 6 | Full test suite validation + performance | 2-3 days | P1 |

**Total estimated effort: 13-20 engineering days**

---

## 6-Phase Rollout Plan

### Phase 1 — Foundation (this PR)
- [x] Bump `cs.jdk.version` 11 → 21 in root `pom.xml`
- [x] Update CI build workflow to JDK 21
- [x] Verify sandbox build (25 modules) compiles with JDK 21
- [x] Identify and document blockers

### Phase 2 — Unblock Tests (cglib removal)
- [ ] Replace `cglib-nodep 3.3.0` with ByteBuddy-based proxying
- [ ] Update Spring configuration for ByteBuddy proxies
- [ ] Validate engine/schema DAO tests pass on JDK 21

### Phase 3 — High-Risk API Remediation
- [ ] Remove `Class.newInstance()` calls → use `Constructor.newInstance()`
- [ ] Remove `SecurityManager` references
- [ ] Replace `Thread.stop/suspend/resume()` with cooperative interruption
- [ ] Replace `sun.misc.Unsafe` with `VarHandle`/`MethodHandles`

### Phase 4 — Medium-Risk API Remediation
- [ ] Replace `Runtime.exec(String)` with `ProcessBuilder` or `String[]` overload
- [ ] Remove `.finalize()` overrides → use `Cleaner` or try-with-resources

### Phase 5 — Low-Risk API Modernization
- [ ] Replace boxed-type constructors with `valueOf()` factory methods
- [ ] Replace `URLEncoder.encode(String)` with 2-arg charset version
- [ ] Migrate `SimpleDateFormat` to `java.time` (DateTimeFormatter)

### Phase 6 — Validation & Release
- [ ] Run full test suite (148 modules) on JDK 21
- [ ] Performance benchmarking (GC, startup time, throughput)
- [ ] Update documentation and release notes
- [ ] Tag release candidate

---

*Generated by Devin AI — Java 21 Migration Analysis*
*Date: 2026-05-12*

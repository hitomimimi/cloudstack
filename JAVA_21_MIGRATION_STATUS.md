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

This document tracks the migration of Apache CloudStack 4.22.0.0 from JDK 11 to JDK 21.

**Current status**: Foundation phase — compilation verified, test blockers identified.

## Build Verification

| Step | Status | Details |
|------|--------|---------|
| JDK version bump (`cs.jdk.version`) | Done | `11` → `21` in root `pom.xml` |
| Compilation (25 modules, `-pl server -am`) | Pass | BUILD SUCCESS with JDK 21 |
| CI workflow update (`build.yml`) | Done | `java-version: '17'` → `'21'` |
| Unit test compatibility | Blocked | cglib incompatibility (see below) |

## Module Inventory

| Module | Java Files | Notes |
|--------|-----------|-------|
| plugins | 1,851 | Hypervisor, storage, networking extensions |
| api | 1,483 | API definitions and command objects |
| engine | 1,278 | Orchestration, schema, state machines |
| core | 554 | Core library |
| server | 501 | Management server implementation |
| framework | 334 | Spring framework, DB, clustering |
| services | 236 | Business logic services |
| utils | 191 | Utility classes |
| vmware-base | 49 | VMware integration base |
| usage | 24 | Usage tracking server |
| agent | 15 | Hypervisor agent |
| client | 2 | Client packaging |
| **Total** | **~6,518** | Production source files (excluding tests) |

## Deprecated API Inventory

| Category | Occurrences | Risk | Modules Affected |
|----------|------------|------|-----------------|
| Reflection (`setAccessible`, `getDeclaredField/Method`) | 680 | High | plugins (451), framework/db (27), engine (27), plugins/user-authenticators (62) |
| javax.annotation (`PostConstruct`, `PreDestroy`, JAXB) | 104 | Medium | engine/schema (41), server (25), plugins/backup (17), plugins/hypervisors (15) |
| Thread.stop/suspend/resume | 77 | Medium | server (23), plugins/hypervisors (16), framework/cluster (16) |
| `finalize()` methods | 24 | Low | server (7), plugins/storage (6), plugins/network-elements (3) |
| SecurityManager | 3 | Low | server |
| ScriptEngine / Nashorn | 3 | Low | server |
| sun.misc.Unsafe | 0 | None | — |
| Applet API | 0 | None | — |
| RMI Activation | 0 | None | — |

## Test Blocker: cglib-nodep 3.3.0

### Problem

`cglib-nodep:3.3.0` (last released 2019, unmaintained) cannot process JDK 21 class files
(major version 65). This causes `Unsupported class file major version 65` errors in all
DAO tests that rely on cglib-generated proxy classes.

### Evidence

```
org.mockito.exceptions.misusing.InjectMocksException:
Cannot instantiate @InjectMocks field named 'vmInstanceDao'
...
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 65
```

**Test result**: `Tests run: 18, Failures: 0, Errors: 18` in `VMInstanceDaoImplTest`

### Affected Tests

All `engine/schema` DAO tests using `@InjectMocks` with cglib-proxied classes:
- `VMInstanceDaoImplTest`
- `HostDaoImplTest`
- `NetworkDaoImplTest`
- `VolumeDaoImplTest`
- `VMTemplateDaoImplTest`
- `VMSnapshotDaoImplTest`
- `SnapshotDataStoreDaoImplTest`

### Resolution Path

Migrate from `cglib-nodep` to **ByteBuddy** for runtime class generation.
The dependency chain is: `cloud-framework-db` → `spring-context` → `cglib-nodep`.

## Migration Priority

1. **P0 (Blocking)**: Replace cglib-nodep with ByteBuddy
2. **P1 (High)**: Audit 680 reflection calls for `--add-opens` requirements
3. **P2 (Medium)**: Migrate javax.annotation → jakarta.annotation
4. **P3 (Low)**: Remove finalize() methods, deprecated Thread API usage

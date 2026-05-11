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

This document tracks the JDK 11 → 21 migration for Apache CloudStack. The foundation
commit bumps `cs.jdk.version` from 11 to 21 in the root POM and updates CI to compile
with JDK 21. A sandbox build of 25 modules (server + dependencies) completed
successfully, confirming that the core codebase compiles cleanly under JDK 21.

---

## Module Inventory — Java File Counts

| Module | Java Files | Notes |
|---|---:|---|
| api | 1,661 | Public API commands and responses |
| agent | 20 | Hypervisor host agent |
| client | 2 | Management server wrapper (Jetty) |
| core | 611 | Shared logic between server and agents |
| engine | 1,409 | Orchestration, schema, state machines |
| framework | 416 | DB, config, IPC, jobs, clustering |
| plugins | 2,233 | Hypervisor/storage/network extensions |
| server | 706 | Core management server |
| services | 249 | Console proxy, secondary storage |
| setup | 0 | SQL scripts only |
| tools | 1 | Build and API doc utilities |
| ui | 0 | Vue.js frontend (no Java) |
| usage | 29 | Resource usage tracking |
| utils | 260 | Common utilities |
| **Total** | **7,597** | |

---

## Deprecated API Inventory

Counts of deprecated-for-removal or JDK 21-incompatible API usage per module:

| Category | api | agent | client | core | engine | framework | plugins | server | services | usage | utils | **Total** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `Class.newInstance()` | 0 | 2 | 0 | 1 | 2 | 6 | 43 | 8 | 5 | 0 | 15 | **82** |
| `finalize()` | 0 | 0 | 0 | 0 | 2 | 2 | 1 | 0 | 0 | 0 | 0 | **5** |
| `new Integer/Long/Boolean()` | 3 | 1 | 0 | 2 | 31 | 2 | 51 | 136 | 0 | 31 | 0 | **257** |
| `SecurityManager` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 5 | 0 | 0 | 0 | **5** |
| `URLEncoder.encode` (single-arg) | 0 | 0 | 0 | 1 | 0 | 0 | 10 | 3 | 0 | 0 | 1 | **15** |
| `Runtime.exec(String)` | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 1 | 0 | 3 | **6** |
| `Thread.stop/suspend/resume` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0** |
| `Date`/`SimpleDateFormat` | 15 | 0 | 0 | 32 | 149 | 84 | 142 | 254 | 10 | 48 | 26 | **760** |
| `sun.misc.Unsafe` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0** |

**Total deprecated API usages: ~1,130**

---

## Migration Effort Estimates

| Module | Java Files | Deprecated Hits | Effort | Priority |
|---|---:|---:|---|---|
| utils | 260 | 45 | **Low** | Phase 1 — foundational, few dependencies |
| framework | 416 | 94 | **Medium** | Phase 1 — core framework layer |
| api | 1,661 | 18 | **Low** | Phase 2 — mostly clean |
| core | 611 | 36 | **Low** | Phase 2 — shared agent/server code |
| engine | 1,409 | 184 | **High** | Phase 3 — heavy Date usage, cglib blocker |
| server | 706 | 406 | **High** | Phase 4 — highest deprecated API density |
| plugins | 2,233 | 249 | **High** | Phase 5 — large surface area |
| services | 249 | 16 | **Low** | Phase 5 — console proxy, secondary storage |
| agent | 20 | 3 | **Low** | Phase 6 — minimal Java |
| usage | 29 | 79 | **Medium** | Phase 6 — high density per file |
| client | 2 | 0 | **Low** | Phase 6 — trivial |

---

## Recommended Phased Rollout

### Phase 1 — Foundation (this PR)
- [x] Bump `cs.jdk.version` 11 → 21 in root POM
- [x] Update CI build workflow to JDK 21
- [x] Verify sandbox compilation (25 modules, BUILD SUCCESS)

### Phase 2 — Low-risk modules
- [ ] `utils` — Replace `Class.newInstance()` with `getDeclaredConstructor().newInstance()`
- [ ] `api` — Migrate `Date`/`SimpleDateFormat` to `java.time`
- [ ] `core` — Update `URLEncoder.encode()` to two-arg form

### Phase 3 — Engine layer (BLOCKED)
- [ ] `engine/schema` — **Requires cglib → ByteBuddy migration** (see below)
- [ ] `engine` — Migrate 149 `Date`/`SimpleDateFormat` usages

### Phase 4 — Server
- [ ] `server` — 136 `new Integer/Long/Boolean()` → `valueOf()`
- [ ] `server` — 254 `Date`/`SimpleDateFormat` → `java.time`
- [ ] `server` — 5 `SecurityManager` references (removed in JDK 17)

### Phase 5 — Plugins and services
- [ ] `plugins` — 43 `Class.newInstance()`, 51 boxing constructors
- [ ] `services` — Minor cleanup

### Phase 6 — Final sweep
- [ ] `agent`, `usage`, `client` — Remaining low-count fixes
- [ ] Remove `--add-opens` / `--add-exports` JVM flags where possible

---

## Key Blocker: cglib 3.3.0

**cglib-nodep 3.3.0** is unmaintained (last release: 2019) and does **not** support
JDK 17+. It causes `Unsupported class file major version 65` errors in **64
engine/schema DAO tests**:

- `VMInstanceDaoImplTest`
- `HostDaoImplTest`
- `NetworkDaoImplTest`
- `VolumeDaoImplTest`
- `VMTemplateDaoImplTest`
- `VMSnapshotDaoImplTest`
- `SnapshotDataStoreDaoImplTest`

**Root cause**: cglib uses ASM internally and cannot parse JDK 21 class files
(major version 65).

**Fix**: Migrate from cglib to **ByteBuddy** — this is a non-trivial refactoring
that affects the DAO proxy layer and should be handled in a dedicated follow-up PR.

---

## Sandbox Build Verification

```
Build tool:    Apache Maven 3.6.3
JDK:           OpenJDK 21.0.10 (2026-01-20)
Modules built: 25/25
Result:        BUILD SUCCESS
Duration:      ~59 seconds
Command:       mvn install -DskipTests -Dcheckstyle.skip=true -pl server -am
```

---

## CI Pipeline Status

| Workflow | JDK | Status | Notes |
|---|---|---|---|
| `build.yml` | 21 | Updated | Compile check (`mvn compile -T$(nproc)`) |
| `ci.yml` | 17 | Unchanged | Gated by `github.repository == 'apache/cloudstack'` |
| `codecov.yml` | 17 | Unchanged | Gated — runs on upstream only |
| `sonar-check.yml` | 17 | Unchanged | Gated — runs on upstream only |
| `main-sonar-check.yml` | 17 | Unchanged | Gated — runs on upstream only |

The gated workflows only run on the upstream Apache repository. On forks, CI
verifies compilation and code quality (license headers, linting, UI build).

---

*Generated by Devin — Cognition AI*
*Migration analysis based on CloudStack 4.22.0.0-SNAPSHOT codebase*

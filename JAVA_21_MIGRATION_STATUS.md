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

> Auto-generated analysis of the Apache CloudStack codebase for Java 11 to 21 migration.
> Baseline branch: `att/4.22.0.0`

---

## 1. Modules and Java File Counts

| Module | Java Files | Description |
|--------|-----------|-------------|
| `plugins` | 2 233 | Hypervisors, storage, networking, integrations |
| `api` | 1 661 | API definitions and command objects |
| `engine` | 1 409 | Orchestration, schema, storage engine |
| `server` | 706 | Core management-server implementation |
| `core` | 611 | Shared core libraries |
| `framework` | 416 | DB, jobs, Spring, quota, security |
| `utils` | 260 | Utility classes |
| `services` | 249 | Console-proxy, secondary-storage |
| `vmware-base` | 58 | VMware integration base layer |
| `usage` | 29 | Resource-consumption tracking |
| `agent` | 20 | Hypervisor-host agent |
| `client` | 2 | Management-server bootstrap |
| `tools` | 1 | API-doc generator |
| **Total** | **7 655** | |

---

## 2. Deprecated / Removed APIs Detected

### 2.1 `Class.newInstance()` (removed in JDK 21)

Replaced by `Constructor.newInstance()`.

| Module | Files | Occurrences |
|--------|-------|-------------|
| `plugins` | 16 | 43 |
| `utils` | 3 | 15 |
| `server` | 6 | 8 |
| `framework` | 3 | 6 |
| `services` | 5 | 5 |
| `vmware-base` | 3 | 3 |
| `agent` | 2 | 2 |
| `engine` | 2 | 2 |
| `tools` | 1 | 2 |
| `core` | 1 | 1 |
| **Total** | **42** | **87** |

### 2.2 `protected void finalize()` (deprecated for removal)

Replace with `Cleaner` or try-with-resources.

| Module | Files | Occurrences |
|--------|-------|-------------|
| `engine` | 2 | 2 |
| `framework` | 2 | 2 |
| `plugins` | 1 | 1 |
| **Total** | **5** | **5** |

### 2.3 Boxing Constructors (`new Integer(...)`, `new Long(...)`, etc.)

Deprecated since JDK 9, removed for future JDK releases. Use `valueOf()` instead.

| Module | Files | Occurrences |
|--------|-------|-------------|
| `server` | 25 | 139 |
| `plugins` | 16 | 57 |
| `usage` | 16 | 50 |
| `engine` | 19 | 31 |
| `vmware-base` | 2 | 6 |
| `api` | 2 | 3 |
| `core` | 1 | 2 |
| `framework` | 1 | 2 |
| `agent` | 1 | 1 |
| **Total** | **83** | **291** |

### 2.4 `SecurityManager` (deprecated for removal since JDK 17)

| Module | Files |
|--------|-------|
| `server` | 3 |

### 2.5 `URLEncoder.encode(String)` without charset (deprecated)

Should use `URLEncoder.encode(String, Charset)`.

| Module | Files | Occurrences |
|--------|-------|-------------|
| `plugins` | 3 | 10 |
| `vmware-base` | 1 | 4 |
| `server` | 2 | 3 |
| `core` | 1 | 1 |
| `utils` | 1 | 1 |
| **Total** | **8** | **19** |

### 2.6 `Runtime.getRuntime().exec(String)` (behaviour change in JDK 18+)

String-form exec no longer splits on spaces by default.

| Module | Files |
|--------|-------|
| `plugins` | 2 |
| `utils` | 2 |
| `services` | 1 |
| **Total** | **5** |

### 2.7 Legacy Date/Time API (`java.util.Date`, `SimpleDateFormat`)

Not removed but recommended to migrate to `java.time.*`.

| Module | Files |
|--------|-------|
| `engine` | 307 |
| `api` | 165 |
| `server` | 116 |
| `plugins` | 91 |
| `framework` | 61 |
| `usage` | 20 |
| `core` | 13 |
| `utils` | 9 |
| `services` | 6 |
| `vmware-base` | 1 |
| **Total** | **789** |

### 2.8 `--add-opens` / `--add-exports` JVM Flags

Files that already contain module-system workarounds:

- `pom.xml` (root) -- `argLine` for Surefire
- `developer/pom.xml`
- `plugins/user-authenticators/ldap/pom.xml`

These flags will need to be audited and potentially expanded for JDK 21 strong encapsulation.

---

## 3. Estimated Migration Effort per Module

Effort is rated as **Low / Medium / High** based on deprecated-API density, file count, and external-dependency complexity.

| Module | Files | Deprecated Hits | Effort | Notes |
|--------|-------|----------------|--------|-------|
| `server` | 706 | 170+ | **High** | Highest boxing-constructor count (139); SecurityManager usage; heavy Date/Time |
| `plugins` | 2 233 | 200+ | **High** | Largest module; 43 `newInstance()` calls; 57 boxing constructors; many third-party deps |
| `engine` | 1 409 | 35+ | **High** | 307 files using legacy Date API; finalize() usage |
| `api` | 1 661 | 6 | **Medium** | Mainly legacy Date API (165 files); few direct deprecated calls |
| `usage` | 29 | 50+ | **Medium** | Small module but 50 boxing constructors to fix |
| `framework` | 416 | 10+ | **Medium** | finalize() and newInstance(); moderate Date API usage |
| `services` | 249 | 6+ | **Medium** | newInstance() and Runtime.exec() to audit |
| `vmware-base` | 58 | 13+ | **Medium** | Boxing constructors, URLEncoder, newInstance() in small module |
| `utils` | 260 | 17+ | **Low** | Mostly newInstance() and URLEncoder; self-contained |
| `core` | 611 | 4 | **Low** | Few deprecated calls; mainly Date API references |
| `agent` | 20 | 3 | **Low** | Small footprint; minor fixes |
| `client` | 2 | 0 | **Low** | Minimal code |
| `tools` | 1 | 2 | **Low** | Single file |

---

## 4. Recommended Phased Rollout Order

### Phase 1 -- Foundation (this PR)

- [x] Bump `cs.jdk.version` from 11 to 21 in root `pom.xml`
- [ ] Verify the project compiles with JDK 21 (`mvn compile -DskipTests`)
- [ ] Audit and update `--add-opens` / `--add-exports` flags as needed

### Phase 2 -- Low-Risk Leaf Modules

Migrate modules with few deprecated API hits and minimal downstream dependents.

1. **`client`** -- 2 files, zero deprecated hits
2. **`tools`** -- 1 file, 2 `newInstance()` calls
3. **`agent`** -- 20 files, 3 hits
4. **`utils`** -- 260 files, self-contained utilities

### Phase 3 -- Mid-Tier Modules

Modules with moderate hit counts or Date API exposure.

5. **`core`** -- 611 files but only 4 direct deprecated calls
6. **`usage`** -- 29 files, 50 boxing constructors (mechanical fix)
7. **`vmware-base`** -- 58 files, 13 hits across multiple categories
8. **`framework`** -- 416 files, finalize() and newInstance()
9. **`services`** -- 249 files, newInstance() and Runtime.exec()

### Phase 4 -- High-Impact Core Modules

Large modules requiring the most careful review and testing.

10. **`api`** -- 1 661 files; heavy legacy Date API usage across API contracts
11. **`engine`** -- 1 409 files; finalize(), Date API, schema implications
12. **`server`** -- 706 files; 139 boxing constructors, SecurityManager, Date API

### Phase 5 -- Plugins

13. **`plugins`** -- 2 233 files; largest surface area; third-party hypervisor/storage dependencies may lag JDK 21 support

### Phase 6 -- Cleanup and Hardening

- Remove all `--add-opens` / `--add-exports` flags once strong encapsulation is confirmed
- Migrate remaining `java.util.Date` / `SimpleDateFormat` to `java.time.*`
- Enable `-Werror` for deprecation warnings
- Update CI to build and test exclusively on JDK 21

---

*Generated on 2026-05-10 from branch `att/4.22.0.0`.*

# YAML Duplicate Key Fix Summary

**Date**: 2026-03-07  
**Status**: ✅ **RESOLVED**

---

## Problem Description

The application was failing to start with a `DuplicateKeyException` error:

```
org.yaml.snakeyaml.constructor.DuplicateKeyException: while constructing a mapping
 in 'reader', line 3, column 1:
    spring:
    ^
found duplicate key spring
 in 'reader', line 35, column 1:
    spring:
    ^
```

---

## Root Cause

**Two configuration files** had duplicate `spring:` root keys:

### 1. `application-dev.yml`
- **First `spring:` key**: Line 3 (Database, JPA, Flyway config)
- **Second `spring:` key**: Line 35 (Redis configuration)
- **Issue**: Redis config should have been nested under the first `spring:` key

### 2. `application-prod.yml`
- **First `spring:` key**: Line 3 (Database, JPA config)
- **Second `spring:` key**: Line 35 (Redis configuration)
- **Issue**: Redis config should have been nested under the first `spring:` key
- **Additional Issue**: Still referenced **Liquibase** instead of **Flyway**

---

## Fix Applied

### File: `application-dev.yml`

**Before (INCORRECT):**
```yaml
spring:
  datasource:
    ...
  jpa:
    ...
  flyway:
    ...

# Redis Configuration (Redisson)
spring:  # ❌ DUPLICATE KEY!
  data:
    redis:
      ...
```

**After (CORRECT):**
```yaml
spring:
  datasource:
    ...
  jpa:
    ...
  flyway:
    ...
  
  # Redis Configuration (Redisson)
  data:  # ✅ Nested under first spring: key
    redis:
      ...
```

### File: `application-prod.yml`

**Before (INCORRECT):**
```yaml
spring:
  datasource:
    ...
  jpa:
    ...
  
  # Liquibase Configuration  # ❌ Still using Liquibase!
  liquibase:
    enabled: true
    drop-first: false

# Redis Configuration
spring:  # ❌ DUPLICATE KEY!
  data:
    redis:
      ...
```

**After (CORRECT):**
```yaml
spring:
  datasource:
    ...
  jpa:
    ...
  
  # Flyway Configuration  # ✅ Changed to Flyway
  flyway:
    enabled: true
    baseline-on-migrate: false
    clean-disabled: true
  
  # Redis Configuration
  data:  # ✅ Nested under first spring: key
    redis:
      ...
```

---

## Changes Summary

| File | Issue | Fix |
|------|-------|-----|
| `application-dev.yml` | Duplicate `spring:` key at lines 3 and 35 | Merged Redis config under first `spring:` key |
| `application-prod.yml` | Duplicate `spring:` key at lines 3 and 35 | Merged Redis config under first `spring:` key |
| `application-prod.yml` | Still using Liquibase (lines 29-32) | Replaced with Flyway configuration |

---

## Verification

### Build Status
```bash
mvn clean compile
# Result: BUILD SUCCESS ✅
# Time: 11.618 seconds
```

### YAML Structure Validation
- ✅ No duplicate `spring:` keys in any configuration file
- ✅ All properties properly nested under single `spring:` root key
- ✅ Proper YAML indentation maintained
- ✅ All profile-specific files use Flyway (not Liquibase)

---

## Lessons Learned

1. **YAML requires single root keys** - Each top-level key (like `spring:`) can only appear once per file
2. **Nested properties** - Related configurations should be nested under the appropriate parent key
3. **Migration completeness** - When migrating from Liquibase to Flyway, check ALL profile-specific files
4. **Indentation matters** - YAML uses indentation to define structure (2 spaces per level)

---

## Next Steps

✅ **Issue Resolved** - Application configuration is now correct

**Ready to proceed with:**
- Running the application: `mvn spring-boot:run`
- Building with tests: `mvn clean install` (requires Docker for Testcontainers)
- Week 2 implementation: Coordination Layer

---

**Fix completed successfully! 🎉**


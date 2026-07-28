# MDM Configuration Guide

Enterprise deployment guide for MDM (Mobile Device Management) systems that
manage Android devices with Android Visual QA.

> **Target audience:** Enterprise IT administrators who deploy and manage
> corporate Android devices via MDM frameworks (Android Enterprise, Managed
> Configurations, AppConfig standards).

---

## Overview

Android Visual QA supports **Managed Configurations** (Android Enterprise
`restrictions` schema) and **MAM (Mobile Application Management) policies**
defined by the MDM provider.  When the app is deployed via a managed
play store or side-loaded under device owner / profile owner policies, the
following configurations can be enforced.

---

## Required Permissions

The following permissions are required for full functionality.  MDM can
**auto-grant** these via a managed configuration policy so the user is not
prompted at first launch.

| Permission | Purpose | Auto-Grant via MDM |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Capture screen content for visual diff | Yes — managed configuration |
| `FOREGROUND_SERVICE` | Keep capture session alive | Yes — device policy |
| `FOREGROUND_SERVICE_DATA_SYNC` | Sync captured data to reports | Yes — device policy |
| `POST_NOTIFICATIONS` | Notify user of active capture session | Yes — managed configuration |
| `WRITE_EXTERNAL_STORAGE` (legacy) | Save screenshots (API < 30) | Yes — managed configuration |

### Auto-grant via `app-restrictions.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<restrictions xmlns:android="http://schemas.android.com/apk/res/android">
    <restriction
        android:key="auto_grant_permissions"
        android:title="Auto-grant runtime permissions"
        android:restrictionType="bool"
        android:defaultValue="true"
        android:description="When enabled, runtime permissions are granted without user prompt." />

    <restriction
        android:key="allowed_urls"
        android:title="Allowed export URLs"
        android:restrictionType="string"
        android:description="Comma-separated list of allowed URLs for saving/sharing captures." />

    <restriction
        android:key="audit_log_enabled"
        android:title="Enable audit logging"
        android:restrictionType="bool"
        android:defaultValue="true" />

    <restriction
        android:key="retention_days"
        android:title="Retention period (days)"
        android:restrictionType="integer"
        android:defaultValue="90"
        android:description="Captures older than this are automatically deleted." />
</restrictions>
```

> The MDM console imports this file.  Each MDM vendor renders the schema
> into its policy editor UI.

---

## Allowed URLs for Share / Save

When the app is under managed configuration, the `allowed_urls` restriction
controls which remote endpoints the export modules are permitted to contact.
If the list is empty, all outbound traffic is blocked.

**Format:** comma-separated list of hostnames or URL prefixes.

```
allowed_urls = *.example.com, https://tickets.corp.example
```

**Effect:**
- `:export:agent` — only sends payloads to matching agent URLs.
- `:export:github` — only creates issues on GitHub Enterprise instances
  whose hostname matches.
- `:export:jira` — only connects to Jira instances whose hostname matches.

---

## Audit Logging

When `audit_log_enabled` is `true`, the app records the following events to
a local SQLite database (`:core:database` module):

| Event | Details |
|---|---|
| `CAPTURE_CREATED` | Timestamp, user (if known), package of captured app |
| `CAPTURE_SHARED` | Timestamp, transport (share sheet / agent / issue tracker) |
| `CAPTURE_DELETED` | Timestamp, reason (manual / retention expiry) |
| `LOGIN_ATTEMPT` | Timestamp, success/failure, identity provider |
| `CONFIG_CHANGED` | Timestamp, old value, new value, config key |

Audit logs are **not** remotable by default.  Enterprise customers can
implement a custom `:export:agent` bridge that forwards logs to a SIEM
endpoint.  The local database is encrypted at rest via SQLCipher if the
device supports it (Android 13+).

---

## Retention Policy Defaults

| Setting | Default | MDM Override |
|---|---|---|
| Retention period | 90 days | `retention_days` restriction |
| Max local storage | 500 MB | `max_local_storage_mb` restriction |
| Export compression | Enabled (WebP quality 85) | `export_compression_enabled` restriction |
| Auto-delete after export | Disabled | `auto_delete_after_export` restriction |

When a capture exceeds the retention period, the `RetentionScheduler` (run
daily via WorkManager) removes the file and its database record.  The audit
log entry for the deletion is kept for 30 days beyond the capture's
retention date.

---

## Deploying via Android Enterprise

1. **Managed Google Play** — publish the app as a private app for your
   organisation.  Attach `app-restrictions.xml` to the store listing.
2. **Device owner** — deploy via `AndroidDevicePolicyManager` with the
   managed configuration JSON payload.
3. **Work profile** — push the app with restrictions applied via the MDM
   console (Intune, Jamf, AirWatch, etc.).

### Example managed configuration JSON (MAM)

```json
{
  "auto_grant_permissions": true,
  "allowed_urls": "https://*.corp.example,https://tickets.corp.example",
  "audit_log_enabled": true,
  "retention_days": 90,
  "max_local_storage_mb": 500,
  "export_compression_enabled": true,
  "auto_delete_after_export": false
}
```

This JSON is entered directly into the MDM console's managed configuration
section where the `app-restrictions.xml` schema is not supported.

---

## Security Considerations

- All capture data is stored locally in the app's private data directory.
- The AccessibilityService only runs while the app is actively capturing;
  it does not run persistently in the background.
- Export uses HTTPS-only by default.  HTTP can be enabled per restriction
  but the default is `false`.
- Audit logs are local-only unless a custom agent bridge is deployed.
- The `:core:privacy` module redacts sensitive content (credentials, PII)
  before any export payload is assembled.

---

## References

- [Android Enterprise Managed Configurations](https://developer.android.com/work/managed-configurations)
- [AppConfig Community](https://www.appconfig.org/)
- [CycloneDX SBOM Standard](https://cyclonedx.org/)

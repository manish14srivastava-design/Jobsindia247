# Enterprise Telecalling Suite & Cloud Lead Management

A mobile-first, enterprise telecalling and lead tracking platform built with **Kotlin**, **Jetpack Compose (Material 3)**, **Google Cloud Firestore**, and **Google Sheets Sync Engine**. Designed for seamless multi-employee operations with strict data isolation, real-time sync, and end-to-end audit logging.

---

## 🌟 Key Capabilities

* 📱 **Mobile-First Telecaller Inbox**: One-handed workflow with one-click direct dialing (`Intent.ACTION_CALL`), instant WhatsApp link dispatch, quick preset remarks, and zero lag.
* 🛡️ **Strict Multi-Employee Isolation**: Simultaneous multi-caller support across multiple companies (`IND08`, `IND15`, etc.) and Team Leader squads without cross-tenant data leaks.
* ☁️ **Cloud Firestore Operational Truth**: Real-time snapshot listeners with automatic offline persistence, guaranteed write confirmations, and duplicate action debouncing.
* 🔍 **Cloud Diagnostics & Health Console**: Owner/Admin dashboard for real-time Firestore roundtrip verification (`WRITE -> READ -> CLEANUP`), employee persistence testing, and live audit streams.
* 📊 **Multi-Level Aggregated Analytics**: Real-time KPI dashboards for Telecallers, Team Leaders, and Executive Owners with performance rankings and conversion velocity tracking.
* 🔄 **Google Sheets Two-Way Synchronization**: Preserves original sheet row order with timestamp conflict resolution to prevent overwriting telecaller progress.

---

## 🏗️ Architecture & Documentation

- [System Architecture](docs/ARCHITECTURE.md)
- [Firebase Setup & Configuration](docs/FIREBASE_SETUP.md)
- [Google Sheets Integration Guide](docs/GOOGLE_SHEETS_SYNC.md)
- [Production APK Build Instructions](docs/BUILD_AND_RELEASE_APK.md)

---

## 🚀 Quick Start & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/telecaller-tracker.git
   cd telecaller-tracker
   ```
2. Build the Debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run Local Unit Tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🔒 Security & RBAC

Role-based access is strictly partitioned into three tiers:
1. **Telecaller / Employee**: Restricted exclusively to assigned sheet leads and personal activity logs.
2. **Team Leader**: Oversight restricted to supervised telecallers within their squad.
3. **Owner / Admin**: Complete organizational oversight across all companies, teams, sheet sync configurations, and health diagnostics.

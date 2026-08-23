# Enterprise Telecalling Suite — Technical Architecture

## 1. System Architecture Overview

```
                          ┌──────────────────────────┐
                          │   GOOGLE SHEETS CLOUD    │
                          │  (Dynamic Source Engine) │
                          └─────────────┬────────────┘
                                        │ (HTTPS / Two-Way Sync)
                                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      FIREBASE FIRESTORE CLOUD                          │
│                                                                        │
│  Collections:                                                          │
│   • /leads/{leadId}             • /companies/{companyId}               │
│   • /activity_records/{id}      • /team_leaders/{tlId}                 │
│   • /audit_logs/{logId}         • /employees/{employeeId}              │
│   • /followup_records/{phone}   • /systemHealth/{testId}               │
└───────▲───────────────────────────────▲────────────────────────▲───────┘
        │ (Realtime Snapshot Listeners) │                        │
        │                               │                        │
┌───────┴───────────────┐ ┌─────────────┴─────────────┐ ┌────────┴──────────────┐
│    EMPLOYEE PORTAL    │ │    TEAM LEADER PORTAL     │ │  OWNER / ADMIN PORTAL │
│ (Strict Emp Isolation)│ │ (Strict Team Isolation)   │ │  (Full Org Oversight) │
│                       │ │                           │ │                       │
│ • Realtime Work Inbox │ │ • Team Live Dashboard     │ │ • Master Multi-Company│
│ • One-Click Call/Link │ │ • Telecaller Audits       │ │ • Live Diagnostic Ping│
│ • Offline Cache Queue │ │ • Aggregated Analytics    │ │ • Sheet Auto-Sync Hub │
└───────────────────────┘ └───────────────────────────┘ └───────────────────────┘
```

## 2. Multi-Employee Data Isolation Engine

Every lead document is identified with a deterministic, collision-proof compound key:
`leadId = "${companyId}_${employeeId}_${sourceRowIndex}"`

### Stable Lead Identity Structure:
* `leadId`: Unique UUID / compound identifier.
* `companyId`: Unique ID representing the business unit (e.g. `comp_01`).
* `companyCode`: Human-readable identifier (e.g. `IND08`, `IND15`).
* `teamLeaderId`: Unique ID of the supervising Team Leader.
* `teamLeaderName`: Name of the supervising Team Leader.
* `assignedEmployeeId`: Unique ID of the assigned telecaller.
* `assignedEmployeeName`: Name of the assigned telecaller.
* `phone`: Normalized 10-digit calling number.
* `sourceSheetId`: Connected Google Sheet document ID.
* `sheetTabName`: Telecaller's individual worksheet tab.
* `sourceRowIndex`: Exact row index in the original Google Sheet.
* `status`: Operational state (`PENDING`, `INTERESTED`, `SUCCESSFUL`, `LINK_SENT`, `CALLBACK`, `FOLLOW_UP`, `NOT_INTERESTED`).
* `currentRemark`: Specific qualitative tag selected by caller.
* `updatedAt`: Epoc millisecond timestamp of last state transition.

## 3. Conflict Resolution & Sync Idempotency

When synchronizing between the remote Google Sheet and Firestore:
1. **Timestamp Precedence**: If a local or Firestore lead has an `updatedAt` newer than the sheet import time, employee actions (e.g. `INTERESTED` / `SUCCESSFUL`) are preserved and never overwritten by older sheet values.
2. **Deterministic Ordering**: Visual rendering strictly respects `sourceRowIndex` sorting so telecallers always navigate their queue in original sheet order without random shuffling.
3. **Duplicate Suppression**: Debounced UI actions and immutable action tokens guarantee that tapping action buttons repeatedly does not generate duplicate Firestore records or corrupted state transitions.

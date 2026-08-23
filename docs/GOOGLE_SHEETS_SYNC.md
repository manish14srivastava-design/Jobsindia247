# Google Sheets Two-Way Integration Guide

## 1. Sheet Structure Requirements

Each assigned telecaller has a dedicated tab inside the company Google Sheet. 

### Standard Column Mapping:
| Column A | Column B | Column C | Column D | Column E |
| :--- | :--- | :--- | :--- | :--- |
| **Phone Number** | **Customer Name** | **Status** | **Remarks** | **Last Updated** |
| 9876543210 | Ramesh Sharma | INTERESTED | Callback requested | 2026-08-23 15:30:00 |

## 2. Dynamic Live Sync Flow
1. **Fetch & Ingest**: When an employee or Team Leader initiates a sync, the app reads the assigned tab.
2. **Deterministic Identifier**: Each row is indexed and mapped to `sourceRowIndex`.
3. **Smart Merge**:
   - If the lead does not exist in Firestore, it is created with `PENDING` state.
   - If the lead already exists and has been marked by an employee (`INTERESTED`, `SUCCESSFUL`), the newer local/cloud state is preserved.
4. **Push Updates**: Converted or modified leads are written to Firestore and exported back to the sheet dynamically.

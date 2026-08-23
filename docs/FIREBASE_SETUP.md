# Firebase Firestore Setup & Configuration Guide

## 1. Prerequisites
1. A Google Cloud / Firebase account.
2. Firebase project created at [Firebase Console](https://console.firebase.google.com).
3. Android app registered with package name `com.example` (or your configured `applicationId`).

## 2. Setting Up `google-services.json`
1. Download `google-services.json` from your Firebase Project Settings.
2. Place the file in `/app/google-services.json`.

## 3. Provisioning Firestore Database
1. In the Firebase console, navigate to **Firestore Database** -> **Create database**.
2. Select standard Native mode and your preferred geographic region (e.g. `asia-south1`).
3. Deploy the security rules from `/firebase/firestore.rules`:
```bash
firebase deploy --only firestore:rules
```
4. Deploy the compound indexes from `/firebase/indexes.json`:
```bash
firebase deploy --only firestore:indexes
```

## 4. Operational Collections
* `/leads`: Master operational records with telecaller and status mapping.
* `/activity_records`: Granular audit log of every call, remark, and link event.
* `/audit_logs`: Detailed time-stamped transitions with previous and new states.
* `/companies`: Company entities (`IND08`, `IND15`, etc.).
* `/team_leaders`: Team Leader definitions with supervised team rosters.
* `/employees`: Telecaller registry with individual Google Sheet tab mappings.
* `/departments`: Organizational divisions (`Telecalling`, `Verification`, etc.).
* `/systemHealth`: Diagnostic ping endpoint for real-time connection verification.

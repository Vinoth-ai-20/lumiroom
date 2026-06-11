# Synchronization Architecture

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  

[⬅ Back to Architecture](Architecture.md)

Lumiroom allows users to design rooms fully offline. The `SyncManager` is responsible for reconciling the local SQLite state with the remote Firestore state when connectivity is available.

## The Sync Process

Synchronization is treated as a background process entirely decoupled from the UI.

1. **Change Tracking**: Every entity in the Room database has an `updated_at` timestamp.
2. **Observation**: `SyncManager` observes the `RoomDesignDao` for changes where `updated_at` is newer than the last successful sync timestamp.
3. **Connectivity Manager**: The sync process is gated by an Android `ConnectivityManager` flow.
4. **Batching**: Changes are bundled into a Firebase `WriteBatch` to minimize network calls and ensure atomicity.
5. **Conflict Resolution**: Lumiroom uses a simple **Last-Write-Wins (LWW)** strategy based on the `updated_at` timestamp. Complex collaborative merging (like CRDTs) is not currently supported; the system assumes a single primary designer per room.

## WorkManager Integration

If the app is closed while changes are pending, a constrained `WorkRequest` (`RoomSyncWorker`) is enqueued via Android `WorkManager`. 

**Constraints:**
- `NetworkType.CONNECTED`
- `BatteryNotLow`

This ensures that the user's work is eventually backed up to the cloud without draining battery or data plans.

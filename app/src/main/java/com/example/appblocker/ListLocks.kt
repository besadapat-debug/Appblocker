package com.example.appblocker

import android.content.Context

/**
 * Independent Strict locks, one per block list plus one for the adult filter,
 * on top of the master lock. Each uses its own scope in StrictModeStore /
 * UnlockMethodStore / StrictScheduleStore, so deactivating one (say, Social
 * media) does NOT unlock the others (say, Adult sites).
 *
 * Uninstalling / disabling the app stays blocked while ANY lock is active — the
 * master lock OR any list lock OR the adult lock — so everything must be
 * individually unlocked before the app can be removed.
 */
object ListLocks {

    const val ADULT_SCOPE = "_adult_"

    fun scopeFor(listId: String): String = "_list_$listId"

    fun isListLocked(context: Context, listId: String): Boolean =
        StrictModeStore.isActive(context, scopeFor(listId))

    fun isAdultLocked(context: Context): Boolean =
        StrictModeStore.isActive(context, ADULT_SCOPE)

    fun anyListLocked(context: Context): Boolean =
        BlockListStore.getAll(context).any { isListLocked(context, it.id) }

    /** True if the master lock, any list lock, or the adult lock is active. */
    fun anyLockActive(context: Context): Boolean =
        StrictModeStore.isActive(context) || isAdultLocked(context) || anyListLocked(context)

    /** A block list can't be weakened/disabled while its own lock OR the master lock is on. */
    fun isListFrozen(context: Context, listId: String): Boolean =
        StrictModeStore.isActive(context) || isListLocked(context, listId)
}

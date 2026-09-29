package com.theseuntaylor.bookawards.data

/** What a manual refresh says: always something, since a silent failure looks like nothing happened. */
fun RefreshOutcome.message(): String = when (this) {
    is RefreshOutcome.Updated -> if (newNominations == 1) "1 new nomination" else "$newNominations new nominations"
    RefreshOutcome.UpToDate -> "You're up to date"
    RefreshOutcome.Failed -> "Couldn't check for new nominations"
}

/** The automatic check on launch only speaks up when it brought in something new. */
fun RefreshOutcome.launchMessage(): String? = (this as? RefreshOutcome.Updated)?.message()

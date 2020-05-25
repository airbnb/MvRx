package com.airbnb.mvrx;

import androidx.annotation.RestrictTo;

public class MvRxTestOverrides {
    /**
     * This should only be set by the MvRxTestRule from the mvrx-testing artifact.
     *
     * This can be used to force MvRxViewModels to disable lifecycle aware observer for unit testing.
     * This is Java so it can be package private.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Boolean FORCE_DISABLE_LIFECYCLE_AWARE_OBSERVER = false;
}

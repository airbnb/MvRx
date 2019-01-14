# ------------------------
#
#  MvRx Config
#
# -----------------------

# BaseMvRxViewModels loads the Companion class via reflection and thus we need to make sure we keep
# the name of the Companion object.
-keepclassmembers class ** extends com.airbnb.mvrx.BaseMvRxViewModel {
    ** Companion;
}

# Classes extending BaseMvRxViewModel are recreated using reflection, which assumes that a one argument
# constructor accepting a data class holding the state exists. Need to make sure to keep the constructor
# around. Additionally, a static create / inital state method will be generated in the case a
# companion object factory is used with JvmStatic. This is accessed via reflection.
-keepclassmembers class ** extends com.airbnb.mvrx.BaseMvRxViewModel {
    public <init>(...);
    public static *** create(...);
    public static *** initialState(...);
}

# If a MvRxViewModelFactory is used with JvmStatic, keep create and initalState methods which
# are accessed via reflection.
-keepclassmembers class ** implements com.airbnb.mvrx.MvRxViewModelFactory {
     public <init>(...);
     public *** create(...);
     public *** initialState(...);
}


# Members of the Kotlin data classes used as the state in MvRx are read via Kotlin reflection which cause trouble
# with Proguard if they are not kept.
-keepclassmembers class ** implements com.airbnb.mvrx.MvRxState {
   *;
}
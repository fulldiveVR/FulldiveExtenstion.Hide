package pan.alexander.tordnscrypt.appextension

sealed class AppExtensionState(val id: String) {
    object START: AppExtensionState("START")
    object STOP: AppExtensionState("STOP")
    object FAILURE: AppExtensionState("FAILURE")
}
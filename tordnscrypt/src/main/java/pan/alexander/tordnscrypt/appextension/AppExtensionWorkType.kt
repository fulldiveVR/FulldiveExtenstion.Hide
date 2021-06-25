package pan.alexander.tordnscrypt.appextension

sealed class AppExtensionWorkType(val id: String) {
    object START: AppExtensionWorkType("START")
    object STOP: AppExtensionWorkType("STOP")
    object OPEN: AppExtensionWorkType("OPEN")
}
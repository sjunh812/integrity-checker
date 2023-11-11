package org.sjhstudio.integritychecker.integrity

sealed interface IntegrityState {
    data object UnKnown : IntegrityState
    data object Pass : IntegrityState
    data object Fail : IntegrityState
}
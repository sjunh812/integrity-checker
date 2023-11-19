package org.sjhstudio.integritychecker.integrity.model

sealed interface IntegrityState {
    object UnKnown : IntegrityState
    object Pass : IntegrityState
    object Fail : IntegrityState
}
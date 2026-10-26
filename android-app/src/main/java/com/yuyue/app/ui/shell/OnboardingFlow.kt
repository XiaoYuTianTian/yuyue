package com.yuyue.app.ui.shell

/**
 * Pure onboarding state machine: intro → real hold-to-speak → preview → first entry.
 * Mic permission is requested on the speak action (UI), not during Intro.
 */
enum class OnboardingPhase {
    Intro,
    Speak,
    Preview,
    Committing,
    Finished,
}

data class OnboardingState(
    val phase: OnboardingPhase = OnboardingPhase.Intro,
    val previewText: String = "",
    val error: String? = null,
    val micExplainerShown: Boolean = false,
) {
    val canConfirmPreview: Boolean
        get() = phase == OnboardingPhase.Preview && previewText.isNotBlank()
}

sealed interface OnboardingEvent {
    data object StartHoldFlow : OnboardingEvent
    data object MicExplainerAcknowledged : OnboardingEvent
    data class PreviewReady(val transcript: String) : OnboardingEvent
    data object Redo : OnboardingEvent
    data object ConfirmWrite : OnboardingEvent
    data object WriteSucceeded : OnboardingEvent
    data class WriteFailed(val message: String) : OnboardingEvent
    data class SpeakFailed(val message: String) : OnboardingEvent
}

object OnboardingFlow {
    fun reduce(state: OnboardingState, event: OnboardingEvent): OnboardingState = when (event) {
        OnboardingEvent.StartHoldFlow -> state.copy(
            phase = OnboardingPhase.Speak,
            previewText = "",
            error = null,
        )
        OnboardingEvent.MicExplainerAcknowledged -> state.copy(micExplainerShown = true)
        is OnboardingEvent.PreviewReady -> {
            val text = event.transcript.trim()
            if (text.isBlank()) {
                state.copy(phase = OnboardingPhase.Speak, error = "没听清，请再按住说一句")
            } else {
                state.copy(
                    phase = OnboardingPhase.Preview,
                    previewText = text,
                    error = null,
                )
            }
        }
        OnboardingEvent.Redo -> state.copy(
            phase = OnboardingPhase.Speak,
            previewText = "",
            error = null,
        )
        OnboardingEvent.ConfirmWrite -> {
            if (!state.canConfirmPreview) state
            else state.copy(phase = OnboardingPhase.Committing, error = null)
        }
        OnboardingEvent.WriteSucceeded -> state.copy(phase = OnboardingPhase.Finished)
        is OnboardingEvent.WriteFailed -> state.copy(
            phase = OnboardingPhase.Preview,
            error = event.message,
        )
        is OnboardingEvent.SpeakFailed -> state.copy(
            phase = OnboardingPhase.Speak,
            error = event.message,
        )
    }
}

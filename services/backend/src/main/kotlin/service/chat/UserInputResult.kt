sealed interface UserInputResult {

    data class CommandHandled(
        val systemResponse: String
    ) : UserInputResult

    data class IntentHandled(
        val systemResponse: String
    ) : UserInputResult

    data class ForwardToLlm(
        val userMessage: String
    ) : UserInputResult
}

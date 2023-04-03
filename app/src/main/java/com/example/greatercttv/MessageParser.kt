package com.example.greatercttv

fun parseMessage(message: String): ParsedMessage? {
    val parsedMessage = ParsedMessage(null, null, null, null, null)

    var idx = 0

    var rawTagsComponent: String? = null
    var rawSourceComponent: String? = null
    var rawCommandComponent: String? = null
    var rawParametersComponent: String? = null

    if (message[idx] == '@') {
        val endIdx = message.indexOf(' ')
        rawTagsComponent = message.substring(1, endIdx)
        idx = endIdx + 1
    }

    if (message[idx] == ':') {
        idx += 1
        val endIdx = message.indexOf(' ', idx)
        rawSourceComponent = message.substring(idx, endIdx)
        idx = endIdx + 1
    }

    val endIdx = message.indexOf(':', idx).let { if (it == -1) message.length else it }
    rawCommandComponent = message.substring(idx, endIdx).trim()

    if (endIdx != message.length) {
        idx = endIdx + 1
        rawParametersComponent = message.substring(idx)
    }

    parsedMessage.command = parseCommand(rawCommandComponent)

    parsedMessage.command?.let {
        if (rawTagsComponent != null) {
            parsedMessage.tags = parseTags(rawTagsComponent)
        }

        parsedMessage.source = parseSource(rawSourceComponent)


        parsedMessage.parameters = rawParametersComponent
        if (rawParametersComponent != null && rawParametersComponent[0] == '!') {
            parsedMessage.command = parseParameters(rawParametersComponent, it)
        }
    } ?: return null

    return parsedMessage
}

fun parseTags(tags: String): Map<String, Any?> {
    val tagsToIgnore = setOf("client-nonce", "flags")
    val dictParsedTags = mutableMapOf<String, Any?>()
    val parsedTags = tags.split(';')

    parsedTags.forEach { tag ->
        val parsedTag = tag.split('=')
        val tagValue = if (parsedTag[1].isEmpty()) null else parsedTag[1]

        when (parsedTag[0]) {
            "badges", "badge-info" -> {
                if (tagValue != null) {
                    val dict = mutableMapOf<String, String>()
                    val badges = tagValue.split(',')
                    badges.forEach { pair ->
                        val badgeParts = pair.split('/')
                        dict[badgeParts[0]] = badgeParts[1]
                    }
                    dictParsedTags[parsedTag[0]] = dict
                } else {
                    dictParsedTags[parsedTag[0]] = null
                }
            }

            "emotes" -> {
                if (tagValue != null) {
                    val dictEmotes = mutableMapOf<String, List<Map<String, String>>>()
                    val emotes = tagValue.split('/')
                    emotes.forEach { emote ->
                        val emoteParts = emote.split(':')

                        val textPositions = mutableListOf<Map<String, String>>()
                        val positions = emoteParts[1].split(',')
                        positions.forEach { position ->
                            val positionParts = position.split('-')
                            textPositions.add(
                                mapOf(
                                    "startPosition" to positionParts[0],
                                    "endPosition" to positionParts[1]
                                )
                            )
                        }

                        dictEmotes[emoteParts[0]] = textPositions
                    }

                    dictParsedTags[parsedTag[0]] = dictEmotes
                } else {
                    dictParsedTags[parsedTag[0]] = null
                }
            }

            "emote-sets" -> {
                val emoteSetIds = tagValue?.split(',') ?: emptyList()
                dictParsedTags[parsedTag[0]] = emoteSetIds
            }

            else -> {
                if (!tagsToIgnore.contains(parsedTag[0])) {
                    dictParsedTags[parsedTag[0]] = tagValue
                }
            }
        }
    }

    return dictParsedTags
}

fun parseCommand(rawCommandComponent: String): Command? {
    val commandParts = rawCommandComponent.split(' ')
    val parsedCommand = when (commandParts[0]) {
        "JOIN", "PART", "NOTICE", "CLEARCHAT", "HOSTTARGET", "PRIVMSG" ->
            Command(commandParts[0], commandParts[1], null, null)

        "PING" -> Command(commandParts[0], null, null, null)
        "CAP" -> Command(commandParts[0], null, commandParts[2] == "ACK", null)
        "GLOBALUSERSTATE", "USERSTATE", "ROOMSTATE" ->
            Command(commandParts[0], commandParts[1], null, null)

        "RECONNECT" -> {
            println("The Twitch IRC server is about to terminate the connection for maintenance.")
            Command(commandParts[0], null, null, null)
        }

        "421" -> {
            println("Unsupported IRC command: ${commandParts[2]}")
            return null
        }

        "001" -> Command(commandParts[0], commandParts[1], null, null)
        "002", "003", "004", "353", "366", "372", "375", "376" -> {
            println("numeric message: ${commandParts[0]}")
            return null
        }

        else -> {
            println("\nUnexpected command: ${commandParts[0]}\n")
            return null
        }
    }

    return parsedCommand
}

fun parseSource(rawSourceComponent: String?): Source? {
    if (rawSourceComponent == null) {
        return null
    }

    val sourceParts = rawSourceComponent.split('!')
    return if (sourceParts.size == 2) {
        Source(sourceParts[0], sourceParts[1])
    } else {
        Source(null, sourceParts[0])
    }
}

fun parseParameters(rawParametersComponent: String, command: Command): Command {
    val commandParts = rawParametersComponent.substring(1).trim()
    val paramsIdx = commandParts.indexOf(' ')

    return if (paramsIdx == -1) {
        command.copy(botCommand = commandParts)
    } else {
        command.copy(
            botCommand = commandParts.substring(0, paramsIdx),
            botCommandParams = commandParts.substring(paramsIdx).trim()
        )
    }
}

data class ParsedMessage(
    var tags: Map<String, Any?>?,
    var source: Source?,
    var command: Command?,
    var parameters: String?,
    var split: MutableList<String>?
)

data class Command(
    val command: String,
    val channel: String?,
    val isCapRequestEnabled: Boolean?,
    var botCommand: String?,
    var botCommandParams: String? = null
)

data class Source(val nick: String?, val host: String)


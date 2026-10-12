package com.dtolan.numbers.data

/**
 * Everything one language needs for Speak mode (answer out loud, graded by speech recognition —
 * see [com.dtolan.numbers.logic.SpeakGenerator]). Templates use `{ord}` for an ordinal in words
 * and `{k}` for a plain count; the generator fills them and the TTS voice reads them.
 */
data class SpeakPack(
    /**
     * The recognizer locale, with a region: the on-device recognizer rejects a bare "fr"/"it" and
     * the server fallback it drops to often returns nothing for a single short word ("dix").
     */
    val recognizerTag: String,
    val modeLabel: String,
    val modeSubtitle: String,
    val hint: String,
    val homeSection: String,
    /** One title per level, in order (see SpeakGenerator.LEVEL_COUNT). */
    val levelTitles: List<String>,
    val sayNumber: String,
    val sayOrdinal: String,
    val sayYear: String,
    val sayNext: String,
    val sayPrevious: String,
    val sayResult: String,
    val sayPosition: String,
    val plus: String,
    val minus: String,
    /** You overtake the runner in {ord} place — you're now in that place. {ord} agrees with "position/place". */
    val raceTemplate: String,
    /** You're {ord} in a queue and {k} people ahead leave. */
    val queueTemplate: String,
    /** You're on the {ord} floor and go up {k} floors. */
    val floorTemplate: String,
    /** Whether "position"/"place" in [raceTemplate] takes a feminine ordinal (French, Italian). */
    val raceFeminine: Boolean,
    val micButton: String,
    val listeningLabel: String,
    val heardLabel: String,          // format with 1 %s = what the recognizer heard
    val retryButton: String,
    /** Has the voice say the correct answer, as a model to copy. */
    val hearAnswerButton: String,
    /** Shown after a miss: nudges toward saying it again before moving on. */
    val repeatToContinue: String,
    /** "Next" after a miss asks first; confirming skips, the other choice says it again. */
    val skipConfirmTitle: String,
    val skipConfirmBody: String,
    val skipConfirmYes: String,
    val noSpeechMessage: String,
    val permissionMessage: String,
    val unavailableMessage: String,
    val errorMessage: String,        // format with 1 %d = SpeechRecognizer error code
)

object SpeakPacks {
    val FRENCH = SpeakPack(
        recognizerTag = "fr-FR",
        modeLabel = "Parler",
        modeSubtitle = "Répondez à voix haute",
        hint = "Dites la réponse à voix haute — le téléphone vous écoute et vérifie. Choisissez librement votre niveau.",
        homeSection = "Parler",
        levelTitles = listOf(
            "Nombres 0–100", "Grands nombres", "Ordinaux", "Années",
            "Suivant / précédent", "Calcul mental", "Positions",
        ),
        sayNumber = "Dites ce nombre",
        sayOrdinal = "Dites ce rang (premier, deuxième…)",
        sayYear = "Dites cette année",
        sayNext = "Écoutez, puis dites le nombre suivant",
        sayPrevious = "Écoutez, puis dites le nombre précédent",
        sayResult = "Écoutez, puis dites le résultat",
        sayPosition = "Écoutez, puis répondez par un rang (premier, deuxième…)",
        plus = "plus",
        minus = "moins",
        raceTemplate = "Pendant une course, tu dépasses le coureur en {ord} position. En quelle position es-tu maintenant ?",
        queueTemplate = "Tu es {ord} dans la file d'attente. {k} personnes devant toi s'en vont. À quelle place es-tu maintenant ?",
        floorTemplate = "Tu es au {ord} étage. Tu montes de {k} étages. À quel étage es-tu maintenant ?",
        raceFeminine = true,
        micButton = "🎤 Parler",
        listeningLabel = "J'écoute…",
        heardLabel = "Entendu : « %s »",
        retryButton = "Redire",
        hearAnswerButton = "🔊 Écouter la bonne réponse",
        repeatToContinue = "Écoutez la bonne réponse, puis redites-la correctement.",
        skipConfirmTitle = "Passer ?",
        skipConfirmBody = "Vous ne l'avez pas encore dit correctement. Passer quand même à la suivante ?",
        skipConfirmYes = "Passer",
        noSpeechMessage = "Je n'ai rien entendu — réessayez.",
        permissionMessage = "Autorisez le micro pour répondre à voix haute.",
        unavailableMessage = "La reconnaissance vocale n'est pas disponible sur cet appareil.",
        errorMessage = "La reconnaissance vocale a échoué (erreur %d) — réessayez.",
    )

    val ITALIAN = SpeakPack(
        recognizerTag = "it-IT",
        modeLabel = "Parlare",
        modeSubtitle = "Rispondi a voce",
        hint = "Di' la risposta ad alta voce: il telefono ascolta e controlla. Scegli liberamente il livello.",
        homeSection = "Parlare",
        levelTitles = listOf(
            "Numeri 0–100", "Numeri grandi", "Ordinali", "Anni",
            "Successivo / precedente", "Calcolo a mente", "Posizioni",
        ),
        sayNumber = "Di' questo numero",
        sayOrdinal = "Di' questo ordinale (primo, secondo…)",
        sayYear = "Di' quest'anno",
        sayNext = "Ascolta, poi di' il numero successivo",
        sayPrevious = "Ascolta, poi di' il numero precedente",
        sayResult = "Ascolta, poi di' il risultato",
        sayPosition = "Ascolta, poi rispondi con un ordinale (primo, secondo…)",
        plus = "più",
        minus = "meno",
        raceTemplate = "Durante una gara, superi il corridore in {ord} posizione. In che posizione sei adesso?",
        queueTemplate = "Sei {ord} in fila. {k} persone davanti a te se ne vanno. In che posizione sei adesso?",
        floorTemplate = "Sei al {ord} piano. Sali di {k} piani. A che piano sei adesso?",
        raceFeminine = true,
        micButton = "🎤 Parla",
        listeningLabel = "Ascolto…",
        heardLabel = "Ho sentito: «%s»",
        retryButton = "Ripeti",
        hearAnswerButton = "🔊 Ascolta la risposta giusta",
        repeatToContinue = "Ascolta la risposta giusta, poi ripetila correttamente.",
        skipConfirmTitle = "Saltare?",
        skipConfirmBody = "Non l'hai ancora detto correttamente. Passare comunque alla successiva?",
        skipConfirmYes = "Salta",
        noSpeechMessage = "Non ho sentito niente: riprova.",
        permissionMessage = "Consenti l'uso del microfono per rispondere a voce.",
        unavailableMessage = "Il riconoscimento vocale non è disponibile su questo dispositivo.",
        errorMessage = "Riconoscimento vocale non riuscito (errore %d): riprova.",
    )

    val ENGLISH = SpeakPack(
        recognizerTag = "en-US",
        modeLabel = "Speak",
        modeSubtitle = "Answer out loud",
        hint = "Say the answer out loud — the phone listens and checks it. Pick any level you like.",
        homeSection = "Speak",
        levelTitles = listOf(
            "Numbers 0–100", "Big numbers", "Ordinals", "Years",
            "Next / previous", "Mental maths", "Positions",
        ),
        sayNumber = "Say this number",
        sayOrdinal = "Say this position (first, second…)",
        sayYear = "Say this year",
        sayNext = "Listen, then say the next number",
        sayPrevious = "Listen, then say the number before it",
        sayResult = "Listen, then say the answer",
        sayPosition = "Listen, then answer with a position (first, second…)",
        plus = "plus",
        minus = "minus",
        raceTemplate = "In a race, you overtake the runner in {ord} place. What place are you in now?",
        queueTemplate = "You're {ord} in line. {k} people ahead of you leave. What place are you in now?",
        floorTemplate = "You're on the {ord} floor. You go up {k} floors. What floor are you on now?",
        raceFeminine = false,
        micButton = "🎤 Speak",
        listeningLabel = "Listening…",
        heardLabel = "Heard: “%s”",
        retryButton = "Say it again",
        hearAnswerButton = "🔊 Hear how it's said",
        repeatToContinue = "Listen to the right answer, then say it correctly.",
        skipConfirmTitle = "Skip this one?",
        skipConfirmBody = "You haven't said it correctly yet. Move on anyway?",
        skipConfirmYes = "Skip",
        noSpeechMessage = "I didn't catch that — try again.",
        permissionMessage = "Allow the microphone to answer out loud.",
        unavailableMessage = "Speech recognition isn't available on this device.",
        errorMessage = "Speech recognition failed (error %d) — try again.",
    )
}

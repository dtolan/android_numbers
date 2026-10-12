package com.dtolan.numbers.data

import com.dtolan.numbers.data.StoryKey.*
import com.dtolan.numbers.data.StoryTheme.*
import com.dtolan.numbers.logic.pad

/**
 * Story-problem content for each built-in language, referenced from [Languages]. A new language
 * needs one more [StoryPack] here (or alongside its [LanguagePack]) defining every [StoryKey].
 */
object Stories {

    private fun joinWith(conjunction: String): (List<String>) -> String = { parts ->
        if (parts.size <= 1) parts.joinToString("") else parts.dropLast(1).joinToString(", ") + " $conjunction " + parts.last()
    }

    /** French elides "de" before a vowel sound: "d'oranges", "d'œufs" — but not before y ("de yaourts"). */
    private fun frDe(word: String) = if (word.first().lowercaseChar() in "aeiouœéè") "d'$word" else "de $word"

    /** Italian masculine plural article: "gli" before a vowel, y, z, gn, ps or s+consonant, else "i". */
    private fun itMasculinePlural(word: String): String {
        val w = word.lowercase()
        val gli = w.first() in "aeiouyz" || w.startsWith("gn") || w.startsWith("ps") ||
            (w.length > 1 && w[0] == 's' && w[1] !in "aeiou")
        return if (gli) "gli" else "i"
    }

    val FRENCH = StoryPack(
        themeTitles = mapOf(
            SHOPPING to "Courses", CAFE to "Café", TRAVEL to "Transports",
            APPOINTMENTS to "Rendez-vous", HEALTH to "Santé", HOME to "Maison",
        ),
        levelTitles = mapOf(
            SHOPPING to listOf(
                "Liste courte", "Liste longue", "Total", "Prix", "Au poids", "Monnaie",
                "Changement d'avis", "Courses complètes", "Dialogue au marché",
            ),
            CAFE to listOf("Commande", "Addition", "Partager", "Table", "Payer", "Changement d'avis"),
            TRAVEL to listOf("Bus", "Départ", "Arrivée", "Gare routière", "Retard", "Guichet"),
            APPOINTMENTS to listOf("Heure", "Date", "Durée", "Horaires d'ouverture", "Report", "Âges", "Correction"),
            HEALTH to listOf("Posologie", "Traitement", "Pharmacie", "Cabinet", "Pharmacien"),
            HOME to listOf("Adresse", "Appartement", "Code", "Message", "Correction", "Invitation"),
        ),
        groceries = listOf(
            StoryItem("pommes", true), StoryItem("bananes", true), StoryItem("oranges", true),
            StoryItem("tomates", true), StoryItem("carottes", true), StoryItem("œufs"),
            StoryItem("citrons"), StoryItem("poires", true), StoryItem("yaourts"), StoryItem("croissants"),
        ),
        cafeItems = listOf(
            StoryItem("cafés"), StoryItem("thés"), StoryItem("croissants"),
            StoryItem("chocolats chauds"), StoryItem("sandwichs"), StoryItem("crêpes", true),
        ),
        deliItems = listOf("jambon", "fromage", "saumon fumé", "poulet rôti", "pâté"),
        pictures = mapOf(
            "pommes" to "🍎", "bananes" to "🍌", "oranges" to "🍊", "tomates" to "🍅", "carottes" to "🥕",
            "œufs" to "🥚", "citrons" to "🍋", "poires" to "🍐", "yaourts" to "🥛", "croissants" to "🥐",
            "cafés" to "☕", "thés" to "🍵", "chocolats chauds" to "🍫", "sandwichs" to "🥪", "crêpes" to "🥞",
            "jambon" to "🍖", "fromage" to "🧀", "saumon fumé" to "🐟", "poulet rôti" to "🍗", "pâté" to "🥫",
        ),
        cities = listOf("Lyon", "Marseille", "Bordeaux", "Lille", "Nantes", "Strasbourg"),
        streets = listOf("rue Victor Hugo", "avenue Jean Jaurès", "boulevard Voltaire", "rue de la Paix", "rue Pasteur"),
        join = joinWith("et"),
        howMany = { item -> "Combien ${frDe(item.plural)}" },
        the = { item -> "les ${item.plural}" },
        clock = { hour, minute ->
            val h = if (hour <= 1) "$hour heure" else "$hour heures"
            if (minute == 0) h else "$h $minute"
        },
        duration = { minutes ->
            val h = minutes / 60
            val m = minutes % 60
            when {
                h == 0 -> "$m minutes"
                m == 0 -> if (h == 1) "1 heure" else "$h heures"
                else -> if (h == 1) "1 heure $m" else "$h heures $m"
            }
        },
        templates = mapOf(
            PRICED_LINE to "{qty} {item} à {price} pièce",
            PAY_WITH_NOTE to "Tu paies avec un billet de {note}.",
            ASK_PAY to "Combien dois-tu payer en tout ?",
            ASK_CHANGE to "Combien d'argent te rend-on ?",
            ASK_FLOOR to "À quel étage ?",

            SHOP_LIST to "Tu vas au magasin acheter {list}.",
            SHOP_ASK_HOW_MANY to "{howMany} dois-tu acheter ?",
            SHOP_ASK_TOTAL to "Combien d'articles dois-tu acheter en tout ?",
            SHOP_PURCHASE to "Au marché, tu achètes {list}.",
            SHOP_COST to "Tes courses coûtent {total}.",
            SHOP_DELI to "À la charcuterie, tu demandes {grams} grammes de {item} à {price} le kilo.",
            SHOP_CORRECTION to "Ah non, finalement, il te faut {qty} {item}.",
            SHOP_DIALOGUE_ASK to "Bonjour, combien coûtent {theItem} ?",
            SHOP_DIALOGUE_PRICE to "{price} pièce, ou {n} pour {bundle}.",
            SHOP_DIALOGUE_TAKE to "Alors j'en prends {n}.",

            CAFE_ORDER to "Au café, vous commandez {list}.",
            CAFE_ASK_HOW_MANY to "{howMany} y a-t-il dans la commande ?",
            CAFE_BILL to "Au café, vous prenez {list}.",
            CAFE_ASK_BILL to "Combien coûte l'addition ?",
            CAFE_SPLIT to "L'addition est de {total}. Vous êtes {n} et vous partagez en parts égales.",
            CAFE_ASK_EACH to "Combien paie chacun ?",
            CAFE_TABLE to "Vous êtes {people} à la table {table} et vous commandez {list}.",
            CAFE_ASK_TABLE to "À quelle table êtes-vous ?",
            CAFE_ASK_PEOPLE to "Combien de personnes êtes-vous ?",
            CAFE_WAITER_TOTAL to "Ça fera {total}, s'il vous plaît.",
            CAFE_YOU_PAY to "Tenez, voilà {note}.",
            CAFE_ASK_WAITER_CHANGE to "Combien le serveur doit-il te rendre ?",
            CAFE_CHANGE_MIND to "Puis vous changez d'avis : finalement, {newQty} {item} au lieu de {oldQty}.",
            CAFE_ASK_TOTAL_ITEMS to "Combien de consommations y a-t-il en tout ?",

            TRAVEL_BUS to "Pour aller à la gare, prends le bus {bus}.",
            TRAVEL_ASK_BUS to "Quel bus dois-tu prendre ?",
            TRAVEL_DEPARTS to "Le train pour {city} part à {time}.",
            TRAVEL_ASK_DEPARTURE to "À quelle heure part le train ?",
            TRAVEL_DURATION to "Le trajet dure {duration}.",
            TRAVEL_ASK_ARRIVAL to "À quelle heure arrives-tu ?",
            TRAVEL_COACH to "Le car {coach} part du quai {bay} à {time}.",
            TRAVEL_ASK_COACH to "Quel est le numéro du car ?",
            TRAVEL_ASK_BAY to "De quel quai part le car ?",
            TRAVEL_ASK_COACH_TIME to "À quelle heure part le car ?",
            TRAVEL_DELAY to "Le train de {time} a {delay} minutes de retard.",
            TRAVEL_ASK_NEW_DEPARTURE to "À quelle heure va-t-il partir ?",
            TRAVEL_TICKET_YOU to "Bonjour, {n} billets pour {city}, s'il vous plaît.",
            TRAVEL_TICKET_CLERK to "Ça fait {price} le billet. Le train part de la voie {platform} à {time}.",
            TRAVEL_ASK_PLATFORM to "De quelle voie part le train ?",

            APPT_TIME to "Ton rendez-vous chez le dentiste est à {time}.",
            APPT_ASK_TIME to "À quelle heure est ton rendez-vous ?",
            APPT_DATE to "Ton rendez-vous chez le médecin est {date}.",
            APPT_ASK_DATE to "Quelle est la date de ton rendez-vous ?",
            APPT_FILM to "Le film commence à {time} et dure {duration}.",
            APPT_ASK_FILM_END to "À quelle heure se termine-t-il ?",
            APPT_OPENING to "La banque est ouverte de {a} à {b}, puis de {c} à {d}.",
            APPT_ASK_OPEN_HOURS to "Combien d'heures est-elle ouverte par jour ?",
            APPT_POSTPONED to "Ton rendez-vous était prévu {date}. Il est repoussé de {days} jours.",
            APPT_ASK_NEW_DATE to "Quelle est la nouvelle date ?",
            APPT_AGE to "Ta grand-mère est née en {year}.",
            APPT_ASK_AGE to "Quel âge aura-t-elle à son anniversaire en {now} ?",
            APPT_MEETING to "La réunion est à {t1}… non, pardon, à {t2}.",
            APPT_ASK_MEETING to "À quelle heure est la réunion ?",

            HEALTH_DOSE to "Le médecin dit : prends {perDose} comprimés {times} fois par jour.",
            HEALTH_ASK_PER_DOSE to "Combien de comprimés prends-tu à chaque fois ?",
            HEALTH_COURSE to "Le médecin dit : prends {perDose} comprimés {times} fois par jour pendant {days} jours.",
            HEALTH_ASK_TOTAL to "Combien de comprimés prends-tu en tout ?",
            HEALTH_PHARMACY to "À la pharmacie, tu achètes {n} boîtes à {price} la boîte.",
            HEALTH_CLINIC to "Le docteur Martin consulte au {floor}e étage, salle {room}, à {time}.",
            HEALTH_ASK_ROOM to "Dans quelle salle ?",
            HEALTH_PHARMACIST to "Prenez {perDose} comprimés matin et soir pendant {days} jours.",
            HEALTH_YOU_BOXES to "Il me faut combien de boîtes ?",
            HEALTH_BOX_SIZE to "Une boîte contient {box} comprimés.",
            HEALTH_ASK_BOXES to "Combien de boîtes te faut-il ?",

            HOME_ADDRESS to "J'habite au {number} {street}.",
            HOME_ASK_NUMBER to "À quel numéro j'habite ?",
            HOME_FULL to "J'habite au {number} {street}, au {floor}e étage, appartement {apt}.",
            HOME_ASK_APT to "Quel est le numéro de l'appartement ?",
            HOME_CODE to "Le code de la porte d'entrée est {code}.",
            HOME_ASK_CODE to "Quel est le code ?",
            HOME_MESSAGE to "Message de Paul : rappelle-moi à la maison au {home}, ou au bureau au {office}.",
            HOME_ASK_HOME_PHONE to "Quel est le numéro de Paul à la maison ?",
            HOME_CODE_CORRECTION to "Le code est {c1}. Ah non, pardon, c'est {c2}.",
            HOME_INVITE to "Viens chez moi samedi à {time}. J'habite au {number} {street}, au {floor}e étage, et le code est {code}.",
            HOME_ASK_FRIEND_NUMBER to "À quel numéro habite ton ami ?",
            HOME_ASK_PARTY_TIME to "À quelle heure dois-tu arriver ?",
        ),
    )

    val ITALIAN = StoryPack(
        themeTitles = mapOf(
            SHOPPING to "Spesa", CAFE to "Bar", TRAVEL to "Viaggi",
            APPOINTMENTS to "Appuntamenti", HEALTH to "Salute", HOME to "Casa",
        ),
        levelTitles = mapOf(
            SHOPPING to listOf(
                "Lista breve", "Lista lunga", "Totale", "Prezzi", "A peso", "Resto",
                "Cambio idea", "Spesa completa", "Dialogo al mercato",
            ),
            CAFE to listOf("Ordinazione", "Conto", "Dividere", "Tavolo", "Pagare", "Cambio idea"),
            TRAVEL to listOf("Autobus", "Partenza", "Arrivo", "Autostazione", "Ritardo", "Biglietteria"),
            APPOINTMENTS to listOf("Ora", "Data", "Durata", "Orari di apertura", "Rinvio", "Età", "Correzione"),
            HEALTH to listOf("Dosaggio", "Terapia", "Farmacia", "Ambulatorio", "Farmacista"),
            HOME to listOf("Indirizzo", "Appartamento", "Codice", "Messaggio", "Correzione", "Invito"),
        ),
        groceries = listOf(
            StoryItem("mele", true), StoryItem("banane", true), StoryItem("arance", true),
            StoryItem("pomodori"), StoryItem("carote", true), StoryItem("uova", true),
            StoryItem("limoni"), StoryItem("pere", true), StoryItem("yogurt"), StoryItem("cornetti"),
        ),
        cafeItems = listOf(
            StoryItem("caffè"), StoryItem("tè"), StoryItem("cornetti"),
            StoryItem("cappuccini"), StoryItem("panini"), StoryItem("spremute", true),
        ),
        deliItems = listOf("prosciutto", "formaggio", "salame", "mortadella", "bresaola"),
        pictures = mapOf(
            "mele" to "🍎", "banane" to "🍌", "arance" to "🍊", "pomodori" to "🍅", "carote" to "🥕",
            "uova" to "🥚", "limoni" to "🍋", "pere" to "🍐", "yogurt" to "🥛", "cornetti" to "🥐",
            "caffè" to "☕", "tè" to "🍵", "cappuccini" to "☕", "panini" to "🥪", "spremute" to "🧃",
            "prosciutto" to "🍖", "formaggio" to "🧀", "salame" to "🍖", "mortadella" to "🍖", "bresaola" to "🥩",
        ),
        cities = listOf("Milano", "Roma", "Firenze", "Napoli", "Bologna", "Torino"),
        streets = listOf("via Garibaldi", "via Roma", "corso Italia", "via Mazzini", "piazza Dante"),
        join = joinWith("e"),
        howMany = { item -> "${if (item.feminine) "Quante" else "Quanti"} ${item.plural}" },
        the = { item -> "${if (item.feminine) "le" else itMasculinePlural(item.plural)} ${item.plural}" },
        clock = { hour, minute -> if (minute == 0) "$hour" else "$hour e $minute" },
        duration = { minutes ->
            val h = minutes / 60
            val m = minutes % 60
            when {
                h == 0 -> "$m minuti"
                m == 0 -> if (h == 1) "un'ora" else "$h ore"
                else -> if (h == 1) "un'ora e $m minuti" else "$h ore e $m minuti"
            }
        },
        templates = mapOf(
            PRICED_LINE to "{qty} {item} a {price} al pezzo",
            PAY_WITH_NOTE to "Paghi con una banconota da {note}.",
            ASK_PAY to "Quanto devi pagare in tutto?",
            ASK_CHANGE to "Quanto resto ricevi?",
            ASK_FLOOR to "A che piano?",

            SHOP_LIST to "Vai al negozio a comprare {list}.",
            SHOP_ASK_HOW_MANY to "{howMany} devi comprare?",
            SHOP_ASK_TOTAL to "Quanti articoli devi comprare in tutto?",
            SHOP_PURCHASE to "Al mercato compri {list}.",
            SHOP_COST to "La spesa costa {total}.",
            SHOP_DELI to "Al banco salumi chiedi {grams} grammi di {item} a {price} al chilo.",
            SHOP_CORRECTION to "Anzi no, alla fine ti servono {qty} {item}.",
            SHOP_DIALOGUE_ASK to "Buongiorno, quanto costano {theItem}?",
            SHOP_DIALOGUE_PRICE to "{price} al pezzo, oppure {n} per {bundle}.",
            SHOP_DIALOGUE_TAKE to "Allora ne prendo {n}.",

            CAFE_ORDER to "Al bar ordinate {list}.",
            CAFE_ASK_HOW_MANY to "{howMany} avete ordinato?",
            CAFE_BILL to "Al bar prendete {list}.",
            CAFE_ASK_BILL to "Quanto viene il conto?",
            CAFE_SPLIT to "Il conto è di {total}. Siete in {n} e dividete in parti uguali.",
            CAFE_ASK_EACH to "Quanto paga ciascuno?",
            CAFE_TABLE to "Siete in {people} al tavolo {table} e ordinate {list}.",
            CAFE_ASK_TABLE to "A che tavolo siete?",
            CAFE_ASK_PEOPLE to "In quanti siete?",
            CAFE_WAITER_TOTAL to "Sono {total}, grazie.",
            CAFE_YOU_PAY to "Ecco a lei {note}.",
            CAFE_ASK_WAITER_CHANGE to "Quanto resto ti deve dare il cameriere?",
            CAFE_CHANGE_MIND to "Poi cambiate idea: alla fine {newQty} {item} invece di {oldQty}.",
            CAFE_ASK_TOTAL_ITEMS to "Quante consumazioni ci sono in tutto?",

            TRAVEL_BUS to "Per andare alla stazione, prendi l'autobus {bus}.",
            TRAVEL_ASK_BUS to "Quale autobus devi prendere?",
            TRAVEL_DEPARTS to "Il treno per {city} parte alle {time}.",
            TRAVEL_ASK_DEPARTURE to "A che ora parte il treno?",
            TRAVEL_DURATION to "Il viaggio dura {duration}.",
            TRAVEL_ASK_ARRIVAL to "A che ora arrivi?",
            TRAVEL_COACH to "Il pullman {coach} parte dalla corsia {bay} alle {time}.",
            TRAVEL_ASK_COACH to "Qual è il numero del pullman?",
            TRAVEL_ASK_BAY to "Da quale corsia parte il pullman?",
            TRAVEL_ASK_COACH_TIME to "A che ora parte il pullman?",
            TRAVEL_DELAY to "Il treno delle {time} ha {delay} minuti di ritardo.",
            TRAVEL_ASK_NEW_DEPARTURE to "A che ora partirà?",
            TRAVEL_TICKET_YOU to "Buongiorno, {n} biglietti per {city}, per favore.",
            TRAVEL_TICKET_CLERK to "Sono {price} a biglietto. Il treno parte dal binario {platform} alle {time}.",
            TRAVEL_ASK_PLATFORM to "Da quale binario parte il treno?",

            APPT_TIME to "Il tuo appuntamento dal dentista è alle {time}.",
            APPT_ASK_TIME to "A che ora è il tuo appuntamento?",
            APPT_DATE to "Il tuo appuntamento dal medico è {date}.",
            APPT_ASK_DATE to "Qual è la data del tuo appuntamento?",
            APPT_FILM to "Il film comincia alle {time} e dura {duration}.",
            APPT_ASK_FILM_END to "A che ora finisce?",
            APPT_OPENING to "La banca è aperta dalle {a} alle {b} e poi dalle {c} alle {d}.",
            APPT_ASK_OPEN_HOURS to "Quante ore al giorno è aperta?",
            APPT_POSTPONED to "Il tuo appuntamento era previsto per {date}. È rimandato di {days} giorni.",
            APPT_ASK_NEW_DATE to "Qual è la nuova data?",
            APPT_AGE to "Tua nonna è nata nel {year}.",
            APPT_ASK_AGE to "Quanti anni compie nel {now}?",
            APPT_MEETING to "La riunione è alle {t1}… no, scusa, alle {t2}.",
            APPT_ASK_MEETING to "A che ora è la riunione?",

            HEALTH_DOSE to "Il medico dice: prendi {perDose} compresse {times} volte al giorno.",
            HEALTH_ASK_PER_DOSE to "Quante compresse prendi ogni volta?",
            HEALTH_COURSE to "Il medico dice: prendi {perDose} compresse {times} volte al giorno per {days} giorni.",
            HEALTH_ASK_TOTAL to "Quante compresse prendi in tutto?",
            HEALTH_PHARMACY to "In farmacia compri {n} confezioni a {price} l'una.",
            HEALTH_CLINIC to "La dottoressa Rossi riceve al piano {floor}, stanza {room}, alle {time}.",
            HEALTH_ASK_ROOM to "In quale stanza?",
            HEALTH_PHARMACIST to "Prenda {perDose} compresse mattina e sera per {days} giorni.",
            HEALTH_YOU_BOXES to "Quante confezioni mi servono?",
            HEALTH_BOX_SIZE to "Una confezione contiene {box} compresse.",
            HEALTH_ASK_BOXES to "Quante confezioni ti servono?",

            HOME_ADDRESS to "Abito in {street} {number}.",
            HOME_ASK_NUMBER to "A che numero abito?",
            HOME_FULL to "Abito in {street} {number}, al piano {floor}, interno {apt}.",
            HOME_ASK_APT to "Qual è il numero dell'interno?",
            HOME_CODE to "Il codice del portone è {code}.",
            HOME_ASK_CODE to "Qual è il codice?",
            HOME_MESSAGE to "Messaggio di Marco: richiamami a casa al {home}, oppure in ufficio al {office}.",
            HOME_ASK_HOME_PHONE to "Qual è il numero di casa di Marco?",
            HOME_CODE_CORRECTION to "Il codice è {c1}. Ah no, scusa, è {c2}.",
            HOME_INVITE to "Vieni da me sabato alle {time}. Abito in {street} {number}, al piano {floor}, e il codice è {code}.",
            HOME_ASK_FRIEND_NUMBER to "A che numero abita il tuo amico?",
            HOME_ASK_PARTY_TIME to "A che ora devi arrivare?",
        ),
    )

    val ENGLISH = StoryPack(
        themeTitles = mapOf(
            SHOPPING to "Shopping", CAFE to "Café", TRAVEL to "Travel",
            APPOINTMENTS to "Appointments", HEALTH to "Health", HOME to "Home",
        ),
        levelTitles = mapOf(
            SHOPPING to listOf(
                "Short list", "Long list", "Totals", "Prices", "By weight", "Change",
                "Change of mind", "Full shopping trip", "Market dialogue",
            ),
            CAFE to listOf("Order", "Bill", "Splitting", "Table", "Paying", "Change of mind"),
            TRAVEL to listOf("Bus", "Departure", "Arrival", "Coach station", "Delays", "Ticket office"),
            APPOINTMENTS to listOf("Time", "Date", "Duration", "Opening hours", "Postponed", "Ages", "Correction"),
            HEALTH to listOf("Dosage", "Treatment", "Pharmacy", "Clinic", "Pharmacist"),
            HOME to listOf("Address", "Apartment", "Door code", "Message", "Correction", "Invitation"),
        ),
        groceries = listOf(
            StoryItem("apples"), StoryItem("bananas"), StoryItem("oranges"), StoryItem("tomatoes"),
            StoryItem("carrots"), StoryItem("eggs"), StoryItem("lemons"), StoryItem("pears"),
            StoryItem("yogurts"), StoryItem("croissants"),
        ),
        cafeItems = listOf(
            StoryItem("coffees"), StoryItem("teas"), StoryItem("croissants"),
            StoryItem("hot chocolates"), StoryItem("sandwiches"), StoryItem("cappuccinos"),
        ),
        deliItems = listOf("ham", "cheese", "smoked salmon", "salami", "roast turkey"),
        pictures = mapOf(
            "apples" to "🍎", "bananas" to "🍌", "oranges" to "🍊", "tomatoes" to "🍅", "carrots" to "🥕",
            "eggs" to "🥚", "lemons" to "🍋", "pears" to "🍐", "yogurts" to "🥛", "croissants" to "🥐",
            "coffees" to "☕", "teas" to "🍵", "hot chocolates" to "🍫", "sandwiches" to "🥪", "cappuccinos" to "☕",
            "ham" to "🍖", "cheese" to "🧀", "smoked salmon" to "🐟", "salami" to "🍖", "roast turkey" to "🦃",
        ),
        cities = listOf("Boston", "Chicago", "Seattle", "Denver", "Atlanta", "Dallas"),
        streets = listOf("Maple Street", "Oak Avenue", "Park Road", "Elm Street", "Main Street"),
        join = joinWith("and"),
        howMany = { item -> "How many ${item.plural}" },
        the = { item -> "the ${item.plural}" },
        clock = { hour, minute -> "$hour:${pad(minute)}" },
        duration = { minutes ->
            val h = minutes / 60
            val m = minutes % 60
            val hours = if (h == 1) "1 hour" else "$h hours"
            when {
                h == 0 -> "$m minutes"
                m == 0 -> hours
                else -> "$hours and $m minutes"
            }
        },
        templates = mapOf(
            PRICED_LINE to "{qty} {item} at {price} each",
            PAY_WITH_NOTE to "You pay with a {note} bill.",
            ASK_PAY to "How much do you pay in total?",
            ASK_CHANGE to "How much change do you get back?",
            ASK_FLOOR to "Which floor?",

            SHOP_LIST to "You're going to the store to pick up {list}.",
            SHOP_ASK_HOW_MANY to "{howMany} do you need?",
            SHOP_ASK_TOTAL to "How many items do you need in total?",
            SHOP_PURCHASE to "At the market, you buy {list}.",
            SHOP_COST to "Your groceries come to {total}.",
            SHOP_DELI to "At the deli, you ask for {grams} grams of {item} at {price} a kilo.",
            SHOP_CORRECTION to "Oh wait, actually, make that {qty} {item}.",
            SHOP_DIALOGUE_ASK to "Hi, how much are {theItem}?",
            SHOP_DIALOGUE_PRICE to "{price} each, or {n} for {bundle}.",
            SHOP_DIALOGUE_TAKE to "Then I'll take {n}.",

            CAFE_ORDER to "At the café, you order {list}.",
            CAFE_ASK_HOW_MANY to "{howMany} did you order?",
            CAFE_BILL to "At the café, you have {list}.",
            CAFE_ASK_BILL to "How much is the bill?",
            CAFE_SPLIT to "The bill comes to {total}. There are {n} of you, splitting it evenly.",
            CAFE_ASK_EACH to "How much does each person pay?",
            CAFE_TABLE to "The {people} of you are at table {table}, and you order {list}.",
            CAFE_ASK_TABLE to "What table are you at?",
            CAFE_ASK_PEOPLE to "How many of you are there?",
            CAFE_WAITER_TOTAL to "That'll be {total}, please.",
            CAFE_YOU_PAY to "Here's {note}.",
            CAFE_ASK_WAITER_CHANGE to "How much change should the waiter give you?",
            CAFE_CHANGE_MIND to "Then you change your mind: {newQty} {item} instead of {oldQty}.",
            CAFE_ASK_TOTAL_ITEMS to "How many items are on the order in total?",

            TRAVEL_BUS to "To get to the station, take bus {bus}.",
            TRAVEL_ASK_BUS to "Which bus do you take?",
            TRAVEL_DEPARTS to "The train to {city} leaves at {time}.",
            TRAVEL_ASK_DEPARTURE to "What time does the train leave?",
            TRAVEL_DURATION to "The journey takes {duration}.",
            TRAVEL_ASK_ARRIVAL to "What time do you arrive?",
            TRAVEL_COACH to "Coach {coach} leaves from bay {bay} at {time}.",
            TRAVEL_ASK_COACH to "What's the coach number?",
            TRAVEL_ASK_BAY to "Which bay does it leave from?",
            TRAVEL_ASK_COACH_TIME to "What time does the coach leave?",
            TRAVEL_DELAY to "The {time} train is running {delay} minutes late.",
            TRAVEL_ASK_NEW_DEPARTURE to "What time will it leave now?",
            TRAVEL_TICKET_YOU to "Hi, {n} tickets to {city}, please.",
            TRAVEL_TICKET_CLERK to "That's {price} per ticket. The train leaves from platform {platform} at {time}.",
            TRAVEL_ASK_PLATFORM to "Which platform does the train leave from?",

            APPT_TIME to "Your dentist appointment is at {time}.",
            APPT_ASK_TIME to "What time is your appointment?",
            APPT_DATE to "Your doctor's appointment is on {date}.",
            APPT_ASK_DATE to "What's the date of your appointment?",
            APPT_FILM to "The movie starts at {time} and runs {duration}.",
            APPT_ASK_FILM_END to "What time does it end?",
            APPT_OPENING to "The bank is open from {a} to {b}, then from {c} to {d}.",
            APPT_ASK_OPEN_HOURS to "How many hours a day is it open?",
            APPT_POSTPONED to "Your appointment was set for {date}. It's been pushed back {days} days.",
            APPT_ASK_NEW_DATE to "What's the new date?",
            APPT_AGE to "Your grandmother was born in {year}.",
            APPT_ASK_AGE to "How old does she turn in {now}?",
            APPT_MEETING to "The meeting is at {t1}… no, sorry, at {t2}.",
            APPT_ASK_MEETING to "What time is the meeting?",

            HEALTH_DOSE to "The doctor says: take {perDose} tablets {times} times a day.",
            HEALTH_ASK_PER_DOSE to "How many tablets do you take each time?",
            HEALTH_COURSE to "The doctor says: take {perDose} tablets {times} times a day for {days} days.",
            HEALTH_ASK_TOTAL to "How many tablets do you take in total?",
            HEALTH_PHARMACY to "At the pharmacy, you buy {n} boxes at {price} a box.",
            HEALTH_CLINIC to "Dr. Smith sees patients on floor {floor}, room {room}, at {time}.",
            HEALTH_ASK_ROOM to "Which room?",
            HEALTH_PHARMACIST to "Take {perDose} tablets morning and evening for {days} days.",
            HEALTH_YOU_BOXES to "How many boxes will I need?",
            HEALTH_BOX_SIZE to "Each box has {box} tablets.",
            HEALTH_ASK_BOXES to "How many boxes do you need?",

            HOME_ADDRESS to "I live at {number} {street}.",
            HOME_ASK_NUMBER to "What number do I live at?",
            HOME_FULL to "I live at {number} {street}, on floor {floor}, apartment {apt}.",
            HOME_ASK_APT to "What's the apartment number?",
            HOME_CODE to "The front door code is {code}.",
            HOME_ASK_CODE to "What's the code?",
            HOME_MESSAGE to "Message from Mike: call me back at home at {home}, or at the office at {office}.",
            HOME_ASK_HOME_PHONE to "What's Mike's home number?",
            HOME_CODE_CORRECTION to "The code is {c1}. Oh no, sorry, it's {c2}.",
            HOME_INVITE to "Come over on Saturday at {time}. I live at {number} {street}, floor {floor}, and the code is {code}.",
            HOME_ASK_FRIEND_NUMBER to "What number does your friend live at?",
            HOME_ASK_PARTY_TIME to "What time should you arrive?",
        ),
    )
}

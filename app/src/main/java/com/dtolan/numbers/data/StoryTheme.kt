package com.dtolan.numbers.data

/** A Stories-mode theme; each has its own ladder of levels (see StoryGenerator) and unlock progress. */
enum class StoryTheme { SHOPPING, CAFE, TRAVEL, APPOINTMENTS, HEALTH, HOME }

/**
 * Sentence templates every [StoryPack] must define. Placeholders in braces are filled by
 * StoryGenerator; the comment lists the ones each template receives.
 */
enum class StoryKey {
    // Shared
    PRICED_LINE,        // {qty} {item} {price}
    PAY_WITH_NOTE,      // {note}
    ASK_PAY,
    ASK_CHANGE,
    ASK_FLOOR,

    // Shopping
    SHOP_LIST,          // {list}
    SHOP_ASK_HOW_MANY,  // {howMany}
    SHOP_ASK_TOTAL,
    SHOP_PURCHASE,      // {list}
    SHOP_COST,          // {total}
    SHOP_DELI,          // {grams} {item} {price}
    SHOP_CORRECTION,    // {qty} {item}
    SHOP_DIALOGUE_ASK,  // {theItem}           (you)
    SHOP_DIALOGUE_PRICE,// {price} {n} {bundle} (vendor)
    SHOP_DIALOGUE_TAKE, // {n}                 (you)

    // Café
    CAFE_ORDER,         // {list}
    CAFE_ASK_HOW_MANY,  // {howMany}
    CAFE_BILL,          // {list}
    CAFE_ASK_BILL,
    CAFE_SPLIT,         // {total} {n}
    CAFE_ASK_EACH,
    CAFE_TABLE,         // {table} {people} {list}
    CAFE_ASK_TABLE,
    CAFE_ASK_PEOPLE,
    CAFE_WAITER_TOTAL,  // {total} (waiter)
    CAFE_YOU_PAY,       // {note}  (you)
    CAFE_ASK_WAITER_CHANGE,
    CAFE_CHANGE_MIND,   // {newQty} {item} {oldQty}
    CAFE_ASK_TOTAL_ITEMS,

    // Travel
    TRAVEL_BUS,         // {bus}
    TRAVEL_ASK_BUS,
    TRAVEL_DEPARTS,     // {city} {time}
    TRAVEL_ASK_DEPARTURE,
    TRAVEL_DURATION,    // {duration}
    TRAVEL_ASK_ARRIVAL,
    TRAVEL_COACH,       // {coach} {bay} {time}
    TRAVEL_ASK_COACH,
    TRAVEL_ASK_BAY,
    TRAVEL_ASK_COACH_TIME,
    TRAVEL_DELAY,       // {time} {delay}
    TRAVEL_ASK_NEW_DEPARTURE,
    TRAVEL_TICKET_YOU,  // {n} {city}                (you)
    TRAVEL_TICKET_CLERK,// {price} {platform} {time} (clerk)
    TRAVEL_ASK_PLATFORM,

    // Appointments
    APPT_TIME,          // {time}
    APPT_ASK_TIME,
    APPT_DATE,          // {date}
    APPT_ASK_DATE,
    APPT_FILM,          // {time} {duration}
    APPT_ASK_FILM_END,
    APPT_OPENING,       // {a} {b} {c} {d}
    APPT_ASK_OPEN_HOURS,
    APPT_POSTPONED,     // {date} {days}
    APPT_ASK_NEW_DATE,
    APPT_AGE,           // {year}
    APPT_ASK_AGE,       // {now}
    APPT_MEETING,       // {t1} {t2}
    APPT_ASK_MEETING,

    // Health
    HEALTH_DOSE,        // {perDose} {times}
    HEALTH_ASK_PER_DOSE,
    HEALTH_COURSE,      // {perDose} {times} {days}
    HEALTH_ASK_TOTAL,
    HEALTH_PHARMACY,    // {n} {price}
    HEALTH_CLINIC,      // {floor} {room} {time}
    HEALTH_ASK_ROOM,
    HEALTH_PHARMACIST,  // {perDose} {days}  (pharmacist)
    HEALTH_YOU_BOXES,   //                  (you)
    HEALTH_BOX_SIZE,    // {box}            (pharmacist)
    HEALTH_ASK_BOXES,

    // Home
    HOME_ADDRESS,       // {number} {street}
    HOME_ASK_NUMBER,
    HOME_FULL,          // {number} {street} {floor} {apt}
    HOME_ASK_APT,
    HOME_CODE,          // {code}
    HOME_ASK_CODE,
    HOME_MESSAGE,       // {home} {office}
    HOME_ASK_HOME_PHONE,
    HOME_CODE_CORRECTION, // {c1} {c2}
    HOME_INVITE,        // {time} {number} {street} {floor} {code} (friend)
    HOME_ASK_FRIEND_NUMBER,
    HOME_ASK_PARTY_TIME,
}

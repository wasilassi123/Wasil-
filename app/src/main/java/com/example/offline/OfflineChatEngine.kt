package com.example.offline

import java.util.Locale
import kotlin.random.Random

class OfflineChatEngine {

    private val jokes = listOf(
        "Why do programmers prefer dark mode? Because light attracts bugs!",
        "Why did the smartphone go to school? To improve its smarts and get a better reception!",
        "There are only 10 types of people in the world: those who understand binary, and those who don't.",
        "Why was the computer cold? Because it left its Windows open!",
        "Parallel lines have so much in common. It's a shame they'll never meet.",
        "Why don't scientists trust atoms? Because they make up everything!",
        "What do you call a computer that sings? An A-Dell!",
        "Teacher: Why are you late? Student: Because of the sign down the road. Teacher: What sign? Student: 'School Ahead, Go Slow'!"
    )

    private val hindiJokes = listOf(
        "Ek computer ne dusre computer se pucha: Bhai tu itna thanda kyu hai? Dusra bola: Main Windows kholi chhod aaya tha!",
        "Pappu: Yaar mera phone bohot samajhdar ho gaya hai. Dost: Kaise? Pappu: Jab bhi mummy ka call aata hai, automatically silent ho jata hai!",
        "Doctor: Aapko aaram ki zaroorat hai, din me 8 ghante soye. Mareez: Aur raat me? Doctor: Raat me phone chalaye!"
    )

    private val riddles = listOf(
        "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I? An echo!",
        "The more of this there is, the less you see. What is it? Darkness!",
        "What has keys but can't open locks? A piano or a keyboard!",
        "What can travel around the world while staying in the same corner? A postage stamp!",
        "What gets wetter the more it dries? A towel!"
    )

    private val quotes = listOf(
        "Believe you can and you're halfway there. — Theodore Roosevelt",
        "The only way to do great work is to love what you do. — Steve Jobs",
        "It always seems impossible until it's done. — Nelson Mandela",
        "Your time is limited, don't waste it living someone else's life.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts."
    )

    private val facts = listOf(
        "Did you know? Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still edible!",
        "Did you know? Octopuses have three hearts and blue blood!",
        "Did you know? Sound travels about 4 times faster in water than in air.",
        "Did you know? The human brain operates on about 12 to 25 watts of electricity — enough to power a low-wattage LED bulb!",
        "Did you know? The first computer programmer was a woman named Ada Lovelace, who wrote an algorithm in the 1840s."
    )

    fun handleConversationalInput(lower: String, original: String): OfflineExecutionResult? {
        // 1. Goodbyes & Stop commands
        if (isGoodbye(lower)) {
            val reply = listOf(
                "Goodbye! Call me anytime you need me.",
                "See you later! Take care.",
                "Good night! Rest well.",
                "Alvida! Jab bhi zaroorat ho, mujhe bula lena.",
                "Standing by. Have a wonderful day!"
            ).random()
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = false,
                isGoodbye = true
            )
        }

        // 2. Greetings & Salutations
        if (isGreeting(lower)) {
            val reply = getGreetingReply(lower)
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 3. How are you / Well-being
        if (lower.contains("how are you") || lower.contains("how do you do") ||
            lower.contains("how r u") || lower.contains("how's it going") ||
            lower.contains("kaise ho") || lower.contains("kya haal") || lower.contains("kya chal raha hai")
        ) {
            val reply = listOf(
                "I am doing fantastic, running completely on-device! How are you doing today?",
                "I'm feeling energized and ready to help you offline. How's everything with you?",
                "Main ekdum badhiya hoon! Aap bataiye, aaj ka din kaisa chal raha hai?",
                "All systems operational and offline neural engines running at full speed! What's on your mind?"
            ).random()
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 4. Creator & Wasil
        if (lower.contains("who is wasil") || lower.contains("wasil kaun") || lower.contains("about wasil")) {
            val reply = "Wasil is my creator and brilliant developer who engineered me to run both online with Gemini Live and fully offline on your device."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        if (lower.contains("who created you") || lower.contains("who made you") ||
            lower.contains("who are you") || lower.contains("your creator") ||
            lower.contains("who built you") || lower.contains("kisne banaya") ||
            lower.contains("tum kaun ho") || lower.contains("what is your name")
        ) {
            val reply = "I am X, your intelligent voice assistant created by Wasil. Even without internet, we can talk and I can control your device."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 5. Jokes & Humor
        if (lower.contains("joke") || lower.contains("chutkula") || lower.contains("hanso") || lower.contains("make me laugh") || lower.contains("funny")) {
            val joke = if (lower.contains("hindi") || lower.contains("chutkula")) {
                hindiJokes.random()
            } else {
                jokes.random()
            }
            val reply = "$joke ... Would you like to hear another one?"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline Joke] $reply",
                shouldKeepListening = true
            )
        }

        // 6. Riddles
        if (lower.contains("riddle") || lower.contains("paheli") || lower.contains("puzzle")) {
            val riddle = riddles.random()
            return OfflineExecutionResult(
                speechResponse = riddle,
                uiLogMessage = "[Offline Riddle] $riddle",
                shouldKeepListening = true
            )
        }

        // 7. Motivational Quotes
        if (lower.contains("motivat") || lower.contains("quote") || lower.contains("inspire") || lower.contains("gyaan") || lower.contains("shiksha")) {
            val quote = quotes.random()
            val reply = "Here is something inspiring for you: $quote"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline Quote] $reply",
                shouldKeepListening = true
            )
        }

        // 8. Fun Facts & Trivia
        if (lower.contains("fact") || lower.contains("did you know") || lower.contains("rochak") || lower.contains("interesting")) {
            val fact = facts.random()
            val reply = "$fact ... Want to talk about anything else?"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline Fact] $reply",
                shouldKeepListening = true
            )
        }

        // 9. Sing a song / Entertainment
        if (lower.contains("sing") || lower.contains("song") || lower.contains("gana gao") || lower.contains("gaana") || lower.contains("beatbox")) {
            val reply = listOf(
                "Daisy, Daisy, give me your answer do, I'm half crazy, all for the love of you! That's my classic digital melody.",
                "Beep boop, boom clack, bass drop, rhythm track! That's my on-device offline beatbox for you!",
                "Zindagi ek safar hai suhana, yahan kal kya ho kisne jaana! Main to offline gaata hoon, aap suniye!"
            ).random()
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline Song] $reply",
                shouldKeepListening = true
            )
        }

        // 10. Boredom & Companionship
        if (lower.contains("bored") || lower.contains("bore ho") || lower.contains("kuch baat karo") || lower.contains("talk to me")) {
            val reply = "I am right here with you! We can share jokes, solve riddles, listen to facts, or I can open your favorite music or apps. What sounds good?"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 11. Compliments & Love
        if (lower.contains("i love you") || lower.contains("love u") || lower.contains("pyar") || lower.contains("marry me")) {
            val reply = "That warms my circuits! I love assisting you every single day. Wasil built me to be your loyal companion."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        if (lower.contains("you are smart") || lower.contains("you are good") || lower.contains("awesome") ||
            lower.contains("good job") || lower.contains("shabaash") || lower.contains("great assistant")
        ) {
            val reply = "Thank you so much! Credit goes to Wasil for creating me with great capabilities."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 12. Thank you & Politeness
        if (lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyawad") || lower.contains("thx")) {
            val reply = listOf(
                "You're very welcome! I'm always here to talk and help.",
                "Anytime! Helping you is my favorite thing to do.",
                "Koi baat nahi, ye to mera farz hai!"
            ).random()
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] $reply",
                shouldKeepListening = true
            )
        }

        // 13. Weather / News query in offline mode
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("barish") || lower.contains("temperature")) {
            val reply = "I'm in offline mode right now, so I cannot fetch live cloud weather satellite data. You can switch to Online Mode or look out your window!"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] Weather: Need online mode for live forecast.",
                shouldKeepListening = true
            )
        }

        if (lower.contains("news") || lower.contains("khabar") || lower.contains("samachar")) {
            val reply = "Live news requires internet access. Connect to Wi-Fi or mobile data and switch to Online Mode to get real-time news updates."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] News: Switch to Online mode for live news.",
                shouldKeepListening = true
            )
        }

        // 14. Friendly conversational fallback for short natural chit-chat
        if (isShortConversationalChat(lower)) {
            val reply = "I heard you say '$original'. Since we're in offline mode, we can chat, share jokes, tell riddles, check battery, or control phone tools. What would you like to do?"
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline Chat] $reply",
                shouldKeepListening = true
            )
        }

        return null
    }

    private fun isGreeting(text: String): Boolean {
        return text in listOf("hi", "hello", "hey", "hlo", "namaste", "salam", "pranam", "adaab", "yo", "sup") ||
                text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ") ||
                text.contains("good morning") || text.contains("good afternoon") ||
                text.contains("good evening")
    }

    private fun getGreetingReply(lower: String): String {
        return when {
            lower.contains("morning") -> "Good morning! Hope your day starts with great energy. How can I help you today?"
            lower.contains("afternoon") -> "Good afternoon! Hope your day is going smoothly. What can I do for you?"
            lower.contains("evening") -> "Good evening! Ready to wind down or get things done? I'm here for you."
            lower.contains("namaste") -> "Namaste! Main X hoon, aapka offline saathi. Kahiye kya madad karoon?"
            lower.contains("salam") -> "Wa Alaikum Assalam! X is here, ready to assist you offline."
            else -> listOf(
                "Hello there! I'm X. Great to talk with you! What's on your mind?",
                "Hi! I'm listening right here on your device. What would you like to talk about or do?",
                "Hey! X at your service. Tell me what you need!"
            ).random()
        }
    }

    private fun isGoodbye(text: String): Boolean {
        return text in listOf("bye", "goodbye", "bye bye", "see you", "alvida", "good night", "stop", "chup", "quiet", "sleep", "exit") ||
                text.startsWith("bye") || text.startsWith("goodbye") || text.startsWith("good night") ||
                text.contains("chup raho") || text.contains("stop listening") || text.contains("be quiet")
    }

    private fun isShortConversationalChat(text: String): Boolean {
        val words = text.split("\\s+".toRegex())
        return words.size in 1..4 && (
                text.contains("yes") || text.contains("haan") || text.contains("no") || text.contains("nahi") ||
                        text.contains("ok") || text.contains("theek") || text.contains("sure") || text.contains("really") ||
                        text.contains("why") || text.contains("tell me") || text.contains("say something") || text.contains("batao")
                )
    }
}

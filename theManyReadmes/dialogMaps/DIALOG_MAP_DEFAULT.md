note: when a conversation calls optionSet_beginning it always runs the 'base' optionsSet that is ran in this dialog. (in this case, it runs 'greeting's options.)
* "greeting"
  * "option_avoid_battle" :           "suggest_ceasefire"
  * "option_ask_tournament" :         "start_tournament"
  * "option_dedicate_tournament" :    "dedicate_tournament"
  * "option_host_wedding" :           "marriage_ceremony"
  * "option_ask_current_task" :       "current_task_desc"
  * "option_ask_question" :           "ask_question"
  * "option_suggest_action" :         "suggest_action"
  * "option_speak_privately" :        "speak_privately"
  * "option_cutComLink" :             "exitDialog"
* "suggest_ceasefire":
  * copys 'greeting' options

* "start_tournament":
  * "option_continue_to_tournament" : -opens tournament-
  * "option_avoid_tournament" :       "greeting"

* "dedicate_tournament"
  * copys 'greeting' options


* "current_task_desc":
  * '%c0' is the lords current target name.
  * null options (keeps options that are up when loaded.)

* "ask_question"
  * "option_ask_location" :         "accept_ask_location"
  * "option_ask_quest" :            "ask_quest"
  * "option_profess_admiration" :   "admiration_response"
  * "option_ask_date" :             "spend_time_together"
  * "option_ask_marriage" :         "marriage_response"
  * "option_ask_leave_party" :      "leave_party_explanation"
  * "option_ask_join_party" :       "join_party_explanation"
  * "option_sway_council_support" : "swayProposal_forCounsel"
  * "option_sway_council_oppose" :  "swayProposal_againstCounsel"
  * "option_sway_player" :          "swayProposal_forPlayer"
  * "option_nevermind_askQuestion" :"greeting"

* "ask_quest" (runs 'quest_available'&&'' || 'no_quest_available')
  * copys 'ask_question' options

* "admiration_response"
  * copys 'ask_question' options

* "spend_time_together"
  * "option_give_gift" :            "give_gift"
  * "option_dont_give_gift" :       "ask_question"

* "give_gift"
  * "option_gift_alpha_core" :      "giveGift_alpha_core"
  * "option_gift_hand_weapons" :    "giveGift_hand_weapons"
  * "option_gift_food" :            "giveGift_food"
  * "option_gift_luxury_goods" :    "giveGift_luxury_goods"
  * "option_gift_lobster" :         "giveGift_lobster"
  * "option_gift_drugs" :           "giveGift_drugs"
  * "option_gift_domestic_goods" :  "giveGift_domestic_goods"
  * "options_give_gift_exit" :      "ask_question"

* "giveGift_alpha_core"
* "giveGift_hand_weapons"
* "giveGift_food"
* "giveGift_luxury_goods"
* "giveGift_lobster"
* "giveGift_drugs"
* "giveGift_domestic_goods"
  * copys 'ask_question' options

* "marriage_ceremony"
    * copys 'greeting' options

* "marriage_response"
    * copys 'ask_question' options

* "join_party_explanation"
    * "option_confirm_join_party": "ask_question"
    * "option_nevermind_join_party": "ask_question"

* "leave_party_explanation"
    * "option_confirm_leave_party": "ask_question"
    * "option_nevermind_leave_party": "ask_question"

* "swayProposal_forCounsel":
  * copys 'ask_question' options
  * ||
  * "swayProposal_forCounsel_Bribe_accept": "swayProposal_forCounsel_acceptBribe"
  * "swayProposal_forCounsel_Bribe_refuse": "ask_question" 
  * ||
  * "swayProposal_forCounsel_Bargain_accept": "swayProposal_forCounsel_acceptBargain"
  * "swayProposal_forCounsel_Bargain_refuse": "ask_question"
* "swayProposal_againstCounsel":
  * copys 'ask_question' options
  * ||
  * "swayProposal_againstCounsel_Bribe_accept": "swayProposal_againstCounsel_acceptBribe"
  * "swayProposal_againstCounsel_Bribe_refuse": "ask_question"
  * ||
  * "swayProposal_againstCounsel_Bargain_accept": "swayProposal_againstCounsel_acceptBargain"
  * "swayProposal_againstCounsel_Bargain_refuse": "ask_question"
* "swayProposal_forPlayer":
  * copys 'ask_question' options
  * ||
  * "swayProposal_forPlayer_Bribe_accept": "swayProposal_forPlayer_acceptBribe"
  * "swayProposal_forPlayer_Bribe_refuse": "ask_question"
  * ||
  * "swayProposal_forPlayer_Bargain_accept": swayProposal_forPlayer_acceptBargain
  * "swayProposal_forPlayer_Bargain_refuse": "ask_question"

* "swayProposal_forCounsel_acceptBribe"
    * copys 'ask_question' options
* "swayProposal_forPlayer_acceptBribe"
    * copys 'ask_question' options
* "swayProposal_againstCounsel_acceptBribe"
    * copys 'ask_question' options
* "swayProposal_forCounsel_acceptBargain"
  * copys 'ask_question' options
* "swayProposal_forPlayer_acceptBargain"
  * copys 'ask_question' options
* "swayProposal_againstCounsel_acceptBargain"
  * copys 'ask_question' options

* "accept_ask_location"
    * "optionSet_LordLocationFinder": "option_LordLocation" : "LordLocation"
    * "option_nevermind_accept_ask_location" : option_nevermind_accept_ask_location

* "suggest_action"
  * "option_stop_follow_me" : "stop_follow_me"
  * "option_follow_me" : "follow_me"
  * "option_suggest_raid" : "ask_raid_location"
  * "option_suggest_patrol" : "ask_patrol_location"
  * "option_suggest_upgrade" : suggest_upgrade
  * "option_nevermind" : "greeting"
  * ||
  * copys "greetings" option
  
* "option_stop_follow_me"
  * copys "greetings" option
* "option_follow_me"
  * copys "greetings" option
* "ask_raid_location"
  * "optionSet_suggest_raid_location": "option_suggest_raid_target" : "raidLocation"
  * "option_suggest_raid_nevermind" : "greeting"
* "ask_patrol_location"
  * "optionSet_suggest_raid_location": "optionSet_suggest_patrol_location" : "option_suggest_patrol_target"
  * "option_nevermind_accept_ask_location" : "greeting"
* "option_suggest_upgrade"
  * copys "greetings" option
  
* "speak_privately"
  * "if the lord won't speak to you": copys 'greeting' options
  * "option_ask_worldview" : "worldview"
  * "option_ask_liege_opinion" : "liege_opinion"
  * "optionSet_ask_friend_preferences" : "option_ask_friend_preferences" : "ask_friend_fav_gift"
  * "speak_privately_exit" : "greeting"

* "worldview"
  * may run additional line: "learnedWorldview"
  * copys 'speak_privately' options

* "liege_opinion"
  * copys 'speak_privately' options
  * ||
  * "option_suggest_defect" : "consider_defect"
  * "option_liege_opinion_exit" : "greeting"

* "consider_defect"
  * "option_suggest_defection_calculating" : bargain_defect
  * "option_suggest_defection_upstanding" : justify_defect_upstanding
  * "option_suggest_defection_martial" : justify_defect_martial
  * "option_suggest_defection_quarrelsome" : justify_defect_quarrelsome
  * "option_suggest_defection_abort" : "greeting"
  
* "bargain_defect"
  * "option_bargain_credits_0" : justify_defect_calculating
  * "option_bargain_credits_1" : justify_defect_calculating
  * "option_bargain_rank_1" : justify_defect_calculating
  * "option_bargain_rank_2" : justify_defect_calculating
  * "option_bargain_defect_nothing" : justify_defect_calculating

* "justify_defect_calculating" (runs 6 lines before options)
  * "justify_defect_calculating_justification"
  * "justify_defect_calculating_legitimacy"
  * "justify_defect_calculating_factionPreference"
  * "justify_defect_calculating_lordPreference"
  * "justify_defect_calculating_argumentAgreement"
  * "justify_defect_calculating_final"
  * "option_justify_defect_calculating_confirm" : "justify_defect_calculating_answer"
  * "option_justify_defect_calculating_exit" : "greeting"
  
* "justify_defect_upstanding" (runs 6 lines before options)
  * "justify_defect_upstanding_justification"
  * "justify_defect_upstanding_legitimacy"
  * "justify_defect_upstanding_factionPreference"
  * "justify_defect_upstanding_lordPreference"
  * "justify_defect_upstanding_argumentAgreement"
  * "justify_defect_upstanding_final"
  * "option_justify_defect_upstanding_confirm" : "justify_defect_upstanding_answer"
  * "option_justify_defect_upstanding_exit" : "greeting"
  
* "justify_defect_martial" (runs 6 lines before options)
  * "justify_defect_martial_justification"
  * "justify_defect_martial_legitimacy"
  * "justify_defect_martial_factionPreference"
  * "justify_defect_martial_lordPreference"
  * "justify_defect_martial_argumentAgreement"
  * "justify_defect_martial_final"
  * "option_justify_defect_martial_confirm" : "justify_defect_martial_answer"
  * "option_justify_defect_martial_exit" : "greeting"
  
* "justify_defect_quarrelsome" (runs 6 lines before options)
  * "justify_defect_quarrelsome_justification"
  * "justify_defect_quarrelsome_legitimacy"
  * "justify_defect_quarrelsome_factionPreference"
  * "justify_defect_quarrelsome_lordPreference"
  * "justify_defect_quarrelsome_argumentAgreement"
  * "justify_defect_quarrelsome_final"
  * "option_justify_defect_quarrelsome_confirm" : "justify_defect_quarrelsome_answer"
  * "option_justify_defect_quarrelsome_exit" : "greeting"

* "ask_friend_fav_gift":
  * copys 'greeting' options


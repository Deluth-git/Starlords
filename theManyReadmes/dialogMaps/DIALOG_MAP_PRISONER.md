note: when a conversation calls optionSet_beginning it always runs the 'base' optionsSet that is ran in this dialog. (in this case, it runs 'greetings_prisoner's options.)
* "greetings_prisoner" (note: when a conversation calls "greetings" it always returns to the first dialog item. in this case, "greetings_prisoner")
    * "option_speak_privately" :        "speak_privately"
    * "option_release_prisoner" :       "prisoner_release"
    * "option_ransom_prisoner" :        "option_ransom_prisoner"
    * "option_leave_prisoner" :         "exitDialog"

* "speak_privately"
  * mimics [DIALOG_MAP_DEFAULT.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/dialogMaps/DIALOG_MAP_DEFAULT.md) "speak_privately".
* "prisoner_release"
  * "option_leave_prisoner_freed":          "exitDialog"
* "ransom_prisoner"
  * "option_leave_prisoner_freed":          "exitDialog"

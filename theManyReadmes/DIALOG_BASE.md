* the dialog.json is made out of 'dialogs'. each 'dialog' has a "priority", "rules", and "lines" inputs.
    * "priority": is a integer. it is the order that the game will attempt to grab a line from dialog (provided that the line exists in a dialogSet in this dialog). if set to 0, only a lords "dialogOverride" data can be used to grab this dialog. (with the exseption of the "default" dialog, wish is used as a backup.)
    * "rules": is a jsonObject. the 'rules' contents can be seen [DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md). all 'rules' must meet requirements to function.
    * "lines": is a list of dialog sets. each dialog set can have their own priority, lines, and rules.
* each "dialog" contains a list of "dialog sets". if the "dialogs" rules are true, the game will attempt to get the highest priority dialogset that is true withen it.
* "priority" is the priority of this dialog. should have a value of at least 0 the dialog with the highest priority that has all its "rule" reuqirements met will be used in any instance, unless the lord you are talking to has a at least one valid "dialogOverride"
* "rules" is each requirement that must be met for a set of dialog to be used by a given starlord. every condition must be met for this to happen. conditions are as follows:
* each line can be formed two ways.
    * this is the basic way, and the way most lines are formed. 
        * "lineID": "what you are going to say here =)"
    * advanced is more complicated. its a json object, that must include a "line" (to act as the normal lineID), but also additional json peramiters.
        * "line": String. this what you are going to say. 
        * "addons": this is code that can be ran when the line is ran. you can read about it here: [DIALOG_ADDONS.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_ADDONS.md)
        * "color" color override for this dialog line. not required. has 3 'preset' colors, but also the option for a custom color. cannot be used in any "tooltip_" line.
            * "RED"
            * "GREEN"
            * "YELLOW"
            * "ORANGE"
            * optional array is as follows: (with a default value of 0 for r,b,g, and a default value of 255 for a.)
            * "r"
            * "g"
            * "b"
            * "a"
        * "highlight":jsonObject {"highlight":String||JsonArray, "color":color object (see "color addon") } highligts a string or multible strings on the same line with the inputed color. does not work if this line is called as a option.
        * "options" is an jsonArray containing the lineID's of any option or option set that this line should run. please note, that most lines by default have a entry in the [default_dialog_options.json](https://github.com/Alaricdragon/Starlords_Temp/tree/master/data/lords/default_dialog_options.json) file
        * "show" is an jsonObject that contains the same functions as 'rules' ([DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md)), but instead of determining if a line can be ran, if any conditions in the object are false, and this line is selected, it will not be shown. for "option_"'s, if this line is selected and the 'hide' is true, it will not be ran.
        * "enable": (only effects 'option_' lines) is a jsonObject that contains the same functions as 'rules' ([DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md)), but instead of determining if a line can be ran. if any conditions in this object are false, and this option will be greyed out and unable to be used.
        * "optionData": is the line to a line that happens when you click this option. if called as a line, and no options are selected for this line, it will automaticly load the linked line.
        * "optionData": is the advanced option data form. this form runs through an list of items. (based on the inputed type.) then creates an option for each items that meets the rules requirements.
            * "type": String. if this is not set, the advanced option will use the type of 'targetMarket'.
                * "targetLord". this advanced option runs the 'rules' with the 'lord' as this lord. clicking on an option sets the dialogs target lord to this lord.
                * "targetMarket". this advanced option runs the 'market rules', clicking on an option sets the target market to this market.
            * "optionData": is the line to a line that happens when you click this option(s)
            * "rules": is the rules that are required for each 'type' in the game to be shown.
        * "hint": is the hover over hint that happens when you hover only an option. only works if this line is called as an option
        * "shortcut" : "shortcutLey" if this is set to one of the acsepted value, will add a hotkey to an option. only works for "option_" lines. possable options are:
            * "ESCAPE"
        * "customInserts": [DIALOG_INSERTS.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_INSERTS.md)
* for both basic and advanced lines, you can add 'markers' to strings, to display different data at different points in your dialog. please keep in mind that if the data you are requesting does not exist, the marker will have something like 'no one' or 'nothing' instead of the requested string.
    * there are a few different types of markers.
    * person markers (the markers here are about people)
        * there are a few different targets for markers. the targets are:
            * PLAYER
            * PLAYER_SPOUSE
            * LORD
            * LORD_SPOUSE
            * LORD_HOST
            * LORD_HOST_SPOUSE
            * WEDDING_TARGET
            * WEDDING_TARGET_SPOUSE
            * SECOND_LORD
            * SECOND_LORD_SPOUSE
        * markers of the following. (replace TARGET with diffrent target types at will)
            * "%TARGET_FACTION_NAME"
            * "%TARGET_STARTING_FACTION_NAME"
            * "%TARGET_NAME"
            * "%TARGET_NAME_FIRST"
            * "%TARGET_NAME_LAST"
            * "%TARGET_TITLE"
            * "%TARGET_GENDER_MAN_OR_WOMEN"
            * "%TARGET_GENDER_HE_OR_SHE"
            * "%TARGET_GENDER_HIM_OR_HER"
            * "%TARGET_GENDER_HIS_OR_HER"
            * "%TARGET_GENDER_HUSBAND_OR_WIFE"
            * "%TARGET_GENDER_SUIT_OR_DRESS"
            * "%TARGET_GENDER_SIR_OR_MAAM"
            * "%TARGET_GENDER_NAME"
            * "%TARGET_FLAGSHIP_HULLNAME" target flagship ship hull name (return "nothing" if the target has no flagship)
            * "%TARGET_FLAGSHIP_NAME" target flagship name (returns "nothing" if the target has no flagship)
            * "%TARGET_PROPOSAL_NAME" target proposal name (returns "nothing" if the target has no active proposal)
            * "%TARGET_FLEET_LOCATION" target fleet location (returns "nowere" if target fleet location cannot be found)
            * "%TARGET_LIEGE_NAME" the name of the leader of this faction. do to -reasons- this might often be null
            * "%TARGET_FACTION_RANK_TITLE0" the name of tier 0 lords title of the target faction
            * "%TARGET_FACTION_RANK_TITLE1" the name of tier 1 lords title of the target faction
            * "%TARGET_FACTION_RANK_TITLE2" the name of tier 2 lords title of the target faction
    * market markers (the markers here are about markets)
      * there is only one type of target right now:
        * TARGET_MARKET
      * markers of the following. (replace TARGET with diffrent target types at will)
        * "%TARGET_NAME"
        * "%TARGET_FACTION"
        * "%TARGET_SYSTEM"
        * "%TARGET_SIZE"
        * "%TARGET_STABILITY"
    * some lines will also use custom inputted data (this is hardcoded). in this case, they will use the '%c#' marker, with # being the order they are added to the line.
    * see 'customInserts' for custom insert data. (its farther up on this page)
you can view the full dialog map() here: 
    * as a note: keep in mind it is 100% possable to acsess any line at any time, so long as it is called. the dialog maps are put into different files for ease of use.
    * [DIALOG_MAP_DEFAULT.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/dialogMaps/DIALOG_MAP_DEFAULT.md)
    * [DIALOG_MAP_PRISONER.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/dialogMaps/DIALOG_MAP_PRISONER.md)
    
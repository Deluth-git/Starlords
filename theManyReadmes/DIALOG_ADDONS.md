"addons" are additional conditions and effects that you can have run at the moment this line is ran. some 'addons' also add a line of dialog to show what effects they had. any "option_" will only run addons after the option is selected. and "tooltip_" line cannot use "addons"
* modifiers:
  * "conditionalAddon": {"rules": [DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md) ,"addons": [DIALOG_ADDONS.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_ADDONS.md) } this addon only runs the addons from its 'addon' jsonObject only if its 'rules' jsonObject meets requirements.
* targets:
    * "targetLord" does nothing if: target lord is unset. runs the contained 'addon' json object, for the target lord instead of the interacting lord
* "customDialogAddon" jsonObject. were each item is "customDialogID": jsonObject. "customDialogID" should match a created 'DialogAddon_custom'.
  * to use this you must do the following:
  * 1) you must create a class that overrides DialogAddon_custom.
  * 2) override the 'apply' function. within this class, you can run whatever it is you want this addon to do.
    * please keep in mind, your class with have its 'json' variable set whenever it attempts to run. this is the jsonObject you fed your addon.
  * 3) create an instance of your class (new DialogAddon_custom(String id)). when created, your class will automatically be added to the list of possible custom addons. please keep in mind, only one item with a given ID can exist at a time. so I addive you use something like "modID_whatThisDoes" were 'modID' is your mods ID, and 'whatThisDoes' is... whatever you deside. this should prevent you from overriding someones else custom addon.
  * 4) in customDialogAddon{} add in your addon as a jsonObject. the items id should match your classes id, and the jsonObject you input will be the one your class will be given whenever it is ran.
* value changes: value changes can be written in the following ways:
  * Integer: the value that will be added / removed from a given stat. if min and max are both set, the value will be a random number between the 2.
  * dialogValue: a ([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)) json object. the return value of the dialog value is what the item must have to meet requirements.
  * {"min","max"}: were "min" and "max" can be a Integer, or a ([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)).
  * {"value": ('value change' object),"setValue":boolean}. if organized like there, were "value" would be the normal data (of any oboth type) that you want to cahnge/set your item to, and 'setValue' is set to true, instead of just adjusting the value, it will instead set the value to the inputed value.
  * "repChange" changes the relation between the lord and player
  * "creditsChange" changes the amount of credits the player has.
  * "exchangeCreditsWithLord" gives / takes credits from/to a lord from/to the player. a negative value gives, a positive value takes.
  * "changeLordCredits" changes the amount of credits the lord has.
  * "romanceChange" changes the stored romance value between the lord and player.
  * "changeCommoditysInPlayersFleet" holds a jsonObject of commoditys, were the value is how mush this commoidity changes. the value follows the same rules as any other value addon.
  * "adjustDialogData_Int" jsonObject. sets data that is stored in the dialog, and is deleted when the interaction ends.
    * each line is "dataID" : 'value changes', were "dataID" is the id of whatever data you want to save.
  * "adjustMemoryData_Int" jsonObject. sets data that is stored in starsectors memory, and is permanent. (unless a timer is set.)
    * each line is "dataID" : 'value changes', were "dataID" is the id of whatever data you want to save. each "dataID" must start with "$" to work.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
  * "adjustLordMemoryData_Int" jsonObject. sets memory key data, linked to the interacted starlord. memory is held in the save file, and will remain forever, or until the starlord dies.
    * each line is "dataID" : 'value changes', were "dataID" is the id of whatever data you want to save.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
* boolean data: sets a value to an boolean. can be written as the following:
  * Boolean. this just sets the value to this boolean.
  * [DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md) jsonobject. sets the boolean to the output of this rules object.
  * "setDialogData_Boolean": jsonObject sets data that is stored in the dialog, and is deleted when the interaction ends.
    * each line is "dataID" : 'boolean data', were "dataID" is the id of whatever data you want to save.
  * "setMemoryData_Boolean": jsonObject sets data that is stored in starsectors memory, and is permanent. (unless a timer is set.)
    * each line is "dataID" : 'boolean data', were "dataID" is the id of whatever data you want to save. each "dataID" must start with "$" to work.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
  * "setLordMemoryData_Boolean": jsonObject sets memory key data, linked to the interacted starlord. memory is held in the save file, and will remain forever, or until the starlord dies.
    * each line is "dataID" : 'boolean data', were "dataID" is the id of whatever data you want to save.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
* string data: sets a value to an inputted string (with all the inserts normal placed in dialog). can be written as the following:
  * String. this just sets the value to this string.
  * {"String":String, "customInserts": [DIALOG_INSERTS.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_INSERTS.md) }, were "String" is the string you want to use as a base
  * "setDialogData_String": jsonObject sets data that is stored in the dialog, and is deleted when the interaction ends.
    * each line is "dataID" : 'string data', were "dataID" is the id of whatever data you want to save.
  * "setMemoryData_String": jsonObject sets data that is stored in starsectors memory, and is permanent. (unless a timer is set.)
    * each line is "dataID" : 'string data', were "dataID" is the id of whatever data you want to save. each "dataID" must start with "$" to work.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
  * "setLordMemoryData_String": jsonObject sets memory key data, linked to the interacted starlord. memory is held in the save file, and will remain forever, or until the starlord dies.
    * each line is "dataID" : 'string data', were "dataID" is the id of whatever data you want to save.
    * each line can also be: "dataID": {"time":Integer/([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)), "data": (the same data as a normal dataID)}. in this instance, the data will only last for the inputted 'time' of days, before being removed.
  
* "additionalText": "lineID" adds an additional line of dialog, with the inputed name. also take a jsonArray as input, running each inputed line in sequence.
* "wedPlayerToLord": boolean. if set to true, marry's the lord and player. if set to false, and the player and lord are married, un-marry's the lord and player.
* "wedPlayerToWeddingTarget": boolean. if set to true, marry's the wedding target and player. if set to false, and the player and wedding target are married, un-marry's the wedding target and player.
* "startWedding": boolean. if set to true, sets the current feast the player is at to a wedding ceremony (or howeer that works). will do nothing if not at a feast.
* "dedicateTournamentVictoryToLord": boolean. if set to true, dedicates your tournament victory to the target lord. only runs if you are at a feast, and won the tournament.
* "startTournament": boolean. if set to true, starts a Tournament.
* "defectLordToFaction": String || {"factionID" String, "newRank":Intiger || dialogValue, "includeFiefs": Boolean}. causes the lord to defect to the target faction. can optionaly set the lords rank, or if they take there fiefs with them. default is rank 0, and will take fiefs with them. (although some factions cannot have fiefs taken in defection.)
  * if you want the lord to defect to the players current faction, set the base String, of factionID to "playerCurrFaction".
* "playSound": String || {"soundID":String, "pitch":Integer || dialogValue, "volume": Integer || dialogValue}. runs Global.getSoundPlayer().playUISound on the inputed data.
* "attemptToAddRandomQuest": boolean. if set to true, it will run ether the '' line, or the '' line. depending on if a quest can actually get gotten or not. please note: this is to be removed, and replaced with a more complicated quest system in the future.
* "preventAttacksOnPlayer" Integer. sets the number of days that the interacting lord will be unable to attack the player.
* "preventAttacksOnTargetLord" Integer. sets the number of days that the interacting lord will be unable to attack the target lord. (if one exsists)
* "releaseLord": boolean. if set to true, releases the starlord from captivity.
* "captureLord": boolean. if set to true, puts the starlord into the player's prison.
* "setHeldDate": boolean. sets if you have held a data this feast.
* "setProfessedAdmiration": boolean. sets if you have held professed admiration this feast.
* "setCourted" : boolean. sets whether you are courting this lord. setting this to true lets you do romance =)
* "setInPlayerFleet": boolean. sets whether this lord is in the player fleet, or in their own fleet.
* "setPledgedFor_CurrentProposal": boolean. sets whether the lord you are talking to has pledged support for the current proposal or not.
* "setPledgedAgainst_CurrentProposal": boolean. sets whether the lord you are talking to has pledged opposition for the current proposal or not.
* "setPledgedFor_PlayerProposal": boolean. sets whether the lord you are talking to has pledged support for the player proposal or not.
* "setPledgedAgainst_PlayerProposal": boolean. sets whether the lord you are talking to has pledged support for the player proposal or not.
* "setPlayerSupportForLordProposal": boolean. sets weather the player is currently supporting the lords proposal, or is opposed to the lords proposal.
* "setPlayerSupportForCurProposal": boolean. sets weather the player is currently supporting the current proposal or is opposed to the current proposal.
* "setSwayed": boolean. sets whether the lord you are talking to has been swayed or not. if so, they cannot be swayed until the next proposal.
* "setPersonalityKnown": boolean. sets weather the player knows this lords personality.
* "setLordRank": Integer || dialogValue. sets the lords rank.
* "setPlayerRank": Integer || dialogValue. sets the players rank.
* "setFeastInteracted": boolean. sets weather or not you have interacted at anyone in this feast, when this lord was here. does nothing if the lord is not at a feast.
* "AICommand" : String. this commands that lord to change there AI action. you can command a lord to do the following:
  *  "END_CURRENT_ACTIONS" this lets the lord chose a new action to take. all on there own.
  *  "FOLLOW_PLAYER_FLEET" this make the lord follow the player fleet.
  *  "FOLLOW_TARGET_LORD" this makes the lord follow the target lord (if one exists)
  *  "RAID_TARGET_MARKET": this make the lord raid the target market (if one exists)
  *  "UPGRADE" this make the lord go and upgrade there fleet
  *  "PATROL_TARGET_MARKET" this makes the lord go and patrol the target market (if one exists)
  *  --NOT YET ADDED--"ORGANIZE_CAMPAIGN" this makes the lord organize a camping. (provided they have the power to do so, and one is not already active.)
  *  --NOT YET ADDED--"JOIN_CAMPAIGN" this makes the lord join the active campaign. (if one exists)
* "applyAddonsToMultiple": JSonObject. this applys addons to multible targets.
  * "target": String. what this set of addons will target. will apply 'rules' to every instance of the target type in the game. possable options are:
    * "LORDS". targets all lords in the game.
    * "MARKETS". targets all markets in the game
  * "rules": [DIALOG_RULES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_RULES.md). all rules in here must be meet to run contained addons.
  * "addons": [DIALOG_ADDONS.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_ADDONS.md). runs the contained addons on all valid lords.

  

* "setLordTags": jsonObject. were each item is 'tagName' : boolean. set to true to add a tag, false to remove it. tags will remain on a starlord until removed.
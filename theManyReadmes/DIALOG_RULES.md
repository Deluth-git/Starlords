this is the 'dialog rules' json object. within, I will cover the 3 different 'rule types' (lord, target lord, and target market). please keep in mind: the different rule types are NOT different json objects. they are all held under the 'rules' object. however, other than the 'lord' rules, the rules can only be called apon on certen conditions.
* 'custom rules'
  * "customDialogRule" jsonObject. were each item is "customDialogID": jsonObject. "customDialogID" should match a created 'DialogRule_custom'.
    * to use this you must do the following:
    * 1) you must create a class that overrides DialogRule_custom. when created
    * 2) override the 'condition' function. within this class, you can run whatever it is you want this addon to do.
      * please keep in mind, your class with have its 'json' variable set whenever it attempts to run. this is the jsonObject you fed your addon.
    * 3) create an instance of your class (new DialogRule_custom(String id)). when created, your class will automatically be added to the list of possible custom rules. please keep in mind, only one item with a given ID can exist at a time. so I addive you use something like "modID_whatThisDoes" were 'modID' is your mods ID, and 'whatThisDoes' is... whatever you deside. this should prevent you from overriding someones else custom addon.
    * 4) in customDialogRule{} add in your addon as a jsonObject. the items id should match your classes id, and the jsonObject you input will be the one your class will be given whenever it is ran.
* 'lord' rules
  * target rules: all 'target' rules contain the 'rules' json object. and instead of runing the rules onto the interacting lord, they run the rules for whatever lord they interact with instead. --will return false if whatever lord the rule wants cannot be found--
      * "SECOND_LORD" : 'target lord'.
      * "SECOND_LORD_SPOUSE" : 'target lords' spouse.
      * "PLAYER_SPOUSE" : playes spouse
      * "LORD_SPOUSE" : interacting lords spouse
      * "LORD_HOST" : whoever is hosting the feast the lord is currently at.
      * "LORD_HOST_SPOUSE" : the spouse of whoever is hosting the fleet the lord is currently at
      * "WEDDING_TARGET" : whoever is getting married at this feast
      * "WEDDING_TARGET_SPOUSE" : whoever is getting married at this feasts spouse
  * condition rules: are things that can contain rules objects but run diffrent operaters on said objected.
      * "or" contains a jsonArray, were each item in the array is a 'rules' jsonObject. if any of the contained 'rules' jsonObjects have all there rules return true, this condition returns true.
  * value rules. value conditions can contain the following data:
      * Integer: the value that the item must have to meet requirements.
      * dialogValue: a dialog value json object ([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)). the return value of the dialog value is what the item must have to meet requirements.
      * {"min","max"}: were "min" and "max" can be a Integer, or a ([DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md)). the item must be between min and max to meet requirements. min and max have a default value of -infinity and +infinity respectively.
      * "relationWithPlayer" is the relationship that this lord has with the player.
      * "playerWealth": is the number of credits the player has.
      * "lordWealth": is the number of credits the lord has.
      * "playerLevel": is the level the player is.
      * "lordLevel": is the level the lord is.
      * "playerRank": is the rank of the player in there faction.
      * "lordRank": is the rank of the lor din there faction.
      * "lordsCourted": is the number of lords the player has professed admiration to.
      * "playerLordRomanceAction":  is the number of romantic actions the player and lord have had together <3.
      * "lordsInFeast": will return false if: not at a feast. is the number of lords at the current feast.
      * "optionOfCurrProposal": will return false if: the lords faction is not currently voting on a proposal. is the opinion of the current proposal the lord has.
      * "optionOfPlayerProposal": will return false if: the player has no active proposal. is the opinion of the player proposal the lord has.
      * "lordProposalSupporters": will return false if: lord does not have a proposal active. is the number of supports the lords proposal has.
      * "lordProposalOpposers": always returns false if: lord does not have a proposal active. is the number of opposers the lords proposal has.
      * "playerProposalSupporters": always returns false if: player does not have a proposal active. is the number of supports the players proposal has.
      * "playerProposalOpposers": always returns false if: player does not have a proposal active. is the number of opposers the players proposal has.
      * "curProposalSupporters": always returns false if: there is no active proposal in the lords faction counsel. is the number of supports the lords factions current proposal has
      * "curProposalOpposers": always returns false if: there is no active proposal in the lords faction counsel. is the number of supports the lord factions current proposal has
      * "lordLoyalty": is the relationship this lord has with there faction.
      * "playerHasCommodity": is a jsonObject containing a set of commodityID's : value condition. the player must meet the requirements of every commodity.
      * "validLordNumbers": this is a jsonArray with 3 parts: "min", "max", "rules". what this does is it looks at all starlords in the game, and the number of lords that meet all "rule" requirements must be between the "min" and "max" values.
      * "baseValue": {"value":intiger || dialogValue, "min":intiger || dialogValue, "max":intiger || dialogValue}. this does not have a base value, instead has an inputted value. useful when compairing dialogValues without having to set them into memory first.
  * boolean rules. each data here must be set to true or false. if set to true, the item must also be true. if set to false, the item must also be false.
      * "isMarriedToPlayer": if the lord is married to the player
      * "isMarried": if the lord is married to anyone
      * "isPlayerMarried": if the player is married to anyone.
      * "willEngage":if the lord is willing to attack the player
      * "hostileFaction": if the faction the lord is part of is hostile to the faction the player is a part of.
      * "isAtFeast": if the lord is at a feast.
      * "isHostingFeast": will return false if the lord is not at a feast. is if the lord is hosting the feast they are at.
      * "playerSubject" if the lord is part of the player faction.
      * "playerFactionMarital" will return false if the player and lord are not in the same faction. is if the player is the faction marital or not.
      * "lordFactionMarital" will return false if the player and lord are not in the same faction. is if the lord is the faction marital or not.
      * "lordAndPlayerSameFaction" if the lord and player are start of the same faction.
      * "isLordCourtedByPlayer": is if the player is corting the lord.
      * "availableTournament" : will return false if not at a feast. is if the feasts tournament has not yet happened.
      * "playerTournamentVictory": will return false if not at a feast. is if the player has won the tournament at the current feast.
      * "lordTournamentVictory": will return false if not at a feast. is if the player has won the tournament at the current feast.
      * "playerTournamentVictoryDedicated": will return false if: not at a feast, or the player has not won the tournament there. is if the player has already dedicated there victory at the tournament.
      * "tournamentDedicatedToLord": will return false if not at a feast. is if the lord is the one this tournament is dedicated to.
      * "feastIsHostingWedding": will return false if not at a feast. is if this feast is hosting a wedding.
      * "isWeddingTarget": will reutrn false if not at a feast, or the feast is not hosting a wedding. is if the lord is the wedding target or not.
      * "firstMeeting": is if this is your first time talking to this starlord or not.
      * "hasProfessedAdmirationThisFeast": will return false if: not at a feast. is if the player has professed admiration this feast.
      * "hasHeldDateThisFeast": will return false if: not at a feast. is if the player has been on a date yet this feast.
      * "lordPledgedSupport_forActiveProposal": will return false if the lords faction is not voting on anything. is if the lord has pledged support for active proposal or not.
      * "lordPledgedSupport_againstActiveProposal": will return false if the lords faction is not voting on anything. is if the lord has pledged opposition for the active proposal or not.
      * "lordPledgedSupport_forPlayerProposal":  will return false if the player has no proposal. is if the lord has pledged support for the players proposal or not.
      * "lordPledgedSupport_againstPlayerProposal": will return false if the has no proposal. is if the lord has pledged against the players proposal or not.
      * "playerProposalExists": if the player has a active proposal.
      * "lordProposalExists": if the lord has a active proposal.
      * "lordFactionHasActiveConsole": if the lords faction is voting on a proposal or not.
      * "playerFactionHasActiveConsole": if the players faction is voting on a proposal or not.
      * "lordActingInPlayerFleet": if the lord is in the player fleet or not.
      * "isSwayed": if the player has attempted to sway the lord to support / oppose a proposal.
      * "lordProposalPlayerSupports": if the lord is supporting the players proposal or not
      * "playerProposalLordSupports": if the lord is supporting the players proposal or not
      * "curProposalPlayerSupports": if the player is supporting the currently active proposal or not
      * "curProposalLordSupports": if the lord is supporting the currently active proposal or not
      * "lordFleetIsAlive": if the lords fleet is alive or not.
      * "lordHasLiege": if the lord has a liege or not (some factions don't have lieges.)
      * "playerHasLiege": if the lord has a liege or not (some factions don't have lieges.)
      * "isPersonalityKnown": if the player knows the lords personality yet.
      * "hasInteractedThisFeast": if the player has interacted to this feast well this lord was here.
      * "isValidMarket": if this market is a valid market, as desided by starlords rules.
  * "compareStrings": {"a":JSONObject, "b":JSONObject}. compares to strings together. if they are the same, returns true. "a" and "b" contain the following data:
      "type": String. this is what type of string you are compairing. types are as follows:
        "MEMORY" gets a string from memory (with a ID of "data")
        "DIALOG" gets a string from dialog data (with a ID of "data")
        "LORD" gets a string from lord memory (with a ID of "data")
        "FAV_ITEM": gets the fav item of the lord. ignores "data".
        "STATIC" simple compares the string inputted as data.
      "data": String
  * whitelist / blacklist rules. each rule here contains a jsonObject, were a item ID is what its looking at, and its boolean value is wether or not the item is whitelisted or blacklisted. all blacklisted items must not be present, and at least 1 (if present) whitelisted item must be present
      * "startingFaction" is the starting faction the lord has.
      * "currentFaction" is the current faction the lord is in.
      * "lordsFavItem": is the lord fav item
      * "LordTags": is the tags that need to be attached to the lord person
      * "dialogType": this is what 'dialog' you are current in. possible values are:
        * "default": this is the dialog that normally opens when you meet a starlord in the wild.
        * "prisoner": this is the dialog that opens when the starlord is imprisoned by you.
  * "lordPersonality": is the lord personality type this rule requires to meet requirements. set 'true' to all lord personality types you want to be allowed to meet requirements.
      * "Upstanding"
      * "Martial"
      * "Calculating"
      * "Quarrelsome"
  * "lordBattlerPersonality": is the lord personality type this rule requires to meet requirements. set 'true' to all lord personality types you want to be allowed to meet requirements.
      * "Reckless"
      * "Aggressive"
      * "Steady"
      * "Cautious"
      * "Timid"
  * "currentAction": is the current action that the lord is taking. set too false to blacklist, and true to whitelist. the lord must be preforming the whitelisted actions, and must not be preforming the blacklisted actions.
      * "IS_UPGRADING": if the lord is currently attempting to upgrade
      * "IS_ON_PATROL": if the lord is currently on patrol.
      * "IS_RAIDING": if the lord is currently raiding.
      * "IS_FOLLOWING_PLAYER": if the lord is following the player
      * "IS_FOLLOWING":  if the lord is following someone
      * "IS_ON_CAMPAIGN": if the lord is on campaign
      * "IS_ORGANIZING_CAMPAIGN": if the lord is organizing a campaign
      * "IS_FEASTING": if the lord is enjoying a feast.
      * "IS_ORGANIZING_FEAST": if the lord is organizing a feast.
      * "IS_COLLECTING_TAXES": if the lord is collecting taxes
      * "IS_DEFENDING": if the lord is defending a attack
      * "IS_IMPRISONED": if the lord is imprisoned somewhere
      * "IS_RESPAWNING": if the lord is respawning
      * "IS_ON_VENTURE": is currently preforming trade actions
  * "random": returns true if a random number (called 'range') is less then the base value.
      * "range": Integer || dialogValue. this is the range the random number will have
      * "value": Integer || dialogValue. this is the base value
  * "getDialogData": jsonObject gets a list of 'dialog data'. for each different type of data, the requirements are different.
      * "dataID" : jsonObject. is the string data required to meet requirements. set to true for strings you require this memory to be, and false for strings this memory must be. To meet requirements, the string must match all of the 'true' strings, and not match any of the 'false' strings. if a string of the dataID does not exist, treats the string as though it is ""
      * "dataID" : jsonObject. {"min","max"} is the value rules required to meet requirements. (can use [DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md) for min and max values)
      * "dataID" : boolean.  is the boolean value of this data required to meet requirements. if set to true, the boolean must also be true. if set to false, the boolean data must also be false. if a boolean of the dataID does not exist, treats the boolean as though it is false.
  * "getMemoryData": jsonObject gets a list of 'game memory data;. for each different type of data, the requirements are different. (keep in mind: this can get any data in starsectors memory. data ID must start with '$' or it might break something)
      * "dataID" : jsonObject. is the string data required to meet requirements. set to true for strings you require this memory to be, and false for strings this memory must be. To meet requirements, the string must match all of the 'true' strings, and not match any of the 'false' strings. if a string of the dataID does not exist, treats the string as though it is ""
      * "dataID" : jsonObject. {"min","max"} is the value rules required to meet requirements. (can use [DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md) for min and max values)
      * "dataID" : boolean.  is the boolean value of this data required to meet requirements. if set to true, the boolean must also be true. if set to false, the boolean data must also be false. if a boolean of the dataID does not exist, treats the boolean as though it is false.
  * "getLordMemoryData": jsonObject gets a list of 'lord memory data". for each different type of data, the requirements are different.
      * "dataID" : jsonObject. is the string data required to meet requirements. set to true for strings you require this memory to be, and false for strings this memory must be. To meet requirements, the string must match all of the 'true' strings, and not match any of the 'false' strings. if a string of the dataID does not exist, treats the string as though it is ""
      * "dataID" : jsonObject. {"min","max"} is the value rules required to meet requirements. (can use [DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md) for min and max values)
      * "dataID" : boolean.  is the boolean value of this data required to meet requirements. if set to true, the boolean must also be true. if set to false, the boolean data must also be false. if a boolean of the dataID does not exist, treats the boolean as though it is false.
* 'target lord' rules
  * the following options only work if used in an option called by advanced option data with a type of 'targetLord' (and anything in dialog afterwords), or if used in the "validLordNumbers" rule.
    * "relationsBetweenLords": is the relationship range that this lord must have with the target lord to meet requirements. set between a "min" and "max" value. range must be between -100 and 100.
    * "lordAndTargetSameFaction": if set to false, the lord and target must not be part of the same faction to meet requirements. if set to ture, the lord and target must be part of the same faction to meet requirements.
    * "isInteractingLord": if set to false, the lord and target must not be the same to meet requirements. if set to true, the lord and target must be the same to meet requirements.
    * "LordAndTargetAtSameFeast": if set to false, the lord and target must not be at the same feast to meet requirements. if set to true, the lord and target must be at the same feast to meet requirements. always returns false if ether lord is not at a feast.
* 'target market' rules
  * the following options only work if used in an option called by advanced option data with a type of 'targetMarket'
  * value rules. value conditions can contain the following data:
    * Integer: the value that the item must have to meet requirements.
    * dialogValue: a [DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md) json object. the return value of the dialog value is what the item must have to meet requirements.
    * {"min","max"}: were "min" and "max" can be a Integer, or a dialogValue. the item must be between min and max to meet requirements. min and max have a default value of -infinity and +infinity respectively.
    * "marketFactionRelationToLordsFaction": the relation of whatever faction controls the target market to the lord
    * "marketFactionRelationToPlayersFaction": the relation of whatever faction controls the target market to the player 
    * "marketFactionRelationToTargetLordsFaction": the relation of whatever faction controls the target market to the target lord
    * "marketSize": the size of the market
    * "marketStability": the stability of the market
  * boolean rules. each data here must be set to true or false. if set to true, the item must also be true. if set to false, the item must also be false.
    * "marketPlayerFaction": if the market is part of the players faction (or the faction the player is commissioned by)
    * "marketLordFaction": if the market si part of the faction the lord belongs to
    * "marketTargetLordFaction": if the market is part of the faction the target lord belongs to
    * "marketHasFiefOwner": if a starlord, or the player, owns the target market (as a fief)
    * "marketFiefBelongsToPlayer": if the target market belongs to the player (as a fief)
    * "marketFiefBelongsToLord": if the target market belongs to the lord (as a fief)
    * "marketFiefBelongsToTargetLord": if the target market belongs to the target lord (as a fief)
    * "marketIsValidTarget": if the market can be attacked under normal circumstances

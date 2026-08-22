please note: this file references 'private factions' a lot. a pirate faction is determined by the custom:{"pirateBehavior"} value in a given data\world\factions file.
faction templates contain a large amount of data to fully customize your faction. the data you can have is as follows.
* "leaderID": String. this is the person ID of the leader of this faction. default value is null.
* "rank_0": String. this is what the common starlords rank string. default value is __
* "rank_1": String. this is what the promoted starlords rank string. default value is __
* "rank_2": String. this is what the highest rank starlords rank string. default value is __
* 
* "canBeAttacked": boolean. this determines if a faction can be attacked in any way at all. default is false if this faction is a pirate faction.
* "canAttack": boolean. this determines if a faction can attack others in any way at all. default is true.
* "canInvade": boolean. this determines if this faction can invade / be invaded or not. default value is determined by nexerlin has decided for this faction.
* "canSatBomb": boolean. this determines if a faction can preform sat bombardments or not. default value copys canInvade. if nexerlin is not enabled, is false if the faction is a 'pirate' faction.
* "canBeSatBomb": boolean. this determines if a faction can be sat bombardments or not. default value copys canInvade. if nexerlin is not enabled, is false if the faction is a 'pirate' faction.
* "canBeRaided": boolean. this determines if this faction can be raided. default is true.
* "canRaid": boolean. this determines if this faction can raid. default value is true.
* "canTacticalBombed": boolean. this determines if a faction can preform tatical bombarded. default value is true.
* "canBeTacticalBomb": boolean. this determines if a faction can be tatical bombarded. default value is true.
* "canHaveCampaigns": boolean. this determins if a faction can have campains. is nexerlin is enabled, default is determined by nexerlin if nexerlin is not enabled, is false if this faction is a pirate faction.
*
* "canStarlordsJoin": boolean. this determines if this faction can have starlords join or leave it. default value is true.
* 
* "canPreformDiplomacy": boolean. this determines if this faction can do things like declare war and peace, as well as have others declare war and peace. default is false if this faction is a 'pirate' faction. if nexerlin is installed, default value is determined by nexerlin. 
* "canPreformPolicy": boolean. this determines if this faction can have policy's be voted on. default is false if this faction is a 'pirate' faction. if nexerlin is installed, default value is determined by nexerlin.
* "canHoldFeasts": boolean. this determines if this faction can hold feasts or not. default value is true.
* 
* "canTrade": boolean. this determines if starlords in this faction can trade with others. default value is true
* "canBeTradedWith": boolean. this determines if starlords in this faction can be traded with by others. default value is true.
*
* "canGivesFiefs": boolean. this determines if lords in this faction can get fiefs. default value is true.
* "canLordsTakeFiefsWithDefection": boolean. this determins if lords can take fiefs with them when they defect (or if when a lord defects to this faction if they take feifs with them). default value copys 'canInvade'. if nexerlin is not installed, default is false if this faction is a 'pirate' faction.
* "maxNumberOfFiefs": int. this determins the max number of fiefs a starlord can have. default value is infinity, unless the faction is a pirate faction, then its 1.
*
* "fiefIncomeMulti": double. this determines the income multi of fiefs for lords in this faction. default is 1, unless the faction is a pirate faction, then its 0.5
* "combatIncomeMulti": double. this determines the income multi of destroying ships. default is 1, unless the faction is a pirate faction, then its 2
* "tradeIncomeMulti": double. this determines the income multi of trading. default is 1, unless the faction is a pirate faction, then its _
* "commissionIncomeMulti": double. this determines the income multi passive income a lord gets. default is 1, unless the faction is a pirate faction, then its 2
*
* "lordFleetUpkeepCostMulti": double. this determines the cost multi of fleet upkeep for this lord. default is 1.
*
* "lordRepGainPerWin": double. this determines the rep gain a lord gets for there faction, when they win a battle. base rep gain is killFP / totalFP rounded down. default is 0, unless the faction is a pirate faction, then its 1.
# Star Lords

Star Lords attempts to bring the world of [Starsector](https://fractalsoftworks.com/) to life with many character-centric features. No longer will your only allies be nameless faction fleets who you'll meet once and never again. The core mechanic is the addition of dozens of Lords to the game, each with their own personality, fleet composition, backstory, and agendas.

![](example/starlords_readme1.PNG)

Lords will roam the map alongside the player-- raiding, trading, feasting, expanding, and more. Lords follow their own economic system, requiring them to physically visit their fiefs to levy taxes and purchase new ships. They operate persistently and without requiring any interference from the player. When the Marshal raises the banners, the Lords will muster to war in organized interplanetary campaigns of unprecedented scale.

![](example/starlords_readme5.PNG)

A Lord's behavior is heavily dependent on their personality and interpersonal relations with other Lords. Inter-lord relations are fully modeled, culminating in a brand new political system where Lords will politick, scheme, form alliances, and backstab while jockeying for wealth and influence within the realm.

![](example/starlords_readme3.PNG)

Curry favor with Lords by completing their quests, fighting alongside them in battle, or supporting their political agenda. Form a core of close allies and leverage their support to seize de-facto power in your faction's political system. Or convince them to join your own faction and support your claim to unite the sector.

Star Lords is heavily inspired by campaign mechanics of Mount and Blade. The goal is to eventually add all major campaign mechanics from Warband. This mod aims to have minimal side-effects on the game. There are no changes to in-battle gameplay, and no direct effects on campaign balance aside from the actions of the Lords themselves.

### Disclaimer
This mod is currently in early development. Expect plenty of bugs, crashes, questionable balancing, etc. Please report any crash logs or balance feedback to the forums. Thank you for your cooperation.

### Installation
Download the latest release [here](https://github.com/Deluth-git/Starlords/releases) and unzip in your Starsector mod folder.

This mod may be added to existing savegames, but may not be removed after being added. Make sure to back up your save game first in case of unexpected issues.

### Full Feature List
* Adds __48 unique Lords__ spread among all the base major factions.
* Adds fief system, where each market is a fiefdom which can be owned by a Lord. 
* Custom __Economic system__ for Lords
  * Lords collect taxes from their own fiefs and participate in business ventures in friendly markets. 
  * Income is used to expand their fleet, hire marines, and maintain existing ships.
* Custom fleet constructor for Lords, which allows each Lord to have their own __distinctive fleet composition__.
* Lords are __active on the map__, collecting income and waging war.
  * Lord actions are heavily dependent on Lord's personality and relations with other Lords
  * Lord's officer corps slowly level up from fighting in battles
* __Player-Lord Interactions__- If a Lord trusts you sufficiently, they may follow the player's military orders, offer quests, participate in scheming, and share sensitive intel about their operations.
* __Feast system__- Take a reprieve from braving the sector and join the lords of the realm in a night of feasting and revelry. Feasts are an excellent chance to meet all the lords and build rapport with them.
* __Campaign system__- Campaigns may be started by a faction's appointed marshal. All lords of the realm will gather to launch a grand invasion into enemy territory or defend against an enemy's invasion. If Nexerelin is enabled, lords may also use the Nexerelin ground combat system to capture markets.
* __Defection system__- Lords who are dissatisfied with their faction may defect to another. Or you can speed along the process and persuade or bribe Lords to join your own faction as your subordinates.
* __Prisoner system__- Lords may be captured in battle and either ransomed for credits or released for future goodwill.
* __Political system__- The cornerstone of all lord relations, the new political system is where all Lords of the realm gather to propose and vote on legislation. Appoint new marshals, squabble for newly conquered fiefs, conduct foreign policy, and more, as long as you can control the political situation.



### Major Planned Features
Most of these are inspired by Mount & Blade.
* Continued Lord AI improvement and optimization.
* Political marriage/courtship system for forming marriage alliances.
* Expanded subterfuge system involving scheming with friendly lords to increase your status in the realm or discredit mutual rivals.
* "Freelancer" system for enlisting in a Lord's army and fighting as a common pilot.
* Flesh out feast system, with feast tournaments as friendly competition with fellow Lords
* Better integration with Nexerelin invasions and base game crises.
*  Custom questlines for certain lords, e.g. allowing AI-sympathizing lords to field [REDACTED] fleets.


### Dependencies/Compatibility
This mod has no dependencies, though it's recommended to play with [Nexerelin](https://github.com/Histidine91/Nexerelin/tree/master) for planet capture mechanics. This mod should work with faction/ship/weapon mods which don't impact base campaign mechanics. Any kind of mod that only affects in-battle gameplay is also fine. All specific compatibilities are not yet tested.


### Adding your own Lords
If you're a modder or just want to put your own characters into the game, all you have to do is add another entry to the [lords.json](https://github.com/Deluth-git/Starlords/blob/master/data/lords/lords.json) file. A few notes:
* "faction" should be a valid [faction id](https://fractalsoftworks.com/starfarer.api/constant-values.html#com.fs.starfarer.api.impl.campaign.ids.Factions.DIKTAT)
* "fief" should be a valid market id or null
* "ranking" is the lord's rank, which affects their political weight and base income. It should be between 0 and 2, where 2 is highest.
* Flagship and ship preferences must contain valid ship variant ids. You can find these under the `/data/variants` folder of `starsector-core` or any mod directories.
* Faction and fief will be automatically converted to lower case. Ship variants are case-sensitive.

After that, your lord should be created automatically upon starting a new game.

### Credits
Starsector team for developing the game\
[Nexerelin](https://github.com/Histidine91/Nexerelin/tree/master) team's codebase for providing excellent references to many obscure parts of the Starsector API \
Interestio for Lord [portraits](https://fractalsoftworks.com/forum/index.php?topic=17066.0)

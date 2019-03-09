# U4Rando
Ultima 4 NES Randomizer

Download all the files and put them into the same directory.  If you have a .cfg file from older versions delete it.  This requires Java 1.6 or later and the Ultima 4 NES Rom.
See the FAQ for usage.

New in ver 1.31:
- The source of the Neagate bug has been found and eliminated
- The source of the forest walk bug has been found and eliminated
- You can now randomize all of your virtues to distinct, random values (rather than have all of them at the same value)
- Updated UI suggests Beginner, Intermediate, Advanced, and Finalist flags (much like Zelda 2 randomizer)

New in ver 1.3:
- You can now shorten the Abyss
- You can now randomize where shops are found
- You can now take advantage of a modified View spell, which lists the number of chests and stones on each floor of the dungeon you are currently in
- Fixed the Basil and Calumney text bugs
- Stone room stair exits now work, and I can no longer reproduce the Negate bug
- Improved map generation significantly reduces invisible mountains and makes the map a little less square

New in ver 1.2:
- You can set the balloon to be next to Castle Britannia (instead of outside Hythloth)
- Calumney of Yew gives the proper location for Manroot and Basil of Moonglow gives you the proper location for Fungus
- You can now randomize enemy attacks
- You can now randomly make enemies tougher (25, 50, or 75% of the game's enemies have thier HP, attack and defense tripled and give 5x the EXP)
- You can now randomize the spells given to you by spell-teaching villagers
- The Black Stone of Humility can be picked up at anytime; no need to wait for new moons

New in ver 1.1:
- You can now start as Avatar
- You can now add a sea border to generated maps (to make them smaller)
- You can now include the chests at Empath Abbey and the Lyceaum for item shuffling
- You can now randomize initially known spells, as well as specifically exclude Heal and Blink
- You can now add gold to each gold chest
- Also fixed a UI bug.
- There is a known UI inconsistency where the flag order in the UI and the flag order in the generated ROM will be different.  This may be fixed in a future version.

Known bugs and issues:
- Do not pick up the Robe if your inventory is full, or the game will get very angry with you
- A bug sometimes happens with visibility zones around Castle Britannia
- Casting the Wind spell with a short spell list causes a known visual artifact

Note: Some interest was expressed in the source code for this Rando.  I have uploaded the lastest java file.  I use launch4j to build the downloadable Windows executable; feel free to look at/play with/suggest bug fixes.

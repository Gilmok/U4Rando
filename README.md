# U4Rando
Ultima 4 NES Randomizer

Download all the files and put them into the same directory.  If you have a .cfg file from older versions delete it.  This requires Java 1.6 or later and the Ultima 4 NES Rom.
See the FAQ for usage.

New in ver 1.51 (bugfix edition):
  - The game no longer resets if you say "no" to a stone guard, useful runes no longer bugs the game out if you set random virtues, randomizing encounter rooms will actually produce a ROM, the View spell will properly tell you how many stones are in each dungeon if dungeons are not randomized, and the View spell will simply tell you how many stone rooms are in a dungeon if stone locations are randomized 

New in ver 1.5:
  - Useful Runes: Puts a stair case in each shrine to the corresponding stone in their respective dungeons
  - Encounter room flags: You can randomize the encounter rooms and/or the encounter room enemies
  - You can now turn off random encounters on the overworld.

New in ver 1.4:
  - You can now shrink the size of the overworld map from 256x256 to 128x128.
  - You can now include the dungeon stones in item randomization.

New in ver 1.31:
- The source of the Negate bug has been found and eliminated
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

Known bugs and issues:
- Do not pick up the Robe if your inventory is full, or the game will get very angry with you
- A bug sometimes happens with visibility zones around Castle Britannia
- Casting the Wind spell with a short spell list causes a known visual artifact
- Shrinking the map causes a known "fog" effect at the edge of the map that causes your sprite to disappear (if you are the ship or walking around; this bug does not occur for the balloon).

Note: Some interest was expressed in the source code for this Rando.  I have uploaded the lastest java file.  I use launch4j to build the downloadable Windows executable; feel free to look at/play with/suggest bug fixes.

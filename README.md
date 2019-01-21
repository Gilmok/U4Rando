# U4Rando
Ultima 4 NES Randomizer

Download all the files and put them into the same directory.  If you have a .cfg file from older versions delete it.  This requires Java 1.6 or later and the Ultima 4 NES Rom.
See the FAQ for usage.

New in ver 1.1:
- You can now start as Avatar
- You can now add a sea border to generated maps (to make them smaller)
- You can now include the chests at Empath Abbey and the Lyceaum for item shuffling
- You can now randomize initially known spells, as well as specifically exclude Heal and Blink
- You can now add gold to each gold chest
- Also fixed a UI bug.
- There is a known UI inconsistency where the flag order in the UI and the flag order in the generated ROM will be different.  This may be fixed in a future version.

Recently discovered bugs:
- Some flag combinations really mess up randomized dungeons
- Randomizing spells resets the game once you try to create a new file
- There is a chance to lose some randomly placed items and possibly soft-lock your seed

Note: Some interest was expressed in the source code for this Rando.  I have uploaded the lastest java file.  I use launch4j to build the downloadable Windows executable; feel free to look at/play with/suggest bug fixes.

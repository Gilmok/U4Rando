package u4rando;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class NESRom
{
	byte[] romData;
	String filename;
	
	NESRom(String fn) throws Exception
	{
		filename = fn;
		romData = loadData(fn);
		setupInventoryList();
	}
	
	public byte[] loadData(String fname) throws Exception
	{
		File file = new File(fname);
	    byte[] dest = new byte[(int) file.length()];
	    DataInputStream dis;
		//try 
		//{
			dis = new DataInputStream(new FileInputStream(file));
			dis.readFully(dest);
			dis.close();
		/*} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			System.out.println("Error: could not read nes file " + fname);
			e.printStackTrace();
		}*/
		return dest;
	}
	
	public void changeMap(byte[] newMap)
	{
		//byte[][] rVal = new byte[8][];
		int wpos = 16400;
		int rpos = 0;
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 8192; j++)
			{
				//0x4010->0x600f, 0x8010->0xa00f, etc.
				romData[wpos + j] = newMap[rpos];
				rpos++;
			}
			wpos += 16384;
			//rVal[i] = getData(16400 + 16384 * i, 24591 + 16384 * i);
		}
		//return rVal;
	}
	
	public void dumpRom(String fname)
	{
		FileOutputStream fos;
		try 
		{
			fos = new FileOutputStream(fname);
			fos.write(romData);
			fos.close();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		  
	}
	
	public NESRom randomize(String flags, String outFileName) throws Exception
	{
		OpeningScreen os = new OpeningScreen(this);
		os.replaceOpenScreen(this);
		UltCharacter ch = new UltCharacter();
		boolean randomizeMap = false;
		boolean randomizeMoongates = false;
		boolean britBalloon = false;
		boolean changeSpellTeachers = false;
		int changeShops = -1;
		int dungeonPostProcessing = -1;
		LinkedHashMap<String, Integer> flagMap = new LinkedHashMap<String, Integer>();
		String[] okFlags =  {"M", "D", "O", "B", "T", "A", "P", "W", "R", "E", "C", "V", "Q", "I", "H", "G", "S", "Y"};
		boolean[] usesVal = {true, false, false, false, false, false, true, false, true, true, true, true, true, true, true, true, true, true};
		for(int i = 0; i < okFlags.length; i++)
			flagMap.put(okFlags[i], -1);
		while(flags.length() > 0)
		{
			char ch1 = flags.charAt(0);
			char ch2 = 'A';
			if(flags.length() > 1)
				ch2 = flags.charAt(1);
			int val = 1;
			int grabChars = 1;
			if(Character.isDigit(ch2))
			{
				val = Integer.parseInt(String.valueOf(ch2));
				grabChars++;
			}
			for(int i = 0; i < okFlags.length; i++)
			{
				if(okFlags[i].equals("" + ch1))
				{
					flagMap.put("" + ch1, val);
					if(!usesVal[i])
						flagMap.put("" + ch1, -2);
					break;
				}
			}
			flags = flags.substring(grabChars);
		}
		String[] ffs = new String[flagMap.keySet().size()];
		ffs = flagMap.keySet().toArray(ffs);
		String finalFlags = "";
		int mapPad = 0;
		for(int i = 0; i < ffs.length; i++)
		{
			char ch1 = ffs[i].charAt(0);
			int val = flagMap.get(ffs[i]);
			if(val == -1)
				continue;
			else if(val >= 0)
				finalFlags += "" + ch1 + val;
			else
				finalFlags += "" + ch1;
			switch(ch1)
			{
			case 'M':  //randomize map
				randomizeMap = true;
				mapPad = 5 * val;
				break;
			case 'D':
				Dungeon[] ds = UltimaRando.randomizeDungeons(this);
				dungeonPostProcessing = this.changeDungeonBlock(ds);
				break;
			case 'O':
				randomizeMoongates = true;
				break;
			case 'B':
				if(!randomizeMap)
					changeBalloonToBritannia(new Point(86, 108));
				else
					britBalloon = true;
				break;
			case 'T':
				//changeSpellTeachers();
				changeSpellTeachers = true;
				break;
			case 'A':
				changeEnemyAI();
				break;
			case 'P':
				changeShops = val;
				break;
			case 'W':
				changeViewSpell();
				break;
			case 'R':
				this.changeSearchLocItems(UltimaRando.shuffleSearchItems(this.getSearchLocItems(val), val), val);
				break;
			case 'E':
				int eChangeAmt = 0;
				if((val & 1) == 1)
					eChangeAmt += 10;
				if((val & 2) == 2)
					eChangeAmt += 20;
				ArrayList<Integer> lstStillWeak = toughenEnemies(eChangeAmt);
				if((val & 4) == 4)
					toughenDungeonSet(lstStillWeak);
				break;
			case 'C':
				changeChestGold(val);
				break;
			case 'V':
				if(val <= 2)
					changeVirtues(ch.getRandomVirtue(val));
				else
					grantAvatarhoodAtStart();
				break;
			case 'Q':
				changeEquippableEquipment(ch.getRandomEquippables(val, this.getInitEquippables()));
				clearInitEquips();
				break;
			case 'I':
				changeStartingEquipment(ch.getRandomStartEquip(val, this.getInitItems()));
				clearInitEquips();
				break;
			case 'H':
				changeReagents(ch.getRandomReagents(val));
				break;
			case 'G':
				changeGold(ch.getRandomGold(val));
				break;
			case 'S':
				changeInitSpells(ch.getRandomInitSpells(val));
				break;
			case 'Y':
				changeAbyssLength(val, dungeonPostProcessing);
				break;
			}
		}
		if(changeSpellTeachers || changeShops > -1)
		{
			if(changeSpellTeachers && changeShops > -1)
				changeSpellsAndShops(changeShops);
			else 
			{
				if(changeSpellTeachers)
					changeSpellTeachers();
				else if(changeShops > -1)
					changeShopLocations(changeShops);
			}
		}
		//randomize map and moongates
		if(randomizeMap)
		{
			Map m = new Map(256, 256);
			m.setSeaPad(mapPad);
			m.makeRandomMap();
			byte[] fm = m.generateFinalMap();
			changeStartLocs(m.getNewStartingLocs());
			changeOutsideSearchLocs(m.getOutsideSearchSpots());
			changeODLocations(m.getLocationBlock());
			int[] shd = m.getShrineHumilData();
			changeShrineHumilData(shd[0], shd[1], shd[2]);
			byte[][] mgate = m.getMoongateBlock(randomizeMoongates);
			changeMoongateLocs(mgate[0], mgate[1]);
			changeBalloonAndWhirlpool(m.balloon, m.whirlpool);
			if(britBalloon)
				changeBalloonToBritannia(m.getBritBalloon());
			changeMap(fm);
			/*MapWindow mw = new MapWindow(fm, true);
			mw.setPOIList(m.getPOIList());
			System.out.println(this.getODLocations());
			System.out.println(this.getStartLocs());*/
		}
		else if(randomizeMoongates)
		{
			byte[][] mgate = getRandomMoongates();
			changeMoongateLocs(mgate[0], mgate[1]);
		}
		if(outFileName == null)
		{
			return null;
		}
		File f = new File(outFileName);
		String fn = f.getName();
		if(fn.startsWith("U4Rando."))
		{
			String[] parts = fn.split("\\.");
			parts[2] = finalFlags;
			fn = "";
			for(int i = 0; i < parts.length - 1; i++)
				fn += parts[i] + ".";
			fn += "nes";
			
			String path = f.getParent() + File.separator + fn;
			outFileName = path;
		}
		dumpRom(outFileName);
		return new NESRom(outFileName);
	}
	
	private void changeGateSpellCities(byte[] ord)
	{
		//grab the cities
		byte[] cNameData = getData(7458, 7519);
		byte[][] cNames = new byte[8][];
		int rd = 0;
		for(int i = 0; i < 8; i++)
		{
			cNames[i] = new byte[15];
			int j = 0;
			while(cNameData[rd] != 0)
			{
				cNames[i][j] = cNameData[rd];
				j++;
				rd++;
			}
			rd++;
		}
		byte[][] outNames = new byte[8][];
		for(int i = 0; i < ord.length; i++)
			outNames[i] = cNames[ord[i]];
		int wpos = 7458;
		rd = 0;
		for(int i = 0; i < 8; i++)
		{
			int j = 0;
			while(outNames[i][j] != 0)
			{
				romData[wpos] = outNames[i][j];
				j++;
				wpos++;
			}
			romData[wpos] = 0;
			wpos++;
		}
	}

	private byte[][] getRandomMoongates() 
	{
		ArrayList<Point> mgs = getMoongates();
		ArrayList<Point> mgsf = new ArrayList<Point>(mgs.size());
		ArrayList<Integer> ci = new ArrayList<Integer>();
		for(int i = 0; i < 8; i++)
			ci.add(i);
		//byte[] old = {4,2,1,6,0,7,5,3};
		byte[] cNew = new byte[8];
		int j = 0;
		while(mgs.size() > 0)
		{
			int r = (int) (UltimaRando.rand() * mgs.size());
			Point p = mgs.remove(r);
			int cty = ci.remove(r);
			mgsf.add(p);
			cNew[j] = (byte) cty;
			j++;
		}
		byte[] rv = new byte[mgsf.size() * 2 + 1];
		for(int i = 0; i < mgsf.size(); i++)
		{
			Point p = mgsf.get(i);
			rv[i] = (byte) p.x;
			rv[i + 9] = (byte) p.y;
		}
		rv[8] = -128;
		byte[][] frv = {rv, cNew};
		return frv;
	}

	public byte[] getData(int firstOffset, int lastOffset)
	{
		byte[] rval = new byte[lastOffset - firstOffset + 1];
		for(int i = firstOffset; i <= lastOffset; i++)
			rval[i - firstOffset] = romData[i];
		return rval;
	}
	
	public byte[][] getPotMap()
	{
		byte[][] rVal = new byte[8][];
		for(int i = 0; i < 8; i++)
		{
			//0x4010->0x600f, 0x8010->0xa00f, etc.
			rVal[i] = getData(16400 + 16384 * i, 24591 + 16384 * i);
		}
		return rVal;
	}
	
	private int btoi(byte v)
	{
		int ii = v & 255;
		return ii;
	}
	
	public String getInitShrineHumilData()
	{
		int dir1 = romData[256182];  //0x3e8b6 you are testing either x or y here (y is $43, x is $42)
		int reqValue = romData[256184] & 255;  //0x3e8b8 you are comparing to a value here
		int dir2 = romData[256188];     //0x3e8bc  you are testing the other one here (y is $42, x is $43)
		int d2min = romData[256190] & 255;    //0x3e8be  you are comparing to a min here
		int d2max = romData[256194] & 255;    //0x3e8c2  you are comparing to a max here
		int pushDir = romData[159633];  //0x26f91  you are changing this value when the battle happens
		byte pushOp = romData[159632];   //0x26f90  you are adjusting it up or down (inc vs dec)
		
		String s = "The Shrine of Humility checks if ";
		if(dir1 == 66) s += "x ";
		else s += "y ";
		s += "is equal to " + reqValue + "\nand if ";
		if(dir2 == 66) s+= "x ";
		else s += "y ";
		s += " is between " + d2min + " and " + d2max + ".\nIf a battle is triggered, then ";
		if(pushDir == 66) s += "x ";
		else s += "y ";
		s += "is ";
		if(pushOp == -58)  s += "decremented.";
		else s += "incremented.";
		return s;
	}
	
	public void changeShrineHumilData(int shrineX, int shrineY, int shrineEntDir)
	{
		//shrineEntDir: 0=from west, 1=from east, 2=from north (default), 3=from south
		byte dir1 = 67;
		byte reqValue;
		byte dir2 = 66;
		byte d2min = (byte) (shrineX - 1);
		byte d2max = (byte) (shrineX + 2);
		byte pushDir = 67;
		byte pushOp;
		if(shrineEntDir < 2)
		{
			dir1 = 66;
			dir2 = 67;
			pushDir = 66;
			d2min = (byte) (shrineY - 1);
			d2max = (byte) (shrineY + 2);
		}
		byte[] reqV = {(byte) (shrineX - 1), (byte) (shrineX + 1), (byte) (shrineY - 1), (byte) (shrineY + 1)};
		byte[] ops = {-58, -26, -58, -26};  //c6 (-58) for decement, e6 (-26) for increment
		reqValue = reqV[shrineEntDir];
		pushOp = ops[shrineEntDir];
		
		romData[256182] = dir1;     //0x3e8b6 you are testing either x or y here (y is $43, x is $42)
		romData[256184] = reqValue; //0x3e8b8 you are comparing to a value here
		romData[256188] = dir2;     //0x3e8bc  you are testing the other one here (y is $42, x is $43)
		romData[256190] = d2min;    //0x3e8be  you are comparing to a min here
		romData[256194] = d2max;    //0x3e8c2  you are comparing to a max here
		romData[159633] = pushDir;  //0x26f91  you are changing this value when the battle happens
		romData[159632] = pushOp;   //0x26f90  you are adjusting it up or down (inc or dec)
		
		//the horn usage radius must be updated too
		int xMin = Math.max(shrineX - 4, 0);
		int xMax = Math.min(shrineX + 4, 255);
		int yMin = Math.max(shrineY - 4, 0);
		int yMax = Math.min(shrineY + 4, 255);
		romData[13731] = (byte) xMin; //0x35a3 horn x min
		romData[13735] = (byte) xMax; //0x35a7 horn x max
		romData[13741] = (byte) yMin; //0x35ad horn y min
		romData[13745] = (byte) yMax; //0x35b1 horn y max
	}
	
	public String getStartLocs()
	{
		int slData = 222723;  //0x36603 is xs, 0x3660b is ys
		String[] startChars = {"Mage", "Bard", "Fighter", "Druid", "Tinker", "Paladin", "ranger", "Shepherd"};
		String s = "";
		for(int i = 0; i < 8; i++)
		{
			s += startChars[i] + " starts at " + btoi(romData[slData + i]) + "," + btoi(romData[slData + 8 + i]) + "\n";
		}
		return s;
	}
	
	public void changeStartLocs(ArrayList<Point> newLocs)
	{
		int slData = 222723;
		for(int i = 0; i < newLocs.size(); i++)
		{
			romData[slData + i] = (byte) newLocs.get(i).x;
			romData[slData + i + 8] = (byte) newLocs.get(i).y;
		}
	}
	
	public void changeBalloonAndWhirlpool(Point balloon, Point whirlpool)
	{
		//s += "\nWhirlpool location: " + btoi() + "    ";
		romData[218603] = (byte) whirlpool.x; 
		romData[218604] = (byte) whirlpool.y;
		//s += "Balloon location: " + btoi();
		romData[218601] = (byte) balloon.x;
		romData[218602] = (byte) balloon.y;
		romData[256610] = (byte) balloon.x;
		romData[256615] = (byte) balloon.y;
	}
	
	public void changeBalloonToBritannia(Point britBalloon)
	{
		romData[218601] = (byte) britBalloon.x;
		romData[218602] = (byte) britBalloon.y;
		//at this point we need to flip the code that checks for Hythloth's exit to move the balloon and
		//the code that sets your overworld x and y after leaving a location
		romData[256307] = 74;  //execute the earlier code
		String s = "a5 4b 29 bf c9 2f d0 03 ad c2 06 a2 00 dd f3 ea f0 06 e8 e0 26 90 f6 60 c9 01 d0 0d a9 ";
		s += Integer.toHexString(britBalloon.x) + " 8d f0 68 a9 ";
		s += Integer.toHexString(britBalloon.y) + " 8d f1 68 ea ea ea 8a 0a";
		byte[] fx = strToBytes(s);
		for(int i = 0; i < fx.length; i++)
			romData[256602 + i] = fx[i];
	}
	
	public String getODLocations()
	{
		String[] odLocs = {"Shrine of Justice", "Shrine of Spirituality", "Dungeon Wrong", "Minoc", "Dungeon Covetous", "Yew",
				"Shrine of Sacrifice", "Empath Abbey W", "Empath Abbey E", "Vesper", "Shrine of Honesty", "Dungeon Despise", "Dungeon Deceit",
				"Serpent's Spire", "Cove", "Shrine of Compassion", "Dungeon Shame", "Britain", "Castle Britannia W", "Castle Britannia E",
				"Lyceaum W", "Lyceaum E", "Skara Brae", "Moonglow", "Paws", "Buccaneer's Den", "Dungeon Destard", "Magincia", "Trinsic",
				"Shrine of Honor", "Shrine of Humility", "Jhelom", "Shrine of Valor", "The Abyss", "Island Shrine", "Hythloth",
				"Serpent's Hold W", "Serpent's Hold E"};
		String s = "";
		int dataLoc = 256695;
		for(int i = 0; i < odLocs.length; i++)
		{
			int dlx = romData[dataLoc + (2 * i)] & 255;
			int dly = romData[dataLoc + (2 * i + 1)] & 255;
			s += odLocs[i] + " is at " + dlx + "," + dly + "\n";
		}
		return s;
	}
	
	public ArrayList<POI> getPOIList()
	{
		String[] odLocs = {"Shrine of Justice", "Shrine of Spirituality", "Dungeon Wrong", "Minoc", "Dungeon Covetous", "Yew",
				"Shrine of Sacrifice", "Empath Abbey W", "Empath Abbey E", "Vesper", "Shrine of Honesty", "Dungeon Despise", "Dungeon Deceit",
				"Serpent's Spire", "Cove", "Shrine of Compassion", "Dungeon Shame", "Britain", "Castle Britannia W", "Castle Britannia E",
				"Lyceaum W", "Lyceaum E", "Skara Brae", "Moonglow", "Paws", "Buccaneer's Den", "Dungeon Destard", "Magincia", "Trinsic",
				"Shrine of Honor", "Shrine of Humility", "Jhelom", "Shrine of Valor", "The Abyss", "Island Shrine", "Hythloth",
				"Serpent's Hold W", "Serpent's Hold E"};
		ArrayList<POI> rv = new ArrayList<POI>(odLocs.length);
		int dataLoc = 256695;
		for(int i = 0; i < odLocs.length; i++)
		{
			int dlx = romData[dataLoc + (2 * i)] & 255;
			int dly = romData[dataLoc + (2 * i + 1)] & 255;
			Point p = new Point(dlx, dly);
			POI pt = new POI(p, odLocs[i]);
			rv.add(pt);
		}
		return rv;
	}
	
	public void changeODLocations(byte[] block)
	{
		int dataLoc = 256695;
		for(int i = 0; i < block.length; i++)
			romData[i + dataLoc] = block[i];
		//eliminate the "early exit" from the lookup (3e9a6-3e9a7 -> nop (ea,-22))
		romData[256422] = -22;
		romData[256423] = -22;
		//point #33 (block 66) is the Abyss 
		//so to make the abyss items work we need to set the value at 0x336d to the Abyss's new location,
		//which has a requirement to be on the center diagonal line 
		romData[13165] = block[66];
	}
	
	public String getMoongateLocs()
	{
		String s = "";
		String[] moons = {"New", "Waxing Crescent", "Waxing Half", "Waxing Gibbous", "Full", "Waning Gibbous", "Waning Half", "Waning Crescent"};
		int dataLoc = 81741;
		for(int i = 0; i < 8; i++)
		{
			int dlx = romData[dataLoc + i] & 255;
			int dly = romData[dataLoc + 9 + i] & 255;
			s += moons[i] + " Moongate is at " + dlx + "," + dly + "\n";
		}
		return s;
	}
	
	public ArrayList<Point> getMoongates()
	{
		int dataLoc = 81741;
		ArrayList<Point> pts = new ArrayList<Point>(8);
		for(int i = 0; i < 8; i++)
		{
			int dlx = romData[dataLoc + i] & 255;
			int dly = romData[dataLoc + 9 + i] & 255;
			Point pp = new Point(dlx, dly);
			pts.add(pp);
		}
		return pts;
	}
	
	public void changeMoongateLocs(byte[] block, byte[] gateOrder)  //the block needs to have a -128 in the center
	{
		int dataLoc = 81741;
		int dataLoc2 = 98125;
		byte ssx = block[4];
		byte ssy = block[13];
		byte s4x = block[0];
		byte s4y = block[9];
		for(int i = 0; i < block.length; i++)  
		{
			romData[dataLoc + i] = block[i];
			romData[dataLoc2 + i] = block[i];
		}
		//the fourth search location on the map is the 1st moongate location
		int dataLoc3 = 25008;
		if(romData[dataLoc3] != s4x)
		{
			System.out.println("Updating New Moon Moongate search location x due to change in Moongate location");
			romData[dataLoc3] = s4x;
		}
		if(romData[dataLoc3 + 1] != s4y)
		{
			System.out.println("Updating New Moon Moongate search location y due to change in Moongate location");
			romData[dataLoc3 + 1] = s4y;
		}
		//the Shrine of Spirituality goes to the 4th moongate location
		dataLoc3 = 256697;
		if(romData[dataLoc3] != ssx)
		{
			System.out.println("Updating Shrine of Spirituality x location due to change in Moongate location");
			romData[dataLoc3] = ssx;
		}
		if(romData[dataLoc3 + 1] != ssy)
		{
			System.out.println("Updating Shrine of Spirituality y location due to change in Moongate location");
			romData[dataLoc3 + 1] = ssy;
			//eliminate the "early exit" from the lookup (3e9a6-3e9a7 -> nop (ea,-22))
			romData[256422] = -22;
			romData[256423] = -22;
		}
		
		changeGateSpellCities(gateOrder);
	}
	
	public String getInitSearchSpotData()
	{
		String s = "";
		String[] runes = {"Honesty", "Compassion", "Valor", "Justice", "Sacrifice", "Honor", "Spirituality", "Humility"};
		String[] runeLocs = {"1635b", "239b1", "1f0d6", "168e1", "12650", "1a44f", "65e6", "1ae0a"};
		String[] iItems = {"Wht Stone", "Scale", "Flute", "Candle", "Book", "Robe"};
		String[] iIndex = {"d6", "85", "86", "87", "8", "a7"};
		String[] iLocs = {"22bb9", "22bb5", "bbe8", "1e4e8", "a467", "bb54"};
		String[] mapItems = {"Horn", "Skull", "Bell", "Bk Stone", "Fungus", "Manroot"};
		String[] mapIndex = {"b", "c", "9", "57", "36", "37"};
		String[] mapLocs = {"61a2", "61a6", "61b6", "61b2", "61aa", "61ae"};
		for(int i = 0; i < runes.length; i++)
		{
			s += "Rune of " + runes[i];
			int dLoc = Integer.parseInt(runeLocs[i], 16);
			byte b = romData[dLoc];
			if(b == 64 + i)
				s += " FOUND at " + romData[dLoc - 2] + "," + romData[dLoc - 1];
			else
				s += " NOT FOUND";
			s += "\n";
		}
		for(int i = 0; i < iItems.length; i++)
		{
			s += "Inside Item " + iItems[i];
			int dLoc = Integer.parseInt(iLocs[i], 16);
			byte b = romData[dLoc];
			byte ll = (byte) Integer.parseInt(iIndex[i], 16);
			if(b == ll)
				s += " FOUND at " + romData[dLoc - 2] + "," + romData[dLoc - 1];
			else
				s += " NOT FOUND";
			s += "\n";
		}
		for(int i = 0; i < mapItems.length; i++)
		{
			s += "Map Item " + mapItems[i];
			int dLoc = Integer.parseInt(mapLocs[i], 16);
			byte b = romData[dLoc];
			byte ll =  (byte) Integer.parseInt(mapIndex[i], 16);
			int xx = romData[dLoc - 2] & 255;
			int yy = romData[dLoc - 1] & 255;
			if(b == ll)
				s += " FOUND at " + xx + "," + yy;
			else
				s += " NOT FOUND";
			s += "\n";
		}
		return s;
	}
	
	public String getFinalSearchLocData()
	{
		ArrayList<Byte> bts = getSearchLocItems(2);
		String[] spots = {"Avatar Sword", "Avatar Armor", "Moonglow", "Britain", "Jhelom", "Yew", "Minoc", "Trinsic", "Castle Britannia", "Paws",
				"Serpent's Hold 1", "Serpent's Hold 2", "Magincia 1", "Cove", "Lyceaum", "Magincia 2",
				"Horn Island", "Skull Shoal", "Bell Shoal", "New Moongates", "Fungus", "Manroot"};
		String[] items = {"1e", "28", "56", "5", "6", "7", "8", "27", "b", "c", "9", "57", "36", "37"};
		String[] iNames = {"Pradise Sword", "Exotic Armor", "Wht Stone", "Scale", "Flute", "Candle", "Book", "Robe", "Horn", "Skull", "Bell", "Bk Stone", "Fungus", "Manroot"};
		String[] runes = {"Honesty", "Compassion", "Valor", "Justice", "Sacrifice", "Honor", "Spirituality", "Humility"};
		
		String rv = "";
		for(int i = 0; i < bts.size(); i++)
		{
			rv += spots[i] + " search spot has ";
			int itm = bts.get(i) & 127;
			boolean iFoundIt = false;
			for(int j = 0; j < runes.length; j++)
			{
				if(itm == (64 + j))
				{
					rv += "Rune of " + runes[j];
					iFoundIt = true;
					break;
				}
			}
			if(!iFoundIt)
			{
				for(int j = 0; j < items.length; j++)
				{
					if(itm == Integer.parseInt(items[j], 16))
					{
						rv += iNames[j];
						iFoundIt = true;
						break;
					}
				}
				if(!iFoundIt && itm >= 16 && ((itm - 15) < inventoryList.size()))
				{
					rv += inventoryList.get(itm - 15);
					iFoundIt = true;
				}
			}
			rv += "\n";
		}
		return rv;
	}
	
	public ArrayList<Byte> getSearchLocItems(int mode)
	{
		String[] castleLocs = {"a601", "abd3"};
		String[] runeLocs = {"1635b", "239b1", "1f0d6", "168e1", "12650", "1a44f", "65e6", "1ae0a"};
		String[] iLocs = {"22bb9", "22bb5", "bbe8", "1e4e8", "a467", "bb54"};
		String[] mapLocs = {"61a2", "61a6", "61b6", "61b2", "61aa", "61ae"};
		ArrayList<Byte> rv = new ArrayList<Byte>(22);
		int j = 0;
		if((mode & 4) == 4)
		{
			byte m = (byte) (21 + (UltimaRando.rand() * 10));
			rv.add((byte) (m | -128));//add random melee
			m = (byte) (32 + (UltimaRando.rand() * 10));
			rv.add((byte) (m | -128));//add random armor
			j = 2;
		}
		else if((mode & 2) == 2)
		{
			for(int i = 0; i < castleLocs.length; i++)
			{
				int dLoc = Integer.parseInt(castleLocs[i], 16);
				rv.add(romData[dLoc]);// = newItems.get(j);
				j++;
			}
		}
		for(int i = 0; i < runeLocs.length; i++)
		{
			int dLoc = Integer.parseInt(runeLocs[i], 16);
			rv.add(romData[dLoc]);// = newItems.get(j);
			j++;
		}
		for(int i = 0; i < iLocs.length; i++)
		{
			int dLoc = Integer.parseInt(iLocs[i], 16);
			rv.add(romData[dLoc]);// = newItems.get(j);
			j++;
		}
		for(int i = 0; i < mapLocs.length; i++)
		{
			int dLoc = Integer.parseInt(mapLocs[i], 16);
			rv.add(romData[dLoc]);// = newItems.get(j);
			j++;
		}
		return rv;
	}
	
	public void changeSearchLocItems(ArrayList<Byte> newItems, int mode)
	{
		String[] castleLocs = {"a601", "abd3"};
		String[] runeLocs = {"1635b", "239b1", "1f0d6", "168e1", "12650", "1a44f", "65e6", "1ae0a"};
		String[] iLocs = {"22bb9", "22bb5", "bbe8", "1e4e8", "a467", "bb54"};
		String[] mapLocs = {"61a2", "61a6", "61b6", "61b2", "61aa", "61ae"};
		int j = 0;
		if(mode > 1)
		{
			for(int i = 0; i < castleLocs.length; i++)
			{
				int dLoc = Integer.parseInt(castleLocs[i], 16);
				romData[dLoc] = newItems.get(j);
				j++;
			}
		}
		for(int i = 0; i < runeLocs.length; i++)
		{
			int dLoc = Integer.parseInt(runeLocs[i], 16);
			romData[dLoc] = newItems.get(j);
			j++;
		}
		for(int i = 0; i < iLocs.length; i++)
		{
			int dLoc = Integer.parseInt(iLocs[i], 16);
			romData[dLoc] = newItems.get(j);
			j++;
		}
		for(int i = 0; i < mapLocs.length; i++)
		{
			int dLoc = Integer.parseInt(mapLocs[i], 16);
			romData[dLoc] = newItems.get(j);
			j++;
		}
		//eliminate the moon check on items whose index & 7 == 7
		String s = "ea a9 00";
		byte[] bts = strToBytes(s);
		for(int i = 0; i < bts.length; i++)
			romData[12400 + i] = bts[i];
	}
	
	public void changeOutsideSearchLocs(ArrayList<Point> newOutsideLocs)  //newOutsideLocs will have the balloon, whirlpool, and abyss which we do not care about
	{
		String[] mapLocs = {"61a2", "61a6", "61b6", "61b2", "61aa", "61ae"};
		//horn, skull, bell, black stone, fungus, manroot
		for(int i = 0; i < mapLocs.length; i++)
		{
			int dloc = Integer.parseInt(mapLocs[i], 16);
			Point p = newOutsideLocs.get(i);
			romData[dloc - 2] = (byte) p.x;
			romData[dloc - 1] = (byte) p.y;
		}
		changeFungusSearchText(newOutsideLocs.get(4));
		changeManrootSearchText(newOutsideLocs.get(5));
	}
	
	private void changeManrootSearchText(Point p)
	{
		String yy = "" + p.y;
		String xx = "" + p.x;
		while(yy.length() < 2)
			yy = " " + yy;
		while(xx.length() < 3)
			xx = " " + xx;
		String sss = "at latitude " + yy + ",\nlongitude " + xx;
		if(yy.length() == 3)
			sss = "at latitude " + yy + "\nlongitude " + xx;
		TextFinder tf = new TextFinder(this);
		tf.replaceSpokenText(95292, 95362, sss, 4);
	}
	
	private void changeFungusSearchText(Point p)
	{
		String yy = "" + p.y;
		String xx = "" + p.x;
		//for(int k = 2; k > j; k--)
			//xx += " ";
		//for(int k = 0; k < j; k++)
			//xx += "1";
		int nspc = 5 - yy.length() - xx.length();
		String aa = " Search ";
		for(int k = 0; k < nspc; k++)
			aa += " ";
		String sss = aa + yy + "," + xx + " on t";
		if(nspc < 0)
			sss = " Seek  " + yy + "," + xx + " on t";
		TextFinder tf = new TextFinder(this);
		tf.replaceSpokenText(93248, 93288, sss, 2);
	}
	
	public void changeGold(int newVal)
	{
		byte g1 = (byte) (newVal & 255);
		byte g2 = (byte) (newVal >> 8);
		romData[218449] = g1;
		romData[218454] = g2;
	}
	
	public void changeChestGold(int newVal) //uses ffa9-ffb3
	{
		if(newVal <= 6)
			newVal *= 25;
		String ff = "69 " + Integer.toHexString(newVal) + " e8 e8 9d 01 01 6d 27 68 60";
		//String f2 = "ac 15 68 a9 63 99 90 68 99 98 68 20 d2 cc 60";
		byte[] fcb = strToBytes(ff);
		int ff1 = 262073;
		for(int i = 0; i < fcb.length; i++)
			romData[ff1 + i] = fcb[i];
		String fcall2 = "20 a9 ff";  //->ffa9
		fcb = strToBytes(fcall2);
		ff1 = 248437;
		for(int i = 0; i < fcb.length; i++)
			romData[ff1 + i] = fcb[i];
	}
	
	public void changeVirtues(int newVal)
	{
		romData[218482] = (byte) newVal;
	}
	
	public void grantAvatarhoodAtStart()
	{
		romData[218482] = (byte) 100;
		byte b = romData[218477];
		if(b == -87)
		{
			for(int i = 0; i < 4; i++)
				romData[218477 + i] = -22;  //NOP out the unnecessary LDA 0 TAX TAY
		}
		/*String fcall = "20 ea bf";
		byte[] fcb = strToBytes(fcall);
		int fc1 = 218478;
		for(int i = 0; i < fcb.length; i++)
			romData[fc1 + i] = fcb[i];
		String f1 = " 60";
		fcb = strToBytes(f1);
		int ff1 = 229370;
		for(int i = 0; i < fcb.length; i++)
			romData[ff1 + i] = fcb[i];*/
		//put avatar loader in D:3f04
		String f2 = "a9 ff 8d 14 68 ac 15 68 a9 63 99 90 68 99 98 68 4c 42 c2";
		byte[] fcb = strToBytes(f2);
		int ff1 = 229140;
		for(int i = 0; i < fcb.length; i++)
			romData[ff1 + i] = fcb[i];
		String fcall2 = "4c 04 bf";
		fcb = strToBytes(fcall2);
		ff1 = 221785;
		for(int i = 0; i < fcb.length; i++)
			romData[ff1 + i] = fcb[i];
	}
	
	public void changeViewSpell() //uses ffa1-ffa8
	{
		//point F:FE75 (3fe85) to jump to ffa1
		String aa = "a1 ff";
		byte[] bts = strToBytes(aa);
		for(int i = 0; i < bts.length; i++)
			romData[261765 + i] = bts[i];
		//ffa1 instructs you to jump to bf70 on page 7
		String jf = "a9 07 20 31 f3 4c 70 bf";
		bts = strToBytes(jf);
		for(int i = 0; i < bts.length; i++)
			romData[262065 + i] = bts[i];
		//put main string selector at 7:BF6C (1ff7c)
		String ssel = "c6 2a c6 2a 20 12 bf ad e0 06 c9 08 d0 f2 ";
		ssel += "a9 00 8d e0 06 8d e4 06 4c 31 f3";
		bts = strToBytes(ssel);
		for(int i = 0; i < bts.length; i++)
			romData[130940 + i] = bts[i];
		//put main string constructor at 7:BF12 (1ff22)
		String scon = "ba 86 2b a2 3f a9 00 8d e1 06 8d e2 06 ac e3 06 ";
		scon += "ad e4 06 d0 05 b9 00 62 50 03 b9 00 63 ";
		scon += "c9 05 d0 03 ee e1 06 ";
		scon += "c9 32 d0 03 ee e2 06 ";
		scon += "c8 d0 03 ee e4 06 ca 10 dc 20 d0 bf ee e0 06 8c e3 06 ";
		scon += "a2 02 a9 00 48 bd e0 06 48 ca 10 f6 ";
		scon += "a9 60 85 28 a9 c9 85 29 ";
		scon += "a9 bf 48 a9 b0 48 4c 42 c2";
		bts = strToBytes(scon);
		for(int i = 0; i < bts.length; i++)
			romData[130850 + i] = bts[i];
		//put execution chain at 7:BFA8 (1ffb8) - not necessary
		/*String ec = "fe ca 67 cb 31 c3";
		bts = strToBytes(ec);
		for(int i = 0; i < bts.length; i++)
			romData[131000 + i] = bts[i];*/
		//put skeleton string at 7:BFB0 (1ffc0)
		String sstr = "16 00 1c 1f 1f 22 00 06 08 1c 00 0c 00 08 1c 06 00 13 18 15 23 24 23 0d 3e ";
		sstr += "00 08 1c 06 00 23 24 1f 1e 15 23 3f";
		bts = strToBytes(sstr);
		romData[131008] = (byte) bts.length;
		TextFinder tf = new TextFinder(this);
		bts = tf.compressSpokenBytes(bts);
		for(int i = 0; i < bts.length; i++)
			romData[131009 + i] = bts[i];
		
		//stone test at 7:BFD0 (1ffe0)
		String stnt = "ad e2 06 f0 19 ae e0 06 bd d1 06 38 e9 80 c9 06 f0 09 aa bd e7 cf 2d 19 68 f0 03 ce e2 06 60";
		bts = strToBytes(stnt);
		for(int i = 0; i < bts.length; i++)
			romData[131040 + i] = bts[i];
		
		//finally, disable the View spell on the overworld map
		//F:ECE9 (3ecf9) -> 8
		romData[257273] = 8;
	}
	
	public void changeInitSpells(byte[] newSpells)
	{
		//System.out.println("New Spells = " + Arrays.toString(newSpells));
		romData[218459] = newSpells[0];
		String newCode = "a9 " + Integer.toHexString(newSpells[1]) + " 8d 1c 68 a9 " + 
		       Integer.toHexString(newSpells[2]) + " 8d 1e 68 a9 01 8d d1 68 ea ea ea";
		int ncs = 218463;
		byte[] nc = strToBytes(newCode);
		for(int i = 0; i < nc.length; i++)
			romData[ncs + i] = nc[i];
	}
	
	public void changeStartingEquipment(byte[] newEquip)
	{
		for(int i = 0; i < 8; i++)
		{
			romData[218685 + i * 2] = newEquip[i * 2];
			romData[218686 + i * 2] = newEquip[i * 2 + 1];
		}
	}
	
	public void clearInitEquips()
	{
		for(int i = 0; i < 8; i++)
		{
			romData[218685 + i * 2] &= 127;
			romData[218686 + i * 2] &= 127;
		}
	}
	
	public void changeEquippableEquipment(byte[] newEquip)
	{
		for(int i = 0; i < newEquip.length; i++)
			romData[3956 + i] = newEquip[i];
	}
	
	public void changeReagents(byte[] reagents)
	{
		for(int i = 0; i < 8; i++)
		{
			romData[218677 + i] = reagents[i];
		}
	}
	
	public void changeSpellTeachers()
	{
		villagerSwap(0, 0);
		villagerSwap(1, 0);
		
	}
	
	public void changeShopLocations(int mode)
	{
		villagerSwap(0, 0);
		villagerSwap(2, mode);
	}
	
	public void changeSpellsAndShops(int mode)
	{
		villagerSwap(0, 0);
		villagerSwap(3, mode);
	}
	
	public void changeAbyssLength(int val, int dungPostProcessingLoc)  //uses only bank 9 if dungeons are randomized, else uses ff78-ff88
	{
		val--;
		String a1 = "79 01 29 3f 09 1f 1b 09";
		byte[] floorStart = strToBytes(a1);
		
		//F:EBBC (3ebcc) is floorStart[val]
		romData[256972] = floorStart[val];
		
		String call = "";
		int callLoc = 0;
		if(dungPostProcessingLoc != -1)
		{
			call = bytesToStr(getData(dungPostProcessingLoc, dungPostProcessingLoc + 2));
			//call = call.toLowerCase();
			callLoc = dungPostProcessingLoc;
		}
		else
		{
			//point e5d6 to call shortAbyss selector (f:e5d6, or 3e5e6)
			String selHook = "20 78 ff";
			byte[] sh = strToBytes(selHook);
			for(int i = 0; i < sh.length; i++)
				romData[255462 + i] = sh[i];
			
			//place abyssSelector at ff78 (f:ff88, or 3ff88)
			String abyssSelect = "a5 4b c9 87 d0 03 20 d8 bf 20 a3 e5 60";
			byte[] nf = strToBytes(abyssSelect);
			for(int i = 0; i < nf.length; i++)
				romData[262024 + i] = nf[i];
			callLoc = 262030;
		}
		
		String shortAbyss = "a5 4b c9 87 d0 05 a9 0" + val + " 8d c1 06 a9 00 " + call + "60";
		//put it at 9:BFD8 (27fe8)
		byte[] nf = strToBytes(shortAbyss);
		for(int i = 0; i < nf.length; i++)
			romData[163816 + i] = nf[i];
		
		String rep = "20 d8 bf";
		byte[] rb = strToBytes(rep);
		for(int i = 0; i < rb.length; i++)
			romData[callLoc + i] = rb[i];
		
	}
	
	public void changeBusywork(boolean cst)  //may not be doable
	{
		/*if(cst)
			villagerSwap(3);
		else
		{
			villagerSwap(0);
			villagerSwap(2);
		}
		romData[249912] = 27;
		romData[249915] = 27;*/
	}
	
	private void toughenEnemy(int idx)
	{
		//idx must be between 1 and 65, inclusive
		if(idx < 1 || idx > 65)
			return;
		int[] vals = new int[4];
		int[] locs = new int[4];
		int[] caps = {255, 255, 192, 255};
		int[] muls = {3,3,3,5};
		int[] gazerMuls = {3,2,2,5};
		//26b58 - hp
		locs[0] = 158552 + idx;
		vals[0] = romData[locs[0]] & 255;
		//26b99 - attack
		locs[1] = 158617 + idx;
		vals[1] = romData[locs[1]] & 255;
		//26bda - defense (capped at 192)
		locs[2] = 158682 + idx;
		vals[2] = romData[locs[2]] & 255;
		// 2a64 - exp
		locs[3] = 10852 + idx;
		vals[3] = romData[locs[3]] & 255;
		//check for gazerAI
		byte ai = (byte) (romData[158747 + idx] & 15);
		for(int i = 0; i < 4; i++)
		{
			if(ai != 12)
				vals[i] *= muls[i];
			else
				vals[i] *= gazerMuls[i];
			if(vals[i] > caps[i])
				vals[i] = caps[i];
			romData[locs[i]] = (byte) vals[i];
		}
	}
	
	public void toughenDungeonSet(ArrayList<Integer> notTough)
	{
		int[] dSet = {1,2,3,4,5,6,7,8,37,44,51,52,54,55};
		for(int i = 0; i < dSet.length; i++)
			if(notTough.contains(dSet[i]))
				toughenEnemy(dSet[i]);
	}
	
	//returns the list of enemies not toughened
	public ArrayList<Integer> toughenEnemies(int amt)  //can be 10, 20, or 30
	{
		//the set of enemies that can be toughened are the first 8 (final boss), sailor(14), pirate(18), 
		//and 0x23-0x41 (35-65), inclusive
		//we don't toughen other villagers or animals
		ArrayList<Integer> enemies = new ArrayList<Integer>(41);
		for(int i = 1; i <= 8; i++)
			enemies.add(i);
		enemies.add(14);
		enemies.add(18);
		for(int i = 35; i <= 65; i++)
			enemies.add(i);
		for(int i = 0; i < amt; i++)
		{
			int r = (int) (UltimaRando.rand() * enemies.size());
			int e = enemies.remove(r);
			toughenEnemy(e);
		}
		return enemies;
	}
	
	public void changeEnemyAI()
	{
		//randomizes enemy AI by assigning each enemy one of a possible 14 AIs
		//26c1b - the last 4 bits determine AI; min is 0 max is 13 (D)
		boolean balrogInFB = false;
		for(int i = 1; i <= 8; i++)
		{
			//the final boss cannot have AI 6 (25% sleep), AI 8 (25% negate) or AI C (missile with 50% chance to sleep)
			//the final boss can have at most 1 AI that is Balrog (D) (1/16 sleep)
			while(true)
			{
				int b = romData[158747 + i] & 255;
				b &= 240; //F0
				int r = (int) (UltimaRando.rand() * 14);
				if(r == 6 || r == 8 || r == 12)
					continue;
				if(r == 13)
				{
					if(balrogInFB)
						continue;
					else
						balrogInFB = true;
				}
				b |= r;
				//byte x = (byte) (b & 255);
				romData[158747 + i] = (byte) (b & 255);
				break;
			}
		}
		for(int i = 9; i <= 65; i++)
		{
			int b = romData[158747 + i] & 255;
			b &= 240; //F0
			int r = (int) (UltimaRando.rand() * 14);
			b |= r;
			romData[158747 + i] = (byte) (b & 255);
		}
	}
	
	private void villagerSwap(int mode, int val)  //uses ffb4-ffc2
	{
		if(mode == 0)
		{
			//villager swap involves swapping the event of two villagers; 
			//it is code intensive because it spans multiple memory pages
			//step 1: line ec4a (3ec5a) calls ffb4
			String code = "4c b4 ff";
			byte[] bts = strToBytes(code);
			int s = 257114;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 2: ffb4 (3ffc4) swaps to page 3 and calls be46
			code = "a9 03 20 33 f3 4c 46 be";
			bts = strToBytes(code);
			s = 262084;
			int l1 = bts.length + s;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 2a: line d892 (3d8a2) calls ffbc
			code = "20 bc ff";
			bts = strToBytes(code);
			s = 252066;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 2c: ffbc loads the proper shop cleanup page and calls f333
			code = "ad dc 06 20 33 f3 60";
			bts = strToBytes(code);
			s = l1;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 3: page 3 be46 (0fe56) captures the city index, villager index, and swap-to page
			//it then looks to find the villager you are talking to in a replacement table, depending on
			//which villagers have been swapped; note that the swap-to page is stored twice to ensure compatibility with
			//shop swap
			//if a villager's index and city index is found in the table, hot swap in the 2 byte city index, the page index, and 
			//push the villager index on the stack
			//if a 0 is found, exit the function
			//it is a 76 byte monstrosity in total
			String[] a0 = {"93", "94", "95", "96", "97", "98"};
			code = "a5 53 8d de 06 a5 54 8d df 06 a5 4c 8d dd 06 8d dc 06 "; //store off current city index and current memory page
			code += "68 a2 00 a8 bd " + a0[0] + " be d0 05 98 48 4c a7 bf ";  //pull off villager index, store in y, and exit if 0 is the next entry in the table
			code += "98 dd " + a0[0] + " be d0 07 a5 53 dd " + a0[1] + " be f0 07 18 8a 69 06 aa d0 e2 ";  //also test city index and if not equal move to the next table entry
			code += "bd " + a0[2] + " be 48 bd " + a0[3] + " be 85 53 bd " + a0[4] + " be 85 54 bd " + a0[5] + " be 85 4c 8d dc 06 4c a7 bf";  //if found, do the hot swap of city index and page index
			bts = strToBytes(code);
			s = 65110;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 4: line ec5f (3ec6f) calls bf88 to cleanup (restore you to the proper city)
			code = "4c 88 bf";
			bts = strToBytes(code);
			s = 257135;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			//step 5: line ec62 (3ec72) calls bf8e to cleanup
			code = "20 8e bf";
			bts = strToBytes(code);
			s = 257138;
			for(int i = 0; i < bts.length; i++)
				romData[s + i] = bts[i];
			int[] pgs = {1, 2, 3, 4, 5, 6, 7, 8, 13};
			for(int j = 0; j < pgs.length; j++)
			{
				int page = 16384 * pgs[j];
				//step 6: put cleanup in all pertinent pages at bf88 (pages 1-8 and D)
				code = "20 95 bf 4c fc c1 20 95 bf 20 2a c9 60 ";
				code += "48 ad dd 06 85 4c ad de 06 85 53 ad df 06 85 54 68 60 ";
				//step 7: put this small function to return the final villager in all pertinent pages (1-8 and D)
				code += "20 2f f3 4c 4d ec";
				bts = strToBytes(code);
				s = page + 16280; //addr + 3f98  (16 byte shift)
				for(int i = 0; i < bts.length; i++)
					romData[s + i] = bts[i];
			}
		}
		//start the array at fea3 - this array has room for 40 swaps
		int arr = 65187;
		if((mode & 1) == 1)  //put the spell teacher villager swaps in (9 swaps)
		{
			String ssrc = "08 b2 09 ee 0a 85 0a eb 04 c6 08 63 09 bf 0e ee 0d dc";
			String sdest = "08 b2 ba 06 09 ee ab 02 0a 85 bb 07 0a eb b8 03 04 c6 af 05 08 63 a5 07 09 bf a9 01 0e ee ab 02 0d dc a8 05";
			arr = insertVillagerSwapEntries(arr, ssrc, sdest, 0);
		}
		if((mode & 2) == 2)  //put the shop swaps in (17 swaps)
		{
			int a1 = arr;
			String ssrc = "06 dc 04 32 05 b2 08 1d 04 ac ";
			String sdest = "06 dc a8 05 04 32 a3 08 05 b2 ba 06 08 1d b6 06 04 ac b9 08 ";
			ssrc += "05 3e 04 4b 05 fe 05 1d 04 85 ";
			sdest += "05 3e a4 03 04 4b a6 04 05 fe ad 06 05 1d b6 06 04 85 bb 07 ";
			ssrc += "06 ac 07 3e 06 fe 06 1d ";
			sdest += "06 ac b9 08 07 3e a4 03 06 fe ad 06 06 1d b6 06 ";
			ssrc += "04 b2 07 85 07 1d";
			sdest += "04 b2 ba 06 07 85 bb 07 07 1d b6 06";
			arr = insertVillagerSwapEntries(arr, ssrc, sdest, val);
			//find where B.Den's herb shop village is and replace the check
			//find where Vesper's guild is and replace the check
			replaceChecksForSecretSale(a1);
		}
	}
	
	private void replaceChecksForSecretSale(int a1)
	{
		String oldIdx = "08 07";
		byte[] oib = strToBytes(oldIdx);
		String oldLoc = "1d 85";
		byte[] olb = strToBytes(oldLoc);
		String newLoc = "dc 32 b2 1d ac 3e 4b fe 85";  //Moonglow, Sk.Brae, Paws, B.Den, Britain, Jhelom, Minoc, Trinsic, Vesper
		byte[] nlb = strToBytes(newLoc);
		String replaceWith = "05 0b 10 0d 06 07 09 0a 0e";
		byte[] rwb = strToBytes(replaceWith);
		int[] rLoc = {16204, 16158};  //3f4c, 3f1e
		for(int k = 0; k < 2; k++)
		{
			for(int i = 0; i < (6 * 17); i += 6)  //17 shop entries
			{
				if(romData[a1 + i + 2] == oib[k] && romData[a1 + i + 3] == olb[k])
				{
					byte c = romData[a1 + i + 1];
					for(int j = 0; j < nlb.length; j++)
					{
						if(c == nlb[j])
						{
							//replace
							byte d = rwb[j];
							romData[rLoc[k]] = d;
							break;
						}
					}
					break;
				}
			}
		}
	}
	
	private int insertVillagerSwapEntries(int atLoc, String ssrc, String sdest, int mode)
	{
		byte[] sbs = strToBytes(ssrc);
		byte[] sbd = strToBytes(sdest);
		int nVils = sbs.length / 2;
		ArrayList<Integer> ints = new ArrayList<Integer>();
		byte[][] src = new byte[nVils][];
		byte[][] dest = new byte[nVils][];
		for(int i = 0; i < nVils; i++)
		{
			ints.add(i);
			src[i] = new byte[2];
			dest[i] = new byte[4];
		}
		
		
		for(int i = 0; i < sbs.length; i += 2)
		{
			src[i / 2][0] = sbs[i];
			src[i / 2][1] = sbs[i + 1];
		}
		
		
		for(int i = 0; i < sbd.length; i += 4)
		{
			for(int j = 0; j < 4; j++)
				dest[i / 4][j] = sbd[i + j];
		}
		int k = 0;
		int arrayStart = atLoc;
		while(ints.size() > 0)
		{
			int r = (int) (UltimaRando.rand() * ints.size());
			int s = ints.remove(r);
			for(int i = 0; i < src[k].length; i++)
			{
				romData[atLoc] = src[k][i];
				atLoc++;
			}
			for(int i = 0; i < dest[s].length; i++)
			{
				romData[atLoc] = dest[s][i];
				atLoc++;
			}
			k++;
		}
		if(mode == 0)
			return atLoc;
		else if(mode == 1)
		{
			enforceNonCompete(arrayStart);
			return atLoc;
		}
		else
		{
			enforceSameShop(arrayStart);
			return atLoc;
		}
	}
	
	private void enforceSameShop(int arrayStart) 
	{
		int[] shopTypes = {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3};
		for(int i = 0; i < shopTypes.length; i++)
		{
			int l3 = arrayStart + (i * 6) + 2;
			int l4 = l3 + 1;
			int sh = getShopType(romData[l3], romData[l4]);
			if(sh != shopTypes[i])
			{
				for(int j = i + 1; j < shopTypes.length; j++)
				{
					int l1 = arrayStart + (j * 6) + 2;
					int l2 = l1 + 1;
					int s1 = getShopType(romData[l1], romData[l2]);
					if(s1 == shopTypes[i])
					{
						shopSwap(i, j, arrayStart);
						break;
					}
				}
			}
		}
	}

	private void enforceNonCompete(int arrayStart) 
	{
		int[][] shopLocs = {
				{3, 8, 13, 16},
				{2, 14}, {5, 11}, {7, 12},
				{4, 10}, {9, 15}};
		ArrayList<Integer> unique = new ArrayList<Integer>();
		ArrayList<Boolean> avail = new ArrayList<Boolean>();
		for(int i = 0; i < 17; i++)
			avail.add(true);
		for(int i = 0; i < shopLocs.length; i++)
		{
			unique.clear();
			for(int j = 0; j < shopLocs[i].length; j++)
				avail.set(shopLocs[i][j], false);
			for(int j = 0; j < shopLocs[i].length; j++)
			{
				int l1 = arrayStart + (shopLocs[i][j] * 6) + 2;
				int l2 = l1 + 1;
				int s1 = getShopType(romData[l1], romData[l2]);
				if(unique.contains(s1))  //need to swap
				{
					int fail = 0;
					while(fail < 100)
					{
						int r = (int) (UltimaRando.rand() * avail.size());
						while(true)
						{
							if(avail.get(r) == true)
								break;
							r++;
							if(r >= avail.size())
								r = 0;
						}
						int l5 = arrayStart + (r * 6) + 2;
						int l6 = l5 + 1;
						int s3 = getShopType(romData[l5], romData[l6]);
						if(unique.contains(s3))
							fail++;
						else
						{
							//do the swap and break
							shopSwap(shopLocs[i][j], r, arrayStart);
							s1 = getShopType(romData[l1], romData[l2]);
							break;
						}
						if(fail == 100)
						{
							//at this point you failed to find a swap
							if(i != shopLocs.length - 1)  //just keep trying
							{
								fail = 0;
							}
							else  //weapon shop lock on the last town - swap with any other 2 shop town
							{
								r = 0;
								boolean[] av = new boolean[17];
								for(int k = 1; k < 5; k++)
								{
									for(int l = 0; l < shopLocs[k].length; l++)
										av[shopLocs[k][l]] = true;
								}
								r = (int) (UltimaRando.rand() * 17);
								while(true)
								{
									if(av[r] == true)
										break;
									r++;
									if(r >= av.length)
										r = 0;
								}
								shopSwap(shopLocs[i][j], r, arrayStart);
								s1 = getShopType(romData[l1], romData[l2]);
								break;
							}
						}
					}
				}
				unique.add(s1);
			}
		}
	}
	
	public String listShops(int arrayStart)
	{
		int[] shopTypes = new int[17];
		for(int i = 0; i < 17; i++)
		{
			int l3 = arrayStart + (i * 6) + 2;
			int l4 = l3 + 1;
			shopTypes[i] = getShopType(romData[l3], romData[l4]);
		}
		String[] towns = {"Moonglow", "Skara Brae", "Paws", "Bucaneer's Den", "Britain", "Jhelom", "Minoc", "Trinsic", "Vesper"};
		int[] shopAt = {0, 1, 2, 3, 4, 5, 6, 7, 3, 8, 4, 5, 7, 3, 2, 8, 3};
		for(int i = 0; i < 17; i++)
			towns[shopAt[i]] += " " + shopTypes[i];
		String rv = "";
		for(int i = 0; i < towns.length; i++)
			rv += towns[i] + "\n";
		return rv;
	}
		
	private void shopSwap(int e1, int e2, int arrayStart)
	{
		int l1 = arrayStart + (6 * e1) + 2;
		int l2 = arrayStart + (6 * e2) + 2;
		byte[] old = this.getData(l1, l1 + 3);
		for(int i = 0; i < 4; i++)
		{
			romData[l1 + i] = romData[l2 + i];
			romData[l2 + i] = old[i];
		}
	}
	
	private int getShopType(byte vil, byte town)  //0:herb, 1:weapon, 2:armor, 3:guild
	{
		String ts = Integer.toHexString(town & 255);
		String[][] in = {
				{"32","ac", "4b", "85", "b2"},
				{"b2", "3e", "fe", "1d"},
				{"dc", "ac", "fe", "1d"},
				{"3e", "85", "1d"},
				{"1d"}};
		int[][] out = {
				{0, 1, 1, 1, 2},
				{0, 1, 1, 1},
				{0, 2, 2, 2},
				{2, 3, 3},
				{0}};
		int vv = vil - 4;
		for(int i = 0; i < in[vv].length; i++)
			if(ts.equals(in[vv][i]))
				return out[vv][i];
		return -1;
	}

	public byte[] getInitItems()
	{
		byte[] rv = new byte[16];
		for(int i = 0; i < 8; i++)
		{
			rv[i * 2] = romData[218685 + 2 * i];
			rv[i * 2 + 1] = romData[218686 + 2 * i];
		}
		return rv;
	}
	
	public byte[] getInitEquippables()
	{
		return getData(3956, 3980);
	}
	
	public String getInitCharData()
	{
		String s = "";
		//gold: 35551,35556
		int gold = btoi(romData[218449]) + (btoi(romData[218454]) * 256);
		s += "GP:" + gold + "\n";
		int[] ks = new int[4];
		s += "Known spells:";
		int[] ksLocs = {218459,0,0,218468};
		String[][] spells = {{"Light", "Missile", "Awaken", "Cure", "Wind", "Heal", "Fire", "Exit"},
				             {"Dispel", "View", "Protect", "Ice","Blink","Energy","Quick",""},
				             {"Sleep","Reflect","Negate","","Destroy","Jinx","Squish","Gate"},
				             {"Tremor", "Life","","Defeat","","","","",""}};
		String spellList = "";
		for(int i = 0; i < 4; i++)
		{
			if(i == 0 || i == 3)
				ks[i] = btoi(romData[ksLocs[i]]);
			else if(i == 1)
			{
				byte op = romData[218463];
				if(op == 74)  //4a? shift
					ks[i] = (ks[0] >> 1);
				else if(op == -87)  //a9? lda
				{
					ks[i] = romData[218464];
					ksLocs[3]++;
				}
			}
			else
				ks[i] = 0;
			String sa = Long.toBinaryString(ks[i]);
			while(sa.length() < 8)
				sa = "0" + sa;
			s += "     " + sa;
			for(int j = 0; j < 8; j++)
			{
				if(((ks[i] >> j) & 1) == 1)
					spellList += spells[i][j] + ",";
			}
		}
		s += "Spell list:\n";
		s += spellList;
		int virt = btoi(romData[218482]);
		s += "\nAll virtues begin at " + virt + "\n";
		String[] charName = {"Mariah", "Iolo", "Geoff", "Jaana", "Julius", "Dupre", "Shimano", "Katrina"};
		for(int i = 0; i < 8; i++)
		{
			int hp = btoi(romData[218613 + i * 2]) + 256 * btoi(romData[218614 + i * 2]);
			int mhp = btoi(romData[218613 + i * 2]) + 256 * btoi(romData[218614 + i * 2]);
			int exp = btoi(romData[218661 + i * 2]) + 256 * btoi(romData[218662 + i * 2]);
			int mp = btoi(romData[218629 + i]);
			int mmp = btoi(romData[218629 + i]);
			int level = btoi(romData[218605 + i]);
			int str = btoi(romData[218637 + i]);
			int intel = btoi(romData[218645 + i]);
			int dex = btoi(romData[218653 + i]);
			int[] inventory = {btoi(romData[218685 + i * 2]), btoi(romData[218686 + i * 2]), 0, 0, 0, 0};
			String d = charName[i] + "'s stats: LV " + level + " HP " + hp + "/" + mhp + " MP " + mp + "/" + mmp + " STR " + str + " INT " + intel + " DEX " + dex + " EXP " + exp;
			for(int j = 0; j < inventory.length; j++)
			{
				d += "  I" + j + ":";
				if(inventory[j] > 128)
				{
					inventory[j] -= 128;
					d += "<eq>";
				}
				d += inventoryList.get(inventory[j]) + " ";
			}
			s += d + "\n";
		}
		s += "Herbs: ";
		String[] reagents = {"Ash", "Ginseng", "Garlic", "Silkweb", "Moss", "Pearl", "Fungus", "Manroot"};
		for(int i = 0; i < 8; i++)
		{
			int amt = btoi(romData[218677 + i]);
			s += reagents[i] + ":" + amt + "  ";
		}
		
		s += "\nEquippable Items:\n";
		String[] wpar = {"Sling", "Bow", "XBow", "+1 Bow", "Dagger", "Staff", "Club", "Axe", "Sword", "+1 Sword", "+2 Sword",
				"+1 Axe", "Wand", "+2 Axe", "Sword of Paradise", "Cloth", "Leather", "Chain", "Ring", "+1 Cloth", "Plate", "+1 Chain",
				"+1 Plate", "Robe", "Exotic Armor"};
		for(int i = 0; i < wpar.length; i++)
		{
			int eby = btoi(romData[3956 + i]);
			s += String.format("%-20s", wpar[i]);
			for(int j = 0; j < charName.length; j++)
			{
				if((eby & (1 << j)) > 0)
					s += String.format("%-10s", charName[j]);
			}
			if(eby == 0)
				s += "Avatar Only";
			s += "\n";
		}
		s += "\nWhirlpool location: " + btoi(romData[218603]) + "," + btoi(romData[218604]) + "    ";
		s += "Balloon location: " + btoi(romData[218601]) + "," + btoi(romData[218602]);
		return s;
	}
	
	//changeCharData
	
	public byte[] getDungeonBlock()
	{
		int start = 147472;
		Dungeon[] dungeons = UltimaRando.constructAllDungeons(this);
		int end = dungeons[7].input - 1;
		return this.getData(start, end);
	}
	
	public static void freeSpaceAnalysis(NESRom rom)
	{
		double avg = 0.0;
		int min = Integer.MAX_VALUE;
		int max = 0;
		int nTests = 10000;
		HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
		for(int k = 0; k < nTests; k++)
		{
			Dungeon[] newDs = UltimaRando.randomizeDungeons(rom);
			byte[] oldBlock = rom.getDungeonBlock();
			byte[] newBlock = new byte[oldBlock.length];
			int nbw = 2;
			//put the old data ptr first
			newBlock[0] = oldBlock[16];
			newBlock[1] = oldBlock[17];
			byte[][] newBlocks = UltimaRando.encodeAllDungeons(rom, newDs);
			byte[] combinedTree = UltimaRando.newTree;
			/*int nsz = 2 + combinedTree.length + 7 + 27 + 9 + 20;
			for(int i = 0; i < newBlocks.length; i++)
				nsz += newBlocks[i].length;*/
			//then put the tree
			for(int i = 0; i < combinedTree.length; i++)
			{
				newBlock[nbw] = combinedTree[i];
				nbw++;
			}
			byte lStart = (byte) nbw;
			//then put the dungeon lengths of deceit - hythloth
			for(int i = 0; i < newBlocks.length - 1; i++)
			{
				newBlock[nbw] = (byte) newBlocks[i].length;
				nbw++;
			}
			byte bstart = (byte) nbw;
			//now dump the blocks
			for(int i = 0; i < newBlocks.length; i++)
			{
				for(int j = 0; j < newBlocks[i].length; j++)
				{
					newBlock[nbw] = newBlocks[i][j];
					nbw++;
				}
			}
			int free = 0;
			while(nbw < newBlock.length)
			{
				free++;
				nbw++;
			}
			avg += free;
			if(free < min)
				min = free;
			if(free > max)
				max = free;
			if(count.get(free) != null)
				count.put(free, count.get(free) + 1);
			else
				count.put(free, 1);
		}
		avg /= nTests;
		System.out.println("Free space after new encoding: max=" + max + " min=" + min + " avg=" + avg);
		int a = 0;
		while(a <= max)
		{
			if(count.get(a) != null)
				System.out.println(a + ":" + count.get(a));
			a++;
		}
	}
	
	public int changeDungeonBlock(Dungeon[] newDs) throws Exception  //uses ff78-ffa0 + ffc3-ffd0
	{
		byte[] oldBlock = getDungeonBlock();
		byte[] newBlock = new byte[oldBlock.length];
		int nbw = 2;
		//put the old data ptr first
		newBlock[0] = oldBlock[16];
		newBlock[1] = oldBlock[17];
		byte[][] newBlocks = UltimaRando.encodeAllDungeons(this, newDs);
		byte[] combinedTree = UltimaRando.newTree;
		/*int nsz = 2 + combinedTree.length + 7 + 27 + 9 + 20;
		for(int i = 0; i < newBlocks.length; i++)
			nsz += newBlocks[i].length;*/
		//then put the tree
		for(int i = 0; i < combinedTree.length; i++)
		{
			newBlock[nbw] = combinedTree[i];
			nbw++;
		}
		byte lStart = (byte) nbw;
		//then put the dungeon sizes of deceit - hythloth
		for(int i = 0; i < newBlocks.length - 1; i++)
		{
			newBlock[nbw] = (byte) newBlocks[i].length;
			nbw++;
		}
		byte bstart = (byte) nbw;
		//now dump the blocks that contain dungeon data
		for(int i = 0; i < newBlocks.length; i++)
		{
			for(int j = 0; j < newBlocks[i].length; j++)
			{
				newBlock[nbw] = newBlocks[i][j];
				nbw++;
			}
		}
		int abyssEnd = nbw - 1;
		//add in some functionality
		int memBankStart = 147472;
		
			
		int code2 = nbw + memBankStart;  //code to properly load dungeons from the one combined huffman tree (dungeon data offset accumulator)
		String hLoad = "68 68 a9 2b 85 40 a5 4b e9 80 a8 30 0e 18 b9 " + 
		               Integer.toHexString(lStart) + " 80 65 40 90 01 e8 85 40 88 10 f2 86 41 c8 84 16 20 d9 e5 20 00 00 60";
		byte[] dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
		{
			newBlock[nbw] = dsl[i];
			nbw++;
		}
		int postProcess = memBankStart + nbw - 4;
		//dump the new block
		for(int i = 0; i < newBlock.length; i++)
			romData[memBankStart + i] = newBlock[i];
		//System.out.println("Old block size = " + oldBlock.length);
		//System.out.println("New block size = " + nsz);
		
		//System.out.println("Here you have 360 free bytes");
		int memBankEnd = 163528;
		int code1 = 1 + memBankEnd;  //code to load stone room data into 06d1-06d9
		String dStoneLoad = "c9 81 30 10 a8 f0 0d 88 a9 07 38 e5 0e aa 98 9d d1 06 a9 32 20 34 e6 60";
		dsl = strToBytes(dStoneLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[code1 + i] = dsl[i];
		
		int code3 = code1 + dsl.length;  //method to copy stone room data from $06dx to $06d0 when loading floor
		hLoad = "a8 bd d1 06 8d d0 06 98 20 39 95 60";
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
		{
			romData[code3 + i] = dsl[i];
			nbw++;
		}
		
		//dungeon floor updater (sets the stone room 06d0 to the proper number according to the
		//floor you enter
		int code4 = code3 + dsl.length;
		//String dStoneEnter = "20 ba e5 ae c1 06 bd d1 06 8d d0 06 60";  //old
		String dStoneEnter = "ae c1 06 bd d1 06 8d d0 06 60";
		dsl = strToBytes(dStoneEnter);
		for(int i = 0; i < dsl.length; i++)
			romData[code4 + i] = dsl[i];
		
		//put the deceit stair table here
		int stair1 = code4 + dsl.length;
		byte[] deceitStairs = newDs[0].floorTable;
		for(int i = 0; i < deceitStairs.length; i++)
			romData[stair1 + i] = deceitStairs[i];
		
		//System.out.println("DeceitStairs is " + deceitStairs.length + " bytes");
		
		//now dump all the other stair tables 
		int stairX = newDs[0].floorTableLoc;
		int stairW = 0;
		newDs[0].floorTableLoc = stair1;
		for(int i = 1; i < newDs.length; i++)
		{
			newDs[i].floorTableLoc = stairX + stairW;
			for(int j = 0; j < newDs[i].floorTable.length; j++)
			{
				romData[stairX + stairW] = newDs[i].floorTable[j];
				stairW++;
			}
		}
		
		//update stair table pointers
		int romPage = 147472;
		int p1 = 154853;
		int p2 = 154861;
		for(int i = 0; i < newDs.length; i++)
		{
			newDs[i].floorTablePtr = p1;
			int ramLoc = newDs[i].floorTableLoc - romPage;
			ramLoc += 32768;
			String s1 = Integer.toHexString(ramLoc);
			String s2[] = new String[2];
			s2[0] = s1.substring(0, 2);
			s2[1] = s1.substring(2, 4);
			romData[p1 + i] = (byte) Integer.parseInt(s2[1], 16);
			romData[p2 + i] = (byte) Integer.parseInt(s2[0], 16);
		}
		
		//now dump all the combat room tables and update pointer
		stairX = 155844;
		int stairP = 155828;
		stairW = 0;
		//int tablePtr = newDs[0].encounterRoomTablePtr;
		for(int i = 0; i < newDs.length; i++)
		{
			newDs[i].encounterRoomTablePtr = stairP + 2 * i;
			newDs[i].encounterRoomTableLoc = stairX + stairW;
			String ert = Integer.toHexString(newDs[i].encounterRoomTableLoc - romPage + 32768);
			String[] e2 = new String[2];
			e2[0] = ert.substring(0, 2);
			e2[1] = ert.substring(2, 4);
			romData[newDs[i].encounterRoomTablePtr] = (byte) Integer.parseInt(e2[1], 16);
			romData[newDs[i].encounterRoomTablePtr + 1] = (byte) Integer.parseInt(e2[0], 16);
			for(int j = 0; j < newDs[i].encounterRoomTable.length; j++)
			{
				romData[stairX + stairW] = newDs[i].encounterRoomTable[j];
				stairW++;
				//tablePtr++;
			}
		}
		//now do all sorts of code editing
		String[] cdx = new String[2];
		int hcode = 0;
		
		//add in a load dungeon selector and put it at 0x3ff88; it calls code2; uses ff78-ff8b
		String cd2 = Integer.toHexString(code2 - romPage + 32768);
		cdx[0] = cd2.substring(0, 2);
		cdx[1] = cd2.substring(2, 4);
		hLoad = "4c "+ cdx[1] + " " + cdx[0];
		hcode = 262024;
		hLoad = "a5 4b c9 80 30 07 c9 88 10 03 " + hLoad + " 20 a3 e5 60";
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		int l1 = dsl.length;
		
		//point line E5D6 (3e5e6) to call dungeon selector at ff78
		int romPage2 = 245776;
		String cddl = Integer.toHexString(hcode - romPage2 + 49152);  //should be "ff78"
		cdx[0] = cddl.substring(0, 2);
		cdx[1] = cddl.substring(2, 4);
		hLoad = "20 "+ cdx[1] + " " + cdx[0];
		hcode = 255462;
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		
		//put dungeon selector2 after dungeon selector1 to call code1; uses ff8c-ffa0
		hcode = 262024;
		int code5 = hcode + l1;
		String cd1 = Integer.toHexString(code1 - romPage + 32768);
		cdx[0] = cd1.substring(0, 2);
		cdx[1] = cd1.substring(2, 4);
		String dungTest2 = "a8 a5 4b c9 80 30 09 c9 88 10 05 98 20 " + cdx[1] + " " + cdx[0] + " 60 98 20 34 e6 60";
		//                  a8 a5 4b c9 80 30 09 c9 88 10 05 98 20 -- --                         60 98 20 34 e6 60
		dsl = strToBytes(dungTest2);
		for(int i = 0; i < dsl.length; i++)
			romData[code5 + i] = dsl[i];
		
		//put in dungeon load selector at ffc3 to call code4
		/*hcode = 262099;
		int code6 = hcode;
		String cd4 = Integer.toHexString(code4 - romPage + 32768);
		cdx[0] = cd4.substring(0, 2);
		cdx[1] = cd4.substring(2, 4);
		String dungTest3 = "a5 4c c9 09 d0 04 20 " + cdx[1] + " " + cdx[0] + " 60 20 bc e6 60";
		dsl = strToBytes(dungTest3);
		for(int i = 0; i < dsl.length; i++)
			romData[code6 + i] = dsl[i];*/
		
		
		//point line E600 (3e610) to call dungeon selector 2
		cd1 = Integer.toHexString(code5 - romPage2 + 49152);
		cdx[0] = cd1.substring(0, 2);
		cdx[1] = cd1.substring(2, 4);
		hLoad = "20 "+ cdx[1] + " " + cdx[0];
		hcode = 255504;
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		
		//point line "Post process" in code2 to call code4
		//code4 % 16384 + 32752
		cd1 = Integer.toHexString(code4 % 16384 + 32752);   
		cdx[0] = cd1.substring(0, 2);
		cdx[1] = cd1.substring(2, 4);
		hLoad = "20 "+ cdx[1] + " " + cdx[0];
		hcode = postProcess;
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		
		//point line 94FD (2550d) and 9516 (25526) to call code3
		String cd3 = Integer.toHexString(code3 - romPage + 32768);
		cdx[0] = cd3.substring(0, 2);
		cdx[1] = cd3.substring(2, 4);
		hLoad = "20 "+ cdx[1] + " " + cdx[0];
		hcode = 152845;  //2550d
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		hcode = 152870; //25526
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		
		//change e5c5-e5c7 (3e5b7-3e5ba) - makes it point to my one huffman tree (+value swap)
		hcode = 255445;
		hLoad = "a2 80 a9 02 ea 85 06 86 07";
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i]; 
		
		//point future code to look at the new data
		/*hcode = 259021;
		hLoad = "bd f0 7f bc f1 7f";
		dsl = strToBytes(hLoad);
		for(int i = 0; i < dsl.length; i++)
			romData[hcode + i] = dsl[i];*/
		
		//and finally have the stone room read $06d0 when loading
		romData[60742] = -48;  //d0  load 06d0  when selecting a stone room
		romData[60729] = -48;  //d0  compare to 06d0 when testing which stone room
		romData[60697] = -48;  //d0  load 06d0 when deciding which stone to put in stone room #1
		romData[60703] = -48;  //d0  #2
		
		//and to avoid a psychodelic experience, have the palette loader go to the proper location
		romData[255680] = 0;
		
		//testing
		/*for(int i = 0; i < newDs.length; i++)
		{
			int bStart = bstart;
			int bEnd = bstart;
			for(int j = i - 1; j >= 0; j--)
				bStart += (newBlock[lStart + j] & 255);
			if(i < 7)
				bEnd = bStart + (newBlock[lStart + i] & 255) - 1;
			else
				bEnd = abyssEnd;
			byte[] encoded = this.getData(memBankStart + bStart, memBankStart + bEnd);
			if(encoded.length != newBlocks[i].length)
				System.out.println("Wrong encoded length; expected=" + newBlocks[i].length + " actual=" + encoded.length);
			for(int k = 0; k < encoded.length; k++)
			{
				if(newBlocks[i][k] != encoded[k])
					System.out.println("Wrong encoded value at #" + k + ";  expected=" + newBlocks[i][k] + "  actual=" + encoded[k]);
			}
			newDs[i].setTreeData(combinedTree);
			byte[] decoded = newDs[i].decode(encoded);
			byte[] dec2 = newDs[i].decode(newBlocks[i]);
			System.out.println(newDs[i].getLayout(decoded));
		}
		
		NESRom orom = new NESRom("C:\\Users\\AaronX\\Desktop\\Ultima - Quest of the Avatar (U).nes");  //the golden rom
		Dungeon[] orig = UltimaRando.loadDungeons(orom);
		for(int i = 0; i < 8; i++)
			orig[i].allFloors = orig[i].calcDecodedFloors(null, orig[i].floorTable, orig[i].encounterRoomTable);*/
		//combat room tests
		/*for(int i = 0; i < newDs.length; i++)
		{
			
			for(int j = 0; j < newDs[i].allFloors.length; j++)
			{
				Floor fp = newDs[i].allFloors[j];
				int origFloorNum = fp.origFloor;
				int origDungeonNum = fp.origDIndex + 127;
				Floor fo = orig[origDungeonNum].allFloors[origFloorNum];
				String s = "";
				s += "\nnew=";
				for(int k = 0; k < fp.combatRooms.length; k++)
					s += fp.combatRooms[k] + ",";
				s += "\norg=";
				for(int k = 0; k < fp.combatRooms.length; k++)
					s += fo.combatRooms[k] + ",";
				boolean giveInfo = false;
				for(int k = 0; k < fp.combatRooms.length; k++)
				{
					if(fp.combatRooms[k] != fo.combatRooms[k])
					{
						giveInfo = true;
						s += "\nstorage difference in #" + k;
					}
				}
				if(giveInfo)
					System.out.println(s);
			}
		}*/
		
		//stair table tests
		/*for(int i = 0; i < newDs.length; i++)
		{
			//System.out.println("Stair testing " + newDs[i].name);
			newDs[i].testStairTable();
		}
		*/
		return postProcess;
	}
	
	HashMap<Integer,String> inventoryList;
	private void setupInventoryList()
	{
		inventoryList = new HashMap<Integer,String>();
		String[] items = {"Sling", "Bow", "X-Bow", "+1 Bow", "Dagger", "Staff", "Club", "Axe", "Sword", "+1 Sword", "+2 Sword",
				          "+1 Axe", "Wand", "+2 Axe", "Sword of Paradise", "Cloth", "Leather", "Chain", "Ring", "+1 Cloth",
				          "Plate", "+1 Chain", "+1 Plate", "Robe", "Exotic Armor"};
		
		for(int i = 0; i < items.length; i++)
		{
			inventoryList.put(i + 1, items[i]);
		}
	}
	
	public byte[] strToBytes(String in)
	{
		in = in.toLowerCase();
		String[] all = in.split(" ");
		byte[] rtnVal = new byte[all.length];
		for(int i = 0; i < all.length; i++)
		{
			int vv = Integer.parseInt(all[i], 16);
			byte bb = (byte) vv;
			rtnVal[i] = bb;
			//rtnVal[i] = Byte.parseByte(all[i], 16);
		}
		return rtnVal;
	}
	
	public String bytesToStr(byte[] in)
	{
		String rtnVal = "";
		for(int i = 0; i < in.length; i++)
		{
			String ss = Integer.toHexString(in[i]);
			if(ss.length() == 1)
				ss = "0" + ss;
			ss = ss.substring(ss.length() - 2, ss.length());
			rtnVal += ss + " ";
		}
		return rtnVal;
	}
	
	public void findMatch(String dfname) throws Exception
	{
		byte[] dumpData = loadData(dfname);
		findMatch(dumpData);
	}
	
	public void findMatch(byte[] toMatch)
	{
		int matched = 0;
		System.out.println("Searching for " + bytesToStr(toMatch));
		for(int i = 0; i < romData.length; i++)
		{
			if(romData[i] == toMatch[matched])
				matched++;
			else
				matched = 0;
			if(matched == toMatch.length)
			{
				System.out.println("Match at " + Integer.toHexString(i - matched) + " - " + Integer.toHexString(i));
				matched = 0;
			}
		}
	}
}

class FloorZone
{
	ArrayList<Point> points;
	ArrayList<Point> upStairs;
	ArrayList<Point> downStairs;
	long marks;
	
	FloorZone(ArrayList<Point> points, ArrayList<Point> upStairs, ArrayList<Point> downStairs)
	{
		this.points = points;
		this.upStairs = upStairs;
		this.downStairs = downStairs;
		marks = 0;
	}
}

class Floor
{
	byte[] layout;
	ArrayList<FloorZone> zones;
	String origDName;
	byte origDIndex;
	int origFloor;
	byte[] combatRooms;
	byte[] stairTable;
	ArrayList<Point> upStairs;
	ArrayList<Point> downStairs;
	
	/*ArrayList<Point>[] zones;
	ArrayList<Point> upStairs;
	ArrayList<Point> downStairs;*/
	
	Floor(byte[] floorData, String name, int di, int fn)
	{
		layout = floorData;
		origDName = name;
		origDIndex = (byte) di;
		origFloor = fn;
		enableMultiStone();
		calcZones();
		/*System.out.println(outputZoneData());
		System.out.println("Done zoning " + name + " Floor #" + (fn+1) + "; it has " + zones.size() + " zones.");
		System.out.print("  ");
		for(int j = 0; j < zones.size(); j++)
		{	
			FloorZone z = zones.get(j);
			System.out.print("   Z#" + j + ": Pts=" + z.points.size() + "  UpS=" + z.upStairs.size() + "  DnS=" + z.downStairs.size());
		}
		System.out.println("");*/
	}
	
	private void enableMultiStone()
	{
		for(int i = 0; i < layout.length; i++)
			if(layout[i] == 50)  //0x32
				layout[i] = origDIndex;
	}
	
	public void setCombatRooms(byte[] rooms)
	{
		combatRooms = rooms;
	}
	
	public void setInitStairTable(byte[] stairs)
	{
		stairTable = stairs;
	}
	
	public String printStairTable()
	{
		String s = "";
		for(int i = 0; i < stairTable.length; i++)
			s += Integer.toHexString(stairTable[i] & 255) + " ";
		return s;
	}
	
	
	
	public String stairMappingTest(ArrayList<Point> ups, ArrayList<Point> downs)
	{
		//test the layout for steps
		String rv = "";
		for(int i = 0 ; i < layout.length; i++)
		{
			byte b = layout[i];
			int x = i & 7;
			int y = i >> 3;
			boolean up = false;
			if(b == 1 || b == 3)
				up = true;
			boolean down = false;
			if(b == 2 || b == 3)
				down = true;
			boolean upFound = false;
			for(int j = 0; j < ups.size(); j++)
			{
				Point p = ups.get(j);
				if(p.x == x && p.y == y)
				{
					upFound = true;
					break;
				}
			}
			boolean downFound = false;
			for(int j = 0; j < downs.size(); j++)
			{
				Point p = downs.get(j);
				if(p.x == x && p.y == y)
				{
					downFound = true;
					break;
				}
			}
			if(up & !upFound)
				rv += "Stair list failed to contain up stair at " + x + "," + y + "\n";
			if(upFound & !up)
				rv += "Stair list contains up stair that was not found at " + x + "," + y + "\n";
			if(down & !downFound)
				rv += "Stair list failed to contain down stair at " + x + "," + y + "\n";
			if(downFound && !down)
				rv += "Stair list contains down stair that was not found at " + x + "," + y + "\n";
		}
		return rv;
	}
	
	public void redoStairTable(Floor upper)
	{
		ArrayList<Byte> data = new ArrayList<Byte>();
		ArrayList<Point> ups = new ArrayList<Point>();
		ArrayList<Point> downs = new ArrayList<Point>();
		ArrayList<Integer> dindex = new ArrayList<Integer>();
		for(int i = 0; i < zones.size(); i++)
		{
			for(int j = 0; j < zones.get(i).upStairs.size(); j++)
			{
				Point p = zones.get(i).upStairs.get(j);
				ups.add(p);
				byte aa = (byte) (p.y * 16 + p.x);
				if(!(layout[p.y * 8 + p.x] == 1 || layout[p.y * 8 + p.x] == 3))
					System.out.println("There is not an up stair at " + p.x + "," + p.y + " (=" + layout[p.y * 8 + p.x]);
				data.add(aa);
				data.add((byte) 0);
			}
		}
		for(int i = 0; i < zones.size(); i++)
		{
			for(int j = 0; j < zones.get(i).downStairs.size(); j++)
			{
				Point p = zones.get(i).downStairs.get(j);
				downs.add(p);
				byte aa = (byte) (p.y * 16 + p.x);
				if(!(layout[p.y * 8 + p.x] == 2 || layout[p.y * 8 + p.x] == 3))
					System.out.println("There is not a down stair at " + p.x + "," + p.y + " (=" + layout[p.y * 8 + p.x]);
				boolean addMe = true;
				for(int k = 0; k < data.size(); k += 2)
				{
					if(data.get(k) == aa)
					{
						dindex.add(k + 1);
						addMe = false;
					}
				}
				if(addMe)
				{
					dindex.add(data.size() + 1);
					data.add(aa);
					data.add((byte) 0);
				}
			}
		}
		upStairs = new ArrayList<Point>();
		downStairs = new ArrayList<Point>();
		upStairs.addAll(ups);
		downStairs.addAll(downs);
		stairMappingTest(upStairs, downStairs);
		int ord = 0;
		while(ups.size() > 0)
		{
			int ii = 0;
			//find ord in upper indexes
			if(upper != null)
			{
				for(int j = 0; j < upper.stairTable.length; j += 2)
				{
					byte xy = upper.stairTable[j];
					int xx = xy & 15;
					int yy = (xy >> 4) & 15;
					boolean goesDown = false;
					for(int k = 0; k < upper.downStairs.size(); k++)
					{
						Point p = upper.downStairs.get(k);
						if(p.x == xx && p.y == yy)
						{
							goesDown = true;
							break;
						}
					}
					byte u = upper.stairTable[j + 1];
					u &= 7;
					if(u == ord && goesDown)
						break;
					ii++;
				}
			}
			byte b = data.get(2 * ord + 1);
			if(upper == null)
				b += (15 << 4);
			else
				b += (ii << 4);
			data.set(2 * ord + 1, b);
			ups.remove(0);
			ord++;
		}
		ord = 0;
		while(downs.size() > 0)
		{
			double r = UltimaRando.rand() * downs.size();
			int ii = (int) r;
			Point p = downs.remove(ii);
			int ind = dindex.remove(ii);
			byte b = data.get(ind);
			b += (ord);
			data.set(ind, b);
			ord++;
		}
		byte[] rv = new byte[data.size()];
		for(int i = 0; i < rv.length; i++)
			rv[i] = data.get(i);
		stairTable = rv;
	}
	
	public int getUpCount()
	{
		int rv = 0;
		for(int i = 0; i < zones.size(); i++)
			rv += zones.get(i).upStairs.size();
		return rv;
	}
	
	public int getDownCount()
	{
		int rv = 0;
		for(int i = 0; i < zones.size(); i++)
			rv += zones.get(i).downStairs.size();
		return rv;
	}
	
	public Floor copyFloor()
	{
		
		byte[] layout2 = Arrays.copyOf(layout, layout.length);
		//for(int i = 0; i < layout.length; i++)
			//layout2[i] = layout[i];
		Floor nf = new Floor(layout2, origDName, origDIndex, origFloor);
		if(combatRooms != null)
			nf.combatRooms = combatRooms;
		if(stairTable != null)
			nf.stairTable = stairTable;
		return nf;
	}
	
	public boolean testConnectionWith(Floor f)
	{
		if(getDownCount() == f.getUpCount())
			return true;
		return false;
	}
	
	public int connectToBelow(Floor f)
	{
		int a = getDownCount();
		int b = f.getUpCount();
		int net = 0;
		//take away from larger
		if(a > b)
		{
			
			ArrayList<Point> ableToRemove = new ArrayList<Point>();
			for(int i = 0; i < zones.size(); i++)
			{
				FloorZone z = zones.get(i);
				for(int j = 0; j < z.downStairs.size(); j++)
				{
					if(z.downStairs.size() > 1)
						ableToRemove.add(z.downStairs.get(j));
				}
			}
			while(a > b)
			{
				//System.out.print("1");
				if(ableToRemove.size() == 0)
					break;
				int r = (int) (UltimaRando.rand() * ableToRemove.size());
				Point p = ableToRemove.get(r);
				for(int i = 0; i < zones.size(); i++)
				{
					if(zones.get(i).downStairs.contains(p))
					{
						zones.get(i).downStairs.remove(p);
						if(zones.get(i).downStairs.size() == 1)
							ableToRemove.remove(zones.get(i).downStairs.get(0));
						ableToRemove.remove(p);
						layout[p.y * 8 + p.x] -= 2;  //2 -> 0 and 3 -> 1; takes away the downstairs
						a--;
						net--;
						break;
					}
				}
			}
			ArrayList<FloorZone> pZones = null;
			ArrayList<Integer> counts = null;
			while(a > b)  //now add up-stairs on the bottom floor
			{
				//System.out.print("2");
				if(pZones == null)
				{
					pZones = new ArrayList<FloorZone>();
					counts = new ArrayList<Integer>();
					pZones.addAll(f.zones);
					for(int i = 0; i < pZones.size(); i++)
					{
						int pAvail = 0;
						for(int j = 0; j < pZones.get(i).points.size(); j++)
						{
							int xx = pZones.get(i).points.get(j).x;
							int yy = pZones.get(i).points.get(j).y;
							byte bb = f.layout[yy * 8 + xx];
							if(bb == 0 || bb == 2 || bb == 5)  //Covetous lv 5 forces us to consider chest spaces as open
								pAvail++;
						}
						if(pAvail == 0)
						{
							pZones.remove(i);
							i--;
						}
						else
							counts.add(pAvail);
					}
				}
				int rz, pz;
				Point pp;
				int nFailures = 0;
				while(true)
				{
					rz = (int) (UltimaRando.rand() * pZones.size());
					if(pZones.get(rz).points.size() == 0)
						continue;
					pz = (int) (UltimaRando.rand() * pZones.get(rz).points.size());
					
					pp = pZones.get(rz).points.get(pz);
					byte bb = f.layout[pp.y * 8 + pp.x];
					if(bb == 0 || bb == 2 || bb == 5)
						break;
					nFailures++;
					if(nFailures == 1000)
						return 2048;
				}
				f.layout[pp.y * 8 + pp.x] += 1;
				if(f.layout[pp.y * 8 + pp.x] == 6)
					f.layout[pp.y * 8 + pp.x] = 1;
				f.zones.get(rz).upStairs.add(pp);
				counts.set(rz, counts.get(rz) - 1);
				if(counts.get(rz) == 0)
				{
					counts.remove(rz);
					pZones.remove(rz);
				}
				b++;
				net++;
			}
		}
		
		if(b > a)
		{
			ArrayList<Point> ableToRemove = new ArrayList<Point>();
			for(int i = 0; i < f.zones.size(); i++)
			{
				FloorZone z = f.zones.get(i);
				for(int j = 0; j < z.upStairs.size(); j++)
				{
					if(z.upStairs.size() > 1)
						ableToRemove.add(z.upStairs.get(j));
				}
			}
			while(b > a)
			{
				//System.out.print("3");
				if(ableToRemove.size() == 0)
					break;
				int r = (int) (UltimaRando.rand() * ableToRemove.size());
				Point p = ableToRemove.get(r);
				for(int i = 0; i < f.zones.size(); i++)
				{
					if(f.zones.get(i).upStairs.contains(p))
					{
						f.zones.get(i).upStairs.remove(p);
						if(f.zones.get(i).upStairs.size() == 1)
							ableToRemove.remove(f.zones.get(i).upStairs.get(0));
						ableToRemove.remove(p);
						f.layout[p.y * 8 + p.x] -= 1;  //1 -> 0 and 2 -> 1; takes away the upstairs
						b--;
						net--;
						break;
					}
				}
			}
			ArrayList<FloorZone> pZones = null;
			ArrayList<Integer> counts = null;
			while(b > a)  //now add down-stairs on the top floor
			{
				//System.out.print("4");
				if(pZones == null)
				{
					pZones = new ArrayList<FloorZone>();
					counts = new ArrayList<Integer>();
					pZones.addAll(zones);
					for(int i = 0; i < pZones.size(); i++)
					{
						int pAvail = 0;
						for(int j = 0; j < pZones.get(i).points.size(); j++)
						{
							int xx = pZones.get(i).points.get(j).x;
							int yy = pZones.get(i).points.get(j).y;
							byte bb = layout[yy * 8 + xx];
							if(bb == 0 || bb == 1 || bb == 5)  //gotta love Covetous lv 5
								pAvail++;
						}
						if(pAvail == 0)
						{
							pZones.remove(i);
							i--;
						}
						else
							counts.add(pAvail);
					}
				}
				int rz, pz;
				Point pp;
				int nFailures = 0;
				while(true)
				{
					rz = (int) (UltimaRando.rand() * pZones.size());
					if(pZones.get(rz).points.size() == 0)
						continue;
					pz = (int) (UltimaRando.rand() * pZones.get(rz).points.size());
					
					pp = pZones.get(rz).points.get(pz);
					byte bb = layout[pp.y * 8 + pp.x];
					if(bb == 0 || bb == 1 || bb == 5)
						break;
					nFailures++;
					if(nFailures == 1000)
						return 2048;
				}
				layout[pp.y * 8 + pp.x] += 2;
				if(layout[pp.y * 8 + pp.x] == 7)
					layout[pp.y * 8 + pp.x] = 2;
				zones.get(rz).downStairs.add(pp);
				counts.set(rz, counts.get(rz) - 1);
				if(counts.get(rz) == 0)
				{
					counts.remove(rz);
					pZones.remove(rz);
				}
				a++;
				net++;
			}
		}
		return net;
	}
	
	public Point findRandUpstairs(FloorZone zn)
	{
		ArrayList<Point> okForUp = new ArrayList<Point>();
		for(int i = 0; i < zn.points.size(); i++)
		{
			Point pp = zn.points.get(i);
			byte bb = layout[pp.y * 8 + pp.x];
			if(bb == 0 || bb == 2 || bb == 5)
				okForUp.add(pp);
		}
		if(okForUp.size() == 0)
			return null;
		int r = (int) (UltimaRando.rand() * okForUp.size());
		Point px = okForUp.get(r);
		/*layout[px.y * 8 + px.x] += 1;
		if(layout[px.y * 8 + px.x] == 6)
			layout[px.y * 8 + px.x] = 1;*/
		return px;
	}
	
	public Point findRandDownstairs(FloorZone zn)
	{
		ArrayList<Point> okForUp = new ArrayList<Point>();
		for(int i = 0; i < zn.points.size(); i++)
		{
			Point pp = zn.points.get(i);
			byte bb = layout[pp.y * 8 + pp.x];
			if(bb == 0 || bb == 1 || bb == 5)
				okForUp.add(pp);
		}
		if(okForUp.size() == 0)
			return null;
		int r = (int) (UltimaRando.rand() * okForUp.size());
		Point px = okForUp.get(r);
		/*layout[px.y * 8 + px.x] += 2;
		if(layout[px.y * 8 + px.x] == 7)
			layout[px.y * 8 + px.x] = 2;*/
		return px;
	}
	
	public void calcZones()
	{
		byte[] temp = new byte[layout.length];
		for(int i = 0; i < temp.length; i++)
			temp[i] = layout[i];
		ArrayList<FloorZone> all = new ArrayList<FloorZone>();
		//int currZone = 0;
		for(int i = 0; i < temp.length; i++)
		{
			if(isPassable(temp[i]))
			{
				int yy = i / 8;
				int xx = i % 8;
				ArrayList<Point> pts = calcZoneForPoint(xx, yy, temp);
				ArrayList<Point> ups = new ArrayList<Point>();
				ArrayList<Point> downs = new ArrayList<Point>();
				for(int j = 0; j < pts.size(); j++)
				{
					int zy = pts.get(j).y;
					int zx = pts.get(j).x;
					byte bb = layout[8 * zy + zx];
					if(bb == 1 || bb == 3)
						ups.add(new Point(zx, zy));
					if(bb == 2 || bb == 3)
						downs.add(new Point(zx, zy));
				}
				FloorZone zn = new FloorZone(pts, ups, downs);
				all.add(zn);
			}
		}
		zones = all;
		//System.out.print(x)
	}
	
	private boolean isPassable(byte val)
	{
		switch(val)
		{
		//1F, 0a, 09, 08
		case 31: case 10: case 9: case 8: case -127: 
			return false;
		}
		if(val == origDIndex)
			return false;
		return true;
	}
	
	public int getZoneIndexForStair(byte xy)
	{
		int x = xy & 15;
		int y = (xy >> 4) & 15;
		for(int i = 0; i < this.zones.size(); i++)
		{
			FloorZone zn = zones.get(i);
			for(int j = 0; j < zn.upStairs.size(); j++)
			{
				Point p = zn.upStairs.get(j);
				if(p.x == x && p.y == y)
					return i;
			}
			for(int j = 0; j < zn.downStairs.size(); j++)
			{
				Point p = zn.downStairs.get(j);
				if(p.x == x && p.y == y)
					return i;
			}
		}
		return -1;
	}
	
	public String outputZoneData()
	{
		String out = "";
		for(int i = 0; i < 8; i++)
		{
			String s = "";
			String s2 = "";
			for(int j = 0; j < 8; j++)
			{
				String aa = Integer.toHexString(layout[8 * i + j] & 255);
				if(aa.length() == 1)
					aa = "0" + aa;
				s += aa + " ";
				String s3 = "..";
				for(int k = 0; k < zones.size(); k++)
				{
					for(int l = 0; l < zones.get(k).points.size(); l++)
					{
						Point pp = zones.get(k).points.get(l);
						if(pp.x == j && pp.y == i)
						{
							s3 = "0" + k;
							break;
						}
					}
					if(!s3.equals(".."))
						break;
				}
				s2 += s3 + " ";
			}
			out += s + "        " + s2 + "\n";
		}
		return out;
	}
	
	private ArrayList<Point> calcZoneForPoint(int x, int y, byte[] lcopy)
	{
		ArrayList<Point> piz = new ArrayList<Point>();
		piz.add(new Point(x, y));
		int lastPoint = piz.size() - 1;
		lcopy[y * 8 + x] = -127;
		while(true)
		{
			x++;
			if(x == 8)
				x = 0;
			if(isPassable(lcopy[y * 8 + x]))
			{
				piz.add(new Point(x, y));
				lcopy[y * 8 + x] = -127;
			}
			x -= 2;
			if(x < 0)
				x = x + 8;
			if(isPassable(lcopy[y * 8 + x]))
			{
				piz.add(new Point(x, y));
				lcopy[y * 8 + x] = -127;
			}
			x = piz.get(lastPoint).x;
			y++;
			if(y == 8)
				y = 0;
			if(isPassable(lcopy[y * 8 + x]))
			{
				piz.add(new Point(x, y));
				lcopy[y * 8 + x] = -127;
			}
			y -= 2;
			if(y < 0)
				y = y + 8;
			if(isPassable(lcopy[y * 8 + x]))
			{
				piz.add(new Point(x, y));
				lcopy[y * 8 + x] = -127;
			}
			if(piz.size() - 1 == lastPoint)
				break;
			lastPoint++;
			x = piz.get(lastPoint).x;
			y = piz.get(lastPoint).y;
			
		}
		for(int i = 0; i < piz.size(); i++)
		{
			x = piz.get(i).x;
			y = piz.get(i).y;
			if(isPassable(layout[y * 8 + x]) == false)
				System.out.println("Problem at " + x + "," + y + "; lcopy=" + lcopy[y * 8 + x]);
		}
		return piz;
	}

	public String getOrigLoc(boolean up) 
	{
		// TODO Auto-generated method stub
		String s = " (";
		if(up)
			s += getUpCount();
		else
			s += getDownCount();
		return origDName + " floor " + origFloor + s + ")";
	}

	public void addUpstairsAt(Point px, FloorZone zn) 
	{
		layout[px.y * 8 + px.x] += 1;
		if(layout[px.y * 8 + px.x] == 6)
			layout[px.y * 8 + px.x] = 1;
		zn.upStairs.add(px);
	}

	public void addDownstairsAt(Point px, FloorZone zn) 
	{
		layout[px.y * 8 + px.x] += 2;
		if(layout[px.y * 8 + px.x] == 7)
			layout[px.y * 8 + px.x] = 2;
		zn.downStairs.add(px);
	}
}

class Dungeon
{
	String name;
	int treeLoc;  //the location of the output table
	int dataLoc;  //the location of the compressed table
	int lookup;   //huffman value lookup table
	byte[] floorTable;  //the stair-floor table
	int floorTableLoc;  //the location of the stair-floor table
	int floorTablePtr;  //the location of the floor table pointer
	byte[] encounterRoomTable;  //the encounter room table
	int encounterRoomTableLoc;  //location of floor table
	int encounterRoomTablePtr;  //the location of the encounter room table pointer
	byte stoneRoomEquiv;
	byte[] allData;
	byte[] encoded; // used for testing
	byte[] treeData;  //used for testing
	
	byte[][] floorData;  //a decoded floor
	
	Floor[] allFloors;
	
	byte a, x, y, bb, bitCount, bitStream, bitMask, nBits; //a,x,and y are PPU registers; bb counts as the zero flag, 
	                                                       //bitCount=$16, bitStream=$17, bitMask=$4, nBits=$5
	int input, output;
	
	Dungeon(String dungeonName, int treeLocation, int dataLocation, byte[] data)
	{
		name = dungeonName;
		treeLoc = treeLocation;
		dataLoc = dataLocation;
		allData = data;
		lookup = 255581;
		String[] dnames = {"Deceit","Despise","Destard","Wrong","Covetous","Shame","Hythloth","Abyss"};
		for(int i = 0; i < dnames.length; i++)
			if(dungeonName.equals(dnames[i]))
				stoneRoomEquiv = (byte) (-127 + i);
	}
	
	public void addStairConnection(int topFloor, Point topStair, Point botStair)
	{
		int tsi = topStair.y * 16 + topStair.x;
		int bsi = botStair.y * 16 + botStair.x;
		byte ts = (byte) tsi;
		byte bs = (byte) bsi;
		byte[] a1 = allFloors[topFloor].stairTable;
		byte[] a2 = allFloors[topFloor + 1].stairTable;
		byte topIndex = (byte) ((a1.length) / 2);
		byte botIndex = (byte) ((a2.length) / 2);
		int tf = -1;
		int bf = -1;
		for(int i = 0; i < a1.length; i += 2)
		{
			if(a1[i] == ts)
			{
				topIndex = (byte) (i / 2);
				tf = i + 1;
				break;
			}
		}
		for(int i = 0; i < a2.length; i += 2)
		{
			if(a2[i] == bs)
			{
				botIndex = (byte) (i / 2);
				bf = i + 1;
				break;
			}
		}
		if(tf >= 0)
			a1[tf] |= botIndex;
		else
		{
			byte[] a3 = Arrays.copyOf(a1, a1.length + 2);
			int l = a1.length;
			a3[l] = ts;
			a3[l + 1] = botIndex;
			allFloors[topFloor].stairTable = a3;
		}
		if(bf >= 0)
			a2[bf] |= (topIndex << 4);
		else
		{
			byte[] a4 = Arrays.copyOf(a2, a2.length + 2);
			int l = a2.length;
			a4[l] = bs;
			a4[l + 1] = (byte) (topIndex << 4);
			allFloors[topFloor + 1].stairTable = a4;
		}
	}
	
	public String getLayout(byte[] decoded)
	{
		String s = "Dungeon " + name + "\n";
		for(int i = 0; i < allFloors.length; i++)
		{
			s += "Floor " + (i) + " is " + allFloors[i].origDName + " #" + allFloors[i].origFloor + "\n";
			for(int y = 0; y < 8; y++)
			{
				for(int x = 0; x < 8; x++)
				{
					String a = Integer.toHexString(allFloors[i].layout[y * 8 + x] & 255);
					if(a.length() == 1)
						a = "0" + a;
					String b = Integer.toHexString(decoded[i * 64 + y * 8 + x] & 255);
					if(b.length() == 1)
						b = "0" + b;
					char spc = '|';
					if(a.equals(b))
						spc = '=';
					s += a + spc + b + " ";
				}
				s += "\n";
			}
			s += "----------------------\n";
		}
		return s;
	}
	
	public void setTreeData(byte[] data)
	{
		treeData = data;
	}
	
	
	
	public String report()
	{
		byte[] dungeon = decode(null);
		byte[] loaded = new byte[output + 1];
		for(int i = 0; i <= output; i++)
			loaded[i] = dungeon[i];
		String s = "";
		int k = 0;
		for(int i = 0; i < 8; i++)
		{
			s += name + " floor #" + (i + 1) + ":\n";
			for(int j = 0; j < 64; j++)
			{
				if(k > output)
				{
					s += "\n";
					break;
				}
				String ss = Integer.toHexString(loaded[k]).toUpperCase();
				if(ss.length() == 1)
					ss = "0" + ss;
				ss = ss.substring(ss.length() - 2, ss.length());
				s += ss + " ";
				if((j + 1) % 16 == 0)
					s += "\n";
				k++;
			}
		}
		return s;
	}
	
	private int valueForIndex(int index)
	{
		if(index == 0)
			return 0;
		int prefixBits = 1;
		int treeValue = 0;
		int postfixBits = 2;
		int postfix = 0;
		int postfixMax = (1 << postfixBits) - 1;
		//int shift = 0;
		//if(index == 5)
			//System.out.print("");
		
		while(true)
		{
			if(postfix == postfixMax)
			{
				prefixBits += postfixBits;
				postfixBits++;
				postfixMax = (1 << postfixBits) - 1;
				postfix = 0;
			}
			treeValue++;
			if(treeValue == index)
				break;
			postfix++;
		}
		int r = (1 << prefixBits) - 1;
		r <<= postfixBits;
		r |= postfix;
		return r;
		//return (prefix << shift) + postfix;
	}
	
	public void testValueForIndex()
	{
		for(int i = 0; i < 30; i++)
		{
			System.out.println(i + ":" + Integer.toBinaryString(valueForIndex(i)));
		}
	}
	
	private byte[] enableMultiStone(byte[] rawData, byte[] treeData)  //must be done at floor level
	{
		/*for(int i = 0; i < rawData.length; i++)
			if(rawData[i] == 50)  //==0x32?
				rawData[i] = stoneRoomEquiv;*/
		for(int i = 0; i < treeData.length; i++)
			if(treeData[i] == 50)
				treeData[i] = stoneRoomEquiv;
		return treeData;
	}
	
	public byte[] encode(byte[] rawData, byte[] treeData, int padToLength)  //outputs the huffman encoded dungeon
	{
		treeData = enableMultiStone(rawData, treeData);
		ArrayList<Byte> output = new ArrayList<Byte>();
		int bitsLeft = 31;
		int current = 0;
		for(int i = 0; i < rawData.length; i++)
		{
			byte raw = rawData[i];
			int idx = 0;
			if(treeData == null)
			{
				while(raw != allData[treeLoc + idx])
					idx++;
			}
			else
			{
				while(raw != treeData[idx])
					idx++;
			}
			int val = valueForIndex(idx);
			//System.out.println(raw + " became " + val);
			int width = 32 - Integer.numberOfLeadingZeros(val);
			width = Math.max(1, width);
			if(bitsLeft < width)
			{
				int a = val >> (width - bitsLeft);
				current |= a;
				for(int j = 3; j >= 0; j--)
				{
					byte bb = (byte) ((current >> (8 * j)) & 255);
					output.add(bb);
				}
				width -= bitsLeft;
				int mask = (1 << width) - 1;
				val &= mask;
				bitsLeft = 32;
				current = 0;
			}
			bitsLeft -= width;
			val <<= bitsLeft;
			current |= val;
		}
		for(int j = 3; j >= 0; j--)
		{
			byte bb = (byte) ((current >> (8 * j)) & 255);
			//if(bb > 0)
			output.add(bb);
		}
		byte[] rval;
		if(padToLength > output.size())
			rval = new byte[padToLength + 1];
		else
			rval = new byte[output.size() + 1];
		for(int i = 0; i < output.size(); i++)
			rval[i] = output.get(i);
		return rval;
	}
	
	public void testEncoder()
	{
		byte[] b1 = decode(null);
		byte[] b2 = encode(b1, null, encodedSize);
		for(int i = 0; i < b2.length; i++)
		{
			byte a = allData[dataLoc + i];
			byte b = b2[i];
			if(a != b)
			{
				/*if(a == 32 && b > 80)
					System.out.println("Stone room changed to " + b);
				else
				{
					System.out.println(i + ":" + a + "  " + b);
					System.out.println("Exp " + Integer.toBinaryString(a & 255) + "\nGot " + Integer.toBinaryString(b & 255));
				}*/
			}
		}
		byte[] b3 = decode(b2);
		for(int i = 0; i < b3.length - 1; i++)
		{
			if(b3[i] != b1[i])
			{
				if(b3[i] > 80 && b1[i] == 32)
					System.out.println("Stone room successfully changed to " + b3[i]);
				else
					System.out.println("Encoding error in byte #" + i + " " + b1[i]  + " was expected and " + b3[i] + " was found");
			}
		}
	}
	
	public Floor[] calcDecodedFloors(byte[] encData, byte[] stairTable, byte[] roomTable)
	{
		byte[] allFloors = decode(encData);
		floorData = new byte[8][];
		Floor[] rv = new Floor[8];
		for(int i = 0; i < 8; i++)
		{
			floorData[i] = new byte[64];
			for(int j = 0; j < 64; j++)
			{
				this.floorData[i][j] = allFloors[i * 64 + j];
			}
			rv[i] = new Floor(floorData[i], name, stoneRoomEquiv, i);
			//stair table
			if(stairTable != null)
			{
				int start = stairTable[i] & 255;
				int end = 0;
				if(i < 7)
					end = stairTable[i + 1] & 255;
				else
					end = stairTable.length;
				byte[] st = new byte[end - start];
				for(int j = start; j < end; j++)
					st[j - start] = stairTable[j];
				rv[i].setInitStairTable(st);
			}
			//room table
			if(roomTable != null)
			{
				ArrayList<Byte> rt = new ArrayList<Byte>();
				int j = 0;
				while(j < roomTable.length && roomTable[j] != i)
					j += 3;
				while(j < roomTable.length && roomTable[j] == i)
				{
					for(int k = 0; k < 3; k++)
						rt.add(roomTable[j + k]);
					j += 3;
				}
				byte[] rT = new byte[rt.size()];
				for(int k = 0; k < rT.length; k++)
					rT[k] = rt.get(k);
				rv[i].setCombatRooms(rT);
			}
		}
		this.allFloors = rv;
		return rv;
	}
	
	
	
	HashMap<String, Integer> huffMap;
	private int encodedSize;
	public byte[] decode(byte[] encData)
	{
		byte[] rtnVal = new byte[8 * 64];
		
		bitCount = 7;
		input = dataLoc;
		output = 0;
		if(encData == null)
		{
			bitStream = allData[input];
			encoded = allData;
		}
		else
		{
			input = 0;
			dataLoc = 0;
			encoded = encData;
			bitStream = encData[0];
		}
		bitStream = (byte) ((bitStream << 1) & -2);
		input++;
		//the algorithm
		huffMap = new HashMap<String, Integer>();
		for(int i = 0; i < 8; i++)
		{
			while(e655() != 80)
			{
				if(e634(rtnVal) == 1)
				{
					//System.out.println("Floor #" + i + " of dungeon " + name + " decoded; input is at " + (input - dataLoc) + " output is at " + output);
					break;
				}
			}
		}
		//System.out.println(huffMap.toString());
		encodedSize = input - dataLoc - 1;
		return rtnVal;
	}
	
	byte oldByte;
	byte oldByte2;
	byte bytesRead;
	String readVal;
	
	public byte e655()
	{
		x = 1;
		bitMask = x;
		oldByte = bitStream;
		oldByte2 = encoded[input];  //input has already been incremented
		bytesRead = 0;
		readVal = "";
		while(true)
		{
			e68e();
			if(a != bitMask)  //done
			{
				break;
			}
			x++;
			bitMask = (byte) ((bitMask << 1) + 1);
		}
		//byte nextMask = (byte) (bitMask >> 1);
		//int readVal = (bitMask << x) + a;
		a += allData[lookup + x];
		y = a;
		if(treeData != null)
			a = treeData[y];
		else
			a = allData[treeLoc + y];
		//System.out.println("SB=" + Integer.toBinaryString(oldByte & 255) + " " + Integer.toBinaryString(oldByte2 & 255) + "\nEB=" + Integer.toBinaryString(bitStream & 255) + " " + Integer.toBinaryString(encoded[input] & 255) + "\nRB=" + (readVal) + "\nbits read=" + bytesRead + " mapped to " + y);
		huffMap.put(readVal, (int) y);
		
		return a;
	}
	
	public byte e68e()
	{
		nBits = x;
		a = 0;
		y = a;
		while(true)
		{
			bitCount--;
			if(bitCount < 0)
			{
				bitCount = 7;
				bitStream = encoded[input];
				input++;
			}
			bb = (byte) (bitStream & -128);
			bb = (byte) ((bb >> 7) & 1);
			readVal += bb;
			a <<= 1;
			a += bb;
			bitStream = (byte) ((bitStream << 1) & -2);
			x--;
			bytesRead++;
			if(x == 0)
				break;
		}
		x = nBits;
		return a;
	}
	
	public byte e634(byte[] out)
	{
		out[output] = a;
		output++;
		if(((output) % 64) == 0)
			return 1;
		else
			return 0;
	}
	
	private int getStairXY(int index, int floor)
	{
		int stairStart = this.floorTable[floor];
		return floorTable[stairStart + index * 2];
	}
	
	private int getStairNumber(int xy, int floor)
	{
		int stairStart = this.floorTable[floor];
		int stairEnd;
		if(floor == 7)
			stairEnd = floorTable.length;
		else
			stairEnd = floorTable[floor + 1];
		for(int i = stairStart; i < stairEnd; i += 2)
		{
			if(floorTable[i] == xy)
			{
				return ((i - stairStart) / 2) + 1;
			}
		}
		return -1;
	}
	
	private int getStairIndex(int xy, int floor, boolean down)
	{
		int stairStart = this.floorTable[floor];
		int stairEnd;
		if(floor == 7)
			stairEnd = floorTable.length;
		else
			stairEnd = floorTable[floor + 1];
		for(int i = stairStart; i < stairEnd; i += 2)
		{
			if(floorTable[i] == xy)
			{
				if(down)
					return floorTable[i + 1] & 15;
				else
					return (floorTable[i + 1] >> 4) & 15;
			}
		}
		return -1;
	}
	
	public void testStairTable()
	{
		for(int i = 0; i < allFloors.length - 1; i++)
		{
			Floor f1 = allFloors[i];
			Floor f2 = allFloors[i + 1];
			boolean c = f1.testConnectionWith(f2);
			if(c == false)
				System.out.println("The down stair count of floor " + i + " and the up stair count of floor " + (i + 1) +" do not match");
		}
		for(int i = 0; i < allFloors.length; i++)
		{
			int stairStart = this.floorTable[i];
			int stairEnd;
			if(i == 7)
				stairEnd = floorTable.length;
			else
				stairEnd = floorTable[i + 1];
			Floor f = allFloors[i];
			Floor fup = null;
			if(i >= 1 )
				fup = allFloors[i - 1];
			Floor fd = null;
			if(i < 7)
				fd = allFloors[i + 1];
			int floorErrors = 0;
			ArrayList<Byte> downs = new ArrayList<Byte>();
			ArrayList<Byte> ups = new ArrayList<Byte>();
			ArrayList<Byte> topDowns = new ArrayList<Byte>();
			ArrayList<Byte> botUps = new ArrayList<Byte>();
			for(int j = 0; j < f.layout.length; j++)
			{
				byte yx = (byte) (((j >> 3) << 4) + (j & 7));
				if(f.layout[j] == 1 || f.layout[j] == 3)
					ups.add((byte) yx);
				if(f.layout[j] == 2 || f.layout[j] == 3)
					downs.add((byte) yx);
			}
			/*ArrayList<Integer> sc = new ArrayList<Integer>(ups.size());
			for(int j = 0; j < ups.size(); j++)
			{
				sc.add(j);
				if(i == 0)
					sc.set(j, 15);
			}
			for(int j = 0; j < ups.size(); j++)
			{
				int xy = ups.get(j);
				int n = getStairIndex(xy, i, false);
				if(n == -1)
				{
					System.out.println("Error: stair @" + xy + " was not in the list");
					floorErrors++;
				}
				else
					sc.remove(new Integer(n));
			}
			for(int j = 0; j < sc.size(); j++)
			{
				System.out.println("Error: couldn't find stair index #" + j);
				floorErrors++;
			}
			sc.clear();
			for(int j = 0; j < downs.size(); j++)
				sc.add(j);
			for(int j = 0; j < downs.size(); j++)
			{
				int xy = downs.get(j);
				int n = getStairIndex(xy, i, true);
				if(n == -1)
				{
					System.out.println("Error: stair @" + xy + " was not in the list");
					floorErrors++;
				}
				else
					sc.remove(new Integer(n));
			}
			for(int j = 0; j < sc.size(); j++)
			{
				System.out.println("Error: couldn't find stair index #" + j);
				floorErrors++;
			}
			sc.clear();*/
			if(fup != null)
			{
				for(int j = 0; j < fup.layout.length; j++)
				{
					if(fup.layout[j] == 2 || fup.layout[j] == 3)
						topDowns.add((byte) j);
				}
			}
			if(fd != null)
			{
				for(int j = 0; j < fd.layout.length; j++)
				{
					if(fd.layout[j] == 1 || fd.layout[j] == 3)
						botUps.add((byte) j);
				}
			}
			
			for(int j = 0; j < ups.size(); j++)
			{
				if(fup == null)
					break;
				int xym = ups.get(j);
				for(int k = stairStart; k < stairEnd; k += 2)
				{
					int xy = floorTable[k];  //xy is y-x location
					if(xym != xy)
						continue;
					int udi = floorTable[k + 1];  //udi contains up-down index 
					int up = (udi >> 4) & 15;
					byte xyu = (byte) getStairXY(up, i - 1);
					int xyl = ((xyu >> 4) << 3) + (xyu & 7);
					//byte xyu = topDowns.get(up);
					//test f to see that there's a down stair at xy
					if(fup.layout[xyl] == 2 || fup.layout[xyl] == 3)
					{
						int uidx = getStairIndex(xyu, i - 1, true);
						//does this down stair put me there?
						if(uidx == -1)
						{
							System.out.println("floor " + i + " up #" + j + " @" + xym + ":going up then down takes me to nowhere");
							floorErrors++;
						}
						int xy2 = getStairXY(uidx, i);
						if(xy2 != xym)
						{
							System.out.println("Stair mismatch: floor " + i + " down #" + j + " @" + xym + " goes to floor " + (i + 1) + " #" + getStairNumber(xyu, i + 1) + " @" + xyu);
							System.out.println("  but that stair goes to #" + uidx + " @" + xy2);
							floorErrors++;
						}	
					}
					else
					{
						System.out.println("floor " + i + " up #" + j + " @" + xym + ":going up takes me to nowhere");
						floorErrors++;
					}
				}
			}
			for(int j = 0; j < downs.size(); j++)
			{
				if(fd == null)
					break;
				int xym = downs.get(j);
				for(int k = stairStart; k < stairEnd; k += 2)
				{
					int xy = floorTable[k];  //xy is y-x location
					if(xym != xy)
						continue;
					int udi = floorTable[k + 1];  //udi contains up-down index 
					int dn = udi & 15;
					byte xyu = (byte) getStairXY(dn, i + 1);  //takes you down
					int xyl = ((xyu >> 4) << 3) + (xyu & 7);
					//byte xyu = botUps.get(dn);
					//test f to see that there's an up stair at xy
					if(fd.layout[xyl] == 1 || fd.layout[xyl] == 3)
					{
						int uidx = getStairIndex(xyu, i + 1, false);  //now go back up from lower floor
						
						//does this down stair put me there?
						if(uidx == -1)
						{
							System.out.println("floor " + i + " down #" + j + " @" + xym + ":going down then up takes me to nowhere");
							floorErrors++;
						}
						int xy2 = getStairXY(uidx, i);
						if(xy2 != xym)
						{
							System.out.println("Stair mismatch: floor " + i + " down #" + j + " @" + xym + " goes to floor " + (i + 1) + " #" + getStairNumber(xyu, i + 1) + " @" + xyu);
							System.out.println("  but that stair goes to #" + uidx + " @" + xy2);
							floorErrors++;
						}	
					}
					else
					{
						System.out.println("floor " + i + " down #" + j + " @" + xym + ":going down takes me to nowhere");
						floorErrors++;
					}
				}
			}
			if(floorErrors > 0)
			{
				System.out.println(floorErrors + " errors on floor " + i);
				if(i >= 1)
				{
					System.out.print("Floor " + (i - 1) + " stair table:");
					System.out.println(allFloors[i - 1].printStairTable());
				}
				System.out.print("Floor " + i + " stair table:");
				System.out.println(allFloors[i].printStairTable());
				if(i <= 6)
				{
					System.out.print("Floor " + (i + 1) + " stair table:");
					System.out.println(allFloors[i + 1].printStairTable());
				}
			}
		}
	}
	
	class StairEntry
	{
		Point p;
		boolean up;
		int floor;
		int zone;
		int destIndex;
		//boolean visited;
		
		StairEntry(Point p, boolean up, int floor, int zone, int destIndex)
		{
			this.p = p;
			this.up = up;
			this.floor = floor;
			this.zone = zone;
			this.destIndex = destIndex;
			/*String output = "[";
			if(up)
				output += "U";
			else
				output += "D";
			output += floor + "" + destIndex + "]";
			System.out.print(output);*/
			//visited = false;
		}
		
		public boolean matchesEntry(byte xy, int floor, boolean up)
		{
			int x = xy & 15;
			int y = (xy >> 4) & 15;
			if(p.x == x && p.y == y && this.floor == floor && this.up == up)
				return true;
			return false;
		}
	}
	
	private int getStairDestIndex(Point stair, int floor, boolean up)
	{
		byte[] st1 = allFloors[floor].stairTable;
		for(int j = 0; j < st1.length; j += 2)
		{
			int xx = st1[j] & 15;
			int yy = (st1[j] >> 4) & 15;
			if(xx == stair.x && yy == stair.y)
			{
				if(up)
					return (st1[j + 1] >> 4) & 7;
				else
					return st1[j + 1] & 7;
			}
		}
		return -1;
	}
	
	private void dungeonWalk(ArrayList<StairEntry> ups, ArrayList<StairEntry> downs) 
	{
		while((ups.size() + downs.size()) > 0)
		{
			while(ups.size() > 0)
			{
				StairEntry se = ups.remove(0);
				//get where it is going
				int floorDest = se.floor - 1;
				int di = se.destIndex;
				byte xy = allFloors[floorDest].stairTable[di * 2];
				
				/*for(int i = 0; i < downs.size(); i++)  //remove the corresponding down stair
				{
					if(downs.get(i).matchesEntry(xy, floorDest, false))
					{
						downs.remove(i);
						break;
					}
				}*/
				
				//mark the zone
				int zoneNum = allFloors[floorDest].getZoneIndexForStair(xy);
				FloorZone fz = allFloors[floorDest].zones.get(zoneNum);
				if(fz.marks == 0)
				{
					fz.marks = 1;
					//add all stairs on this zone
					for(int i = 0; i < fz.upStairs.size(); i++)
					{
						Point p = fz.upStairs.get(i);
						int destIndex = getStairDestIndex(p, floorDest, true);
						//if(allFloors[floorDest - 1].stairTable.length <= (destIndex * 2))
							//getStairDestIndex(p, floorDest, true);
						ups.add(new StairEntry(p, true, floorDest, zoneNum, destIndex));
					}
					for(int i = 0; i < fz.downStairs.size(); i++)
					{
						Point p = fz.downStairs.get(i);
						int destIndex = getStairDestIndex(p, floorDest, false);
						//if(allFloors[floorDest + 1].stairTable.length <= (destIndex * 2))
							//getStairDestIndex(p, floorDest, false);
						downs.add(new StairEntry(p, false, floorDest, zoneNum, destIndex));
					}
				}
			}
			while(downs.size() > 0)
			{
				StairEntry se = downs.remove(0);
				//get where it is going
				int floorDest = se.floor + 1;
				int di = se.destIndex;
				byte xy = allFloors[floorDest].stairTable[di * 2];
				
				/*for(int i = 0; i < ups.size(); i++)  //remove the corresponding up stair
				{
					if(ups.get(i).matchesEntry(xy, floorDest, true))
					{
						ups.remove(i);
						break;
					}
				}*/
				
				//mark the zone
				int zoneNum = allFloors[floorDest].getZoneIndexForStair(xy);
				FloorZone fz = allFloors[floorDest].zones.get(zoneNum);
				if(fz.marks == 0)
				{
					fz.marks = 1;
					//add all stairs on this zone
					for(int i = 0; i < fz.upStairs.size(); i++)
					{
						Point p = fz.upStairs.get(i);
						int destIndex = getStairDestIndex(p, floorDest, true);
						//if(allFloors[floorDest - 1].stairTable.length <= (destIndex * 2))
							//getStairDestIndex(p, floorDest, true);
						ups.add(new StairEntry(p, true, floorDest, zoneNum, destIndex));
					}
					for(int i = 0; i < fz.downStairs.size(); i++)
					{
						Point p = fz.downStairs.get(i);
						int destIndex = getStairDestIndex(p, floorDest, false);
						//if(allFloors[floorDest + 1].stairTable.length <= (destIndex * 2))
							//getStairDestIndex(p, floorDest, false);
						downs.add(new StairEntry(p, false, floorDest, zoneNum, destIndex));
					}
				}
			}
		}
	}
	
	public String traverseDungeon()
	{
		//find the up stairs (-16)
		
		byte[] st1 = allFloors[0].stairTable;
		int stairNum = -1;
		int zoneNum = -1;
		FloorZone fz = null;
		for(int i = 1; i < st1.length; i += 2)
		{
			if((st1[i] & -16) == -16)
			{
				stairNum = (i - 1) / 2;
				
				zoneNum = allFloors[0].getZoneIndexForStair(st1[i - 1]);
				if(zoneNum != -1)
					fz = allFloors[0].zones.get(zoneNum);
				//else
					//System.err.println("WTF");
			}
		}
		ArrayList<StairEntry> ups = new ArrayList<StairEntry>();
		ArrayList<StairEntry> downs = new ArrayList<StairEntry>();
		//add all down stairs in zone to down list
		fz.marks = 1;
		for(int i = 0; i < fz.downStairs.size(); i++)
		{
			Point p = fz.downStairs.get(i);
			int destIndex = getStairDestIndex(p, 0, false);
			downs.add(new StairEntry(p, false, 0, zoneNum, destIndex));
		}
		dungeonWalk(ups, downs);
		String rv = "";
		//int marooned = 0;
		ArrayList<FloorZone> marooned = new ArrayList<FloorZone>();
		ArrayList<Integer> mfloor = new ArrayList<Integer>();
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < allFloors[i].zones.size(); j++)
			{
				fz = allFloors[i].zones.get(j);
				if(fz.marks == 0)
				{
					marooned.add(fz);
					mfloor.add(i);
				}
			}
		}
		int l = 0;
		while(marooned.size() > 0)
		{
			if(l >= marooned.size())
				l = 0;
			fz = marooned.get(l);
			if(fz.marks == 1)
			{
				marooned.remove(l);
				mfloor.remove(l);
			}
			else
			{
				int i = mfloor.get(l);
				int j = allFloors[i].zones.indexOf(fz);
				rv += name + " Floor #" + i + " zone #" + j + " is unreachable\n";
				//select a random zone to connect to
				FloorZone cz = null;
				int czFloor = -1;
				int czIdx = -1;
				ArrayList<FloorZone> connectTo = new ArrayList<FloorZone>();
				if(i > 0)
					connectTo.addAll(allFloors[i - 1].zones);
				for(int k = 0; k < connectTo.size(); k++)
				{
					if(connectTo.get(k).marks == 0)
					{
						connectTo.remove(k);
						k--;
					}
				}
				int upZones = connectTo.size();
				if(i < 7)
					connectTo.addAll(allFloors[i + 1].zones);
				for(int k = upZones; k < connectTo.size(); k++)
				{
					if(connectTo.get(k).marks == 0)
					{
						connectTo.remove(k);
						k--;
					}
				}
				while(connectTo.size() > 0)
				{
					int r = (int) (UltimaRando.rand() * connectTo.size());
					cz = connectTo.get(r);
					Point a = null;
					Point b = null;
					if(r < upZones)
					{
						a = allFloors[i].findRandUpstairs(fz);
						b = allFloors[i - 1].findRandDownstairs(cz);
						if(a == null) //then you cannot go upstairs
						{
							while(upZones > 0)
							{
								connectTo.remove(0);
								upZones--;
							}
						}
						else if(b == null)
						{
							connectTo.remove(cz);
							upZones--;
						}
						if(a == null || b == null)
						{
							cz = null;
							continue;
						}
						czFloor = i - 1;
						czIdx = allFloors[czFloor].zones.indexOf(cz);
						allFloors[i].addUpstairsAt(a, fz);
						allFloors[i - 1].addDownstairsAt(b, cz);
						rv += "Added upstairs   at " + a.x + "," + a.y + " Floor " + i + " Old Stair Table:" + Arrays.toString(allFloors[i].stairTable) + "\n";
						rv += "Added downstairs at " + b.x + "," + b.y + " Floor " + (i - 1) + " Old Stair Table:" + Arrays.toString(allFloors[i - 1].stairTable) + "\n";
						addStairConnection(i - 1, b, a);
						rv += "Floor " + i + " New Stair Table:" + Arrays.toString(allFloors[i].stairTable) + "\n";
						rv += "Floor " + (i - 1) + " New Stair Table:" + Arrays.toString(allFloors[i - 1].stairTable) + "\n";
						break;
					}
					else
					{
						a = allFloors[i].findRandDownstairs(fz);
						b = allFloors[i + 1].findRandUpstairs(cz);
						if(a == null)  //then you cannot go downstairs
						{
							while(connectTo.size() > upZones)
								connectTo.remove(connectTo.size() - 1);
						}
						else if(b == null)
							connectTo.remove(cz);
						if(a == null || b == null)
						{
							cz = null;
							continue;
						}
						czFloor = i + 1;
						czIdx = allFloors[czFloor].zones.indexOf(cz);
						allFloors[i].addDownstairsAt(a, fz);
						allFloors[i + 1].addUpstairsAt(b, cz);
						rv += "Added downstairs at " + a.x + "," + a.y + " Floor " + i + " Old Stair Table:" + Arrays.toString(allFloors[i].stairTable) + "\n";
						rv += "Added upstairs   at " + b.x + "," + b.y + " Floor " + (i + 1) + " Old Stair Table:" + Arrays.toString(allFloors[i + 1].stairTable) + "\n";
						addStairConnection(i, a, b);
						rv += "Floor " + i + " New Stair Table:" + Arrays.toString(allFloors[i].stairTable) + "\n";
						rv += "Floor " + (i + 1) + " New Stair Table:" + Arrays.toString(allFloors[i + 1].stairTable) + "\n";
						break;
					}
				}
				//System.out.println(rv);
				if(cz != null)  //we want to walk from a marked zone
				{
					if(czFloor > 0)
					{
						for(int k = 0; k < cz.upStairs.size(); k++)  //i is floor j is zone
						{
							Point p = cz.upStairs.get(k);
							int destIndex = getStairDestIndex(p, czFloor, true);
							ups.add(new StairEntry(p, true, czFloor, czIdx, destIndex));
						}
					}
					if(czFloor < 7)
					{
						for(int k = 0; k < cz.downStairs.size(); k++)  //i is floor j is zone
						{
							Point p = cz.downStairs.get(k);
							int destIndex = getStairDestIndex(p, czFloor, false);
							downs.add(new StairEntry(p, false, czFloor, czIdx, destIndex));
						}
					}
					dungeonWalk(ups, downs);
				}
			}
			l++;
		}
		
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < allFloors[i].zones.size(); j++)
			{
				fz = allFloors[i].zones.get(j);
				if(fz.marks == 0)
					System.out.println("Error:" + name + " Floor #" + i + " zone #" + j + " is unreachable");
				fz.marks = 0;
			}
		}
		return rv;
	}
	
	public void stairPreservationTest(byte[] stairTable)
	{
		
	}

	public void reassemble() 
	{
		int stairLen = 0;
		int roomLen = 0;
		for(int i = 0; i < allFloors.length; i++)
		{
			if(i == 0)
				allFloors[i].redoStairTable(null);
			else
				allFloors[i].redoStairTable(allFloors[i - 1]);
		}
		//System.out.println(traverseDungeon());  //one last check to avoid softlocks
		traverseDungeon();
		for(int i = 0; i < allFloors.length; i++)
		{
			floorData[i] = allFloors[i].layout;
			stairLen += allFloors[i].stairTable.length;
			roomLen += allFloors[i].combatRooms.length;
		}
		this.encounterRoomTable = new byte[roomLen];
		this.floorTable = new byte[stairLen + 8];
		int sw = 8;
		int erw = 0;
		for(int i = 0; i < allFloors.length; i++)
		{
			Floor f = allFloors[i];
			for(int j = 0; j < f.combatRooms.length; j++)
			{
				if(j % 3 == 0)
					encounterRoomTable[erw] = (byte) i;
				else
					encounterRoomTable[erw] = f.combatRooms[j];
				erw++;
			}
			floorTable[i] = (byte) sw;
			for(int j = 0; j < f.stairTable.length; j++)
			{
				floorTable[sw] = f.stairTable[j];
				sw++;
			}
		}
	}
}

class TextFinder
{
	NESRom rom;
	int[] i1;
	int[] i2;
	int[] i3;
	char[] c1;
	char[] c2;
	char[] c3;
	
	int foundThisLine;
	
	TextFinder(NESRom romData)
	{
		rom = romData;
		int[] ov1 = {208,144,188,0,138};
		char[] co1 = {'-','!','_','|','?'};
		int[] ov2 = {191,192,193,194,195,196,197,198,199,200,201,202,203,204,205};
		char[] co2 = {'%','/','.',',','@','"','$','\'','#','*','+','&','x',':',';'};
		int[] ov3 = {105,106,107,108,109,110,111,112,113,114,115,116,117};
		char[] co3 = {'N','E','S','W','G','P','D','M','B','F','H','T','R'};
		i1 = ov1;
		i2 = ov2;
		i3 = ov3;
		c1 = co1;
		c2 = co2;
		c3 = co3;
	}
	
	public String replaceSpokenText(int start, int end, String toReplace, int align)
	{
		decodeSpokenText(start - 5, end - start + 5, align, rom.romData);
		System.out.println("");
		byte[] s1 = encodeSpokenText(start, toReplace, align);
		for(int i = 0; i < s1.length; i++)
			rom.romData[start + i] = s1[i];
		String rv = decodeSpokenText(start - 5, end - start + 5, align, rom.romData);
		System.out.println("");
		return rv;
	}
	
	public byte[] decompressSpokenBytesAt(int a)
	{
		int len = rom.romData[a];
		byte[] rv = new byte[len];
		a++;
		int bits = 5;
		int shift = 7;
		byte val = 0;
		int k = 0;
		while(k < len)
		{
			val <<= 1;
			val |= ((rom.romData[a] >> shift) & 1);
			shift--;
			if(shift == -1)
			{
				a++;
				shift = 7;
			}
			bits--;
			if(bits == -1)
			{
				rv[k] = val;
				val = 0;
				k++;
				bits = 5;
			}
		}
		return rv;
	}
	
	public byte[] compressSpokenBytes(byte[] a)
	{
		int bshift = 5;
		int bleft = 7;
		byte b = 0;
		ArrayList<Byte> out = new ArrayList<Byte>();
		int i = 0;
		while(true)
		{
			b <<= 1;
			b |= ((a[i] >> bshift) & 1);
			bshift--;
			if(bshift == -1)
			{
				bshift = 5;
				i++;
				if(i >= a.length)
					break;
			}
			bleft--;
			if(bleft == -1)
			{
				bleft = 7;
				out.add(b);
				b = 0;
			}
		}
		if(bleft != 7)
		{
			b <<= bleft;
			out.add(b);
		}
		byte[] rv = new byte[out.size()];
		for(int j = 0; j < rv.length; j++)
			rv[j] = out.get(j);
		return rv;
	}
	
	private void bitCompare(byte[] a, byte[] b, int align)
	{
		String s1 = "";
		String s2 = "";
		int a1 = 0;
		int a2 = 0;
		int b1 = a[0] << align;
		int b2 = b[0];
		int p = 0;
		int bl = 7;
		boolean done = false;
		while(!done)
		{
			for(int i = 0; i < 6; i++)
			{
				a1 <<= 1;
				a2 <<= 1;
				a1 |= ((b1 & 128) >> 7);
				a2 |= ((b2 & 128) >> 7);
				s1 += a1 & 1;
				s2 += a2 & 1;
				b1 <<= 1;
				b2 <<= 1;
				bl--;
				if(bl < 0)
				{
					if(p < (a.length - 1))
					{
						p++;
						b1 = a[p];
						b2 = b[p];
						bl = 7;
					}
					else
					{
						done = true;
						break;
					}
				}
			}
			s1 += "|";
			s2 += "|";
		}
		System.out.println(s1);
		System.out.println(s2);
	}
	
	public byte[] encodeSpokenText(int dataLoc, String toEncode, int align)
	{
		byte val = 0;
		String rv = "";
		int bitsLeft = 7;
		byte src = rom.romData[dataLoc];
		int a = src & 255;
		byte[] oth = rom.strToBytes("00 00 be bf c3 d0 c6 c7 c8 c9 ca 00 00 be bf c1 c2 7f fe 3d 90 8a 20");
		int[] res;
		for(int i = 0; i < align; i++)
		{
			a <<= 1;
			bitsLeft--;
		}
		if(bitsLeft < 7)
			a >>= 8;
		boolean lcase = false;
		boolean numLoc = false;
		int ctrl = 0;
		ArrayList<Byte> out = new ArrayList<Byte>();
		for(int i = 0; i < toEncode.length(); i++)
		{
			//get the encoded character
			char ch = toEncode.charAt(i);
			//write control bits if necessary
			byte cc = getControlChar(ch, lcase, numLoc, oth);
			if(cc != -1)
			{
				if(cc == 0)
					lcase = !lcase;
				if(cc == 1)
					numLoc = !numLoc;
				res = outputChar(a, bitsLeft, cc, out);
				a = res[0];
				bitsLeft = res[1];
				ctrl++;
			}
			//write the 6 bits
			byte outChar = encodeChar(ch, oth);
			res = outputChar(a, bitsLeft, outChar, out);
			a = res[0];
			bitsLeft = res[1];
		}
		int advBits = ((toEncode.length() + ctrl) * 6) + align;
		int advBytes = advBits / 8;
		dataLoc += advBytes;
		bitsLeft++;
		a <<= bitsLeft;
		int bmask = (1 << (bitsLeft)) - 1;
		//System.out.println("bitsLeft=" + bitsLeft);
		//System.out.println("a0=" + Long.toBinaryString(a & 255));
		a += rom.romData[dataLoc] & bmask;
		//System.out.println("a1=" + Long.toBinaryString(a & 255));
		out.add(new Byte((byte) (a & 255)));
		
		byte[] bb = new byte[out.size()];
		for(int i = 0; i < out.size(); i++)
		{
			bb[i] = out.get(i);
			//System.out.print("[" + bb[i] + "]");
		}
		//System.out.println("BitsLeft ended at " + bitsLeft);
		//bitCompare(rom.getData(dataLoc - advBytes, dataLoc), bb, align);
		/*byte[] toView = {rom.romData[dataLoc - 1], bb[bb.length - 2], rom.romData[dataLoc], bb[bb.length - 1], rom.romData[dataLoc + 1]};
		for(int i = 0; i < toView.length; i++)
		{
			String s = "";
			if(i % 2 == 1)
				s += ("D:");
			else
				s += ("R:");
			int nz = Long.numberOfLeadingZeros(toView[i]) - 56;
			for(int j = 0; j < nz; j++)
				s += "0";
			s += Long.toBinaryString(toView[i]);
			System.out.println(s);
		}*/
		//System.out.println();
		return bb;
		//return decodeSpokenText(0, toEncode.length() + ctrl, align, bb);
		
	}
	
	private byte getControlChar(char curr, boolean lcase, boolean numloc, byte[] oth)
	{
		boolean cUcase = false;
		boolean pUcase = !lcase;
		boolean cNum = false;
		boolean pNum = numloc;
		if(curr >= 'A' && curr <= 'Z')
			cUcase = true;
		else
		{
			if(curr == ' ')
				cUcase = true;
			else
			{
				for(int i = 0; i < c2.length; i++)
				{
					if(curr == c2[i])
					{
						//find the corresponding i2 in oth
						for(int j = 0; j < oth.length; j++)
						{
							int v2 = oth[j] & 255;
							if(i2[i] == v2)
							{
								if(j < 11)
									cUcase = false;
								else
									cUcase = true;
								break;
							}
						}
						break;
					}
				}
			}
		}
		/*if(prev >= 'A' && prev <= 'Z')
			pUcase = true;
		else
			pUcase = false;*/
		if(curr >= '0' && curr <= '9')
			cNum = true;
		else
			cNum = false;
		/*if(prev >= '0' && prev <= '9')
			pNum = true;
		else
			pNum = false;*/
		if(pNum != cNum)
			return 1;
		if(cNum)
			return -1;
		if(cUcase != pUcase)
			return 0;
		return -1;
	}
	
	private byte encodeChar(char c, byte[] oth)
	{
		if(c >= 'A' && c <= 'Z')
			return (byte) (c - 'A' + 17);
		if(c >= 'a' && c <= 'z')
			return (byte) (c - 'a' + 17);
		if(c >= '0' && c <= '9')
			return (byte) (c - '0' + 2);
		if(c == ' ')
			return 6;
		if(c == '\n')
			return 62;
		for(int i = 0; i < i2.length; i++)
		{
			if(c == c2[i])
			{
				int vv = i2[i];
				for(int j = 0; j < oth.length; j++)
				{
					int v2 = oth[j] & 255;
					if(vv == v2)
						return (byte) (j % 11);
				}
			}
		}
		return 6;
	}
	
	private int[] outputChar(int a, int writeBit, byte b, ArrayList<Byte> out)
	{
		int bb = ((b << 2) & 255);
		for(int i = 0; i < 6; i++)
		{
			a <<= 1;
			a |= ((bb >> 7) & 1);
			bb <<= 1;
			writeBit--;
			if(writeBit < 0)
			{
				out.add(new Byte((byte) (a & 255)));
				writeBit = 7;
			}
		}
		int[] rv = {a, writeBit};
		return rv;
	}
	
	public String decodeSpokenText(int dataLoc, int len, int align, byte[] allData)
	{
		//this data isn't really huffman encoded so much as it is encoded in 6-bit chunks
		byte val = 0;
		String rv = "";
		int bitsLeft = 7;
		byte src = allData[dataLoc];
		byte[] oth = rom.strToBytes("00 00 be bf c3 d0 c6 c7 c8 c9 ca 00 00 be bf c1 c2 7f fe 3d 90 8a 20");
		for(int i = 0; i < align; i++)
		{
			src <<= 1;
			bitsLeft--;
		}
		boolean lcase = false;
		boolean numLoc = false;
		//ArrayList<Integer> p = new ArrayList<Integer>();
		for(int i = 0; i < len; i++)
		{
			val = 0;
			for(int j = 0; j < 6; j++)
			{
				val <<= 1;
				if((src & 128) == 128)
					val++;
				src <<= 1;
				bitsLeft--;
				if(bitsLeft < 0)
				{
					dataLoc++;
					src = allData[dataLoc];
					bitsLeft = 7;
				}
			}
			
			System.out.print("[" + String.format("%2d", val) + "]");
			if(val == 0)
			{
				lcase = !lcase;
				continue;
			}
			if(val == 1)
			{
				numLoc = !numLoc;
				continue;
			}
			else if(val == 63)
				return rv;
			else if(val == 62)
				rv += "\n";
			else if(val == 16)
				return rv;
			if(val >= 17)
			{
				val |= 128;
				if(lcase)
					val |= 64;
				rv += convertByte(val);
			}
			else if(val <= 11)
			{
				if(numLoc)
				{
					int vv = val + 126;
					val = (byte) (vv & 255);
				}
				else
				{
					if(!lcase)  //y is 0 when uppercase
						val += 11;
					val = oth[val];
				}
				rv += convertByte(val);
			}
		}
		//System.out.println();
		return rv;
	}
	
	char convertByte0(byte in)
	{
		char val = (char) (in & 255);
		if(val <= 25)
			return (char) (val + 'A');
		switch(val)
		{
		case 26: return '.';
		case 27: return '@';
		case 28: return '0';
		case 29: return '1';
		case 30: return '5';
		case 31: return '8';
		case 32: return '9';
		default: return ' ';
		}
	}
	
	char convertByte(byte in)
	{
		//if(in == -111)
			//System.out.print("");
		char val = (char) (in & 255);
		//145 - 170 -> A-Z
		if(val >= 145 && val <= 170)
		{
			val = (char) (val - 145 + 'A');
			return val;
		}
		//209 - 234 -> a-z
		if(val >= 209 && val <= 234)
		{
			val = (char) (val - 209 + 'a');
			return val;
		}
		//0x80 - 0x89 -> numbers
		if(val >= 128 && val <= 137)
		{
			val = (char) (val - 128 + '0');
			return val;
		}
		//other characters
		for(int i = 0; i < i1.length; i++)
		{
			if(val == i1[i])
				return c1[i];
		}
		for(int i = 0; i < i2.length; i++)
		{
			if(val == i2[i])
				return c2[i];
		}
		for(int i = 0; i < i3.length; i++)
		{
			if(val == i3[i])
				return c3[i];
		}
		return ' ';
	}
	
	public void scanLine(byte[] line, int start, int end)
	{
		String out = "";
		if(7080 >= start && 7080 <= end)
		{
			byte bb = line[7080 - start];
			byte cc = rom.romData[7080];
			byte[] test = new byte[7085-7072];
			for(int k = 7072; k <= 7084; k++)
				test[k - 7072] = rom.romData[k];
			System.out.print("");
		}
		for(int i = 0; i < line.length; i++)
		{
			char c = convertByte(line[i]);
			/*if(line[i] == -36)
			{
				byte bb = line[i];
				System.out.print("");
			}*/
			if(c >= 'A' && c <= 'Z')
				foundThisLine++;
			if(c >= 'a' && c <= 'z')
				foundThisLine++;
			out += c;
		}
		if(foundThisLine > 80)
			System.out.println(Integer.toHexString(start) + " - " + Integer.toHexString(end) +  " : " + out);
	}
	
	public void scanLine0(byte[] line, int start, int end)
	{
		String out = "";
		if(7080 >= start && 7080 <= end)
		{
			byte bb = line[7080 - start];
			byte cc = rom.romData[7080];
			byte[] test = new byte[7085-7072];
			for(int k = 7072; k <= 7084; k++)
				test[k - 7072] = rom.romData[k];
			System.out.print("");
		}
		for(int i = 0; i < line.length; i++)
		{
			char c = convertByte0(line[i]);
			/*if(line[i] == -36)
			{
				byte bb = line[i];
				System.out.print("");
			}*/
			if(c >= 'A' && c <= 'Z')
				foundThisLine++;
			if(c >= 'a' && c <= 'z')
				foundThisLine++;
			out += c;
		}
		if(foundThisLine > 5)
			System.out.println(Integer.toHexString(start) + " - " + Integer.toHexString(end) +  " : " + out);
	}
	
	public void tryArea(int start)  //tries an area of 64*256 bytes
	{
		byte[] line;
		for(int j = 0; j < 64; j++)
		{
			int ss = start + 256 * j;
			int ee = ss + 255;
			line = rom.getData(ss, ee);
			scanLine(line, ss, ee);
		}
	}
	
	public void tryArea0(int start)  //tries an area of 64*256 bytes
	{
		byte[] line;
		for(int j = 0; j < 64; j++)
		{
			int ss = start + 256 * j;
			int ee = ss + 255;
			line = rom.getData(ss, ee);
			scanLine0(line, ss, ee);
		}
	}
	
	public void scanForDiff(String in)
	{
		int[] diff = new int[in.length() - 1];
		char[] str = in.toCharArray();
		
		for(int i = 1; i < str.length; i++)
		{
			diff[i - 1] = str[i] - str[i - 1];
		}
		
		int match = 0;
		byte[] rd = rom.romData;
		for(int i = 1; i < rom.romData.length; i++)
		{
			int b = rd[i] & 255;
			int a = rd[i - 1] & 255;
			int d = b - a;
			if(d == diff[match])
				match++;
			else
				match = 0;
			if(match == diff.length - 1)
			{
				System.out.println("Match found at " + Integer.toHexString(i - match) + " - " + Integer.toHexString(i));
				match = 0;
			}
		}
	}
	
}

class Descriptor
{
	Color c;
	String desc;
	
	Descriptor(Color col, String d)
	{
		c = col;
		desc = d;
	}
}

class POI
{
	Point p;
	String desc;
	
	POI(Point p, String desc)
	{
		this.p = p;
		this.desc = desc;
	}
}

class UltCharacter
{
	UltCharacter()
	{
		
	}
	
	public int getRandomGold(int mode)
	{
		switch(mode)
		{
		case 0:  return 0;
		case 1:  return (int) (UltimaRando.rand() * 401);
		case 2:  return (int) (UltimaRando.rand() * 1000);
		default: return 400;
		}
	}
	
	public byte getRandomVirtue(int mode)
	{
		switch(mode)
		{
		case 0:  return (byte) 0;
		case 1:  return (byte) (UltimaRando.rand() * 100);
		case 2:  return (byte) 99;
		default: return (byte) 50;
		}
	}
	
	public byte[] getRandomInitSpells(int mode)
	{
		byte[] rv = new byte[3];
		if((mode & 1) == 1)
		{
			rv[0] = (byte) ((UltimaRando.rand() * 256) - 128);
			rv[0] |= 16;
			rv[1] = (byte) (UltimaRando.rand() * 128);
			if(UltimaRando.rand() > 0.5)
				rv[2] = 8;
			else
				rv[2] = 0;
		}
		else
		{
			rv[0] = (byte) -1;
			rv[1] = 127;
			rv[2] = 8;
		}
		if((mode & 2) == 2)
			rv[0] &= -33;
		if((mode & 4) == 4)
			rv[1] &= -17;
		return rv;
	}
	
	public byte getAvatarhoodValue()
	{
		return (byte) 99;
	}
	
	public byte[] getRandomReagents(int mode)
	{
		byte[] rv = new byte[8];
		if(mode == 1)  //shuffle
		{
			int[] a = {8,8,9,7,8,4,0,0};
			ArrayList<Integer> li = new ArrayList<Integer>(a.length);
			for(int i : a)
				li.add(i);
			int j = 0;
			while(li.size() > 0)
			{
				int n = li.remove((int) (UltimaRando.rand() * li.size()));
				a[j] = n;
				j++;
			}
			for(int i = 0; i < a.length; i++)
				rv[i] = (byte) a[i];
			return rv;
		}
		else
		{
			for(int i = 0; i < 8; i++)
				rv[i] = (byte) (UltimaRando.rand() * 10);
		}
		return rv;
	}
	
	public byte[] getRandomStartEquip(int mode, byte[] oldItems)
	{
		byte[] rv = new byte[16];
		ArrayList<Integer> wps = new ArrayList<Integer>(8);
		ArrayList<Integer> arm = new ArrayList<Integer>(8);
		for(int i = 0; i < 8; i++)
		{
			wps.add((int) oldItems[i * 2]);
			arm.add((int) oldItems[i * 2 + 1]);
		}
		for(int i = 0; i < 8; i++)
		{
			if(mode == 1)
			{
				rv[i * 2] = wps.remove((int) (UltimaRando.rand() * wps.size())).byteValue();
				rv[i * 2 + 1] = arm.remove((int) (UltimaRando.rand() * arm.size())).byteValue();
			}
			else
			{
				rv[i * 2] = (byte) (UltimaRando.rand() * 14 + 1);
				rv[i * 2 + 1] = (byte) (UltimaRando.rand() * 8 + 16);
			}
		}
		return rv;
	}
	
	public byte[] getRandomEquippables(int mode, byte[] oldEquips)
	{
		byte[] rv = new byte[25];
		for(int i = 0; i < 25; i++)
		{
			if(oldEquips[i] == 0)
				rv[i] = 0;
			else if(mode == 1)  //shuffle
			{
				int n = Long.bitCount(oldEquips[i] & 255);
				ArrayList<Integer> li = new ArrayList<Integer>(8);
				for(int j = 0; j < 8; j++)
					li.add(j);
				byte bb = 0;
				for(int j = 0; j < n; j++)
					bb |= 1 << li.remove((int) (UltimaRando.rand() * li.size()));
				rv[i] = bb;
			}
			else
				rv[i] = (byte) (UltimaRando.rand() * 256);
		}
		return rv;
	}
}

class OpeningScreen
{
	byte[] initData;
	byte[] finalData;
	int blockLoc;
	int blockEnd1;
	int blockEnd2;
	int treeLoc;
	int dataLoc1;
	int dataLoc2;
	
	String inTree;
	ArrayList<Byte> tree;
	ArrayList<Byte> info;
	ArrayList<Byte> decompressed;
	
	HuffmanDecoder hud;
	
	OpeningScreen(NESRom rom)
	{
		blockLoc = 192514;
		blockEnd1 = 192657;
		initData = rom.getData(blockLoc, blockEnd1);
		treeLoc = blockLoc + 3;
		dataLoc1 = treeLoc + initData[2];
		inTree = "LICENSED BY NINTENDO OF AMERICA INC. ORIGINAL VERSION 1985 ORIGIN SYSTEMS INC. NES VERSION 1990 FCI INC. 1990 PONYCANYON INC.";
		tree = new ArrayList<Byte>();
		for(int i = 0; i < initData[2]; i++)
			tree.add(initData[3 + i]);
		info = new ArrayList<Byte>();
		for(int i = dataLoc1; i <= blockEnd1; i++)
			info.add(rom.romData[i]);
		decompressed = new ArrayList<Byte>();
		HuffmanDecoder hd = new HuffmanDecoder(rom.romData, treeLoc, dataLoc1);
		hd.setEndInput(blockEnd1);
		hd.setLookup(255612);
		byte[] openScreen = hd.decode(null, 0, 2, 3);
		for(int i = 0; i < openScreen.length; i++)
		{
			decompressed.add(openScreen[i]);
		}
		//printDecompressed();
		hud = hd;
		appendString("RANDOMIZER BY\nGILMOK");
		//printDecompressed();
		byte[] aa = generateFinalScreen(rom);
		finalData = aa;
	}
	
	public void replaceOpenScreen(NESRom rom)
	{
		//blockLoc = 196368;  //bf00 - will this be my bff?
		for(int i = 0; i < finalData.length; i++)
			rom.romData[blockLoc + i] = finalData[i];
		//System.out.println("Opening screen new size=" + finalData.length + " + 40 palette bytes requires " + (finalData.length + 40) + " bytes of ram ------------------------");
		/*for(int i = 0; i < finalData.length; i++)
		{
			System.out.print(Integer.toHexString(finalData[i] & 255) + " ");
			if(i % 16 == 0)
				System.out.println();
		}*/
		//180290, 180291 aff2 -> bf00  (point the memory to where it needs to be)
		rom.romData[blockLoc] = 0;
		rom.romData[blockLoc + 1] = -65;
		
		byte[] alsoCopy = rom.getData(192722, 192737);
		int acDest = 196432; //bf40
		for(int i = 0; i < alsoCopy.length; i++)
			rom.romData[acDest + i] = alsoCopy[i];  //bf50
	}
	
	private String needsMoreTree(String toAppend)
	{
		String s = "";
		for(int i = 0; i < toAppend.length(); i++)
			if(inTree.indexOf(toAppend.charAt(i)) == -1 && toAppend.charAt(i) != ' ' && toAppend.charAt(i) != '\n')
				s += toAppend.charAt(i);
		return s;
	}
	
	public void appendString(String toAppend)
	{
		String moreTree = needsMoreTree(toAppend);
		for(int i = 0; i < moreTree.length(); i++)
		{
			char c = moreTree.charAt(i);
			byte a = (byte) (c - 'A');
			tree.add(a);
		}
		int aSpot = decompressed.size() - 6;
		int atStart = 107;
		tree.add((byte) atStart);
		int mid = 52;
		tree.add((byte) mid);
		int atEnd = 276 - atStart - mid;
		tree.add((byte) atEnd);
		decompressed.set(aSpot - 1, (byte) atStart);
		for(int i = 0; i < toAppend.length(); i++)
		{
			char c = toAppend.charAt(i);
			if(c == ' ')
				decompressed.add(aSpot, Byte.parseByte("7f", 16));
			else if(c == '\n')
			{
				decompressed.add(aSpot, Byte.parseByte("7f", 16));
				aSpot++;
				decompressed.add(aSpot, (byte) -127);
				aSpot++;
				decompressed.add(aSpot, (byte) mid);
			}
			else
			{
				byte d = (byte) (c - 'A');
				decompressed.add(aSpot, d);
			}
			aSpot++;
		}
		for(int i = 0; i < 3; i++)
			decompressed.remove(decompressed.size() - 1);
		decompressed.set(decompressed.size() - 1, (byte) atEnd);
	}
	
	private void printDecompressed()
	{
		for(int i = 0; i < decompressed.size(); i++)
		{
			byte v = decompressed.get(i);
			String s = Integer.toHexString(v & 255);
			if(s.length() == 1)
				s = "0" + s;
			System.out.print(s + "|");
		}
		System.out.println();
		for(int i = 0; i < decompressed.size(); i++)
		{
			byte v = decompressed.get(i);
			if(v >= 0 && v <= 25)
			{
				char aa = (char) (v + 'A');
				System.out.print(" " + aa + "|");
			}
			char[] aa = {'.', '@', '0', '1', '5', '8', '9'};
			if(v >= 26 || v < 0)
			{
				int o = v - 26;
				if(o >= 0 && o < aa.length)
					System.out.print(" " + aa[o]);
				else
					System.out.print("  ");
				System.out.print('|');
			}
		}
		System.out.println();
	}
	
	public byte[] generateFinalScreen(NESRom rom)
	{
		int treeGrow = 0;
		int dataGrow = 0;
		ArrayList<Byte> newTree = new ArrayList<Byte>();
		//byte[] treeB = new byte[tree.size()];
		for(int i = 0; i < tree.size(); i++)
		{
			//treeB[i] = tree.get(i);
			if(hud.addToHuffMap(tree.get(i)) == true)
				newTree.add(tree.get(i));
		}
		treeGrow = newTree.size();
		byte[] treeB = new byte[initData[2] + treeGrow];
		for(int i = 0; i < initData[2]; i++)
			treeB[i] = initData[3 + i];
		for(int i = 0; i < newTree.size(); i++)
			treeB[i + initData[2]] = newTree.get(i);
		byte[] decompB = new byte[decompressed.size()];
		for(int i = 0; i < decompressed.size(); i++)
			decompB[i] = decompressed.get(i);
		byte[] out1 = hud.encodeWithMap(decompB);
		dataGrow = out1.length - info.size();
		byte[] rv = new byte[initData.length + treeGrow + dataGrow];
		rv[0] = (byte) (initData[0] + treeGrow + dataGrow - 1);
		rv[1] = initData[1];  //it won't go into b1
		rv[2] = (byte) (initData[2] + treeGrow);
		for(int i = 0; i < treeB.length; i++)
			rv[3 + i] = treeB[i];
		for(int i = 0; i < out1.length; i++)
			rv[3 + treeB.length + i] = out1[i];
		//System.out.println("ENC:" + HuffmanDecoder.printByte(rv[0]) + " " + HuffmanDecoder.printByte(rv[1]) + " " + HuffmanDecoder.printByte(rv[2]));
		//byte[] old = rom.getData(dataLoc1, blockEnd1);
		//System.out.println("OLD:" + HuffmanDecoder.printByte(old[0]) + " " + HuffmanDecoder.printByte(old[1]) + " " + HuffmanDecoder.printByte(old[2]));
		//HuffmanDecoder hd2 = new HuffmanDecoder(treeB, rv);
		//hd2.setLookup(255612);
		//byte[] openScreen = hd2.decode(rv, 0, 2, 3);
		return rv;
	}
}

class HuffmanDecoder
{
	byte bitCount;
	int input;
	int dataLoc;
	byte output;
	byte bitMask;
	byte bitStream;
	byte[] encoded;
	int encodedSize;
	byte[] allData;
	HashMap<Byte, String> huffMap;
	byte a, x, y;
	byte[] treeData;
	int treeLoc;
	
	HuffmanDecoder(byte[] treeData, byte[] encoded)
	{
		this.treeData = treeData;
		this.encoded = encoded;
		encodedSize = encoded.length;
		dataLoc = 0;
		huffMap = new HashMap<Byte, String>();
	}
	
	HuffmanDecoder(byte[] allData, int treeLoc, int dataLoc)
	{
		this.allData = allData;
		this.treeLoc = treeLoc;
		//System.out.println("Treeloc data starts with " + allData[treeLoc]);
		
		this.dataLoc = dataLoc;
		//System.out.println("Dataloc data starts with " + allData[dataLoc]);
		huffMap = new HashMap<Byte, String>();
	}
	
	public byte[] decode(byte[] encData, int iBC, int initX, int initMask) //allows you to set the initial bit count and initX
	{
		ArrayList<Byte> e1 = new ArrayList<Byte>();
		input = dataLoc;
		if(encData == null)
		{
			bitStream = allData[input];
			encoded = allData;
		}
		else
		{
			input = 0;
			dataLoc = 0;
			encoded = encData;
			bitStream = encData[0];
		}
		a = 0;
		bitCount = (byte) iBC;
		while(a != 80)
		{
			x = (byte) initX;
			bitMask = (byte) initMask;
			readVal = "";
			while(true)
			{
				e68e();
				if(a != bitMask)  //done
				{
					break;
				}
				x++;
				bitMask = (byte) ((bitMask << 1) + 1);
			}
			//byte nextMask = (byte) (bitMask >> 1);
			//int readVal = (bitMask << x) + a;
			a += allData[lookup + x];
			y = a;
			if(treeData != null)
				a = treeData[y];
			else
				a = allData[treeLoc + y];
			//System.out.println("SB=" + Integer.toBinaryString(oldByte & 255) + " " + Integer.toBinaryString(oldByte2 & 255) + "\nEB=" + Integer.toBinaryString(bitStream & 255) + " " + Integer.toBinaryString(encoded[input] & 255) + "\nRB=" + (readVal) + "\nbits read=" + bytesRead + " mapped to " + y);
			huffMap.put(a, readVal);
			e1.add(a);
			if(input > endInput)
				break;
		}
		byte[] rv = new byte[e1.size()];
		for(int i = 0; i < rv.length; i++)
			rv[i] = e1.get(i);
		return rv;
		//return a;
	}
	
	public byte[] decode(byte[] encData)
	{
		byte[] rtnVal = new byte[8 * 64];
		
		bitCount = 7;
		input = dataLoc;
		output = 0;
		if(encData == null)
		{
			bitStream = allData[input];
			encoded = allData;
		}
		else
		{
			input = 0;
			dataLoc = 0;
			encoded = encData;
			bitStream = encData[0];
		}
		bitStream = (byte) ((bitStream << 1) & -2);
		input++;
		//the algorithm
		//huffMap = new HashMap<String, Integer>();
		//for(int i = 0; i < 8; i++)
		//{
			while(e655() != 80)
			{
				if(e634(rtnVal) == 1)
				{
					//System.out.println("Floor #" + i + " of dungeon " + name + " decoded; input is at " + (input - dataLoc) + " output is at " + output);
					break;
				}
				if(input > endInput)
					break;
			}
		//}
		//System.out.println(huffMap.toString());
		encodedSize = input - dataLoc - 1;
		return rtnVal;
	}
	
	public static String printByte(byte b)
	{
		String s = "";
		for(int i = 0; i < 8; i++)
		{
			if((b >> i & 1) == 1)
				s = "1" + s;
			else
				s = "0" + s;
		}
		String e = Integer.toHexString(b & 255);
		return s + " " + e;
	}
	
	private int valueForIndex(int index)
	{
		if(index == 0)
			return 0;
		int prefixBits = 1;
		int treeValue = 0;
		int postfixBits = 2;
		int postfix = 0;
		int postfixMax = (1 << postfixBits) - 1;
		//int shift = 0;
		//if(index == 5)
			//System.out.print("");
		
		while(true)
		{
			if(postfix == postfixMax)
			{
				prefixBits += postfixBits;
				postfixBits++;
				postfixMax = (1 << postfixBits) - 1;
				postfix = 0;
			}
			treeValue++;
			if(treeValue == index)
				break;
			postfix++;
		}
		int r = (1 << prefixBits) - 1;
		r <<= postfixBits;
		r |= postfix;
		return r;
		//return (prefix << shift) + postfix;
	}
	
	public byte[] encodeWithMap(byte[] rawData)  //a slower way to encode using the hashmap
	{
		String s = "";  
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		for(int i = 0; i < rawData.length; i++)
		{
			byte ba = rawData[i];
			String s2 = huffMap.get(ba);
			s += s2;
			while(s.length() >= 8)
			{
				String sb = s.substring(0, 8);
				s = s.substring(8, s.length());
				byte b = 0;
				for(int j = 0; j < 8; j++)
				{
					b <<= 1;
					if(sb.charAt(j) == '1')
						b++;
				}
				bytes.add(b);
			}
		}
		while(s.length() < 8)
			s += '0';
		byte b = 0;
		for(int j = 0; j < 8; j++)
		{
			b <<= 1;
			if(s.charAt(j) == '1')
				b++;
		}
		if(b != 0)
			bytes.add(b);
		byte[] rv = new byte[bytes.size()];
		for(int i  = 0; i < bytes.size(); i++)
			rv[i] = bytes.get(i);
		return rv;
	}
	
	private String getNextTreeValue()
	{
		long high = 0;
		long low = 100000;
		String lowStr = "";
		Set<Byte> intss = huffMap.keySet();
		Byte[] ints = new Byte[intss.size()];
		ints = intss.toArray(ints);
		for(int i = 0; i < ints.length; i++)
		{
			String s = huffMap.get(ints[i]);
			long l = convertKey(s);
			if(l > high)
				high = l;
			if(l < low)
			{
				low = l;
				lowStr = s;
			}
		}
		long next = high + 1;
		if(Long.bitCount(next + 1) == 1)  //next is all 1s
		{
			int n = Long.bitCount(next);
			int chip = lowStr.length();
			while(n > 0)
			{
				n -= chip;
				chip++;
			}
			next <<= chip;
		}
		return Long.toBinaryString(next);
	}
	
	private long convertKey(String k)
	{
		long l = 0;
		for(int i = 0; i < k.length(); i++)
		{
			l <<= 1;
			if(k.charAt(i) == '1')
				l++;
		}
		return l;
	}
	
	public boolean addToHuffMap(byte value)
	{
		if(!huffMap.containsKey(value))
		{
			huffMap.put(value, getNextTreeValue());
			return true;
		}
		return false;
	}
	
	public byte[] encode(byte[] rawData, byte[] treeData, int padToLength)  //outputs the huffman encoded data
	{
		ArrayList<Byte> output = new ArrayList<Byte>();
		int bitsLeft = 31;
		int current = 0;
		for(int i = 0; i < rawData.length; i++)
		{
			byte raw = rawData[i];
			int idx = 0;
			if(treeData == null)
			{
				while(raw != allData[treeLoc + idx])
					idx++;
			}
			else
			{
				while(raw != treeData[idx])
					idx++;
			}
			int val = valueForIndex(idx);
			//System.out.println(raw + " became " + val);
			int width = 32 - Integer.numberOfLeadingZeros(val);
			width = Math.max(1, width);
			if(bitsLeft < width)
			{
				int a = val >> (width - bitsLeft);
				current |= a;
				for(int j = 3; j >= 0; j--)
				{
					byte bb = (byte) ((current >> (8 * j)) & 255);
					output.add(bb);
				}
				width -= bitsLeft;
				int mask = (1 << width) - 1;
				val &= mask;
				bitsLeft = 32;
				current = 0;
			}
			bitsLeft -= width;
			val <<= bitsLeft;
			current |= val;
		}
		for(int j = 3; j >= 0; j--)
		{
			byte bb = (byte) ((current >> (8 * j)) & 255);
			//if(bb > 0)
			output.add(bb);
		}
		byte[] rval;
		if(padToLength > output.size())
			rval = new byte[padToLength + 1];
		else
			rval = new byte[output.size() + 1];
		for(int i = 0; i < output.size(); i++)
			rval[i] = output.get(i);
		return rval;
	}
	
	byte oldByte;
	byte oldByte2;
	byte bytesRead;
	String readVal;
	int lookup;
	
	public byte e655()
	{
		x = 1;
		bitMask = x;
		oldByte = bitStream;
		oldByte2 = encoded[input];  //input has already been incremented
		bytesRead = 0;
		readVal = "";
		while(true)
		{
			e68e();
			if(a != bitMask)  //done
			{
				break;
			}
			x++;
			bitMask = (byte) ((bitMask << 1) + 1);
		}
		//byte nextMask = (byte) (bitMask >> 1);
		//int readVal = (bitMask << x) + a;
		a += allData[lookup + x];
		y = a;
		if(treeData != null)
			a = treeData[y];
		else
			a = allData[treeLoc + y];
		//System.out.println("SB=" + Integer.toBinaryString(oldByte & 255) + " " + Integer.toBinaryString(oldByte2 & 255) + "\nEB=" + Integer.toBinaryString(bitStream & 255) + " " + Integer.toBinaryString(encoded[input] & 255) + "\nRB=" + (readVal) + "\nbits read=" + bytesRead + " mapped to " + y);
		huffMap.put(a, readVal);
		
		return a;
	}
	
	byte nBits, bb;
	
	public byte e68e()
	{
		nBits = x;
		a = 0;
		y = a;
		while(true)
		{
			bitCount--;
			if(bitCount < 0)
			{
				bitCount = 7;
				bitStream = encoded[input];
				input++;
			}
			bb = (byte) (bitStream & -128);
			bb = (byte) ((bb >> 7) & 1);
			readVal += bb;
			a <<= 1;
			a += bb;
			bitStream = (byte) ((bitStream << 1) & -2);
			x--;
			bytesRead++;
			if(x == 0)
				break;
		}
		x = nBits;
		return a;
	}
	
	public byte e634(byte[] out)
	{
		out[output] = a;
		output++;
		if(((output + 1) % 64) == 0)
			return 1;
		else
			return 0;
	}

	int endInput;
	public void setEndInput(int i) 
	{
		endInput = i;
	}
	
	public void setLookup(int i)
	{
		lookup = i;
	}

}

class Map
{
	byte[] mapData;
	byte[] zoneData;
	byte[] accessData;
	byte[] visibility;
	boolean proportionalWater;
	ArrayList<MapZone> zones;
	ArrayList<MapZone> features;
	ArrayList<Point> virtueTowns;  //everything must be connected to at least one of these
	ArrayList<Point> moongates;
	ArrayList<Point> dungeons;
	ArrayList<Point> shrines;
	ArrayList<Point> townsNCastles;
	Point abyss;
	Point fungus;
	Point manroot;
	
	class MapZone
	{
		Rectangle area;
		byte terrain;
		byte index;
		ArrayList<Point> filled;
		int fillPoint;
		int inZone;
		int cx;
		int cy;
		boolean isUsed;
		Point innerPOI;
		
		MapZone(Rectangle r, byte type, int idx)
		{
			area = r;
			terrain = type;
			filled = new ArrayList<Point>();
			fillPoint = -1;
			inZone = -1;
			cx = getCenterX();
			cy = getCenterY();
			index = (byte) idx;
			isUsed = false;
			innerPOI = null;
		}
		
		public MapZone() 
		{
			// TODO Auto-generated constructor stub
		}
		
		public String toString()
		{
			return area.toString() + ":" + this.terrain;
		}
		
		public int findZone(ArrayList<MapZone> zones)
		{
			//Point c = new Point(getCenterX(), getCenterY());
			int dist = Integer.MAX_VALUE;
			int rv = -1;
			for(int i = 0; i < zones.size(); i++)
			{
				MapZone zn = zones.get(i);
				int dd = Math.abs(zn.getCenterX() - getCenterX());
				dd += Math.abs(zn.getCenterY() - getCenterY());
				if(dd < dist)
				{
					dist = dd;
					rv = i;
				}
			}
			inZone = rv;
			return rv;
		}

		private void fillPoint(int x, int y)
		{
			if(mapData[256 * y + x] == 0)
			{
				mapData[256 * y + x] = terrain;
				zoneData[256 * y + x] = index;
				filled.add(new Point(x, y));
			}
			else if(mapData[256 * y + x] == terrain)
				return;
			else if(UltimaRando.rand() > 0.85)
			{
				mapData[256 * y + x] = terrain;
				zoneData[256 * y + x] = index;
				filled.add(new Point(x, y));
			}
		}
		
		public boolean zoneContains(int x, int y)
		{
			if(x < area.x || y < area.y)
				return false;
			if(x > area.x + area.width - 1)
				return false;
			if(y > area.y + area.height - 1)
				return false;
			return true;
		}
		
		private void fillFeaturePoint(int x, int y, int val, double probIn, double probOut, boolean landMatch)
		{
			byte b = mapData[256 * y + x];
			if(b == val)
			{
				//filled.add(new Point(x, y));
				return;
			}
			boolean isMatch = false;
			if(val >= 10 && b >= 10)
				isMatch = true;
			if(val <= 7 && b <= 7)
				isMatch = true;
			if(isMatch != landMatch)  //some features require land types to match, others require them to not match
				probIn = probOut;     //it is treated as an outside point if the land match requirement is not met
				
			if(zoneContains(x, y))
			{
				if(UltimaRando.rand() < probIn)
				{
					mapData[256 * y + x] = (byte) val;
					filled.add(new Point(x, y));
				}
			}
			else if(UltimaRando.rand() < probOut)
			{
				mapData[256 * y + x] = (byte) val;
				filled.add(new Point(x, y));
			}
			if(filled.size() == 0)
				System.out.println("Fail");
		}
		
		public int getCenterX()
		{
			return (int) area.x + (area.width / 2);
		}
		
		public int getCenterY()
		{
			return (int) area.y + (area.height / 2);
		}
		
		public boolean featureOKForTown(int alsoAllow)
		{
			switch(terrain)
			{
			case 0:  case 1:  case 4:  case 6:
				return true;
			case 7:
				if(alsoAllow >= 1)  return true;
				break;
			case 8:
				if(alsoAllow >= 2)  return true;
				break;
			default:
				return false;
			}
			return false;
		}
		
		public boolean featureOKForDungeon()
		{
			switch(terrain)
			{
			case 3:  case 6:  case 8:
				return true;
			default:
				return false;
			}
		}
		
		public boolean featureOKForShrine()  //I can't think of any features that shrines cannot be on
		{
			if(terrain < 40 || terrain > 44)
				return true;
			else
				return false;
		}
		
		public void placeFeature(int fnum)
		{
			
			//System.out.println("Placing feature type " + fnum);
			
			switch(fnum)
			{
			case 0: //shrub
			case 1: //forest
				placeLandFeature(11 + fnum); break;
			case 3: //mtn
			case 4: //swammp
				placeLandFeature(10 + fnum); break;
			case 5: //lake
				placeLandFeature(3);  break;
			case 2:
				if(fnum == olapWith)
				{
					area = null;
					return;
				}
				placeRiverDelta(); break;
			/*case 3: placeMountain(); break;
			case 4: placeSwamp(); break;
			case 5: placeLake(); break;*/
			case 6: 
			case 8:
				int ww = area.width;
				int hh = area.height;
				
				double[] iSize = {0.4, 0.8, 0.95, 1.0};
				if(fnum == 8)
				{
					iSize[0] = 0.5;
					iSize[1] = 1.0;
				}
				double rw = UltimaRando.rand();
				for(int i = 0; i < iSize.length; i++)
				{
					if(rw < iSize[i])
					{
						ww = (area.width / 4) * (i + 1);
						break;
					}
				}
				double rh = UltimaRando.rand();
				for(int i = 0; i < iSize.length; i++)
				{
					if(rh < iSize[i])
					{
						hh = (area.height / 4) * (i + 1);
						break;
					}
				}
				if(fnum == 6)
					placeIsland(0.35, getCenterX(), getCenterY(), ww, hh); 
				else
					placeVolcanicIsland();
				if(filled.size() == 0)
					area = null;
				break;
			case 7: placeArchipelago(); break;
			case 9: placeShoal(); break;
			case 41:  placeHornIsland();  break;
			case 43:  placeSkullIsland();  break;
			case 42:  placeBellShoal();  break;
			}
			//if(filled.size() == 0 && fnum < 40 && area != null)
				//System.out.println("wtf");
			if(zones != null && area != null && filled.size() > 0)
			{
				terrain = (byte) fnum;
				findZone(zones);
			}
		}
		
		public boolean checkFeaturePlacement(int fnum)
		{
			switch(fnum)
			{
			case 41:  //horn island
				area.width = 10;
				area.height = 10;
				break;
			case 42:  //skull island
				area.width = 14;
				area.height = 13;
				break;
			case 43:  //bell shoal
				area.width = 5;
				area.height = 5;
				break;
			default:
				return true;
			}
			return !majorityIsLand();
		}
		
		private void placeBellShoal() 
		{
			area.width = 5;
			area.height = 5;
			terrain = 42;
			byte[][] bellShoal = {
					{3,3,3,3,3},
					{3,7,7,7,3},
					{3,7,1,7,3},
					{3,7,3,7,3},
					{3,3,3,3,3}};
			for(int i = 0; i < bellShoal.length; i++)
			{
				for(int j = 0; j < bellShoal[i].length; j++)
				{
					putTerrain(area.x + j, area.y + i, bellShoal[i][j]);
				}
			}
			innerPOI = new Point(area.x + 2, area.y + 2);
			filled.add(new Point(area.x, area.y));
		}

		private void placeSkullIsland() 
		{
			area.width = 14;
			area.height = 13;
			terrain = 43;
			byte[][] skullIsland = {
					{1,1,1,3,3,3,3,3,1,1,1,1,1,1},
					{1,1,3,3,7,7,7,3,3,3,1,1,1,1},
					{1,1,3,7,7,16,7,7,3,3,3,1,1,1},
					{1,1,3,7,16,17,16,7,3,3,3,3,3,1},
					{1,1,3,7,7,16,7,7,7,7,7,7,3,3},
					{1,3,3,3,7,7,7,7,7,7,16,7,7,3},
					{1,3,3,3,3,3,3,7,7,16,17,16,7,3},
					{3,3,7,7,7,3,3,7,7,7,16,7,7,3},
					{3,7,7,16,7,7,7,7,7,7,7,7,3,3},
					{3,7,16,17,16,7,7,7,3,3,3,3,3,1},
					{3,7,7,16,7,7,3,3,3,1,1,1,1,1},
					{3,3,7,7,7,3,3,1,1,1,1,1,1,1},
					{1,3,3,3,3,3,1,1,1,1,1,1,1,1}};
			
			for(int i = 0; i < skullIsland.length; i++)
			{
				for(int j = 0; j < skullIsland[i].length; j++)
				{
					if(skullIsland[i][j] == 1 && getTerrain(area.x + j, area.y + i) == 3)
						continue;
					putTerrain(area.x + j, area.y + i, skullIsland[i][j]);
				}
			}
			innerPOI = new Point(area.x + 6, area.y + 7);
			filled.add(new Point(area.x, area.y));
		}

		private void placeHornIsland() 
		{
			area.width = 10;
			area.height = 10;
			terrain = 41;
			byte[][] hornIsland = {
					{1,1,1,3,3,3,3,3,1,1},
					{1,1,3,3,3,3,3,3,3,1},
					{1,1,3,3,3,14,14,3,3,3},
					{1,3,3,3,15,15,14,3,3,3},
					{3,3,3,3,15,11,11,3,3,3},
					{3,3,3,15,15,11,3,3,3,1},
					{3,3,3,15,11,11,3,3,3,1},
					{3,3,3,11,11,3,3,3,1,1},
					{1,3,3,3,3,3,3,3,1,1},
					{1,1,3,3,3,3,3,1,1,1}};
			for(int i = 0; i < hornIsland.length; i++)
			{
				for(int j = 0; j < hornIsland[i].length; j++)
				{
					if(hornIsland[i][j] == 1 && getTerrain(area.x + j, area.y + i) == 3)
						continue;
					putTerrain(area.x + j, area.y + i, hornIsland[i][j]);
				}
			}
			innerPOI = new Point(area.x + 5, area.y + 5);
			filled.add(new Point(area.x, area.y));
		}

		private void placeShoal() 
		{
			double probNFI = 0.05;
			double probNFO = 0.55;
			double probN1 = 0.03;
			double probN2 = 0.02;  //fibonacci growth
			
			featureFill(7, 1.0, 1.0, true);
			while(featureFill(7, 1.0 - probNFI, 1.0 - probNFO, true))
			{
				probN2 = probN1;
				probN1 = probNFI;
				probNFI = probN2 + probN1;
				if(probNFI >= 0.55)
					probNFI = 0.55;
				Point pp = filled.get(fillPoint);
				if(zoneContains(pp.x, pp.y) == false)
				{
					probNFO += 0.15;
					if(probNFO > 0.85)
						probNFO = 0.85;
				}
			}
			
		}

		private void placeLandFeature(int fnum)
		{
			double probNFI = 0.05;
			double probNFO = 0.55;
			double probN1 = 0.03;
			double probN2 = 0.02;  //fibonacci growth
			
			featureFill(fnum, 1.0, 1.0, true);
			while(featureFill(fnum, 1.0 - probNFI, 1.0 - probNFO, true))
			{
				probN2 = probN1;
				probN1 = probNFI;
				probNFI = probN2 + probN1;
				if(probNFI >= 0.55)
					probNFI = 0.55;
				Point pp = filled.get(fillPoint - 1);
				if(zoneContains(pp.x, pp.y) == false)
				{
					probNFO += 0.15;
					if(probNFO > 0.85)
						probNFO = 0.85;
				}
			}
			if(filled.size() == 0)  //you couldn't fill yourself so no area for you (you don't exist)
				area = null;
		}
		
		private Rectangle getFilledRect()
		{
			int minX = Integer.MAX_VALUE;
			int maxX = 0;
			int minY = Integer.MAX_VALUE;
			int maxY = 0;
			for(int i = 0; i < filled.size(); i++)
			{
				Point p = filled.get(i);
				if(p.x < minX)
					minX = p.x;
				if(p.x > maxX)
					maxX = p.x;
				if(p.y < minY)
					minY = p.y;
				if(p.y > maxY)
					maxY = p.y;	
			}
			return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
		}
		
		
		
		public double[] stretchRange(double[] old, int index, double toRange, double factor)
		{
			double shrink =  1.0;
			double grow = 1.0;
			double sum = 0.0;
			double oldVal = old[index];
			if(index > 0)
				oldVal -= old[index - 1];
			if(factor > 0.1)
				toRange = oldVal * factor;
			double oldNeg = 1.0 - oldVal;
			double newNeg = 1.0 - toRange;
			shrink = newNeg / oldNeg;
			grow = toRange / oldVal;
			
			for(int i = old.length - 1; i > 0; i--)
			{
				if(i == index)
					old[i] = (old[i] - old[i - 1]) * grow;
				else
					old[i] = (old[i] - old[i - 1]) * shrink;
				//sum += old[i];
			}
			if(index == 0)
				old[0] *= grow;
			else
				old[0] *= shrink;
			for(int i = 0; i < old.length; i++)
			{
				old[i] += sum;
				sum = old[i];
			}
			//old[index] = 1.0 - neg;
			return old;
		}
		
		private Rectangle placeIsland(double cliffed, int x, int y, int w, int h) //returns the x2,y2 of the island created
		{
			int maxDx = w / 2;
			int maxDy = h / 2;
			double dIn = 1.0;
			byte pfb = mapData[y * 256 + x];
			featureFill(10, 1.0, 1.0, false);  //note that this feature fill requires land types to be different (you are placing land on water)
			if(filled.size() == 0)
			{
				return null;
			}
			/*if(filled.size() == 0)
			{
				System.out.println("FFFAil");
				fillPoint = -1;
				mapData[y * 256 + x] = pfb;
				featureFill(10, 1.0, 1.0, false);
			}*/
			int nZones = (int) ((UltimaRando.rand() * 3) + 1);
			byte[] ztype = new byte[nZones];
			for(int i = 0; i < nZones; i++)
			{
				double[] ranges = {0.2, 0.3, 0.6, 0.7, 0.9, 1.0};  //shrub, forest, plains, mountain, brick, swamp
				byte[] fval = {11, 12, 10, 13, 15, 14};
				double r = UltimaRando.rand();
				for(int k = 0; k < ranges.length; k++)
				{
					if(r > ranges[k])
						continue;
					ztype[i] = fval[k];
					break;
				}
			}
			for(int i = 0; i < nZones; i++)
			{
				while(true)
				{
					Point pp = filled.get(fillPoint);
					int dcx = Math.abs(pp.x - x);
					int dcy = Math.abs(pp.y - y);
					dIn = ((dcx + dcy) * 1.0) / ((maxDx + maxDy) * 1.0);
					//dIn *= dIn;
					dIn = 1.0 - dIn;
					if(dIn < 0.12)
						dIn = 0.12;
					double dOut = 0.12;
					if(featureFill(ztype[i], dIn, dOut, false) == false)
						break;
				}
				fillPoint = (int) (UltimaRando.rand() * filled.size());
			}
			//System.out.println(printIslandData());
			for(int i = 0; i < filled.size(); i++)  //fill in cliff edges
			{
				Point p = filled.get(i);
				//int xx = p.x;
				//int yy = p.y;
				byte[] edges = getSurr4(p);
				boolean isEdge = false;
				for(int j = 0; j < 4; j++)
				{
					if(edges[j] == 1)
					{
						isEdge = true;
						break;
					}
				}
				if(isEdge && UltimaRando.rand() < cliffed)
					mapData[p.y * 256 + p.x] = 13;
			}
			//System.out.println(printIslandData());
			/*for(int i = filled.size() - 1; i >= 0; i--)
			{
				Point p = filled.get(i);
				byte bb = mapData[p.y * 256 + p.x];
				if(bb == 1 || bb == 13)
					continue;
				byte[] s4 = getSurr4(p);
				double[] ranges = {0.2, 0.3, 0.5, 0.6, 0.8, 0.9, 1.0};  //shrub, forest, plains, mountain, brick, swamp, lake
				int[] fval = {11, 12, 10, 13, 15, 14, 3};
				for(int j = 0; j < 4; j++)
				{
					bb = s4[j];
					switch(bb)
					{
					case 13:  //mountain
						if((ranges[4] - ranges[3]) < 0.6)
							ranges = stretchRange(ranges, 4, 0.6, 0.0);
						else
							ranges = stretchRange(ranges, 4, 0.0, 2.0);
						break;
					case 3: //water
						ranges = stretchRange(ranges, 2, 0.0, 1.5);
						ranges = stretchRange(ranges, 5, 0.0, 1.5);
						break;
					case 11: //shrub
						ranges = stretchRange(ranges, 0, 0.0, 1.5);
						ranges = stretchRange(ranges, 5, 0.0, 1.25);
						break;
					case 12: //forest
						ranges = stretchRange(ranges, 0, 0.0, 1.5);
						ranges = stretchRange(ranges, 1, 0.0, 1.5);
						break;
					case 14: //swamp
						ranges = stretchRange(ranges, 5, 0.0, 1.25);
						break;
					}
				}
				double t = UltimaRando.rand();
				for(int j = 0; j < ranges.length; j++)
				{
					if(t < ranges[j])
					{
						mapData[p.y * 256 + p.x] = (byte) fval[j];
						break;
					}
				}
			}*/
			
			//System.out.println(printIslandData());
			return getFilledRect();
		}
		
		private String printIslandData()
		{
			//System.out.println("Island");
			Rectangle r = getFilledRect();
			String o = "Island\n";
			for(int yy = r.y; yy <= r.y + r.height; yy++)
			{
				for(int xx = r.x; xx <= r.x + r.width; xx++)
				{
					byte b = mapData[yy * 256 + xx];
					if(b == 1)
						o += ".";
					else if(b == 3)
						o += "?";
					else
						o += "X";
				}
				o += "\n";
			}
			return o;
		}
		
		private void placeArchipelago()
		{
			int maxIslands = area.width * area.height / 16;
			int nIslands = (int) (UltimaRando.rand() * maxIslands);
			if(nIslands == 0)
			{
			//	System.out.println("0 islands in archipelago");
				area = null;
				return;
			}
			int w = area.width / nIslands;
			int h = area.height / nIslands;
			if(w < 2 || h < 2)
			{
				//System.out.println("Archipelago islands are not large enough");
				area = null;
				return;
			}
			ArrayList<Rectangle> currIslands = new ArrayList<Rectangle>();
			ArrayList<Point> allPoints = new ArrayList<Point>();
			for(int i = 0; i < nIslands; i++)
			{
				int xx = 0; 
				int yy = 0; 
				int fail = 0;
				while(fail < 40)
				{
					xx = (int) ((UltimaRando.rand() * area.width) + area.x);
					yy = (int) ((UltimaRando.rand() * area.height) + area.y);
					int j = 0;
					for(; j < currIslands.size(); j++)
					{
						if(currIslands.get(j).contains(new Point(xx, yy)))
							break;
					}
					if(j == currIslands.size())
						break;
					else
						fail++;
				}
				if(fail == 40)
					break;
				allPoints.addAll(filled);
				filled.clear();
				fillPoint = -1;
				Rectangle r = placeIsland(0.10, xx, yy, w, h); //placeisland fails because the rectangle's center is already on land
				if(r == null)
					continue;
				currIslands.add(r);
			}
			filled = allPoints;
			if(filled.size() == 0)
			{
				area = null;
				//System.out.println("Failed to fill any points in archipelago");
				return;
			}
			//System.out.println("Archipelago islands placed=" + currIslands.size());
		}
		
		private void placeVolcanicIsland()
		{
			Rectangle rr = placeIsland(0.95, getCenterX(), getCenterY(), area.width, area.height);
			if(rr == null)
				return;
			double[] ranges = {0.1, 0.6, 0.95, 1.0};  //brick, lava, crater
			byte[] types = {13, 15, 16, 17};
			for(int i = 0; i < filled.size(); i++)
			{
				Point p = filled.get(i);
				byte b = getTerrain(p.x, p.y);
				if(b >= 10 && b != 13)
				{
					double r = UltimaRando.rand();
					for(int k = 0; k < ranges.length; k++)
					{
						if(r < ranges[k])
						{
							putTerrain(p.x, p.y, types[k]);
							break;
						}
					}
				}
				
			}
		}
		
		class ITree
		{
			int l;
			ITree left;
			int pLeft;
			ITree right;
			int pRight;
			double branchPct;
			int pos;  //horiz position
			int y;
			boolean isValid;
			
			ITree(int len)
			{
				l = len;
				left = null;
				right = null;
				branchPct = 0.85;
				pos = 0;
				y = 0;
				isValid = true;
			}
			
			public void addLeft(ITree t, int p)
			{
				pLeft = p;
				left = t;
				left.branchPct = branchPct - 0.2;
				left.pos = pos - 2;
				if(UltimaRando.rand() > 0.75)
					left.pos--;
				left.y = y + p;
			}
			
			public void addRight(ITree t, int p)
			{
				pRight = p;
				right = t;
				right.branchPct = branchPct - 0.2;
				right.pos = pos + 2;
				if(UltimaRando.rand() > 0.75)
					right.pos++;
				right.y = y + p;
			}

			public boolean invalidate(ITree b) 
			{
				if(b == null)
					return true;
				if(b == left)
				{
					left.isValid = false;
					left.invalidate(left.left);
					left.invalidate(left.right);
					pLeft = 0;
					left = null;
					return true;
				}
				else if(b == right)
				{
					right.isValid = false;
					right.invalidate(right.left);
					right.invalidate(right.right);
					pRight = 0;
					right = null;
					return true;
				}
				else
				{
					boolean x = false;
					if(left != null)
						x = left.invalidate(b);
					if(right != null && x == false)
						x = right.invalidate(b);
					return x;
				}
			}

			public String printString() 
			{
				String s = "x=" + pos;
				s += " y=" + y;
				s += " len=" + l;
				s += " lf=" + pLeft;
				s += " rg=" + pRight;
				return s;
			}
		}
		
		private ArrayList<ITree> getRiverDelta()
		{
			ArrayList<ITree> rv = new ArrayList<ITree>();
			int ll = (int) ((UltimaRando.rand() * 8) + 3);
			rv.add(new ITree(ll));
			int idx = 0;
			while(idx < rv.size())
			{
				ITree current = rv.get(idx);
				if(UltimaRando.rand() < current.branchPct)  //add a right
				{
					ll = (int) ((UltimaRando.rand() * 8) + 3);
					ITree lft = new ITree(ll);
					int bp = (int) ((UltimaRando.rand() * (current.l - 1)) + 1);
					current.addLeft(lft, bp);
					rv.add(lft);
				}
				if(UltimaRando.rand() < current.branchPct) //add a left
				{
					ll = (int) ((UltimaRando.rand() * 8) + 3);
					ITree rgt = new ITree(ll);
					int bp = (int) ((UltimaRando.rand() * (current.l - 1)) + 1);
					current.addRight(rgt, bp);
					rv.add(rgt);
				}
				idx++;
			}
			ArrayList<Integer> ends = new ArrayList<Integer>();
			for(int i = 0; i < rv.size(); i++)
			{
				if(rv.get(i).isValid == false)
					continue;
				int pos = rv.get(i).pos;
				if(ends.contains(pos))
				{
					for(int j = i - 1; j >= 0; j--)
					{
						if(rv.get(j).isValid == false)
							continue;
						int pos2 = rv.get(j).pos;
						if(pos2 == pos)
						{
							ITree a = rv.get(i);
							ITree b = rv.get(j);
							if(a.y < b.y) //a is higher on tree
							{
								if(a.l + a.y >= b.y + 1)
								{
									int bot = b.y + b.l;
									a.l = bot - a.y;
									a.invalidate(b);
									//b.isValid = false;
								}
							}
							else
							{
								if(b.l + b.y >= a.y + 1)
								{
									int bot = a.y + a.l;
									b.l = bot - b.y;
									b.invalidate(a);
									//a.isValid = false;
								}
							}
						}
					}
				}
				else
					ends.add(pos);
			}
			return rv;
		}
		
		private Rectangle placeRiverDelta()
		{
			terrain = 45;
			Point pc = new Point(getCenterX(), getCenterY());
			Point pa = null;
			int initDir = 0;
			int initPoss = 0;
			int msqr = (int) Math.sqrt(mapData.length);
			boolean v[] = new boolean[mapData.length];
			ArrayList<ITree> tree = getRiverDelta();
			if(mapData[pc.y * msqr + pc.x] != 1)
			{
				ArrayList<Point> toSea = new ArrayList<Point>();
				toSea.add(pc);
				int point = 0;
				v[pc.y * msqr + pc.x] = true;
				while(true)
				{
					Point p = toSea.get(point);
					if(mapData[p.y * msqr + p.x] == 1)
						break;
					ArrayList<Point> ta = getSurrPoints(p);
					for(int j = 0; j < ta.size(); j++)
					{
						Point tj = ta.get(j);
						if(v[tj.y * msqr + tj.x] == false)
						{
							v[tj.y * msqr + tj.x] = true;
						//if(!toSea.contains(ta.get(j)))
							toSea.add(ta.get(j));
						}
					}
					point++;
				}
				ArrayList<Point> landAnchor = getSurrPoints(toSea.get(point));  //point is a sea point
				for(int i = 0; i < 4; i++)
				{
					Point p = landAnchor.get(i);
					if(mapData[p.y * msqr + p.x] != 1)
					{
						pa = p;
						initPoss |= ( 1 << i);
						//initDir = i;
						break;
					}
				}
			}
			else
			{
				ArrayList<Point> toLand = new ArrayList<Point>();
				toLand.add(pc);
				int point = 0;
				v[pc.y * msqr + pc.x] = true;
				while(true)
				{
					Point p = toLand.get(point);
					if(mapData[p.y * msqr + p.x] != 1)
						break;
					ArrayList<Point> ta = getSurrPoints(p);
					for(int j = 0; j < ta.size(); j++)
					{
						Point tj = ta.get(j);
						if(v[tj.y * msqr + tj.x] == false)
						{
						//if(!toLand.contains(ta.get(j)))
							v[tj.y * msqr + tj.x] = true;
							toLand.add(ta.get(j));
						}
					}
					point++;
				}
				ArrayList<Point> landAnchor = getSurrPoints(toLand.get(point));  //this is a land point
				pa = toLand.get(point);
				for(int i = 3; i >= 0; i--)
				{
					Point p = landAnchor.get(i);
					if(mapData[p.y * msqr + p.x] == 1)
					{
						//pa = p;
						initPoss |= ( 1 << (i ^ 1));
						//initDir = i ^ 1;
						break;
					}
				}
			}
			int dx = 0;
			int dy = 0;
			//get the biggest y
			int bigY = 0;
			for(int j = 0; j < tree.size(); j++)
				if(tree.get(j).y + tree.get(j).l > bigY)
					bigY = tree.get(j).y + tree.get(j).l;
			for(int i = 0; i < 5; i++)
			{
				if(i == 4)  //you cannot place the river delta
				{
					area = null;
					return null;
				}
				if(((initPoss >> i) & 1) == 0)
					continue;
				initDir = i;
				
				switch(initDir)
				{
					case 0: dx = -1; dy = 0; break;
					case 1: dx = 1; dy = 0; break;
					case 2: dx = 0; dy = -1; break;
					case 3: dx = 0; dy = 1; break;
				}
				int xx = pa.x + (dx * bigY);  if(xx < 0) xx += msqr;  if(xx >= msqr) xx -= msqr;
				int yy = pa.y + (dy * bigY);  if(yy < 0) yy += msqr;  if(yy >= msqr) yy -= msqr;
				//System.out.println("IPoss=" + initPoss + "  IDir=" + initDir + "  MData=" + mapData[yy * msqr + xx]);
				if(mapData[yy * msqr + xx] >= 10)
					break;
			}
			
			
			for(int i = 0; i < tree.size(); i++)
			{
				ITree t = tree.get(i);
				if(!t.isValid)
					continue;
				int xx = pa.x;
				int yy = pa.y;
				//move into position
				if(dx == 0){xx += t.pos; if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr; 
				            yy = pa.y + (dy * t.y);  if(yy < 0) yy += msqr;  if(yy >= msqr) yy -= msqr;}
				else{yy += t.pos; if(yy < 0) yy += msqr; if(yy >= msqr) yy -= msqr;
					 xx = pa.x + (dx * t.y);  if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr;}
				//System.out.println(t.printString());
				//System.out.println("Starting at " + (yy - pa.y) + "," + (xx - pa.x));
				for(int d = 0; d < t.l; d++)  //do the length
				{
					mapData[yy * msqr + xx] = 7;
					filled.add(new Point(xx, yy));
					xx += dx;  if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr;
					yy += dy;  if(yy < 0) yy += msqr; if(yy >= msqr) yy -= msqr;
					
				}
				ITree tl = t.left;
				if(tl != null && tl.isValid)  //left side stub
				{
					xx -= (t.l - t.pLeft) * dx;  if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr;
					yy -= (t.l - t.pLeft) * dy;  if(yy < 0) yy += msqr; if(yy >= msqr) yy -= msqr;
					int dd = 1;
					if(tl.pos > t.pos)
						dd = -1;
					for(int d = tl.pos; d != t.pos; d += dd)
					{
						if(dx == 0)
						{
							int ax = pa.x + d; if(ax < 0) ax += msqr; if(ax >= msqr) ax -= msqr;
							mapData[msqr * yy + ax] = 7;
							filled.add(new Point(ax, yy));
						}
						else
						{
							int ay = pa.y + d; if(ay < 0) ay += msqr; if(ay >= msqr) ay -= msqr;
							mapData[msqr * ay + xx] = 7;
							filled.add(new Point(xx, ay));
						}
					}
					xx += (t.l - t.pLeft) * dx;  if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr;
					yy += (t.l - t.pLeft) * dy;  if(yy < 0) yy += msqr; if(yy >= msqr) yy -= msqr;
				}
				ITree tr = t.right;
				if(tr != null && tr.isValid)  //right side stub
				{
					xx -= (t.l - t.pRight) * dx;  if(xx < 0) xx += msqr; if(xx >= msqr) xx -= msqr;
					yy -= (t.l - t.pRight) * dy;  if(yy < 0) yy += msqr; if(yy >= msqr) yy -= msqr;
					int dd = 1;
					if(tr.pos > t.pos)
						dd = -1;
					for(int d = tr.pos; d != t.pos; d += dd)
					{
						if(dx == 0)
						{
							int ax = pa.x + d; if(ax < 0) ax += msqr; if(ax >= msqr) ax -= msqr;
							mapData[msqr * yy + ax] = 7;
							filled.add(new Point(ax, yy));
						}
						else
						{
							int ay = pa.y + d; if(ay < 0) ay += msqr; if(ay >= msqr) ay -= msqr;
							mapData[msqr * ay + xx] = 7;
							filled.add(new Point(xx, ay));
						}
					}
				}
			}
			//calculate the rectangle
			int minX = Integer.MAX_VALUE;
			int maxX = 0;
			int minY = Integer.MAX_VALUE;
			int maxY = 0;
			for(int i = 0; i < tree.size(); i++)
			{
				ITree t = tree.get(i);
				if(!t.isValid)
					continue;
				if(t.pos < minX)
					minX = t.pos;
				if(t.pos > maxX)
					maxX = t.pos;
				if(t.y < minY)
					minY = t.y;
				if(t.l + t.y > maxY)
					maxY = t.l + t.y;
			//	System.out.print(t.printString() + "; ");
			}
		//	System.out.println();
			area = new Rectangle(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
			return area;
		}
		
		public void testRiverDelta()
		{
			mapData = new byte[4096];
			for(int i = 0; i < 4096; i++)
			{
				if(i > 3456)
					mapData[i] = 1;
				else
					mapData[i] = 10;
			}
			int xx = 10;
			int yy = 10;
			MapZone mz = new MapZone(new Rectangle(xx, yy, 40, 40), (byte) 2, -1);
			mz.placeFeature(2);
			for(int i = 0; i < 64; i++)
			{
				for(int j = 0; j < 64; j++)
				{
					byte b = mapData[64 * i + j];
					if(b == 1)
						System.out.print("0");
					else if(b == 10)
						System.out.print(".");
					else
						System.out.print("X");
				}
				System.out.println();
			}
			
		}
		
		public boolean featureFill(int val, double probIn, double probOut, boolean requireMatch)
		{
			if(fillPoint == -1)
			{
				int cx = area.x + (area.width / 2);
				int cy = area.y + (area.height / 2);
				filled = new ArrayList<Point>();
				fillFeaturePoint(cx, cy, val, probIn, probOut, requireMatch);
				
				fillPoint = 0;
				return true;
			}
			else
			{
				if(fillPoint >= filled.size())
					return false;
				Point cp = filled.get(fillPoint);
				int xx = cp.x;
				int yy = cp.y;
				xx++;
				if(xx > 255)
					xx = 0;
				fillFeaturePoint(xx, yy, val, probIn, probOut, requireMatch);
				xx -= 2;
				if(xx < 0)
					xx += 256;
				fillFeaturePoint(xx, yy, val, probIn, probOut, requireMatch);
				xx = cp.x;
				yy++;
				if(yy > 255)
					yy = 0;
				fillFeaturePoint(xx, yy, val, probIn, probOut, requireMatch);
				yy -= 2;
				if(yy < 0)
					yy += 256;
				fillFeaturePoint(xx, yy, val, probIn, probOut, requireMatch);
				fillPoint++;
				if(fillPoint >= filled.size())
					return false;
			}
			return true;
		}

		public boolean centerFill()
		{
			if(fillPoint == -1)
			{
				int cx = area.x + (area.width / 2);
				int cy = area.y + (area.height / 2);
				filled = new ArrayList<Point>();
				fillPoint(cx, cy);
				fillPoint = 0;
				return true;
			}
			else
			{
				if(fillPoint >= filled.size())
					return false;
				Point cp = filled.get(fillPoint);
				int xx = cp.x;
				int yy = cp.y;
				xx++;
				if(xx > 255)
					xx = 0;
				fillPoint(xx, yy);
				xx -= 2;
				if(xx < 0)
					xx += 256;
				fillPoint(xx, yy);
				xx = cp.x;
				yy++;
				if(yy > 255)
					yy = 0;
				fillPoint(xx, yy);
				yy -= 2;
				if(yy < 0)
					yy += 256;
				fillPoint(xx, yy);
				fillPoint++;
			}
			return true;
		}
		
		public int getTotalArea()
		{
			return area.width * area.height;
		}
		
		public int getLandArea()
		{
			int rv = 0;
			for(int y = area.y; y < area.y + area.height; y++)
			{
				for(int x = area.x; x < area.x + area.width; x++)
				{
					byte b = mapData[256 * y + x];
					if(b > 7 || (b & 1) == 0)
						rv++;
				}
			}
			return rv;
		}
		
		public int getWaterArea()
		{
			int a = getTotalArea();
			return a - getLandArea();
		}
		
		public boolean majorityIsLand()
		{
			int a = getTotalArea();
			int l = getLandArea();
			if(l > (a - l))
				return true;
			else 
				return false;
		}

		public boolean heightFill(double[] heightMap) 
		{
			if(fillPoint == -1)
			{
				int cx = area.x + (area.width / 2);
				int cy = area.y + (area.height / 2);
				filled = new ArrayList<Point>();
				fillPoint(cx, cy);
				heightMap[cy * 256 + cx] = getHeight(cx, cy, heightMap);
				fillPoint = 0;
				return true;
			}
			else
			{
				if(fillPoint >= filled.size())
					return false;
				Point cp = filled.get(fillPoint);
				int xx = cp.x;
				int yy = cp.y;
				xx++;
				if(xx > 255)
					xx = 0;
				fillPoint(xx, yy);
				heightMap[xx * 256 + yy] = getHeight(xx, yy, heightMap);
				xx -= 2;
				if(xx < 0)
					xx += 256;
				fillPoint(xx, yy);
				heightMap[xx * 256 + yy] = getHeight(xx, yy, heightMap);
				xx = cp.x;
				yy++;
				if(yy > 255)
					yy = 0;
				fillPoint(xx, yy);
				heightMap[xx * 256 + yy] = getHeight(xx, yy, heightMap);
				yy -= 2;
				if(yy < 0)
					yy += 256;
				fillPoint(xx, yy);
				heightMap[xx * 256 + yy] = getHeight(xx, yy, heightMap);
				fillPoint++;
			}
			return true;
		}
		
		private byte getBaseHeight(byte terrain)
		{
			switch(terrain)
			{
			case 1: return -16;
			case 10: return 1; 
			case 12: return 3;
			case 13: return 16;
			default: return 0;
			}
		}
		
		private double getHeight(int x, int y, double[] heightMap)
		{
			//base terrain height
			ArrayList<Point> surrPoints = getSurrPoints(new Point(x ,y));
			byte base = getBaseHeight(mapData[256 * y + x]);
			double avg = base;
			int count = 1;
			for(int i = 0; i < surrPoints.size(); i++)
			{
				Point p = surrPoints.get(i);
				if(mapData[256 * p.y + p.x] == 0)
					continue;
				avg += heightMap[256 * p.y + p.x];
				count++;
			}
			avg /= count;
			double r = (UltimaRando.rand() * 32) - 16;
			if(r > avg)
			{
				//double a = r - avg;
				avg += ((Math.abs(avg) / 16.0));
			}
			else
			{
				//double a = avg - r;
				avg -= ((Math.abs(avg) / 16.0));
			}
			//avg = Math.floor(avg + 0.5);
			//return (byte) avg;
			return avg;
		}

		
		/*private void fillHeightPoint(int x, int y, byte[] heightMap) 
		{
			if(mapData[256 * y + x] == 0)
			{
				mapData[256 * y + x] = terrain;
				filled.add(new Point(x, y));
			}
			else if(mapData[256 * y + x] == terrain)
				return;
			else if(UltimaRando.rand() > 0.85)
			{
				mapData[256 * y + x] = terrain;
				filled.add(new Point(x, y));
			}
			
		}*/
	}
	
	public static boolean containsCenterDiagonal(Rectangle r) 
	{
		//test top horizontal
		if(r.y >= r.x && r.y <= (r.x + r.width))
			return true;
		//test side vertical
		if(r.x >= r.y && r.x <= (r.y + r.height))
			return true;
		return false;
	}

	
	Map(int w, int h)
	{
		mapData = new byte[w * h];
		zoneData = new byte[w * h];
		zones = null;
		features = null;
	}
	
	public void setSeaPad(int amt)
	{
		seaPad = amt;
		int noise = 4;
		int r = 0;
		for(int j = 0; j < 256; j++)
		{
			r = (int) (UltimaRando.rand() * 8);
			if(r < noise)
				noise--;
			if(r > noise)
				noise++;
			for(int i = 0; i < seaPad + noise - 4; i++)
				mapData[(i << 8) + j] = 1;
		}
		//System.out.println();
		noise = 4;
		for(int j = 0; j < 256; j++)
		{
			r = (int) (UltimaRando.rand() * 8);
			if(r < noise)
				noise--;
			if(r > noise)
				noise++;
			for(int i = 256 - seaPad - noise + 4; i < 256; i++)
				mapData[(i << 8) + j] = 1;
		}
		//System.out.println();
		int noise2 = 4;
		for(int i = seaPad - 4; i < 256 - seaPad + 4; i++)
		{
			r = (int) (UltimaRando.rand() * 8);
			if(r < noise)
				noise--;
			if(r > noise)
				noise++;
			for(int j = 0; j < seaPad + noise - 4; j++)
				mapData[(i << 8) + j] = 1;
			r = (int) (UltimaRando.rand() * 8);
			if(r < noise2)
				noise2--;
			if(r > noise2)
				noise2++;
			for(int j = 256 - seaPad - noise2 + 4; j < 256; j++)
				mapData[(i << 8) + j] = 1;
		}
		//MapWindow mwn = new MapWindow(mapData, false);
	}
	
	public void setRandParams()
	{
		
	}
	
	ArrayList<Point> vbranches;
	public ArrayList<Point> visibilityFill(Point origin)
	{
		ArrayList<Point> toLand = new ArrayList<Point>();
		toLand.add(origin);
		int wp = 0;
		while(wp < toLand.size())
		{
			Point wwp = toLand.get(wp);
			byte b = mapData[wwp.x + 256 * wwp.y];  //test to expand
			byte c = visibility[wwp.x + 256 * wwp.y];
			if(b != 12 && b != 13)
			{
				ArrayList<Point> sp = getSurrPoints(wwp);
				for(int i = 0; i < 4; i++)
				{
					Point wn = sp.get(i);
					byte d = visibility[wn.x + 256 * wn.y];
					if(d == 0)
						toLand.add(wn);  //always add
					visibility[wn.x + 256 * wn.y] = 1;
				}
			}
			visibility[wwp.x + 256 * wwp.y] = 1;
			wp++;
		}
		//System.out.print("vfill");
		boolean isMain = false;
		for(int i = 0; i < toLand.size(); i++)
		{
			Point p = toLand.get(i);
			if(mapData[p.x + 256 * p.y] == 1)
			{
				isMain = true;
				break;
			}
		}
		if(!isMain)
		{
			for(int i = 0; i < toLand.size(); i++)
			{
				Point p = toLand.get(i);
				visibility[p.x + 256 * p.y] = 2;  //zone 1 -> 2
				byte b = mapData[p.x + 256 * p.y];
				if(b != 12 && b != 13)  //this flood select captures the corners
				{
					ArrayList<Point> pts = getSurrPoints(p);
					for(int j = 0; j < pts.size(); j++)
					{
						Point p2 = pts.get(j);
						if(visibility[p2.x + 256 * p2.y] != 2)
							toLand.add(p2);
					}
				}
			}
		}
		if(toLand.size() == 1)
		{
			if(getTerrain(origin.x, origin.y) == 12)
				visibility[origin.x + 256 * origin.y] = 3;
			else
			{
				visibility[origin.x + 256 * origin.y] = 4;
				ArrayList<Point> vb = getSurrPoints(origin);  //1 step; still need 2nd step away
				for(int i = 0; i < vb.size(); i++)
				{
					Point p = vb.get(i);
					ArrayList<Point> vb2 = getSurrPoints(p);  //this is the 2nd step
					for(int j = 0; j < vb2.size(); j++)
					{
						Point p2 = vb2.get(j);
						byte a = accessData[p2.x + 256 * p2.y];
						if(visibility[p2.x + 256 * p2.y] == 1 && a > 0 && a < 8)
							vbranches.add(p2);
					}
				}
			}
		}
		return toLand;
	}
	
	private void setupVisibility()
	{
		visibility = new byte[mapData.length];
		System.out.print("vis;");
		vbranches = new ArrayList<Point>();
		for(int y = 0; y < 256; y++)
		{
			for(int x = 0; x < 256; x++)
			{
				if(visibility[x + 256 * y] == 0)
					visibilityFill(new Point(x, y));
			}
		}
		System.out.print("vis2;");
		int fixed = 0;
		for(int y = 0; y < 256; y++)
		{
			for(int x = 0; x < 256; x++)
			{
				if(visibility[x + 256 * y] > 1)
					fixed += visibilityFix(x, y);
			}
		}
		System.out.println("vf" + fixed + ";");
		/*MapWindow mwr = new MapWindow(mapData, false);
		mwr.setAccessData(accessData);
		mwr.setVisibilityData(visibility);*/
		for(int i = 0; i < vbranches.size(); i++)
			testVBranch(vbranches.get(i), false);
	}
	
	private int visibilityFix(int x, int y) //ensures that all edge mountains and forests are visible from zone 0
	{
		ArrayList<Point> s4 = getSurrPoints(new Point(x, y));
		for(int i = 0; i < s4.size(); i++)
		{
			Point p = s4.get(i);
			int loc = p.x + 256 * p.y;
			if(visibility[loc] == 1 && mapData[loc] != 12 && mapData[loc] != 13)
			{
				visibility[x + 256 * y] = 1;
				return 1;
			}
		}
		return 0;
	}


	private int[] testVBranch(Point p, boolean distOnly)
	{
		if(visibility[p.x + 256 * p.y] == 1 || distOnly)
		{
			//I'm at a branch opening
			ArrayList<Point> s4 = getSurrPoints(p);
			int[] dists = {0,0,0,0};
			for(int i = 0; i < 4; i++)
			{
				int k = 1;
				int dx = 0;
				int dy = 0;
				switch(i)
				{
				case 0: dx = -1; break;
				case 1: dx = 1;  break;
				case 2: dy = -1; break;
				case 3: dy = 1;  break;
				}
				while(true)
				{
					Point np = new Point(p.x + k * dx, p.y + k * dy);
					np = normalizePoint(np);
					if(visibility[np.x + 256 * np.y] == 1)
						k++;
					else
						break;
					if(k > 40)
						break;
				}
				dists[i] = k;//get vchoke and hchoke, get min hchoke/vchoke of each side
			}
			
			int[] choke = {0,0};
			choke[0] = dists[0] + dists[1] + 1;
			choke[1] = dists[2] + dists[3] + 1;
			if(distOnly)
				return choke;
			if(Math.min(choke[0], choke[1]) > 8) //not a choke point
				return null;
			//int closestChoke = Math.min(choke[0], choke[1]);
			int[] other = {0, 0};
			for(int i = 0; i < 4; i++)
			{
				Point po = s4.get(i);
				if(choke[0] < choke[1] && i >= 2)  //check horiz chokes of 3,4
				{
					int[] open = testVBranch(po, true);
					other[i - 2] = open[0];
				}
				else if(choke[1] < choke[0] && i < 2)
				{
					int[] open = testVBranch(po, true);
					other[i] = open[1];
				}
			}
			ArrayList<Point> fillLine = new ArrayList<Point>();
			boolean hChoke = false;
			if(choke[0] < choke[1])
			{
				if(Math.min(dists[2], dists[3]) < 3)  //not a choke point
					return null; 
				byte vis = 0;
				Point po;
				if(other[0] < other[1])  //the above horizontal choke < the below horizontal choke
					po = new Point(p.x, p.y + 1);  //po cuts off the bottom
				else
					po = new Point(p.x, p.y - 1);  //po cuts off the top
				po = normalizePoint(po);
				vis = visibility[po.x + 256 * po.y];
				if(vis < 5 || vis == 7)  vis = 5;
				else vis = 7;
				/*vis = (byte) (visibility[po.x + 256 * po.y] + 1);
				if(vis == 8)
					vis--;*/
				fillLine.add(po);
				//draw the horizontal line
				for(int i = 0; i <= dists[0]; i++)
				{
					Point pl = new Point(p.x - i, p.y);
					pl = normalizePoint(pl);
					visibility[pl.x + 256 * pl.y] = vis;
					fillLine.add(pl);
				}
				for(int i = 0; i <= dists[1]; i++)
				{
					Point pl = new Point(p.x + i, p.y);
					pl = normalizePoint(pl);
					visibility[pl.x + 256 * pl.y] = vis;
					fillLine.add(pl);
				}
				System.out.println("Added H mtn choke point at " + p + "; po=" + po + "; chk=" + choke[0]);
				hChoke = true;
			}
			else
			{
				if(Math.min(dists[0], dists[1]) < 3)  //not a choke point
					return null; 
				byte vis = 0;
				Point po;
				if(other[0] < other[1])  //if the left vertical choke < the right vertical choke
					po = new Point(p.x + 1, p.y);  //cut off filling to the right
				else
					po = new Point(p.x - 1, p.y);  //cut off fillig to the left
				po = normalizePoint(po);
				byte vs = visibility[po.x + 256 * po.y];
				if(vs < 5 || vs == 7)  vs = 5;
				else vs = 7;
				/*vis = (byte) (visibility[po.x + 256 * po.y] + 1);
				if(vis == 8)
					vis -= 2; */ //7->6, not 8
				fillLine.add(po);
				//draw the vertical line
				for(int i = 0; i <= dists[2]; i++)
				{
					Point pl = new Point(p.x, p.y - i);
					pl = normalizePoint(pl);
					visibility[pl.x + 256 * pl.y] = vis;
					fillLine.add(pl);
				}
				for(int i = 0; i <= dists[3]; i++)
				{
					Point pl = new Point(p.x, p.y + i);
					pl = normalizePoint(pl);
					visibility[pl.x + 256 * pl.y] = vis;
					fillLine.add(pl);
				}
				System.out.println("Added V mtn choke point at " + p + "; po=" + po + "; chk=" + choke[1]);
			}
			visibilityFill2(p, fillLine, hChoke);
		}
		int[] rv = {0,0};
		return rv;
		
	}
	
	private void visibilityFill2(Point p, ArrayList<Point> fillLine, boolean horiz) 
	{
		boolean[] sel = new boolean[visibility.length];
		ArrayList<Point> toLand = new ArrayList<Point>();
		toLand.add(p);
		visibility[p.x + 256 * p.y] = 1;
		byte val = (byte) (visibility[p.x + 256 * p.y] + 1);  //set everything to val
		for(int i = 0; i < fillLine.size(); i++)  //this sets up the guard against filling both sides
		{
			Point px = fillLine.get(i);
			sel[px.x + 256 * px.y] = true;
		}
		sel[p.x + 256 * p.y] = false;
		int wp = 0;
		int score = 0;
		while(wp < toLand.size())
		{
			Point wwp = toLand.get(wp);
			byte d = mapData[wwp.x + 256 * wwp.y];
			if(d == 1)  //too far
			{
				score = -1;
				break;
			}
			if(d != 12 && d != 13)  //mountain or forest: don't expand
			{
				ArrayList<Point> sp = getSurrPoints(wwp);
				for(int i = 0; i < 4; i++)
				{
					Point wn = sp.get(i);
					boolean c = sel[wn.x + 256 * wn.y];
					byte b = visibility[wn.x + 256 * wn.y];
					if(c == false && b == 1)
					{
						//visibility[wn.x + 256 * wn.y] = val;
						toLand.add(wn);  
						sel[wn.x + 256 * wn.y] = true;
						//System.out.print("+");
						//
						if(horiz)
							score += ((wn.y - p.y) * (wn.y - p.y));
						else
							score += ((wn.x - p.x) * (wn.x - p.x));
						if(ensurePOIUnique(new Rectangle(wn.x, wn.y, 1, 1), false) == false)
							score += 16;
					}
				}
			}
			
			wp++;
		}
		if(score > 30)
			System.out.println("  accepted choke point with a score of " + score);
		else
			System.out.println("  rejected choke point with a score of " + score);
		for(int i = 0; i < toLand.size(); i++)
		{
			Point wn = toLand.get(i);
			if(score > 30)
				visibility[wn.x + 256 * wn.y] = val;
			else if(visibility[wn.x + 256 * wn.y] >= 5)
				visibility[wn.x + 256 * wn.y] = 1;  //not a sufficiently good fill point
		}
		for(int i = 0; i < fillLine.size(); i++)
		{
			Point wn = fillLine.get(i);
			if(score > 30)
				visibility[wn.x + 256 * wn.y] = val;
			else if(visibility[wn.x + 256 * wn.y] >= 5)
				visibility[wn.x + 256 * wn.y] = 1;  //not a sufficiently good fill point
		}
	}

	private void reportFeatures()
	{
		int[] fType = new int[50];
		for(int i = 0; i < features.size(); i++)
		{
			MapZone ff = features.get(i);
			fType[ff.terrain]++;
		}
		for(int i = 0; i < fType.length; i++)
		{
			if(fType[i] > 0)
				System.out.println("Type #" + i + ":" + fType[i]);
		}
		System.out.println(features.size() + " total features");
	}
	
	private void smoothMap(int x, int y, int x2, int y2)
	{
		for(int yy = y / 4; yy < y2 / 4; yy++)
		{
			for(int xx = x / 4; xx < x2 / 4; xx++)
			{
				avgArea(xx * 4, yy * 4);
			}
		}
	}
	
	private void extendShores()
	{
		//insert shoreline
		for(int y = 0; y < 256; y++)
		{
			for(int x = 0; x < 256; x++)
			{
				if(getTerrain(x, y) == 1)
				{
					byte[] surr = getSurr4(new Point(x, y));
					for(int k = 0; k < 4; k++)
					{
						if(surr[k] >= 10)
						{
							putTerrain(x, y, (byte) 3);
						}
					}
				}
			}
		}
		
		//extend shoreline
		for(int y = 0; y < 256; y++)
		{
			for(int x = 0; x < 256; x++)
			{
				if(getTerrain(x, y) == 3)
				{
					byte[] surr = getSurr4(new Point(x, y));
					for(int k = 0; k < 4; k++)
					{
						if(surr[k] >= 10)
						{
							int len = 5;
							if(surr[k] == 13)  //mountain
								len = 3;
							int dx = 0;
							int dy = 0;
							switch(k)
							{
							case 0:  //go right
								dx = 1; break;
							case 1:  //go left
								dx = -1; break;
							case 2:  //go down
								dy = 1; break;
							case 3:  //go up
								dy = -1; break;
							}
							for(int l = 1; l <= len; l++)
							{
								int xx = l * dx + x;
								int yy = l * dy + y;
								byte bb = getTerrain(xx, yy);
								if(bb == 1)
									putTerrain(xx, yy, (byte) 3);
							}
							len--;
							int side = 1;
							while(len > 0)  //now "peak" it
							{
								for(int l = 0; l <= len; l++)
								{
									int xx = l * dx + x;
									if(dx == 0)
										xx = x + side;
									int yy = l * dy + y;
									if(dy == 0)
										yy = y + side;
									byte bb = getTerrain(xx, yy);
									if(bb == 1)
										putTerrain(xx, yy, (byte) 3);
									if(dx == 0)
										xx = x - side;
									if(dy == 0)
										yy = y - side;
									bb = getTerrain(xx, yy);
									if(bb == 1)
										putTerrain(xx, yy, (byte) 3);
								}
								len--;
								side++;
							}
							
						}
					}
				}
			}
		}
		
	}
	
	private byte getFinalTile(int x, int y)
	{
		byte t = mapData[x + 256 * y];
		byte v = visibility[x + 256 * y];
		byte out = 0;
		byte adj = 0;
		ArrayList<Point> pts = getSurrPoints(new Point(x, y));
		//special location adjustment
		if(t == 30 || t == 31) t = 10;
		else if(t == 32) t = 1;
		else if(t == 29) t = 17;
		switch(t)  //calc adj
		{
		case 10:  case 11:  case 15:  //4 direction adj if outside is water
			for(int i = 0; i < pts.size(); i++)
				if(getTerrain(pts.get(i)) < 10)
					adj += 1 << i;
			break;
		case 3:  //4 direction adj if outside is 7 or land
			for(int i = 0; i < pts.size(); i++)
				if(getTerrain(pts.get(i)) >= 7)
					adj += 1 << i;
			break;
		case 12:
			if(getTerrain(pts.get(2)) != 12)
				adj += 4;
			break;
		case 13:  //mountains
			for(int i = 2; i < pts.size(); i++)
				if(getTerrain(pts.get(i)) != 13)
					adj += 1 << (i);
			break;
		}
		byte adjF = 0;
		switch(t)
		{
		case 3:  case 10:  case 11:  case 15:
			if((adj & 3) == 3)
				adj--;
			if((adj & 12) == 12)
				adj -= 8;
			if((adj & 1) == 1)
				adjF--;
			if((adj & 2) == 2)
				adjF++;
			if((adj & 4) == 4)
				adjF -= 8;
			if((adj & 8) == 8)
				adjF += 8;
			break;
		case 12:
			if((adj & 4) == 4)
				adjF -= 45;
			else if(v == 1)
				adjF -= 37;
			break;
		case 13:
			if((adj & 4) == 4)
				adjF = 12;
			if((adj & 8) == 8)
				adjF = 11;
			if((adj & 12) == 12)
				adjF = 13;
			break;
		}
		byte o = v;  //o is for final output
		if(o == 0 || o >= 5)
			o = 1;
		o--;
		o <<= 6;
		
		switch(t)
		{
		case 1:  return (byte) (o + 7);
		case 3:  return (byte) (o + 36 + adjF);
		case 7:  return (byte) (o + 15);
		case 10: return (byte) (o + 12 + adjF);
		case 11: 
			if(adjF + 9 == 0) return (byte) (o + 58);
			else return (byte) (o + 9 + adjF);
		case 12: return (byte) (o + 59 + adjF);
		case 13: return (byte) (o + 49 + adjF);
		case 14: return (byte) (o + 23);
		case 15: return (byte) (o + 33 + adjF);
		case 16: return (byte) (o + 51);
		case 17: return (byte) (o + 50);
		case 20: return (byte) (o + 52);
		case 21: return (byte) (o + 53);
		case 22: return (byte) (o + 54);
		case 26: return (byte) (o + 55);
		case 27: return (byte) (o + 56);
		case 28: return (byte) (o + 57);
		default: return 0;
		}
	}
	
	
	
	private int getTerrain(Point p) 
	{
		// TODO Auto-generated method stub
		return getTerrain(p.x, p.y);
	}

	public byte[] generateFinalMap()
	{
		byte[] rv = new byte[mapData.length];
		for(int i = 0; i < 256; i++)
		{
			for(int j = 0; j < 256; j++)
			{
				rv[j + 256 * i] = getFinalTile(j, i);
			}
		}
		return rv;
	}
	
	public ArrayList<POI> getPOIList()
	{
		byte[] locs = getLocationBlock();
		String[] odLocs = {"Shrine of Justice", "Shrine of Spirituality", "Dungeon Wrong", "Minoc", "Dungeon Covetous", "Yew",
				"Shrine of Sacrifice", "Empath Abbey W", "Empath Abbey E", "Vesper", "Shrine of Honesty", "Dungeon Despise", "Dungeon Deceit",
				"Serpent's Spire", "Cove", "Shrine of Compassion", "Dungeon Shame", "Britain", "Castle Britannia W", "Castle Britannia E",
				"Lyceaum W", "Lyceaum E", "Skara Brae", "Moonglow", "Paws", "Buccaneer's Den", "Dungeon Destard", "Magincia", "Trinsic",
				"Shrine of Honor", "Shrine of Humility", "Jhelom", "Shrine of Valor", "The Abyss", "Island Shrine", "Hythloth",
				"Serpent's Hold W", "Serpent's Hold E"};
		ArrayList<POI> rv = new ArrayList<POI>(odLocs.length);
		for(int i = 0; i < odLocs.length; i++)
		{
			Point p = new Point((int) (locs[i * 2] & 255), (int) (locs[i * 2 + 1] & 255));
			POI pt = new POI(p, odLocs[i]);
			rv.add(pt);
		}
		return rv;
	}
	
	public byte[] getLocationBlock()  //points are implicitly randomized so no need to re-randomize
	{
		Point bd = dungeons.get(balloonDungeon);
		dungeons.remove(balloonDungeon);
		Point[] ps = new Point[38];
		int[] vTownLoc = {3, 5, 17, 22, 23, 27, 28, 31};  //Minoc, Yew, Britain, Skara Brae, Moonglow, Magincia, Trinsic, Jhelom 
		for(int i = 0; i < vTownLoc.length; i++)
			ps[vTownLoc[i]] = virtueTowns.get(i);
		int[] shrineLoc = {0, 6, 10, 15, 29, 34, 32, 30};  //note: 30 (Shrine of Humility) must be last
		for(int i = 0; i < shrineLoc.length; i++)
			ps[shrineLoc[i]] = shrines.get(i);
		ps[1] = moongates.get(0);  //shrine of Spirituality, which is not on the map
		int[] dung = {2, 4, 11, 12, 16, 26, 13};
		for(int i = 0; i < dung.length; i++)
			ps[dung[i]] = dungeons.get(i);
		ps[35] = bd;  //Hythloth opens to the balloon
		ps[33] = abyss;
		int[] castles = {7, 18, 20, 36};  //Empath Abbey, Britannia, Lyceaum, Serpent's Hold
		for(int i = 0; i < castles.length; i++)
		{
			Point c = townsNCastles.get(i);
			Point nc = new Point(c);
			nc.x += 1;
			nc.y += 2;
			nc = normalizePoint(nc);
			ps[castles[i]] = nc;
			Point n2 = new Point(nc);
			n2.x++;
			n2 = normalizePoint(n2);
			ps[castles[i] + 1] = n2;
		}
		int[] towns = {25, 14, 24, 9};
		for(int i = 0; i < towns.length; i++)
			ps[towns[i]] = townsNCastles.get(i + 4);
		
		byte[] rv = new byte[ps.length * 2];
		for(int i = 0; i < ps.length; i++)
		{
			Point p = ps[i];
			rv[i * 2] = (byte) p.x;
			rv[i * 2 + 1] = (byte) p.y;
		}
		dungeons.add(balloonDungeon, bd);
		return rv;
	}
	
	public byte[][] getMoongateBlock(boolean rando)  //"rando" randomizes the destinations of the moongates
	{
		//was 4,2,7,1,0,6,3,5
		int[] mgOrder = {4, 2, 7, 1, 0, 6, 3, 5};  //order of MOONGATES to cities in old game 
		//(Moongate 0 goes to city 4 (Moonglow), Moongate 1 goes to city 2 (Britain), Moongate 2 goes to city 7 (Jhelom), Moongate 3 goes to city 1 (Yew),
		//Moongate 4 goes to city 0 (Minoc), Moongate 5 goes to city 6 (Trinsic), Moongate 6 goes to city 3 (Skara Brae), Moongate 7 goes to city 5 (Magincia)) 
		if(rando)
		{
			ArrayList<Integer> ords = new ArrayList<Integer>(8);
			for(int i = 0; i < 8; i++)
				ords.add(i);
			for(int i = 0; i < 8; i++)
				mgOrder[i] = (ords.remove((int) (UltimaRando.rand() * ords.size())));
			//note: the shrine of spirituality is moved to the moongate in index 4 of the moongates or you softlock
			
		}
		
		byte[] rv = new byte[17];
		for(int i = 0; i < mgOrder.length; i++)
			rv[i] = (byte) moongates.get(mgOrder[i]).x;
		rv[8] = -128; //required
		for(int i = 0; i < mgOrder.length; i++)
			rv[i + 9] = (byte) moongates.get(mgOrder[i]).y;
		
		byte[] rv2 = new byte[8];
		int findOrder[] = {4,2,7,1,0,6,3,5};
		for(int i = 0; i < mgOrder.length; i++)
		{
			int findMe = mgOrder[i];
			for(int j = 0; j < findOrder.length; j++)
			{
				if(findOrder[j] == findMe)
				{
					rv2[i] = (byte) j;
					break;
				}
			}
		}
		
		byte[][] frv = {rv, rv2};
		return frv;
	}
	
	public ArrayList<Point> getOutsideSearchSpots()
	{
		ArrayList<Point> pts = new ArrayList<Point>(6);
		pts.addAll(searchSpots);
		pts.add(3, moongates.get(4));  //the moongate associated with Moonglow
		return pts;
	}
	
	public int[] getShrineHumilData()
	{
		int[] rv = new int[3];
		Point p = shrines.get(7);
		rv[0] = p.x;
		rv[1] = p.y;
		rv[2] = humShrineOpening;
		return rv;
	}
	
	public ArrayList<Point> getNewStartingLocs()
	{
		ArrayList<Point> starts = new ArrayList<Point>(8);
		for(int i = 0; i < 8; i++)
		{
			int x1 = moongates.get(i).x;
			int y1 = moongates.get(i).y;
			int x2 = virtueTowns.get(i).x;
			int y2 = virtueTowns.get(i).y;
			int xf = (x1 + x2) / 2;
			if(Math.abs(x1 - xf) > 10)
			{
				int xa = Math.min(x1, x2) + 256;
				xf = (Math.max(x1, x2) + xa) / 2;
				xf &= 255;
			}
			int yf = (y1 + y2) / 2;
			if(Math.abs(y1 - yf) > 10)
			{
				int ya = Math.min(y1, y2) + 256;
				yf = (Math.max(y1, y2) + ya) / 2;
				yf &= 255;
			}
			Point p = new Point(xf, yf);
			starts.add(p);
		}
		//starting locations must be associated with their proper character
		ArrayList<Point> fstarts = new ArrayList<Point>(starts.size());  
		int assoc[] = {4, 2, 7, 1, 0, 6, 3, 5};
		for(int i = 0; i < assoc.length; i++)
		{
			fstarts.add(starts.get(assoc[i]));
		}
		return fstarts;
	}
	
	public Point getBritBalloon()
	{
		return britBalloon;
	}
	
	public byte[] processHumilityShrine()  //not used
	{
		byte cb1 = 67;  //67 = $43 (y); one y val is tested
		byte cb2 = 66;  //66 = $42 (x); three x values are tested
		if(humShrineOpening < 2)
		{
			cb1 = 66; //$43 is y
			cb2 = 67;
		}
		Point sh = shrines.get(7);
		//cb1 is tested against cb1Val
		byte[] cb1Val = {(byte) (sh.x - 1), (byte) (sh.x + 1), (byte) (sh.y - 1), (byte) (sh.y + 1)};
		//cb2 is tested against cb2v1 and cb2v2
		byte cb2v1 = (byte) (sh.x - 1);
		byte cb2v2 = (byte) (sh.x + 2);
		if(humShrineOpening < 2)
		{
			cb2v1 = (byte) (sh.y - 1);
			cb2v2 = (byte) (sh.y + 2);
		}
		byte incdec = -58;  //c6 is a decrement instruction for when you are pushed back (west:x--, north:y--)
		if((humShrineOpening & 1) == 1)  //on east or south increment (e6) instead (east:x++, south:y++)
			incdec = -26;
		byte[] rv = {cb1, cb2, cb2v1, cb2v2, incdec};
		return rv;
	}
	
	public byte[] makeRandomMap() throws Exception
	{
		zones = makeZones();
		
		/*for(int y = 0; y < 4; y++)
		{
			for(int x = 0; x < 4; x++)
				smoothMap(x, y, 253 + x, 253 + y);
		}*/
		/*smoothMap(1, 1, 253, 253);
		smoothMap(2, 2, 254, 254);
		smoothMap(3, 3, 255, 255);*/
		int mzs = 0;
		int fzs = 0;
		for(int i = 0; i < zones.size(); i++)
		{
			if(zones.get(i).terrain == 13)
				mzs++;
			if(zones.get(i).terrain == 12)
				fzs++;
		}
		//System.out.println("zones placed");
		features = makeFeatures(fzs, mzs);
		
		for(int i = 0; i < features.size(); i++)
		{
			MapZone f = features.get(i);
			if(f.terrain != 2 && f.terrain < 40)
			{
				smoothMap(f.area.x, f.area.y, f.area.x + f.area.width - 1, f.area.y + f.area.height - 1);
			}
		}
		extendShores();
		for(int i = 0; i < features.size(); i++)
		{
			MapZone f = features.get(i);
			if(f.terrain > 40)
			{
				f.placeFeature(f.terrain);
			}
		}
		poi = new ArrayList<Rectangle>();
		makeSearchSpots();
		abyss = placeAbyss();
		virtueTowns = placeVirtueTowns();
		moongates = placeMoongates(virtueTowns);
		dungeons = placeDungeons();
		shrines = placeShrines();
		townsNCastles = new ArrayList<Point>();
		townsNCastles.addAll(placeCastles());
		townsNCastles.addAll(placeOtherTowns());
		abyss = placeAbyss();
		//System.out.println("All locations placed.");
		ensureAccess();
		whirlpool = placeWhirlpool();
		putTerrain(whirlpool.x, whirlpool.y, (byte) 32);
		balloon = placeBalloon();
		putTerrain(balloon.x, balloon.y, (byte) 31);
		if(finalSanityCheck() == false)
		{
			//System.out.println("Failed final sanity check");
			/*MapWindow mw = new MapWindow(mapData, false);
			mw.setAccessData(accessData);
			mw.setPOIData(poi);*/
			return null;
		}
		else
		{
			setupVisibility();
			return mapData;
		}
	}
	
	public static void testMapGen(int nTests) throws Exception
	{
		int pass = 0;
		int fail = 0;
		long ts = System.nanoTime();
		for(int i = 0; i < nTests; i++)
		{
			Map m = new Map(256,256);
			byte[] arr = m.makeRandomMap();
			if(arr == null)
			{
				System.out.println("FAIL");
				fail++;
			}
			else
			{
				System.out.println("PASS");
				pass++;
			}
		}
		long tt = System.nanoTime() - ts;
		System.out.println(nTests + " completed; " + pass + " maps generated and " + fail + " failures in " + tt + " ns.");
	}
	
	private void makeSearchSpots() 
	{
		System.out.print("F");
		searchSpots = new ArrayList<Point>();
		for(int i = 0; i < features.size(); i++)
		{
			MapZone f = features.get(i);
			if(f.terrain > 40)
			{
				poi.add(f.area);
				searchSpots.add(f.innerPOI);
			}
			if(searchSpots.size() == 3)
			{
				//swap the bell and the skull
				Point p = searchSpots.remove(2);
				searchSpots.add(1, p);
				break;
			}
		}
		//manroot search spot
		ArrayList<MapZone> plains = selectZonesOfType(10);
		while(true)
		{
			MapZone manroot = plains.get((int) (UltimaRando.rand() * plains.size()));
			int dx = (int) (UltimaRando.rand() * 30 + 20);
			int dy = (int) (UltimaRando.rand() * 30 + 20);
			if(UltimaRando.rand() < 0.5)
				dx *= -1;
			if(UltimaRando.rand() < 0.5)
				dy *= -1;
			Point mp = new Point(manroot.cx + dx, manroot.cy + dy);
			if(getTerrain(mp.x, mp.y) == 10)
			{
				mp.x += 256;
				mp.x %= 256;
				mp.y += 256;
				mp.y %= 256;
				putTerrain(mp.x, mp.y, (byte) 14);
				//searchSpots.add(mp);
				ensurePOIUnique(new Rectangle(mp.x, mp.y, 1, 1), true);
				this.manroot = mp;
				break;
			}
		}
		
		//fungus search spot
		System.out.print("M");
		ArrayList<MapZone> fors = selectZonesOfType(12);
		int fail = 0;
		while(true)
		{
			MapZone ff = fors.get((int) (UltimaRando.rand() * fors.size()));
			int dx = (int) (UltimaRando.rand() * 20 + 5);
			int dy = (int) (UltimaRando.rand() * 20 + 5);
			if(UltimaRando.rand() < 0.5)
				dx *= -1;
			if(UltimaRando.rand() < 0.5)
				dy *= -1;
			Point fp = new Point(ff.cx + dx, ff.cy + dy);
			if(fail > 300)
				putTerrain(fp.x, fp.y, (byte) 12);
			if(getTerrain(fp.x, fp.y) == 12)
			{
				fp.x += 256;
				fp.x %= 256;
				fp.y += 256;
				fp.y %= 256;
				//putTerrain(fp.x, fp.y, (byte) 14);
				//searchSpots.add(fp);
				ensurePOIUnique(new Rectangle(fp.x, fp.y, 1, 1), true);
				fungus = fp;
				break;
			}
			else
			{
				fail++;
				/*if(fail > 300)
				{
					String s = "\nLooking at " + fp.x + "," + fp.y + "; terr=" + getTerrain(fp.x, fp.y) + "\n";
					s += "Forest zones:" + fors.size() + "\n";
					for(int i = 0; i < fors.size(); i++)
					{
						MapZone fz = fors.get(i);
						if(fz.filled.contains(fp))
							s += "#" + i + " contains point" + fp.x + "," + fp.y + " rect=" + fz.area.toString() + "\n";
						else
						{
							s += "#" + i + " filled size=" + fz.filled.size() + " rect=" + fz.area.toString() + "\n";
							for(int y = 0; y <= fz.area.height; y++)
							{
								for(int x = 0; x <= fz.area.width; x++)
								{
									s += getTerrain(fz.area.x + x, fz.area.y + y) + "|";
								}
								s += "\n";
							}
						}
					}
					System.out.println(s);
				}*/
			}
		}
		searchSpots.add(fungus);
		searchSpots.add(manroot);
	}

	public byte getTerrain(int x, int y)
	{
		if(x < 0)  x += 256;
		if(x > 255)  x -= 255;
		if(y < 0)  y += 256;
		if(y > 255) y -= 256;
		return mapData[y * 256 + x];
	}
	
	public byte getZone(int x, int y)
	{
		if(x < 0)  x += 256;
		if(x > 255)  x -= 255;
		if(y < 0)  y += 256;
		if(y > 255) y -= 256;
		return zoneData[y * 256 + x];
	}
	
	public void putTerrain(int x, int y, byte v)
	{
		//System.out.println("Setting " + x + "," + y + " to " + v);
		if(x < 0)  x += 256;
		if(x > 255)  x -= 255;
		if(y < 0)  y += 256;
		if(y > 255) y -= 256;
		mapData[y * 256 + x] = v;
	}
	
	public void putZone(int x, int y, byte v)
	{
		if(x < 0)  x += 256;
		if(x > 255)  x -= 255;
		if(y < 0)  y += 256;
		if(y > 255) y -= 256;
		mapData[y * 256 + x] = v;
	}
	
	private byte getAvg(byte b1, byte b2)
	{
		if(b1 == b2)
			return b2;
		byte val = 1;
		switch(b1)
		{
		case 1://////// 0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H};  //1:deep wtr, 3: watr, 7:shoal, 10:plains, 11:shrub, 12:forest, 13:mtn, 14:swamp, 15:brick, 16:lava, 17:crater
			int[] aa = {1,1,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3};  val = (byte) aa[b2];  break;
		case 3:
			int[] bb = {1,1,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3};  val = (byte) bb[b2];  break;
		case 7:
			int[] cc = {3,3,3,3,3,3,3,7,7,7,5,5,5,5,5,5,5,5};  val = (byte) cc[b2];  break;
		case 10:
			int[] pp = {3,3,3,3,3,3,3,3,3,3,10,21,11,15,35,25,15,15};  val = (byte) pp[b2];  break;
		case 11:
		    int[] ss = {3,3,3,3,3,3,3,3,3,3,21,11,11,15,35,26,15,15};  val = (byte) ss[b2];  break;
		case 12:
			int[] ff = {3,3,3,3,3,3,3,3,3,3,11,23,12,26,23,12,15,15}; val = (byte) ff[b2];  break;
		case 13:
			int[] mm = {3,3,3,3,3,3,3,3,3,3,15,26,27,13,25,28,15,15}; val = (byte) mm[b2];  break;
		case 14:
			int[] po = {3,3,3,3,3,3,3,3,3,3,24,25,26,29,35,40,15,15}; val = (byte) po[b2];  break;
		case 15:
			int[] br = {3,3,3,3,3,3,3,3,3,3,25,26,27,28,40,15,15,15}; val = (byte) br[b2];  break;
		}
		double rr = UltimaRando.rand();
		if(val == 5)
		{
			if(rr > 0.75)  
				return 7;
			else  
				return 3;
		}
		if(val < 20) 
			return val;
		else
		{
			int a1 = val - b1;
			int a2 = val - a1;
			if(a1 > 20)
			{
				if(val == 35)  //combines 10,11,14
				{
					if(rr < 0.4)  return b1;
					val -= b1;  //val will now be 25, 24, or 21
					if(rr < 0.7)  return (byte) Math.max(val - 14, 10);  //11, 10, or 10
					else return (byte) Math.min(val - 10, 14);  //14, 14, or 11
				}
				else if(val == 40)  //combines 11,14,15
				{
					if(rr < 0.4)  return b1;
					val -= b1;  //val will now be 29, 26, or 25
					if(rr < 0.7)  return (byte) Math.max(val - 15, 11);  //14, 11, or 11
					else return (byte) Math.min(val - 11, 15);  //15, 15, or 14
				}
				else return 0;
			}
			else
			{
				//if(a1 > 20 || a2 > 20)
					//System.out.print("wf");
				if(UltimaRando.rand() > 0.5)  
					return (byte) a1;
				else 
					return (byte) a2;
			}
		}
	}
	
	public ArrayList<Point> getSurrPoints(Point p)
	{
		ArrayList<Point> rv = new ArrayList<Point>();
		int xx = p.x;
		int yy = p.y;
		xx--;
		if(xx < 0)
			xx = 255;
		rv.add(new Point(xx, yy));
		xx += 2;
		if(xx >= 256)
			xx -= 256;
		rv.add(new Point(xx, yy));
		xx = p.x;
		yy--;
		if(yy < 0)
			yy = 255;
		rv.add(new Point(xx, yy));
		yy += 2;
		if(yy >= 256)
			yy -= 256;
		rv.add(new Point(xx, yy));
		return rv;
	}
	
	public byte[] getSurr4(Point p)
	{
		byte[] rv = new byte[4];
		int xx = p.x;
		int yy = p.y;
		xx--;
		if(xx < 0)
			xx = 255;
		rv[0] = mapData[yy * 256 + xx];
		xx += 2;
		if(xx >= 256)
			xx -= 256;
		rv[1] = mapData[yy * 256 + xx];
		xx = p.x;
		yy--;
		if(yy < 0)
			yy = 255;
		rv[2] = mapData[yy * 256 + xx];
		yy += 2;
		if(yy >= 256)
			yy -= 256;
		rv[3] = mapData[yy * 256 + xx];
		return rv;
	}
	
	public byte[] getSurr8(Point p)
	{
		byte[] rv = new byte[8];
		int xx = p.x;
		int yy = p.y;
		int xf, yf;
		int c = 0;
		for(int i = -1; i <= 1; i++)
		{
			yf = yy + i;
			if(yf < 0)
				yf = 255;
			if(yf > 255)
				yf -= 255;
			for(int j = -1; j <= 1; j++)
			{
				xf = xx + j;
				if(xf < 0)
					xf = 255;
				if(xf > 255)
					xf -= 255;
				rv[c] = mapData[yf * 256 + xf];
				c++;
			}
		}
		return rv;
	}
	
	private void avgArea(int x, int y)
	{
		//System.out.println("Averaging " + x + "," + y + " to " + (x + 3) + "," + (y + 3));
		byte c1 = getTerrain(x, y);
		byte c2 = getTerrain(x + 3, y);
		byte c3 = getTerrain(x, y + 3);
		byte c4 = getTerrain(x + 3, y + 3);
		byte[] c = {c1,c2,c3,c4};
		for(int i = 0; i <= 1; i++)
		{
			putTerrain(x + i + 1, y, getAvg(c[i], c[1 - i]));  //0,1  (the indeces must flop)
			putTerrain(x + i + 1, y + 3, getAvg(c[i + 2], c[3 - i]));  //2,3
			putTerrain(x, y + i + 1, getAvg(c[2 * i], c[2 - (i * 2)]));  //0,2
			putTerrain(x + 3, y  + i + 1, getAvg(c[2 * i + 1], c[3 - (i * 2)])); //1,3
		}
		putTerrain(x + 1, y + 1, getAvg(getTerrain(x, y + 1), getTerrain(x + 1, y)));
		putTerrain(x + 1, y + 2, getAvg(getTerrain(x, y + 2), getTerrain(x + 1, y + 3)));
		putTerrain(x + 2, y + 1, getAvg(getTerrain(x + 3, y + 1), getTerrain(x + 2, y)));
		putTerrain(x + 2, y + 2, getAvg(getTerrain(x + 3, y + 2), getTerrain(x + 2, y + 3)));
	}
	
	private static int olapWith;
	private int seaPad;
	private Rectangle noOlapRect(ArrayList<MapZone> rv, int minLen, int maxLen, int maxOverlapCount, boolean onCenterLine)
	{
		int failures = 0;
		olapWith = -1;
		while(failures < 300)
		{
			int x = (int) ((UltimaRando.rand() * (200 - seaPad)) + seaPad);
			int y = (int) ((UltimaRando.rand() * (200 - seaPad)) + seaPad);
			if(onCenterLine)
				y = x;
			int lspr = maxLen - minLen + 1;
			int dx = (int) (UltimaRando.rand() * lspr + minLen);
			int dy = (int) (UltimaRando.rand() * lspr + minLen);
			
			if(x + dx > 255)
				continue;
			if(y + dy > 255)
				continue;
			
			Rectangle zn = new Rectangle(x, y, dx, dy);
			int olap = 0;
			for(int k = 0; k < rv.size(); k++)
			{
				Rectangle rr = rv.get(k).area;
				olapWith = -1;
				if(rr.contains(new Point(x, y)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(rr.contains(new Point(x + dx, y)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(rr.contains(new Point(x, y + dy)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(rr.contains(new Point(x + dx, y + dy)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(zn.contains(new Point(rr.x, rr.y)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(zn.contains(new Point(rr.x + rr.width - 1, rr.y)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(zn.contains(new Point(rr.x, rr.y + rr.height - 1)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
				if(zn.contains(new Point(rr.x + rr.width - 1, rr.height + rr.y - 1)))
				{
					olap++;
					olapWith = rv.get(k).terrain;
					if(olap > maxOverlapCount)
						break;
					else
						continue;
				}
			}
			if(olap <= maxOverlapCount)
				return zn;
			else
				failures++;
		}
		return null;
	}
	
	private ArrayList<Rectangle> noOlapRects(int minAmt, int maxAmt, int minLen, int maxLen, int maxOverlapCount)
	{
		int spr = maxAmt - minAmt;
		int nRects = (int) (UltimaRando.rand() * spr + minAmt);
		int i = 0;
		ArrayList<MapZone> rv = new ArrayList<MapZone>(nRects);
		ArrayList<Rectangle> rv2 = new ArrayList<Rectangle>(nRects);
		while(i < nRects)
		{
			Rectangle zn = noOlapRect(rv, minLen, maxLen, maxOverlapCount, false);
			if(zn == null)
				break;
			rv.add(new MapZone(zn, (byte) -1, -1));
			rv2.add(zn);
			i++;
		}
		return rv2;
	}
	
	private ArrayList<Point> getWalkingPoints(Point origin)
	{
		boolean[] selected = new boolean[mapData.length];
		ArrayList<Point> toLand = new ArrayList<Point>();
		toLand.add(origin);
		selected[origin.x + 256 * origin.y] = true;
		int wp = 0;
		while(wp < toLand.size())
		{
			Point wwp = toLand.get(wp);
			ArrayList<Point> sp = getSurrPoints(wwp);
			for(int i = 0; i < 4; i++)
			{
				Point wn = sp.get(i);
				byte b = mapData[wn.x + 256 * wn.y];
				boolean c = selected[wn.x + 256 * wn.y];
				if(b >= 10 && b != 13 && c == false)
				{
					toLand.add(wn);
					selected[wn.x + 256 * wn.y] = true;
				}
			}
			wp++;
		}
		return toLand;
	}
	
	private ArrayList<MapZone> makeZonesHM()
	{
		ArrayList<Rectangle> currZones = noOlapRects(10, 15, 30, 50, 0);
		//int[] zoneCounts = {3,2,1,1};
		double[] heightMap = new double[mapData.length];  //1 MB in height data
		double[] types = {0.50, 0.75, 0.85, 1.00};
		int[] zTypes = {1, 10, 13, 12};
		ArrayList<MapZone> finalZones = new ArrayList<MapZone>(currZones.size());
		//required zones: 3 water, 2 plains, 1 mountain, 1 forest
		finalZones.add(new MapZone(currZones.get(0), (byte) 1, 0));
		finalZones.add(new MapZone(currZones.get(1), (byte) 1, 1));
		finalZones.add(new MapZone(currZones.get(2), (byte) 1, 2));
		finalZones.add(new MapZone(currZones.get(3), (byte) 10, 3));
		finalZones.add(new MapZone(currZones.get(4), (byte) 10, 4));
		finalZones.add(new MapZone(currZones.get(5), (byte) 12, 5));
		finalZones.add(new MapZone(currZones.get(6), (byte) 13, 6));
		//the rest are random
		for(int j = 7; j < currZones.size(); j++)  //a zone can be water (50%), plains (25%), mountain (10%), or forest (15%)
		{
			Rectangle r = currZones.get(j);
			double a = UltimaRando.rand();
			byte type = 0;
			for(int k = 0; k < types.length; k++)
			{
				if(a < types[k])
				{
					type = (byte) zTypes[k];
					finalZones.add(new MapZone(r, type, j));
					//zoneCounts[k]++;
					break;
				}
			}	
		}
		boolean filling = false;
		//int count = 0;
		do
		{
			filling = false;
			for(int j = 0; j < finalZones.size(); j++)
				filling |= finalZones.get(j).heightFill(heightMap);
			/*count++;
			System.out.print(count + ":");
			for(int j = 0; j < finalZones.size(); j++)
			{
				int left = finalZones.get(j).filled.size() - finalZones.get(j).fillPoint;
				System.out.print(finalZones.get(j).fillPoint + "(" + left + ") ");
			}
			System.out.println();*/
		}while(filling);
		byte[] hts = {-99, -16, -3, 0, 2, 3, 6, 9, 99};
		byte[] hVals = {1, 1, 3, 10, 11, 12, 15, 13};
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for(int i = 0; i < heightMap.length; i++)
		{
			if(min > heightMap[i])
				min = heightMap[i];
			if(max < heightMap[i])
				max = heightMap[i];
		}
		System.out.println("Height map min=" + min + "   max=" + max);
		for(int i = 0; i < heightMap.length; i++)
		{
			double h = heightMap[i];
			for(int j = 0; j < hts.length; j++)
			{
				if(h >= hts[j])
					continue;
				mapData[i] = hVals[j - 1];
				break;
			}
		}
		for(int j = 0; j < finalZones.size(); j++)
			finalZones.get(j).filled.clear();
		//System.out.println("There are " + finalZones.size() + " zones on the map:");
		return finalZones;
	}
	
	private ArrayList<MapZone> makeZones()
	{
		ArrayList<Rectangle> currZones = noOlapRects(9, 15, 30, 50, 0);
		//int[] zoneCounts = {3,2,1,1};
		double[] types = {0.50, 0.75, 0.85, 1.00};
		int[] zTypes = {1, 10, 13, 12};
		ArrayList<MapZone> finalZones = new ArrayList<MapZone>(currZones.size());
		//required zones: 3 water, 2 plains, 1 mountain, 1 forest
		finalZones.add(new MapZone(currZones.get(0), (byte) 1, 1));
		finalZones.add(new MapZone(currZones.get(1), (byte) 1, 2));
		finalZones.add(new MapZone(currZones.get(2), (byte) 1, 3));
		finalZones.add(new MapZone(currZones.get(3), (byte) 10, 4));
		finalZones.add(new MapZone(currZones.get(4), (byte) 10, 5));
		finalZones.add(new MapZone(currZones.get(5), (byte) 12, 6));
		finalZones.add(new MapZone(currZones.get(6), (byte) 13, 7));
		//the rest are random
		for(int j = 7; j < currZones.size(); j++)  //a zone can be water (50%), plains (25%), mountain (10%), or forest (15%)
		{
			Rectangle r = currZones.get(j);
			double a = UltimaRando.rand();
			byte type = 0;
			for(int k = 0; k < types.length; k++)
			{
				if(a < types[k])
				{
					type = (byte) zTypes[k];
					finalZones.add(new MapZone(r, type, j + 1));
					//zoneCounts[k]++;
					break;
				}
			}	
		}
		boolean filling = false;
		//int count = 0;
		do
		{
			filling = false;
			for(int j = 0; j < finalZones.size(); j++)
				filling |= finalZones.get(j).centerFill();
			/*count++;
			System.out.print(count + ":");
			for(int j = 0; j < finalZones.size(); j++)
			{
				int left = finalZones.get(j).filled.size() - finalZones.get(j).fillPoint;
				System.out.print(finalZones.get(j).fillPoint + "(" + left + ") ");
			}
			System.out.println();*/
		}while(filling);
		for(int j = 0; j < finalZones.size(); j++)
		{
			finalZones.get(j).filled.clear();
			System.out.print(finalZones.get(j).toString() + ",");
		}
		System.out.println();
		//System.out.println("There are " + finalZones.size() + " zones on the map:");
		smoothMap(0, 0, 256, 256);
		smoothMap(2,2,255,255);
		
		System.out.print("Z#" + finalZones.size());
		return finalZones;
	}
	
	private ArrayList<MapZone> grabFreeFeaturesOfType(int j, boolean getUsed)
	{
		ArrayList<MapZone> ffs = new ArrayList<MapZone>();
		for(int i = 0; i < features.size(); i++)
			if(features.get(i).terrain == j && (features.get(i).isUsed == false || getUsed == true))
				ffs.add(features.get(i));
		return ffs;
	}
	
	private ArrayList<MapZone> selectZonesOfType(int t)
	{
		ArrayList<MapZone> ffs = new ArrayList<MapZone>();
		for(int i = 0; i < zones.size(); i++)
			if(zones.get(i).terrain == t)
				ffs.add(zones.get(i));
		return ffs;
	}
	
	public byte[] rleCompress(boolean collapseLand, boolean collapseWater)
	{
		int count = 0;
		byte val = 0;
		ArrayList<Byte> out1 = new ArrayList<Byte>();
		for(int yy = 0; yy < 256; yy++)
		{
			for(int xx = 0; xx < 256; xx++)
			{
				byte md = mapData[256 * yy + xx];
				if(collapseLand)
				{
					byte[] diff = {-9, -8, -7, -1, +1, +7, +8, +9};
					for(int i = 0; i < diff.length; i++)
					{
						if(md + diff[i] == 12)
						{
							md = 12;
							break;
						}
						if(md + diff[i] == 9)
						{
							md = 9;
							break;
						}
						if(md + diff[i] == 33)
						{
							md = 33;
							break;
						}
					}
					if(md == 58)
						md = 9;
				}
				if(collapseWater)
				{
					byte[] diff = {-9, -8, -7, -1, +1, +7, +8, +9};
					for(int i = 0; i < diff.length; i++)
					{
						if(md + diff[i] == 36)
						{
							md = 36;
							break;
						}
					}
				}
				if(md != val)
				{
					if(count > 0)
						out1.add((byte) count);
					out1.add(md);
					count = 1;
					val = md;
				}
				else
					count++;
			}
			out1.add((byte) count);
			count = 0;
			val = 0;
		}
		byte[] rv = new byte[out1.size()];
		for(int i = 0; i < out1.size(); i++)
			rv[i] = out1.get(i);
		return rv;
	}
	
	private ArrayList<Rectangle> poi;
	private ArrayList<Point> searchSpots;
	Point whirlpool;
	Point balloon;
	Point britBalloon;
	private int balloonDungeon;
	
	private boolean sanity(int type, String desc, ArrayList<Point> list)
	{
		boolean rv = true;
		for(int i = 0; i < list.size(); i++)
		{
			Point p = list.get(i);
			if(accessData[p.x + p.y * 256] == 0)
			{
				ArrayList<Point> ps = getSurrPoints(p);
				for(int j = 0; j < ps.size(); j++)
				{
					Point pa = ps.get(j);
					byte bb = accessData[pa.x + pa.y * 256];
					if(bb > 0 && bb != 8)
					{
						accessData[p.x + p.y * 256] = bb;
						break;
					}
				}
				if(accessData[p.x + p.y * 256] == 0)
				{
					System.out.print("f" + desc + i + ";");
					rv = false;
				}
			}
			byte t = mapData[p.x + p.y * 256];
			if(t != type)
			{
				System.out.print("+" + desc +  i + ";");
				mapData[p.x + p.y * 256] = (byte) type;
			}
		}
		if(rv == false)
		{
			System.out.println("\n" + desc + " Location list:");
			for(int i = 0; i < list.size(); i++)
			{
				System.out.println(list.get(i));
			}
		}
		return rv;
	}
	
	public boolean finalSanityCheck()
	{
		System.out.print("san");
		boolean fv = true;
		fv &= sanity(20, "vt", virtueTowns);
		fv &= sanity(21, "dg", dungeons);
		fv &= sanity(22, "sh", shrines);
		fv &= sanity(30, "mg", moongates);
		fv &= sanity(10, "ca", new ArrayList<Point>(townsNCastles.subList(0, 4)));
		fv &= sanity(20, "nt", new ArrayList<Point>(townsNCastles.subList(4, 8)));
		searchSpots.add(balloon);
		searchSpots.add(whirlpool);
		searchSpots.add(abyss);
		int[] searchS = {11, 3, 1, 12, 14, 31, 32, 29};
		String[] searchN = {"Ho", "Sk", "Dv", "Fu", "Ma", "Ba", "Wp", "Ab"};
		for(int i = 0; i < searchS.length; i++)
			fv &= sanity(searchS[i], searchN[i], new ArrayList<Point>(searchSpots.subList(i, i + 1)));
		System.out.print(";");
		//MapPanel mp = null;
		return fv;
	}
	
	public static void testPOIUnique()  //no failures
	{
		Map m = new Map(0,0);
		m.poi = new ArrayList<Rectangle>();
		Rectangle r1 = new Rectangle(2,2,3,4);
		m.poi.add(r1);
		Rectangle r2 = new Rectangle(1,1,1,1);
		boolean[][] t1 = new boolean[7][];
		for(int i = 0; i < 7; i++)
		{
			t1[i] = new boolean[7];
			for(int j = 0; j < 7; j++)
			{
				if(i >= 2 && i <= 5 && j >= 2 && j <= 4)
					t1[i][j] = false;
				else
					t1[i][j] = true;
			}
		}
		for(int y = 0; y < 7; y++)
		{
			r2.y = y;
			for(int x = 0; x < 7; x++)
			{
				r2.x = x;
				boolean b = m.ensurePOIUnique(r2, false);
				if(b != t1[y][x])
					System.out.println("Error in testPOIUnique 1x1 test @" + x + "," + y);
			}
		}
		Rectangle r3 = new Rectangle(0,0,2,2);
		for(int i = 0; i < 7; i++)
		{
			t1[i] = new boolean[7];
			for(int j = 0; j < 7; j++)
			{
				if(i == 0 || j == 0 || i == 6 || j >= 5)
					t1[i][j] = true;
				else
					t1[i][j] = false;
			}
		}
		for(int y = 0; y < 7; y++)
		{
			r3.y = y;
			for(int x = 0; x < 7; x++)
			{
				r3.x = x;
				boolean b = m.ensurePOIUnique(r3, false);
				if(b != t1[y][x])
					System.out.println("Error in testPOIUnique 2x2 test @" + x + "," + y);
			}
		}
	}
	
	private boolean rectContains(Rectangle r, Point p)
	{
		if(p.x >= r.x && p.x < r.x + r.width && p.y >= r.y && p.y < r.y + r.height)
			return true;
		else
			return false;
	}
	
	private void openLandForBalloon(Point origin, byte[] access)
	{
		//boolean[] selected = new boolean[access.length];
		ArrayList<Point> openLand = getWalkingPoints(origin);
		
		//remove the origin point (as it is the entrance to a dungeon)
		//selected[origin.x + 256 * origin.y] = false;
		openLand.remove(0);
		//System.out.println("Opening for ballon; walking points=" + openLand.size());
		//select a random point and flatten it for the balloon
		while(openLand.size() > 1)
		{
			double r = UltimaRando.rand() * openLand.size();
			Point p = openLand.remove((int) r);
			if(ensurePOIUnique(new Rectangle(p.x, p.y, 2, 2), false) == false)
				continue;
			for(int x = p.x; x < p.x + 2; x++)
			{
				for(int y = p.y; y < p.y + 2; y++)
				{
					putTerrain(x, y, (byte) 10);
				}
			}
			ArrayList<Point> walkTo = getWalkingPoints(p);
			for(int i = 0; i < walkTo.size(); i++)
			{
				Point p2 = walkTo.get(i);
				access[p2.x + 256 * p2.y] |= 4;
			}
			return;
		}
		//now for the one case
		ArrayList<Point> p4 = new ArrayList<Point>();
		if(openLand.size() == 1)
		{
			Point p = openLand.remove(0);
			p4 = getSurrPoints(p);
			for(int i = 0; i < 4; i++)
			{
				Point p2 = p4.get(i);
				if(p2.x < p.x)
					p2.x--;
				if(p2.y < p.y)
					p2.y--;
				if(ensurePOIUnique(new Rectangle(p2.x, p2.y, 2, 2), false) == false)
					continue;
				for(int x = p2.x; x < p2.x + 2; x++)
				{
					for(int y = p2.y; y < p2.y + 2; y++)
					{
						putTerrain(x, y, (byte) 10);
					}
				}
				ArrayList<Point> walkTo = getWalkingPoints(p2);
				for(int j = 0; j < walkTo.size(); j++)
				{
					Point p3 = walkTo.get(j);
					access[p3.x + 256 * p3.y] |= 4;
				}
				return;
			}
			putTerrain(p.x, p.y, (byte) 13);
		}
		//at this point the dungeon opening spot cannot be used and must be moved
		
		p4.clear();
		p4 = getSurrPoints(origin);
		for(int i = 0; i < 4; i++)
		{
			Point p2 = p4.get(i);
			Point pn = new Point(p2.x, p2.y);
			if(p2.x < origin.x)
				p2.x--;
			else if(p2.x > origin.x)
				p2.x++;
			if(p2.y < origin.y)
				p2.y--;
			else if(p2.y > origin.y)
				p2.y++;
			if(ensurePOIUnique(new Rectangle(p2.x, p2.y, 2, 2), false) == false)
				continue;
			for(int x = p2.x; x < p2.x + 2; x++)
			{
				for(int y = p2.y; y < p2.y + 2; y++)
				{
					putTerrain(x, y, (byte) 10);
				}
			}
			putTerrain(pn.x, pn.y, (byte) 10);
			ArrayList<Point> walkTo = getWalkingPoints(p2);
			for(int j = 0; j < walkTo.size(); j++)
			{
				Point p3 = walkTo.get(j);
				access[p3.x + 256 * p3.y] |= 4;
			}
			return;
		}
		//at this point you are truly screwed
		System.out.print("XX");
		openLandForBalloon(origin, access);
	}
	
	private ArrayList<Point> doWaterAccess(Point origin, byte[] access)
	{
		boolean[] selected = new boolean[access.length];
		ArrayList<Point> fromWater = new ArrayList<Point>();
		fromWater.add(origin);
		selected[origin.x + 256 * origin.y] = true;
		int wp = 0;
		while(wp < fromWater.size())
		{
			Point wwp = fromWater.get(wp);
			ArrayList<Point> sp = getSurrPoints(wwp);
			for(int i = 0; i < 4; i++)
			{
				Point wn = sp.get(i);
				byte b = mapData[wn.x + 256 * wn.y];
				boolean c = selected[wn.x + 256 * wn.y];
				if((b == 1 || b == 3) && c == false)
				{
					fromWater.add(wn);
					selected[wn.x + 256 * wn.y] = true;
				}
			}
			wp++;
		}
		return fromWater;
	}
	
	private boolean doAccess(ArrayList<Point> pts, int qp, Point origin, byte[] access, int dwi, boolean[] dwa)  //deep water access?
	{
		if(pts.size() > 0 && qp >= pts.size())
			return false;
		if(qp == -1)
		{
			pts.add(origin);
			access[origin.y * 256 + origin.x] = 2;
			return true;
		}
		
		Point p = pts.get(qp);
		//boolean fromLand = true;
		//boolean more = false;
		
		ArrayList<Point> sp = getSurrPoints(p);
			
		for(int i = 0; i < sp.size(); i++)
		{
			Point p2 = sp.get(i);
			if(access[p2.y * 256 + p2.x] != 0)
				continue;
			byte b = mapData[p2.y * 256 + p2.x];
			if(b == 7 || b == 13)
			{
				access[p2.y * 256 + p2.x] = 8;
				continue;
			}
			if(b < 10)
			{
				ArrayList<Point> wps = doWaterAccess(p, access);
				boolean good = false;
				for(int j = 0; j < wps.size(); j++)
				{
					Point pp = wps.get(j);
					if(mapData[pp.x + 256 * pp.y] == 1)
					{
						good = true;
						dwa[dwi] = true;
						for(int k = 0; k < pts.size(); k++)
						{
							Point pl = pts.get(k);
							access[pl.x + 256 * pl.y] |= 1;
						}
						break;
					}
				}
				for(int j = 0; j < wps.size(); j++)
				{
					Point pp = wps.get(j);
					if(good)
					{
						access[pp.x + 256 * pp.y] = 1;
						pts.add(pp);
					}
					else
						access[pp.x + 256 * pp.y] = 8;
				}
			}
			else
			{
				access[p2.y * 256 + p2.x] |= 2;  //walk access
				if(b == 10)
					access[p2.y * 256 + p2.x] |= 4;  //balloon access
				if(dwa[dwi])
					access[p2.y * 256 + p2.x] |= 1;  //deep water access
				pts.add(p2);
			}
		}
		return true;
	}
	
	public static int randSelect(double[] prob, int[] vals)
	{
		double r = UltimaRando.rand();
		for(int k = 0; k < prob.length; k++)
		{
			if(r < prob[k])
				return vals[k];
		}
		return vals[0];
	}
	
	private Point[] connectNearestAccessPoint(ArrayList<Point> area, int reqAccess)
	{
		int minX = 600;
		int minY = 600;
		int maxX = -600;
		int maxY = -600;
		for(int i = 0; i < area.size(); i++)  //grab a box of the area
		{
			Point p = area.get(i);
			if(minX > p.x)  minX = p.x;
			if(minY > p.y)  minY = p.y;
			if(maxX < p.x)  maxX = p.x;
			if(maxY < p.y)  maxY = p.y;
		}
		Point[] pivots = {new Point(minX, minY), new Point(maxX, minY), new Point(minX, maxY), new Point(maxX, maxY)};
		int minDist = 600;
		//int minDir = -1;
		int dx;
		int dy;
		int dist = 0;
		Point pa = null;
		Point pb = null;
		for(int i = 0; i < 8; i++)
		{
			Point p1 = pivots[i % 4];
			dist = 0;
			if(i < 4)  //check xs
			{
				dx = 1;
				if((i & 1) == 1)
					dx = -1;
				dy = 0;
			}
			else
			{
				dy = 1;
				if((i & 2) == 1)
					dy = -1;
				dx = 0;
			}
			int xx, yy;
			while(true)
			{
				dist++;
				xx = ((p1.x + (dist * dx)) + 256) % 256;
				yy = ((p1.y + (dist * dy)) + 256) % 256;
				byte b = accessData[xx + 256 * yy];
				if((b & reqAccess) > 0)
					break;
				if(dist >= 256)  
					break;
			}
			if(dist < minDist)
			{
				minDist = dist;
				pb = new Point(xx, yy);  //pb is the closest point to access
			//	minDir = i;
			}
		}
		//pa is the nearest point to pb; pb has access while pa does not
		minDist = 5000;
		int ww = 0;
		int hh = 0;
		int xx, yy;
		for(int i = 0; i < area.size(); i++)
		{
			Point p = area.get(i);
			if(p.x > pb.x)
				xx = Math.min(Math.abs(pb.x - p.x), Math.abs((pb.x + 256) - p.x));
			else
				xx = Math.min(Math.abs(pb.x - p.x), Math.abs((p.x + 256) - pb.x));
			if(p.y > pb.y)
				yy = Math.min(Math.abs(pb.y - p.y), Math.abs((pb.y + 256) - p.y));
			else
				yy = Math.min(Math.abs(pb.y - p.y), Math.abs((p.y + 256) - pb.y));
			dist = xx + yy;
			if(dist < minDist)
			{
				minDist = dist;
				pa = p;
				ww = xx;
				hh = yy;
			}
		}
		//draw the box
		xx = Math.min(pa.x, pb.x);
		if(ww != Math.abs(pa.x - pb.x))
			xx = Math.max(pa.x, pb.x);
		yy = Math.min(pa.y, pb.y);
		if(hh != Math.abs(pa.y - pb.y))
			yy = Math.max(pb.x, pb.y);
		double[] mtnPlace = {0.1,0.3,0.9,1.0};
		int[] mtnVal = {10,11,15,14};
		//byte connected = 0;
		byte near = getTerrain(pb.x, pb.y);
		Point pc = normalizePoint(pb);
		byte nacc = accessData[pc.x + 256 * pc.y];
		ArrayList<Point> connection = new ArrayList<Point>();
		if(ww > hh)
		{
			for(int x = 0; x <= ww; x++)
			{
				for(int y = 0; y <= hh; y++)
				{
					byte b = getTerrain(x + xx, y + yy);
					int vv = ((x + xx) % 256) + 256 * ((y + yy) % 256);
					if(b == 7 && near < 10)  //blocked by shoal
					{
						b = 3;
						accessData[vv] = 1;
					}
					else if((b == 13 && near >= 10) || (b >= 10 && near >= 20))  //blocked by mountain
					{
						b = (byte) randSelect(mtnPlace, mtnVal);
						accessData[vv] = 2;
					}
					else
					{
						b = near;
						if(near >= 20)
							b = (byte) 10;
						accessData[vv] = nacc;
					}
					putTerrain(x + xx, y + yy, b);
					Point pn = normalizePoint(new Point(x + xx, y + yy));
					connection.addAll(getSurrPoints(pn));
				}
			}
		}
		else
		{
			for(int y = 0; y <= hh; y++)
			{
				for(int x = 0; x <= ww; x++)
				{
					byte b = getTerrain(x + xx, y + yy);
					int vv = ((x + xx) % 256) + 256 * ((y + yy) % 256);
					if(b == 7 && near < 10)
					{
						b = 3;
						accessData[vv] = 1;
					}
					else if((b == 13 && near >= 10) || (b >= 10 && near >= 20))
					{
						b = (byte) randSelect(mtnPlace, mtnVal);
						accessData[vv] = 2;
					}
					else
					{
						b = near;
						if(near >= 20)
							b = (byte) 10;
						accessData[vv] = nacc;
					}
					putTerrain(x + xx, y + yy, b);
					Point pn = normalizePoint(new Point(x + xx, y + yy));
					connection.addAll(getSurrPoints(pn));
				}
			}
		}
		boolean connected = false;
		for(int i = 0; i < connection.size(); i++)
		{
			Point pn = connection.get(i);
			if(pa.x == pn.x && pa.y == pn.y)
			{
				connected = true;
				break;
			}
		}
		if(connected)
		{
			for(int i = 0; i < area.size(); i++)
			{
				Point p = area.get(i);
				accessData[p.x + 256 * p.y] |= nacc;
				if(getTerrain(p.x, p.y) >= 10)
					accessData[p.x + 256 * p.y] |= 2;
				else
					accessData[p.x + 256 * p.y] |= 1;
			}
		}
		Point[] rv = {pa, pb};
		return rv;
	}
	
	private void openAccess(Point p, int reqAccess)
	{
		ArrayList<Point> openLand = new ArrayList<Point>();
		boolean connectWater = false;
		if(reqAccess == 2)
		{
			openLand = getWalkingPoints(p);
		}
		else
		{
			if(getTerrain(p.x, p.y) >= 10)
				openLand = getWalkingPoints(p);
			else
			{
				openLand = doWaterAccess(p, accessData);
				connectWater = true;
			}
		}
		//System.out.println("openLand has " + openLand.size() + " points; p is " + getTerrain(p.x, p.y) + "; connectWater is " + connectWater);
		connectNearestAccessPoint(openLand, reqAccess);
		//openLand contains the floodfill of access
		//grab the last one
		/*int last = 1;
		Point pe = openLand.get(openLand.size() - 1);
		while(last < openLand.size())
		{
			pe = openLand.get(openLand.size() - last);
			byte t = getTerrain(pe.x, pe.y);
			if(t < 10 && connectWater)
				break;
			if(t >= 10 && connectWater == false)
				break;
			last++;
		}
		int minDist = Integer.MAX_VALUE;
		int d = 1;
		boolean horiz = true;
		if(pe.x > p.x) //go right
		{
			int x = pe.x + 1;
			while((access[(x % 256) + (256 * pe.y)] & reqAccess) == 0)
			{
				x++;
				d++;
			}
		}
		else
		{
			d = -1;
			int x = 256 + pe.x - 1;
			while((access[(x % 256) + (256 * pe.y)] & reqAccess) == 0)
			{
				x--;
				d--;
			}
		}
		minDist = Math.abs(d);
		if(pe.y > p.y) //go down
		{
			d = 1;
			int y = pe.y + 1;
			while((access[pe.x + (256 * (y % 256))] & reqAccess) == 0)
			{
				y++;
				d++;
			}
		}
		else
		{
			d = -1;
			int y = 256 + pe.y - 1;
			while((access[pe.x + (256 * (y % 256))] & reqAccess) == 0)
			{
				y--;
				d--;
			}
		}
		if(Math.abs(d) < minDist)
		{
			minDist = Math.abs(d);
			horiz = false;
		}
		int dir = -1;
		if(d < 0)
			dir = 1;
		byte nd = getTerrain(pe.x, pe.y);
		for(int i = d; i != 0; i += dir)  //fill in the terrain to match the outside point
		{
			if(horiz)
				putTerrain(pe.x + i, pe.y, nd);
			else
				putTerrain(pe.x, pe.y + i, nd);
		}
		byte cval = 0;
		if(nd < 10)  //now floodfill in access from the pe
		{
			ArrayList<Point> wps = doWaterAccess(pe, access);
			for(int i = 0; i < wps.size(); i++)
			{
				Point pw = wps.get(i);
				access[pw.x + 256 * pw.y] = 1;
				if(pw == pe)
					cval = 1;
			}
		}
		else
		{
			ArrayList<Point> wps = getWalkingPoints(pe);
			for(int i = 0; i < wps.size(); i++)
			{
				Point pw = wps.get(i);
				access[pw.x + 256 * pw.y] = 2;
				if(pw == pe)
					cval = 2;
			}
		}
		if(cval > 0)
		{
			for(int i = 0; i < openLand.size(); i++)
			{
				Point pw = openLand.get(i);
				access[pw.x + 256 * pw.y] = cval;
				if(getTerrain(pw.x, pw.y) >= 10)
					access[pw.x + 256 * pw.y] |= 2;
			}
		}*/
	}
	
	private void ensureAccess()
	{
		System.out.print("acc");
		byte[] access = new byte[mapData.length];
		ArrayList<Point>[] vta = new ArrayList[8];
		int[] queueProg = new int[8];
		boolean[] deep = new boolean[8];
		for(int i = 0; i < 8; i++)
		{
			vta[i] = new ArrayList<Point>();
			deep[i] = false;
			queueProg[i] = -1;
		}
		
		boolean more = false;
		do
		{
			more = false;
			for(int i = 0; i < 8; i++)
			{
				if(doAccess(vta[i], queueProg[i], virtueTowns.get(i), access, i, deep))
				{
					queueProg[i]++;
					more = true;
				}
			}
		} while(more);
		//lists
		accessData = access;
		//MapWindow mmw = new MapWindow(mapData, false);
		//mmw.setAccessData(access);
		ArrayList<Point> allPoints = new ArrayList<Point>();
		//allPoints.addAll(moongates);
		for(int i = 0; i < moongates.size(); i++)
		{
			Point p = moongates.get(i);
			if((access[p.y * 256 + p.x] & 2) == 0)
			{
				//System.out.println("Moongate # " + i + " isn't walk accessable; opening access");
				System.out.print("OM" + i);
				openAccess(p, 2);
			}
		}
		allPoints.clear();
		/*Point humPt = shrines.get(7);
		humPt = normalizePoint(new Point(humPt.x, humPt.y - 2));*/
		allPoints.addAll(shrines);
		//allPoints.add(humPt);
		//allPoints.remove(7);
		
		allPoints.addAll(townsNCastles);
		allPoints.addAll(searchSpots);
		allPoints.add(abyss);
		for(int i = 0; i < allPoints.size(); i++)
		{
			Point p = allPoints.get(i);
			byte acc = access[p.y * 256 + p.x];
			if(acc == 0 || acc == 8)
			{
				//System.out.println("Generic point # " + i + "(" + p + ") isn't accessable; opening access");
				System.out.print("OG" + i);
				openAccess(p, 3);
			}
		}
		allPoints.clear();
		allPoints.addAll(dungeons);
		//int dAccess = 0;
		for(int i = 0; i < allPoints.size(); i++)
		{
			Point p = allPoints.get(i);
			byte acc = access[p.y * 256 + p.x];
			if(acc > 0 && acc < 8)
			{
				allPoints.remove(i);
				i--;
			}
		}
		//System.out.println(allPoints.size() + " dungeons are currently inaccessible; direct connecting all but 2");
		while(allPoints.size() > 2)
		{
			int i = (int) (UltimaRando.rand() * allPoints.size());
			Point p = allPoints.get(i);
			System.out.print("OD" + i);
			openAccess(p, 3);
			allPoints.remove(i);
		}
		//ensure balloon access to all remaining points
		//System.out.println(allPoints.size() + " dungeons are getting balloon access only");
		for(int i = 0; i < allPoints.size(); i++)
		{
			Point p = allPoints.get(i);
			System.out.print("OB" + i);
			openLandForBalloon(p, access);
			/*ArrayList<Point> pts = getSurrPoints(p);
			for(int j = 0; j < pts.size(); j++)
			{
				Point p2 = pts.get(j);
				byte b = mapData[p2.x + 256 * p.y];
				if(b != 13 && b >= 10)
					
			}*/
			allPoints.remove(i);
			i--;
		}
		humShrineAccess();
		System.out.print(";");
		accessData = access;
	}
	
	public int humShrineOpening;
	private void humShrineAccess() 
	{
		ArrayList<Point> pts = getSurrPoints(shrines.get(7));
		humShrineOpening = 2;
		Point shrub = pts.remove(2);
		for(int i = 0; i < pts.size(); i++)
		{
			Point p = pts.get(i);
			if(getTerrain(p.x, p.y) != 13)
			{
				byte c = getTerrain(p.x, p.y);
				if(i < 2)
				{
					for(int j = -2; j <= 2; j++)
					{
						putTerrain(p.x - 1 + (i * 2), p.y + j, c);
					}
				}
				else
				{
					for(int j = -2; j <= 2; j++)
					{
						putTerrain(p.x + j, p.y + 1, c);
					}
					i++;
				}
				putTerrain(shrub.x, shrub.y, (byte) 13);  //block north
				humShrineOpening = i;
				//ensure balloon access to horn island
				
				break;
			}
		}
		
	}

	private Point placeWhirlpool()  //placement of this requires access to be known
	{
		System.out.print("W");
		ArrayList<MapZone> fors = selectZonesOfType(1);
		int fail = 0;
		while(true)
		{
			fail++;
			if(fail > 200)
				break;
			MapZone ff = fors.get((int) (UltimaRando.rand() * fors.size()));
			int dx = (int) (UltimaRando.rand() * 20 + 5);
			int dy = (int) (UltimaRando.rand() * 20 + 5);
			if(UltimaRando.rand() < 0.5)
				dx *= -1;
			if(UltimaRando.rand() < 0.5)
				dy *= -1;
			Point fp = new Point(ff.cx + dx, ff.cy + dy);
			if(getTerrain(fp.x, fp.y) == 1)
			{
				fp.x += 256;
				fp.x %= 256;
				fp.y += 256;
				fp.y %= 256;
				if(accessData[fp.x + 256 * fp.y] != 1)
					continue;
				//putTerrain(fp.x, fp.y, (byte) 14);
				//searchSpots.add(fp);
				if(ensurePOIUnique(new Rectangle(fp.x, fp.y, 1, 1), false) == false)
					continue;
				return fp;
			}
		}
		//the hard way
		ArrayList<Point> pts = new ArrayList<Point>();
		for(int i = 0; i < 256; i++)
		{
			for(int j = 0; j < 256; j++)
			{
				if(mapData[256 * i + j] == 1)
					pts.add(new Point(j, i));
			}
		}
		while(true)
		{
			if(pts.size() == 0)
				break;
			double r = UltimaRando.rand() * pts.size();
			int sp = (int) r;
			Point fp = pts.remove(sp);
			if(ensurePOIUnique(new Rectangle(fp.x, fp.y, 1, 1), false) == false)
				continue;
			return fp;
		}
		//absolutely no hope at this point?!?
		return null;
	}
	
	private Point placeBalloon()
	{
		System.out.print("B");
		ArrayList<Point> d2 = new ArrayList<Point>();
		ArrayList<Integer> ord = new ArrayList<Integer>();
		for(int i = 0; i < dungeons.size(); i++)  //front load dungeons with balloon access only
		{
			Point p = dungeons.get(i);
			if(accessData[p.x + p.y * 256] == 4)
			{
				d2.add(p);
				ord.add(i);
			}
		}
		for(int i = 0; i < dungeons.size(); i++)
		{
			Point p = dungeons.get(i);
			if(accessData[p.x + p.y * 256] != 4)
			{
				d2.add(p);
				ord.add(i);
			}
		}
		Point pb = null;
		int minSize = 3;
		for(int i = 0; i < d2.size(); i++)
		{
			Point p = d2.get(i);
			ArrayList<Point> pw = getWalkingPoints(p);
			if(pw.size() > minSize + 1)
			{
				pw.remove(0);
				while(pw.size() > minSize)
				{
					int idx = (int) (UltimaRando.rand() * Math.min(pw.size(), 20));
					pb = pw.get(idx);
					if(ensurePOIUnique(new Rectangle(pb.x, pb.y, 1, 1), false) == false)
					{
						pw.remove(idx);
						continue;
					}
					putTerrain(pb.x, pb.y, (byte) 10);
					balloonDungeon = ord.get(i);
					return pb;
				}
			}
		}
		//at this point the loop above will fail as the point
		Point p = d2.get(0);
		ArrayList<Point> pw = getWalkingPoints(p);
		pw.remove(0);
		balloonDungeon = 0;
		return pw.get(0);  //this will ensure that the balloon is right next to a dungeon opening
	}
	
	//using using rectangles will not be nearly as bad as using individual points
	private boolean ensurePOIUnique(Rectangle nr, boolean insert)
	{
		//System.out.println("Testing " + nr.toString() + " for POI collision");
		Point p = new Point(nr.x, nr.y);
		normalizePoint(p);
		nr.x = p.x;
		nr.y = p.y;
		for(int j = 0; j < poi.size(); j++)
		{
			Rectangle rr = poi.get(j);
			if(rectContains(rr, new Point(nr.x, nr.y)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 1");
				return false;
			}
			if(rectContains(rr, new Point(nr.x + nr.width - 1, nr.y)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 2");
				return false;
			}
			if(rectContains(rr, new Point(nr.x, nr.y + nr.height - 1)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 3");
				return false;
			}
			if(rectContains(rr, new Point(nr.x + nr.width - 1, nr.y + nr.height - 1)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 4");
				return false;
			}
			if(rectContains(nr, new Point(rr.x, rr.y)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 5");
				return false;
			}
			if(rectContains(nr, new Point(rr.x + rr.width - 1, rr.y)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 6");
				return false;
			}
			if(rectContains(nr, new Point(rr.x, rr.y + rr.height - 1)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 7");
				return false;
			}
			if(rectContains(nr, new Point(rr.x + rr.width - 1, rr.height + rr.y - 1)))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 8");
				return false;
			}
		}
		if(insert)
			poi.add(nr);
		return true;
	}
	
	private ArrayList<Point> placeVirtueTowns()
	{
		int[] vtZones = new int[8];
		ArrayList<Point> rv = new ArrayList<Point>(8);
		int reqIslandTowns = 2;
		int setIslandTowns = 0;
		int aa = 0;
		System.out.print("vt");
		for(int i = 0; i < 8; i++)
			vtZones[i] = -1;
		for(int i = 0; i < 8; i++)
		{
			//System.out.println("Placing virtue town ");
			System.out.print(i);
			int fail = 0;
			while(true)
			{
				int zn = -1;
				MapZone feat = null;
				if(setIslandTowns < reqIslandTowns)
				{
					ArrayList<MapZone> isl = grabFreeFeaturesOfType(6, false);
					if(isl.size() == 0)
					{
						aa++;
						isl.addAll(grabFreeFeaturesOfType(7, false));
					}
					if(isl.size() == 0)
					{
						aa++;
						isl.addAll(grabFreeFeaturesOfType(8, false));
					}
					if(isl.size() < reqIslandTowns)
						reqIslandTowns = isl.size();
					if(setIslandTowns < reqIslandTowns)
					{
						double r = UltimaRando.rand();
						int sel = (int) (r * isl.size() * 1.0);
						zn = isl.get(sel).inZone;
						feat = isl.get(sel);
						//System.out.println("Set island feature for town " + i);
					}
					else
					{
						double r = UltimaRando.rand();
						zn = (int) (r * zones.size());
					}
				}
				else
				{
					double r = UltimaRando.rand();
					zn = (int) (r * zones.size());
				}
				boolean unique = true;
				for(int j = i - 1; j >= 0; j--) //test new zone
				{
					//towns must be in a unique zone
					if(zn == vtZones[j])
					{
						unique = false;
						//System.out.print("r");
						break;
					}
				}
				if(unique)
				{
					byte terrT = zones.get(zn).terrain;
					if(terrT == 13 || terrT < 10 || feat != null)
					{
						//find a feature you can put the town in
						//System.out.println("Using a feature");
						System.out.print("f");
						boolean featureFound = false;
						if(feat == null)  //find a feature in this zone if you didn't already find one
						{
							for(int f = 0; f < features.size(); f++)
							{
								MapZone ff = features.get(f);
								if(ff.inZone == zn && ff.featureOKForTown(aa) && ff.isUsed == false)
								{
									feat = ff;
									break;
								}
							}
						}
						if(feat != null)
						{
							//find a random point
							//System.out.println("Found a feature to use");
							System.out.print("F");
							Point px = null;
							int sp = -1;
							ArrayList<Point> pts = new ArrayList<Point>(feat.filled);
							do
							{
								px = null;
								if(sp != -1)
									pts.remove(sp);
								if(pts.size() == 0)
									break;
								double r = UltimaRando.rand();
								sp = (int) (r * pts.size());
								px = pts.get(sp);
							} while(ensurePOIUnique(new Rectangle(px.x - 1, px.y - 1, 3, 3), true) == false);
							if(px != null)
							{
								for(int yy = -1; yy <= 1; yy++)
									for(int xx = -1; xx <= 1; xx++)
										putTerrain(px.x + xx, px.y + yy, (byte) 10);
								putTerrain(px.x, px.y, (byte) 20);
								//System.out.println("Found a spot");
								System.out.print("P");
								rv.add(px);
								//System.out.println("VT#" + i + " was placed on a feature type " + feat.terrain + " @" + feat.area);
								featureFound = true;
								if(feat.terrain >= 6)
									setIslandTowns++;
								feat.isUsed = true;
							}
						}
						if(featureFound == false)
							//System.out.println("Failed to find a suitable feature in this zone");
							System.out.print("X");
						unique = featureFound;
					}
					else  
					{
						Point px = null;
						//System.out.println("Using a zone");
						System.out.print("z");
						do
						{
							int dx = (int) (UltimaRando.rand() * 40 - 20);
							int dy = (int) (UltimaRando.rand() * 40 - 20);
							px = new Point(zones.get(zn).getCenterX() + dx, zones.get(zn).getCenterY() + dy);
						} while(ensurePOIUnique(new Rectangle(px.x - 1, px.y - 1, 3, 3), true) == false);
						for(int yy = -1; yy <= 1; yy++)
							for(int xx = -1; xx <= 1; xx++)
								putTerrain(px.x + xx, px.y + yy, (byte) 10);
						//System.out.println("Found a spot");
						System.out.print("P");
						putTerrain(px.x, px.y, (byte) 20);
						//System.out.println("VT#" + i + " was placed int zone #" + zn  + " with terrain " + zones.get(zn).terrain);
						rv.add(px);
					}
				}
				if(unique)
				{
					vtZones[i] = zn;
					break;
				}
				else
				{
					fail++;
					if(fail == 10)
					{
						//System.out.println("Failed to place town #" + i);
						aa++;
						double r = UltimaRando.rand();
						int rr = (int) (r * i);  //a random zone must be re-used to avoid infinite loop
						vtZones[rr] = -1;
						fail = 0;
					}
				}
			}
		}
		for(int i = 0; i < rv.size(); i++)  //normalize the locations
		{
			Point p = rv.get(i);
			if(p.x < 0) p.x += 256;
			if(p.x > 255) p.x -= 255;
			if(p.y < 0) p.y += 256;
			if(p.y > 255) p.y -= 255;
			putTerrain(p.x, p.y, (byte) 20);  //one last sure placement
		}
		System.out.print(";");
		return rv;
	}
	
	private Point placeAbyss()
	{
		//System.out.println("Now placing the abyss");
		System.out.print("Y");
		ArrayList<MapZone> vis = grabFreeFeaturesOfType(8, true);
		//a volcanic island found this way must be found on the x == y diagonal line
		ArrayList<MapZone> abyssOK = new ArrayList<MapZone>();
		for(int i = 0; i < vis.size(); i++)
		{
			MapZone zn = vis.get(i);
			if(Map.containsCenterDiagonal(zn.area))
				abyssOK.add(zn);
		}
		int failures = 0;
		while(abyssOK.size() == 0 && failures < 300)
		{
			//place a new volcanic island with a crater in it
			//System.out.println("No volcanic islands found; placing a new one for abyss");
			failures++;
			Rectangle vi = noOlapRect(features, 12, 30, 0, true);
			if(vi == null)
				continue;
			MapZone zn = new MapZone(vi, (byte) 8, 0);
			if(zn.majorityIsLand())
				continue;
			if(ensurePOIUnique(vi, false) == false)
				continue;
			zn.placeVolcanicIsland();
			features.add(zn);
			putTerrain(zn.cx, zn.cx, (byte) 17);
			vis.add(zn);
			abyssOK.add(zn);
		}
		while(abyssOK.size() == 0)  //at this point there are only 3 other POI so this should not go infinite
		{
			Rectangle vi = noOlapRect(features, 12, 30, 0, true);
			MapZone zn = new MapZone(vi, (byte) 8, 0);
			if(ensurePOIUnique(vi, false) == false)
				continue;
			zn.placeVolcanicIsland();
			features.add(zn);
			
			putTerrain(zn.cx, zn.cx, (byte) 17);
			abyssOK.add(zn);
			vis.add(zn);
		}
		ArrayList<Point> craters = new ArrayList<Point>();
		for(int i = 0; i < vis.size(); i++)
		{
			MapZone zn = vis.get(i);
			
			for(int j = 0; j < zn.filled.size(); j++)
			{
				Point p = zn.filled.get(j);
				byte bb = getTerrain(p.x, p.y);
				if(bb == 17)
					craters.add(p);
			}
		}
		//System.out.println("Found " + craters.size() + "craters");
		if(craters.size() == 0)
		{
			//grab a VI and put a crater on it
			//System.out.println("Putting a crater on volc island 0 because no craters found");
			boolean craterPlaced = false;
			for(int i = 0; i < vis.size(); i++)
			{
				MapZone zn = vis.get(i);
				ArrayList<Point> pts = new ArrayList<Point>(zn.filled);
				while(pts.size() > 0)
				{
					double r = UltimaRando.rand() * pts.size();
					Point p = pts.remove((int) r);
					//we will deal only with the p.x at this point to ensure that p.x == p.y
					if(ensurePOIUnique(new Rectangle(p.x, p.x, 1, 1), false) == false)
						continue;
					putTerrain(p.x, p.x, (byte) 17);
					craters.add(new Point(p.x, p.x));
					craterPlaced = true;
					break;
				}
				if(craterPlaced)
					break;
			}
		}
		ArrayList<Point> abCraters = new ArrayList<Point>(craters.size());
		for(int i = 0; i < craters.size(); i++)
		{
			Point p = craters.get(i);
			if(p.x == p.y)
				abCraters.add(p);
		}
		if(abCraters.size() == 0)
		{
			//select the crater with the lowest distance
			int minD = Integer.MAX_VALUE;
			int minI = -1;
			for(int i = 0; i < craters.size(); i++)
			{
				Point p = craters.get(i);
				int dd = Math.abs(p.x - p.y);
				if(dd < minD)
				{
					minD = dd;
					minI = i;
				}
			}
			//and move the crater
			Point pnc = craters.get(minI);
			putTerrain(pnc.x, pnc.y, (byte) 16);
			craters.remove(minI);
			Point nc = new Point(pnc.x, pnc.x);
			putTerrain(pnc.x, pnc.x, (byte) 17);
			craters.add(nc);
			abCraters.add(nc);
		}
		double r = UltimaRando.rand() * abCraters.size();
		Point ab = abCraters.get((int) r);
		putTerrain(ab.x, ab.y, (byte) 29);
		ensurePOIUnique(new Rectangle(ab.x, ab.y, 1, 1), true);
		while(craters.size() > 3)
		{
			//eliminate a random crater from map 
			//System.out.println(craters.size() + " craters left");
			double c = UltimaRando.rand() * craters.size();
			Point cp = craters.get((int) c);
			if(cp != ab)
			{
				putTerrain(cp.x, cp.y, (byte) 16);
				craters.remove((int) c);
			}
		}
		//System.out.println("Abyss placed at " + ab.x + "," + ab.y);
		return ab;
	}
	
	private ArrayList<Point> placeMoongates(ArrayList<Point> virtueTowns)
	{
		ArrayList<Point> rv = new ArrayList<Point>();
		System.out.print("mg");
		for(int i = 0; i < 8; i++)
		{
			//System.out.println("I am placing a moongate");
			System.out.print(i);
			Point vt = virtueTowns.get(i);
			Point pm = null;
			int dx, dy, rects;
			Rectangle uRect;
			do
			{
				rects = (int) ((UltimaRando.rand() * 3.0) + 2);
				dx = (int) ((UltimaRando.rand() * 5.0) + 2);
				dy = (int) ((UltimaRando.rand() * 5.0) + 2);
				int quad = (int) (UltimaRando.rand() * 4.0);
				switch(quad)
				{
					case 1:  dx *= -1;  break;
					case 2:  dy *= -1;  break;
					case 3:  dx *= -1;  dy *= -1;  break;
				}
				pm = new Point(vt.x + dx, vt.y + dy);
				int ux, uy, uw, uh;
				if(dx > 0) 
				{
					ux = vt.x + 2;
					uw = dx - 1;
				}
				else 
				{
					ux = pm.x - 1;
					uw = Math.abs(dx - 1);
				}
				if(dy > 0) 
				{
					uy = vt.y + 2;
					uh = dy - 1;
				}
				else 
				{
					uy = pm.y - 1;
					uh = Math.abs(dy  - 1);
				}
				uRect = new Rectangle(ux, uy, uw, uh);
			} while(ensurePOIUnique(uRect, true) == false);
			System.out.print("p");
			//System.out.println("I have found the place for the moongate");
			int rdx = dx;
			double xAvg = rdx / rects;
			int rdy = dy;
			double yAvg = rdy / rects;
			int rsx = vt.x;
			int sdx = -1;
			if(dx < 0)  sdx = 1;
			int rsy = vt.y;
			int sdy = -1;
			if(dy < 0)  sdx = 1;
			else rsy--;
			ArrayList<Rectangle> rs = new ArrayList<Rectangle>(rects);
			for(int j = 0; j < rects - 1; j++)
			{
				int gx = (int) ((UltimaRando.rand() * 2.0) + (xAvg - 1.0));
				int gy = (int) ((UltimaRando.rand() * 2.0) + (yAvg - 1.0));
				rdx -= gx;
				rdy -= gy;
				if(gx < 0)
					rsx += (gx);
				if(gy < 0)
					rsy += (gy);
				
				xAvg = rdx * 1.0 / (rects - j - 1);
				yAvg = rdx * 1.0 / (rects - j - 1);
				rs.add(new Rectangle(rsx + sdx, rsy + sdy, Math.abs(gx) + 1, Math.abs(gy) + 1));
				if(gx > 0)
					rsx += gx;
				if(gy > 0)
					rsy += gy;
			}
			if(rdx < 0)
				rsx += (rdx);
			if(rdy < 0)
				rsy += (rdy);
			rs.add(new Rectangle(rsx + sdx, rsy + sdy, Math.abs(rdx) + 2, Math.abs(rdy) + 2));
			byte[] badList = {1,3,7,12,13,14,16,17};
			for(int j = 0; j < rs.size(); j++)
			{
				Rectangle r = rs.get(j);
				for(int xx = r.x; xx < r.x + r.width; xx++)
				{
					for(int yy = r.y; yy < r.y + r.height; yy++)
					{
						byte bb = getTerrain(xx, yy);
						for(int k = 0; k < badList.length; k++)
						{
							if(bb == badList[k])
							{
								if(UltimaRando.rand() < 0.5)
									bb = 10;
								else
									bb = 11;
								putTerrain(xx, yy, bb);
								break;
							}
						}
					}
				}
			}
			ArrayList<Point> s4 = getSurrPoints(pm);
			for(int j = 0; j < 4; j++)
			{
				Point p = s4.get(j);
				byte bb = getTerrain(p.x, p.y);
				for(int k = 0; k < badList.length; k++)
				{
					if(bb == badList[k])
					{
						if(UltimaRando.rand() < 0.5)
							bb = 10;
						else
							bb = 11;
						putTerrain(p.x, p.y, bb);
						break;
					}
				}
			}
			//System.out.println("I am now placing the moongate");
			System.out.print("P");
			rv.add(pm);
		}
		for(int i = 0 ; i < rv.size(); i++)
		{
			Point mp = rv.get(i);
			putTerrain(mp.x, mp.y, (byte) 30);
		}
		//normalize the points
		for(int i = 0; i < rv.size(); i++)
			rv.set(i, normalizePoint(rv.get(i)));
		System.out.print(";");
		return rv;
	}
	
	public static Point normalizePoint(Point p)
	{
		p.x = (p.x + 256) % 256;
		p.y = (p.y + 256) % 256;
		return p;
	}
	
	private ArrayList<Point> placeOtherTowns()
	{
		int[] vtZones = new int[4];
		ArrayList<Point> rv = new ArrayList<Point>(4);
		int reqSeaTowns = 2;  //Buccanneer's Den and Cove must be next to water
		int setSeaTowns = 0;
		int aa = 0;
		for(int i = 0; i < 4; i++)
			vtZones[i] = -1;
		System.out.print("nt");
		for(int i = 0; i < 4; i++)
		{
			System.out.print(i);
			int fail = 0;
			while(true)
			{
				int zn = -1;
				MapZone feat = null;
				if(setSeaTowns < reqSeaTowns && UltimaRando.rand() > 0.5)
				{
					ArrayList<MapZone> isl = grabFreeFeaturesOfType(6, false);
					if(isl.size() == 0)
					{
						aa++;
						isl.addAll(grabFreeFeaturesOfType(7, false));
					}
					if(isl.size() == 0)
					{
						aa++;
						isl.addAll(grabFreeFeaturesOfType(8, false));
					}
					if(setSeaTowns < reqSeaTowns && isl.size() > 0)
					{
						double r = UltimaRando.rand();
						int sel = (int) (r * isl.size() * 1.0);
						zn = isl.get(sel).inZone;
						feat = isl.get(sel);
						//System.out.println("Set island feature for town " + i);
					}
					else
					{
						double r = UltimaRando.rand();
						zn = (int) (r * zones.size());
					}
				}
				else
				{
					double r = UltimaRando.rand();
					zn = (int) (r * zones.size());
				}
				boolean unique = true;
				for(int j = i - 1; j >= 0; j--) //test new zone
				{
					//towns must be in a unique zone
					if(zn == vtZones[j])
					{
						unique = false;
						System.out.print("r");
						break;
					}
				}
				if(unique)
				{
					//System.out.println("Found zone for normal town");
					System.out.print("z");
					byte terrT = zones.get(zn).terrain;
					if(terrT == 13 || terrT < 10 || feat != null)
					{
						//find a feature you can put the town in
						//System.out.println("Town uses a feature");
						System.out.print("f");
						boolean featureFound = false;
						if(feat == null)  //if you didn't already find one
						{
							for(int f = 0; f < features.size(); f++)
							{
								MapZone ff = features.get(f);
								if(ff.inZone == zn && ff.featureOKForTown(aa) && ff.isUsed == false)
								{
									feat = ff;
									break;
								}
							}
						}
						if(feat != null)
						{
							//find a random point
							Point px = null;
							int sp = -1;
							ArrayList<Point> pts = new ArrayList<Point>(feat.filled);
							do
							{
								px = null;
								if(sp > -1)
									pts.remove(sp);
								if(pts.size() > 0)
								{
									double r = UltimaRando.rand();
									sp = (int) (r * pts.size());
									px = pts.get(sp);
								}
								else
									break;
							} while(ensurePOIUnique(new Rectangle(px.x, px.y, 1, 1), true) == false);
							/*for(int yy = -1; yy <= 1; yy++)
								for(int xx = -1; xx <= 1; xx++)
									putTerrain(px.x + xx, px.y + yy, (byte) 10);*/
							//System.out.println("Found location for town");
							if(px == null)
							{
								//i--;
								continue;
							}
							System.out.print("P");
							putTerrain(px.x, px.y, (byte) 20);
							rv.add(px);
							//System.out.println("VT#" + i + " was placed on a feature type " + feat.terrain + " @" + feat.area);
							featureFound = true;
							if(feat.terrain >= 6)
								setSeaTowns++;
							feat.isUsed = true;
						}
						unique = featureFound;
					}
					else  
					{
						Point px = null;
						//System.out.println("I am using a zone for the town");
						System.out.print("Z");
						boolean seaAnchor = false;
						do
						{
							seaAnchor = false;
							int dx = (int) (UltimaRando.rand() * 40 - 20);
							int dy = (int) (UltimaRando.rand() * 40 - 20);
							px = new Point(zones.get(zn).getCenterX() + dx, zones.get(zn).getCenterY() + dy);
							if(setSeaTowns < reqSeaTowns)  //anchor the town to the nearest sea
							{
								//System.out.println("The town requires a sea anchor");
								System.out.print("a");
								//Point pa = null;
								ArrayList<Point> toSea = new ArrayList<Point>();
								toSea.add(px);
								int point = 0;
								boolean[] v = new boolean[mapData.length];
								v[px.y * 256 + px.x] = true;
								while(true)
								{
									Point p = toSea.get(point);
									if(mapData[p.y * 256 + p.x] < 7)
										break;
									ArrayList<Point> ta = getSurrPoints(p);
									for(int j = 0; j < ta.size(); j++)
									{
										Point tj = ta.get(j);
										if(v[tj.y * 256 + tj.x] == false)
										{
											toSea.add(tj);
											v[tj.y * 256 + tj.x] = true;
										}
									}
									point++;
								}
								ArrayList<Point> landAnchor = getSurrPoints(toSea.get(point));  //point is a sea point
								for(int k = 0; k < 4; k++)
								{
									Point p = landAnchor.get(k);
									if(mapData[p.y * 256 + p.x] >= 10)
									{
										px = p;
										//initPoss |= ( 1 << i);
										//initDir = i;
										seaAnchor = true;
										break;
									}
								}
							}
						} while(ensurePOIUnique(new Rectangle(px.x, px.y, 1, 1), true) == false);
						/*for(int yy = -1; yy <= 1; yy++)
							for(int xx = -1; xx <= 1; xx++)
								putTerrain(px.x + xx, px.y + yy, (byte) 10);*/
						//System.out.println("I have found a place for the town");
						System.out.print("P");
						putTerrain(px.x, px.y, (byte) 20);
						if(seaAnchor)
							setSeaTowns++;
						//System.out.println("VT#" + i + " was placed int zone #" + zn  + " with terrain " + zones.get(zn).terrain);
						rv.add(px);
					}
				}
				if(unique)
				{
					vtZones[i] = zn;
					break;
				}
				else
				{
					fail++;
					if(fail == 10)
					{
						//System.out.println("Failed to place town #" + i);
						System.out.print("X");
						aa++;
						double r = UltimaRando.rand();
						int rr = (int) (r * i);  //a random zone must be re-used to avoid infinite loop
						vtZones[rr] = -1;
						fail = 0;
					}
				}
			}
		}
		for(int i = 0; i < rv.size(); i++)  //normalize the locations
		{
			Point p = rv.get(i);
			if(p.x < 0) p.x += 256;
			if(p.x > 255) p.x -= 255;
			if(p.y < 0) p.y += 256;
			if(p.y > 255) p.y -= 255;
			putTerrain(p.x, p.y, (byte) 20);  //one last sure placement
			//System.out.println("Placed normal town at " + p.x + ", " + p.y);
		}
		System.out.print(";");
		return rv;
	}
	
	private ArrayList<Point> placeDungeons()  //the 8 dungeons are the 7 dungeons + Serpent's Spine; Abyss is placed separately
	{
		int[] vtZones = new int[8];
		ArrayList<Point> rv = new ArrayList<Point>(8);
		ArrayList<MapZone> mtnZones = new ArrayList<MapZone>();
		ArrayList<MapZone> okFeatures = new ArrayList<MapZone>();
		for(int i = 0; i < zones.size(); i++)  //gather mountain zones
		{
			if(zones.get(i).terrain == 13)
				mtnZones.add(zones.get(i));
		}
		for(int i = 0; i < features.size(); i++) //gather unused features
		{
			MapZone ff = features.get(i);
			if(ff.isUsed)  //skip used features
				continue;
			
			if(ff.featureOKForDungeon())  //add if OK for dungeon
				okFeatures.add(ff);
			for(int j = 0; j < mtnZones.size(); j++)  //add if it's on a mountain
			{
				if(ff.inZone == mtnZones.get(j).index)
					okFeatures.add(ff);
			}
		}
		System.out.print("dg");
		for(int i = 0; i < 8; i++)
		{
			System.out.print(i);
			if(okFeatures.size() == 0)
			{
				//grab a mountain zone
				break;
			}
			double r = UltimaRando.rand();
			int fsel = (int) (r * okFeatures.size());
			MapZone dz = okFeatures.get(fsel);
			
			if(dz.featureOKForDungeon())  //convert a random mountain tile on the feature to the dungeon
			{
				//System.out.println("Placing dungeon on dungeon-friendly feature");
				System.out.print("f");
				ArrayList<Point> mtnPoints = new ArrayList<Point>();
				for(int j = 0; j < dz.filled.size(); j++)
				{
					Point p = dz.filled.get(j);
					if(getTerrain(p.x, p.y) == 13)
						mtnPoints.add(p);
				}
				//System.out.println("Feature has " + mtnPoints.size() + " mountains and " + dz.filled.size() + " other points");
				System.out.print("m" + mtnPoints.size() + "s" + dz.filled.size());
				Point fdp = null;
				int fails = 0;
				do
				{
					if(fails > 30)
					{
						//System.out.println("This feature doesn't work");
						System.out.print("X");
						break;
					}
					r = UltimaRando.rand();
					int dp = -1;
					
					if(mtnPoints.size() == 0)
					{
						dp = (int) (r * dz.filled.size());
						fdp = dz.filled.get(dp);
					}
					else
					{
						dp = (int) (r * mtnPoints.size());
						fdp = mtnPoints.get(dp);
					}
					fails++;
				} while(ensurePOIUnique(new Rectangle(fdp.x - 1, fdp.y - 1, 3, 3), true) == false);
				if(fails > 30)
				{
					okFeatures.remove(fsel);
					i--;
					continue;
				}
				//System.out.println("Found dungeon spot");
				System.out.print("P");
				rv.add(fdp);
				dz.isUsed = true;
				okFeatures.remove(fsel);
			}
			else  //you are a feature on a mountain
			{
				//System.out.println("Placing dungeon on feature in mountain");
				System.out.print("M");
				Point fdp = null;
				int dp = -1;
				//int fail = 0;
				ArrayList<Point> ptsHere = new ArrayList<Point>(dz.filled.size());
				ptsHere.addAll(dz.filled);
				do
				{
					if(dp >= 0)
						ptsHere.remove(dp);
					System.out.print(ptsHere.size() + "|");
					if(ptsHere.size() == 0)
						break;
					r = UltimaRando.rand();
					dp = (int) (r * ptsHere.size());
					fdp = ptsHere.get(dp);
					ArrayList<Point> pts = getSurrPoints(fdp);
					fdp = null;
					for(int k = 0; k < pts.size(); k++)
					{
						Point p = pts.get(k);
						if(getTerrain(p.x, p.y) == 13)
						{
							fdp = p;
							break;
						}
					} 
				} while(fdp == null || ensurePOIUnique(new Rectangle(fdp.x - 1, fdp.y - 1, 3, 3), true) == false);
				if(ptsHere.size() == 0)
				{
					okFeatures.remove(fsel);
					i--;
					continue;
				}
				//System.out.println("Found dungeon spot");
				System.out.print("P");
				rv.add(fdp);
				dz.isUsed = true;
				okFeatures.remove(fsel);
			}
		}
		
		for(int i = 0; i < rv.size(); i++)  //normalize the locations
		{
			Point p = rv.get(i);
			normalizePoint(p);
			//System.out.println("Placing dungeon #" + i + " at " + p.x + "," + p.y);
			putTerrain(p.x, p.y, (byte) 21);  //place the dungeon
			ArrayList<Point> dps = getSurrPoints(p);
			int mtnCount = 0;
			for(int j = 0; j < dps.size(); j++)  //each dungeon is surrounded by exactly 3 mountains
			{
				Point p2 = dps.get(j);
				if(getTerrain(p2.x, p2.y) == 13)
					mtnCount++;
			}
			if(mtnCount == 4)  //replace the last mountain with a walkable land
			{
				double r = UltimaRando.rand() * 4;
				int ch = (int) r;
				Point p2 = dps.get(ch);
				r = UltimaRando.rand();
				if(r < 0.2)  putTerrain(p2.x, p2.y, (byte) 10);
				else if(r < 0.5)  putTerrain(p2.x, p2.y, (byte) 11);
				else if(r < 0.8)   putTerrain(p2.x, p2.y, (byte) 15);
				else  putTerrain(p2.x, p2.y, (byte) 14);
			}
			else
			{
				while(mtnCount < 3)
				{
					double r = UltimaRando.rand() * 4;
					int ch = (int) r;
					Point p2 = dps.get(ch);
					if(getTerrain(p2.x, p2.y) != 13)
					{
						putTerrain(p2.x, p2.y, (byte) 13);
						mtnCount++;
					}
				}
				for(int j = 0; j < dps.size(); j++)  //find the last one
				{
					Point p2 = dps.get(j);
					if(getTerrain(p2.x, p2.y) != 13)
					{
						double r = UltimaRando.rand();
						if(r < 0.2)  putTerrain(p2.x, p2.y, (byte) 10);
						else if(r < 0.5)  putTerrain(p2.x, p2.y, (byte) 11);
						else if(r < 0.8)   putTerrain(p2.x, p2.y, (byte) 15);
						else  putTerrain(p2.x, p2.y, (byte) 14);
					}
				}
			}
		}
		System.out.print(";");
		return rv;
	}
	
	private ArrayList<Point> placeShrines()  //each shrine is place at a feature but with less restrictions; but each shrine should be in a different zone
	{  
		int[] vtZones = new int[8];
		for(int i = 0; i < 8; i++)
			vtZones[i] = -1;
		ArrayList<Point> rv = new ArrayList<Point>(8);
		//ArrayList<MapZone> mtnZones = new ArrayList<MapZone>();
		ArrayList<MapZone> okFeatures = new ArrayList<MapZone>();
		/*for(int i = 0; i < zones.size(); i++)
		{
			if(zones.get(i).terrain == 13)
				mtnZones.add(zones.get(i));
		}*/
		for(int i = 0; i < features.size(); i++)
		{
			MapZone ff = features.get(i);
			if(ff.isUsed)
				continue;
			if(!ff.featureOKForShrine())
				continue;
			//if(ff.featureOKForDungeon())
			okFeatures.add(ff);
			/*for(int j = 0; j < mtnZones.size(); j++)
			{
				if(ff.inZone == mtnZones.get(j).index)
					okFeatures.add(ff);
			}*/
		}
		System.out.print("sh");
		for(int i = 0; i < 8; i++)  //there are 8 shrines; the Shrine of Spirituality isn't on the map but the island shrine is
		{
			if(okFeatures.size() == 0)
				break;
			System.out.print(i);
			int failures = 0;
			MapZone sz = null;
			int fsel = -1;
			while(failures < 20)
			{
				double r = UltimaRando.rand();
				fsel = (int) (r * okFeatures.size());
				sz = okFeatures.get(fsel);
				boolean isUnique = true;
				for(int j = i - 1; j >= 0; j--)
				{
					if(sz.inZone == vtZones[j])
					{
						isUnique = false;
						break;
					}
				}
				if(isUnique)
					break;
				else
				{
					failures++;
					if(failures == 20)
					{
						r = UltimaRando.rand() * (i - 1);
						vtZones[(int) r] = -1;
						failures = 0;
					}
				}
			}
			
			if(sz != null)  //convert a random tile on the feature to the shrine
			{
				Point fsp = null;
				//System.out.println("I found a feature for the shrine");
				System.out.print("f");
				int sp = -1;
				ArrayList<Point> pts = new ArrayList<Point>(sz.filled.size());
				pts.addAll(sz.filled);
				Rectangle sRect = null;
				do
				{
					fsp = null;
					sRect = null;
					if(sp != -1)
						pts.remove(sp);
					if(pts.size() > 0)
					{
						double r = UltimaRando.rand();
						sp = (int) (r * pts.size());
						fsp = pts.get(sp);
						sRect = new Rectangle(fsp.x - 1, fsp.y - 1, 3, 3);
						if(i == 7)
							sRect = new Rectangle(fsp.x - 2, fsp.y - 2, 5, 5);  //the shrine of humility is big
					}
					else
						break;
				} while(ensurePOIUnique(sRect, true) == false);
				//System.out.println("I found a place for my shrine");
				if(fsp == null)
				{
					okFeatures.remove(fsel);
					i--;
					System.out.print("X");
					continue;
				}
				else
				{
					System.out.print("P");
					rv.add(fsp);
					sz.isUsed = true;
					okFeatures.remove(fsel);
					vtZones[i] = sz.inZone;
				}
			}
		}		
		
		for(int i = 0; i < rv.size(); i++)  //normalize the locations
		{
			Point p = rv.get(i);
			if(p.x < 0) p.x += 256;
			if(p.x > 255) p.x -= 255;
			if(p.y < 0) p.y += 256;
			if(p.y > 255) p.y -= 255;
			//System.out.println("Placing shrine #" + i + " at " + p.x + "," + p.y);
			putTerrain(p.x, p.y, (byte) 22);  //place the shrine
			if(i == 7)  //the shrine of Humility is special and has a special surrounding area
			{
				byte[] terrs = {15, 15, 15, 15, 11, 15, 13, 22, 13, 13, 13, 13};
				int ti = 0;
				for(int y = -2; y <= 1; y++)
				{
					for(int x = -1; x <= 1; x++)
					{
						Point pp = normalizePoint(new Point(p.x + x, p.y + y));
						putTerrain(pp.x, pp.y, terrs[ti]);
						ti++;
					}
				}
			}
			else
			{
				ArrayList<Point> dps = getSurrPoints(p);
				dps.remove((int) UltimaRando.rand() * 2);
				dps.remove((int) (UltimaRando.rand() * 2) + 1);
				int ax =  0;
				int by = 0;
				for(int j = 0; j < dps.size(); j++)  //each shrine is surrounded by exactly 2 shrubs
				{
					Point p2 = dps.get(j);
					putTerrain(p2.x, p2.y, (byte) 11);
					if(j == 0)
						ax = p2.x;
					else
						by = p2.y;
				}
				putTerrain(ax, by, (byte) 10);  //and 1 plains
			}
		}
		System.out.print(";");
		return rv;
	}
	
	private ArrayList<Point> placeCastles()
	{
		int[] vtZones = new int[4];
		for(int i = 0; i < 4; i++)
			vtZones[i] = -1;
		ArrayList<Point> rv = new ArrayList<Point>(4);
		//ArrayList<MapZone> mtnZones = new ArrayList<MapZone>();
		ArrayList<MapZone> okZones = new ArrayList<MapZone>();
		for(int i = 0; i < zones.size(); i++)
		{
			MapZone cz = zones.get(i);
			if(cz.terrain == 10 || cz.terrain == 12)
				okZones.add(cz);
		}
		ArrayList<MapZone> okCopy = new ArrayList<MapZone>(okZones);
		if(okZones.size() < 4)  //there is a very small chance that there will be 3 zones
		{
			okZones.addAll(okCopy);
		}
		System.out.print("ca");
		for(int i = 0; i < 4; i++)
		{
			int failures = 0;
			MapZone cz = null;
			//System.out.println("I am placing a castle in one of " + okZones.size() + " zones");
			System.out.print(i);
			while(true)
			{
				double r = UltimaRando.rand();
				if(okZones.size() == 0)
					okZones.addAll(okCopy);
				cz = okZones.get((int) (r * okZones.size()));
				boolean unique = true;
				break;
				/*for(int j = i - 1; j >= 0; j--)
				{
					if(cz.index == vtZones[j])
					{
						unique = false;
						break;
					}
				}
				if(unique)
					break;
				else
				{
					failures++;
					if(failures == 20)
					{
						r = UltimaRando.rand();
						vtZones[(int) (r * (i - 1))] = -1;
						failures = 0;
					}
				}*/
			}
			//System.out.println("I found a unique zone (" + cz.index + ")" + vtZones);
			System.out.print("z" + cz.index);
			//find a zone - water anchor point
			int rx = (int) ((UltimaRando.rand() * 50) - 25);
			int ry = (int) ((UltimaRando.rand() * 50) - 25);
			Point pc = new Point(cz.cx + rx, cz.cy + ry);  //inject a little random
			pc = normalizePoint(pc);
			Point pa = null;
			boolean[] v = new boolean[mapData.length];
			if(mapData[pc.y * 256 + pc.x] >= 10)
			{
				ArrayList<Point> toSea = new ArrayList<Point>();
				toSea.add(pc);
				int point = 0;
				v[pc.y * 256 + pc.x] = true;
				while(true)
				{
					Point p = toSea.get(point);
					if(mapData[p.y * 256 + p.x] < 10)
						break;
					ArrayList<Point> ta = getSurrPoints(p);
					for(int j = 0; j < ta.size(); j++)
					{
						Point tj = ta.get(j);
						if(v[tj.y * 256 + tj.x] == false)
						{//if(!toSea.contains(ta.get(j)))
							toSea.add(ta.get(j));
							v[tj.y * 256 + tj.x] = true;
						}
					}
					point++;
				}
				ArrayList<Point> landAnchor = getSurrPoints(toSea.get(point));  //point is a sea point
				for(int k = 0; k < 4; k++)
				{
					Point p = landAnchor.get(k);
					if(mapData[p.y * 256 + p.x] >= 10)
					{
						pa = p;
						//initPoss |= ( 1 << i);
						//initDir = i;
						break;
					}
				}
			}
			else
			{
				ArrayList<Point> toLand = new ArrayList<Point>();
				toLand.add(pc);
				int point = 0;
				v[pc.y * 256 + pc.x] = true;
				while(true)
				{
					Point p = toLand.get(point);
					if(mapData[p.y * 256 + p.x] >= 10)
						break;
					ArrayList<Point> ta = getSurrPoints(p);
					for(int j = 0; j < ta.size(); j++)
					{
						Point tj = ta.get(j);
						if(v[tj.y * 256 + tj.x] == false)
						{//if(!toSea.contains(ta.get(j)))
							toLand.add(ta.get(j));
							v[tj.y * 256 + tj.x] = true;
						}
					}
					point++;
				}
				//ArrayList<Point> landAnchor = getSurrPoints(toLand.get(point));  //this is a land point
				pa = toLand.get(point);
			}
			//System.out.println("I found a sea anchor point");
			System.out.print("a");
			//now do a "knight's move" to the place with the least amount of water
			Point cLoc = null;
			int fails = 0;
			do
			{
				fails++;
				if(fails > 30)
					break;
				int dx = (int) (UltimaRando.rand() * 4) + 1;
				int dy = (int) (UltimaRando.rand() * 4) + 1;
				ArrayList<Integer> vals = new ArrayList<Integer>(4);
				ArrayList<Point> pts = new ArrayList<Point>(4);
				int good;
				/*int bestDx = 0;
				int bestDy = 0;*/
			
				for(int k = -1; k <= 1; k += 2)
				{
					for(int l = -1; l <= 1; l += 2)
					{
						good = 0;
						for(int m = 0; m < 4; m++)
						{
							for(int n = 0; n < 4; n++)
							{
								Point pt = new Point(pa.x + (dx * l) + n, pa.y + (dy * k) + m);
								if(getTerrain(pt.x, pt.y) >= 10)
									good++;
							}
						}
						int idx = 0;
						for(int m = 0; m < vals.size(); m++)
						{
							if(good > vals.get(m))
								break;
							else
								idx++;
						}
						vals.add(idx, good);
						pts.add(idx, new Point(pa.x + (dx * l), pa.y + (dy * k)));
					}
				}
				for(int k = 0; k < pts.size(); k++)
				{
					cLoc = pts.get(k);
					if(ensurePOIUnique(new Rectangle(cLoc.x, cLoc.y, 4, 4), false) == true)
						break;
				}
			}while(ensurePOIUnique(new Rectangle(cLoc.x, cLoc.y, 4, 4), true) == false);
			if(fails > 30)
			{
				i--;
				okZones.remove(cz);
				continue;
			}
			//System.out.println("I found the castle spot");
			System.out.print("P");
			rv.add(cLoc);
			vtZones[i] = cz.index;
			okZones.remove(cz);
		}
		for(int i = 0; i < 4; i++)
		{
			Point p = rv.get(i);
			p = normalizePoint(p);
			rv.set(i, p);
			//System.out.println("Placing castle #" + i + " at " + p.x + "," + p.y + " in zone #" + vtZones[i]);
			for(int yy = 0; yy < 4; yy++)
			{
				for(int xx = 0; xx < 4; xx++)
				{
					if(xx == 0 || yy == 0 || xx == 3 || yy == 3)
						putTerrain(p.x + xx, p.y + yy, (byte) 10);
					else if(yy == 1)
						putTerrain(p.x + xx, p.y + yy, (byte) 26);
					else if(xx == 1)
						putTerrain(p.x + xx, p.y + yy, (byte) 27);
					else
						putTerrain(p.x + xx, p.y + yy, (byte) 28);
				}
			}
		}
		Point brit = rv.get(1);
		britBalloon = new Point(brit.x + 1, brit.y + 3);
		britBalloon = normalizePoint(britBalloon);
		System.out.print(";");
		return rv;
	}
	
	private ArrayList<MapZone> makeFeatures(int forestZones, int mtnZones)
	{
		int nFeatures = (int) ((UltimaRando.rand() * 25) + 50);
		ArrayList<Rectangle> features = new ArrayList<Rectangle>(nFeatures);
				//noOlapRects(50, 75, 12, 30, 1);
		ArrayList<MapZone> rv = new ArrayList<MapZone>(nFeatures);
		double[] landFeatures = {0.2, 0.4, 0.6, 0.7, 0.9, 1.0};  //shrub, forest, rdelta, mountain, swamp, lake
		int[] reqLandF = {2, 2 - 2 * forestZones, 8, 4 - mtnZones, 3, 1};
		double[] waterFeatures = {0.3, 0.6, 0.8, 1.0}; //island, archipelago, volc island, shoal
		int reqWatrF = 3;
		int waterF = 0;
		//int reqMountains = 3 - mtnZones;
		//int placedMountains = 0;
		//System.out.println("Attempting to place " + nFeatures + " features");
		for(int i = 0; i < nFeatures; i++)
		{
			if(waterF > reqWatrF)
				features.add(noOlapRect(rv, 12, 30, 1, false));
			else
				features.add(noOlapRect(rv, 12, 30, 0, false));
			MapZone mz = new MapZone(features.get(i), (byte) 0, -1);
			double ft = UltimaRando.rand();
			if(mz.majorityIsLand())
			{
				boolean randomFeature = true;
				for(int j = 0; j < reqLandF.length; j++)
				{
					if(reqLandF[j] > 0)
					{
						mz.placeFeature(j);
						if(mz.area != null)
							reqLandF[j]--;
						/*else
						{
							i--;
							features.remove(features.size() - 1);
						}*/
						randomFeature = false;
						break;
					}
				}
				if(randomFeature)
				{
					for(int k = 0; k < landFeatures.length; k++)
					{
						if(ft < landFeatures[k])
						{
							mz.placeFeature(k);
							break;
						}
					}
				}
			}
			else
			{
				for(int k = 0; k < waterFeatures.length; k++)
				{
					waterF++;
					if(waterF <= reqWatrF)
					{
						if(mz.checkFeaturePlacement(waterF + 40))
							mz.placeFeature(waterF + 40);
						else
						{
							waterF--;
							mz.area = null;
						}
						break;
					}
					if(ft < waterFeatures[k])
					{
						mz.placeFeature(k + 6);
						break;
					}
				}
			}
			if(mz.area != null && mz.filled.size() > 0)
				rv.add(mz);
			else
			{
				i--;
				features.remove(features.size() - 1);
			}
		}
		System.out.print("F#" + rv.size());
		return rv;
	}

	public String printMapInfo() 
	{
		String s = "";
		for(int i = 0; i < zones.size(); i++)
		{
			MapZone mz = zones.get(i);
			s += "Zone #" + i + ": area=" + mz.area.toString() + " type=" + mz.terrain + ";"; 
		}
		s += "\n";
		for(int i = 0; i < features.size(); i++)
		{
			MapZone f = features.get(i);
			s += "F" + i + ": area=" + f.area.toString() + " type=" + f.terrain + ";";
		}
		s += "\n";
		return s;
	}
}

class MapPanel extends JPanel implements MouseListener, MouseMotionListener
{
	byte[] tiles;
	HashMap<Integer, Descriptor> colorHash;
	
	MapWindow ref;
	int left, right, top, bottom;
	//int absX, absY;
	
	public MapPanel(byte[] ts, HashMap<Integer, Descriptor> ch, MapWindow w)
	{
		tiles = ts;
		colorHash = ch;
		ref = w;
		setPreferredSize(new Dimension(512,512));
		addMouseListener(this);
		left = 0;
		right = left + 127;
		top = 0;
		bottom = top + 127;
		addMouseMotionListener(this);
	}
	
	public void updateCH(HashMap<Integer, Descriptor> ch)
	{
		colorHash = ch;
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 512, 512);
		for(int i = top; i <= bottom; i++)
		{
			for(int j = left; j <= right; j++)
			{
				int t = tiles[(i << 8) + j] & 63;
				/*if(t < 0)
				{
					System.out.println("WTF");
				}
				if(t > 255)
					System.out.println("WTF");*/
				Color c;
				Descriptor dd = colorHash.get(t);
				if(dd == null)
					c = new Color(t,t,t);
				else
					c = dd.c;
				g.setColor(c);
				g.fillRect((j - left) << 2, (i - top) << 2, 4, 4);
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		// TODO Auto-generated method stub
		int x = (e.getX() >> 2) + left;
		int y = (e.getY() >> 2) + top;
		byte t = tiles[(y << 8) + x];
		ref.selectTile(t, x, y);
		//System.out.println("X:" + x + " Y:" + y + " T:" + t);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) 
	{
		if(e.getX() < 60 && left > 0)
		{
			left--;
			right--;
		}
		if(e.getX() > 452 && right < 255)
		{
			left++;
			right++;
		}
		if(e.getY() < 60 && top > 0)
		{
			top--;
			bottom--;
		}
		if(e.getY() > 452 && bottom < 255)
		{
			top++;
			bottom++;
		}
		
	}
}

class SidePanel extends JPanel implements MouseListener
{
	HashMap<Integer, Descriptor> colorHash;
	String sideString;
	byte setVal;
	
	MapWindow ref;
	
	public SidePanel(HashMap<Integer, Descriptor> ch, MapWindow w)
	{
		colorHash = ch;
		sideString = "";
		ref = w;
		setVal = 0;
		setPreferredSize(new Dimension(300,512));
		addMouseListener(this);
	}
	
	public void updateCH(HashMap<Integer, Descriptor> ch)
	{
		colorHash = ch;
	}
	
	public void setSideString(String s, byte v)
	{
		sideString = s;
		setVal = v;
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 300, 512);
		int x = 5;
		int y = 10;
		Set<Integer> ss = colorHash.keySet();
		Integer[] arr = new Integer[ss.size()];
		arr = ss.toArray(arr);
		int j = 0;
		for(int i = 0; i < arr.length; i++)
		{
			Descriptor desc = colorHash.get(arr[i]);
			g.setColor(desc.c);
			g.fillRect(x, y, 10, 16);
			g.drawString(desc.desc, x + 12, y + 16);
			if(j < 2)
			{
				x += 100;
				j++;
			}
			else
			{
				y += 18;
				x = 5;
				j = 0;
			}
		}
		g.setColor(Color.BLACK);
		g.drawRect(x, y, 10, 16);
		g.drawString("New Tile", x + 12, y + 16);
		g.drawString(sideString, 10, 460);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		// TODO Auto-generated method stub
		if(setVal != 0)
			ref.addColor(setVal);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}

class MapWindow extends JFrame implements ActionListener
{
	HashMap<Integer, Descriptor> colorMap;
	byte[] mapData;
	byte[] accessData;
	SidePanel sp;
	MapPanel mp;
	Color[] colors;
	ArrayList<Rectangle> poi;
	ArrayList<POI> poiList;
	
	Timer tim;
	private byte[] visData;
	
	MapWindow(byte[] md, boolean useOrigData)
	{
		mapData = md;
		colorMap = new HashMap<Integer, Descriptor>();
		if(useOrigData)
			initDescriptors();
		else
			initOutputDescriptors();
		sp = new SidePanel(colorMap, this);
		mp = new MapPanel(md, colorMap, this);
		getContentPane().add(sp, BorderLayout.WEST);
		getContentPane().add(mp, BorderLayout.EAST);
		Color[] cc = {Color.BLUE, Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.RED, Color.ORANGE, Color.GREEN};
		colors = cc;
		tim = new Timer(100, this);
		tim.start();
		setSize(850, 600);
		setVisible(true);
	}
	
	public void setPOIData(ArrayList<Rectangle> poi) 
	{
		// TODO Auto-generated method stub
		this.poi = poi;
	}
	
	public void setMoongates(ArrayList<Point> mgs)
	{
		for(int i = 0; i < mgs.size(); i++)
		{
			Point p = mgs.get(i);
			mapData[p.y * 256 + p.x] = 0;
		}
	}
	
	public boolean testPOI(int x, int y)
	{
		Point p = new Point(x, y);
		//normalizePoint(p);
		for(int j = 0; j < poi.size(); j++)
		{
			Rectangle rr = poi.get(j);
			if(rectContains(rr, p))
			{
				//System.out.println("Collided with " + rr.toString() + " on test 1");
				return true;
			}
		}
		return false;
	}

	private boolean rectContains(Rectangle r, Point p) 
	{
		if(p.x >= r.x && p.x < r.x + r.width && p.y >= r.y && p.y < r.y + r.height)
			return true;
		else
			return false;
	}

	public void setAccessData(byte[] ad)
	{
		accessData = ad;
	}
	
	public void setVisibilityData(byte[] vd)
	{
		visData = vd;
	}
	
	public void initOutputDescriptors()
	{
		colorMap.put(1, new Descriptor(new Color(0,0,127), "Deep Water"));
		colorMap.put(3, new Descriptor(new Color(0,0,255), "Water"));
		colorMap.put(7, new Descriptor(new Color(127,127,255), "Shoal"));
		colorMap.put(10, new Descriptor(new Color(0,255,0), "Plains"));
		colorMap.put(11, new Descriptor(new Color(127,255,127), "Shrub"));
		colorMap.put(12, new Descriptor(new Color(0,127,0), "Forest"));
		colorMap.put(13, new Descriptor(new Color(127,127,0), "Mountain"));
		colorMap.put(14, new Descriptor(new Color(127,0,127), "Poison"));
		colorMap.put(15, new Descriptor(new Color(255,255,0), "Brick"));
		colorMap.put(16, new Descriptor(new Color(255,0,0), "Lava"));
		colorMap.put(17, new Descriptor(new Color(128,0,0), "Crater"));
		colorMap.put(20, new Descriptor(new Color(255,0,255), "VTown"));
		colorMap.put(21, new Descriptor(new Color(255,0,255), "Dungeon"));
		colorMap.put(22, new Descriptor(new Color(255,127,255), "Shrine"));
		colorMap.put(26, new Descriptor(new Color(255,0,255), "Castle Flag"));
		colorMap.put(27, new Descriptor(new Color(255,0,255), "Castle L"));
		colorMap.put(28, new Descriptor(new Color(255,0,255), "Castle R"));
		colorMap.put(30, new Descriptor(new Color(127,255,255), "Moongate"));
		colorMap.put(29, new Descriptor(new Color(0,0,0), "Abyss"));
		colorMap.put(31, new Descriptor(new Color(255,0,0), "Balloon"));
		colorMap.put(32, new Descriptor(new Color(0,0,0), "Whirlpool"));
	}
	
	public void initDescriptors()
	{
		colorMap.put(0, new Descriptor(new Color(255,255,255), "Moongate"));
		colorMap.put(7, new Descriptor(new Color(0,0,127),"Deep Water"));
		//colorMap.put(71, new Descriptor(new Color(0,0,127),"Hidden Deep Water"));
		colorMap.put(36, new Descriptor(new Color(0,0,255), "Water"));
		colorMap.put(15, new Descriptor(new Color(127,127,255), "Shoal"));
		colorMap.put(11, new Descriptor(new Color(0,255,0), "Plains W"));
		colorMap.put(3, new Descriptor(new Color(0,255,0), "Plains NW"));
		colorMap.put(5, new Descriptor(new Color(0,255,0), "Plains NE"));
		colorMap.put(13, new Descriptor(new Color(0,255,0), "Plains E"));
		colorMap.put(20, new Descriptor(new Color(0,255,0), "Plains S"));
		colorMap.put(19, new Descriptor(new Color(0,255,0), "Plains SW"));
		colorMap.put(21, new Descriptor(new Color(0,255,0), "Plains SE"));
		colorMap.put(4, new Descriptor(new Color(0,255,0), "Plains N"));
		colorMap.put(30, new Descriptor(new Color(0,255,0), "Plains NS"));
		colorMap.put(12, new Descriptor(new Color(0,255,0), "Plains"));
		colorMap.put(31, new Descriptor(new Color(0,255,0), "Plains S 3side"));
		
		colorMap.put(23, new Descriptor(new Color(127,0,127), "Poison"));
		colorMap.put(9, new Descriptor(new Color(127,255,127), "Shrub"));
		colorMap.put(1, new Descriptor(new Color(127,255,127), "Shrub N"));
		colorMap.put(58, new Descriptor(new Color(127,255,127), "Shrub NW"));
		colorMap.put(2, new Descriptor(new Color(127,255,127), "Shrub NE"));
		colorMap.put(18, new Descriptor(new Color(127,255,127), "Shrub SE"));
		colorMap.put(10, new Descriptor(new Color(127,255,127), "Shrub E"));
		colorMap.put(8, new Descriptor(new Color(127,255,127), "Shrub W"));
		colorMap.put(16, new Descriptor(new Color(127,255,127), "Shrub SW"));
		colorMap.put(17, new Descriptor(new Color(127,255,127), "Shrub S"));
		
		
		//colorMap.put(123, new Descriptor(new Color(0,127,0), "Forest"));
		//colorMap.put(187, new Descriptor(new Color(0,127,0), "Forest"));
		//colorMap.put(251, new Descriptor(new Color(0,127,0), "Forest"));
		//colorMap.put(113, new Descriptor(new Color(127,127,0), "Mountain"));
		colorMap.put(61, new Descriptor(new Color(127,127,0), "N Edge Mountain"));
		colorMap.put(60, new Descriptor(new Color(127,127,0), "S Edge Mountain"));
		colorMap.put(62, new Descriptor(new Color(127,127,0), "Loose Mountain"));
		colorMap.put(35, new Descriptor(new Color(0,0,255), "Water W Shoal"));
		colorMap.put(44, new Descriptor(new Color(0,0,255), "Water S Shoal"));
		colorMap.put(37, new Descriptor(new Color(0,0,255), "Water E Shoal"));
		colorMap.put(28, new Descriptor(new Color(0,0,255), "Water N Shoal"));
		colorMap.put(29, new Descriptor(new Color(0,0,255), "Water NE Shoal"));
		colorMap.put(27, new Descriptor(new Color(0,0,255), "Water NW Shoal"));
		colorMap.put(43, new Descriptor(new Color(0,0,255), "Water SW Shoal"));
		colorMap.put(45, new Descriptor(new Color(0,0,255), "Water SE Shoal"));
		colorMap.put(39, new Descriptor(new Color(0,0,255), "Water NS Shoal"));
		colorMap.put(38, new Descriptor(new Color(0,0,255), "Water EW Shoal"));
		colorMap.put(48, new Descriptor(new Color(0,0,255), "Water N 3side"));
		colorMap.put(22, new Descriptor(new Color(0,128,0), "Forest Entry"));
		colorMap.put(14, new Descriptor(new Color(0,128,0), "Forest Edge"));
		colorMap.put(59, new Descriptor(new Color(0,128,0), "Forest"));
		//colorMap.put(164, new Descriptor(new Color(0,0,255), "Lagoon"));
		colorMap.put(56, new Descriptor(new Color(255,0,255), "Castle W"));
		colorMap.put(57, new Descriptor(new Color(255,0,255), "Castle E"));
		colorMap.put(55, new Descriptor(new Color(255,0,255), "Castle Flag"));
		colorMap.put(52, new Descriptor(new Color(255,0,255), "Town"));
		colorMap.put(54, new Descriptor(new Color(255,0,255), "Shrine"));
		colorMap.put(6, new Descriptor(new Color(255,0,255), "Bridge"));
		colorMap.put(53, new Descriptor(new Color(255,0,255), "Dungeon"));
		
		colorMap.put(33, new Descriptor(new Color(255,255,0), "Brick"));
		colorMap.put(24, new Descriptor(new Color(255,255,0), "Brick NW"));
		colorMap.put(25, new Descriptor(new Color(255,255,0), "Brick N"));
		colorMap.put(26, new Descriptor(new Color(255,255,0), "Brick NE"));
		colorMap.put(32, new Descriptor(new Color(255,255,0), "Brick W"));
		colorMap.put(34, new Descriptor(new Color(255,255,0), "Brick E"));
		colorMap.put(40, new Descriptor(new Color(255,255,0), "Brick SW"));
		colorMap.put(41, new Descriptor(new Color(255,255,0), "Brick S"));
		colorMap.put(42, new Descriptor(new Color(255,255,0), "Brick SE"));
		colorMap.put(46, new Descriptor(new Color(255,255,0), "Brick E 3side"));
		colorMap.put(47, new Descriptor(new Color(255,255,0), "Brick W 3side"));
		colorMap.put(63, new Descriptor(new Color(255,255,0), "Brick S 3side"));
		
		colorMap.put(51, new Descriptor(new Color(255,0,0), "Lava"));
		colorMap.put(50, new Descriptor(new Color(128,0,0), "Crater"));
		
		//colorMap.put(241, new Descriptor(new Color(127,127,0), "Tall Mountain"));
		//colorMap.put(177, new Descriptor(new Color(127,127,0), "Short Mountain"));
		colorMap.put(49, new Descriptor(new Color(127,127,0), "Mountain"));
		
	}
	
	public void addColor(byte forVal)
	{
		int ii = forVal;
		int k = colorMap.keySet().size();
		Descriptor dd = new Descriptor(colors[k], ("#" + k));
		colorMap.put(ii, dd);
		sp.updateCH(colorMap);
		mp.updateCH(colorMap);
	}
	
	public void setPOIList(ArrayList<POI> lst)
	{
		poiList = lst;
	}
	
	public void selectTile(byte val, int x, int y)
	{
		String s = "(" + x + "," + y + "):";
		Integer ii = Integer.valueOf(val) & 63;
		s += Integer.toHexString(ii) + "(" + String.valueOf(ii) + ") - ";
		
		int vis = (val >> 6) & 3;
		if(colorMap.containsKey(ii))
		{
			s += colorMap.get(ii).desc;
		}
		else
			s += "??";
		if(accessData != null)
		{
			s += " Access at (" + x + "," + y + "):" + accessData[256 * y + x]; 
		}
		if(poi != null)
		{
			s += " POI:" + testPOI(x, y);
		}
		if(visData != null)
		{
			byte vd = visData[256 * y + x];
			byte va = vd;
			vd--;
			vd &= 3;
			s += "  VZ:" + va + " (" + vd + ")";
		}
		else
			s += " V" + vis;
		if(poiList != null)
		{
			int[] vals = {52, 54, 56, 57, 53};
			for(int j = 0; j < vals.length; j++)
			{
				if(ii == vals[j])
				{
					for(int i = 0; i < poiList.size(); i++)
					{
						POI poi = poiList.get(i);
						if(poi.p.x == x && poi.p.y == y)
						{
							s += " : " + poi.desc;
							break;
						}
					}
					break;
				}
			}
		}
		sp.setSideString(s, val);
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		repaint();
		sp.repaint();
		mp.repaint();
	}
	
}

//this is all the UI junk
class InputLinePanel extends JPanel implements ActionListener
{
	JLabel lbl;
	JTextField txt;
	JButton btn;
	RandoWindow rw;
	
	InputLinePanel(String labelText, String defaultText, String buttonText, RandoWindow rw)
	{
		lbl = new JLabel(labelText);
		txt = new JTextField(defaultText, 25);
		//txt.setColumns(25);
		btn = new JButton(buttonText);
		this.rw = rw;
		
		setLayout(new FlowLayout());
		add(lbl);
		add(txt);
		add(btn);
		btn.addActionListener(this);
	}
	
	public void setTextText(String s)
	{
		txt.setText(s);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		rw.execute(this);
	}
}

class FlagPanel extends JPanel implements ActionListener
{
	String[] flags;
	JCheckBox[] checks;
	boolean multiSelect;
	boolean bitField;
	String hybridFlag;
	RandoWindow rw;
	
	FlagPanel(String title, String[] options, String[] flags, boolean multiSelect, boolean bitField, RandoWindow rw)
	{
		setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		setLayout(new GridLayout(options.length, 1));
		checks = new JCheckBox[options.length];
		for(int i = 0; i < options.length; i++)
		{
			checks[i] = new JCheckBox(options[i]);
			checks[i].addActionListener(this);
			add(checks[i]);
		}
		this.flags = flags;
		if(bitField)
		{
			this.bitField = true;
			hybridFlag = flags[0].charAt(0) + "0";
		}
		else
			this.bitField = false;
		this.multiSelect = multiSelect;
		this.rw = rw;
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		int i = 0;
		for(; i < checks.length; i++)
			if(e.getSource() == checks[i])
				break;
		if(bitField)
		{
			rw.clearFlag(hybridFlag);
			int hv = Integer.parseInt(String.valueOf(hybridFlag.charAt(1)));
			int n = Integer.parseInt(String.valueOf(flags[i].charAt(1)));
			if(checks[i].isSelected())
				hv += n;
			else
			{
				hv -= n;
				if(n == 0)  //if you delesect the 0 flag
					return;
				if(hv == 0 && !flags[0].endsWith("0"))  //if you set the value to 0 and there is no 0 flag
					return;
			}
			hybridFlag = String.valueOf(hybridFlag.charAt(0)) + hv;
			rw.setFlag(hybridFlag);
		}
		else
		{
			if(checks[i].isSelected())
			{
				rw.setFlag(flags[i]);
				if(!multiSelect)
				{
					for(int j = 0; j < checks.length; j++)
					{
						if(j == i)
							continue;
						if(checks[j].isSelected())
						{
							checks[j].setSelected(false);
							rw.clearFlag(flags[j]);
						}
					}
				}
			}
			else
			{
				rw.clearFlag(flags[i]);
			}
		}
	}

	public void preSelectFlags(String initflags) 
	{
		for(int i = 0; i < flags.length; i++)
		{
			if(initflags.indexOf(flags[i]) > 0)
				checks[i].setSelected(true);
		}
	}
}

class RandoWindow extends JFrame implements ActionListener
{
	InputLinePanel[] inputLines;
	JPanel topPane;
	boolean threadRunning;
	RandomizerThread rt;
	Timer tim;
	JFileChooser jfc;
	JTabbedPane jtp;
	ArrayList<FlagPanel> opts;
	//ArrayList<JCheckBox> checks;
	
	class RandomizerThread extends Thread
	{
		String failure;
		
		RandomizerThread()
		{
			
		}
		
		public void run()
		{
			NESRom rom = null;
			NESRom nr = null;
			String flags = inputLines[2].txt.getText();
			String dir = System.getProperty("user.dir") + File.separator;
			String outFile = dir + inputLines[3].txt.getText();
			String fname = inputLines[1].txt.getText();
			try
			{
				rom = new NESRom(fname);
			}
			catch(Exception ex)
			{
				tim.stop();
				failure += fname + "\n was not found\n";
				JOptionPane.showMessageDialog(RandoWindow.this, failure, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			try
			{
				nr = rom.randomize(flags, outFile);
				outFile = nr.filename;
			}
			catch(Exception ex)
			{
				tim.stop();
				System.err.println(ex.getMessage());
				ex.printStackTrace();
				failure += "This seed failed to successfully generate a ROM.\n";
				JOptionPane.showMessageDialog(RandoWindow.this, failure, "Sorry", JOptionPane.ERROR_MESSAGE);
				return;
			}
			//
			tim.stop();
			saveConfig();
			rt = null;
			//System.out.println(nr.getFinalSearchLocData());
			System.out.println(nr.getODLocations());
			MapWindow mw = new MapWindow(UltimaRando.combineMap(nr.getPotMap()), true);
			mw.setPOIList(nr.getPOIList());
			mw.setMoongates(nr.getMoongates());
			//System.out.println(nr.getInitCharData());
			JOptionPane.showMessageDialog(RandoWindow.this, outFile + "\nhas been successfully generated.", "Done", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	RandoWindow()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
		}
		
		String[] labels = {"Seed ", "Source ROM:", "Flags", "Output ROM:"};
		String[] texts = {String.valueOf((long) (UltimaRando.rand() * Long.MAX_VALUE)), "", "", ""};
		String[] buttons = {"Random Seed", "Find...", "Clear", "Make ROM"};
		
		inputLines = new InputLinePanel[labels.length];
		topPane = new JPanel();
		topPane.setLayout(new GridLayout(2,2));
		
		for(int i = 0; i < inputLines.length; i++)
		{
			inputLines[i] = new InputLinePanel(labels[i], texts[i], buttons[i], this);
			topPane.add(inputLines[i]);
		}
		
		loadConfig();
		
		createOptionPanes(inputLines[2].txt.getText());
		initFlags(inputLines[2].txt.getText());
		getContentPane().add(topPane, BorderLayout.NORTH);
		getContentPane().add(jtp, BorderLayout.CENTER);
		setTitle("Ultima: Quest of the Avatar Randomizer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(100, 100);
		setSize(800, 600);
		setResizable(false);
		setVisible(true);
	}
	
	private void initFlags(String fl) 
	{
		String currFlag = "";
		for(int i = 0; i < fl.length(); i++)
		{
			char ch = fl.charAt(i);
			if(Character.isAlphabetic(ch))
			{
				if(currFlag.length() > 0)
				{
					initFlag(currFlag);
					currFlag = "";
				}
				currFlag += ch;
			}
			else
			{
				currFlag += ch;
				initFlag(currFlag);
				currFlag = "";
			}
		}
	}
		
	private void initFlag(String fl)
	{
		char a = fl.charAt(0);
		for(int i = 0; i < opts.size(); i++)
		{
			FlagPanel fp = opts.get(i);
			if(!fp.flags[0].startsWith("" + a) && fl.length() == 2)
				continue;
			if(fp.bitField == true && fl.length() == 2)
			{
				int val = Integer.parseInt("" + fl.charAt(1));
				for(int j = 0; j < fp.flags.length; j++)
				{
					String s = fp.flags[j];
					int val2 = Integer.parseInt("" + s.charAt(1));
					if((val & val2) > 0 || val2 == 0)
						fp.checks[j].setSelected(true);
					fp.hybridFlag = "" + s.charAt(0) + val;
				}
			}
			else
			{
				for(int j = 0; j < fp.flags.length; j++)
				{
					if(fp.flags[j].equals(fl))
					{
						fp.checks[j].setSelected(true);
						return;
					}
				}
			}
		}
	}

	private String loadTextFile(String tf)
	{
		String currDir = System.getProperty("user.dir");
		String path = currDir + File.separator + tf;
		File f = new File(path);
		String rv = "";
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			while(line != null)
			{
				rv += line + "\n";
				line = br.readLine();
			}
			br.close();
			return rv;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
			return null;
		}
	}
	
	private void saveConfig()
	{
		String c1 = "[Source ROM]=" + inputLines[1].txt.getText();
		String c2 = "[Last Flags]=" + inputLines[2].txt.getText();
		String config = c1 + "\n" + c2 + "\n";
		String currDir = System.getProperty("user.dir");
		String path = currDir + File.separator + "U4RandoConfig.txt";
		File f = new File(path);
		try
		{
			if(!f.exists())
				f.createNewFile();
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(config);
			bw.close();
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		
	}
	
	private String loadFAQ()
	{
		return loadTextFile("U4RandoFAQ.txt");
	}
	
	private void loadConfig()
	{
		String s = loadTextFile("U4RandoConfig.txt");
		if(s != null)
		{
			String[] a = s.split("\n");
			String rom = a[0].substring(a[0].indexOf("=") + 1);
			inputLines[1].setTextText(rom);
			String fl = a[1].substring(a[1].indexOf("=") + 1);
			inputLines[2].setTextText(fl);
			updateSaveName();
		}
	}
	
	private void createOptionPanes(String initflags) 
	{
		opts = new ArrayList<FlagPanel>();
		String[] optM = new String[10];
		String[] flM = new String[10];
		for(int i = 0; i < 10; i++)
		{
			optM[i] = "With " + String.valueOf(i * 5) + " tiles of sea on each border";
			flM[i] = "M" + i;
		}
		opts.add(new FlagPanel("Randomize overworld map", optM, flM, false, false, this));
		String[] opt = {"Randomize Dungeon Layouts", 
				 "Randomize Moongate Destinations", "Put Balloon next to Castle Britannia",
				 "Randomize spells learned by Spell Teaching Villagers", 
				 "Randomize Enemy Abilities", "Modify the View spell"};
		String[] fl = {"D", "O", "B", "T", "A", "W"};
		opts.add(new FlagPanel("General", opt, fl, true, false, this));
		String[] shopt = {"Shuffle Shop Locations", "Enforce Shop Non-Compete", "Preserve Shop Types"};
		String[] shfl = {"P0", "P1", "P2"};
		opts.add(new FlagPanel("Randomize Shop Locations", shopt, shfl, false, false, this));
		String[] abyssM = new String[8];
		String[] abyssF = new String[8];
		for(int i = 1; i < 9; i++)
		{
			abyssM[i - 1] = "Start the Abyss at Floor " + i;
			abyssF[i - 1] = "Y" + i;
		}
		opts.add(new FlagPanel("Shorten Abyss", abyssM, abyssF, false, false, this));
		
		String[] op2 = {"Randomize Items at Search Locations", "Exclude the Fungus and Manroot search spots (recommended)",
				"Include Avatar Equipment in shuffle", "Switch Avatar Equipment with a random Sword and Armor"};
		String[] fla2 = {"R0", "R1", "R2", "R4"};
		opts.add(new FlagPanel("Randomize Search Spot Items", op2, fla2, true, true, this));
		String[] optE = {"Make an additional 25% of enemies tough", "Make an additional 50% of enemies tough",
				         "Make all dungeon enemies tough"};
		String[] fle = {"E1", "E2", "E4"};
		opts.add(new FlagPanel("Tough Enemies", optE, fle, true, true, this));
		String[] optC = new String[6];
		String[] flaC = new String[6];
		for(int i = 1; i < 7; i++)
		{
			optC[i - 1] = "Add " + (25 * i) + " gold to each chest with gold";
			flaC[i - 1] = "C" + i;
		}
		opts.add(new FlagPanel("Gold Chests", optC, flaC, false, false, this));
		String[] opt2 = {"0 Gold", "0-400 Gold", "0-1000 Gold"};
		String[] fl2 = {"G0", "G1", "G2"};
		opts.add(new FlagPanel("Gold", opt2, fl2, false, false, this));
		String[] op5 = {"Shuffle", "Random"};
		String[] fl5 = {"I1", "I2"};
		opts.add(new FlagPanel("Starting Items", op5, fl5, false, false, this));
		String[] op3 = {"0 Virtue", "Random Virtue", "Worthy of Avatarhood", "Start as Avatar"};
		String[] fl3 = {"V0", "V1", "V2", "V3"};
		opts.add(new FlagPanel("Virtue", op3, fl3, false, false, this));
		String[] op4 = {"Shuffle", "Random"};
		String[] fl4 = {"Q1", "Q2"};
		opts.add(new FlagPanel("Equippable Items", op4, fl4, false, false, this));
		
		String[] op6 = {"Shuffle Amounts", "Random 0-9"};
		String[] fl6 = {"H1", "H2"};
		opts.add(new FlagPanel("Starting Herbs", op6, fl6, false, false, this));
		String[] op7 = {"Randomly select known spells", "Remove Heal", "Remove Blink"};
		String[] fl7 = {"S1", "S2", "S4"};
		opts.add(new FlagPanel("Spells", op7, fl7, true, true, this));
		for(int i = 0; i < opts.size(); i++)
		{
			opts.get(i).preSelectFlags(initflags);
		}
		
		JPanel general = new JPanel();
		general.setLayout(new GridLayout(2, 2));
		general.add(opts.get(0));
		
		JPanel gen0 = new JPanel(new BorderLayout());
		gen0.add(opts.get(1), BorderLayout.CENTER);
		gen0.add(opts.get(2), BorderLayout.SOUTH);
		general.add(gen0);
		
		general.add(opts.get(3));
		JPanel gen1 = new JPanel();
		gen1.setLayout(new GridLayout(2, 1));
		for(int i = 4; i < 6; i++)
			gen1.add(opts.get(i));
		/*JPanel gen2 = new JPanel();
		gen2.setLayout(new GridLayout(2, 1));
		for(int i = 2; i < 5; i++)
			gen2.add(opts.get(i));*/
		general.add(gen1);
		//general.add(gen2);
		
		JPanel character = new JPanel();
		character.setLayout(new GridLayout(1, 2));
		JPanel char1 = new JPanel(new GridLayout(2, 1));
		char1.add(opts.get(6));
		JPanel char1B = new JPanel(new GridLayout(2, 1));
		char1B.add(opts.get(7));
		char1B.add(opts.get(8));
		char1.add(char1B);
		JPanel char2 = new JPanel(new GridLayout(4, 1));
		for(int i = 9; i < opts.size(); i++)
			char2.add(opts.get(i));
		character.add(char1);
		character.add(char2);
		
		JPanel faq = new JPanel();
		JTextArea jta = new JTextArea(loadFAQ());
		jta.setColumns(80);
		jta.setRows(25);
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);
		JScrollPane jsp = new JScrollPane(jta);
		faq.add(jsp);
		
		jtp = new JTabbedPane();
		jtp.add("General", general);
		jtp.add("Characters", character);
		jtp.add("FAQ", faq);
	}

	public void clearFlag(String ff) 
	{
		String s = inputLines[2].txt.getText();
		int idx = s.indexOf(ff);
		if(idx > -1)
		{
			String s1 = s.substring(0, idx);
			String s2 = s.substring(idx + ff.length(), s.length());
			inputLines[2].setTextText(s1 + s2);
			updateSaveName();
		}
	}

	public void setFlag(String ff) 
	{
		String s = inputLines[2].txt.getText();
		if(s.indexOf(ff) == -1)
		{
			s += ff;
			inputLines[2].setTextText(s);
			updateSaveName();
		}
	}
	
	private void updateSaveName()
	{
		String sv = "U4Rando." + inputLines[0].txt.getText() + "." + inputLines[2].txt.getText() + ".nes";
		inputLines[3].setTextText(sv);
	}

	public void execute(InputLinePanel src)
	{
		int i = 0;
		for(; i < inputLines.length; i++)
			if(src == inputLines[i])
				break;
		
		switch(i)
		{
		case 0:
			inputLines[i].setTextText(String.valueOf((long) (UltimaRando.rand() * Long.MAX_VALUE)));
			updateSaveName();
			break;
		case 1:
			//select file
			jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) 
            {
                File file = jfc.getSelectedFile();
                inputLines[i].setTextText(file.getAbsolutePath());
            }
			break;
		case 2:
			//inputLines[i].setTextText("");
			//clear options on current panel
			clearAllOptions();
			inputLines[2].setTextText("");
			updateSaveName();
			break;
		case 3:
			//String errorMsg = "";
			String s1 = inputLines[0].txt.getText();
			try
			{
				Long.parseLong(s1);
			}
			catch(Exception ex)
			{
				JOptionPane.showMessageDialog(this, "Seed must be a number", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			long seed = Long.parseLong(s1);
			UltimaRando.setSeed(seed);
			rt = new RandomizerThread();
			tim = new Timer(10000, this);
			//begin the race
			rt.start();
			tim.start();
			break;
		}
	}

	private void clearAllOptions() 
	{
		for(int i = 0; i < opts.size(); i++)
		{
			FlagPanel fp = opts.get(i);
			for(int j = 0; j < fp.checks.length; j++)
				fp.checks[j].setSelected(false);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if(rt != null)
		{
			rt.interrupt();
			rt = null;
			JOptionPane.showMessageDialog(this, "This seed timed out when generating a ROM.\nTry a different seed.", "Sorry", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
}



public class UltimaRando 
{
	private static Random rando;
	
	public static double rand()
	{
		return rando.nextDouble();
	}
	
	public static void setSeed(long seed)
	{
		rando = new Random(seed);
	}

	public static byte[] combineMap(byte[][] mapAreas)
	{
		byte[] rVal = new byte[mapAreas[0].length * 8];
		int k = 0;
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < mapAreas[i].length; j++)
			{
				rVal[k] = mapAreas[i][j];
				k++;
			}
		}
		return rVal;
	}
	
	public static Dungeon[] constructAllDungeons(NESRom rom) 
	{
		HashMap<Integer, Integer> map = getHCount(rom);
		byte[] finalMap = new byte[map.size()];
		int fm = 0;
		while(map.size() > 0)
		{
			int largest = -1;
			int largestIndex = -1;
			for (Integer key : new ArrayList<Integer>(map.keySet()))
			{
			    if (map.get(key) > largest)
			    {
			    	largest = map.get(key);
			    	largestIndex = key;
			    }
			}
			finalMap[fm] = (byte) largestIndex;
			fm++;
			map.remove(largestIndex);
		}
		newTree = finalMap;
		Dungeon[] ds = loadDungeons(rom);
		int totalBytesOld = 0;
		int totalBytesNew = finalMap.length + 1;
		//ArrayList<Byte> allDungeons = new ArrayList<Byte>();
		byte[][] out = new byte[ds.length][];
		for(int i = 0; i < ds.length; i++)
		{
			ds[i].decode(null);
		}
		return ds;
	}

	public static void seeStairDiffs(NESRom rom)
	{
		Dungeon[] ds = loadDungeons(rom);
		for(int i = 0; i < ds.length; i++)
			ds[i].calcDecodedFloors(null, null, null);
		String[] dShort = {"Dc", "Ds", "Dd", "Wr", "Co", "Sh"};
		ArrayList<Floor> floors = new ArrayList<Floor>();
		for(int i = 0; i < 6; i++)  //pass 1 doesn't include Hythloth or Abyss
		{
			for(int j = 1; j < 7; j++)
			{
				floors.add(ds[i].allFloors[j]);
			}
		}
		int avg = 0; 
		int avgCount = 0;
		int min = Integer.MAX_VALUE; 
		int max = 0;
		for(int i = 0; i < 6; i++)
		{
			System.out.println(ds[i].name + " connecting floor 0 to:");
			for(int k = 0; k < floors.size(); k++)
			{
				Floor f1 = ds[i].allFloors[0].copyFloor();
				Floor f2 = floors.get(k).copyFloor();
				int a = f1.connectToBelow(f2);
				System.out.print(dShort[k / 6] + " " + f2.origFloor + ":" + a + "  ");
				if(a < min)
					min = a;
				if(a > max)
					max = a;
				avg += a;
				avgCount++;
			}
			System.out.println("");
			for(int j = 1; j < 7; j++)
			{
				System.out.println(ds[i].name + " connecting floor " + j + " to:");
				for(int k = 0; k < floors.size(); k++)
				{
					Floor f1 = ds[i].allFloors[j].copyFloor();
					Floor f2 = floors.get(k).copyFloor();
					int a = f1.connectToBelow(f2);
					System.out.print(dShort[k / 6] + " " + f2.origFloor + ":" + a + "  ");
					if(a < min)
						min = a;
					if(a > max)
						max = a;
					avg += a;
					avgCount++;
				}
				System.out.println("");
			}
			System.out.println(ds[i].name + " connecting floor 7 up to:");
			for(int k = 0; k < floors.size(); k++)
			{
				Floor f1 = ds[i].allFloors[7].copyFloor();
				Floor f2 = floors.get(k).copyFloor();
				int a = f2.connectToBelow(f1);
				System.out.print(dShort[k / 6] + " " + f2.origFloor + ":" + a + "  ");
				if(a < min)
					min = a;
				if(a > max)
					max = a;
				avg += a;
				avgCount++;
			}
			System.out.println("");
		}
		System.out.println("---------------");
		double avgd = (double) (avg * 1.0 / avgCount * 1.0);
		System.out.println("min=" + min + "   max=" + max + "   avg=" + avg);
	}
	
	public static void testDungeonMake(NESRom rom)
	{
		for(int k = 1; k <= 10000; k++)
		{
			randomizeDungeons(rom);
			if(k % 100 == 0)
				System.out.print("T");
		}
	}
	
	public static void testShopSwaps()
	{
		try
		{
			int as = 65187;
			int failures = 0;
			for(int k = 1; k < 100001; k++)
			{
				NESRom nr = new NESRom("C:\\Users\\AaronX\\Desktop\\Ultima - Quest of the Avatar (U).nes");  //the golden rom
				boolean ff = false;
				if(k % 100 == 0)
					System.out.print("T");
				if(k % 10000 == 0)
					System.out.println("");
				nr.randomize("P1", null);
				String a = nr.listShops(as);
				ArrayList<Integer> shops = new ArrayList<Integer>();
				String[] aa = a.split("\n");
				for(int i = 0; i < aa.length; i++)
				{
					shops.clear();
					String[] ab = aa[i].split(" ");
					for(int j = 0; j < ab.length; j++)
					{
						try
						{
							int xx = Integer.parseInt(ab[j]);
							if(shops.contains(xx))
							{
								System.out.println("Duplicate shop type in " + ab[0] + "\n--------\n" + a);
								ff = true;
							}
							else
								shops.add(xx);
						}
						catch(Exception ex){}	
					}
				}
				if(ff)
					failures++;
			}
			System.out.println("100000 tests run with " + failures + " failures");
		}
		catch(Exception ex)
		{
			ex.printStackTrace(System.err);
			return;
		}
	}
	
	public static void testShopType()
	{
		try
		{
			int as = 65187;
			int failures = 0;
			for(int k = 1; k < 100001; k++)
			{
				NESRom nr = new NESRom("C:\\Users\\AaronX\\Desktop\\Ultima - Quest of the Avatar (U).nes");  //the golden rom
				boolean ff = false;
				if(k % 100 == 0)
					System.out.print("T");
				if(k % 10000 == 0)
					System.out.println("");
				nr.randomize("P2", null);
				String a = nr.listShops(as);
				ArrayList<Integer> shops = new ArrayList<Integer>();
				String[] aa = a.split("\n");
				for(int i = 0; i < aa.length; i++)
				{
					String[] ab = aa[i].split(" ");
					for(int j = 0; j < ab.length; j++)
					{
						try
						{
							int xx = Integer.parseInt(ab[j]);
							shops.add(xx);
						}
						catch(Exception ex){}	
					}
				}
				int[] shopTypes = {0, 0, 0, 2, 0, 1, 2, 3, 1, 2, 1, 2, 1, 1, 2, 1, 3};
				for(int i = 0; i < shopTypes.length; i++)
					if(shops.get(i) != shopTypes[i])
						ff = true;
				if(ff)
					failures++;
			}
			System.out.println("100000 tests run with " + failures + " failures");
		}
		catch(Exception ex)
		{
			ex.printStackTrace(System.err);
			return;
		}
	}
	
	
	public static Dungeon[] randomizeDungeons(NESRom rom)
	{
		Dungeon[] ds = loadDungeons(rom);
		for(int i = 0; i < ds.length; i++)
			ds[i].calcDecodedFloors(null, ds[i].floorTable, ds[i].encounterRoomTable);
		ArrayList<Floor> floors = new ArrayList<Floor>();
		for(int i = 0; i < 6; i++)  //pass 1 doesn't include Hythloth or Abyss
		{
			for(int j = 1; j < 7; j++)
			{
				floors.add(ds[i].allFloors[j]);
			}
		}
		int dNo = 0;
		int fNo = 1;
		int net = 0;
		while(floors.size() > 0)
		{
			int r = (int) (UltimaRando.rand() * floors.size());
			Floor ff = floors.get(r);
			ds[dNo].allFloors[fNo] = ff;
			int nn = ds[dNo].allFloors[fNo - 1].connectToBelow(ff);
			//ds[dNo].allFloors[fNo - 1].redoStairTable();
			String a = ds[dNo].allFloors[fNo - 1].getOrigLoc(false);
			String b = ff.getOrigLoc(true);
			//System.out.println(ds[dNo].name + " connecting floor " + (fNo - 1) + " to " + fNo + ":" + nn + "   (" + a + " to " + b + ")");
			net += nn;
			floors.remove(r);
			fNo++;
			if(fNo == 7)
			{
				nn = ds[dNo].allFloors[fNo - 1].connectToBelow(ds[dNo].allFloors[7]);
				//ds[dNo].allFloors[fNo].redoStairTable(ds[dNo].allFloors[6]);
				a = ds[dNo].allFloors[fNo - 1].getOrigLoc(false);
				b = ds[dNo].allFloors[7].getOrigLoc(true);
				//System.out.println(ds[dNo].name + " connecting floor " + (fNo - 1) + " to " + fNo + ":" + nn + "   (" + a + " to " + b + ")");
				net += nn;
				fNo = 1;
				dNo++;
			}
		}
		//System.out.println("Net stair gain=" + net);
		for(int i = 0; i < 6; i++)  //do not reassemble Hythloth or Abyss - it will randomize their stair tables!!!
			ds[i].reassemble();
		return ds;
	}
	
	public static Dungeon[] loadDungeons(NESRom rom)
	{
		String[] dNames = {"Deceit", "Despise", "Destard", "Wrong", "Covetous", "Shame", "Hythloth", "Abyss"};
		int[] treeLocs = {147491, 147658, 147825, 148001, 148171, 148344, 148509, 148665};
		int[] dataLocs = {147507, 147676, 147842, 148017, 148188, 148360, 148522, 148676};
		int[] roomTables = {155844, 155880, 155916, 155958, 155988, 156027, 156054, 156096, 156222};
		int[] stairTables = {154869, 154925, 154977, 155051, 155109, 155165, 155233, 155311, 155353};
		
		Dungeon[] rVal = new Dungeon[8];
		
		for(int i = 0; i < 8; i++)
		{
			rVal[i] = new Dungeon(dNames[i], treeLocs[i], dataLocs[i], rom.romData);
			rVal[i].encounterRoomTableLoc = roomTables[i];
			rVal[i].encounterRoomTable = rom.getData(roomTables[i], roomTables[i + 1] - 1);
			rVal[i].encounterRoomTablePtr = 155828 + 2 * i;
			rVal[i].floorTableLoc = stairTables[i];
			rVal[i].floorTable = rom.getData(stairTables[i], stairTables[i + 1] - 1);
			rVal[i].stoneRoomEquiv = (byte) (-127 + i);
		}
		return rVal;
	}
	
	public static HashMap<Integer, Integer> getHCount(NESRom rom)
	{
		
		Dungeon[] ds = loadDungeons(rom);
		HashMap<Integer, Integer> rVal = new HashMap<Integer, Integer>();
		
		
		for(int i = 0; i < ds.length; i++)
		{
			byte[] d1 = ds[i].decode(null);
			for(int j = 0; j < d1.length; j++)
			{
				if(rVal.get((int) d1[j]) == null)
					rVal.put((int) d1[j], 1);
				else
				{
					int cnt = rVal.get((int) d1[j]);
					rVal.put((int) d1[j], cnt + 1);
				}
			}	
		}
		//enable multi-stone
		rVal.remove(50);
		for(int i = -127; i <= -121; i++)
			rVal.put(i, 1);
		//System.out.println(rVal.toString());
		//System.out.println("total tile types=" + rVal.size());
		
		return rVal;
	}
	
	
	
	public static byte[][] encodeAllDungeons(NESRom rom, Dungeon[] newDs)
	{
		HashMap<Integer, Integer> map = getHCount(rom);
		byte[] finalMap = new byte[map.size()];
		int fm = 0;
		while(map.size() > 0)
		{
			int largest = -1;
			int largestIndex = -1;
			for (Integer key : new ArrayList<Integer>(map.keySet()))
			{
			    if (map.get(key) > largest)
			    {
			    	largest = map.get(key);
			    	largestIndex = key;
			    }
			}
			finalMap[fm] = (byte) largestIndex;
			fm++;
			map.remove(largestIndex);
		}
		newTree = finalMap;
		Dungeon[] ds = newDs;
		boolean needsDecode = false;
		if(ds == null)
		{
			ds = loadDungeons(rom);
			needsDecode = true;
		}
		int totalBytesOld = 0;
		int totalBytesNew = finalMap.length + 1;
		//ArrayList<Byte> allDungeons = new ArrayList<Byte>();
		byte[][] out = new byte[ds.length][];
		for(int i = 0; i < ds.length; i++)
		{
			byte[] raw;
			if(needsDecode)
				raw = ds[i].decode(null);
			else
			{
				raw = new byte[512];
				int ww = 0;
				for(int k = 0; k < ds[i].allFloors.length; k++)
				{
					for(int l = 0; l < ds[i].allFloors[k].layout.length; l++)
					{
						raw[ww] = ds[i].allFloors[k].layout[l];
						ww++;
					}
				}
			}
			totalBytesOld += raw.length;
			totalBytesOld += rom.romData[ds[i].treeLoc - 1];
			byte[] tt = ds[i].encode(raw, finalMap, 1);
			totalBytesNew += tt.length;
			//for(int j = 0; j < tt.length; j++)
				//allDungeons.add(tt[j]);
			//allDungeons.add((byte) 0);
			out[i] = tt;
		}
		//System.out.println("OLD BYTES:" + totalBytesOld);
		//System.out.println("NEW BYTES:" + totalBytesNew);
		
		//for(int j = 0; j < allDungeons.size(); j++)
			//out[j] = allDungeons.get(j);
		return out;
	}
	
	public static ArrayList<Byte> shuffleSearchItems(ArrayList<Byte> oldItems, int mode)
	{
		ArrayList<Byte> rv = new ArrayList<Byte>(oldItems.size());
		for(int i = 0; i < oldItems.size(); i++)
		{
			byte b = oldItems.get(i);
			if(b < 0)
			{
				rv.add((byte) -128);
				b &= 127;
				oldItems.set(i, b);
			}
			else
				rv.add((byte) 0);
		}
		if((mode & 1) == 1)  //remove the fungus and manroot spots
		{
			for(int i = 0; i < 2; i++)
			{
				int l = oldItems.size() - 1;
				byte b = oldItems.remove(l);
				rv.set(l, b);
			}
		}
		int j = 0;
		if((mode & 2) == 2)
		{
			//grab a non-rune item for 0,1
			for(int i = 0; i < 2; i++)
			{
				while(true)
				{
					int l = (int) (UltimaRando.rand() * oldItems.size());
					byte b = oldItems.get(l);
					byte d = (byte) (b & 127);
					if(d >= 64 && d <= 71 )  //if you're a rune continue
						continue;
					if(d == 11)  //if you're the horn continue
						continue;
					b = oldItems.remove(l);
					byte c = rv.get(j);
					b |= c;
					rv.set(j, b);
					break;
				}
				j++;
			}
		}
		else if((mode & 4) == 4)
		{
			for(int i = 0; i < 2; i++)
			{
				byte b = oldItems.remove(0);
				rv.set(j, b);
				j++;
			}
		}
		while(oldItems.size() > 0)
		{
			byte b = oldItems.remove((int) (UltimaRando.rand() * oldItems.size()));
			byte c = rv.get(j);
			b |= c;
			rv.set(j, b);
			j++;
		}
		return rv;
	}
	
	public static byte[] newTree;
	
	public static void testDungeonFloors(NESRom rom)
	{
		Dungeon[] ds = loadDungeons(rom);
		for(int i = 0; i < ds.length; i++)
		{
			ds[i].calcDecodedFloors(null, null, null);
		}
		for(int i = 0; i < ds.length; i++)
		{
			for(int j = 0; j < 7; j++)
			{
				Floor top = ds[i].allFloors[j];
				System.out.println(ds[i].name + "Floor #" + j + "has " + top.getDownCount() + " stairs down and can go down to:");
				for(int k = 0; k < ds.length; k++)
				{
					for(int l = 1; l < 8; l++)
					{
						if(top.testConnectionWith(ds[k].allFloors[l]))
							System.out.print(" " + k + ":" + l);
					}
				}
				System.out.println("");
			}
		}
	}
	
	public static void testAreaCenterLine()
	{
		int[] aa = {100, 200, 300, 400, 500};
		for(int i = 0; i < aa.length; i++)
		{
			for(int j = 0; j < aa.length; j++)
			{
				Rectangle r = new Rectangle(aa[i], aa[j], 100, 100);
				System.out.println(r.toString() + " in center line=" + Map.containsCenterDiagonal(r));
			}
		}
	}
	
	public static void testAllDungeons(NESRom rom)
	{
		byte[][] all = encodeAllDungeons(rom, null);
		Dungeon[] allDs = loadDungeons(rom);
		//int[] starts = new int[allDs.length];
		for(int i = 0; i < allDs.length; i++)
		{
			Dungeon d = allDs[i];
			d.setTreeData(null);
			d.calcDecodedFloors(null, d.floorTable, d.encounterRoomTable);
			byte[] d0 = d.decode(null);
			
			d.setTreeData(newTree);
			byte[] d1 = d.decode(all[i]);
			for(int j = 0; j < d1.length; j++)
			{
				if(d0[j] != d1[j])
				{
					if(d1[j] < -120 && d0[j] == 50)
						System.out.println("Stone room successfully replaced");
					else
						System.out.println("Mismatch in decoding of dungeon " + d.name + ": mismatch on byte #" + j + "; expected=" + d0[j] + " actual=" + d1[j]);
				}
			}
			System.out.println(d0[0] + " " + d1[0] + " " + allDs[0].allFloors[0].layout[0]);
			System.out.println(allDs[i].getLayout(d1));
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		/*byte t = -128;
		t = (byte) ((t >> 7) & 1);
		System.out.println("SHR:" + t);
		t <<= 2;
		System.out.println("SHL:" + t);*/
		//testAreaCenterLine();
		setSeed((long) (Math.random() * Long.MAX_VALUE));  //this should be the only call to Math.random()
		/*NESRom nr = null;
		try
		{
			nr = new NESRom("C:\\Users\\AaronX\\Desktop\\Ultima - Quest of the Avatar (U).nes");  //the golden rom
			/*nr = new NESRom("C:\\Users\\AaronX\\Desktop\\fceux-2.2.3-win32\\U4Rando.6003455369068555264.MO.nes");
			System.out.println(nr.getFinalSearchLocData());
			System.out.println(nr.getODLocations());
			MapWindow mw = new MapWindow(UltimaRando.combineMap(nr.getPotMap()), true);
			
			mw.setPOIList(nr.getPOIList());*/
		/*}
		catch(Exception ex)
		{
			return;
		}
		//testDungeonMake(nr);
		TextFinder tf = new TextFinder(nr);
		byte[] x1 = tf.decompressSpokenBytesAt(11257);
		System.out.println("Sextant 1:" + Arrays.toString(x1));
		byte[] xx = tf.decompressSpokenBytesAt(13008);
		System.out.println("Sextant 2:" + Arrays.toString(xx));
		byte[] h2 = tf.decompressSpokenBytesAt(8401);
		System.out.println("Heal 2:" + Arrays.toString(h2));*/
		
		/*for(int i = 2; i < 3; i++)
		{
			String s = i + "x:";
			s += tf.decodeSpokenText(93248, 40, i, nr.romData);  //ac30, 0x16c40
			System.out.println(s);
		}
		tf.replaceSpokenText(93248, 93288, " ", 2);
		for(int i = 1; i < 4; i++)
		{
			String yy = "";
			//for(int k = 3; k > i; k--)
				//yy += " ";
			for(int k = 0; k < i; k++)
				yy += "1";
			for(int j = 1; j < 4; j++)
			{
				String xx = "";
				//for(int k = 2; k > j; k--)
					//xx += " ";
				for(int k = 0; k < j; k++)
					xx += "1";
				int nspc = 5 - i - j;
				String aa = " Search ";
				for(int k = 0; k < nspc; k++)
					aa += " ";
				String sss = aa + yy + "," + xx + " on t";
				if(nspc < 0)
					sss = " Seek  " + yy + "," + xx + " on t";
				sss = tf.replaceSpokenText(93248, 93288, sss, 2);
				System.out.println(i + "" + j + ":" + sss);
			}
		}
		//String s = tf.replaceSpokenText(93248, 93288, " Seek 111,111 on ", 2);
		//System.out.println("E:" + s);
		for(int i = 4; i < 5; i++)
		{
			String ss = i + ":";
			ss += tf.decodeSpokenText(95292, 70, i, nr.romData);  //b431, 0x17441
			System.out.println(ss);
		}
		
		for(int i = 1; i < 4; i++)
		{
			String yy = "";
			for(int k = 2; k > i; k--)
				yy += " ";
			for(int k = 0; k < i; k++)
				yy += "1";
			for(int j = 1; j < 4; j++)
			{
				String xx = "";
				for(int k = 3; k > j; k--)
					xx += " ";
				for(int k = 0; k < j; k++)
					xx += "1";
				String sss = "at latitude " + yy + ",\nlongitude " + xx;
				if(i == 3)
					sss = "at latitude " + yy + "\nlongitude " + xx;
				sss = tf.replaceSpokenText(95292, 95362, sss, 4);
				System.out.println(i + "" + j + ":" + sss);
			}
		}*/
		
		//String ss = tf.replaceSpokenText(95292, 95362, "at latitude 155\nlongitude ", 4);
		//System.out.println("E:" + ss);
		//NESRom nr = new NESRom("C:\\Users\\AaronX\\Desktop\\UOpenScr");  //a sample RAM dump
		
		//byte[] map = combineMap(nr.getPotMap());
		//MapWindow mw = new MapWindow(map, true);
		/*Map m = new Map(256, 256);
		m.mapData = map;
		map = m.rleCompress(false, false);
		System.out.println("RLE Compression compressed " + m.mapData.length + " bytes into " + map.length + " bytes");
		map = m.rleCompress(true, false);
		System.out.println("RLE Compression with just land collapsing compressed " + m.mapData.length + " bytes into " + map.length + " bytes");
		map = m.rleCompress(false, true);
		System.out.println("RLE Compression with just water collapsing compressed " + m.mapData.length + " bytes into " + map.length + " bytes");
		map = m.rleCompress(true, true);
		System.out.println("RLE Compression with full collapsing compressed " + m.mapData.length + " bytes into " + map.length + " bytes");*/
		//System.out.println(nr.getInitCharData());
		//System.out.println(nr.getInitSearchSpotData());
		
		//Map.testPOIUnique();
		//Map.testMapGen(100);
		//random map
		/*Map m = new Map(256, 256);
		m.makeRandomMap();
		MapWindow mwr = new MapWindow(m.mapData, false);
		mwr.setAccessData(m.accessData);
		mwr.setVisibilityData(m.visibility);*/
		//System.out.println(m.printMapInfo());
		
		/*TextFinder tf = new TextFinder(nr);
		int block = 0;  //this nes rom has 16 blocks, 0 - 15; dumps have 4 blocks 0 - 3
		tf.tryArea(block * 16384);*/
		//nr.dumpRom("C:\\Users\\AaronX\\Desktop\\UltimaRomCopy.nes");
		//tf.scanForDiff("NINTENDO");
		
		/*HuffmanDecoder hd = new HuffmanDecoder(nr.romData, 192517, 192551);
		hd.setEndInput(192696);
		hd.setLookup(255612);
		byte[] openScreen = hd.decode(null, 0, 2, 3);
		TextFinder tf = new TextFinder(nr);
		tf.scanLine0(openScreen, 0, openScreen.length);*/
		
		//OpeningScreen os = new OpeningScreen(nr);
		//os.replaceOpenScreen(nr);
		
		
		
		//nr.dumpRom("C:\\Users\\AaronX\\Desktop\\fceux-2.2.3-win32\\Deltima.nes");
		
		String initChar = 
		 "CF E8 D0 EC 20 C1 95 BD 00 66 A8 C8 C0 0A 90 02 " +
		 "A0 00 20 C7 95 90 B9 86 03 20 F3 CC A6 03 BD 00 " +
		 "66 09 80 A8 8A 20 0A CE 4C 10 95 20 C1 95 BD 00 " +
		 "66 A8 88 10 DD A0 09 D0 D9 A9 28 20 21 97 20 C0 " +
		 "96 20 F3 96 20 0C 96 20 49 F4 B5 95 BD 95 81 95 " +
		 "81 95 96 95 A7 95 20 EF 96 20 C1 95 F0 E3 CA CA " +
		 "8A 99 06 61 4C 81 95 20 EF 96 20 C1 95 E0 10 B0 " +
		 "D0 E8 E8 D0 EB 20 C1 95 8A 4A 4C C6 94 A9 FF D0 " +
		 "F9 A4 65 BE 06 61 60 48 98 9D 00 66 8A 48 20 E7 " +
		 "95 85 02 86 03 38 A5 18 E5 02 A5 19 E5 03 68 AA " +
		 "68 B0 03 9D 00 66 60 A9 00 AA A4 16 85 02 86 03 " +
		 "A2 0A 20 F3 F3 A5 04 A6 05 18 79 00 66 90 01 E8 " +
		 "C8 C4 17 90 E7 F0 E5 60 A9 00 AA 60 20 56 F1 20 " +
		 "C7 96 8A 30 F7 60 A9 7F D0 02 A9 79 85 02 20 FC " +
		 "CC 85 13 B9 06 61 A4 02 4C 0A CE 20 C0 96 20 F3 " +
		 "96 20 56 F1 20 C7 96 8A 10 0B A5 F8 29 20 25 72";
		
		String initChar2 = 
		"A9 90 8D 27 68 A9 01 8D 28 68 A9 FF 8D 1B 68 4A " +
		"8D 1C 68 A9 08 8D 1E 68 A9 01 8D D1 68 A9 00 AA " +
		"A8 A9 32 99 0C 68 BD E5 95 9D 71 68 9D 81 68 BD " +
		"E6 95 9D 72 68 9D 82 68 BD 15 96 9D B9 68 BD 16 " +
		"96 9D BA 68 B9 F5 95 99 91 68 99 99 68 B9 FD 95 " +
		"99 A1 68 B9 05 96 99 A9 68 B9 0D 96 99 B1 68 B9 " +
		"25 96 99 1F 68 B9 DD 95 99 69 68 84 02 98 0A 65 " +
		"02 0A A8 BD 2D 96 99 39 68 BD 2E 96 99 3A 68 A4 " +
		"02 E8 E8 C8 C0 08 90 99 A2 03 BD D9 95 9D F0 68 " +
		"CA 10 F7 A9 02 8D 08 69 60 E9 F3 7D 7F 02 03 03 " +
		"02 02 03 02 02 C8 00 2C 01 2C 01 C8 00 C8 00 2C " +
		"01 C8 00 C8 00 32 0B 00 10 04 0A 0B 00 10 14 1B " +
		"12 15 15 18 12 17 19 13 14 18 12 13 12 19 16 11 " +
		"16 12 14 16 11 7D 00 F0 00 CD 00 AF 00 6E 00 45 " +
		"01 96 00 69 00 08 08 09 07 08 04 00 00 86 90 81 " +
		"90 88 91 86 90 87 91 89 92 89 91 86 90 7F 7F 7F";
		
		String itemFind =   //found at 1a3af; item coords = 1a44d(x), 1a44e(y) (Trinsic search spot, 1a44f is item)
				"4B 4C 4B 4C 3E 3E 3E 3E 4E 4E 4E 4E 3F 3F 3F 3F " +
				"4F 4F 4F 4F 1D 1D 2D 2D 00 22 15 54 54 15 0A 0A " +
				"06 06 54 47 51 15 57 57 01 29 51 01 51 51 41 59 " +
				"58 57 55 55 55 15 47 51 54 54 15 15 15 47 15 57 " +
				"54 54 54 57 12 12 51 51 54 57 47 12 51 51 30 51 " +
				"56 56 02 02 02 02 1A 00 10 30 17 37 30 19 28 18 " +
				"12 22 30 18 82 23 00 FF 10 2A 15 02 10 2A 17 10 " +
				"10 2A 1B 10 10 2A 06 02 18 2B 01 1D 06 27 04 06 " +
				"0D 2A 09 07 06 26 03 0E 1E 26 0A 03 1C 06 0D 14 " +
				"17 28 17 19 06 07 1C 05 1E 27 17 14 00 1A 1A 45 " +
				"21 0D 02 80 40 0D 03 80 41 0E 02 80 42 0E 03 80 " +
				"43 0F 02 80 44 0F 03 80 45 0F 04 80 46 10 02 80 " +
				"47 10 03 80 48 10 04 80 49 00 A0 06 A2 09 20 D6 " +
				"CF AD E8 68 D0 0D AE 08 69 E8 E0 04 90 02 A2 00 " +
				"8E 08 69 A9 80 8D E8 68 A0 19 98 48 20 BE E3 AE " +
				"08 69 BD A6 A4 A0 17 91 02 C8 C0 1D 90 F9 68 A8";
		
		String mapLocations = //found at 3ea40, map locations x,y begin at 3eab7 and go to to 3eb02 in order of increasing y
				"07 8D 05 07 85 E8 85 EA 68 20 86 EA 20 26 F0 A5 " +
				"4B 0A A9 00 2A 85 5F 4C 46 C0 AD C2 06 C9 86 D0 " +
				"0A A9 E9 8D F0 68 A9 F3 8D F1 68 A5 4B 29 BF C9 " +
				"2F D0 03 AD C2 06 A2 00 DD F3 EA F0 06 E8 E0 26 " +
				"90 F6 60 8A 0A AA BD A7 EA 85 42 BD A8 EA 85 43 " +
				"A9 00 85 4B F0 B3 29 7F AA BD 19 EB 85 4F BD 1A " +
				"EB 85 50 8A 4A AA BD 81 EB 85 4C 60 A5 47 29 3F " +
				"AA BD 00 06 29 FC 60 49 0B A6 13 7E 14 9F 14 9C " +  //starts here at 49 0B with Shrine of Justice
				"1B 3A 2B CD 2D 1D 32 1C 32 C9 3B E9 42 5B 43 F0 " +
				"49 3F 51 88 5A 80 5C 3A 66 53 6A 57 6B 56 6B DB " +
				"6B DA 6B 16 80 E8 87 61 92 88 9D 48 A8 BB A9 6A " +
				"B8 51 CF E7 D7 26 DC 26 E3 E9 E9 D5 EB EF F0 92 " +
				"F1 93 F1 1E 21 83 09 84 08 1F 03 03 0E 1B 81 80 " +  //ends here at 93 F1 with Serpent's Hold E
				"2C 0F 1C 85 06 01 01 02 02 0B 05 10 0D 82 0C 0A " +
				"20 22 07 1D 31 2D 86 04 04 FE 9F E9 A1 00 A0 F5 " +
				"A5 0A B5 00 A0 4C B5 00 A0 62 A3 71 A2 00 A0 00";
		
		String moongates = "E0 60 2C 31 A6 67 17 BB 80 85 66 DE 24 13 C3 7D B1 4E 01";  //moongates are in two locations (0x13f4d and 0x17f4d)  and store all xs followed by all ys (the 8 x locations and the 8 y locations)
		//nr.findMatch(nr.strToBytes(moongates));
		
		String shom = "DE CE";
		//nr.findMatch(nr.strToBytes(shom));
		/*Map mtr = new Map(0,0);
		Map.MapZone mz = mtr.new MapZone();
		mz.testRiverDelta();*/
		//nr.findMatch("C:\\Users\\AaronX\\Desktop\\UFullDespise");
		//nr.findMatch("C:\\Users\\AaronX\\Desktop\\UHuffTable");
		//nr.findMatch("C:\\Users\\AaronX\\Desktop\\EquippableTable");  //equippable table found at 0xf74-0xf8c; 
		//each item has a bit field containing equippablity for each characer; 1=Mariah, 2=Iolo 4=Geoff 8=Jaana 16=Julius 32=Dupre 64=Shimano 128=Katrina
		//nr.findMatch("C:\\Users\\AaronX\\Desktop\\RuneValor");
		//System.out.println(nr.getInitCharData());
		//Dungeon d1 = new Dungeon("Despise" , 147658, 147676, nr.romData);
		//System.out.println(d1.report());
		//d1.testValueForIndex();
		//d1.testEncoder();
		//testAllDungeons(nr);
		//nr.changeDungeonBlock(null);
		//Dungeon[] ds = loadDungeons(nr);
		//randomizeDungeons(nr);
		//seeStairDiffs(nr);
		//System.out.println("Net stair gain=" + nn);
		//testAllDungeons(nr);
		//NESRom.freeSpaceAnalysis(nr);
		
		/*NESRom nr2 = nr.randomize("MV2Q2B2", "C:\\Users\\AaronX\\Desktop\\fceux-2.2.3-win32\\Deltima.nes");
		MapWindow mw = new MapWindow(combineMap(nr.getPotMap()), true);
		mw.setPOIList(nr.getPOIList());
		mw.setMoongates(nr.getMoongates());
		//System.out.println(nr2.getInitCharData());
		//MapWindow mw2 = new MapWindow(combineMap(nr.getPotMap()), false);
		String s = "Starting locations:\n";
		s += nr.getStartLocs();
		s += "\nSearch spots:\n";
		s += nr.getInitSearchSpotData();
		s += "\nOutdoor locations:\n";
		s += nr.getODLocations();
		s += "\nMoongate locations:\n";
		s += nr.getMoongateLocs();
		s += "\nShrine of Humility:\n";
		s += nr.getInitShrineHumilData();
		System.out.println(s);*/
		//testShopSwaps();
		//testShopType();
		RandoWindow rw = new RandoWindow();
		
	}

}

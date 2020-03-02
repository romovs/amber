/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.error.ErrorHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.util.*;

import static haven.Utils.getprop;

public class Config {
    public static final boolean iswindows = System.getProperty("os.name").startsWith("Windows");
    public static String authuser = null;
    public static String authserv = null;
    public static String defserv = null;
    public static URL resurl = null;
    public static boolean dbtext = false;
    public static boolean profile = false;
    public static boolean profilegpu = false;
    public static String resdir = getprop("haven.resdir", System.getenv("HAFEN_RESDIR"));
    public static boolean nopreload = false;
    public static int mainport = 1870;
    public static int authport = 1871;
    public static URL screenurl = geturl("http://game.havenandhearth.com/mt/ss");
    public static boolean hideflocomplete = Utils.getprefb("hideflocomplete", false);
    public static boolean hideflovisual = Utils.getprefb("hideflovisual", false);
    public static boolean daylight = Utils.getprefb("daylight", false);
    public static boolean showkinnames = Utils.getprefb("showkinnames", true);
    public static boolean savemmap = Utils.getprefb("savemmap", false);
    public static boolean studylock = Utils.getprefb("studylock", false);
    public static boolean chatsave = Utils.getprefb("chatsave", false);
    public static boolean chattimestamp = Utils.getprefb("chattimestamp", true);
    public static boolean alarmunknown = Utils.getprefb("alarmunknown", false);
    public static double alarmunknownvol = Utils.getprefd("alarmunknownvol", 0.32);
    public static boolean alarmred = Utils.getprefb("alarmred", false);
    public static double alarmredvol = Utils.getprefd("alarmredvol", 0.32);
    public static boolean showquality = Utils.getprefb("showquality", true);
    public static boolean qualitywhole = Utils.getprefb("qualitywhole", true);
    public static int badcamsensitivity = Utils.getprefi("badcamsensitivity", 5);
    public static List<LoginData> logins = new ArrayList<LoginData>();
    public static boolean mapshowgrid = Utils.getprefb("mapshowgrid", false);
    public static boolean mapshowviewdist = Utils.getprefb("mapshowviewdist", false);
    public static boolean disabletiletrans = Utils.getprefb("disabletiletrans", false);
    public static boolean itemmeterbar = Utils.getprefb("itemmeterbar", false);
    public static boolean showprogressperc = Utils.getprefb("showprogressperc", true);
    public static boolean timersalarm = Utils.getprefb("timersalarm", false);
    public static double timersalarmvol = Utils.getprefd("timersalarmvol", 0.8);
    public static boolean quickslots = Utils.getprefb("quickslots", true);
    public static boolean quickbelt = Utils.getprefb("quickbelt", false);
    public static boolean statuswdgvisible = Utils.getprefb("statuswdgvisible", false);
    public static boolean chatalarm = Utils.getprefb("chatalarm", true);
    public static double chatalarmvol = Utils.getprefd("chatalarmvol", 0.8);
    public static boolean studyalarm = Utils.getprefb("studyalarm", false);
    public static double studyalarmvol = Utils.getprefd("studyalarmvol", 0.8);
    public static double sfxchipvol = Utils.getprefd("sfxchipvol", 0.9);
    public static double sfxquernvol = Utils.getprefd("sfxquernvol", 0.9);
    public static double sfxfirevol = Utils.getprefd("sfxfirevol", 1.0);
    public static double sfxbeevol = Utils.getprefd("sfxbeevol", 0.5);
    public static boolean showcraftcap = Utils.getprefb("showcraftcap", true);
    public static boolean showgobhp = Utils.getprefb("showgobhp", false);
    public static boolean showplantgrowstage = Utils.getprefb("showplantgrowstage", false);
    public static boolean notifykinonline = Utils.getprefb("notifykinonline", true);
    public static boolean showminerad = Utils.getprefb("showminerad", false);
    public static boolean showfarmrad = Utils.getprefb("showfarmrad", false);
    public static boolean showweather = Utils.getprefb("showweather", true);
    public static boolean simplecrops = Utils.getprefb("simplecrops", false);
    public static boolean hidecrops = Utils.getprefb("hidecrops", false);
    public static boolean showfps = Utils.getprefb("showfps", false);
    public static boolean autohearth = Utils.getprefb("autohearth", false);
    public static boolean showplayerpaths = Utils.getprefb("showplayerpaths", false);
    public static boolean showinvonlogin = Utils.getprefb("showinvonlogin", false);
    public static boolean runonlogin = Utils.getprefb("runonlogin", false);
    public static Coord chatsz = Utils.getprefc("chatsz", new Coord(683, 111));
    public static boolean autostudy = Utils.getprefb("autostudy", false);
    public static boolean showdmgop = Utils.getprefb("showdmgop", true);
    public static boolean hidegobs = Utils.getprefb("hidegobs", false);
    public static boolean qualitybg = Utils.getprefb("qualitybg", true);
    public static int qualitybgtransparency = Utils.getprefi("qualitybgtransparency", 5);
    public static boolean tilecenter = Utils.getprefb("tilecenter", false);
    public static boolean userazerty = Utils.getprefb("userazerty", false);
    public static boolean hlightcuropp = Utils.getprefb("hlightcuropp", false);
    public static boolean reversebadcamx = Utils.getprefb("reversebadcamx", false);
    public static boolean reversebadcamy = Utils.getprefb("reversebadcamy", false);
    public static boolean showservertime = Utils.getprefb("showservertime", false);
    public static boolean enabletracking = Utils.getprefb("enabletracking", false);
    public static boolean enablecrime = Utils.getprefb("enablecrime", false);
    public static boolean resinfo = Utils.getprefb("resinfo", false);
    public static boolean showanimalrad = Utils.getprefb("showanimalrad", true);
    public static boolean hwcursor = Utils.getprefb("hwcursor", false);
    public static boolean showboundingboxes = Utils.getprefb("showboundingboxes", false);
    public static double alarmonforagablesvol = Utils.getprefd("alarmonforagablesvol", 0.8);
    public static boolean alarmlocres = Utils.getprefb("alarmlocres", false);
    public static double alarmlocresvol = Utils.getprefd("alarmlocresvol", 0.8);
    public static boolean alarmtroll = Utils.getprefb("alarmtroll", false);
    public static double alarmtrollvol = Utils.getprefd("alarmtrollvol", 0.8);
    public static boolean showcooldown = Utils.getprefb("showcooldown", false);
    public static boolean nodropping = Utils.getprefb("nodropping", false);
    public static boolean nodropping_all = Utils.getprefb("nodropping_all", false);
    public static boolean fbelt = Utils.getprefb("fbelt", false);
    public static boolean histbelt = Utils.getprefb("histbelt", false);
    public static boolean dropMinedStones = Utils.getprefb("dropMinedStones", true);
    public static boolean dropMinedOre = Utils.getprefb("dropMinedOre", true);
    public static boolean dropMinedOrePrecious = Utils.getprefb("dropMinedOrePrecious", true);
    public static boolean dropMinedCurios = Utils.getprefb("dropMinedCurios", true);
    public static boolean dropEverything = false; //for safety.  //Utils.getprefb("dropEverything", true);
    public static boolean dropSoil = Utils.getprefb("dropSoil", false);
    public static boolean showdframestatus = Utils.getprefb("showdframestatus", true);
    public static boolean enableorthofullzoom = Utils.getprefb("enableorthofullzoom", false);
    public static boolean partycircles =  Utils.getprefb("partycircles", false);
    public static boolean alarmbram =  Utils.getprefb("alarmbram", false);
    public static double alarmbramvol = Utils.getprefd("alarmbramvol", 1.0);
    public static double sfxwhipvol = Utils.getprefd("sfxwhipvol", 0.9);
    public static boolean showarchvector =  Utils.getprefb("showarchvector", false);
    public static boolean autologout =  Utils.getprefb("autologout", false);
    public static boolean logcombatactions =  Utils.getprefb("logcombatactions", false);
    public static boolean autopickmussels =  Utils.getprefb("autopickmussels", false);
    public static boolean confirmmagic =  Utils.getprefb("confirmmagic", true);
    public static boolean altfightui =  Utils.getprefb("altfightui", false);
    public static boolean combshowkeys =  Utils.getprefb("combshowkeys", true);
    public static boolean combaltopenings =  Utils.getprefb("combaltopenings", true);
    public static boolean studyhist =  Utils.getprefb("studyhist", false);
    public static boolean studybuff =  Utils.getprefb("studybuff", false);
    public static int zkey =  Utils.getprefi("zkey", KeyEvent.VK_Z);
    public static boolean disableterrainsmooth =  Utils.getprefb("disableterrainsmooth", false);
    public static boolean disableelev =  Utils.getprefb("disableelev", false);
    public static String treeboxclr =  Utils.getpref("treeboxclr", "D7FF00");
    public static boolean highlightpots = Utils.getprefb("highlightpots", false);
    public static boolean bonsai = Utils.getprefb("bonsai", false);
    public static int fontsizechat = Utils.getprefi("fontsizechat", 14);
    public static boolean fontaa = Utils.getprefb("fontaa", false);
    public static boolean usefont = Utils.getprefb("usefont", false);
    public static String font = Utils.getpref("font", "SansSerif");
    public static int fontadd = Utils.getprefi("fontadd", 0);
    public static boolean proximityaggro = Utils.getprefb("proximityaggro", false);
    public static boolean proximityaggroanimal = Utils.getprefb("proximityaggroanimal", false);
    public static boolean proximitychase = Utils.getprefb("proximitychase", false);
    public static boolean proximitylift = Utils.getprefb("proximitylift", false);
    public static boolean pf = false;
    public static String playerposfile;
    public static byte[] authck = null;
    public static String prefspec = "hafen";
    public static String version;
    public static String gitrev;
    public static String confid = "amber";

    public final static String chatfile = "chatlog.txt";
    public static PrintWriter chatlog = null;

    public final static HashMap<String, CheckListboxItem> boulders = new HashMap<String, CheckListboxItem>(27) {{
        put("basalt", new CheckListboxItem("Basalt"));
        put("schist", new CheckListboxItem("Schist"));
        put("dolomite", new CheckListboxItem("Dolomite"));
        put("gneiss", new CheckListboxItem("Gneiss"));
        put("granite", new CheckListboxItem("Granite"));
        put("porphyry", new CheckListboxItem("Porphyry"));
        put("quartz", new CheckListboxItem("Quartz"));
        put("limestone", new CheckListboxItem("Limestone"));
        put("sandstone", new CheckListboxItem("Sandstone"));
        put("cinnabar", new CheckListboxItem("Cinnabar"));
        put("feldspar", new CheckListboxItem("Feldspar"));
        put("marble", new CheckListboxItem("Marble"));
        put("flint", new CheckListboxItem("Flint"));
        put("hornblende", new CheckListboxItem("Hornblende"));
        put("olivine", new CheckListboxItem("Olivine"));
        put("alabaster", new CheckListboxItem("Alabaster"));
        put("zincspar", new CheckListboxItem("Zincspar"));
        put("apatite", new CheckListboxItem("Apatite"));
        put("fluorospar", new CheckListboxItem("Fluorospar"));
        put("gabbro", new CheckListboxItem("Gabbro"));
        put("corund", new CheckListboxItem("Corund"));
        put("kyanite", new CheckListboxItem("Kyanite"));
        put("mica", new CheckListboxItem("Mica"));
        put("microlite", new CheckListboxItem("Microlite"));
        put("orthoclase", new CheckListboxItem("Orthoclase"));
        put("soapstone", new CheckListboxItem("Soapstone"));
        put("sodalite", new CheckListboxItem("Sodalite"));
    }};

    public final static HashMap<String, CheckListboxItem> bushes = new HashMap<String, CheckListboxItem>(23) {{
        put("arrowwood", new CheckListboxItem("Arrowwood"));
        put("crampbark", new CheckListboxItem("Crampbark"));
        put("sandthorn", new CheckListboxItem("Sandthorn"));
        put("blackberrybush", new CheckListboxItem("Blackberry"));
        put("dogrose", new CheckListboxItem("Dogrose"));
        put("spindlebush", new CheckListboxItem("Spindlebush"));
        put("blackcurrant", new CheckListboxItem("Blackcurrant"));
        put("elderberrybush", new CheckListboxItem("Elderberry"));
        put("teabush", new CheckListboxItem("Tea"));
        put("blackthorn", new CheckListboxItem("Blackthorn"));
        put("gooseberrybush", new CheckListboxItem("Gooseberry"));
        put("tibast", new CheckListboxItem("Tibast"));
        put("bogmyrtle", new CheckListboxItem("Bogmyrtle"));
        put("hawthorn", new CheckListboxItem("Hawthorn"));
        put("tundrarose", new CheckListboxItem("Tundrarose"));
        put("boxwood", new CheckListboxItem("Boxwood"));
        put("holly", new CheckListboxItem("Hollyberry"));
        put("woodbine", new CheckListboxItem("Fly Woodbine"));
        put("bsnightshade", new CheckListboxItem("Bittersweet Nightshade"));
        put("raspberrybush", new CheckListboxItem("Raspberry"));
        put("caprifole", new CheckListboxItem("Caprifole"));
        put("redcurrant", new CheckListboxItem("Redcurrant"));
        put("gorse", new CheckListboxItem("Gorse"));
    }};

    public final static HashMap<String, CheckListboxItem> trees = new HashMap<String, CheckListboxItem>(55) {{
        put("alder", new CheckListboxItem("Alder"));
        put("corkoak", new CheckListboxItem("Corkoak"));
        put("plumtree", new CheckListboxItem("Plum Tree"));
        put("juniper", new CheckListboxItem("Juniper"));
        put("crabappletree", new CheckListboxItem("Crabapple"));
        put("kingsoak", new CheckListboxItem("King's Oak"));
        put("oak", new CheckListboxItem("Oak"));
        put("walnuttree", new CheckListboxItem("Walnut Tree"));
        put("birdcherrytree", new CheckListboxItem("Birdcherry Tree"));
        put("larch", new CheckListboxItem("Larch"));
        put("poplar", new CheckListboxItem("Poplar"));
        put("whitebeam", new CheckListboxItem("Whitebeam"));
        put("appletree", new CheckListboxItem("Apple Tree"));
        put("cypress", new CheckListboxItem("Cypress"));
        put("buckthorn", new CheckListboxItem("Buckthorn"));
        put("laurel", new CheckListboxItem("Laurel"));
        put("ash", new CheckListboxItem("Ash"));
        put("elm", new CheckListboxItem("Elm"));
        put("rowan", new CheckListboxItem("Rowan"));
        put("willow", new CheckListboxItem("Willow"));
        put("cedar", new CheckListboxItem("Cedar"));
        put("linden", new CheckListboxItem("Linden"));
        put("olivetree", new CheckListboxItem("Olive Tree"));
        put("aspen", new CheckListboxItem("Aspen"));
        put("fir", new CheckListboxItem("Fir"));
        put("baywillow", new CheckListboxItem("Baywillow"));
        put("goldenchain", new CheckListboxItem("Goldenchain"));
        put("peartree", new CheckListboxItem("Pear Tree"));
        put("sallow", new CheckListboxItem("Sallow"));
        put("yew", new CheckListboxItem("Yew"));
        put("cherry", new CheckListboxItem("Cherry"));
        put("maple", new CheckListboxItem("Maple"));
        put("beech", new CheckListboxItem("Beech"));
        put("chestnuttree", new CheckListboxItem("Chestnut Tree"));
        put("hazel", new CheckListboxItem("Hazel"));
        put("spruce", new CheckListboxItem("Spruce"));
        put("hornbeam", new CheckListboxItem("Hornbeam"));
        put("oldtrunk", new CheckListboxItem("Mirkwood Log"));
        put("conkertree", new CheckListboxItem("Conker Tree"));
        put("mulberry", new CheckListboxItem("Mulberry"));
        put("sweetgum", new CheckListboxItem("Sweetgum"));
        put("pine", new CheckListboxItem("Pine"));
        put("birch", new CheckListboxItem("Birch"));
        put("planetree", new CheckListboxItem("Plane Tree"));
        put("quincetree", new CheckListboxItem("Quince"));
        put("almondtree", new CheckListboxItem("Almond"));
        put("terebinth", new CheckListboxItem("Terebinth"));
        put("chastetree", new CheckListboxItem("Chastetree"));
        put("treeheath", new CheckListboxItem("Tree Heath"));
        put("lotetree", new CheckListboxItem("Lote Tree"));
        put("sorbtree", new CheckListboxItem("Sorb"));
        put("persimmontree", new CheckListboxItem("Persimmon"));
        put("medlartree", new CheckListboxItem("Medlar"));
        put("silverfir", new CheckListboxItem("Silver Fir"));
        put("mayflower", new CheckListboxItem("Mayflower"));
    }};

    public final static HashMap<String, CheckListboxItem> icons = new HashMap<String, CheckListboxItem>(49) {{
        put("dandelion", new CheckListboxItem("Dandelion"));
        put("chantrelle", new CheckListboxItem("Chantrelle"));
        put("blueberry", new CheckListboxItem("Blueberry"));
        put("rat", new CheckListboxItem("Rat"));
        put("chicken", new CheckListboxItem("Chicken"));
        put("chick", new CheckListboxItem("Chick"));
        put("spindlytaproot", new CheckListboxItem("Spindly Taproot"));
        put("stingingnettle", new CheckListboxItem("Stinging Nettle"));
        put("dragonfly", new CheckListboxItem("Dragonfly"));
        put("toad", new CheckListboxItem("Toad"));
        put("bram", new CheckListboxItem("Battering Ram"));
        put("rowboat", new CheckListboxItem("Rowboat"));
        put("arrow", new CheckListboxItem("Arrow"));
        put("boarspear", new CheckListboxItem("Boar Spear"));
        put("frog", new CheckListboxItem("Frog"));
        put("wagon", new CheckListboxItem("Wagon"));
        put("wheelbarrow", new CheckListboxItem("Wheelbarrow"));
        put("cart", new CheckListboxItem("Cart"));
        put("wball", new CheckListboxItem("Wrecking Ball"));
        put("windweed", new CheckListboxItem("Wild Windsown Weed"));
        put("mussels", new CheckListboxItem("Mussels"));
        put("mallard", new CheckListboxItem("Duck"));
        put("ladybug", new CheckListboxItem("Ladybug"));
        put("silkmoth", new CheckListboxItem("Silkmoth"));
        put("hedgehog", new CheckListboxItem("Hedgehog"));
        put("squirrel", new CheckListboxItem("Squirrel"));
        put("rabbit", new CheckListboxItem("Rabbit"));
        put("lingon", new CheckListboxItem("Lingonberries"));
        put("grub", new CheckListboxItem("Grub"));
        put("yellowfoot", new CheckListboxItem("Yellowfoot"));
        put("chives", new CheckListboxItem("Chives"));
        put("rustroot", new CheckListboxItem("Rustroot"));
        put("boostspeed", new CheckListboxItem("Speed Boost"));
        put("adder", new CheckListboxItem("Adder"));
        put("crab", new CheckListboxItem("Crab"));
        put("clover", new CheckListboxItem("Clover"));
        put("ladysmantle", new CheckListboxItem("Lady's Mantle"));
        put("grasshopper", new CheckListboxItem("Grasshopper"));
        put("irrbloss", new CheckListboxItem("Irrlight"));
        put("opiumdragon", new CheckListboxItem("Opium Dragon"));
        put("snapdragon", new CheckListboxItem("Uncommon Snapdragon"));
        put("cattail", new CheckListboxItem("Cattail"));
        put("forestsnail", new CheckListboxItem("Forest Snail"));
        put("forestlizard", new CheckListboxItem("Forest Lizard"));
        put("mole", new CheckListboxItem("Mole"));
        put("cavemoth", new CheckListboxItem("Cave Moth"));
        put("thornythistle", new CheckListboxItem("Thorny Thistle"));
        put("mistletoe", new CheckListboxItem("Mistletoe"));
        put("waterstrider", new CheckListboxItem("Waterstrider"));
        put("firefly", new CheckListboxItem("Firefly"));
    }};

    public final static HashMap<String, CheckListboxItem> flowermenus = new HashMap<String, CheckListboxItem>(19) {{
        put("Pick", new CheckListboxItem("Pick", Resource.BUNDLE_FLOWER));
        put("Harvest", new CheckListboxItem("Harvest", Resource.BUNDLE_FLOWER));
        put("Eat", new CheckListboxItem("Eat", Resource.BUNDLE_FLOWER));
        put("Split", new CheckListboxItem("Split", Resource.BUNDLE_FLOWER));
        put("Kill", new CheckListboxItem("Kill", Resource.BUNDLE_FLOWER));
        put("Slice", new CheckListboxItem("Slice", Resource.BUNDLE_FLOWER));
        put("Pluck", new CheckListboxItem("Pluck", Resource.BUNDLE_FLOWER));
        put("Clean", new CheckListboxItem("Clean", Resource.BUNDLE_FLOWER));
        put("Skin", new CheckListboxItem("Skin", Resource.BUNDLE_FLOWER));
        put("Flay", new CheckListboxItem("Flay", Resource.BUNDLE_FLOWER));
        put("Butcher", new CheckListboxItem("Butcher", Resource.BUNDLE_FLOWER));
        put("Giddyup!", new CheckListboxItem("Giddyup!", Resource.BUNDLE_FLOWER));
        put("Shear wool", new CheckListboxItem("Shear wool", Resource.BUNDLE_FLOWER));
        put("Harvest wax", new CheckListboxItem("Harvest wax", Resource.BUNDLE_FLOWER));
        put("Slice up", new CheckListboxItem("Slice up", Resource.BUNDLE_FLOWER));
        put("Chip stone", new CheckListboxItem("Chip stone", Resource.BUNDLE_FLOWER));
        put("Peer into", new CheckListboxItem("Peer into", Resource.BUNDLE_FLOWER));
        put("Break", new CheckListboxItem("Break", Resource.BUNDLE_FLOWER));
        put("Scale", new CheckListboxItem("Scale", Resource.BUNDLE_FLOWER));
    }};

    public final static Map<String, Tex> additonalicons = new HashMap<String, Tex>(16) {{
        put("gfx/terobjs/vehicle/bram", Resource.loadtex("gfx/icons/bram"));
        put("gfx/kritter/toad/toad", Resource.loadtex("gfx/icons/toad"));
        put("gfx/terobjs/vehicle/rowboat", Resource.loadtex("gfx/icons/rowboat"));
        put("gfx/kritter/chicken/chicken", Resource.loadtex("gfx/icons/deadhen"));
        put("gfx/kritter/chicken/rooster", Resource.loadtex("gfx/icons/deadrooster"));
        put("gfx/kritter/rabbit/rabbit", Resource.loadtex("gfx/icons/deadrabbit"));
        put("gfx/kritter/hedgehog/hedgehog", Resource.loadtex("gfx/icons/deadhedgehog"));
        put("gfx/kritter/squirrel/squirrel", Resource.loadtex("gfx/icons/deadsquirrel"));
        put("gfx/terobjs/items/arrow", Resource.loadtex("gfx/icons/arrow"));
        put("gfx/terobjs/items/boarspear", Resource.loadtex("gfx/icons/arrow"));
        put("gfx/kritter/frog/frog", Resource.loadtex("gfx/icons/frog"));
        put("gfx/terobjs/vehicle/wagon", Resource.loadtex("gfx/icons/wagon"));
        put("gfx/terobjs/vehicle/wheelbarrow", Resource.loadtex("gfx/icons/wheelbarrow"));
        put("gfx/terobjs/vehicle/cart", Resource.loadtex("gfx/icons/cart"));
        put("gfx/terobjs/vehicle/wreckingball", Resource.loadtex("gfx/icons/wball"));
        put("gfx/kritter/nidbane/nidbane", Resource.loadtex("gfx/icons/spooky"));
    }};

    public final static HashMap<String, CheckListboxItem> alarmitems = new HashMap<String, CheckListboxItem>(9) {{
        put("gfx/terobjs/herbs/flotsam", new CheckListboxItem("Peculiar Flotsam"));
        put("gfx/terobjs/herbs/chimingbluebell", new CheckListboxItem("Chiming Bluebell"));
        put("gfx/terobjs/herbs/edelweiss", new CheckListboxItem("Edelweiß"));
        put("gfx/terobjs/herbs/bloatedbolete", new CheckListboxItem("Bloated Bolete"));
        put("gfx/terobjs/herbs/glimmermoss", new CheckListboxItem("Glimmermoss"));
        put("gfx/terobjs/herbs/camomile", new CheckListboxItem("Camomile"));
        put("gfx/terobjs/herbs/clay-cave", new CheckListboxItem("Cave Clay"));
        put("gfx/terobjs/herbs/mandrake", new CheckListboxItem("Mandrake Root"));
        put("gfx/terobjs/herbs/seashell", new CheckListboxItem("Rainbow Shell"));
    }};

    public final static Set<String> locres = new HashSet<String>(Arrays.asList(
            "gfx/terobjs/saltbasin",
            "gfx/terobjs/abyssalchasm",
            "gfx/terobjs/windthrow",
            "gfx/terobjs/icespire",
            "gfx/terobjs/woodheart",
            "gfx/terobjs/jotunmussel",
            "gfx/terobjs/guanopile",
            "gfx/terobjs/geyser",
            "gfx/terobjs/claypit",
            "gfx/terobjs/caveorgan",
            "gfx/terobjs/crystalpatch",
            "gfx/terobjs/fairystone",
            "gfx/terobjs/lilypadlotus"));

    public final static Set<String> mineablesStone = new HashSet<String>(Arrays.asList(
            "gneiss",
            "basalt",
            "cinnabar",
            "dolomite",
            "feldspar",
            "flint",
            "granite",
            "hornblende",
            "limestone",
            "marble",
            "porphyry",
            "quartz",
            "sandstone",
            "schist",
            "blackcoal",
            "zincspar",
            "apatite",
            "fluorospar",
            "gabbro",
            "corund",
            "kyanite",
            "mica",
            "microlite",
            "orthoclase",
            "soapstone",
            "sodalite",
            "olivine",
            "alabaster"
    ));


    public final static Set<String> mineablesOre = new HashSet<String>(Arrays.asList(
            "cassiterite",
            "chalcopyrite",
            "malachite",
            "ilmenite",
            "limonite",
            "hematite",
            "magnetite"
    ));

    public final static Set<String> mineablesOrePrecious = new HashSet<String>(Arrays.asList(
            "galena",
            "argentite",
            "hornsilver",
            "petzite",
            "sylvanite",
            "nagyagite"
    ));

    public final static Set<String> mineablesCurios = new HashSet<String>(Arrays.asList(
            "catgold",
            "petrifiedshell",
            "strangecrystal"
    ));

    public final static HashMap<String, CheckListboxItem> disableanim = new HashMap<String, CheckListboxItem>(4) {{
        put("gfx/terobjs/beehive", new CheckListboxItem("Beehives"));
        put("gfx/terobjs/pow", new CheckListboxItem("Fires"));
        put("gfx/terobjs/stockpile-trash", new CheckListboxItem("Full trash stockpiles"));
        put("/idle", new CheckListboxItem("Idle animals"));
    }};

    public final static HashMap<String, String[]> cures = new HashMap<String, String[]>(25) {{
        put("paginae/wound/antburn", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        put("paginae/wound/blunttrauma", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/gauze",
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/camomilecompress",
                "gfx/invobjs/opium"
        });
        put("paginae/wound/bruise", new String[]{
                "gfx/invobjs/leech"
        });
        put("paginae/wound/concussion", new String[]{
                "gfx/invobjs/coldcompress",
                "gfx/invobjs/opium"
        });
        put("paginae/wound/cruelincision", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/stitchpatch",
                "gfx/invobjs/rootfill"
        });
        put("paginae/wound/deepcut", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/stingingpoultice",
                "gfx/invobjs/rootfill",
                "gfx/invobjs/herbs/waybroad",
                "gfx/invobjs/honeybroadaid"
        });
        put("paginae/wound/fellslash", new String[]{
                "gfx/invobjs/gauze"
        });
        put("paginae/wound/nicksnknacks", new String[]{
                "gfx/invobjs/herbs/yarrow",
                "gfx/invobjs/honeybroadaid"
        });
        put("paginae/wound/punchsore", new String[]{
                "gfx/invobjs/mudointment",
                "gfx/invobjs/opium"
        });
        put("paginae/wound/scrapesncuts", new String[]{
                "gfx/invobjs/herbs/yarrow",
                "gfx/invobjs/mudointment",
                "gfx/invobjs/honeybroadaid"
        });
        put("paginae/wound/severemauling", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/opium"
        });
        put("paginae/wound/swollenbump", new String[]{
                "gfx/invobjs/coldcompress",
                "gfx/invobjs/leech",
                "gfx/invobjs/stingingpoultice"
        });
        put("paginae/wound/unfaced", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/mudointment",
                "gfx/invobjs/kelpcream"
        });
        put("paginae/wound/wretchedgore", new String[]{
                "gfx/invobjs/stitchpatch"
        });
        put("paginae/wound/blackeye", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/honeybroadaid",
                "gfx/invobjs/toadbutter"
        });
        put("paginae/wound/bladekiss", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/toadbutter"
        });
        put("paginae/wound/somethingbroken", new String[]{
                "gfx/invobjs/camomilecompress"
        });
        put("paginae/wound/infectedsore", new String[]{
                "gfx/invobjs/camomilecompress",
                "gfx/invobjs/soapbar",
                "gfx/invobjs/opium"
        });
        put("paginae/wound/nastylaceration", new String[]{
                "gfx/invobjs/stitchpatch",
                "gfx/invobjs/toadbutter"
        });
        put("paginae/wound/sealfinger", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/kelpcream"
        });
        put("paginae/wound/coalcough", new String[]{
                "gfx/invobjs/opium"
        });
        put("paginae/wound/beesting", new String[]{
                "gfx/invobjs/kelpcream"
        });
        put("paginae/wound/leechburns", new String[]{
                "gfx/invobjs/toadbutter"
        });
        put("paginae/wound/midgebite", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        put("paginae/wound/sandfleabites", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
    }};

    public static final Map<Long, Pair<String, String>> gridIdsMap = new HashMap<>(58000);

    static {
        Utils.loadprefchklist("disableanim", Config.disableanim);
        Utils.loadprefchklist("alarmitems", Config.alarmitems);

        String p;
        if ((p = getprop("haven.authck", null)) != null)
            authck = Utils.hex2byte(p);

        try {
            InputStream in = ErrorHandler.class.getResourceAsStream("/buildinfo");
            try {
                if (in != null) {
                    java.util.Scanner s = new java.util.Scanner(in);
                    String[] binfo = s.next().split(",");
                    version = binfo[0];
                    gitrev = binfo[1];
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {}

        // populate grid ids map
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("grid_ids.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tknzed = line.split(",");
                try {
                    gridIdsMap.put(Long.parseLong(tknzed[2]), new Pair<>(tknzed[0], tknzed[1]));
                } catch (NumberFormatException nfe) {
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) { // ignored
                }
            }
        }

        loadLogins();
    }

    private static void loadLogins() {
        try {
            String loginsjson = Utils.getpref("logins", null);
            if (loginsjson == null)
                return;
            JSONArray larr = new JSONArray(loginsjson);
            for (int i = 0; i < larr.length(); i++) {
                JSONObject l = larr.getJSONObject(i);
                logins.add(new LoginData(l.get("name").toString(), l.get("pass").toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLogins() {
        try {
            List<String> larr = new ArrayList<String>();
            for (LoginData ld : logins) {
                String ldjson = new JSONObject(ld, new String[] {"name", "pass"}).toString();
                larr.add(ldjson);
            }
            String jsonobjs = "";
            for (String s : larr)
                jsonobjs += s + ",";
            if (jsonobjs.length() > 0)
                jsonobjs = jsonobjs.substring(0, jsonobjs.length()-1);
            Utils.setpref("logins", "[" + jsonobjs + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static URL geturl(String url) {
        if (url.equals(""))
            return null;
        try {
            return new URL(url);
        } catch(java.net.MalformedURLException e) {
            throw(new RuntimeException(e));
        }
    }

    private static void usage(PrintStream out) {
        out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
        out.println("Options include:");
        out.println("  -h                 Display this help");
        out.println("  -d                 Display debug text");
        out.println("  -P                 Enable profiling");
        out.println("  -G                 Enable GPU profiling");
        out.println("  -p FILE            Write player position to a memory mapped file");
        out.println("  -U URL             Use specified external resource URL");
        out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
        out.println("  -u USER            Authenticate as USER (together with -C)");
        out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
    }

    public static void cmdline(String[] args) {
        PosixArgs opt = PosixArgs.getopt(args, "hdPGp:U:r:A:u:C:");
        if (opt == null) {
            usage(System.err);
            System.exit(1);
        }
        for (char c : opt.parsed()) {
            switch (c) {
                case 'h':
                    usage(System.out);
                    System.exit(0);
                    break;
                case 'd':
                    dbtext = true;
                    break;
                case 'P':
                    profile = true;
                    break;
                case 'G':
                    profilegpu = true;
                    break;
                case 'A':
                    int p = opt.arg.indexOf(':');
                    if (p >= 0) {
                        authserv = opt.arg.substring(0, p);
                        authport = Integer.parseInt(opt.arg.substring(p + 1));
                    } else {
                        authserv = opt.arg;
                    }
                    break;
                case 'U':
                    try {
                        resurl = new URL(opt.arg);
                    } catch (java.net.MalformedURLException e) {
                        System.err.println(e);
                        System.exit(1);
                    }
                    break;
                case 'u':
                    authuser = opt.arg;
                    break;
                case 'C':
                    authck = Utils.hex2byte(opt.arg);
                    break;
                case 'p':
                    playerposfile = opt.arg;
                    break;
            }
        }
        if (opt.rest.length > 0) {
            int p = opt.rest[0].indexOf(':');
            if (p >= 0) {
                defserv = opt.rest[0].substring(0, p);
                mainport = Integer.parseInt(opt.rest[0].substring(p + 1));
            } else {
                defserv = opt.rest[0];
            }
        }
    }

    static {
        Console.setscmd("stats", (cons, args) -> dbtext = Utils.parsebool(args[1]));
        Console.setscmd("profile", (cons, args) -> {
            if (args[1].equals("none") || args[1].equals("off")) {
                profile = profilegpu = false;
            } else if (args[1].equals("cpu")) {
                profile = true;
            } else if (args[1].equals("gpu")) {
                profilegpu = true;
            } else if (args[1].equals("all")) {
                profile = profilegpu = true;
            }
        });
    }
}

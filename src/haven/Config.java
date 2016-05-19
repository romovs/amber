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
    public static boolean nopreload = false;
    public static int mainport = 1870;
    public static int authport = 1871;
    public static boolean hideflocomplete = Utils.getprefb("hideflocomplete", false);
    public static boolean hideflovisual = Utils.getprefb("hideflovisual", false);
    public static boolean daylight = Utils.getprefb("daylight", false);
    public static boolean showkinnames = Utils.getprefb("showkinnames", false);
    public static boolean savemmap = Utils.getprefb("savemmap", true);
    public static boolean studylock = Utils.getprefb("studylock", false);
    public static boolean camfree = Utils.getprefb("camfree", false);
    public static boolean chatsave = Utils.getprefb("chatsave", false);
    public static boolean chattimestamp = Utils.getprefb("chattimestamp", false);
    public static boolean alarmunknown = Utils.getprefb("alarmunknown", false);
    public static double alarmunknownvol = Utils.getprefd("alarmunknownvol", 0.32);
    public static boolean alarmred = Utils.getprefb("alarmred", false);
    public static double alarmredvol = Utils.getprefd("alarmredvol", 0.32);
    public static boolean showquality = Utils.getprefb("showquality", false);
    public static boolean qualitywhole = Utils.getprefb("qualitywhole", true);
    public static int badcamsensitivity = Utils.getprefi("badcamsensitivity", 5);
    public static List<LoginData> logins = new ArrayList<LoginData>();
    public static boolean maplocked = Utils.getprefb("maplocked", false);
    public static boolean mapshowgrid = Utils.getprefb("mapshowgrid", false);
    public static boolean mapshowviewdist = Utils.getprefb("mapshowviewdist", false);
    public static boolean disabletiletrans = Utils.getprefb("disabletiletrans", false);
    public static boolean itemmeterbar = Utils.getprefb("itemmeterbar", false);
    public static boolean itempercentage = Utils.getprefb("itempercentage", false);
    public static boolean showprogressperc = Utils.getprefb("showprogressperc", false);
    public static boolean timersalarm = Utils.getprefb("timersalarm", false);
    public static double timersalarmvol = Utils.getprefd("timersalarmvol", 0.8);
    public static boolean quickslots = Utils.getprefb("quickslots", false);
    public static boolean statuswdgvisible = Utils.getprefb("statuswdgvisible", false);
    public static boolean chatalarm = Utils.getprefb("chatalarm", false);
    public static double chatalarmvol = Utils.getprefd("chatalarmvol", 0.8);
    public static boolean studyalarm = Utils.getprefb("studyalarm", false);
    public static double studyalarmvol = Utils.getprefd("studyalarmvol", 0.8);
    public static double sfxchipvol = Utils.getprefd("sfxchipvol", 1.0);
    public static double sfxquernvol = Utils.getprefd("sfxquernvol", 1.0);
    public static double sfxfirevol = Utils.getprefd("sfxfirevol", 1.0);
    public static boolean showcraftcap = Utils.getprefb("showcraftcap", false);
    public static boolean showgobhp = Utils.getprefb("showgobhp", false);
    public static boolean showplantgrowstage = Utils.getprefb("showplantgrowstage", false);
    public static boolean notifykinonline = Utils.getprefb("notifykinonline", false);
    public static boolean showminerad = Utils.getprefb("showminerad", false);
    public static boolean showfarmrad = Utils.getprefb("showfarmrad", false);
    public static boolean showweather = Utils.getprefb("showweather", true);
    public static boolean simplecrops = Utils.getprefb("simplecrops", false);
    public static boolean simpleforage = Utils.getprefb("simpleforage", false);
    public static boolean hidecrops = Utils.getprefb("hidecrops", false);
    public static boolean showfps = Utils.getprefb("showfps", false);
    public static boolean autohearth = Utils.getprefb("autohearth", false);
    public static boolean showplayerpaths = Utils.getprefb("showplayerpaths", false);
    public static boolean showstudylefttime = Utils.getprefb("showstudylefttime", false);
    public static boolean showinvonlogin = Utils.getprefb("showinvonlogin", false);
    public static boolean autopick = Utils.getprefb("autopick", false);
    public static boolean autoharvest = Utils.getprefb("autoharvest", false);
    public static boolean autosplit = Utils.getprefb("autosplit", false);
    public static boolean autoeat = Utils.getprefb("autoeat", false);
    public static boolean autokill = Utils.getprefb("autokill", false);
    public static boolean autoslice = Utils.getprefb("autoslice", false);
    public static boolean autoclean = Utils.getprefb("autoclean", false);
    public static boolean autopluck = Utils.getprefb("autopluck", false);
    public static boolean autoskin = Utils.getprefb("autoskin", false);
    public static boolean autobutcher = Utils.getprefb("autobutcher", false);
    public static boolean autoflay = Utils.getprefb("autoflay", false);
    public static boolean runonlogin = Utils.getprefb("runonlogin", false);
    public static Coord chatsz = Utils.getprefc("chatsz", new Coord(683, 111));
    public static boolean autostudy = Utils.getprefb("autostudy", true);
    public static boolean showdmgop = Utils.getprefb("showdmgop", false);
    public static boolean showdmgmy = Utils.getprefb("showdmgmy", false);
    public static boolean hidegobs = Utils.getprefb("hidegobs", false);
    public static boolean qualitybg = Utils.getprefb("qualitybg", false);
    public static int qualitybgtransparency = Utils.getprefi("qualitybgtransparency", 5);
    public static boolean showwearbars = Utils.getprefb("showwearbars", false);
    public static boolean tilecenter = Utils.getprefb("tilecenter", false);
    public static boolean userazerty = Utils.getprefb("userazerty", false);
    public static boolean hlightcuropp = Utils.getprefb("hlightcuropp", false);
    public static boolean reversebadcamx = Utils.getprefb("reversebadcamx", false);
    public static boolean reversebadcamy = Utils.getprefb("reversebadcamy", false);
    public static boolean showservertime = Utils.getprefb("showservertime", false);
    public static boolean enabletracking = Utils.getprefb("enabletracking", false);
    public static boolean enablecrime = Utils.getprefb("enablecrime", false);
    public static boolean resinfo = Utils.getprefb("resinfo", false);
    public static boolean showanimalrad = Utils.getprefb("showanimalrad", false);
    public static boolean hwcursor = Utils.getprefb("hwcursor", false);
    public static boolean showboundingboxes = Utils.getprefb("showboundingboxes", false);
    public static boolean alarmonforagables = Utils.getprefb("alarmonforagables", false);
    public static double alarmonforagablesvol = Utils.getprefd("alarmonforagablesvol", 0.8);
    public static boolean alarmbears = Utils.getprefb("alarmbears", true);
    public static double alarmbearsvol = Utils.getprefd("alarmbearsvol", 0.8);
    public static boolean alarmlocres = Utils.getprefb("alarmlocres", false);
    public static double alarmlocresvol = Utils.getprefd("alarmlocresvol", 0.8);
    public static boolean alarmtroll = Utils.getprefb("alarmtroll", false);
    public static double alarmtrollvol = Utils.getprefd("alarmtrollvol", 0.8);
    public static boolean alarmmammoth = Utils.getprefb("alarmmammoth", true);
    public static double alarmmammothvol = Utils.getprefd("alarmmammothvol", 0.8);
    public static boolean showcooldown = Utils.getprefb("showcooldown", false);
    public static boolean nodropping = Utils.getprefb("nodropping", false);
    public static boolean fbelt = Utils.getprefb("fbelt", false);
    public static boolean histbelt = Utils.getprefb("histbelt", false);
    public static boolean dropore = Utils.getprefb("dropore", true);
    public static boolean showdframestatus = Utils.getprefb("showdframestatus", false);
    public static boolean enableorthofullzoom = Utils.getprefb("enableorthofullzoom", false);
    public static boolean partycircles =  Utils.getprefb("partycircles", false);
    public static boolean noquests =  Utils.getprefb("noquests", false);
    public static boolean alarmbram =  Utils.getprefb("alarmbram", false);
    public static double alarmbramvol = Utils.getprefd("alarmbramvol", 1.0);
    public static boolean instantflowermenu =  Utils.getprefb("instantflowermenu", false);
    public static double sfxwhipvol = Utils.getprefd("sfxwhipvol", 1.0);
    public static boolean showarchvector =  Utils.getprefb("showarchvector", false);
    //public static boolean showcddelta =  Utils.getprefb("showcddelta", false);
    public static boolean disabledrinkhotkey =  Utils.getprefb("disabledrinkhotkey", false);
    public static boolean autologout =  Utils.getprefb("autologout", false);
    public static int combatkeys =  Utils.getprefi("combatkeys", 0);
    public static boolean logcombatactions =  Utils.getprefb("logcombatactions", false);
    public static boolean autopickmussels =  Utils.getprefb("autopickmussels", false);
    public static boolean confirmmagic =  Utils.getprefb("confirmmagic", true);
    public static boolean altfightui =  Utils.getprefb("altfightui", false);
    public static boolean combshowkeys =  Utils.getprefb("combshowkeys", true);
    public static boolean combaltopenings =  Utils.getprefb("combaltopenings", false);
    public static boolean studyhist =  Utils.getprefb("studyhist", false);
    public static boolean studybuff =  Utils.getprefb("studybuff", false);
    public static int zkey =  Utils.getprefi("zkey", KeyEvent.VK_Z);
    public static boolean disableterrainsmooth =  Utils.getprefb("disableterrainsmooth", false);
    public static boolean disableelev =  Utils.getprefb("disableelev", false);
    private final static Map<String, Integer> defFontSzGlobal =  new HashMap<String, Integer>(3) {{
        put("zh", 16);
        put("en", 11);
        put("ru", 11);
    }};
    private final static Map<String, Integer> defFontSzButton =  new HashMap<String, Integer>(3) {{
        put("zh", 14);
        put("en", 12);
        put("ru", 12);
    }};
    private final static Map<String, Integer> defFontSzAttr =  new HashMap<String, Integer>(3) {{
        put("zh", 14);
        put("en", 14);
        put("ru", 13);
    }};
    public static int fontsizeglobal = Utils.getprefi("fontsizeglobal", defFontSzGlobal.get(Resource.language));
    public static int fontsizebutton = Utils.getprefi("fontsizebutton", defFontSzButton.get(Resource.language));
    public static int fontsizewndcap = Utils.getprefi("fontsizewndcap", 14);
    public static int fontsizeattr = Utils.getprefi("fontsizeattr", defFontSzAttr.get(Resource.language));
    public static int fontsizechat = Utils.getprefi("fontsizechat", 14);
    public static boolean pf = false;
    public static String playerposfile;
    public static byte[] authck = null;
    public static String prefspec = "hafen";
    public static String version;
    public static String gitrev;

    public final static String chatfile = "chatlog.txt";
    public static PrintWriter chatlog = null;

    public final static HashMap<String, CheckListboxItem> boulders = new HashMap<String, CheckListboxItem>(13) {{
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
    }};

    public final static HashMap<String, CheckListboxItem> bushes = new HashMap<String, CheckListboxItem>(22) {{
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
    }};

    public final static HashMap<String, CheckListboxItem> trees = new HashMap<String, CheckListboxItem>(46) {{
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
        put("oldtrunk", new CheckListboxItem("Oldtrunk"));
        put("conkertree", new CheckListboxItem("Conker Tree"));
        put("mulberry", new CheckListboxItem("Mulberry"));
        put("sweetgum", new CheckListboxItem("Sweetgum"));
        put("pine", new CheckListboxItem("Pine"));
        put("birch", new CheckListboxItem("Birch"));
        put("planetree", new CheckListboxItem("Plane Tree"));
        put("quincetree", new CheckListboxItem("Quince"));
        put("almondtree", new CheckListboxItem("Almond"));
    }};

    public final static HashMap<String, CheckListboxItem> icons = new HashMap<String, CheckListboxItem>(21) {{
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
    }};

    public final static Map<String, Tex> additonalicons = new HashMap<String, Tex>(25) {{
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
        // grayscale icons for dead animals
        put("gfx/kritter/badger/badger", Resource.loadtex("gfx/icons/badger"));
        put("gfx/kritter/bear/bear", Resource.loadtex("gfx/icons/bear"));
        put("gfx/kritter/boar/boar", Resource.loadtex("gfx/icons/boar"));
        put("gfx/kritter/fox/fox", Resource.loadtex("gfx/icons/fox"));
        put("gfx/kritter/horse/horse", Resource.loadtex("gfx/icons/horse"));
        put("gfx/kritter/lynx/lynx", Resource.loadtex("gfx/icons/lynx"));
        put("gfx/kritter/mammoth/mammoth", Resource.loadtex("gfx/icons/mammoth"));
        put("gfx/kritter/moose/moose", Resource.loadtex("gfx/icons/moose"));
        put("gfx/kritter/reddeer/reddeer", Resource.loadtex("gfx/icons/reddeer"));
        put("gfx/kritter/troll/troll", Resource.loadtex("gfx/icons/troll"));
        put("gfx/kritter/walrus/walrus", Resource.loadtex("gfx/icons/walrus"));
    }};

    public final static Set<String> dangerousgobres = new HashSet<String>(Arrays.asList(
            "gfx/kritter/bat/bat", "gfx/kritter/bear/bear", "gfx/kritter/boar/boar", "gfx/kritter/lynx/lynx", "gfx/kritter/walrus/walrus",
            "gfx/kritter/badger/badger"));

    public final static Set<String> foragables = new HashSet<String>(Arrays.asList(
            "gfx/terobjs/herbs/flotsam", "gfx/terobjs/herbs/chimingbluebell", "gfx/terobjs/herbs/edelweiss",
            "gfx/terobjs/herbs/bloatedbolete", "gfx/terobjs/herbs/glimmermoss"));

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
            "gfx/terobjs/crystalpatch"));

    public final static ArrayList<Pair<String, String>> disableanim = new ArrayList<Pair<String, String>>(4) {{
        add(new Pair<String, String>("Beehives", "gfx/terobjs/beehive"));
        add(new Pair<String, String>("Fires", "gfx/terobjs/pow"));
        add(new Pair<String, String>("Full trash stockpiles", "gfx/terobjs/stockpile-trash"));
        add(new Pair<String, String>("Idle animals", "/idle"));
    }};
    public final static Set<String> disableanimSet = new HashSet<String>(disableanim.size());
    public static boolean fontaa = Utils.getprefb("fontaa", false);

    public final static HashMap<String, String[]> cures = new HashMap<String, String[]>(14) {{
        put("paginae/wound/antburn", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        put("paginae/wound/blunttrauma", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/gauze"
        });
        put("paginae/wound/bruise", new String[]{
                "gfx/invobjs/leech"
        });
        put("paginae/wound/concussion", new String[]{
                "gfx/invobjs/coldcompress"
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
                "gfx/invobjs/herbs/waybroad"
        });
        put("paginae/wound/fellslash", new String[]{
                "gfx/invobjs/gauze"
        });
        put("paginae/wound/nicksnknacks", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        put("paginae/wound/punchsore", new String[]{
                "gfx/invobjs/mudointment"
        });
        put("paginae/wound/scrapesncuts", new String[]{
                "gfx/invobjs/herbs/yarrow",
                "gfx/invobjs/mudointment"
        });
        put("paginae/wound/severemauling", new String[]{
                "gfx/invobjs/hartshornsalve"
        });
        put("paginae/wound/swollenbump", new String[]{
                "gfx/invobjs/coldcompress",
                "gfx/invobjs/leech",
                "gfx/invobjs/stingingpoultice"
        });
        put("paginae/wound/unfaced", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/mudointment"
        });
        put("paginae/wound/wretchedgore", new String[]{
                "gfx/invobjs/stitchpatch"
        });
    }};

    static {
        Collections.sort(disableanim, (o1, o2) -> o1.a.compareTo(o2.a));

        String[] disableanimsel = Utils.getprefsa("disableanim", null);
        if (disableanimsel != null) {
            for (String selname : disableanimsel) {
                for (Pair<String, String> selpair : Config.disableanim) {
                    if (selpair.a.equals(selname)) {
                        Config.disableanimSet.add(selpair.b);
                        break;
                    }
                }
            }
        }

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

/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
/*
 * @progname       ps-circle.ll
 * @version        2.6.2 of 2003-12-10
 * @author         Jim Eggert (eggertj@ll.mit.edu), Henry Sikkema (hasikkema@yahoo.ca)
 * @category
 * @output         PostScript
 * @description

		   Print a five to ten-generation ancestry circle chart in PostScript.

Version 2.5, December 2002  		by Henry Sikkema (hasikkema@yahoo.ca)
Version 1.1, September 2002
Version 1, 15 September 1993		by Jim Eggert (eggertj@ll.mit.edu)

This program generates a basic five to ten-generation ancestry circle chart.
Its output is a Postscript file specifying the chart.  This program
uses a modified version of the CIRC.PS program written by
David Campbell and John Dunn.

You must choose the number of generations to print (5 - 10 generations).
For a larger number of generations the print may get VERY small but may
be enlarged using a program such as Corel Draw or other programs and printed
onto a larger paper or printed in parts.

You have the option of creating a colour gradient background or an
alternating colour scheme for males and females.  The gradient does take a while to
process since all I do is to draw and fill circles with decreasing radius.  Please
email (see above) me if you know how to make a better gradient. To change the colours
you need to modify the resulting Postscript file.  The colours are given
in RGB format.  The default colors are RED for female text and BLUE for male text,
the backgrounds are opposite: light blue to female box fillin and light red for
male box fill in.  The default colour gradient is a light brown on the inside
to a darker brown on the outside for an attempted antique look.

http://sikkema.netfirms.com/family/tree/ps-circle/ps-circle.html

The data currently printed depends on the level number and on the length
of the names.  When there are more than one given name (i.e. second and
third names), if they are too long they are eliminated.

The full birth date is printed if there is no known death date.  In this
case, the date is preceeded by 'b:' to indicate that the date is a birth,
for example (b: 12 Sep 1901); the only exception is on level one where
the 'b:' is dropped for the sake of space.  When only a death date is known,
it will be preceeded by a dash, for example (-1978).  In every other case, only
the birth and death years are printed, for example (1901-1978).

The case (capitalization) of the names are not changed at all from the GEDCOM file.

This data is currently printed:
            First line            Second Line          Third line
-----------------------------------------------------------------
Level  1:   Given Names           Surname              Dates
Level  2:   Full Name             Dates                ---
Level  3:   Full Name             Dates                ---
Level  4:   First Name            Surname              Dates
Level  5:   First Name            Surname              Dates
Level  6:   First Name            Surname              Dates
Level  7:   Full Name             Dates                ---
Level  8:   Full Name             Dates                ---
Level  9:   Full Name, Dates      ---                  ---
Level 10:   Full Name, Dates      ---                  ---

Future:  - color coding based on country of origin.  (Robert Simms)
         - marriage date estimate
         - proper zooming in Ghostview
         - eliminate blank pages with small radius
*/
import genj.gedcom.Fam;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.PropertyDate;
import genj.gedcom.PropertySex;
import genj.gedcom.time.PointInTime;
import genj.report.Report;
import genj.window.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * GenJ - ReportPSCirc
 * from LifeLines
todo:
- majuscules
- issuer
- rapport a partir de la famille
 */
public class ReportLinesCirc extends Report {
    private final static Charset CHARSET = Charset.forName("ISO-8859-1");

    //==== generations
    private int maxlevel=10;
    public int umaxlevel=2;
    public String umaxlevels[] = { i18n("umaxlevel.5"),
				     i18n("umaxlevel.6"),
				     i18n("umaxlevel.7"),
				     i18n("umaxlevel.8"),
				     i18n("umaxlevel.9"),
				    i18n("umaxlevel.10")};

    //==== report size
    private int x_pages=1;
    private int y_pages=1;
    public int unb_pages=0;
    public String unb_pagess[] = { i18n("unb_pages.0"),
				     i18n("unb_pages.1"),
				     i18n("unb_pages.2"),
				     i18n("unb_pages.3"),
				     i18n("unb_pages.4"),
				     i18n("unb_pages.5"),
				    i18n("unb_pages.6")};
    private int radius=0;

    //==== text colour
    public int colourtext =1;
    public String colourtexts[] = { i18n("colourtext.0"),
				     i18n("colourtext.1")};

    //==== Background
    public int colouroption =0;
    public String colouroptions[] = { i18n("colouroption.0"),
				     i18n("colouroption.1"),
				    i18n("colouroption.2")};
    private boolean alternating;
    private boolean gradient;

    //==== Date Fromat
    public int dateformat=1;
    public String dateformats[] = { i18n("dateformat.0"),
				     i18n("dateformat.1"),
				    i18n("dateformat.2")};

    //==== Marriag Date?
    private boolean marrest;
    private boolean printmarr;
    public boolean uprintmarr=true;
    /*    public String uprintmarrs[] = { i18n("uprintmarr.2"),
				     i18n("uprintmarr.1"),
				    i18n("uprintmarr.0")};
    */

    //==== character Encoding 
    public int uenc_choice=0;
    private int enc_choice=1;
    public String uenc_choices[] = { i18n("uenc_choice.0"),
				     i18n("uenc_choice.1"),
				    i18n("uenc_choice.2")};

    //==== Font
    public int ufont_name=0;
    public String ufont_names[] = { i18n("ufont_name.0"),
				     i18n("ufont_name.1"),
				     i18n("ufont_name.2"),
				     i18n("ufont_name.3"),
				     i18n("ufont_name.4"),
				    i18n("ufont_name.5")};
    private String font_name = "Helvetica";

    //==== Report Date
    public boolean  printdate=false;

    /******************/
    private PrintWriter out;
    private int indicentre=1;
    private int numindilines=0;
    private int nummarr=-1;
    private final String version = "genj 1.0";
    private boolean transparent;
    /**
     * Helper - Create a PrintWriter wrapper for output stream
     */
    private PrintWriter getWriter(OutputStream out) {
	return new PrintWriter(new OutputStreamWriter(out, CHARSET));
    }

    /**
     * Main for argument individual
     */
    public void start(Indi indi) {
	
      // init options
      initUserOptions();

      // ask for file
      File file = getFileFromUser(i18n("output.file"), WindowManager.TXT_OK,true);
      if (file == null)
        return ;
      
      // open output stream
      try{
        out = getWriter(new FileOutputStream(file));
      }catch(IOException ioe){
        System.err.println("IO Exception!");
        ioe.printStackTrace();
        return; //abort
      }

      // generate output
      main(indi,null);
      //	out.println("showpage");
      out.flush();
      out.close();

      // show file the result to the user
      showFileToUser(file);

    }

    private void initUserOptions(){
	/*	marrest=(uprintmarr == 1);
	  printmarr=(uprintmarr != 0);*/
	marrest=false;
	printmarr=true;
	alternating=(colouroption == 0);
	gradient=(colouroption == 2);
	maxlevel=umaxlevel + 5;
	enc_choice=1+uenc_choice;
	font_name = i18n("ufont_name."+ufont_name);
	transparent = (colouroption != 0);
	switch (unb_pages){
	case 1:x_pages=2;y_pages=1;break;
	case 2:x_pages=2;y_pages=2;break;
	case 3:x_pages=3;y_pages=2;break;
	case 4:x_pages=4;y_pages=3;break;
	case 5:x_pages=5;y_pages=4;break;
	case 6:x_pages=6;y_pages=4;break;
	default:x_pages=1;y_pages=1;break;
	}
	    
    }

private String put_given_name(Indi person,int length){
/*	if (ne(trimname(person,add(length,strlen(surname(person)),1)),"")){set(l,trimname(person,add(length,strlen(surname(person)),1)))}else{set(l,givens(person))}
	if(ne(trim(l,sub(index(l,surname(person),1),2)),"")){set(n,trim(l,sub(index(l,surname(person),1),2)))}
	call removeparentheses(n)*/

    return escapePsString(truncateString(givens(person),length));
}

    private String put_full_name(Indi person,int sur_upper,int n_order,int length){
	return fullname(person,sur_upper,n_order,length);

}
    private void endline(int ahnen,int offset,int info,int max){
	out.println(") "+ahnen+" "+offset+" "+info+" "+max+"} addind");
    }
    
    private void putperson(Fam family, Indi person, int level, int ahnen, int info, int dateformat) {
	
	int[] levellength = {25,26,23,16,15,15,21,21,21,21,21};
	PersonCell pCell = new PersonCell();
	if (eq(level,1)) {
	    numindilines += pCell.add(surname(person),numindilines);
	    numindilines += pCell.add(put_given_name(person,levellength[level]),numindilines);
	    numindilines += pCell.add(getDateString(birth(person),death(person),dateformat),numindilines);
	    out.print(pCell.get(ahnen,info));
	}else if(and(ge(level,2),le(level,6))){
	    numindilines += pCell.add(surname(person),numindilines);
	    numindilines += pCell.add(put_given_name(person,levellength[level]),numindilines);
	    numindilines += pCell.add(getDateString(birth(person),death(person),dateformat),numindilines);
	    out.print(pCell.get(ahnen,info));
	}else if(or(eq(level,7),eq(level,8))){
	    numindilines += pCell.add(put_full_name(person,0,1,getel(levellength,level)),numindilines);
	    numindilines += pCell.add(getDateString(birth(person),death(person),dateformat),numindilines);
	    out.print(pCell.get(ahnen,info));
	}else if(ge(level,9)){
	    numindilines += pCell.add(put_full_name(person,0,1,getel(levellength,level))+
				      getDateString(birth(person),death(person),dateformat)
				      ,numindilines);
	    out.print(pCell.get(ahnen,info));
	}
        if (printmarr){
	    // marriage date estimation does not yet work!
	    if (marrest && false){ 
		//		if (ne(date(marriage(family)),"")){if (eq("M",sex(person))){set(nummarr,add(nummarr,1))d(nummarr)" {(" stddate(marriage(family)) ") " d(ahnen) " " d(info)"} addmarr\n"}}
	    }else{
		if (marriage(family)!=null){
		    if (eq("M",sex(person))){
			nummarr++;
			out.println(d(nummarr)+" {(" +date(marriage(family)) +") "+ d(ahnen)+ " "+ d(info)+"} addmarr");
		    }
		}
	    }
	}
    }

    private void semicirc(Fam family, Indi person, int level, int ahnen, int info, int maxlevel, int dateformat) {
	if (person!= null && le(level,maxlevel)) {
	    putperson(family,person,level,ahnen,info,dateformat);
	    int nextlevel = add(level,1);
	    int nextahnen = mul(ahnen,2);
	    semicirc(parents(person), father(person), nextlevel, nextahnen, info,maxlevel,dateformat);
	    semicirc(parents(person), mother(person), nextlevel, add(nextahnen,1), info,maxlevel,dateformat);
	}
    }

    /*
     * LifeLines compatibility helper functions
     */
    boolean le(int a, int b){ return (a<=b);}
    boolean gt(int a, int b){ return (a>b);}
    boolean ge(int a, int b){ return (a>=b);}
    boolean ne(int a, int b){ return (a!=b);}
    boolean eq(int a, int b){ return (a==b);}
    boolean eq(String a, String b){ return (a==b);}
    boolean or(boolean a, boolean b){ return (a||b);}
    boolean and(boolean a, boolean b){ return (a&&b);}
    int add(int a, int b){return a+b;}
    int mul(int a, int b){return a*b;}
    int getel(int[] intarray, int offset){
	if (offset>= intarray.length) return 0;
	else return intarray[offset];
    }
    String d(int i){ return(""+i);}
    String d(boolean b){ return(b?"1":"0");}
    PropertyDate birth(Indi entity){
		// prop exists?
	if (entity==null)
	    return null;
	Property prop = entity.getProperty("BIRT");
	if (prop==null)
	    return null;
	
	return (PropertyDate) prop.getProperty("DATE");
    }
    PropertyDate death(Indi entity){
		// prop exists?
	if (entity==null)
	    return null;
	Property prop = entity.getProperty("DEAT");
	if (prop==null)
	    return null;
	
	return (PropertyDate) prop.getProperty("DATE");
    }
    String date(PropertyDate date){ 
	if (date != null ) {
	    return date.getDisplayValue();
	} else {
	    return "";
	}
    }
    String year(PropertyDate date){ 
	if (date != null ) {
	    return date.getStart().isValid()?""+date.getStart().getYear():"????";
	} else {
	    return "";
	}
    }
    PropertyDate marriage(Fam family){
	Property prop;
	if (family == null) return null;
	prop = family.getProperty("MARR");
	if (prop==null) return null;
	return  (PropertyDate) prop.getProperty("DATE");
    }

    Indi husband(Fam fam){return (fam == null)?null:fam.getHusband();}
    Indi wife(Fam fam){return (fam == null)?null:fam.getWife();}
    Fam parents(Indi person) {return (person == null)?null:person.getFamilyWhereBiologicalChild();}
    Indi father(Indi person){return (person == null)?null:husband(parents(person));}
    Indi mother(Indi person){return (person == null)?null:wife(parents(person));}
    String sex(Indi indi){return((indi.getSex() == PropertySex.MALE )? "M":"F");}
    /*
      Fullname returns the name of a person in a variety of formats. 
      If the second parameter is true the surname is shown in upper case; 
      otherwise the surname is as in the record. 
      If the third parameter is true the parts of the name are shown in the order
      as found in the record; otherwise the surname is given first, followed 
      by a comma, followed by the other name parts. 
      The fourth parameter specifies the maximum length field that can be used
      to show the name; various conversions occur 
      if it is necessary to shorten the name to fit this length.
    */
    private String fullname(Indi indi,int isUpper,int type,int length){
	if (indi.getName().length()<=length){
	    return escapePsString(indi.getName());
	} else {
	    return escapePsString(truncateString(indi.getName(),length));
	}
    }
    String surname(Indi indi){
	if (indi == null) return "";
	return escapePsString(truncateString(indi.getLastName(),30));
    }
    String givens(Indi indi){
	if (indi == null) return "";
	return indi.getFirstName();
    }

    private String truncateString(String s, int length){
	if (s.length()<=length){
	    return s;
	} else {
	    return s.substring(0,length);
	}
    }

    private String escapePsString(String s){
	String result;
	result = s.replaceAll("\\\\","\\\\\\\\");
	result = result.replaceAll("\\(","\\\\(");
	result = result.replaceAll("\\)","\\\\)");
	return result;
    }
    
    private void putpageprintouts(int xn,int yn){
	int page_num = 0;
	int yi = yn-1;
	int yi_ord;
	int xi;
	
	while(yi >= 0) {
	    yi_ord = yn-1-yi;
	    xi=xn- 1;
	    while(xi>=0) {
		page_num = add(page_num, 1);
		out.println("%%Page: "+page_num+ " " +page_num+ "\n"+
			    "cleartomark mark\n"+
			    xi+ " " +yi +" print-a-page\n"+
			    "showpage\n");
		xi= xi- 1;
	    }
	    yi= yi- 1;
	}
    }

    private void printfile(){
	out.println("%!PS-Adobe-3.0");
	out.println("%%Title: (PS-CIRCLE.PS - Circular Genealogical Pedigree Chart in Postscript format)");
	out.println("%%Creator: " +version+ " - a Lifelines circle ancestry chart report generator");
	out.println("%%CreationDate: "/*+ stddate(gettoday())*/);
	out.println("%%Pages: "+d(mul(x_pages,y_pages)));
	out.println("%%PageOrder: Ascend");
	out.println("%%Orientation: Portrait");
	out.println("%%EndComments\n");

	out.println("%%BeginDefaults");
	out.println("%%ViewingOrientation: 1 0 0 1");
	out.println("%%EndDefaults\n");
	
	out.println("%%BeginProlog\n");
	out.println("%   much of the code involved with font encoding and with multipaging");
	out.println("%   is borrowed from Robert Simms <rsimms@ces.clemson.edu>\n");

	out.println("%page margins");
	out.println("/margin_top 20 def");
	out.println("/margin_bottom 20 def");
	out.println("/margin_left 20 def");
	out.println("/margin_right 20 def\n");

	out.println("%number of pages in each direction");

	out.println("/xpages "+d(x_pages)+" def");
	out.println("/ypages "+d(y_pages)+" def\n");

	out.println("/fontname /"+font_name+" def\n");

	out.println("/portrait true def\n");

	out.println("/inch {72 mul} def\n");

	out.println("/*SF {                 % Complete selectfont emulation");
	out.println("  exch findfont exch");
	out.println("  dup type /arraytype eq {makefont}{scalefont} ifelse setfont");
	out.println("} bind def\n");

	out.println("/BuildRectPath{");
	out.println("	dup type dup /integertype eq exch /realtype eq or{");
	out.println("			4 -2 roll moveto 	%Operands are: x y width height");
	out.println("			dup 0 exch rlineto");
	out.println("			exch 0 rlineto");
	out.println("			neg 0 exch rlineto");
	out.println("			closepath");
	out.println("		}{");
	out.println("			dup length 4 sub 0 exch 4 exch{");
	out.println("				1 index exch 4 getinterval aload pop");
	out.println("				BuildRectPath");
	out.println("			}for");
	out.println("			pop");
	out.println("		}ifelse");
	out.println("} bind def\n");

	out.println("/*RC { gsave newpath BuildRectPath fill grestore } bind def\n");

	out.println("% install Level 2 emulations, or substitute built-in Level 2 operators");
	out.println("/languagelevel where");
	out.println("  {pop languagelevel}{1} ifelse");
	out.println("2 lt {");
	out.println("  /RC /*RC load def");
	out.println("  /SF /*SF load def");
	out.println("}{");
	out.println("  /RC /rectclip load def      % use RC instead of rectclip");
	out.println("  /SF /selectfont load def    % use SF instead of selectfont");
	out.println("} ifelse\n");

	out.println("%Coordinate conversion utilities");
	out.println("/polar { %(ang rad) -> (x y)");
	out.println("	/rad exch def		/ang exch def");
	out.println("	/x rad ang cos mul def		/y rad ang sin mul def");
	out.println("	x y");
	out.println("} def\n");

	out.println("/midang {");
	out.println("	/inf exch def");
	out.println("	inf 1 eq {360 2 maxlevel exp div mul -90.0 add}           %for first level male, go counter clockwise from bottom");
	out.println("				{360 2 maxlevel exp div mul 90.0 add} ifelse     %for first level female, go clockwise from bottom");
	out.println("} def\n");

	out.println("%Shortcut macros");
	out.println("/m {moveto} def		/l {lineto} def\n");

	out.println("%Constants");
	out.println("/pi 3.14159265358979 def");
	out.println("/ptsize 10 def");
	out.println("/offset ptsize 1.25 mul neg def\n");

	out.println("/radius {4.0 7.0 div exch indicentre add mul inch} def");

	out.println("%begin font encoding   borrowed from Robert Simms");
	if(ne(enc_choice, 0)) {
	    out.println("/encvecmod* {  % on stack should be /Encoding and an encoding array");
	    out.println("	% make an array copy so we don't try to modify the original via pointer");
	    out.println("	dup length array copy");
	    out.println("	encvecmod aload length dup 2 idiv exch 2 add -1 roll exch");
	    out.println("	{dup 4 2 roll put}");
	    out.println("	repeat");
	    out.println("} def");
	    out.println("/reenc {");
	    out.println("	findfont");
	    out.println("	dup length dict begin");
	    out.println("		{1 index /FID eq {pop pop} {");
	    out.println("			1 index /Encoding eq {");
	    out.println("					encvecmod* def");
	    out.println("				}{def} ifelse");
	    out.println("			} ifelse");
	    out.println("		} forall");
	    out.println("		currentdict");
	    out.println("	end");
	    out.println("	definefont pop");
	    out.println("} def");
	}
	if(eq(enc_choice, 1)) {
	    out.println("% Adjust the font so that it is iso-8859-1 compatible");
	    out.println("/languagelevel where {pop languagelevel}{1} ifelse 2 ge {");
	    out.println("	/encvecmod* {pop ISOLatin1Encoding} def	% Use built-in ISOLatin1Encoding if PS interpreter is Level 2");
	    out.println("}{");
	    /* This array indicates changes to go from the Standard Encoding Vector
	       to the ISOLatin1 Encoding Vector for ISO-8859-1 compatibility,
	       according to the PostScript Language Reference Manual, 2nd ed.
	       The characters from A0 to FF are essential for 8859-1 conformance.
	    */
	    out.println("	/encvecmod [");
	    out.println("		16#90 /dotlessi   16#91 /grave        16#92 /acute      16#93 /circumflex");
	    out.println("		16#94 /tilde      16#95 /macron       16#96 /breve      16#97 /dotaccent");
	    out.println("		16#98 /dieresis   16#99 /.notdef      16#9a /ring       16#9b /cedilla");
	    out.println("		16#9c /.notdef    16#9d /hungarumlaut 16#9e /ogonek     16#9f /caron");
	    out.println("		16#a0 /space      16#a1 /exclamdown   16#a2 /cent       16#a3 /sterling");
	    out.println("		16#a4 /currency   16#a5 /yen         16#a6 /brokenbar   16#a7 /section");
	    out.println("		16#a8 /dieresis   16#a9 /copyright   16#aa /ordfeminine 16#ab /guillemotleft");
	    out.println("		16#ac /logicalnot 16#ad /hyphen      16#ae /registered  16#af /macron");
	    out.println("		16#b0 /degree     16#b1 /plusminus   16#b2 /twosuperior 16#b3 /threesuperior");
	    out.println("		16#b4 /acute      16#b5 /mu          16#b6 /paragraph    16#b7 /periodcentered");
	    out.println("		16#b8 /cedilla    16#b9 /onesuperior 16#ba /ordmasculine 16#bb /guillemotright");
	    out.println("		16#bc /onequarter 16#bd /onehalf    16#be /threequarters 16#bf /questiondown");
	    out.println("		16#c0 /Agrave      16#c1 /Aacute    16#c2 /Acircumflex 16#c3 /Atilde");
	    out.println("		16#c4 /Adieresis   16#c5 /Aring     16#c6 /AE          16#c7 /Ccedilla");
	    out.println("		16#c8 /Egrave      16#c9 /Eacute    16#ca /Ecircumflex 16#cb /Edieresis");
	    out.println("		16#cc /Igrave      16#cd /Iacute    16#ce /Icircumflex 16#cf /Idieresis");
	    out.println("		16#d0 /Eth         16#d1 /Ntilde    16#d2 /Ograve      16#d3 /Oacute");
	    out.println("		16#d4 /Ocircumflex 16#d5 /Otilde    16#d6 /Odieresis   16#d7 /multiply");
	    out.println("		16#d8 /Oslash      16#d9 /Ugrave    16#da /Uacute      16#db /Ucircumflex");
	    out.println("		16#dc /Udieresis   16#dd /Yacute    16#de /Thorn       16#df /germandbls");
	    out.println("		16#e0 /agrave      16#e1 /aacute    16#e2 /acircumflex 16#e3 /atilde");
	    out.println("		16#e4 /adieresis   16#e5 /aring     16#e6 /ae          16#e7 /ccedilla");
	    out.println("		16#e8 /egrave      16#e9 /eacute    16#ea /ecircumflex 16#eb /edieresis");
	    out.println("		16#ec /igrave      16#ed /iacute    16#ee /icircumflex 16#ef /idieresis");
	    out.println("		16#f0 /eth         16#f1 /ntilde    16#f2 /ograve      16#f3 /oacute");
	    out.println("		16#f4 /ocircumflex 16#f5 /otilde    16#f6 /odieresis   16#f7 /divide");
	    out.println("		16#f8 /oslash      16#f9 /ugrave    16#fa /uacute      16#fb /ucircumflex");
	    out.println("		16#fc /udieresis   16#fd /yacute    16#fe /thorn       16#ff /ydieresis");
	    out.println("	] def");
	    out.println("} ifelse\n");
	} else if(eq(enc_choice, 2)) {
	    /* The following array specifies changes to make to a font encoding
	       to make characters A0 through FF match the ISO Latin alphabet no. 2
	       This will work as long as there are instructions in the font for
	       drawing the glyphs named here.  Missing glyphs would be
	       substituted with /.notdef from the font by the PostScript interpreter.
	    */
	    out.println("/encvecmod [");
	    out.println("	16#a0 /space     16#a1 /Aogonek 16#a2 /breve     16#a3 /Lslash");
	    out.println("	16#a4 /currency  16#a5 /Lcaron  16#a6 /Sacute    16#a7 /section");
	    out.println("	16#a8 /dieresis  16#a9 /Scaron  16#aa /Scedilla  16#ab /Tcaron");
	    out.println("	16#ac /Zacute    16#ad /hyphen  16#ae /Zcaron    16#af /Zdotaccent");
	    out.println("	16#b0 /degree    16#b1 /aogonek 16#b2 /ogonek    16#b3 /lslash");
	    out.println("	16#b4 /acute     16#b5 /lcaron  16#b6 /sacute    16#b7 /caron");
	    out.println("	16#b8 /cedilla   16#b9 /scaron  16#ba /scedilla  16#bb /tcaron");
	    out.println("	16#bc /zacute    16#bd /hungarumlaut 16#be /zcaron 16#bf /zdotaccent");
	    out.println("	16#c0 /Racute    16#c1 /Aacute  16#c2 /Acircumflex 16#c3 /Abreve");
	    out.println("	16#c4 /Adieresis 16#c5 /Lacute  16#c6 /Cacute    16#c7 /Ccedilla");
	    out.println("	16#c8 /Ccaron    16#c9 /Eacute  16#ca /Eogonek   16#cb /Edieresis");
	    out.println("	16#cc /Ecaron    16#cd /Iacute  16#ce /Icircumflex 16#cf /Dcaron");
	    out.println("	16#d0 /Dcroat    16#d1 /Nacute   16#d2 /Ncaron    16#d3 /Oacute");
	    out.println("	16#d4 /Ocircumflex 16#d5 /Ohungarumlaut 16#d6 /Odieresis 16#d7 /multiply");
	    out.println("	16#d8 /Rcaron    16#d9 /Uring   16#da /Uacute    16#db /Uhungarumlaut");
	    out.println("	16#dc /Udieresis 16#dd /Yacute  16#de /Tcommaaccent 16#df /germandbls");
	    out.println("	16#e0 /racute    16#e1 /aacute  16#e2 /acircumflex 16#e3 /abreve");
	    out.println("	16#e4 /adieresis 16#e5 /lacute  16#e6 /cacute    16#e7 /ccedilla");
	    out.println("	16#e8 /ccaron    16#e9 /eacute  16#ea /eogonek   16#eb /edieresis");
	    out.println("	16#ec /ecaron    16#ed /iacute  16#ee /icircumflex 16#ef /dcaron");
	    out.println("	16#f0 /dcroat    16#f1 /nacute  16#f2 /ncaron     16#f3 /oacute");
	    out.println("	16#f4 /ocircumflex 16#f5 /ohungarumlaut 16#f6 /odieresis 16#f7 /divide");
	    out.println("	16#f8 /rcaron    16#f9 /uring   16#fa /uacute    16#fb /uhungarumlaut");
	    out.println("	16#fc /udieresis 16#fd /yacute  16#fe /tcommaaccent  16#ff /dotaccent");
	    out.println("] def\n");
	} else if(eq(enc_choice, 3)) {
	    /* This array indicates changes necessary to go from the Standard Encoding
	       Vector to one matching the int'l characters and some others in the
	       IBM Extended Character Set
	    */
	    out.println("/encvecmod [");
	    out.println("	16#80 /Ccedilla    16#81 /udieresis 16#82 /eacute      16#83 /acircumflex");
	    out.println("	16#84 /adieresis   16#85 /agrave    16#86 /aring       16#87 /ccedilla");
	    out.println("	16#88 /ecircumflex 16#89 /edieresis 16#8a /egrave      16#8b /idieresis");
	    out.println("	16#8c /icircumflex 16#8d /igrave    16#8e /Adieresis   16#8f /Aring");
	    out.println("	16#90 /Eacute      16#91 /ae        16#92 /AE          16#93 /ocircumflex");
	    out.println("	16#94 /odieresis   16#95 /ograve    16#96 /ucircumflex 16#97 /ugrave");
	    out.println("	16#98 /ydieresis   16#99 /Odieresis 16#9a /Udieresis   16#9b /cent");
	    out.println("	16#9c /sterling    16#9d /yen       16#9e /.notdef     16#9f /florin");
	    out.println("	16#a0 /aacute      16#a1 /iacute    16#a2 /oacute      16#a3 /uacute");
	    out.println("	16#a4 /ntilde      16#a5 /Ntilde    16#a6 /ordfeminine 16#a7 /ordmasculine");
	    out.println("	16#a8 /questiondown 16#a9 /.notdef  16#aa /.notdef     16#ab /onehalf");
	    out.println("	16#ac /onequarter  16#ad /exclamdown 16#ae /guillemotleft  16#af /guillemotright");
	    out.println("	16#e1 /germandbls  16#ed /oslash    16#f1 /plusminus   16#f6 /divide");
	    out.println("	16#f8 /degree      16#f9 /bullet");
	    out.println("] def\n");
	}
	if(ne(enc_choice, 0)) {
	    out.println("/gedfont fontname reenc");
	    out.println("/fontname /gedfont def\n");
	}
	out.println("%end font encoding   end of section borrowed from Robert Simms");

	if (gradient){
	    out.println("/gradient{   %draw and fill 256 circles with a decreasing radius and slightly diffent colour");
	    out.println("	/blue2 exch def	/green2 exch def	/red2 exch def");
	    out.println("	/blue1 exch def	/green1 exch def	/red1 exch def\n");
	    out.println("	/maxrad maxlevel radius def");
	    out.println("	/delta_r maxrad neg 256 div def                          %find radius step to use\n");
	    out.println("	gsave");
	    out.println("		maxrad delta_r 0.0 {                                  %step through the circles from large to small");
	    out.println("			/r exch def");
	    out.println("			/ratio r maxrad div def");
	    out.println("			/red red1 red2 sub ratio mul red2 add def          % work out the new colour");
	    out.println("			/blue blue1 blue2 sub ratio mul blue2 add def");
	    out.println("			/green green1 green2 sub ratio mul green2 add def\n");
	    out.println("			red green blue setrgbcolor");
	    out.println("			newpath 0.0 0.0 r 0 360 arc fill                   %draw and fill circles");
	    out.println("		} for");
	    out.println("	grestore");
	    out.println("} def\n");
	}
	out.println("/fan{  %Fan Template");
	out.println("	gsave");
	if(or(!printmarr,!transparent)){
	    out.println("	%begin gender specific shading of boxes");
	    out.println("	/c 1 def                          %flag for the alternating colours");
	    out.println("	1 indicentre sub 1 maxlevel {%shade the boxes if necessary");
	    out.println("		/i exch def");
	    out.println("		/delta_ang 360.0 2 i exp div def  %set the angle stepsize");
	    out.println("		/r1 i radius def		/r2 i 1 sub radius def        %find the inner and outer radius for the box");
	    if (ge(maxlevel,8)){
		out.println("		i 8 ge {0}{0.7 radfactor div} ifelse");
	    }else{
		out.println("		.7 radfactor div");
	    }
	    out.println(" setlinewidth                %if level is beyond 7 make lines thinnest possible\n");
	    out.println("		90.0 delta_ang 449.99 { %step through all angles from 90deg to 90deg+360deg (450deg)");
	    out.println("			/ang1 exch def		/ang2 ang1 delta_ang add def     %find the beginning and ending angle for each box");
	    out.println("			newpath");
	    out.println("				i 0 gt{%draw the box");
	    out.println("					ang1 r1 polar m 0 0 r1 ang1 ang2 arc ang2 r2 polar l 0 0 r2 ang2 ang1 arcn");
	    out.println("				}{");
	    out.println("					0 0 1 radius 0 0 1 radius 0 360 arc");
	    out.println("				}ifelse");
	    out.println("			closepath");
	    if(!transparent){
		out.println("				i 0 gt {                              %fill in box if necessary");
		out.println("					c 1 eq {/c1 0 def rf gf bf setrgbcolor} {/c1 1 def rm gm bm setrgbcolor} ifelse");
		out.println("				}{");
		out.println("					centrepersonsex 0 eq {rm gm bm setrgbcolor} {rf gf bf setrgbcolor} ifelse");
		out.println("				}ifelse");
		out.println("				gsave fill grestore");
		out.println("				i 0 gt{/c c1 def}if                                    %exchange color for next box");
		out.println("			rl gl bl setrgbcolor\n");
	    }
	    if(!printmarr){
		if(!transparent){
		    out.println("				i 9 le {stroke} if              %draw outline of box if level is less than 10");
		}else{
		    out.println("				stroke");
		}
	    }
	    out.println("		}for");
	    out.println("	}for %end gender specific shading of boxes");
	}
	if (printmarr){
	    out.println("	%begin draw boxes around husband and wife");
	    out.println("	rl gl bl setrgbcolor");
	    out.println("	2 indicentre sub 1 maxlevel {                    %step through the levels");
	    out.println("		/i exch def");
	    if (ge(maxlevel,8)){
		out.println("		i 8 ge {0}{0.7 radfactor div} ifelse");
	    }else{
		out.println("		.7 radfactor div");
	    }
	    out.println(" setlinewidth\n");
	    out.println("		/delta_ang 360.0 2 i 1 sub exp div def  %set the angle stepsize");
	    out.println("		90.0 delta_ang 449.99 {");
	    out.println("			/ang1 exch def		/ang2 ang1 delta_ang add def");
	    out.println("			/r1 i radius def	/r2 i 1 sub radius def\n");

	    out.println("			%draw tic marks around marriage date");
	    out.println("			/delta_r r1 r2 sub 15 div def");
	    out.println("			/angave ang1 delta_ang 2 div add def");
	    out.println("			/r_inner r2 delta_r add def");
	    out.println("			/r_outer r1 delta_r sub def\n");

	    out.println("			newpath angave r_outer polar m angave r1 polar l stroke");
	    out.println("			r2 0 gt{");
	    out.println("				newpath angave r2 polar m angave r_inner polar l stroke");
	    out.println("			}if\n");

	    if(!transparent){
		out.println("			rm gm bm setrgbcolor         %erase small gap between male and female");
		out.println("			.5 setlinewidth");
		out.println("			newpath angave r_outer polar m angave r_inner polar l stroke");
		out.println("			rl gl bl setrgbcolor");
		if (ge(maxlevel,8)){
		    out.println("		i 8 ge {0}{0.7 radfactor div} ifelse");
		}else{
		    out.println("		.7 radfactor div");
		}
		out.println(" setlinewidth");
	    }

	    out.println("			%finish tic marks\n");

	    out.println("			newpath	%draw box around parents");
	    out.println("				ang1 r1 polar m 0 0 r1 ang1 ang2 arc");
	    out.println("				ang2 r2 polar l 0 0 r2 ang2 ang1 arcn closepath");
	    out.println("			stroke");
	    out.println("		}for");
	    out.println("	}for	%end draw boxes around husband and wife\n");
	}


	if (printdate){
	    out.println("	0 0 0 setrgbcolor");
	    out.println("	fontname 5 SF");
	    out.println("	/radiusprint maxlevel radius 1.01 mul def");
	    out.println("	datetoday radiusprint 300 circtext");
	}
	out.println("	grestore");
	out.println("} def\n");

	out.println("/angtext{   %Angled Line Printing Procedure for outer lines than do not curve");
	out.println("	/inf exch def		/offst exch def		/ang exch def		/levelnum exch def		/str exch def\n");

	out.println("	gsave");
	out.println("	ang rotate                                               %rotate coordinate system for printing\n");

	out.println("	/r1 levelnum 1 sub radius def		/r2 levelnum radius def");
	if(printmarr){
	    out.println("	levelnum 1 eq indicentre 0 eq and{/r1 0 def /r2 0 def}if\n");
	}
	out.println("	/y r1 r2 add 2 div def\n");

	out.println("	inf 0 eq{0 offst -10 mul 15 add translate}{y 0.0 translate}ifelse\n");

	out.println("	str stringwidth pop 2 div neg offst moveto");
	out.println("	str show");
	out.println("	grestore");
	out.println("} def\n");

	out.println("/circtext{   %Circular Line Printing Procedure for inner lines than do curve\n");

	out.println("	/angle exch def	/textradius exch def	/str exch def\n");

	out.println("	/xradius textradius ptsize 4 div add def");
	out.println("	gsave");
	out.println("		angle str findhalfangle add rotate");
	out.println("		str {/charcode exch def ( ) dup 0 charcode put circchar} forall");
	out.println("	grestore");
	out.println("} def\n");

	out.println("/findhalfangle {stringwidth pop 2 div 2 xradius mul pi mul div 360 mul} def\n");

	out.println("/circchar{   %print each character at a different angle around the circle");
	out.println("	/char exch def\n");

	out.println("	/halfangle char findhalfangle def");
	out.println("		gsave");
	out.println("		halfangle neg  rotate");
	out.println("		textradius 0 translate");
	out.println("		-90 rotate");
	out.println("		char stringwidth pop 2 div neg 0 moveto");
	out.println("		char show");
	out.println("	grestore");
	out.println("	halfangle 2 mul neg rotate");
	out.println("} def\n");

	out.println("/setprintcolor{");
	out.println("	/ahnen exch def		/inf exch def");
	out.println("	ahnen 2 div dup cvi eq {redmale greenmale bluemale setrgbcolor}{redfemale greenfemale bluefemale setrgbcolor} ifelse");
	out.println("	ahnen inf mul 1 eq {redmale greenmale bluemale setrgbcolor} if");
	out.println("} def\n");

	out.println("/position{  %compute position from ahnentafel number");
	out.println("	/ahnenn exch def");
	out.println("	ahnenn 2 maxlevel -1 add exp lt {");
	out.println("		/a 2 ahnenn log 1.9999 log div floor exp def");
	out.println("		/numerator 2 a mul -1 add -2 ahnenn a neg add mul add def");
	out.println("		/fact 2 maxlevel -2 add exp def");
	out.println("		numerator a div fact mul");
	out.println("	}{2 maxlevel exp ahnenn neg add} ifelse");
	out.println("} def\n");

	out.println("/level {1 add log 2 log div ceiling cvi} def %compute generation level from ahnentafel number\n");

	out.println("/info{");
	out.println("	/max exch def		/inf exch def		/noffset exch def		/ahnen exch def");
	out.println("	/fntfactor {[0 0.85 0.85 0.8 0.7 0.5 0.4 0.3 0.3 0.25 0.25 0.25 0.25] exch get} def %set different font sizes for each level\n");

	out.println("	ahnen 2 maxlevel exp lt {");
	out.println("		/place ahnen position def");
	out.println("		/levelnum ahnen level def    %get the level number of the current person");
	out.println("		/radtab levelnum radius def  %get the radius of the current level");
	out.println("		/ftsize ptsize levelnum fntfactor mul def  %find the new fontsize depending on the current level number");
	out.println("		/offset ftsize 1.25 mul neg def            %find the distance that the text should be printed from the ring");
	out.println("		inf ahnen setprintcolor      %print the names and information in alternating colors as defined below in line #350");
	out.println("		fontname ftsize SF %set the font to use\n");

	out.println("		levelnum 5 lt {levelnum radtab place noffset inf max inner}  % the inner four rings");
	out.println("						{levelnum place noffset inf 0 max outer} ifelse  % all outer rings");
	out.println("	} if");
	out.println("} def\n");

	if(eq(indicentre,1)){
	    out.println("/indiinfo{");
	    out.println("	/inf exch def		/noffset exch def		/ahnen exch def");
	    out.println("	/ftsize ptsize 0.9 mul def  %find the new fontsize depending on the current level number");
	    out.println("	/offset ftsize 1.25 mul neg def            %find the distance that the text should be printed from the ring");
	    out.println("	inf ahnen setprintcolor      %print the names and information in alternating colors as defined below in line #350");
	    out.println("	fontname ftsize SF %set the font to use\n");

	    out.println("	0 0 noffset 0 angtext");
	    out.println("} def\n");
	}

	out.println("/nstr 7 string def");
	out.println("/prtn {-0.5 inch 5.5 inch m nstr cvs show} def");
	out.println("/prt {-0.5 inch 5.5 inch m	show} def\n");

	if (printmarr){
	    out.println("/minfo{");
	    out.println("	/inf exch def		/ahnen exch def");
	    out.println("	/fntfactor {[0 0.7 0.7 0.6 0.6 0.5 0.4 0.3 0.3 0.25 0.25 0.25 0.25] exch get} def %set different font sizes for each level\n");

	    out.println("	ahnen 2 maxlevel exp lt {");
	    out.println("		/place ahnen 1 eq {0}{ahnen 2 div position}ifelse def  %get the position of the text counting on the outer ring from bottom upwards");
	    out.println("		/levelnum ahnen level def   %get the level number of the current person");
	    out.println("		/ftsize ptsize levelnum fntfactor mul 0.80 mul def  %find the new fontsize depending on the current level number");
	    out.println("		/offset ftsize 0.35 mul neg def            %find the distance that the text should be printed from the ring");
	    out.println("		rl gl bl setrgbcolor");
	    out.println("		dup");
	    out.println("		/namelength exch length def");
	    out.println("		/f namelength 11 lt {1}{11 namelength div}ifelse def");
	    out.println("		fontname ftsize f mul SF %set the font to use\n");

	    out.println("		levelnum place 0 inf 1 1 outer");
	    out.println("	} if");
	    out.println("} def\n");
	}

	out.println("/inner{");
	out.println("	/max exch def		/inf exch def		/noffset exch def		/place exch def		/radtab exch def		/levelnum exch def");
	out.println("	% slight modifications for each level for line spacing");
	if(eq(indicentre,0)){
	    out.println("		max 3 eq {/factor {[0.0 0.98 0.97 0.97 0.975] exch get} def}if");
	    out.println("		max 2 eq {/factor {[0.0 0.80 0.885 0.935 0.94] exch get} def}if");
	    out.println("		max 1 eq {/factor {[0.0 0.70 0.835 0.905 0.91] exch get} def}if\n");
	}
	if(eq(indicentre,1)){
	    out.println("		max 3 eq {/factor {[0.0 0.96 0.98 0.98 0.975] exch get} def}if");
	    out.println("		max 2 eq {/factor {[0.0 0.96 0.935 0.945 0.94] exch get} def}if");
	    out.println("		max 1 eq {/factor {[0.0 0.96 0.905 0.915 0.91] exch get} def}if\n");
	}

	out.println("	levelnum 1 eq indicentre 0 eq and{/offset offset 0.75 mul def} if  %max the offset a bit smaller for the first level");
	out.println("	radtab levelnum factor mul noffset offset mul add place inf midang circtext");
	out.println("} def\n");

	out.println("/outer{");
	out.println("	/max exch def	/marr exch def		/inf exch def		/noffset exch def		/place exch def		/levelnum exch def\n");

	out.println("			% in the following:");
	out.println("			%      f1 spreads the text out apart from eachother when more positive (larger)");
	out.println("			%      f2 shifts the set of text counter clockwise when more positive (larger)");
	if(eq(maxlevel,5)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 1.35 def} if}if");
	    out.println("		max 2 eq {levelnum 5 eq {/f1 -2.5 def	/f2 0.25 def} if}if\n");
	}
	if(eq(maxlevel,6)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 6.50 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.7 def	/f2 1.50 def} if}if");
	    out.println("		max 2 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.7 def	/f2 1.50 def} if}if\n");
	}
	if(eq(maxlevel,7)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 6.50 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.30 def} if}if");
	    out.println("		max 2 eq {");
	    out.println("			 		 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 3.30 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 0.70 def} if}if");
	    out.println("		max 1 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.30 def} if");
	    out.println("					 levelnum 7 eq {/f1 -2.0 def	/f2 1.20 def} if}if\n");
	}
	if(eq(maxlevel,8)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 6.50 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.30 def} if}if");
	    out.println("		max 2 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 3.30 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 2.20 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.7 def	/f2 0.80 def} if}if");
	    out.println("		max 1 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 3.30 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 1.50 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.7 def	/f2 0.50 def} if}if\n");
	}
	if(eq(maxlevel,9)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 6.50 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.30 def} if}if");
	    out.println("		max 2 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.00 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 2.00 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.6 def	/f2 1.40 def} if}if");
	    out.println("		max 1 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.00 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 2.00 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.6 def	/f2 1.40 def} if");
	    out.println("					 levelnum 9 eq {/f1  0.0 def	/f2 0.00 def} if}if\n");
	}
	if(eq(maxlevel,10)){
	    out.println("		max 3 eq {levelnum 5 eq {/f1 -2.5 def	/f2 6.50 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.30 def} if}if");
	    out.println("		max 2 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.00 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 2.00 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.6 def	/f2 1.40 def} if}if");
	    out.println("		max 1 eq {");
	    out.println("					 levelnum 5 eq {/f1 -2.5 def	/f2 4.85 def} if");
	    out.println("					 levelnum 6 eq {/f1 -1.6 def	/f2 4.00 def} if");
	    out.println("					 levelnum 7 eq {/f1 -1.0 def	/f2 1.70 def} if");
	    out.println("					 levelnum 8 eq {/f1 -0.6 def	/f2 1.20 def} if");
	    out.println("					 levelnum 9 eq {/f1  0.0 def	/f2 0.40 def} if");
	    out.println("					 levelnum 10 ge{/f1  0.0 def	/f2 0.225 def}if}if\n");
	}

	out.println("	marr 1 eq {/f1 0.0 def		/f2 0.0 def} if\n");

	out.println("	/ang place inf midang f1 noffset mul f2 add add def");
	out.println("	levelnum ang offset inf angtext");
	out.println("} def\n");

	out.println("%   borrowed from Robert Simms");
	if(eq(indicentre,1)){
	    out.println("/addcenterindi {centerperson_array 3 1 roll put} def");
	}
	if(printmarr){
	    out.println("/addmarr {marriage_array 3 1 roll put} def");
	}
	out.println("/addind {person_array 3 1 roll put} def\n");
    }

    private void main(Indi person, Fam fam) {
	int psex = (person.getSex() == PropertySex.MALE )? 0:1;
	/*	monthformat(4)
	  stddate(0)
	  dayformat(2)

	  set(version, "ps-circle.ll version 2.6.2, 10 December 2003 - code by Henry Sikkema")
	*/
	//numindilines=-1;
	//    nummarr=-1;
	  /*
	  set(mc, -1)

	  while (lt(mc,0)){
	  list(options)
	  setel(options,1,"Family in centre (husband/wife).")
	  setel(options,2,"Individual in centre")
	  set(mc,menuchoose(options, "Select the number of generations you want printed:"))
	  if(eq(mc,0)){break()}
	  if(eq(mc,1)){set(indicentre,0)	getfam(fam)}
	  if(eq(mc,2)){set(indicentre,1)	getindi(person)}
	  }*/
	//indicentre=1;
	/*
	  list(options)
	  setel(options,1,"5 generations.")
	  setel(options,2,"6 generations.")
	  setel(options,3,"7 generations.")
	  setel(options,4,"8 generations.")
	  setel(options,5,"9 generations.")
	  setel(options,6,"10 generations.")
	  set(maxlevel,menuchoose(options, "Select the numbers of generation you want printed:"))
	  if(eq(maxlevel,0)){break()}
	  set(maxlevel,add(maxlevel,4))
	*/
	//maxlevel = 6;
	/*
	  list(options)
	  setel(options,1,"Full birth date info if no date is given: ex b:11 Oct 1758")
	  setel(options,2,"Year only format:  example (1758-1823)")
	  setel(options,3,"Year only format (spaces for unknown date) ex: (    -1823)")
	  set(mc, menuchoose(options, "Select date format:"))
	  if(eq(mc,0)){break()}
	  if(eq(mc,1)){set(dateformat,1)}
	  if(eq(mc,2)){set(dateformat,2)}
	  if(eq(mc,3)){set(dateformat,3)}
	*/
	//dateformat = 1;
	/*
	  list(options)
	  setel(options,1,"Yes, print marriage dates only if exact date is known.")
	  setel(options,2,"Yes, print marriage date even when estimate is found in file")
	  setel(options,3,"No, do not print marriage dates.")
	  set(mc, menuchoose(options, "Print marriage dates?"))
	  if(eq(mc,0)){break()}
	  if(eq(mc,1)){set(printmarr,1)set(marrest,0)}
	  if(eq(mc,3)){set(printmarr,0)}
	  if(eq(mc,2)){set(printmarr,1)set(marrest,1)}
	*/
	//printmarr=0;
	/*	list(options)
	  setel(options,1,"Colour text (default: blue for males, red for females)")
	  setel(options,2,"Black Text  (best for printing on non-colour printers)")
	  set(mc, menuchoose(options, "Select text colour option:"))
	  if(eq(mc,0)){break()}
	  if(eq(mc,1)){set(colourtext,1)}
	  if(eq(mc,2)){set(colourtext,0)}
	*/
	//colourtext = 0;
	/*
	  list(options)
	  setel(options,1,"Gender Specific Colour scheme (default: pink for males, light blue for females)")
	  setel(options,2,"Transparent Background (best for printing on non-colour printers)")
	  setel(options,3,"Gradient Colour scheme")
	  set(mc,menuchoose(options, "Select text colour option:"))
	  if (eq(mc,0)){break()}
	  if (eq(mc,1)){set(alternating,1)set(gradient,0)}
	  if (eq(mc,2)){set(alternating,0)set(gradient,0)}
	  if (eq(mc,3)){set(alternating,0)set(gradient,1)}
	*/
	//alternating = 0; gradient = 1;
	//alternating = 0; gradient = 0;
	/*	list(options)
	  setel(options,1,"Yes, put on today's date.")
	  setel(options,2,"No, do not put on today's date.")
	  set(mc,menuchoose(options, "Do you want today's date printed on the circle?"))
	  if (eq(mc,0)){break()}
	  if (eq(mc,1)){set(printdate,1)}
	  if (eq(mc,2)){set(printdate,0)}
	*/
	//printdate = 0;
	/*	list(options)
	  setel(options,1,"Helvetica/Arial")
	  setel(options,2,"Times-Roman")
	  setel(options,3,"Courier")
	  setel(options,4,"AvantGarde-Book")
	  setel(options,5,"Times-Roman")
	  setel(options,6,"ZapfChancery")

	  set(mc,menuchoose(options, "Choose a font to use:"))
	  if (eq(mc,0)){break()}
	  if (eq(mc,1)){set(font_name,"Helvetica")}
	  if (eq(mc,2)){set(font_name,"Times-Roman")}
	  if (eq(mc,3)){set(font_name,"Courier")}
	  if (eq(mc,4)){set(font_name,"AvantGarde-Book")}
	  if (eq(mc,5)){set(font_name,"Palatino-Roman")}
	  if (eq(mc,6)){set(font_name,"ZapfChancery")}
	*/
	//font_name = "Times-Roman";

	/*	list(options)
	  setel(options,1,"Single page (maximum circle size on a single page)")
	  setel(options,2,"Multipage according to number of pages selected")
	  setel(options,3,"Multipage according to radius of chart")
	  set(mc,menuchoose(options, "Select page type: "))
	  if (eq(mc,0)){break()}
	  if (eq(mc,1)){
	  set(x_pages,1)set(y_pages,1)set(radius,0)
	  }
	  if(gt(mc,1)){
	  print(   "Radius (inches)  # of pages  Radius (inches)  # of pages"
	  ,nl(),"  0-8               1x1=1     32-33             4x4=16"
	  ,nl(),"  8-10              2x1=2     33-42             5x4=20"
	  ,nl()," 10-16              2x2=4     42-43             6x4=24"
	  ,nl()," 16-21              3x2=6     43-50             6x5=30"
	  ,nl()," 21-25              3x3=9     50-54             7x5=35"
	  ,nl()," 25-32              4x3=12    54-59             7x6=42",nl()
	  )
	  }
	  if (eq(mc,2)){
	  getint( x_pages, "Number of horizontal portrait pages on chart")
	  getint( y_pages, "Number of vertical portrait pages on chart")
	  set(radius,0)
	  }
	  if (eq(mc,3)){
	  getint(radius, "Enter desired radius in inches:")
	  if (le(radius,8)){set(x_pages,1)set(y_pages,1)}
	  if (and(ge(radius,8),lt(radius,10))){set(x_pages,2)set(y_pages,1)}
	  if (and(ge(radius,10),lt(radius,16))){set(x_pages,2)set(y_pages,2)}
	  if (and(ge(radius,16),lt(radius,21))){set(x_pages,3)set(y_pages,2)}
	  if (and(ge(radius,21),lt(radius,25))){set(x_pages,3)set(y_pages,3)}
	  if (and(ge(radius,25),lt(radius,32))){set(x_pages,4)set(y_pages,3)}
	  if (and(ge(radius,32),lt(radius,33))){set(x_pages,4)set(y_pages,4)}
	  if (and(ge(radius,33),lt(radius,42))){set(x_pages,5)set(y_pages,4)}
	  if (and(ge(radius,42),lt(radius,43))){set(x_pages,6)set(y_pages,4)}
	  if (and(ge(radius,43),lt(radius,50))){set(x_pages,6)set(y_pages,5)}
	  if (and(ge(radius,50),lt(radius,54))){set(x_pages,7)set(y_pages,5)}
	  if (and(ge(radius,54),lt(radius,59))){set(x_pages,7)set(y_pages,6)}
	  }
	*/
	//	x_pages=1; y_pages=1; radius=8;
	/*
	**  ISO-Latin 1, or ISO 8859-1, is a world-wide standard for most languages
	**  of Latin origin: Albanian, Basque, Breton, Catalan, Cornish, Danish, Dutch
	**  English, Faroese, Finish (exc. S,s,Z,z with caron),
	**  French (exc. OE, oe, Y with dieresis), Frisian, Galician, German,
	**  Greenlandic, Icelandic, Irish Gaelic (new orthography), Italian, Latin,
	**  Luxemburgish, Norwegian, Portuguese, Rhaeto-Romanic, Scottish Gaelic,
	**  Spanish, Swedish.
	**
	**  ISO Latin 2, or ISO 8859-2, covers these languages:  Albanian, Croatian,
	**  Czech, English, German, Hungarian, Latin, Polish, Romanian (cedilla below
	**  S,s,T,t instead of comma), Slovak, Sloverian, Sorbian.
	*/
	/*	 list(options)
	  setel(options, 1, "ISO Latin 1 most West European languages")
	  setel(options, 2, "ISO Latin 2 Central and East European languages")
	  setel(options, 3, "IBM PC (covers at least the international chars)")
	  set(enc_choice, menuchoose(options,
	  "Select font reencoding, or (q) to use what's in the fonts"))
	  if (eq(enc_choice,0)){break()}
	*/
	//	enc_choice=1;
	printfile();

	if (printdate){
	    //		monthformat(6) /*capitalized full word (eg, January, February) */
	    out.println("/datetoday (Date: " +PointInTime.getNow().toString()+ ") def\n\n");
	    //monthformat(4) /*capitalized abbreviation (eg, Jan, Feb) */
	}

	out.println("/indicentre "+d(indicentre)+" def %1=put individual in centre,0=family at centre");
	if(eq(indicentre,1)){
	    out.println("/centrepersonsex "+psex+" def %0=male; 1=female\n\n");
	}

	out.println("/maxlevel "+ d(maxlevel)+ " def");

	out.println("% color  of the text in RGB format");
	if(eq(colourtext,1)){
	    out.println("/redmale   0.0 def  /greenmale   0.0 def  /bluemale   1.0 def");
	    out.println("/redfemale 1.0 def  /greenfemale 0.0 def  /bluefemale 0.0 def\n");
	}else{
	    out.println("/redmale   0.0 def  /greenmale   0.0 def  /bluemale   0.0 def");
	    out.println("/redfemale 0.0 def  /greenfemale 0.0 def  /bluefemale 0.0 def\n");
	}

	if (gradient){
	    out.println("/transparent 1 def         % 1=transparent, 0=color shading\n");

	    out.println("/rf 0.0 def /gf 0.0 def /bf 0.0 def %rgb female box fill");
	    out.println("/rm 0.0 def /gm 0.0 def /bm 0.0 def %rgb male box fill\n");

	}else{
	    if (!alternating){
		out.println("/transparent 1 def         % 1=transparent, 0=color shading\n");
		
		out.println("/rf 1.0 def /gf 1.0 def /bf 1.0 def %rgb female box fill");
		out.println("/rm 1.0 def /gm 1.0 def /bm 1.0 def %rgb male box fill\n");
	    }else{
		out.println("/transparent 0 def         % 1=transparent, 0=color shading\n");
		
		out.println("/rf 0.8 def /gf 0.8 def /bf 1.0 def %rgb female box fill");
		out.println("/rm 1.0 def /gm 0.8 def /bm 0.8 def %rgb male box fill\n");
	    }
	}
	/*	println("/printmarr "+d(printmarr)+" def");*/
	
	out.println("/rl 0.0 def /gl 0.0 def /bl 0.0 def %  rgb for lines");
	
	out.println("%     partially borrowed from Robert Simms");
	out.println("% Find printable dimension for chart with a sequence of steps\n");

	out.println("% get printable area for each page");
	out.println("clippath pathbbox newpath");
	out.println("/ury exch def /urx exch def");
	out.println("/lly exch def /llx exch def\n");

	out.println("/llx llx margin_left add def /lly lly margin_bottom add def");
	out.println("/urx urx margin_right sub def /ury ury margin_top sub def\n");

	out.println("% get available width and height for printing on a sheet of paper");
	out.println("/wp urx llx sub def");
	out.println("/hp ury lly sub def\n");

	out.println("% get width and height of the multi-page printable area");
	out.println("/tw0 wp xpages mul def");
	out.println("/th0 hp ypages mul def\n");

	out.println("tw0 th0 gt {");
	if(eq(radius,0)) {out.println("	/mindim th0 def\n");}
	out.println("	th0 wp div ceiling cvi xpages lt {/xpages th0 wp div ceiling cvi def /tw0 wp xpages mul def /ypages ypages def}{/xpages xpages def /ypages ypages def}ifelse");
	out.println("}{");
	if(eq(radius,0)) {out.println("	/mindim tw0 def\n");}
	out.println("	tw0 hp div ceiling cvi ypages lt {/ypages tw0 hp div ceiling cvi def /th0 hp ypages mul def /xpages xpages def}{/xpages xpages def /ypages ypages def}ifelse");
	out.println("}ifelse\n");
	
	if(gt(radius,0)) {
	    out.println("/radfactor " +d(radius)+ " inch 8 inch div def");
	}else{
	    out.println("/radfactor mindim 8 inch div def");
	}
	out.println("/scalefactor 7.0 maxlevel indicentre add div radfactor mul def\n");

	out.println("/print-a-page { % page printing procedure");
	out.println("	/ypage exch ypages 2 div 1 sub sub def  %y-correction to center chart");
	out.println("	/xpage exch xpages 2 div 1 sub sub def  %x-correction to center chart");
	out.println("	ypage ypages lt xpage xpages lt and { %only print if page is in correct range");
	out.println("		gsave");
	out.println("			llx lly translate");
	out.println("			0 0 wp hp RC		% specify (rectangular) clipping path to keep the margins clean");
	out.println("			xpage wp mul ypage hp mul translate	% move origin so that desired portion of chart lands within clipping path");
	out.println("			scalefactor dup scale  %enlarge scale to fit page");
	if (gradient){
	    out.println("0.6431 0.3255 0.0228  % inside centre color in RGB format");
	    out.println("0.9922 0.7686 0.5490  % outside rim color in RGB format    to form a radial gradient");
	    out.println("gradient\n");
	}
	out.println("			fan  %draw circle template");
	if(eq(indicentre,1)){
	    out.println("			centerperson_array {exec indiinfo} forall %put in center person\n");
	}
	out.println("			person_array {exec info} forall %put in all people with dates");
	if(printmarr) {
	    out.println("			marriage_array {exec minfo} forall %put in marriage dates\n");
	}
	out.println("			1 dup scale %reset scale to normal");
	out.println("		grestore");
	out.println("	} if");
	out.println("} def      % print-a-page procedure\n");
	
	out.println("%%EndProlog");
	out.println("%%BeginSetUp\n");

	out.println("/fillarray{% store vertical lines and individual records in arrays");

	if(eq(indicentre,1)){
	    PersonCell pCell = new PersonCell();
	    int index=0;
	    index+=pCell.add(surname(person),index);
	    index+=pCell.add(put_given_name(person,25),index);
	    index+=pCell.add(getDateString(birth(person),death(person),dateformat),index);
	    out.print(pCell.getCenter(psex));
	    semicirc(parents(person),father(person),1,1,1,maxlevel,dateformat);
	    semicirc(parents(person),mother(person),1,1,2,maxlevel,dateformat);
	}else{
	    semicirc(fam,husband(fam),1,1,1,maxlevel,dateformat);
	    semicirc(fam,wife(fam),1,1,2,maxlevel,dateformat);
	}
	out.println("} def\n");

	if(eq(indicentre,1)){
	    out.println("/centerperson_array 3 array def\n");
	}
		if(printmarr){
	    out.println("/marriage_array "+d(add(nummarr,1))+" array def\n");
	}
	out.println("/person_array "+numindilines+" array def");

	out.println("fillarray\n");

	out.println("mark\n");
	out.println("%%EndSetUp");
	putpageprintouts(x_pages,y_pages);
	out.println("%%EOF");
    }
    
    private String getDateString(PropertyDate birth, PropertyDate death, int dateformat){
	String result = "";
	String birthstr = "";
	String deathstr = "";
	if (birth != null && !birth.isValid()) birth = null;
	if (death != null && !death.isValid()) death = null;
	if (birth == null && death == null) return "";
	switch (dateformat){
	case 2:
	    birthstr = "    ";
	    deathstr = "    ";
	case 1:
	    if (death != null){
		deathstr = year(death);
	    }
	    if (birth != null){
		birthstr = year(birth);
	    }
	    result = "("+birthstr+"-"+deathstr+")";
	    break;
	default:
	    if (death != null){
		if (birth != null){
		    result += year(birth);
		}
		result += "-"+year(death);
	    } else {
		if (birth != null){
		    result += OPTIONS.getBirthSymbol()+" "+birth.getDisplayValue();
		}
	    }
	    break;
	}
	return result;
    }

}    //ReportLinesCirc

 class PersonCell {
	List cells;
	int max;
	PersonCell(){
	    cells = new ArrayList(5);
	    max = 0;
	}
	int add(String text,int index){
	    if (text == null || text.length() == 0) return 0;
	    cells.add(""+index+" {("+text);
	    max++;
	    return 1;
	}
	String get(int ahnen,int info){
	    String result="";
	    int offset;
	    for (offset=1; offset<=max; offset++){
		result += (String)(cells.get(offset-1))+") "+ahnen+" "+offset+" "+info+" "+max+"} addind\n";
	    }
	    return(result);
	}
	String getCenter(int sex){
	    String result="";
	    int offset;
	    for (offset=1; offset<=max; offset++){
		result += (String)(cells.get(offset-1))+") "+sex+" "+offset+" 0} addcenterindi\n";
	    }
	    return(result);
	}
    }
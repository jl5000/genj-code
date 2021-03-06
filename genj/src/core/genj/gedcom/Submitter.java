/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.gedcom;

/**
 * Class for encapsulating a submitter
 */
public class Submitter extends Entity {
  
  private final static TagPath PATH_NAME =new TagPath("SUBM:NAME");

  /**
   * need tag,id-arguments constructor for all entities
   */
  public Submitter(String tag, String id) {
    super(tag, id);
    assertTag(Gedcom.SUBM);
  }
  
  /**
   * Name ...
   */
  @Override
  protected String getToStringPrefix(boolean showIds) {
    return getName();
  }
  
  /**
   * Name of Submitter
   */
  public String getName() {
    return getValue(PATH_NAME, "");
  }
  
  /**
   * Name of Submitter
   */
  public void setName(String name) {
    setValue(PATH_NAME, name);
  }
  
} //Submitter

/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2010 Nils Meier <nils@meiers.net>
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
package genj.edit.beans;

import genj.edit.Images;
import genj.edit.actions.RunExternal;
import genj.gedcom.Media;
import genj.gedcom.Property;
import genj.gedcom.PropertyBlob;
import genj.gedcom.PropertyFile;
import genj.gedcom.PropertyXRef;
import genj.io.InputSource;
import genj.util.DefaultValueMap;
import genj.util.Origin;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;
import genj.util.swing.FileChooserWidget;
import genj.util.swing.NestedBlockLayout;
import genj.util.swing.ThumbnailWidget;
import genj.view.ContextProvider;
import genj.view.ViewContext;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A property bean for managing multimedia files (and blobs) associated with properties 
 */
public class MediaBean extends PropertyBean implements ContextProvider {
  
  private final static Resources RES = Resources.get(MediaBean.class);
  
  private Map<InputSource,Set<Property>> input2properties = 
    new DefaultValueMap<InputSource,Set<Property>>(new HashMap<InputSource,Set<Property>>(), new HashSet<Property>());
  
  private ThumbnailWidget thumbs = new ThumbnailWidget() {
    public String getToolTipText(InputSource source) {
      StringBuffer result = new StringBuffer();
      result.append("<html><body>");
      int i=0; for (Property prop : input2properties.get(source)) {
        if (i++>0) result.append("<br>");
        result.append(prop.toString());
      }
      result.append("</body></html>");
      return result.toString();
    }
  };
  private JToolBar actions = new JToolBar();
  private Action2 add = new Add(), del = new Del();
  
  /**
   * Constructor
   */
  public MediaBean() {
    
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createLoweredBevelBorder());

    add(BorderLayout.NORTH , actions);
    add(BorderLayout.CENTER, thumbs);
    
    setPreferredSize(new Dimension(32,32));
    actions.setFloatable(false);
    
    // some actions
    add(add);
    add(del);
    add(thumbs.getFitAction());
    add(thumbs.getAllAction());

    // done
  }
  
  @Override
  public ViewContext getContext() {
    Property p = getProperty();
    if (p==null)
      return null; 
    InputSource source = thumbs.getSelection();
    if (!(source instanceof InputSource.FileInput))
      return null;
    ViewContext result = new ViewContext(p);
    result.addAction(new RunExternal(((InputSource.FileInput)source).getFile()));
    return result;
  }
  
  private void add(Action2 action) {
    JButton b = new JButton(action);
    b.setFocusable(false);
    actions.add(b);
  }
  
  @Override
  protected void commitImpl(Property property) {
  }

  @Override
  protected void setPropertyImpl(Property prop) {
    
    input2properties.clear();
    
    // clear?
    if (prop==null) {
      thumbs.clear();
      add.setEnabled(false);
      del.setEnabled(false);
    } else {
      
      scan(prop);
      
      thumbs.setSources(new ArrayList<InputSource>(input2properties.keySet()));
      
      add.setEnabled(true);
      del.setEnabled(true);
    }
  }
  
  private void scan(Property root) {
    
    // check OBJEs
    for (int i=0;i<root.getNoOfProperties(); i++) {
      Property child = root.getProperty(i);
      if (!"OBJE".equals(child.getTag()))
        scan(child);
      else
        scan(root, child);
    }
    
    // done
  }
  
  private void scan(Property parent, Property OBJE) {

    // TODO - what if the file was loaded remotely?
    
    // a OBJE reference?
    if (OBJE instanceof PropertyXRef && ((PropertyXRef)OBJE).getTargetEntity() instanceof Media) {
      Media media = (Media)((PropertyXRef)OBJE).getTargetEntity();
      PropertyFile pfile = media.getFile();
      if (pfile!=null&&pfile.getFile()!=null){
        input2properties.get(InputSource.get(pfile.getFile())).add(parent);
        return;
      }
      PropertyBlob blob = media.getBlob();
      if (blob!=null)
        input2properties.get(InputSource.get(media.getTitle(), blob.getBlobData())).add(parent);
      return;
    }
      
    // an inline OBJE|FILE?
    Property FILE = OBJE.getProperty("FILE");
    if (FILE instanceof PropertyFile) {
      File file = ((PropertyFile)FILE).getFile();
      if (file!=null) 
        input2properties.get(InputSource.get(file)).add(parent);
      return;
    }
    
    // unusable OBJE
  }
  
  private class Add extends Action2 implements ListSelectionListener, ChangeListener {
    
    private FileChooserWidget chooser;
    private JList targets;
    private Action ok;
    
    public Add() {
      setImage(ThumbnailWidget.IMG_THUMBNAIL.getOverLayed(Images.imgNew));
    }
    @Override
    public void setEnabled(boolean set) {
      // only if there are targets
      if (set&&getTargets().isEmpty())
        set = false;
      // let through
      super.setEnabled(set);
      // update tip
      if (set)
        setTip(RES.getString("file.add", getProperty().getPropertyName()));
      else
        setTip("");
    }
    
    private List<Property> getTargets() {
      List<Property> result = new ArrayList<Property>();
      Property p = getProperty(); 
      if (p!=null) {
        if (p.getMetaProperty().allows("OBJE"))
          result.add(p);
        for (Property c : p.getProperties())
          if (c.getMetaProperty().allows("OBJE"))
            result.add(c);
      }
      return result;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      // ask user
      chooser = new FileChooserWidget();
      Origin origin = getProperty().getGedcom().getOrigin();
      chooser.setDirectory(origin.getFile()!=null ? origin.getFile().getParent() : null);
      
      targets = new JList(getTargets().toArray());
      targets.setVisibleRowCount(5);

      JPanel options = new JPanel(new NestedBlockLayout("<col><l1 gx=\"1\"/><file gx=\"1\"/><l2 gx=\"1\"/><targets gx=\"1\" gy=\"1\"/></col>"));
      options.add(new JLabel(RES.getString("file.title")));
      options.add(chooser);
      options.add(new JLabel(RES.getString("file.add", "...")));
      options.add(new JScrollPane(targets));

      ok = Action2.ok();

      targets.addListSelectionListener(this);
      chooser.addChangeListener(this);
      
      if (targets.getModel().getSize()>0)
        targets.setSelectedIndex(0);
      
      if (0!=DialogHelper.openDialog(getTip(), DialogHelper.QUESTION_MESSAGE, options, Action2.andCancel(ok), DialogHelper.getComponent(e)))
        return;
      
      // TODO add it
      for (Object target : targets.getSelectedValues()) {
      }

      // done
    }
    
    private File getFile() {
      Origin origin = getProperty().getGedcom().getOrigin();
      return origin.getFile(chooser.getFile().toString());
    }
    
    private void validate() {
      File file =  getFile();
      ok.setEnabled(targets.getSelectedIndices().length>0 && file!=null && file.exists());
    }
    
    public void valueChanged(ListSelectionEvent e) {
      validate();
    }
    
    public void stateChanged(ChangeEvent e) {
      validate();
    }
  } //Add
  
  private class Del extends Action2 implements PropertyChangeListener {
    public Del() {
      setImage(ThumbnailWidget.IMG_THUMBNAIL.getGrayedOut().getOverLayed(Images.imgDel));
      thumbs.addPropertyChangeListener(this);
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(getProperty()!=null);
    }
    @Override
    public void setEnabled(boolean set) {
      if (thumbs.getSelection()==null)
        set = false;
      super.setEnabled(set);
      if (set)
        setTip(RES.getString("file.del", getProperty().getPropertyName()));
      else
        setTip("");
    }
    @Override
    public void actionPerformed(ActionEvent e) {
      // anything to remove?
      InputSource source = thumbs.getSelection();
      if (source==null)
        return;
      // TODO remove!
    }
  }
}
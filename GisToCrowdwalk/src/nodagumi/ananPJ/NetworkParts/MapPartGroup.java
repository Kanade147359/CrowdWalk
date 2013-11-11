package nodagumi.ananPJ.NetworkParts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.misc.FilePathManipulation;

public class MapPartGroup extends OBNode implements Serializable {
    private static final long serialVersionUID = 3059772175240934632L;
    /* Position of the MapPart, relative to the parent node.
     */
    private Point2D pNorthWest = null;
    private Point2D pSouthEast = null;

    private double pTheta = 0.0;
    private double scale = 1.0;
    private double minHeight = -5.0;
    private double maxHeight = 5.0;
    private int verticalDivision = 1;   // tkokada added
    private int horizontalDivision = 1; // tkokada added
    private double rotation = 0.0;  // tkokada added
    private double defaultHeight = 0.0;
    private String imageFileName = null;
      
    private AffineTransform fromParentCache;
    //private AffineTransform fromAbsoluteCache;
    //private AffineTransform fromAbsolute;
    //private AffineTransform toAbsolute;
      
    /* Parameters related on how this frame will be drawn.
     * Will be used in EditorFrame.
     */ 
    public double tx = 0.0;
    public double ty = 0.0;
    public double sx = 1.0;
    public double sy = 1.0;
    public double r  = 0.0;

    /* Constructor */
    public MapPartGroup(int _ID,
            Point2D _pNorthWest,
            Point2D _pSouthEast,
            double _pTheta,
            String _imageFileName){
      super(_ID);
      ID = _ID;
      pNorthWest = _pNorthWest;
      pSouthEast = _pSouthEast;
      pTheta = _pTheta;
      setImageFileName(_imageFileName);

      /* create affine Transform absolute->local */
      fromParentCache = new AffineTransform();
      fromParentCache.translate(-pNorthWest.getX(), -pNorthWest.getY());
      fromParentCache.scale(getScale(), getScale());
      fromParentCache.rotate(-pTheta);
    }
      
    public MapPartGroup(int _ID,
            Point2D _pNorthWest,
            Point2D _pSouthEast,
            double _pTheta) {
        this(_ID, _pNorthWest, _pSouthEast, _pTheta, null);
    }
    
    public MapPartGroup(int _ID) {
        this(_ID,
                new Point2D.Double(5.0, 45.0), /* NW*/
                new Point2D.Double(600.0, 600.0), /* SE */
                0.0 /* _pTheta */
        );
    }

    private void addAttributesToDom(Element element) {
        element.setAttribute("id", "" + ID);
        element.setAttribute("pNorthWestX", "" + pNorthWest.getX());
        element.setAttribute("pNorthWestY", "" + pNorthWest.getY());
        element.setAttribute("pSouthEastX", "" + pSouthEast.getX());
        element.setAttribute("pSouthEastY", "" + pSouthEast.getY());
        element.setAttribute("pTheta", "" + pTheta);

        element.setAttribute("tx", "" + tx);
        element.setAttribute("ty", "" + ty);
        element.setAttribute("sx", "" + sx);
        element.setAttribute("sy", "" + sy);
        element.setAttribute("r", "" + r);
        
        element.setAttribute("scale", "" + getScale());
        element.setAttribute("minHeight", "" + getMinHeight());
        element.setAttribute("maxHeight", "" + getMaxHeight());
        element.setAttribute("defaultHeight", "" + getDefaultHeight());
        element.setAttribute("imageFileName", getImageFileName());
    }
    
    @Override
    protected void getAttributesFromDom(Element element) {
        super.getAttributesFromDom(element);
        ID = Integer.parseInt(element.getAttribute("id"));
        double x = Double.parseDouble(element.getAttribute("pNorthWestX"));
        double y = Double.parseDouble(element.getAttribute("pNorthWestY"));
        pNorthWest = new Point2D.Double(x, y);
        
        x = Double.parseDouble(element.getAttribute("pSouthEastX"));
        y = Double.parseDouble(element.getAttribute("pSouthEastY"));
        pSouthEast = new Point2D.Double(x, y);
        pTheta = Double.parseDouble(element.getAttribute("pTheta"));

        tx = Double.parseDouble(element.getAttribute("tx"));
        ty = Double.parseDouble(element.getAttribute("ty"));
        sx = Double.parseDouble(element.getAttribute("sx"));
        sy = Double.parseDouble(element.getAttribute("sy"));
        r = Double.parseDouble(element.getAttribute("r"));

        setScale(Double.parseDouble(element.getAttribute("scale")));
        setMinHeight(Double.parseDouble(element.getAttribute("minHeight")));
        setMaxHeight(Double.parseDouble(element.getAttribute("maxHeight")));
        setDefaultHeight(Double.parseDouble(element.getAttribute("defaultHeight")));
        setImageFileName(element.getAttribute("imageFileName"));
    }
    
    public boolean haveEditorFrame(){
        EditorFrame frame = (EditorFrame) this.getUserObject();
        if(frame == null) return false;
        return true;
    }

    public Rectangle2D getRegion(){
        return new Rectangle2D.Double(  this.pNorthWest.getX(),
                                        this.pNorthWest.getY(),
                                        this.pSouthEast.getX() - this.pNorthWest.getX(),
                                        this.pSouthEast.getY() - this.pNorthWest.getY()
                                        );
    }
    public void draw(Graphics2D g, boolean simulation, boolean showInside) {
        if (simulation) return;

        if (selected) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.BLACK);
        }
        g.fill(getRegion());
        if (showInside) {
            // not implemented
        }
    }


    @Override
    public NType getNodeType() {
        return NType.GROUP;
    }

    public static String getNodeTypeString() {
        return "Group";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Element toDom(Document dom, String tagname) {
        Element element = super.toDom(dom, getNodeTypeString());

        addAttributesToDom(element);
        for (Enumeration<OBNode> e = children(); e.hasMoreElements();) {
            OBNode child = e.nextElement();
            if (child != null) {
                element.appendChild(child.toDom(dom, null));
            }
        }
        return element;
    }

    public static OBNode fromDom(Element element) {
        MapPartGroup group = new MapPartGroup(0);
        group.getAttributesFromDom(element);
        NodeList elm_children = element.getChildNodes();
        for (int i = 0; i < elm_children.getLength(); ++i) {
            if (elm_children.item(i) instanceof Element) {
                Element node = (Element)elm_children.item(i);
                OBNode child = OBNode.fromDom(node);
                if (child != null) {
                    group.add(child);
                }
            }
        }
        return group;
    }

    /* getters and setters */
    public void setDefaultHeight(double defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public double getDefaultHeight() {
        return defaultHeight;
    }

    public void setMinHeight(double minHeight) {
        this.minHeight = minHeight;
    }

    public double getMinHeight() {
        return minHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getMaxHeight() {
        return maxHeight;
    }
    // tkokada added
    public void setVerticalDivision(int verticalDivision) {
        this.verticalDivision = verticalDivision;
    }
    // tkokada added
    public int getVerticalDivision() {
        return verticalDivision;
    }
    // tkokada added
    public void setHorizontalDivision(int horizontalDivision) {
        this.horizontalDivision = horizontalDivision;
    }
    // tkokada added
    public int getHorizontalDivision() {
        return horizontalDivision;
    }
    // tkokada added
    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    // tkokada added
    public double getRotation() {
        return rotation;
    }
    
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageFileName() {
        return imageFileName;
    }
    
    public void setWest(double west) {
        pNorthWest = new Point2D.Double(west, pNorthWest.getY());
    }
    public double getWest() {
        return pNorthWest.getX();
    }

    public void setNorth(double north) {
        pNorthWest = new Point2D.Double(pNorthWest.getX(), north);
    }
    public double getNorth() {
        return pNorthWest.getY();
    }

    public void setEast(double east) {
        pSouthEast = new Point2D.Double(east, pSouthEast.getY());
    }
    public double getEast() {
        return pSouthEast.getX();
    }

    public void setSouth(double south) {
        pSouthEast = new Point2D.Double(pSouthEast.getX(), south);
    }
    public double getSouth() {
        return pSouthEast.getY();
    }

    @Override
    public String toString() {
        return getTagString();
    }
    
    @Override
    /* verbose output used for hints */
    public String getHintString() {
        return "(" + pNorthWest.toString() + ")-"
        + "(" + pSouthEast.toString() + "), pTheta=" + pTheta + "\n"
        + imageFileName + "\n"
        + "* (sx=" + sx + ", sy=" + sy + ") +(tx="
        + tx + ", ty=" + ty + ") r=" + r
        ;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getScale() {
        return scale;
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList<MapNode> getChildNodes() {
        ArrayList<MapNode> children = new ArrayList<MapNode>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.NODE) {
                children.add((MapNode)node);
            }
        }
        return children;
    }
    @SuppressWarnings("unchecked")
    public ArrayList<MapNode> getChildNodesAndSymlinks() {
        ArrayList<MapNode> children = new ArrayList<MapNode>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.NODE) {
                children.add((MapNode)node);
            } else if (node.getNodeType() == OBNode.NType.SYMLINK) {
                OBNode orig = ((OBNodeSymbolicLink)node).getOriginal();
                if (orig.getNodeType() == OBNode.NType.NODE) {
                    children.add((MapNode)orig);
                }
            }
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<MapLink> getChildLinks() {
        ArrayList<MapLink> children = new ArrayList<MapLink>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.LINK) {
                children.add((MapLink)node);
            }
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<EvacuationAgent> getChildAgents() {
        ArrayList<EvacuationAgent> children = new ArrayList<EvacuationAgent>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.AGENT) {
                children.add((EvacuationAgent)node);
            }
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<MapPartGroup> getChildGroups() {
        ArrayList<MapPartGroup> children = new ArrayList<MapPartGroup>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.GROUP) {
                children.add((MapPartGroup)node);
            }
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<PollutedArea> getChildPollutedAreas() {
        ArrayList<PollutedArea> children = new ArrayList<PollutedArea>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.ROOM) {
                children.add((PollutedArea)node);
            }
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<OBNodeSymbolicLink> getSymbolicLinks() {
        ArrayList<OBNodeSymbolicLink> children = new ArrayList<OBNodeSymbolicLink>();
        Enumeration<OBNode> all_children = children();
        while (all_children.hasMoreElements()) {
            OBNode node = all_children.nextElement();
            if (node.getNodeType() == OBNode.NType.SYMLINK) {
                children.add((OBNodeSymbolicLink)node);
            }
        }
        return children;
    }


    @Override
    public void prepareForSave(boolean hasDisplay) {
        if (hasDisplay)
            if (imageFileName != null) {
                imageFileName =
                    FilePathManipulation.getRelativePath(NetworkMapEditor.getInstance().getDirName(),
                            imageFileName);
            }
    }
    @Override
    public void postLoad(boolean hasDisplay) {
        if (hasDisplay)
            imageFileName = FilePathManipulation.setAbsolutePath(
                    NetworkMapEditor.getInstance().getDirName(), imageFileName);
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
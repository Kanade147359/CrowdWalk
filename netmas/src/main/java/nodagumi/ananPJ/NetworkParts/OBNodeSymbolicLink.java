// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OBNodeSymbolicLink extends OBMapPart {
	/**
	 * エディタ上で別のグループに存在するノードを表示するためのシンボリックリンク．
	 */
	
	private OBNode original = null;
	
	public OBNodeSymbolicLink(int _id) {
		super(_id);
	}
	
	public OBNodeSymbolicLink(int _id,
			OBNode _original) {
		super(_id);
		original = _original;
	}

	public OBNode getOriginal() {
		return original;
	}
	
	public void setOriginal(OBNode _original) {
		original = _original;
	}

	@Override
	public NType getNodeType() {
		return NType.SYMLINK;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public Element toDom(Document dom, String tagname) {
		Element element = super.toDom(dom, getNodeTypeString());
		element.setAttribute("id", "" + ID);
		element.setAttribute("orig", "" + original.ID);
		return element;
	}
		
	public static String getNodeTypeString() {
		return "SymbolicLink";
	}
	
	public static OBNodeSymbolicLink fromDom(Element element) {
		int id = Integer.parseInt(element.getAttribute("id"));
		int orig_id = Integer.parseInt(element.getAttribute("orig"));
		
		OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id);
		symlink.setUserObject(orig_id);

		return symlink;
	}
	
	public String toString() {
		return getTagString();
	}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:

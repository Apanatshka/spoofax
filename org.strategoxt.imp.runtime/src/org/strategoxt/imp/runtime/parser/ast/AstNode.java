package org.strategoxt.imp.runtime.parser.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lpg.runtime.IAst;
import lpg.runtime.IAstVisitor;
import lpg.runtime.IPrsStream;
import lpg.runtime.IToken;

import org.eclipse.core.resources.IResource;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermPrinter;
import org.spoofax.interpreter.terms.InlinePrinter;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.imp.runtime.parser.tokens.SGLRToken;
import org.strategoxt.imp.runtime.stratego.adapter.IStrategoAstNode;

/**
 * A node of an SGLR abstract syntax tree.
 *
 * @author Lennart Kats <L.C.L.Kats add tudelft.nl>
 */
public class AstNode implements IAst, Iterable<AstNode>, IStrategoAstNode, Cloneable {
	// Globally unique object (circumvent interning)

	/** The sort name for strings. */
	public static final String STRING_SORT = new String("<string>");

	// TODO2: Read-only array list
	static final ArrayList<AstNode> EMPTY_LIST = new ArrayList<AstNode>(0);
	
	private ArrayList<AstNode> children;
	
	private final String sort;
	
	private String constructor;
	
	private IStrategoTerm term;
	
	private IToken leftToken, rightToken;
	
	private AstNode parent;
	
	private IStrategoList annotations;
		
	// Accessors
	
	/**
	 * Returns the constructor name of this node, or null if not applicable. 
	 */
	public String getConstructor() {
		return constructor;
	}
	
	public void setConstructor(String constructor) {
		this.constructor = constructor;
	}
	
	public String getSort() {
		return sort;
	}
	
	public boolean isList() {
		return false;
	}
	
	public IResource getResource() {
		return getRoot().getResource();
	}
	
	public SGLRParseController getParseController() {
		return getRoot().getParseController();
	}
	
	// (concrete type exposed by IAst interface)
	public final ArrayList<AstNode> getChildren() {
		assert EMPTY_LIST.size() == 0 && (children.size() == 0 || children.get(0).getParent() == this || this instanceof SubListAstNode);
		
		return children;
	}

	public int getTermType() {
		return IStrategoTerm.APPL;
	}

	/** Get the leftmost token associated with this node. */
	public IToken getLeftIToken() {
		return leftToken;
	}

	/** Get the leftmost token associated with this node. */
	public IToken getRightIToken() {
		return rightToken;
	}

	/** Set the leftmost token associated with this node. */
	protected void setLeftIToken(IToken value) {
		leftToken = value;
	}

	/** Set the leftmost token associated with this node. */
	protected void setRightIToken(IToken value) {
		rightToken = value;
	}

	public AstNode getParent() {
		return parent;
	}

	public void setParent(AstNode value) {
		parent = value;
	}
	
	public RootAstNode getRoot() {
		AstNode result = this;
		while (result.getParent() != null)
			result = result.getParent();
		if (!(result instanceof RootAstNode))
			throw new IllegalStateException("Tree not initialized using RootAstNode.create()");
		else
			return (RootAstNode) result;
	}
	
	public IStrategoList getAnnotations() {
		return annotations;
	}
	
	protected void setAnnotations(IStrategoList annotations) {
		this.annotations = annotations;
	}
	
	public IStrategoTerm getTerm() {
		if (term != null) return term;
		else return Environment.getTermFactory().wrap(this);
	}
	
	// Initialization
	
	/**
	 * Create a new AST node and set it to be the parent node of its children.
	 */
	public AstNode(String sort, IToken leftToken, IToken rightToken, String constructor,
			ArrayList<AstNode> children) {
		
		assert children != null;
		
		this.constructor = constructor;
		this.sort = sort;
		this.leftToken = leftToken;
		this.rightToken = rightToken;
		this.children = children;
		
		assert leftToken != null && rightToken != null;
		setReferences(leftToken, rightToken, children);
	}

	private void setReferences(IToken leftToken, IToken rightToken, ArrayList<AstNode> children) {
		overrideReferences(leftToken, rightToken, children, null);
	}
	
	/**
	 * Set/override references to parent nodes.
	 */
	protected void overrideReferences(IToken leftToken, IToken rightToken, ArrayList<AstNode> children, AstNode oldNode) {
		IPrsStream parseStream = leftToken.getIPrsStream();
		int tokenIndex = leftToken.getTokenIndex();
		int endTokenIndex = rightToken.getTokenIndex();

		// Set ast node for tokens before children, and set parent references
		for (int childIndex = 0, size = children.size(); childIndex < size; childIndex++) {
			AstNode child = children.get(childIndex);
			child.parent = this;
			
			int childStart = child.getLeftIToken().getTokenIndex();
			int childEnd = child.getRightIToken().getTokenIndex();
			
			while (tokenIndex < childStart) {
				SGLRToken token = (SGLRToken) parseStream.getTokenAt(tokenIndex++);
				if (token.getAstNode() == oldNode)
					token.setAstNode(this);
			}
			
			tokenIndex = childEnd + 1; 
		}
		
		// Set ast node for tokens after children
		while (tokenIndex <= endTokenIndex) {
			SGLRToken token = (SGLRToken) parseStream.getTokenAt(tokenIndex++);
			if (token.getAstNode() == oldNode)
				token.setAstNode(this);
		}
	}
	
	// General access
	
	public Iterator<AstNode> iterator() {
		return children.iterator();
	}
	
	/**
	 * Creates a "deep" clone of this AstNode,
	 * but maintains a shallow clone of all tokens,
	 * which still point back to the original AST.
	 */
	public AstNode cloneIgnoreTokens() {
		// TODO: create a better AstNode.clone() method? this is a bit of a cop-out...
		try {
			AstNode result = (AstNode) super.clone();
			ArrayList<AstNode> children = result.children;
			ArrayList<AstNode> newChildren = new ArrayList<AstNode>(children.size());
			for (int i = 0, size = children.size(); i < size; i++) {
				AstNode newChild = children.get(i).cloneIgnoreTokens();
				newChild.parent = result;
				newChildren.add(newChild);
			}
			result.children = newChildren;
			return result;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Deprecated
	public static List<String> getSorts(List<? extends AstNode> children) {
  	  List<String> result = new ArrayList<String>(children.size());
  	  
  	  for (AstNode node : children) {
  		  result.add(node.getSort());
  	  }
  	  
  	  return result;
	}
	
	@Override
	public int hashCode() {
		return getTerm().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IStrategoAstNode) {
			return this == obj || ((IStrategoAstNode) obj).getTerm().equals(getTerm());
		} else {
			return false;
		}
	}
	
	// Visitor support
	
	public void accept(IAstVisitor visitor) {
		if (visitor.preVisit(this)) {
			for (int i = 0, size = children.size(); i < size; i++) {
				children.get(i).accept(visitor);
			}
		}
		
		visitor.postVisit(this);
	}
	
	// LPG legacy/compatibility
	
	/**
	 * Get all children (including the null ones).
	 * 
	 * @deprecated  Unused; ATermAstNode does not include null children.
	 */
	@Deprecated
	public ArrayList<AstNode> getAllChildren() {
		return getChildren();
	}

	@Deprecated
	public IToken[] getPrecedingAdjuncts() {
		return getLeftIToken().getPrecedingAdjuncts();
	}
	
	@Deprecated
	public IToken[] getFollowingAdjuncts() {
		return getRightIToken().getFollowingAdjuncts();
	}

	@Deprecated
	public AstNode getNextAst() {
		return null;
	}

	/**
	 * Pretty prints the AST formed by this node.
	 * 
	 * @see #prettyPrint(ITermPrinter)
	 * @see #yield()
	 */
	@Override
	public final String toString() {
		ITermPrinter result = new InlinePrinter();
		prettyPrint(result);
		return result.getString();
	}
	
	public void prettyPrint(ITermPrinter printer) {
		printer.print(constructor == null ? "<null>" : constructor);
		//sb.append(':');
		//sb.append(sort);
		printer.print("(");
		if (getChildren().size() > 0) {
			getChildren().get(0).prettyPrint(printer);
			for (int i = 1; i < getChildren().size(); i++) {
				printer.print(",");
				getChildren().get(i).prettyPrint(printer);
			}
		}
		printer.print(")");
	}

	/**
	 * Return the input string that formed this AST.
	 */
	public String yield() {
		return getLeftIToken().getIPrsStream().toString(getLeftIToken(), getRightIToken());
	}
}
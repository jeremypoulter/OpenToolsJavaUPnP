package opentools.ILib;

public class ILibXMLNode 
{
		/*! \var Name
			\brief Local Name of the current element
		*/
		public String Name;			// Element Name
		/*! \var NameLength
			\brief Length of \a Name
		*/
		public int NameLength;
		
		/*! \var NSTag
			\brief Namespace Prefix of the current element
			\par
			This can be resolved using a call to \a ILibXML_LookupNamespace(...)
		*/
		public String NSTag;		// Element Prefix
		/*! \var NSLength
			\brief Length of \a NSTag
		*/
		public int NSLength;

		/*! \var StartTag
			\brief boolean indicating if the current element is a start element
		*/
		public int StartTag;		// Non zero if this is a StartElement
		/*! \var EmptyTag
			\brief boolean indicating if this element is an empty element
		*/
		public int EmptyTag;		// Non zero if this is an EmptyElement
		
		//char[] Reserved;		// DO NOT TOUCH
		char[] ReservedEx;
		int ReservedOffset;		// DO NOT TOUCH
		Object Reserved2;	// DO NOT TOUCH
		
		/*! \var Next
			\brief Pointer to the child of the current node
		*/
		public ILibXMLNode Next;			// Next Node
		/*! \var Parent
			\brief Pointer to the Parent of the current node
		*/
		public ILibXMLNode Parent;			// Parent Node
		/*! \var Peer
			\brief Pointer to the sibling of the current node
		*/
		public ILibXMLNode Peer;			// Sibling Node
		public ILibXMLNode ClosingTag;		// Pointer to closing node of this element
		public ILibXMLNode StartingTag;	// Pointer to start node of this element
}

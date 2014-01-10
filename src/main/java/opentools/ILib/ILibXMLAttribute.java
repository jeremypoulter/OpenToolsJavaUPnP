package opentools.ILib;

public class ILibXMLAttribute 
{
	public String Name;						// Attribute Name
	/*! \var NameLength
		\brief Length of \a Name
	*/
	public int NameLength;
	
	/*! \var Prefix
		\brief Namespace Prefix of this attribute
		\par
		This can be resolved by calling \a ILibXML_LookupNamespace(...) and passing in \a Parent as the current node
	*/
	public String Prefix;					// Attribute Namespace Prefix
	/*! \var PrefixLength
		\brief Lenth of \a Prefix
	*/
	public int PrefixLength;
	
	/*! \var Parent
		\brief Pointer to the XML Node that contains this attribute
	*/
	public ILibXMLNode Parent;		// The XML Node this attribute belongs to

	/*! \var Value
		\brief Attribute Value
	*/
	public String Value;					// Attribute Value	
	/*! \var ValueLength
		\brief Length of \a Value
	*/
	public int ValueLength;
	/*! \var Next
		\brief Pointer to the next attribute
	*/
	public ILibXMLAttribute Next;	// Next Attribute
}

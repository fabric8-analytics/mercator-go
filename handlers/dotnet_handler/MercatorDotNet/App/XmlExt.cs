using System;
using System.Xml;
using System.Collections.Generic;

namespace MercatorDotNet
{
	public static class Runtime 
	{
		public static Dictionary<string, XmlNamespaceManager> XmlManagers = new Dictionary<string, XmlNamespaceManager>();
		public static XmlNamespaceManager CurrentManager;

		public static XmlNamespaceManager RegisterNamespaceManager(string prefix, string ns, XmlNameTable nameTable) 
		{
			XmlNamespaceManager mgr = new XmlNamespaceManager(nameTable);
			mgr.AddNamespace (prefix, ns);
			XmlManagers.Add (prefix, mgr);
			return mgr;
		}
	}

	public class ManagerContext : IDisposable 
	{
		public XmlNamespaceManager OldManager { get; private set; }

		public ManagerContext(string xmlMan) 
		{
			OldManager = Runtime.CurrentManager;
			Runtime.CurrentManager = Runtime.XmlManagers [xmlMan];
		}

		public void Dispose() 
		{
			Runtime.CurrentManager = OldManager;
		}
	}

	public static class XmlExt
	{
		public static string GetKeyValue(this XmlAttributeCollection col, string key) 
		{
			if (col.HasKey (key)) {
				return col [key].Value;
			}

			return null;
		}

		public static bool HasKey(this XmlAttributeCollection col, string key) 
		{
			return col [key] != null;
		}
	}
}


using System;
using Onion.SolutionParser.Parser;
using Onion.SolutionParser.Parser.Model;
using System.Collections.Generic;
using System.Xml;
using System.IO;
using NuGet;
using Newtonsoft.Json;

namespace MercatorDotNet.App.Providers
{
	public class ReferenceInclude : IComparable<ReferenceInclude> 
	{
		public string Include { get; private set; }
		public string HintPath { get; private set; }

		private void loadFromXml(XmlNode node, string projectBase) 
		{
			string include = node.Attributes["Include"].Value;

			XmlNode hintNode = node.SelectSingleNode ("ns:HintPath", Runtime.CurrentManager);
			if (hintNode != null) {
				this.HintPath = Path.Combine(projectBase, node.InnerText).CanonicalPath();
			}

			this.Include = include;
		}

		public ReferenceInclude(XmlNode node, string projectBase) 
		{
			this.loadFromXml (node, projectBase);
		}

		public ReferenceInclude(string include, string hintPath) 
		{
			this.Include = include;
			this.HintPath = hintPath;
		}

		public override string ToString ()
		{
			return string.Format ("[ReferenceInclude: Include={0}, HintPath={1}]", Include, HintPath);
		}

		public int CompareTo(ReferenceInclude other) 
		{
			return this.Include.CompareTo (other.Include);
		}
	}

	public class PackageRequirement 
	{
		public string Name { get; set; }
		public IVersionSpec Version { get; set; }
		public string TargetFramework { get; set; }

		public override string ToString ()
		{
			return string.Format ("[PackageRequirement: Name={0}, Version={1}, TargetFramework={2}]", Name, Version, TargetFramework);
		}
	}

	public class SolutionMetadataProvider : IMetadataProvider
	{
		public static string PackageFile = "packages.config";

		private ISolution _solution;
                private string _path;

		public void LoadPath(string path)
		{
                        this._path = path;
			this._solution = SolutionParser.Parse (path);
		}

		private static IList<PackageRequirement> GetPackageRequirements(string path)
		{
			string configPath = Path.Combine (path, PackageFile);
			List<PackageRequirement> items = new List<PackageRequirement> ();

			using (FileStream fs = File.OpenRead (configPath)) 
			{
				var doc = new XmlDocument ();
				doc.Load (fs);

				XmlNode root = doc.DocumentElement;
				foreach (XmlNode node in root.SelectNodes ("package")) 
				{
					string frameworkString = node.Attributes.GetKeyValue("targetFramework");

					items.Add (new PackageRequirement { 
						Name = node.Attributes["id"].Value, 
						Version = VersionUtility.ParseVersionSpec(node.Attributes["version"].Value),
						TargetFramework = frameworkString});
				}
			}

			return items;
		}

		public string DataAsJSON() {
			if (this._solution == null)
				throw new InvalidOperationException("The solution is null, call Parse() first");

			List<ReferenceInclude> references = new List<ReferenceInclude> ();
			foreach (Project project in this._solution.Projects) 
			{
                                string sanitized = project.Path.Replace('\\', '/');
				string projectPath = Path.Combine(Path.GetDirectoryName(this._path), sanitized);
				string dirName = Path.GetDirectoryName (projectPath);
				using (FileStream fs = File.OpenRead (projectPath)) 
				{
					var doc = new XmlDocument ();
					doc.Load (fs);

					Runtime.RegisterNamespaceManager ("ns", "http://schemas.microsoft.com/developer/msbuild/2003", doc.NameTable);
					using (ManagerContext ctx = new ManagerContext ("ns")) 
					{
						XmlNode root = doc.DocumentElement;
						foreach (XmlNode node in root.SelectNodes ("ns:ItemGroup/ns:Reference", Runtime.CurrentManager)) 
						{
							references.Add (new ReferenceInclude (node, dirName));
						}

						references.Sort ();
					}
				}
			}

			return JsonConvert.SerializeObject(new { references = references });
		}
	}
}


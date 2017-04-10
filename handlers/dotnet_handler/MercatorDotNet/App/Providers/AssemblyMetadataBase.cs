using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace MercatorDotNet.App.Providers
{
    public class AssemblyMetadataBase
    {
        protected Assembly _data;

        internal static Dictionary<Type, string> attributeMapping = new Dictionary<Type, string>
        {
            { typeof( AssemblyTitleAttribute ), "name" },
            { typeof( AssemblyDescriptionAttribute ), "description" },
            { typeof( AssemblyConfigurationAttribute ), "configuration" },
            { typeof( AssemblyCompanyAttribute ), "company" },
            { typeof( AssemblyProductAttribute ), "product" },
            { typeof( AssemblyCopyrightAttribute ), "copyright" },
            { typeof( AssemblyTrademarkAttribute ), "trademark" },
            { typeof( AssemblyFileVersionAttribute ), "file_version" },
            { typeof( AssemblyVersionAttribute ), "assembly_version" },
            { typeof( GuidAttribute ), "guid" },
        };

        public virtual Dictionary<string, string> LoadAssemblyInfo(Assembly assembly)
        {
            var output = new Dictionary<string, string> {
                { "version", assembly.GetName().Version.ToString() }
            };

            foreach (var attribute in assembly.GetCustomAttributesData())
            {
                string name;
                if (attributeMapping.TryGetValue(attribute.AttributeType, out name))
                {
                    var arg = attribute.ConstructorArguments.First();
                    string value = arg.Value as string;
                    if (!string.IsNullOrWhiteSpace(value))
                        output.Add(name, value);
                }
            }

            return output;
        }
    }
}

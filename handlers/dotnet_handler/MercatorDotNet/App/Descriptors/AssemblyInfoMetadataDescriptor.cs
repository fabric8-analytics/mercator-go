using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MercatorDotNet.App.Providers;

namespace MercatorDotNet.App.Descriptors
{
    public class AssemblyInfoMetadataDescriptor : IMetadataDescriptor
    {
        public bool CheckPath(string path)
        {
            return Path.GetFileName(path) == "AssemblyInfo.cs";
        }

        public IMetadataProvider CreateProvider()
        {
            return new AssemblyInfoMetadataProvider();
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace MercatorDotNet.App.Providers
{
    public class AssemblyDllMetadataProvider : AssemblyMetadataBase, IMetadataProvider
    {
        public void LoadPath(string path)
        {
            if (string.IsNullOrWhiteSpace(path) || !File.Exists(path))
                throw new ArgumentException("invalid path");

            this._data = Assembly.LoadFile(path);
        }

        public string DataAsJSON()
        {
            return JsonConvert.SerializeObject(this.LoadAssemblyInfo(this._data), Formatting.Indented);
        }
    }
}

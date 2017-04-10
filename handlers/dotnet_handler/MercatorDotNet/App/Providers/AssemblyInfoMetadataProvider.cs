using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.CSharp;
using Newtonsoft.Json;

namespace MercatorDotNet.App.Providers
{
    public class AssemblyInfoMetadataProvider : AssemblyMetadataBase, IMetadataProvider
    {
        public void LoadPath(string path)
        {
            if (string.IsNullOrWhiteSpace(path) || !File.Exists(path))
                throw new ArgumentException("invalid path");

            using (var provider = new CSharpCodeProvider())
            {
                var compilerParameters = new CompilerParameters(new string[]{"System"}, "MercatorDotNet", false);
                var result = provider.CompileAssemblyFromSource(compilerParameters, File.ReadAllText(path));
                if (result.Errors.Count > 0)
                    throw new Exception("Invalid AssemblyInfo.cs file");

                this._data = result.CompiledAssembly;
            }
        }

        public string DataAsJSON()
        {
            return JsonConvert.SerializeObject(this.LoadAssemblyInfo(this._data), Formatting.Indented);
        }
    }
}

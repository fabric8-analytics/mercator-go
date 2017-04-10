using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using NuGet;
using Newtonsoft.Json;

namespace MercatorDotNet.App.Providers
{
    public class NupkgMetadataProvider : IMetadataProvider
    {
        private ZipPackage _data;

        public void LoadPath(string path)
        {
            ZipPackage zp = new ZipPackage(path);
            this._data = zp;
        }

        public string DataAsJSON()
        {
            return JsonConvert.SerializeObject(this._data, Formatting.Indented);
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using NuGet;

namespace MercatorDotNet.App.Providers
{
    public class NuspecMetadataProvider : IMetadataProvider
    {
        /// <summary>
        /// Obtain NuGet file manifest from `stream`
        /// </summary>
        /// <param name="stream"></param>
        /// <returns></returns>
        private static Manifest getManifestFromStream(Stream stream)
        {
            try
            {
                // First try to load the manifest with schema validation on
                Manifest m = Manifest.ReadFrom(stream, true);
                return m;
            }
            catch (Exception)
            {
                // Now try to load without schema validation
                // TODO: Note the fact that the manifest failed schema validation in the returned metadata
                Manifest m2 = Manifest.ReadFrom(stream, false);
                return m2;
            }
        }

        private Manifest _data;

        public void LoadPath(string path)
        {
            using (FileStream fs = File.OpenRead(path))
            {
                this._data = getManifestFromStream(fs);
            }
        }

        public string DataAsJSON()
        {
            return JsonConvert.SerializeObject(this._data, Formatting.Indented);
        }
    }
}

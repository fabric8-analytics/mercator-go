using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MercatorDotNet.App.Providers
{
    public interface IMetadataProvider
    {
        void LoadPath(string path);
        string DataAsJSON();
    }
}

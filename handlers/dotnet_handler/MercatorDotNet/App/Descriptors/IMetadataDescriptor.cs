using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MercatorDotNet.App.Providers;

namespace MercatorDotNet.App.Descriptors
{
    public interface IMetadataDescriptor
    {
        bool CheckPath(string path);
        IMetadataProvider CreateProvider();
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MercatorDotNet
{
    public static class PathExt
    {
        /// <summary>
        /// OS invariant canonicalization of a path
        /// </summary>
        /// <param name="path"></param>
        /// <returns></returns>
        public static string CanonicalPath(this string path)
        {
            return new Uri(Path.GetFullPath(path)).LocalPath;
        }
    }
}

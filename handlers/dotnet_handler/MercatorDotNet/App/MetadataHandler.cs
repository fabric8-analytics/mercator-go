using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MercatorDotNet.App.Descriptors;
using MercatorDotNet.App.Providers;

namespace MercatorDotNet.App
{
    public class MetadataHandler
    {
        /// <summary>
        /// Get all the available descriptors
        /// </summary>
        /// <returns></returns>
        static IEnumerable<IMetadataDescriptor> GetMetadataDescriptors()
        {
            return from assembly in AppDomain.CurrentDomain.GetAssemblies()
                   from type in assembly.GetExportedTypes()
                   where type.GetInterfaces().Contains(typeof(IMetadataDescriptor))
                   select Activator.CreateInstance(type) as IMetadataDescriptor;
        }

        /// <summary>
        /// Parse descriptors from string, this method throws if an invalid descriptor is specified
        /// </summary>
        /// <param name="descriptors"></param>
        /// <returns></returns>
        static IEnumerable<IMetadataDescriptor> ParseDescriptors(IEnumerable<string> descriptors)
        {
            return from desc in descriptors
                   let full = string.Format("MercatorDotNet.App.Descriptors.{0}MetadataDescriptor", desc)
                   let type = Type.GetType(full, true, true)
                   select Activator.CreateInstance(type) as IMetadataDescriptor;
        }

        private IEnumerable<IMetadataDescriptor> _descriptors;

        public MetadataHandler(IEnumerable<String> definitions = null)
        {
            this._descriptors = definitions != null ? ParseDescriptors(definitions) : GetMetadataDescriptors();
        }

        /// <summary>
        /// Find matching descriptor for the given <paramref name="path"/> and return it's <see cref="MetadataProvider"/>
        /// </summary>
        /// <param name="path"></param>
        /// <returns></returns>
        public IMetadataProvider FindProvider(string path)
        {
            foreach (var desc in this._descriptors)
                if (desc.CheckPath(path))
                    return desc.CreateProvider();

            return null;
        }
    }
}

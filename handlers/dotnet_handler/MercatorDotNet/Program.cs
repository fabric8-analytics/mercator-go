using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using MercatorDotNet.App.Descriptors;
using MercatorDotNet.App;
using MercatorDotNet.App.Providers;
using Newtonsoft.Json;
using System.Threading;
using System.Globalization;
using System.Diagnostics;

namespace MercatorDotNet
{
    /// <summary>
    /// Command line options
    /// </summary>
    class Options
    {
        /// <summary>
        /// Target file system path (file)
        /// </summary>
        public string Path { get; set; }
        /// <summary>
        /// Specify which metadata descriptors to use
        /// </summary>
        public IEnumerable<String> Descriptors { get; set; }
    }

    class Program
    {
        /// <summary>
        /// End program with error code and a JSON message
        /// </summary>
        /// <param name="code"></param>
        /// <param name="message"></param>
        static void EndProgram(int code, object message)
        {
            var error = new { error = message };
            Console.WriteLine(JsonConvert.SerializeObject(error, Formatting.Indented));
#if DEBUG
            Console.ReadLine();
#endif
            Environment.Exit(code);
        }

        /// <summary>
        /// Resolve assembly from embedded resource data
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="args"></param>
        static Assembly CurrentDomain_AssemblyResolve (object sender, ResolveEventArgs args)
        {
                var resourceName = 
                        string.Format("MercatorDotNet.Resources.{0}.dll", new AssemblyName(args.Name).Name);
                var asm = Assembly.GetExecutingAssembly ();

                using (var stream = asm.GetManifestResourceStream(resourceName))
                {
                        var assemblyData = new byte[stream.Length];
                        stream.Read(assemblyData, 0, assemblyData.Length);
                        return Assembly.Load(assemblyData);
                }
        }

        /// <summary>
        /// If we fail with an unhandled exception print out the information as JSON and exit
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        static void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            var exc = e.ExceptionObject as Exception;
            if (exc != null)
            {
                var trace = new StackTrace(exc);
                var frames = from t in
                               from frame in trace.GetFrames()
                               select frame.GetMethod()
                             where t != null
                             select t.ToString().Split(' ')[1];

                var exception = new { message = exc.Message, frames = frames.ToArray().Reverse() };
                EndProgram(1, exception);
            }
            else
            {
                EndProgram(1, "An unknown error has occured");
            }
        }

        /// <summary>
        /// Parse input arguments into <see cref="Options"/>
        /// </summary>
        /// <param name="args"></param>
        /// <returns></returns>
        static Options ParseArgs(string[] args)
        {
            if (args.Length == 0)
                throw new ArgumentException("No arguments");
            else if (args.Length == 2 || args.Length > 3)
                throw new ArgumentException("Invalid number of arguments");

            var isSimple = args.Length == 1;
            if (!isSimple)
            {
                if (args[0] != "-d" && args[0] != "--descriptors")
                    throw new ArgumentException(string.Format("Unknown flag: {0}", args[0]));

                return new Options { Descriptors = args[1].Split(','), Path = args[2] };
            }
            else
            {
                return new Options { Path = args[0] };
            }
        }

        static void Main(string[] args)
        {
            AppDomain.CurrentDomain.AssemblyResolve += CurrentDomain_AssemblyResolve;
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;

            var opts = ParseArgs(args);
            var canonical = opts.Path.CanonicalPath();
            var handler = new MetadataHandler(opts.Descriptors);

            var provider = handler.FindProvider(canonical);

            if (provider != null)
            {
                provider.LoadPath(canonical);
                Console.WriteLine(provider.DataAsJSON());
            }
            else
            {
                EndProgram(1, string.Format("No metadata provider found for file {0}", canonical));
            }
#if DEBUG
            Console.ReadLine();
#endif
        }
    }
}

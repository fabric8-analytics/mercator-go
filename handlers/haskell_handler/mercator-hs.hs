{-# LANGUAGE DeriveGeneric #-}
-- Copyright (C) 2016 Red Hat, Inc.
--
-- Mercator is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Lesser General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
-- Mercator is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Lesser General Public License for more details.
-- You should have received a copy of the GNU Lesser General Public License
-- along with Mercator. If not, see <http://www.gnu.org/licenses/>.
import           Data.Aeson.Text
import           Data.Aeson.Types
import           Data.Char                             (toLower)
import           Data.Maybe                            (fromMaybe)
import           Data.Text.Lazy                        (unpack)
import           Distribution.Package
import           Distribution.PackageDescription
import           Distribution.PackageDescription.Parse
import           Distribution.Text                     (display)
import           GHC.Generics                          (Generic)
import           System.Directory                      (doesFileExist)
import           System.Environment                    (getArgs)
import           System.FilePath.Posix


-- |Output structure that will be serialized as JSON
--
data Metadata = Metadata
 {
   mdName         :: String,
   mdLicense      :: String,
   mdCopyright    :: String,
   mdMaintainer   :: String,
   mdAuthor       :: String,
   mdHomePage     :: String,
   mdIssueUrl     :: String,
   mdRepos        :: [Repository],
   mdCategory     :: String,
   mdDescription  :: String,
   mdDependencies :: [String],
   mdLockedDeps   :: [String]
 } deriving (Eq, Ord, Generic)


-- |Simple repository structure, name can be branch/tag
--
data Repository = Repository
 {
    rpType :: String,
    rpURL  :: String,
    rpName :: String
 } deriving (Eq, Ord, Generic)


-- |Implement automagic serialization
--
instance ToJSON Repository
instance ToJSON Metadata


main :: IO ()
main = getArgs >>= processArgs


-- |Process comannd line arguments
--
--  If there's zero or more than one argument print out JSON formatted error.
--
--  If there's a single argument try to parse it as Cabal spec file and
--  return the JSON metadata or if the operation fails an error string.
--
processArgs :: [String] -> IO ()
processArgs [] = putStrLn "{\"error\": \"expecting one argument\"}"
--processArgs [a,b] = do TODO: Lockfile support
processArgs [a] = do
  let dir = takeDirectory a
  --    lockfile = dir </> "cabal.config"
  --checkLockfile <- doesFileExist lockfile
  fileData <- readFile a
  case parsePackageDescription fileData of
    ParseFailed _ -> putStrLn "{\"error\": \"error parsing cabal spec\"}"
    ParseOk _ d -> putStrLn . unpack . encodeToLazyText $ getMetadataObject d []
processArgs _ = putStrLn "{\"error\": \"too many arguments\"}"


-- |Construct metadata object from internal GenericPackageDescription and
--  return it
--
getMetadataObject :: GenericPackageDescription -> [String] -> Metadata
getMetadataObject d = Metadata
                            (getPackageName                               d)
                            (getLicense                                   d)
                            (copyright   . packageDescription $           d)
                            (maintainer  . packageDescription $           d)
                            (author      . packageDescription $           d)
                            (homepage    . packageDescription $           d)
                            (bugReports  . packageDescription $           d)
                            (getRepos                                     d)
                            (category    . packageDescription $           d)
                            (description . packageDescription $           d)
                            (getDeps                                      d)


-- |Convert from internal SourceRepo type to our simplified Repository
--
toRepository :: SourceRepo -> Repository
toRepository r = do
  let rb = repoBranch r
      rg = repoTag    r
      rn = fromMaybe (fromMaybe "master" rg) rb
      rt = map toLower  $ case repoType r of
                            Just t  -> display t
                            Nothing -> ""
      rl = fromMaybe "" $ repoLocation r
  Repository rt rl rn


getRepos :: GenericPackageDescription -> [Repository]
getRepos d = map toRepository (sourceRepos . packageDescription $ d)


getLicense :: GenericPackageDescription -> String
getLicense = display . license . packageDescription


getPackageName :: GenericPackageDescription -> String
getPackageName = display . pkgName . package . packageDescription


getDeps :: GenericPackageDescription -> [String]
getDeps = map showDep . extractDeps


-- |Format dependency as string
--
showDep :: Dependency -> String
showDep (Dependency name spec) = unPackageName name ++ " " ++ display spec


-- |Extract executable & library dependencies with default CondVars
--
extractDeps :: GenericPackageDescription -> [Dependency]
extractDeps d = ldeps ++ edeps
  where ldeps = case condLibrary d of
                  Nothing -> []
                  Just c  -> condTreeConstraints c
        edeps = concatMap (condTreeConstraints . snd) (condExecutables d)

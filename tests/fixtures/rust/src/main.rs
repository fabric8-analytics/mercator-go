extern crate cargo;
extern crate rustc_serialize;

use std::env;
use std::collections::HashMap;
use std::path::Path;
use cargo::core::Package;
use cargo::util::Config;
use rustc_serialize::json;

#[derive(RustcEncodable)]
struct Metadata {
     package: Package,
     authors: Vec<String>,
     keywords: Vec<String>,
     license: Option<String>,
     license_file: Option<String>,
     description: Option<String>,
     readme: Option<String>,
     homepage: Option<String>,
     repository: Option<String>,
     documentation: Option<String>,
}

impl Metadata {
    fn new(package: Package) -> Metadata {
        let meta = package.manifest().metadata().clone();
        Metadata { package: package, authors: meta.authors, keywords: meta.keywords,
                   license: meta.license, license_file: meta.license_file,
                   description: meta.description, readme: meta.readme, 
                   homepage: meta.homepage, repository: meta.repository,
                   documentation: meta.documentation }
    }
}

fn error(message: &str) -> String {
    let mut status = HashMap::<&str, &str>::new();
    status.insert("error", message);
    format!("{}", json::as_pretty_json(&status))
}

fn main() {
    let output = match env::args().nth(1) {
        Some(arg) => {
            let path = Path::new(&arg);
            match Config::default() {
                Ok(config) => {
                    match Package::for_path(&path, &config) {
                        Ok(package) => {
                            let metadata = Metadata::new(package);
                            format!("{}", json::as_pretty_json(&metadata))
                        }, Err(_) => error("error processing package")
                    }
                }, Err(_) => error("no configuration")
            }
        }, None => error("no arguments")
    };

    println!("{}", output);
}

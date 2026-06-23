use std::{env, path::PathBuf};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    emit_macos_swift_runtime_rpath();

    let out_dir = PathBuf::from(env::var("OUT_DIR")?);
    let descriptor_path = out_dir.join("semantic_descriptor.bin");

    tonic_prost_build::configure()
        .file_descriptor_set_path(&descriptor_path)
        .compile_protos(&["proto/common.proto", "proto/semantic.proto"], &["proto"])?;

    println!("cargo:rerun-if-changed=proto/common.proto");
    println!("cargo:rerun-if-changed=proto/semantic.proto");

    Ok(())
}

#[cfg(target_os = "macos")]
fn emit_macos_swift_runtime_rpath() {
    println!("cargo:rustc-link-arg=-Wl,-rpath,/usr/lib/swift");
}

#[cfg(not(target_os = "macos"))]
fn emit_macos_swift_runtime_rpath() {}

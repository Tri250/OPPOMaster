use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    sync::{Arc, Mutex},
};

use tonic::{Request, Response, Status};

use crate::proto::semantic::{
    CancelModelDownloadRequest, CancelModelDownloadResponse, DeleteModelRequest,
    DownloadModelRequest, GetModelDownloadStatusRequest, ListInstalledModelsRequest,
    ListInstalledModelsResponse, ListModelProfilesRequest, ListModelProfilesResponse, ModelAsset,
    ModelManagerResponse, ModelProfile, ResolvedModelManifest as ProtoResolvedModelManifest,
    ValidateModelRequest, model_manager_service_server::ModelManagerService,
};
use crate::service::model_assets::{
    ModelAssetSpec, ModelProfileStatus, ResolvedModelManifest, delete_model_profile,
    download_model_profile, list_installed_profiles, list_profiles, validate_model_profile,
};

#[derive(Debug, Clone)]
pub struct ModelManagerServiceImpl {
    default_model_root: PathBuf,
    default_hf_endpoint: String,
    downloads: Arc<Mutex<HashMap<String, DownloadJob>>>,
}

#[derive(Debug, Clone)]
struct DownloadJob {
    profile_id: String,
    root: PathBuf,
    state: String,
    error: String,
    manifest: Option<ResolvedModelManifest>,
    cancel_requested: bool,
}

impl ModelManagerServiceImpl {
    pub fn new(
        default_model_root: impl Into<PathBuf>,
        default_hf_endpoint: impl Into<String>,
    ) -> Self {
        Self {
            default_model_root: default_model_root.into(),
            default_hf_endpoint: default_hf_endpoint.into(),
            downloads: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    fn base_root(&self, model_root: &str) -> PathBuf {
        if model_root.trim().is_empty() {
            self.default_model_root.clone()
        } else {
            PathBuf::from(model_root)
        }
    }

    fn profile_root(&self, profile_id: &str, model_root: &str) -> PathBuf {
        let base = self.base_root(model_root);
        if base.file_name().and_then(|name| name.to_str()) == Some(profile_id) {
            base
        } else {
            base.join(profile_id)
        }
    }

    fn hf_endpoint<'a>(&'a self, hf_endpoint: &'a str) -> &'a str {
        if hf_endpoint.trim().is_empty() {
            &self.default_hf_endpoint
        } else {
            hf_endpoint
        }
    }

    fn response_from_result(
        status: &'static str,
        job_id: &str,
        profile_id: &str,
        root: &Path,
        result: anyhow::Result<ResolvedModelManifest>,
    ) -> ModelManagerResponse {
        match result {
            Ok(manifest) => {
                let profile = crate::service::model_assets::find_profile(profile_id).ok();
                ModelManagerResponse {
                    ok: true,
                    status: status.to_string(),
                    error: String::new(),
                    job_id: job_id.to_string(),
                    profile: profile.map(|profile| {
                        status_to_proto(&ModelProfileStatus {
                            profile,
                            model_root: root.to_path_buf(),
                            installed: true,
                            status: status.to_string(),
                        })
                    }),
                    manifest: Some(manifest_to_proto(&manifest)),
                }
            }
            Err(err) => ModelManagerResponse {
                ok: false,
                status: "error".to_string(),
                error: err.to_string(),
                job_id: job_id.to_string(),
                profile: crate::service::model_assets::find_profile(profile_id)
                    .ok()
                    .map(|profile| {
                        status_to_proto(&ModelProfileStatus {
                            profile,
                            model_root: root.to_path_buf(),
                            installed: false,
                            status: err.to_string(),
                        })
                    }),
                manifest: None,
            },
        }
    }

    fn response_from_job(job_id: &str, job: &DownloadJob) -> ModelManagerResponse {
        let profile = crate::service::model_assets::find_profile(&job.profile_id).ok();
        ModelManagerResponse {
            ok: job.state == "installed" || job.state == "downloading" || job.state == "queued",
            status: job.state.clone(),
            error: job.error.clone(),
            job_id: job_id.to_string(),
            profile: profile.map(|profile| {
                status_to_proto(&ModelProfileStatus {
                    profile,
                    model_root: job.root.clone(),
                    installed: job.state == "installed",
                    status: job.state.clone(),
                })
            }),
            manifest: job.manifest.as_ref().map(manifest_to_proto),
        }
    }

    fn next_job_id(profile_id: &str) -> String {
        let nanos = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|duration| duration.as_nanos())
            .unwrap_or_default();
        format!("{profile_id}-{nanos}")
    }
}

#[tonic::async_trait]
impl ModelManagerService for ModelManagerServiceImpl {
    async fn list_model_profiles(
        &self,
        request: Request<ListModelProfilesRequest>,
    ) -> Result<Response<ListModelProfilesResponse>, Status> {
        let req = request.into_inner();
        let base = self.base_root(&req.model_root);
        let profiles = list_profiles(Some(&base))
            .iter()
            .map(status_to_proto)
            .collect();
        Ok(Response::new(ListModelProfilesResponse { profiles }))
    }

    async fn list_installed_models(
        &self,
        request: Request<ListInstalledModelsRequest>,
    ) -> Result<Response<ListInstalledModelsResponse>, Status> {
        let req = request.into_inner();
        let base = self.base_root(&req.model_root);
        let profiles = list_installed_profiles(Some(&base))
            .iter()
            .map(status_to_proto)
            .collect();
        Ok(Response::new(ListInstalledModelsResponse { profiles }))
    }

    async fn validate_model(
        &self,
        request: Request<ValidateModelRequest>,
    ) -> Result<Response<ModelManagerResponse>, Status> {
        let req = request.into_inner();
        let root = self.profile_root(&req.profile_id, &req.model_root);
        let response = Self::response_from_result(
            "installed",
            "",
            &req.profile_id,
            &root,
            validate_model_profile(&req.profile_id, &root),
        );
        Ok(Response::new(response))
    }

    async fn download_model(
        &self,
        request: Request<DownloadModelRequest>,
    ) -> Result<Response<ModelManagerResponse>, Status> {
        let req = request.into_inner();
        let root = self.profile_root(&req.profile_id, &req.model_root);
        if let Err(err) = crate::service::model_assets::find_profile(&req.profile_id) {
            let response =
                Self::response_from_result("error", "", &req.profile_id, &root, Err(err));
            return Ok(Response::new(response));
        }

        let job_id = Self::next_job_id(&req.profile_id);
        let endpoint = self.hf_endpoint(&req.hf_endpoint).to_string();
        {
            let mut downloads = self
                .downloads
                .lock()
                .map_err(|_| Status::internal("download registry lock poisoned"))?;
            downloads.insert(
                job_id.clone(),
                DownloadJob {
                    profile_id: req.profile_id.clone(),
                    root: root.clone(),
                    state: "queued".to_string(),
                    error: String::new(),
                    manifest: None,
                    cancel_requested: false,
                },
            );
        }

        let downloads = self.downloads.clone();
        let profile_id = req.profile_id.clone();
        let job_id_for_thread = job_id.clone();
        std::thread::spawn(move || {
            if let Ok(mut jobs) = downloads.lock() {
                if let Some(job) = jobs.get_mut(&job_id_for_thread) {
                    job.state = "downloading".to_string();
                }
            }

            let result = download_model_profile(&profile_id, &root, &endpoint, None);
            if let Ok(mut jobs) = downloads.lock() {
                if let Some(job) = jobs.get_mut(&job_id_for_thread) {
                    if job.cancel_requested {
                        let _ = delete_model_profile(&profile_id, &root);
                        job.state = "cancelled".to_string();
                        job.error.clear();
                        job.manifest = None;
                        return;
                    }

                    match result {
                        Ok(manifest) => {
                            job.state = "installed".to_string();
                            job.error.clear();
                            job.manifest = Some(manifest);
                        }
                        Err(err) => {
                            job.state = "failed".to_string();
                            job.error = err.to_string();
                            job.manifest = None;
                        }
                    }
                }
            }
        });

        let response = {
            let downloads = self
                .downloads
                .lock()
                .map_err(|_| Status::internal("download registry lock poisoned"))?;
            downloads
                .get(&job_id)
                .map(|job| Self::response_from_job(&job_id, job))
                .unwrap_or_else(|| ModelManagerResponse {
                    ok: false,
                    status: "error".to_string(),
                    error: "download job disappeared".to_string(),
                    job_id: job_id.clone(),
                    profile: None,
                    manifest: None,
                })
        };
        Ok(Response::new(response))
    }

    async fn get_model_download_status(
        &self,
        request: Request<GetModelDownloadStatusRequest>,
    ) -> Result<Response<ModelManagerResponse>, Status> {
        let req = request.into_inner();
        let downloads = self
            .downloads
            .lock()
            .map_err(|_| Status::internal("download registry lock poisoned"))?;
        let response = downloads
            .get(&req.job_id)
            .map(|job| Self::response_from_job(&req.job_id, job))
            .unwrap_or_else(|| ModelManagerResponse {
                ok: false,
                status: "unknown_job".to_string(),
                error: format!("unknown model download job {}", req.job_id),
                job_id: req.job_id,
                profile: None,
                manifest: None,
            });
        Ok(Response::new(response))
    }

    async fn cancel_model_download(
        &self,
        request: Request<CancelModelDownloadRequest>,
    ) -> Result<Response<CancelModelDownloadResponse>, Status> {
        let req = request.into_inner();
        let mut downloads = self
            .downloads
            .lock()
            .map_err(|_| Status::internal("download registry lock poisoned"))?;
        let Some(job) = downloads.get_mut(&req.job_id) else {
            return Ok(Response::new(CancelModelDownloadResponse {
                cancelled: false,
                message: format!("unknown model download job {}", req.job_id),
            }));
        };

        if job.state == "installed" || job.state == "failed" || job.state == "cancelled" {
            return Ok(Response::new(CancelModelDownloadResponse {
                cancelled: false,
                message: format!("download job is already {}", job.state),
            }));
        }

        job.cancel_requested = true;
        job.state = "cancel_requested".to_string();
        Ok(Response::new(CancelModelDownloadResponse {
            cancelled: true,
            message: "download cancellation requested".to_string(),
        }))
    }

    async fn delete_model(
        &self,
        request: Request<DeleteModelRequest>,
    ) -> Result<Response<ModelManagerResponse>, Status> {
        let req = request.into_inner();
        let root = self.profile_root(&req.profile_id, &req.model_root);
        let result = delete_model_profile(&req.profile_id, &root).and_then(|_| {
            validate_model_profile(&req.profile_id, &root)
                .map_err(|_| anyhow::anyhow!("model profile deleted"))
        });
        let mut response =
            Self::response_from_result("deleted", "", &req.profile_id, &root, result);
        if !response.ok && response.error == "model profile deleted" {
            response.ok = true;
            response.status = "deleted".to_string();
            response.error.clear();
        }
        Ok(Response::new(response))
    }
}

fn asset_to_proto(asset: &ModelAssetSpec, root: &Path) -> ModelAsset {
    ModelAsset {
        role: asset.role.as_str().to_string(),
        repo_id: asset.repo_id.to_string(),
        revision: asset.revision.to_string(),
        remote_path: asset.remote_path.to_string(),
        local_path: root.join(asset.local_path).to_string_lossy().into_owned(),
        size_bytes: asset.size_bytes,
        sha256: asset.sha256.unwrap_or_default().to_string(),
    }
}

fn status_to_proto(status: &ModelProfileStatus) -> ModelProfile {
    ModelProfile {
        profile_id: status.profile.profile_id.to_string(),
        display_name: status.profile.display_name.to_string(),
        model_id: status.profile.model_id.to_string(),
        revision: status.profile.revision.to_string(),
        engine_profile_id: status.profile.engine_profile_id.to_string(),
        language: status.profile.language.as_str().to_string(),
        embedding_dimension: status.profile.embedding_dimension,
        native_embedding_dimension: status.profile.native_embedding_dimension,
        image_size: status.profile.image_size,
        installed: status.installed,
        local_root: status.model_root.to_string_lossy().into_owned(),
        status: status.status.clone(),
        assets: status
            .profile
            .assets
            .iter()
            .map(|asset| asset_to_proto(asset, &status.model_root))
            .collect(),
    }
}

fn manifest_to_proto(manifest: &ResolvedModelManifest) -> ProtoResolvedModelManifest {
    ProtoResolvedModelManifest {
        profile_id: manifest.profile_id.clone(),
        model_id: manifest.model_id.clone(),
        revision: manifest.revision.clone(),
        engine_profile_id: manifest.engine_profile_id.clone(),
        language: manifest.language.clone(),
        embedding_dimension: manifest.embedding_dimension,
        native_embedding_dimension: manifest.native_embedding_dimension,
        image_size: manifest.image_size,
        model_root: manifest.model_root.clone(),
        assets: manifest
            .assets
            .iter()
            .map(|asset| ModelAsset {
                role: asset.role.clone(),
                repo_id: asset.repo_id.clone(),
                revision: asset.revision.clone(),
                remote_path: asset.remote_path.clone(),
                local_path: asset.local_path.clone(),
                size_bytes: asset.size_bytes,
                sha256: asset.sha256.clone(),
            })
            .collect(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn unique_root() -> PathBuf {
        std::env::temp_dir().join(format!(
            "alcedo-model-manager-{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .expect("system clock should be valid")
                .as_nanos()
        ))
    }

    #[tokio::test]
    async fn lists_fixed_model_profiles_with_512_policy() {
        let root = unique_root();
        let service = ModelManagerServiceImpl::new(&root, "https://hf-mirror.com");
        let response = service
            .list_model_profiles(Request::new(ListModelProfilesRequest {
                model_root: root.to_string_lossy().into_owned(),
            }))
            .await
            .expect("list should succeed")
            .into_inner();

        assert_eq!(response.profiles.len(), 3);
        assert!(
            response
                .profiles
                .iter()
                .any(|profile| profile.language == "multilingual"
                    && profile.profile_id == "jina-clip-v2-int8-multilingual"
                    && profile.embedding_dimension == 512
                    && profile.native_embedding_dimension == 1024)
        );
        assert!(response.profiles.iter().all(|profile| !profile.installed));
        assert!(!root.exists());
    }

    #[tokio::test]
    async fn validate_missing_model_returns_structured_error() {
        let root = unique_root();
        let service = ModelManagerServiceImpl::new(&root, "https://hf-mirror.com");
        let response = service
            .validate_model(Request::new(ValidateModelRequest {
                profile_id: crate::service::model_assets::MOBILECLIP2_ONNX_PROFILE.to_string(),
                model_root: root.to_string_lossy().into_owned(),
            }))
            .await
            .expect("validate should return a response")
            .into_inner();

        assert!(!response.ok);
        assert!(response.error.contains("missing model root directory"));
    }
}

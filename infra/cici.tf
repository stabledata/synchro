provider "google" {
  project = "surface-420608"
}

resource "google_iam_workload_identity_pool" "github_actions_pool" {
  workload_identity_pool_id = "github-actions-pool"
  display_name              = "Github Actions"
  description               = "Pool for federating GitHub Actions identities"
}


resource "google_iam_workload_identity_pool_provider" "github_actions_provider" {
  workload_identity_pool_provider_id = "gha-id-pool-provider"
  workload_identity_pool_id = google_iam_workload_identity_pool.github_actions_pool.workload_identity_pool_id  # Corrected
  display_name              = "GitHub Actions Identity Provider"
  description               = "Identity Provider for GitHub Actions"

  attribute_condition = "assertion.repository_owner == 'stabledata'"
  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.aud" = "assertion.aud"
  }

  oidc {
    issuer_uri        = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account" "github_cicd_service_account" {
  account_id   = "github-actions"
  display_name = "GitHub CICD Actions Service Account"
  description  = "Service account for GitHub Actions CI/CD"
}

resource "google_service_account_iam_member" "allow_github_to_impersonate" {
  service_account_id = google_service_account.github_cicd_service_account.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/projects/${var.project}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_actions_pool.workload_identity_pool_id}/attribute.repository/stabledata/*"
}

resource "google_project_iam_member" "allow_push_to_artifact_registry" {
  project = var.project
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.github_cicd_service_account.email}"
}

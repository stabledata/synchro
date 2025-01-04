
resource "google_service_account" "app_synchro_service_account" {
  account_id   = "app-synchro"
  display_name = "Synchro Application"
  description  = "Service account for Synchro Service App Runtime"
}


resource "google_project_iam_member" "allow_secrets_access" {
  project = var.project
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.app_synchro_service_account.email}"
}
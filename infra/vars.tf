variable "project" {
  type = string
  description = "GCP Project Id"
  default = "791837997629"
}

provider "google" {
  project = "surface-420608"
}

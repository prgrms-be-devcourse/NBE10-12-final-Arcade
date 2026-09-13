terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # HCP Terraform은 상태·잠금·이력을 맡고, plan/apply는 로컬에서 실행한다.
  cloud {
    organization = "arcade-team05"

    # 데브코스 계정의 상태만 관리한다.
    workspaces {
      name = "devcos"
    }
  }
}

provider "aws" {
  region = var.region

  # 지정한 계정 외에는 apply하지 않는다.
  allowed_account_ids = [var.aws_account_id]

  default_tags {
    tags = {
      Team        = var.team_tag
      Project     = "arcade"
      Environment = local.environment
      ManagedBy   = "terraform"
    }
  }
}

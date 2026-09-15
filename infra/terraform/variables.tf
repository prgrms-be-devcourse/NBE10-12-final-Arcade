# AWS 대상 계정과 공통 리소스 식별자
variable "aws_account_id" {
  description = <<-EOT
    apply를 허용할 데브코스 AWS 계정 ID.
    현재 자격증명의 계정이 이 값과 다르면 provider가 중단한다.
  EOT
  type        = string

  validation {
    condition     = can(regex("^[0-9]{12}$", var.aws_account_id)) && var.aws_account_id != "000000000000"
    error_message = "예시 값이 아닌 실제 12자리 AWS 계정 ID여야 한다."
  }
}

variable "team_tag" {
  description = "모든 AWS 리소스의 태그와 이름 접두사에 사용할 데브코스 팀 식별자."
  type        = string
  default     = "devcos-team05"

  validation {
    condition     = can(regex("^devcos-team[0-9]{2}$", var.team_tag))
    error_message = "devcos-teamXX 형식이어야 한다."
  }
}

variable "region" {
  description = "데브코스 인프라를 배포할 AWS 리전."
  type        = string
  default     = "ap-northeast-2"

  validation {
    condition     = contains(["us-east-1", "ap-northeast-2", "ap-northeast-1"], var.region)
    error_message = "데브코스 SCP가 허용한 버지니아 북부·서울·도쿄 리전만 사용할 수 있다."
  }
}

variable "environment" {
  description = "리소스를 구분할 배포 환경 이름."
  type        = string

  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "environment는 dev 또는 prod여야 한다."
  }
}

# GitHub Actions OIDC 배포 대상
variable "github_repo" {
  description = "OIDC 신뢰 대상 GitHub 저장소(owner/name)."
  type        = string
  default     = "prgrms-be-devcourse/NBE10-12-final-Arcade"

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repo))
    error_message = "github_repo는 owner/name 형식이어야 한다."
  }
}

variable "github_repository_owner_id" {
  description = "GitHub 저장소 owner의 변경되지 않는 숫자 ID."
  type        = string

  validation {
    condition     = can(regex("^[1-9][0-9]*$", var.github_repository_owner_id))
    error_message = "github_repository_owner_id는 0이 아닌 숫자 ID여야 한다."
  }
}

variable "github_repository_id" {
  description = "OIDC 신뢰 대상 GitHub 저장소의 변경되지 않는 숫자 ID."
  type        = string

  validation {
    condition     = can(regex("^[1-9][0-9]*$", var.github_repository_id))
    error_message = "github_repository_id는 0이 아닌 숫자 ID여야 한다."
  }
}

variable "deploy_environment" {
  description = "CD job과 OIDC subject에 사용할 GitHub Environment 이름."
  type        = string
  default     = "prod"
}

# 네트워크와 서비스 주소
variable "vpc_cidr" {
  description = "데브코스 애플리케이션 VPC의 IPv4 CIDR."
  type        = string
  default     = "10.5.0.0/16"

  validation {
    condition     = !strcontains(var.vpc_cidr, ":") && can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr은 유효한 IPv4 CIDR이어야 한다."
  }
}

variable "public_subnet_cidr" {
  description = "EC2와 EICE를 배치할 퍼블릭 서브넷의 IPv4 CIDR."
  type        = string
  default     = "10.5.1.0/24"

  validation {
    condition     = !strcontains(var.public_subnet_cidr, ":") && can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr은 유효한 IPv4 CIDR이어야 한다."
  }
}

variable "domain" {
  description = "서비스의 루트 도메인. DNS 레코드는 도메인 제공자에서 별도로 관리한다."
  type        = string
  default     = "crewon.cloud"

  validation {
    condition     = can(regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$", var.domain))
    error_message = "domain은 scheme이나 경로가 없는 유효한 루트 도메인이어야 한다."
  }
}

# EC2와 로컬 스토리지 용량
variable "instance_type" {
  description = "데브코스에서 승인된 EC2 인스턴스 타입."
  type        = string
  default     = "t3.small"

  validation {
    condition     = contains(["t3.micro", "t3.small"], var.instance_type)
    error_message = "데브코스 계정에서는 t3.micro 또는 승인받은 t3.small만 사용한다."
  }
}

variable "root_volume_size" {
  description = "Docker 이미지와 PostgreSQL 데이터를 저장할 루트 EBS 크기(GB)."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 8 && var.root_volume_size <= 30
    error_message = "데브코스 기본 범위에서 루트 EBS는 8~30GB로 제한한다."
  }
}

variable "swap_size_mb" {
  description = "배포 중 신·구 컨테이너가 겹칠 때 사용할 swap 크기(MB)."
  type        = number
  default     = 2048

  validation {
    condition     = var.swap_size_mb >= 512 && var.swap_size_mb <= 4096
    error_message = "swap은 512~4096MB 범위로 제한한다."
  }
}

# 외부 접근 허용 범위
variable "operator_ssh_public_key" {
  description = "최초 EC2 생성 때 ec2-user에 등록할 운영자 OpenSSH 공개키."
  type        = string

  validation {
    condition = can(regex(
      "^(ssh-ed25519|ecdsa-sha2-nistp256|ecdsa-sha2-nistp384|ecdsa-sha2-nistp521|sk-ssh-ed25519@openssh.com|ssh-rsa) [A-Za-z0-9+/]+={0,3}( .*)?$",
      trimspace(var.operator_ssh_public_key),
    ))
    error_message = "operator_ssh_public_key에는 개인키가 아닌 유효한 OpenSSH 공개키 한 줄을 입력해야 한다."
  }
}

variable "app_cidrs" {
  description = "서비스의 HTTP·HTTPS 접근을 허용할 IPv4 CIDR 목록. 공개 서비스는 0.0.0.0/0을 지정한다."
  type        = list(string)

  validation {
    condition = (
      length(var.app_cidrs) > 0 &&
      alltrue([
        for cidr in var.app_cidrs : !strcontains(cidr, ":") && can(cidrhost(cidr, 0))
      ])
    )
    error_message = "app_cidrs에는 HTTP·HTTPS 접근을 허용할 유효한 IPv4 CIDR을 하나 이상 입력해야 한다."
  }
}

variable "ssh_allowed_cidrs" {
  description = "예외적으로 SSH(22)를 허용할 운영자 공인 IPv4 /32 목록. 비우면 EICE에서만 접근한다."
  type        = list(string)
  default     = []

  validation {
    condition = alltrue([
      for cidr in var.ssh_allowed_cidrs :
      cidr != "0.0.0.0/0" &&
      cidr != "::/0" &&
      !strcontains(cidr, ":") &&
      can(cidrhost(cidr, 0)) &&
      can(regex("/32$", cidr))
    ])
    error_message = "ssh_allowed_cidrs는 유효한 운영자 IPv4 /32만 허용하며 0.0.0.0/0은 사용할 수 없다."
  }
}

# 애플리케이션 객체 스토리지
variable "s3_bucket_name" {
  description = "전체 AWS에서 고유한 데브코스 사용자 업로드 S3 버킷 이름."
  type        = string

  validation {
    condition = (
      length(var.s3_bucket_name) >= 3 &&
      length(var.s3_bucket_name) <= 63 &&
      can(regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$", var.s3_bucket_name))
    )
    error_message = "S3 버킷 이름은 3~63자의 소문자·숫자·점·하이픈이어야 한다."
  }
}

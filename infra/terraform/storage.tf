# 공개 읽기가 필요한 업로드를 비공개 상태·백업 버킷과 분리한다.
resource "aws_s3_bucket" "uploads" {
  bucket = var.s3_bucket_name

  # 사용자 파일이 남아 있으면 destroy를 실패시킨다.
  force_destroy = false
}

# ACL은 막고 버킷 정책으로만 공개 읽기를 허용한다.
resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_ownership_controls" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_policy" "uploads_public_read" {
  bucket = aws_s3_bucket.uploads.id

  # 객체 읽기만 허용하고 버킷 목록 권한은 주지 않는다.
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicReadUploads"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.uploads.arn}/*"
    }]
  })

  depends_on = [
    aws_s3_bucket_public_access_block.uploads,
    aws_s3_bucket_ownership_controls.uploads,
  ]
}

resource "aws_s3_bucket_server_side_encryption_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

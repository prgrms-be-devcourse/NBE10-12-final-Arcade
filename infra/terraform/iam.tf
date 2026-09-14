# EC2 실행 주체: EC2 서비스만 맡을 수 있는 IAM 역할
resource "aws_iam_role" "instance" {
  name = "${local.name_prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# EC2 연결: 실행 역할을 EC2 인스턴스에 연결하는 프로파일
resource "aws_iam_instance_profile" "instance" {
  name = "${local.name_prefix}-ec2-profile"
  role = aws_iam_role.instance.name
}

# S3 접근 권한: 백엔드가 업로드 버킷의 객체를 올리고 지운다. 목록 조회는 주지 않는다.
resource "aws_iam_role_policy" "uploads_write" {
  name = "${local.name_prefix}-uploads-write"
  role = aws_iam_role.instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:DeleteObject"]
      Resource = "${aws_s3_bucket.uploads.arn}/*"
    }]
  })
}

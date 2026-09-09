/**
 * 스킬 입력 자동완성용 기술 목록.
 *
 * GitHub Linguist(languages.yml)는 파일 확장자로 언어를 판별하려고 만든 목록이라
 * 프레임워크·인프라가 통째로 없고(React·Spring·Docker·AWS 전부 없다) 대신
 * Apollo Guidance Computer 같은 게 562개 섞여 있다. 그래서 그대로 쓰지 않고,
 * **언어 표기만** 거기서 따오고(C# · Objective-C · PLpgSQL 같은 표기 통일) 나머지는 손으로 골랐다.
 *
 * 목록에 없는 값도 저장은 된다 — 화면이 "등록된 기술이 아니다" 라고 알려주기만 한다.
 * 부트캠프 파티에서 나올 스택은 뻔해서 이 정도면 대부분 덮이고, 그 이상은 관리 비용만 는다.
 */

/** 언어 — Linguist 표기 기준 */
const LANGUAGES = [
  'JavaScript', 'TypeScript', 'Java', 'Kotlin', 'Python', 'Go', 'Rust', 'C', 'C++', 'C#',
  'Swift', 'Objective-C', 'Ruby', 'PHP', 'Dart', 'Scala', 'Elixir', 'Erlang', 'Haskell',
  'Clojure', 'Groovy', 'Lua', 'Perl', 'R', 'MATLAB', 'Julia', 'Solidity', 'Zig', 'OCaml',
  'F#', 'Visual Basic .NET', 'Assembly', 'Shell', 'PowerShell', 'SQL', 'PLpgSQL', 'TSQL',
  'HTML', 'CSS', 'SCSS', 'Sass', 'Less', 'GraphQL', 'Protocol Buffer', 'HCL', 'Nix',
];

/** 프론트엔드 */
const FRONTEND = [
  'React', 'Next.js', 'Vue', 'Nuxt', 'Svelte', 'SvelteKit', 'Angular', 'Astro', 'Remix',
  'React Native', 'Flutter', 'Expo', 'Electron', 'jQuery',
  'Redux', 'Zustand', 'Recoil', 'TanStack Query', 'MobX', 'RxJS',
  'Tailwind CSS', 'styled-components', 'Emotion', 'Bootstrap', 'Material UI', 'Chakra UI',
  'Storybook', 'Vite', 'Webpack', 'Babel', 'esbuild', 'Turbopack',
  'Three.js', 'D3.js', 'Chart.js', 'Framer Motion',
];

/** 백엔드 · 서버 */
const BACKEND = [
  'Spring', 'Spring Boot', 'Spring Security', 'Spring Data JPA', 'Spring Batch', 'Spring Cloud',
  'JPA', 'Hibernate', 'MyBatis', 'QueryDSL', 'Lombok', 'Gradle', 'Maven', 'JUnit', 'Mockito',
  'Node.js', 'Express', 'NestJS', 'Fastify', 'Deno', 'Bun',
  'Django', 'Flask', 'FastAPI', 'Celery',
  'Ruby on Rails', 'Laravel', 'ASP.NET Core', 'Gin', 'Echo', 'Fiber', 'Actix', 'Axum',
  'Ktor', 'Micronaut', 'Quarkus', 'Phoenix',
  'REST API', 'gRPC', 'WebSocket', 'Server-Sent Events', 'OAuth 2.0', 'JWT', 'Swagger',
];

/** 데이터베이스 · 데이터 */
const DATA = [
  'PostgreSQL', 'MySQL', 'MariaDB', 'Oracle Database', 'SQL Server', 'SQLite', 'H2',
  'MongoDB', 'Redis', 'Elasticsearch', 'OpenSearch', 'Cassandra', 'DynamoDB', 'Firestore',
  'Neo4j', 'InfluxDB', 'ClickHouse', 'Snowflake', 'BigQuery',
  'Kafka', 'RabbitMQ', 'ActiveMQ', 'Apache Spark', 'Apache Airflow', 'Flink', 'Hadoop', 'dbt',
  'Pandas', 'NumPy', 'scikit-learn', 'PyTorch', 'TensorFlow', 'Keras', 'Hugging Face',
  'LangChain', 'OpenAI API',
];

/** 인프라 · 배포 · 운영 */
const INFRA = [
  'Docker', 'Docker Compose', 'Kubernetes', 'Helm', 'Terraform', 'Ansible', 'Vagrant',
  'AWS', 'EC2', 'S3', 'RDS', 'Lambda', 'ECS', 'EKS', 'CloudFront', 'Route 53',
  'GCP', 'Azure', 'Vercel', 'Netlify', 'Cloudflare', 'Firebase', 'Supabase', 'Heroku',
  'Nginx', 'Apache HTTP Server', 'Tomcat', 'Linux', 'Ubuntu',
  'GitHub Actions', 'Jenkins', 'GitLab CI', 'ArgoCD', 'CircleCI',
  'Prometheus', 'Grafana', 'Datadog', 'Sentry', 'ELK Stack', 'OpenTelemetry',
];

/** 협업 · 디자인 · 그 밖 */
const TOOLS = [
  'Git', 'GitHub', 'GitLab', 'Bitbucket', 'Jira', 'Confluence', 'Notion', 'Slack', 'Linear',
  'Figma', 'Zeplin', 'Adobe XD', 'Sketch', 'Photoshop', 'Illustrator',
  'Postman', 'Insomnia', 'IntelliJ IDEA', 'VS Code', 'Xcode', 'Android Studio',
  'Jest', 'Vitest', 'Cypress', 'Playwright', 'Testing Library', 'k6', 'JMeter',
  'ESLint', 'Prettier', 'Sonar',
  'TDD', 'DDD', 'MSA', 'CI/CD', '애자일', '스크럼', '코드 리뷰', '기술 문서 작성',
];

/** 자동완성 후보. 정렬하지 않는다 — 분류 순서가 곧 고르기 좋은 순서다 */
export const TECH_STACKS: readonly string[] = [
  ...LANGUAGES,
  ...FRONTEND,
  ...BACKEND,
  ...DATA,
  ...INFRA,
  ...TOOLS,
];

/**
 * 같은 기술의 다른 표기 → 목록의 표준 표기.
 *
 * 대소문자·공백 차이는 아래 lookup 이 알아서 흡수하므로, 여기에는 **철자가 다른 것만** 적는다.
 * 이걸 두는 이유는 java·JAVA·자바 가 서로 다른 행으로 저장돼 나중에 검색·매칭에 못 쓰이는 걸 막으려는 것이다.
 */
const ALIASES: Record<string, string> = {
  js: 'JavaScript', ts: 'TypeScript', 'node js': 'Node.js', node: 'Node.js',
  nextjs: 'Next.js', 'next js': 'Next.js', nuxtjs: 'Nuxt', vuejs: 'Vue', reactjs: 'React',
  golang: 'Go', 'c sharp': 'C#', csharp: 'C#', 'c plus plus': 'C++', cpp: 'C++',
  postgres: 'PostgreSQL', psql: 'PostgreSQL', mongo: 'MongoDB',
  // es(ECMAScript?)·tf(TensorFlow?) 처럼 뜻이 갈리는 줄임말은 넣지 않는다 - 틀리게 고쳐주는 게 더 나쁘다
  k8s: 'Kubernetes', gha: 'GitHub Actions',
  springboot: 'Spring Boot', 'spring-boot': 'Spring Boot', jpa: 'JPA',
  tailwind: 'Tailwind CSS', 'styled components': 'styled-components',
  자바: 'Java', 파이썬: 'Python', 코틀린: 'Kotlin', 리액트: 'React', 스프링: 'Spring',
  도커: 'Docker', 쿠버네티스: 'Kubernetes', 깃: 'Git', 피그마: 'Figma', 리눅스: 'Linux',
};

/** 비교용 키 — 대소문자·공백·점·하이픈 차이를 지운다 */
const keyOf = (value: string) => value.toLowerCase().replace(/[\s._-]/g, '');

const CANONICAL = new Map<string, string>([
  ...TECH_STACKS.map((name) => [keyOf(name), name] as const),
  ...Object.entries(ALIASES).map(([alias, name]) => [keyOf(alias), name] as const),
]);

/**
 * 입력을 목록의 표준 표기로 바꾼다. 목록에 없으면 공백만 정리해 그대로 돌려준다.
 * (2단계 — 막지 않고 알려만 준다)
 */
export function normalizeTechStack(value: string): string {
  const trimmed = value.trim().replace(/\s+/g, ' ');
  return CANONICAL.get(keyOf(trimmed)) ?? trimmed;
}

/** 목록에 있는 기술인지 */
export function isKnownTechStack(value: string): boolean {
  return CANONICAL.has(keyOf(value));
}

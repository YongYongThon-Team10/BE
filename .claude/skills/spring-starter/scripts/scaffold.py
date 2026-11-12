#!/usr/bin/env python3
"""Scaffold a Spring Boot + JWT project (or one new domain) from bundled templates.

Two subcommands:
  init         build the common/security/user boilerplate into a project
  add-domain   add one domain package following the same layering

The templates under assets/template/ are the real, compiling sources of the
reference project with identifiers replaced by {{TOKENS}}. Nothing here
generates Java from scratch -- it copies and substitutes, so what lands in the
target is exactly what was verified to build.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

SKILL_ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = SKILL_ROOT / "assets" / "template"

# Dependency lines the boilerplate needs. Checked against an existing
# build.gradle by "group:artifact" so a differing version is left alone.
REQUIRED_DEPS = [
    ("implementation", "org.springframework.boot:spring-boot-starter-data-jpa"),
    ("implementation", "org.springframework.boot:spring-boot-starter-data-redis"),
    ("implementation", "org.springframework.boot:spring-boot-starter-security"),
    ("implementation", "org.springframework.boot:spring-boot-starter-web"),
    ("implementation", "org.springframework.boot:spring-boot-starter-validation"),
    ("compileOnly", "org.projectlombok:lombok"),
    ("annotationProcessor", "org.projectlombok:lombok"),
    ("runtimeOnly", "org.mariadb.jdbc:mariadb-java-client"),
    ("implementation", "io.jsonwebtoken:jjwt-api:0.12.6"),
    ("runtimeOnly", "io.jsonwebtoken:jjwt-impl:0.12.6"),
    ("runtimeOnly", "io.jsonwebtoken:jjwt-jackson:0.12.6"),
    ("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3"),
    ("testImplementation", "org.springframework.boot:spring-boot-starter-test"),
    ("testImplementation", "org.springframework.security:spring-security-test"),
    ("testRuntimeOnly", "org.junit.platform:junit-platform-launcher"),
]

LOMBOK_CONFIG_BLOCK = """
configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}
"""


# --------------------------------------------------------------------------
# reporting
# --------------------------------------------------------------------------

class Report:
    def __init__(self) -> None:
        self.written: list[str] = []
        self.skipped: list[str] = []
        self.conflicts: list[str] = []
        self.notes: list[str] = []

    def dump(self, target: Path) -> None:
        def rel(p: str) -> str:
            try:
                return str(Path(p).relative_to(target))
            except ValueError:
                return p

        print(f"\n  created {len(self.written)} file(s)")
        for p in self.written:
            print(f"    + {rel(p)}")
        if self.skipped:
            print(f"\n  skipped {len(self.skipped)} file(s) (already present, identical intent)")
            for p in self.skipped:
                print(f"    = {rel(p)}")
        if self.conflicts:
            print(f"\n  CONFLICT -- {len(self.conflicts)} file(s) already existed and differ.")
            print("  A .scaffold copy was written next to each. Merge them by hand,")
            print("  or re-run with --force to overwrite.")
            for p in self.conflicts:
                print(f"    ! {rel(p)}")
        if self.notes:
            print("\n  next steps:")
            for n in self.notes:
                print(f"    - {n}")


# --------------------------------------------------------------------------
# naming helpers
# --------------------------------------------------------------------------

def pascal(text: str) -> str:
    parts = re.split(r"[^0-9A-Za-z]+", text)
    out = []
    for p in parts:
        if not p:
            continue
        out.append(p[0].upper() + p[1:])
    return "".join(out)


def title_words(text: str) -> str:
    parts = [p for p in re.split(r"[^0-9A-Za-z]+", text) if p]
    return " ".join("API" if p.lower() == "api" else p[0].upper() + p[1:] for p in parts)


def default_title(project_name: str) -> str:
    """Swagger title. Don't append "API" to a name that already ends in it."""
    words = title_words(project_name)
    return words if words.endswith("API") else words + " API"


def validate_package(pkg: str) -> str:
    if not re.fullmatch(r"[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)+", pkg):
        die(
            f"invalid package name: {pkg!r}\n"
            "  expected lowercase, dot-separated, at least two segments (e.g. kr.yuns.myapp)"
        )
    return pkg


def die(msg: str) -> None:
    print(f"error: {msg}", file=sys.stderr)
    sys.exit(1)


# --------------------------------------------------------------------------
# project inspection
# --------------------------------------------------------------------------

def find_java_source_root(target: Path) -> Path:
    return target / "src" / "main" / "java"


def detect_application_class(target: Path) -> tuple[str, str, Path] | None:
    """Return (package, class_name, path) of the @SpringBootApplication class."""
    root = find_java_source_root(target)
    if not root.is_dir():
        return None
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        if "@SpringBootApplication" not in text:
            continue
        pkg_match = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.M)
        cls_match = re.search(r"\bclass\s+(\w+)", text)
        if pkg_match and cls_match:
            return pkg_match.group(1), cls_match.group(1), path
    return None


def detect_package(target: Path) -> str | None:
    """Package of the app class, else the deepest single-chain package dir."""
    app = detect_application_class(target)
    if app:
        return app[0]
    root = find_java_source_root(target)
    if not root.is_dir():
        return None
    for path in sorted(root.rglob("*.java")):
        m = re.search(r"^\s*package\s+([\w.]+)\s*;", path.read_text(encoding="utf-8", errors="replace"), re.M)
        if m:
            return m.group(1)
    return None


def detect_project_name(target: Path) -> str | None:
    settings = target / "settings.gradle"
    if not settings.is_file():
        settings = target / "settings.gradle.kts"
    if settings.is_file():
        m = re.search(r"rootProject\.name\s*=\s*['\"]([^'\"]+)['\"]", settings.read_text(encoding="utf-8"))
        if m:
            return m.group(1)
    return None


# --------------------------------------------------------------------------
# file emission
# --------------------------------------------------------------------------

def substitute(text: str, tokens: dict[str, str]) -> str:
    for key, value in tokens.items():
        text = text.replace("{{" + key + "}}", value)
    return text


def resolve_name(name: str, tokens: dict[str, str]) -> str:
    for key, value in tokens.items():
        name = name.replace("__" + key + "__", value)
    return name


def emit(src: Path, dest: Path, tokens: dict[str, str], report: Report, force: bool, binary: bool = False) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if binary:
        payload_bytes = src.read_bytes()
    else:
        payload_bytes = substitute(src.read_text(encoding="utf-8"), tokens).encode("utf-8")

    if dest.exists():
        existing = dest.read_bytes()
        if existing == payload_bytes:
            report.skipped.append(str(dest))
            return
        if not force:
            side = dest.with_name(dest.name + ".scaffold")
            side.write_bytes(payload_bytes)
            report.conflicts.append(str(dest))
            return
    dest.write_bytes(payload_bytes)
    report.written.append(str(dest))


def emit_tree(src_root: Path, dest_root: Path, tokens: dict[str, str], report: Report, force: bool) -> None:
    """Copy a template tree, substituting __TOKEN__ in path segments."""
    for src in sorted(src_root.rglob("*")):
        if src.is_dir():
            continue
        rel_parts = [resolve_name(p, tokens) for p in src.relative_to(src_root).parts]
        dest = dest_root.joinpath(*rel_parts)
        emit(src, dest, tokens, report, force, binary=src.suffix == ".jar")


# --------------------------------------------------------------------------
# build.gradle grafting
# --------------------------------------------------------------------------

def find_block(text: str, header_pattern: str) -> tuple[int, int] | None:
    """Return (open_brace_index, close_brace_index) of a top-level `name { ... }`."""
    m = re.search(header_pattern, text, re.M)
    if not m:
        return None
    open_idx = text.index("{", m.start())
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return open_idx, i
    return None


def graft_build_gradle(target: Path, report: Report) -> None:
    path = target / "build.gradle"
    if not path.is_file():
        report.notes.append(
            "build.gradle.kts detected -- Kotlin DSL is not patched automatically; "
            "add the dependencies listed above by hand."
        )
        return

    text = path.read_text(encoding="utf-8")
    block = find_block(text, r"^\s*dependencies\s*\{")
    if block is None:
        report.notes.append("could not locate a `dependencies { }` block in build.gradle -- add deps by hand.")
        return
    open_idx, close_idx = block
    body = text[open_idx + 1:close_idx]

    missing = []
    for config, coord in REQUIRED_DEPS:
        artifact = ":".join(coord.split(":")[:2])
        # already declared under this configuration?
        if re.search(rf"^\s*{config}\b.*{re.escape(artifact)}", body, re.M):
            continue
        missing.append((config, coord))

    added_lombok = any(c == "org.projectlombok:lombok" for _, c in missing)

    if missing:
        insert = "".join(f"    {config} '{coord}'\n" for config, coord in missing)
        text = text[:close_idx] + insert + text[close_idx:]
        path.write_text(text, encoding="utf-8")
        report.notes.append(f"added {len(missing)} dependency line(s) to build.gradle")
    else:
        report.notes.append("build.gradle already declared every required dependency")

    # lombok needs compileOnly to extend annotationProcessor
    text = path.read_text(encoding="utf-8")
    if added_lombok and find_block(text, r"^\s*configurations\s*\{") is None:
        anchor = find_block(text, r"^\s*(java|dependencies)\s*\{")
        if anchor:
            _, close = anchor
            text = text[:close + 1] + "\n" + LOMBOK_CONFIG_BLOCK + text[close + 1:]
            path.write_text(text, encoding="utf-8")
            report.notes.append("added the `configurations { compileOnly { extendsFrom annotationProcessor } }` block")


# --------------------------------------------------------------------------
# graft patches: timezone in the existing app class, .env in .gitignore
# --------------------------------------------------------------------------

def patch_app_timezone(path: Path, timezone: str, report: Report) -> None:
    """Set the JVM default timezone at the top of main() in an app class the scaffold did not write."""
    text = path.read_text(encoding="utf-8")
    if "TimeZone.setDefault" in text:
        report.notes.append(f"{path.name} already sets a default TimeZone -- left as is")
        return
    m = re.search(r"public\s+static\s+void\s+main\s*\([^)]*\)\s*\{\n", text)
    if not m:
        report.notes.append(
            f"could not find main() in {path.name} -- add "
            f'TimeZone.setDefault(TimeZone.getTimeZone("{timezone}")); before SpringApplication.run yourself'
        )
        return
    indent = "        "
    line = (f"{indent}// 빈 생성 전에 JVM 기본 시간대를 고정해 LocalDateTime.now() 등이 "
            "서버 위치와 무관하게 같은 시간대를 쓰게 한다\n"
            f'{indent}TimeZone.setDefault(TimeZone.getTimeZone("{timezone}"));\n')
    text = text[:m.end()] + line + text[m.end():]
    if "import java.util.TimeZone;" not in text:
        imports = list(re.finditer(r"^import\s+[\w.]+;\n", text, re.M))
        if imports:
            at = imports[-1].end()
            text = text[:at] + "\nimport java.util.TimeZone;\n" + text[at:]
        else:
            pkg = re.search(r"^package\s+[\w.]+;\n", text, re.M)
            at = pkg.end() if pkg else 0
            text = text[:at] + "\nimport java.util.TimeZone;\n" + text[at:]
    path.write_text(text, encoding="utf-8")
    report.notes.append(f"set the default TimeZone to {timezone} at the top of {path.name}.main()")


def ensure_env_ignored(target: Path, report: Report) -> None:
    path = target / ".gitignore"
    if not path.is_file():
        path.write_text("### Env ###\n.env\n", encoding="utf-8")
        report.notes.append("created .gitignore with .env")
        return
    text = path.read_text(encoding="utf-8")
    if re.search(r"^/?\.env\s*$", text, re.M):
        return
    sep = "" if text.endswith("\n") else "\n"
    path.write_text(text + f"{sep}\n### Env ###\n.env\n", encoding="utf-8")
    report.notes.append("added .env to .gitignore")


# --------------------------------------------------------------------------
# roles
# --------------------------------------------------------------------------

ROLE_NAME_RE = r"[A-Z][A-Z0-9_]*"


def parse_roles(raw: str) -> list[str]:
    roles = [r.strip().upper() for r in raw.split(",") if r.strip()]
    if not roles:
        die("--roles is empty -- pass at least one role, e.g. --roles ADMIN,USER")
    for r in roles:
        if not re.fullmatch(ROLE_NAME_RE, r):
            die(f"invalid role name: {r!r}\n  expected UPPER_SNAKE_CASE without the ROLE_ prefix, e.g. ADMIN")
        if r.startswith("ROLE_"):
            die(f"role {r!r} already carries the ROLE_ prefix -- drop it, the enum adds it")
    dupes = sorted({r for r in roles if roles.count(r) > 1})
    if dupes:
        die(f"duplicate role(s) in --roles: {', '.join(dupes)}")
    return roles


def parse_hierarchy(raw: str | None, roles: list[str]) -> list[tuple[str, str]]:
    """'ADMIN>OWNER,ADMIN>MANAGER' or chains like 'ADMIN>OWNER>MANAGER' -> [(hi, lo), ...]."""
    if not raw:
        return []
    pairs: list[tuple[str, str]] = []
    for chain in raw.split(","):
        names = [n.strip().upper() for n in chain.split(">") if n.strip()]
        if len(names) < 2:
            die(f"invalid --role-hierarchy entry: {chain!r}\n  expected HIGHER>LOWER, e.g. ADMIN>USER")
        for n in names:
            if n not in roles:
                die(f"--role-hierarchy names {n!r}, which is not in --roles ({', '.join(roles)})")
        for hi, lo in zip(names, names[1:]):
            if hi == lo:
                die(f"--role-hierarchy has a role above itself: {hi}>{lo}")
            if (hi, lo) not in pairs:
                pairs.append((hi, lo))
    return pairs


def parse_role_requirement(raw: str) -> list[str]:
    return [r.strip().upper() for r in raw.split(",") if r.strip()]


def detect_roles(target: Path, package: str) -> list[str] | None:
    """Constants of the project's UserRole enum, or None if it can't be found."""
    path = find_java_source_root(target).joinpath(*package.split(".")) / "user" / "data" / "enums" / "UserRole.java"
    if not path.is_file():
        return None
    return re.findall(rf'^\s*({ROLE_NAME_RE})\s*\(\s*"ROLE_', path.read_text(encoding="utf-8"), re.M)


def role_tokens(package: str, roles: list[str], default_role: str | None,
                hierarchy: list[tuple[str, str]]) -> dict[str, str]:
    """Template tokens for UserRole, User, SignUpRequestDto, AuthService and SecurityConfiguration.

    default_role=None means signup takes the role from the request body.
    """
    enum_values = ",\n".join(f'    {r}("ROLE_{r}")' for r in roles) + ";"

    if default_role:
        user_role_field = f"    @Builder.Default\n    private UserRole role = UserRole.{default_role};"
        signup_enum_import = signup_notnull_import = signup_field = signup_builder = ""
    else:
        user_role_field = "    private UserRole role;"
        signup_enum_import = f"import {package}.user.data.enums.UserRole;\n"
        signup_notnull_import = "\nimport jakarta.validation.constraints.NotNull;"
        signup_field = "\n\n    @NotNull\n    private UserRole role;"
        signup_builder = "\n                .role(signUpRequestDto.getRole())"

    if hierarchy:
        lines = "\n".join(f"                ROLE_{hi} > ROLE_{lo}" for hi, lo in hierarchy)
        hierarchy_imports = (
            "import org.springframework.security.access.hierarchicalroles.RoleHierarchy;\n"
            "import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;\n"
        )
        hierarchy_bean = (
            "\n\n    // 상위 권한은 하위 권한의 경로에도 접근할 수 있다. 여기 적히지 않은 권한끼리는 서로 포함하지 않는다.\n"
            "    @Bean\n"
            "    public static RoleHierarchy roleHierarchy() {\n"
            '        return RoleHierarchyImpl.fromHierarchy("""\n'
            f"{lines}\n"
            '                """);\n'
            "    }"
        )
    else:
        hierarchy_imports = hierarchy_bean = ""

    return {
        "ROLE_ENUM_VALUES": enum_values,
        "USER_ROLE_FIELD": user_role_field,
        "SIGNUP_ROLE_ENUM_IMPORT": signup_enum_import,
        "SIGNUP_ROLE_NOTNULL_IMPORT": signup_notnull_import,
        "SIGNUP_ROLE_FIELD": signup_field,
        "SIGNUP_ROLE_BUILDER": signup_builder,
        "ROLE_HIERARCHY_IMPORTS": hierarchy_imports,
        "ROLE_HIERARCHY_BEAN": hierarchy_bean,
    }


# --------------------------------------------------------------------------
# SecurityConfiguration patching
# --------------------------------------------------------------------------

def add_security_matcher(target: Path, package: str, domain: str, roles: list[str], report: Report) -> None:
    path = find_java_source_root(target).joinpath(*package.split(".")) / "common" / "security" / "SecurityConfiguration.java"
    if not path.is_file():
        report.notes.append(
            f"SecurityConfiguration.java not found -- authorise /api/v1/{domain}/** yourself"
        )
        return
    text = path.read_text(encoding="utf-8")
    if len(roles) == 1:
        rule = f'hasRole("{roles[0]}")'
    else:
        rule = "hasAnyRole(" + ", ".join(f'"{r}"' for r in roles) + ")"
    matcher = f'.requestMatchers("/api/v1/{domain}/**").{rule}'
    if matcher in text:
        return
    anchor = ".anyRequest().authenticated()"
    if anchor not in text:
        report.notes.append(
            f"could not find `.anyRequest().authenticated()` -- add {matcher} to SecurityConfiguration yourself"
        )
        return
    text = text.replace(anchor, f"{matcher}\n                        {anchor}", 1)
    path.write_text(text, encoding="utf-8")
    report.notes.append(
        f"authorised /api/v1/{domain}/** for {', '.join('ROLE_' + r for r in roles)} in SecurityConfiguration"
    )


# --------------------------------------------------------------------------
# subcommand: add-domain
# --------------------------------------------------------------------------

def do_add_domain(args: argparse.Namespace, report: Report) -> None:
    target = Path(args.target).resolve()
    package = args.package or detect_package(target)
    if not package:
        die(
            f"could not detect the base package under {target}/src/main/java\n"
            "  pass --package explicitly, or run `init` first"
        )
    validate_package(package)

    domain = args.domain
    if not re.fullmatch(r"[a-z][A-Za-z0-9]*", domain):
        die(f"invalid domain name: {domain!r}\n  expected a lowerCamelCase java identifier, e.g. order")

    variant = "standalone" if args.standalone else "owned"
    src_root = TEMPLATE / "domain" / variant
    tokens = {
        "PACKAGE": package,
        "PACKAGE_PATH": "/".join(package.split(".")),
        "DOMAIN": domain,
        "DOMAIN_CLASS": pascal(domain),
    }

    dest_root = find_java_source_root(target)
    if not dest_root.is_dir():
        die(f"{dest_root} does not exist -- run `init` first")

    if variant == "owned":
        known = getattr(args, "known_roles", None) or detect_roles(target, package)
        if args.role:
            required = parse_role_requirement(args.role)
        elif known and "USER" in known:
            required = ["USER"]
        elif known:
            die(
                "this project's UserRole has no USER role, so there is no default for the security matcher.\n"
                f"  pass --role with one or more of: {', '.join(known)} (e.g. --role {known[-1]})"
            )
        else:
            required = ["USER"]
        if known:
            unknown = [r for r in required if r not in known]
            if unknown:
                die(f"--role names {', '.join(unknown)}, which UserRole does not define ({', '.join(known)})")

    emit_tree(src_root, dest_root, tokens, report, args.force)

    if variant == "owned":
        add_security_matcher(target, package, domain, required, report)

    report.notes.append(
        f"replace the placeholder `name`/`description` fields in {pascal(domain)}, "
        f"{pascal(domain)}RequestDto and {pascal(domain)}ResponseDto with this domain's real fields"
    )
    if variant == "owned":
        report.notes.append(
            f"{pascal(domain)} carries a @ManyToOne User owner and {pascal(domain)}Service resolves it "
            "via AuthService.getUserEntity(email) -- drop both if this domain is not user-owned"
        )


# --------------------------------------------------------------------------
# subcommand: init
# --------------------------------------------------------------------------

def do_init(args: argparse.Namespace, report: Report) -> None:
    target = Path(args.target).resolve()
    target.mkdir(parents=True, exist_ok=True)

    existing_build = (target / "build.gradle").is_file() or (target / "build.gradle.kts").is_file()
    mode = "graft" if existing_build else "full"

    detected_app = detect_application_class(target) if mode == "graft" else None
    detected_pkg = detected_app[0] if detected_app else (detect_package(target) if mode == "graft" else None)

    # ---- package ----
    if detected_pkg:
        if args.package and args.package != detected_pkg:
            die(
                f"this project already uses package {detected_pkg!r}, "
                f"but --package said {args.package!r}.\n"
                "  The existing package wins -- drop --package, or fix it to match."
            )
        package = detected_pkg
    else:
        if not args.package:
            die("no existing package detected -- pass --package (e.g. --package kr.yuns.myapp)")
        package = validate_package(args.package)

    # ---- project name / group / app class ----
    project_name = args.name or detect_project_name(target) or target.name
    group = args.group or ".".join(package.split(".")[:-1]) or package
    if detected_app:
        app_class = detected_app[1]
    else:
        app_class = pascal(project_name) + "Application"
    project_title = args.title or default_title(project_name)

    # ---- roles ----
    roles = parse_roles(args.roles)
    hierarchy = parse_hierarchy(args.role_hierarchy, roles)
    if args.role_from_request and args.default_role:
        die("--default-role and --role-from-request are mutually exclusive -- pick one")
    if args.role_from_request:
        default_role = None
    else:
        default_role = (args.default_role or ("USER" if "USER" in roles else "")).upper() or None
        if default_role is None:
            die(
                f"--roles ({', '.join(roles)}) has no USER, so signup has no default role.\n"
                "  pass --default-role <ROLE> to assign one on signup,\n"
                "  or --role-from-request to let the signup body choose it"
            )
        if default_role not in roles:
            die(f"--default-role {default_role!r} is not in --roles ({', '.join(roles)})")

    tokens = {
        "PACKAGE": package,
        "PACKAGE_PATH": "/".join(package.split(".")),
        "APP_CLASS": app_class,
        "PROJECT_NAME": project_name,
        "PROJECT_TITLE": project_title,
        "GROUP": group,
        "TIMEZONE": args.timezone,
        **role_tokens(package, roles, default_role, hierarchy),
    }

    print(f"  mode         {mode}  ({'grafting onto an existing project' if mode == 'graft' else 'creating a new project'})")
    print(f"  target       {target}")
    print(f"  package      {package}")
    print(f"  project      {project_name}")
    print(f"  group        {group}")
    print(f"  app class    {app_class}")
    print(f"  title        {project_title}")
    print(f"  timezone     {args.timezone}")
    print(f"  roles        {', '.join(roles)}")
    print(f"  signup role  {default_role or 'from request body (SignUpRequestDto.role)'}")
    if hierarchy:
        print(f"  hierarchy    {', '.join(f'{hi} > {lo}' for hi, lo in hierarchy)}")

    core = TEMPLATE / "core"

    # ---- java sources ----
    java_src = core / "java"
    for sub in ("common", "user"):
        emit_tree(
            java_src / sub,
            find_java_source_root(target).joinpath(*package.split(".")) / sub,
            tokens, report, args.force,
        )

    if mode == "full":
        emit(
            java_src / "__APP_CLASS__.java",
            find_java_source_root(target).joinpath(*package.split(".")) / f"{app_class}.java",
            tokens, report, args.force,
        )
        emit(
            core / "test" / "__APP_CLASS__Tests.java",
            target / "src" / "test" / "java" / Path(*package.split(".")) / f"{app_class}Tests.java",
            tokens, report, args.force,
        )

    # ---- resources ----
    res_src = core / "resources"
    res_dest = target / "src" / "main" / "resources"
    props = res_dest / "application.properties"
    if props.is_file() and not props.read_text(encoding="utf-8").strip():
        props.unlink()
        report.notes.append("removed the empty application.properties left by Spring Initializr")
    for yml in sorted(res_src.glob("*.yml")):
        emit(yml, res_dest / yml.name, tokens, report, args.force)

    # ---- .env.example (both modes) ----
    emit(core / ".env.example", target / ".env.example", tokens, report, args.force)

    # ---- gradle / build files ----
    if mode == "full":
        for name in ("build.gradle", "settings.gradle", ".gitignore", ".gitattributes"):
            emit(core / name, target / name, tokens, report, args.force)
        emit_tree(core / "gradle", target / "gradle", tokens, report, args.force)
        for wrapper in ("gradlew", "gradlew.bat"):
            emit(core / wrapper, target / wrapper, tokens, report, args.force)
        os.chmod(target / "gradlew", 0o755)
        report.notes.append(
            "run `./gradlew compileJava` to verify the project compiles "
            "(`./gradlew build` also runs contextLoads, which needs a live MariaDB and Redis)"
        )
    else:
        graft_build_gradle(target, report)
        ensure_env_ignored(target, report)
        if detected_app:
            patch_app_timezone(detected_app[2], args.timezone, report)

    # ---- optional demo domain ----
    if args.with_demo:
        demo = argparse.Namespace(
            target=str(target), package=package, domain=args.demo_name,
            standalone=False, force=args.force, known_roles=roles,
            role=default_role or ",".join([r for r in roles if r != "ADMIN"] or roles),
        )
        do_add_domain(demo, report)

    if default_role is None:
        report.notes.append(
            "signup stores whatever role the request sends, ADMIN included -- "
            "reject privileged roles in AuthService.signUp if that is not intended"
        )

    report.notes.append(
        "start MariaDB and Redis before booting: "
        "`docker run -d -p 3306:3306 -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE="
        f"{project_name} mariadb:11` and `docker run -d -p 6379:6379 redis:7`"
    )
    report.notes.append(
        "application-local.yml ships a throwaway base64 JWT secret; set JWT_SECRET_KEY "
        "(and every application-prod.yml env var) before deploying"
    )


# --------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        prog="scaffold.py",
        description="Scaffold the Spring Boot + JWT boilerplate, or one new domain.",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init", help="build the common/security/user boilerplate")
    p_init.add_argument("--target", default=".", help="project directory (default: cwd)")
    p_init.add_argument("--package", help="base package; required only when nothing exists to detect")
    p_init.add_argument("--name", help="project name (default: settings.gradle rootProject.name, else dir name)")
    p_init.add_argument("--group", help="gradle group (default: package minus its last segment)")
    p_init.add_argument("--title", help="Swagger title (default: derived from the project name)")
    p_init.add_argument("--with-demo", action="store_true", help="also generate the example CRUD domain")
    p_init.add_argument("--demo-name", default="product", help="name of the example domain (default: product)")
    p_init.add_argument("--roles", default="ADMIN,USER",
                        help="comma-separated UserRole constants, no ROLE_ prefix (default: ADMIN,USER)")
    p_init.add_argument("--default-role",
                        help="role assigned on signup (default: USER when --roles has it)")
    p_init.add_argument("--role-from-request", action="store_true",
                        help="signup takes the role from the request body instead of a default")
    p_init.add_argument("--role-hierarchy",
                        help="e.g. 'ADMIN>OWNER,ADMIN>MANAGER' or 'ADMIN>MANAGER>USER'; generates a RoleHierarchy bean")
    p_init.add_argument("--timezone", default="Asia/Seoul",
                        help="JVM default timezone set in the app class main() (default: Asia/Seoul)")
    p_init.add_argument("--force", action="store_true", help="overwrite conflicting files")

    p_dom = sub.add_parser("add-domain", help="add one domain package")
    p_dom.add_argument("domain", help="domain name, lowerCamelCase (e.g. order)")
    p_dom.add_argument("--target", default=".", help="project directory (default: cwd)")
    p_dom.add_argument("--package", help="base package (default: detected from the app class)")
    p_dom.add_argument("--standalone", action="store_true", help="no User owner relation")
    p_dom.add_argument("--role",
                       help="role(s) required for /api/v1/<domain>/**, comma-separated for hasAnyRole "
                            "(default: USER when UserRole has it)")
    p_dom.add_argument("--force", action="store_true", help="overwrite conflicting files")

    args = parser.parse_args()
    report = Report()
    if args.command == "init":
        do_init(args, report)
    else:
        do_add_domain(args, report)
    report.dump(Path(args.target).resolve())


if __name__ == "__main__":
    main()

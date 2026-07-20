import { existsSync } from "node:fs";
import { spawn } from "node:child_process";
import { join } from "node:path";

const mode = process.argv[2] ?? "dev";
const rootDirectory = process.cwd();
const backendDirectory = join(rootDirectory, "backend");

const isWindows = process.platform === "win32";
const wrapperName = isWindows ? "mvnw.cmd" : "mvnw";
const wrapperPath = join(backendDirectory, wrapperName);
const pomPath = join(backendDirectory, "pom.xml");

if (!existsSync(pomPath) || !existsSync(wrapperPath)) {
  console.log(
    `[BACKEND] Chưa có Spring Boot trong backend/. Bỏ qua bước ${mode}.`
  );
  process.exit(0);
}

const mavenArguments =
  mode === "build"
    ? ["clean", "package"]
    : ["spring-boot:run"];

const child = isWindows
  ? spawn(wrapperPath, mavenArguments, {
      cwd: backendDirectory,
      stdio: "inherit",
      shell: true,
    })
  : spawn("sh", ["./mvnw", ...mavenArguments], {
      cwd: backendDirectory,
      stdio: "inherit",
    });

child.on("error", (error) => {
  console.error("[BACKEND] Không thể khởi động backend:", error.message);
  process.exit(1);
});

child.on("exit", (code) => {
  process.exit(code ?? 1);
});

process.on("SIGINT", () => {
  child.kill("SIGINT");
});

process.on("SIGTERM", () => {
  child.kill("SIGTERM");
});

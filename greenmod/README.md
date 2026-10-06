# Kelp Client (Fabric, Minecraft 26.1.2) - made by ocxh

Client-side mod. Needs Fabric Loader 0.19.2+, Fabric API and Java 25.

- Open the menu with **Right Shift**.
- Restricted modules (Flight, Fast Sprint, Scaffold, Auto Totem, ESP, Xray, aim assists, ...) are locked on public servers.
  Allow private/test servers under Settings -> Server Safety.
- Profiles and settings live in `config/kelpclient/`.

## Build on GitHub
1. Upload everything in this folder to a repository (the `.github` folder must keep its path `.github/workflows/build.yml`).
2. Open the **Actions** tab -> "Build mod" -> download the `kelpclient-jar` artifact.
3. Put the `.jar` in your `mods` folder (next to Fabric API).

{
  description = "Dev shell for template-pure-java";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";

  outputs = { self, nixpkgs }:
    let
      systems = [ "aarch64-darwin" "x86_64-darwin" "x86_64-linux" "aarch64-linux" ];
      forEachSystem = f: builtins.listToAttrs (map (system: { name = system; value = f system; }) systems);
    in
    {
      devShells = forEachSystem (system:
        let
          pkgs = import nixpkgs { inherit system; };

          chosenJdk = pkgs.jdk25_headless;
        in
        {
          # Shell for local + CI builds, used by universal-build
          default = pkgs.mkShell {
            packages = [
              chosenJdk
            ];

            shellHook = ''
              export JAVA_HOME=${chosenJdk}
            '';
          };

          # Shell for runtime-only environment.
          # The Dockerfile snapshots this environment via `nix print-dev-env` and applies it at container startup.
          runtime = pkgs.mkShell {
            packages = [
              chosenJdk
            ];

            shellHook = ''
              export JAVA_HOME=${chosenJdk}
            '';
          };
        });
    };
}

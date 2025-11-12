{ pkgs, lib, config, inputs, ... }:

{
  packages = with pkgs; [
    babashka
    bbin
    clj-kondo
    cljfmt
    git
    vimPlugins.parinfer-rust
  ];

  languages = {
    javascript = { enable = true; };
    clojure = { enable = true; };
    java = { enable = true; };
  };

  enterShell = ''
    export PATH="$(bbin bin):$PATH"
  '';
}

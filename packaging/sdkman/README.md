# SDKMAN candidate registration for `regulus-cli`.
#
# To register this candidate, fork https://github.com/sdkman/sdkman-candidates
# and add a new directory `regulus-cli/` with:
#
#   - candidate.txt
#   - 0.2.1.yml
#   - 0.2.0.yml
#   - 0.1.0.yml
#
# Then open a PR against sdkman/sdkman-candidates. SDKMAN reviews new
# candidates within ~1-2 weeks; the candidate becomes available globally
# via `sdk install regulus-cli` once merged.
#
# Once SDKMAN publishes the candidate, the canonical install path becomes:
#
#   curl -fsSL https://get.sdkman.io | bash
#   source ~/.sdkman/bin/sdkman-init.sh
#   sdk install regulus-cli
#
# This dir is the source of truth for what we send to the SDKMAN
# upstream. Update here, regenerate, then re-open a sync PR when
# a new Regulus CLI release ships.
